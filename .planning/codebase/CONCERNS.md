# Codebase Concerns

**Analysis Date:** 2026-03-19

## Tech Debt

**Large Frontend Components:**
- Issue: `ArmyViewPage.tsx` (836 lines) is significantly over-sized and manages too much state simultaneously
- Files: `frontend/src/pages/ArmyViewPage.tsx`
- Impact: Component becomes difficult to test, maintain, and reason about; renders multiple effect chains with complex interdependencies that are prone to race conditions
- Fix approach: Extract battle view state into separate hook; extract edit mode logic into custom hook; split component into smaller sub-components with focused responsibilities

**Complex Validation Logic:**
- Issue: `ArmyValidator.scala` (373 lines) combines multiple validation concerns in one module with complex interdependencies between checks
- Files: `backend/src/main/scala/wp40k/domain/army/ArmyValidator.scala`
- Impact: Hard to add new validations without understanding all existing ones; difficult to reuse individual checks
- Fix approach: Consider decomposing into smaller, composable validation modules; each validation should be independently testable

**Wargear Filter Complexity:**
- Issue: `WargearFilter.scala` (285 lines) has complex nested logic for weapon selection and filtering that's hard to follow
- Files: `backend/src/main/scala/wp40k/domain/army/WargearFilter.scala`
- Impact: Risk of subtle bugs when wargear selection logic changes; difficult to understand intent of weapon matching
- Fix approach: Add helper functions to break down matching logic; document weapon prefix matching strategy

## Error Handling Gaps

**Insufficient Async Error Handling in Frontend:**
- Issue: Multiple Promise.all chains in `ArmyViewPage.tsx` don't properly handle individual promise rejections or timeouts
- Files: `frontend/src/pages/ArmyViewPage.tsx` (lines 178-213)
- Impact: If one API call in a chain fails, entire state update fails without granular error reporting; users see blank screens without context
- Fix approach: Wrap individual API calls in try-catch blocks; provide fallback empty data for failed fetches; add timeout handling for slow endpoints

**Silent Error Swallowing:**
- Issue: `api.ts` uses `.catch(() => ({}))` to handle malformed error responses but swallows the actual parse error
- Files: `frontend/src/api.ts` (lines with res.json().catch())
- Impact: When API returns non-JSON error, users see unhelpful generic error messages; harder to debug API issues
- Fix approach: Log parse errors separately; throw more descriptive error with response status + body

**Missing Error Context in UI:**
- Issue: Session storage parsing errors are silently caught without logging
- Files: `frontend/src/pages/ArmyViewPage.tsx` (lines 149, 152) and `FactionDetailPage.tsx`
- Impact: Silent failures make debugging storage issues impossible; users lose session state without warning
- Fix approach: Add console.error or proper error boundary; consider localStorage version migration strategy

## Security Concerns

**Client-Side Token Storage:**
- Risk: Auth token stored in localStorage, vulnerable to XSS attacks
- Files: `frontend/src/context/AuthContext.tsx` (line 34, 48), `frontend/src/components/Header.tsx`
- Current mitigation: DOMPurify sanitization used for HTML content; no tokenization happens on untrusted content
- Recommendations:
  - Add HttpOnly cookie support as primary auth mechanism (would require backend change)
  - Implement token refresh expiration (currently no expiry check)
  - Add localStorage clearing on tab close (implement beforeunload handler)

**HTML Sanitization Dependency Risk:**
- Risk: Security relies on DOMPurify being up-to-date; any version lag is a vulnerability
- Files: `frontend/src/sanitize.ts`, used throughout UI for ability/stratagem descriptions
- Current mitigation: DOMPurify ^3.3.1 pinned in package.json
- Recommendations:
  - Implement automated dependency checking (GitHub security alerts active?)
  - Consider server-side HTML filtering as additional layer
  - Audit what HTML constructs are actually needed (bold, italic, links?)

**No CSRF Protection:**
- Risk: API endpoints accept mutations (POST/PUT/DELETE) without CSRF tokens
- Files: `backend/src/main/scala/wp40k/http/routes/ArmyRoutesTapir.scala`, `AuthRoutesTapir.scala`
- Current mitigation: Credentials-less API calls from frontend (no cookies sent by default)
- Recommendations:
  - Implement SameSite=Strict on auth cookies when moving to cookie-based auth
  - Consider adding CSRF token headers for POST/PUT/DELETE endpoints

**Input Validation Incomplete:**
- Risk: User input (usernames, passwords) not validated client-side before API submission
- Files: `frontend/src/pages/LoginPage.tsx`, `RegisterPage.tsx`
- Impact: Bad UX (wait for server error); potential for accidental invalid data submission
- Fix approach: Add client-side validation for username length, password complexity; validate before form submission

## Performance Bottlenecks

**Reference Data Cache TTL Too Short:**
- Problem: 5-minute cache TTL in `ReferenceDataRepository.scala` causes frequent database reloads
- Files: `backend/src/main/scala/wp40k/db/ReferenceDataRepository.scala` (line 19)
- Impact: Under heavy load, database queries spike every 5 minutes; reference data is static per release
- Improvement path:
  - Cache should invalidate on data reload only, not time-based
  - Implement notification system from data loader to invalidate cache
  - Or extend TTL to 1 hour for production (still updates same session)

**Multiple Fetches in Edit Mode:**
- Problem: `ArmyViewPage.tsx` fetches all edit-related data in parallel even in view-only mode
- Files: `frontend/src/pages/ArmyViewPage.tsx` (lines 178-186)
- Impact: Unnecessary network traffic and parsing for users just viewing armies; slower page load
- Improvement path: Lazy-load edit data only when edit mode is activated; use React Suspense for split loading

**No API Response Caching on Frontend:**
- Problem: Repeated navigation re-fetches all reference data from API
- Files: `frontend/src/api.ts` (cachedFetch function only caches within single fetch chain, not across page loads)
- Impact: Sluggish navigation between pages; loads same datasheet/ability lists multiple times
- Improvement path: Implement persistent cache with versioning; invalidate on data reload endpoint hit

**Missing Database Indexes:**
- Problem: Complex queries in `ReferenceDataRepository.scala` don't have documented index strategy
- Files: `backend/src/main/schema` (Schema.scala)
- Impact: Query performance degrades as data scales; JOIN operations are slow
- Fix approach: Profile queries; add indexes on foreign keys and frequently filtered columns

## Fragile Areas

**Parsing Logic Brittle:**
- Files: `backend/src/main/scala/wp40k/domain/models/CompositionLineParser.scala`, `LoadoutParser.scala`, `DatasheetParser.scala`
- Why fragile: CSV data format parsing assumes specific ordering/presence of columns; small data format changes break parsing
- Safe modification:
  - Add schema versioning to CSV files
  - Add validation that all expected columns exist before parsing
  - Add detailed error messages showing which line failed
- Test coverage: Parser tests exist but don't cover malformed/missing column scenarios

**State Synchronization Between Contexts:**
- Files: `frontend/src/context/AuthContext.tsx`, `ReferenceDataContext.tsx`, `CompactModeContext.tsx`
- Why fragile: No synchronization mechanism between localStorage and context state; window.storage events not subscribed to
- Safe modification:
  - Implement storage event listener for cross-tab sync
  - Add consistency check in context initialization
- Test coverage: No tests for multi-tab scenarios

**Session Token Expiration Not Handled:**
- Files: `backend/src/main/scala/wp40k/db/SessionRepository.scala`
- Why fragile: Token validity checked but no session cleanup of expired tokens; old sessions accumulate in database
- Safe modification:
  - Add periodic cleanup job for expired sessions
  - Add expiration timestamp validation in session verification
- Test coverage: No tests for expired session handling

**Army Unit Wargear Selection Persistence:**
- Files: `backend/src/main/scala/wp40k/db/ArmyRepository.scala` (wargear_selections table)
- Why fragile: Selection deletes and re-inserts could race; no transactional guarantee
- Safe modification:
  - Wrap unit and wargear selection updates in single transaction
  - Add test for concurrent updates
- Test coverage: Current tests don't verify transactional consistency

## Scaling Limits

**SQLite for Production:**
- Current capacity: Single writer at a time; file-based storage limits concurrent access
- Limit: ~100-200 concurrent users before write lock contention causes timeouts
- Scaling path:
  - Migrate to PostgreSQL for multi-writer support
  - Consider read replicas for reference data queries
  - Implement connection pooling (currently possible with doobie)

**In-Memory Session Cache:**
- Current capacity: All sessions stored in-memory in `ReferenceDataRepository` (line 20-21)
- Limit: Memory usage grows unbounded; sessions not persisted across server restarts
- Scaling path:
  - Move cache to Redis
  - Implement distributed cache invalidation
  - Add session timeout cleanup

**CSV Data Loading Blocking:**
- Files: `backend/src/main/scala/wp40k/db/DataLoader.scala`
- Problem: Data loading from CSV happens synchronously on startup; 332 lines of sequential parsing
- Impact: Server startup time increases linearly with data size; 10+ seconds for full dataset
- Improvement: Lazy-load data per faction on first request; cache aggressively

## Dependencies at Risk

**Outdated Doobie Version:**
- Risk: `doobie-core` at `1.0.0-RC6` is a release candidate, not stable
- Files: `backend/build.sbt` (line 29)
- Impact: RC versions may have bugs not caught by production users; migration to stable could introduce breaking changes
- Migration plan: Upgrade to 1.1.x when available; test migration with full integration suite

**jBcrypt for Password Hashing:**
- Risk: jBcrypt is mature but slower than modern alternatives like Argon2; could be performance bottleneck under load
- Files: `backend/src/main/scala/wp40k/auth/PasswordHasher.scala`
- Impact: Login/register endpoints could become bottleneck with many simultaneous auth attempts
- Migration plan: Evaluate Argon2 integration; benchmark password hashing latency under load

**SQLite JDBC Version:**
- Risk: SQLite JDBC 3.44.1.0 is recent but SQLite 3.x versions can have compatibility issues
- Files: `backend/build.sbt` (line 30)
- Current mitigation: CI tests should catch incompatibilities
- Recommendations: Keep dependency updated; document minimum SQLite version

## Missing Critical Features

**No Audit Logging:**
- Problem: User actions (army creation, modification) not logged for compliance
- Blocks: Cannot track who modified army when; no accountability for data changes
- Implementation: Add audit table; log user_id, action, timestamp, affected_resource to all mutations

**No Data Export:**
- Problem: Users cannot export their armies for backup or external tools
- Blocks: Users locked into platform; no data portability
- Implementation: Add export endpoint returning JSON; consider CSV export for armies

**No Army Templates:**
- Problem: Users must manually recreate armies with same composition
- Blocks: New users without preset armies start from scratch every time
- Implementation: Create shared template system; allow users to mark armies as templates

**No Offline Support:**
- Problem: App requires constant internet; no caching strategy for offline browsing
- Blocks: Field use cases where data must be available without connectivity
- Implementation: Service worker + local storage; sync data when online

## Test Coverage Gaps

**Frontend Integration Tests Missing:**
- What's not tested: Multi-step workflows like army creation → modification → validation
- Files: `frontend/tests/e2e/` exists but e2e tests are limited
- Risk: Breaking changes in API contract discovered in production, not by CI
- Priority: High (integration tests catch more real-world bugs than unit tests)

**Backend Validation Error Cases:**
- What's not tested: All validation error types; edge cases like simultaneous warlord + enhancement updates
- Files: `backend/src/test/scala/wp40k/domain/army/ArmyValidatorSpec.scala`
- Risk: Validation logic fails silently on edge cases; users get wrong validation errors
- Priority: Medium (currently core functionality tested, edges missing)

**Error Boundary Testing:**
- What's not tested: Frontend error boundaries; what happens when API is down
- Files: `frontend/src/` (no error boundary component found)
- Risk: Unhandled promise rejections crash app without user-facing error message
- Priority: High (error recovery is critical UX)

**Race Condition Testing:**
- What's not tested: Rapid successive updates to army while validation is in-flight
- Files: `frontend/src/pages/ArmyViewPage.tsx` (validation debounce tested but not race during save)
- Risk: Race condition causes stale updates to overwrite newer ones
- Priority: Medium (happens in edge cases but breaks data consistency)

**Session Handling Tests:**
- What's not tested: Token expiration; cross-tab logout; session cleanup
- Files: `backend/src/test/scala/wp40k/db/SessionRepositorySpec.scala`
- Risk: Expired sessions not cleaned up; users can access deleted accounts
- Priority: High (security issue)

---

*Concerns audit: 2026-03-19*
