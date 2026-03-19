# Testing Patterns

**Analysis Date:** 2026-03-19

## Test Framework

**Runner (Frontend):**
- Playwright for E2E tests
- Config: `playwright.config.ts`
- No unit test framework detected (Jest/Vitest not installed)

**Runner (Backend):**
- ScalaTest 3.2.19
- FlatSpec style (recommended for domain testing)
- Config: `build.sbt` includes ScalaTest as test dependency

**Assertion Library:**
- Frontend: Playwright assertions (`.toBeVisible()`, `.toHaveURL()`, etc.)
- Backend: ScalaTest Matchers DSL (`.should` syntax)

**Run Commands:**
```bash
# Frontend E2E
npm run test:e2e              # Run all Playwright tests
npm run test:e2e:ui          # Playwright UI mode

# Backend
sbt test                       # Run all ScalaTest tests
sbt test -- -z "pattern"      # Run tests matching pattern
```

## Test File Organization

**Frontend:**
- Location: `tests/e2e/` directory (separate from source)
- Naming: `*.spec.ts` suffix. Example: `navigation.spec.ts`, `army-crud.spec.ts`
- One spec file per feature/flow
- Structure: Multiple test cases per file, grouped by feature

**Backend:**
- Location: `src/test/scala/wp40k/` (mirrors source structure)
- Naming: `*Spec.scala` suffix. Example: `DatasheetParserSpec.scala`, `ArmyValidatorSpec.scala`
- Package structure matches source: `wp40k.domain.army` → `wp40k.domain.army` (test)
- One spec file per module/class

## Test Structure

**Frontend (Playwright):**
```typescript
import { test, expect } from '@playwright/test';

test('full navigation flow: factions -> faction -> unit -> back -> back', async ({ page }) => {
  await page.goto('/');
  await expect(page.locator('.faction-list')).toBeVisible();

  await page.getByRole('link', { name: 'Necrons' }).click();
  await expect(page).toHaveURL(/\/factions\/NEC$/);
  await expect(page.locator('.datasheet-list').first()).toBeVisible();

  // ... more steps ...
});
```

**Backend (ScalaTest FlatSpec):**
```scala
class DatasheetParserSpec extends AnyFlatSpec with Matchers with EitherValues {
  "parseLine" should "parse complete datasheet with all fields" in {
    val line = "000000884|Venerable Land Raider|AC|..."
    val result = DatasheetParser.parseLine(line)

    result.value.id shouldBe DatasheetId("000000884")
    result.value.name shouldBe "Venerable Land Raider"
    result.value.factionId shouldBe Some(FactionId("AC"))
  }

  it should "parse datasheet with minimal fields" in {
    val line = "000000882|Custodian Guard|..."
    val result = DatasheetParser.parseLine(line)

    result.value.legend shouldBe Some("Legend text")
  }
}
```

**Patterns:**
- Setup: Test data declared as vals (backend) or as params in test fixture (frontend)
- Teardown: Implicit via scope; playwright auto-closes browser, ScalaTest cleans up after each test
- Assertion: Backend uses `shouldBe` infix operator; Frontend uses `expect().toXxx()` chain

## Mocking

**Frontend:**
- No mocking framework detected (Jest/Vitest mocks unavailable)
- Integration tests hit real backend (configured in `playwright.config.ts`)
- Backend server started automatically via:
  ```typescript
  webServer: [
    {
      command: 'cd ../backend && sbt run',
      port: 8080,
      reuseExistingServer: !process.env.CI,
      timeout: 120000,
    },
    {
      command: 'npm run dev',
      port: 5173,
      reuseExistingServer: !process.env.CI,
    },
  ]
  ```

**Backend:**
- No mocking framework detected
- Tests work with real data structures and pure functions
- Parser tests feed raw CSV strings and assert on parsed objects
- Validator tests build complete `ReferenceData` fixtures with all required data

**What to Mock:**
- Frontend: Not applicable (no mocking framework)
- Backend: Not applicable (pure functions preferred)

**What NOT to Mock:**
- Business logic (validators, parsers) - test with real data structures
- Database queries (in integration tests) - use fixtures with test data

## Fixtures and Factories

**Test Data (Backend):**
```scala
val orkFaction: FactionId = FactionId("Ork")
val warbossId: DatasheetId = DatasheetId("000000001")
val warbossDs: Datasheet = Datasheet(
  warbossId, "Warboss", Some(orkFaction), None, None,
  Some(Role.Characters), None, None, false, None, None, None, None, ""
)
val allDatasheets: List[Datasheet] = List(
  warbossDs, boyzDs, trukDs, meganobzDs, ghazDs, painboyDs, smCaptainDs,
  knightErrantDs, armigerWarglaiveDs, bloodlettersDs, coLeaderDs
)

def baseRef: ReferenceData = ReferenceData(
  allDatasheets, orkKeywords, unitCosts, enhancements, leaderMappings, detachmentAbilities
)
```

**Location:**
- Backend: Defined inline in spec files as val declarations (example: `ArmyValidatorSpec.scala`)
- Frontend: Page objects built on-the-fly via Playwright selectors

## Coverage

**Requirements:** No coverage tool detected or enforced

**View Coverage (Backend):**
- ScalaTest HTML reports can be generated but not configured by default
- Run: `sbt test` produces test output; coverage tools not integrated

## Test Types

**Unit Tests (Frontend):**
- Not implemented (no Jest/Vitest)
- E2E tests serve as primary coverage strategy

**Unit Tests (Backend):**
- Parser tests: `DatasheetParserSpec.scala`, `CsvParsingSpec.scala`
  - Input: Raw CSV strings or primitive values
  - Output: Parsed domain objects or `Either[ParseError, T]`
  - Scope: Single function in isolation
  - Example: CSV line → Datasheet, with assertions on all fields

- Type parser tests: `RoleSpec.scala`, `SourceTypeSpec.scala`
  - Input: String values
  - Output: `Either[ParseError, Type]`
  - Scope: Type validation and parsing

- Domain logic tests: `ArmyValidatorSpec.scala`, `WargearFilterSpec.scala`
  - Input: Complete domain objects (Army, ReferenceData)
  - Output: List[ValidationError] or filtered wargear lists
  - Scope: Business rules and constraints

**Integration Tests (Backend):**
- Database tests: `DatabaseSpec.scala`, `ArmyRepositorySpec.scala`
  - Setup: Creates in-memory SQLite database
  - Tests: CRUD operations on database schema
  - Teardown: Implicit via scope

- Full roundtrip tests: Not observed in samples

**E2E Tests (Frontend):**
- Navigation flow: `navigation.spec.ts`
  - Tests: Multi-page navigation, back button, URL matching
  - Flow: Home → Faction list → Unit detail → Back → Home

- Feature workflows: `army-crud.spec.ts`, `army-validation.spec.ts`, `datasheet-detail.spec.ts`
  - Tests: User workflows (create, edit, validate army)
  - Pages hit: Multiple routes, forms, validation messaging
  - Assertions: URL checks, element visibility, form state

## Common Patterns

**Async Testing (Backend with cats-effect):**
```scala
val limiter = RateLimiter.create(RateLimitConfig(maxAttempts = 3, windowSeconds = 60)).unsafeRunSync()
limiter.isAllowed("user1").unsafeRunSync() shouldBe true
```
- Effects run synchronously in tests via `unsafeRunSync()`
- No mocking of effects; real IO operations executed

**Error Testing (Scala):**
```scala
// Using EitherValues mixin
it should "reject invalid datasheet ID" in {
  val line = "INVALID|Test|AC|..."
  val result = DatasheetParser.parseLine(line)
  result.left.value shouldBe a [InvalidId]
}

// Alternative: pattern matching
result match {
  case Left(error) => error shouldBe a [InvalidId]
  case Right(_) => fail("Expected parse error")
}
```

**Playwright assertions:**
```typescript
await expect(page.locator('.faction-list')).toBeVisible();
await expect(page).toHaveURL(/\/factions\/NEC$/);
await expect(page.getByRole('heading')).toContainText('Units');
```

## Test Organization (Backend)

**Structure pattern for validator tests:**
1. Define test data (IDs, domain objects, references)
2. Define helper methods to build complete fixtures
3. Test each validation rule separately with minimal setup
4. Reuse shared fixture builders

Example from `ArmyValidatorSpec.scala`:
```scala
class ArmyValidatorSpec extends AnyFlatSpec with Matchers {
  val orkFaction: FactionId = FactionId("Ork")
  // ... 50+ lines of val declarations for test data ...

  def baseRef: ReferenceData = ReferenceData(...)

  "validate" should "accept valid army" in {
    // Build minimal army that passes baseRef validation
    val army = Army(orkFaction, BattleSize.StrikeForce, ...)
    val errors = ArmyValidator.validate(army, baseRef)
    errors shouldBe empty
  }
}
```

---

*Testing analysis: 2026-03-19*
