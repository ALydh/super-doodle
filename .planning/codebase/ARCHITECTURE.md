# Architecture

**Analysis Date:** 2026-03-19

## Pattern Overview

**Overall:** Full-stack client-server architecture with distinct domain, HTTP, and database layers on the backend, and React-based component hierarchy with context-driven state on the frontend.

**Key Characteristics:**
- Clear separation between domain logic (army validation) and infrastructure (HTTP, database)
- Tagless final pattern for effect management in Scala using cats-effect
- Server-centric validation with client-side caching for reference data
- Split database capability for reference data (immutable game rules) and user data (armies, authentication)
- Functional composition with minimal boilerplate

## Layers

**Domain Layer:**
- Purpose: Core business logic for army building and validation
- Location: `backend/src/main/scala/wp40k/domain/`
- Contains: Army composition models, validation rules, type definitions, role enums
- Depends on: Nothing (pure domain)
- Used by: HTTP routes, database repositories

**Data Layer:**
- Purpose: Database access and schema management
- Location: `backend/src/main/scala/wp40k/db/`
- Contains: Repositories (ArmyRepository, UserRepository, ReferenceDataRepository), schema definitions, Doobie transactors
- Depends on: Domain models, Doobie, SQLite JDBC driver
- Used by: HTTP routes via dependency injection

**HTTP Layer:**
- Purpose: API endpoints, request validation, security middleware
- Location: `backend/src/main/scala/wp40k/http/`
- Contains: Tapir endpoint definitions, routes, authentication middleware, input validation
- Depends on: Domain, data layer, Tapir, HTTP4S, Circe
- Used by: HttpServer entry point

**Frontend Domain:**
- Purpose: React components, state management, type-safe API consumption
- Location: `frontend/src/`
- Contains: Page components, context providers, hooks, API client, type definitions
- Depends on: React, React Router, Fetch API
- Used by: App root and router

## Data Flow

**Army Building Flow:**

1. User navigates to `/factions/:factionId/armies/new` (protected route)
2. `ArmyBuilderPage` fetches detachments, enhancements, datasheets, stratagem via `api.ts`
3. Frontend caches reference data in memory (5-minute TTL in `referenceCache`)
4. User selects units via `UnitPicker`, modifies wargear, adds enhancements
5. On every change, `ArmyBuilderPage` calls `validateArmy()` which POSTs to `/api/armies/validate`
6. Backend `ArmyRoutesTapir.validateArmyRoute` invokes `ArmyValidator.validate()` with ReferenceData
7. Validation indexes datasheets, keywords, costs in-memory and runs 13 distinct validation functions
8. Errors returned as JSON with discriminated error types (FactionMismatch, PointsExceeded, etc.)
9. Frontend renders `ValidationErrors` component displaying constraint violations

**Army Persistence Flow:**

1. User clicks "Save Army" in `ArmyBuilderPage`
2. POST to `/api/armies` with `CreateArmyRequest` (name + Army object)
3. `ArmyRoutesTapir.createArmyRoute` validates name via `InputValidation`
4. Generates UUID for army ID
5. `ArmyRepository.create()` inserts into `armies` table with optional `owner_id`
6. For each `ArmyUnit`, inserts into `army_units` table
7. For wargear selections, inserts into `army_unit_wargear_selections` table
8. Returns `PersistedArmy` JSON with generated ID
9. Frontend navigates to `/armies/:armyId` to view persisted army

**Viewing Public Army Flow:**

1. User visits `/armies/:armyId` (public route, no auth required)
2. `ArmyViewPage` fetches army via `fetchArmy(armyId)`
3. If `owner_id` is NULL, display as public read-only
4. If user is logged in and matches `owner_id`, enable edit button
5. Edit navigates to same page in edit mode via `ArmyViewPage` state

**State Management:**
- Authentication: `AuthContext` persists token to localStorage, manages login/logout
- Global Reference Data: Cached in-memory in `api.ts` with 5-minute TTL
- Builder State: Local component state in `ArmyBuilderPage` (units, detachment, etc.)
- Compact Mode: `CompactModeContext` provides mobile UI toggle globally

## Key Abstractions

**Army (Domain):**
- Purpose: Represents a complete army composition with units, enhancements, selections
- Examples: `backend/src/main/scala/wp40k/domain/army/Army.scala`
- Pattern: Case class with nested ArmyUnit and WargearSelection types; immutable

**ValidationError (ADT):**
- Purpose: Typed error responses from army validation
- Examples: `FactionMismatch`, `PointsExceeded`, `InvalidWarlord`, `DuplicateEnhancement`
- Pattern: Sealed trait with case class subtypes; enables exhaustive pattern matching

**Datasheet (Domain Model):**
- Purpose: Single unit profile with stats, keywords, abilities
- Examples: `backend/src/main/scala/wp40k/domain/models/Datasheet.scala`
- Pattern: Case class parsed from CSV with optional fields (leaderHead, leaderFooter, etc.)

**Repository Pattern:**
- Purpose: Isolate database access behind functional interfaces
- Examples: `ArmyRepository.scala`, `ReferenceDataRepository.scala`
- Pattern: Object with static methods returning `IO[T]` (Doobie ConnectionIO lifted to IO)

**Endpoint (Tapir):**
- Purpose: Declarative API endpoint specification
- Examples: `ArmyEndpoints.scala` defines listArmies, getArmy, createArmy, etc.
- Pattern: Composable DSL for path, method, input, output, error types; routes are interpreters

## Entry Points

**Backend:**
- Location: `backend/src/main/scala/wp40k/Main.scala`
- Triggers: JVM startup (via sbt run)
- Responsibilities: Parse config, initialize databases, start HTTP server on :8080

**Frontend:**
- Location: `frontend/src/main.tsx`
- Triggers: Browser loads index.html
- Responsibilities: Mount React App to DOM

**HTTP Server:**
- Location: `backend/src/main/scala/wp40k/http/HttpServer.scala`
- Triggers: Called from Main after schema initialization
- Responsibilities: Compose all routes, add middleware (CORS, logging), bind to port 8080

## Error Handling

**Strategy:** Typed ADTs on domain boundary; IO error channels for infrastructure failures

**Patterns:**
- Domain validation uses sealed traits (`ValidationError`) with pattern matching on routes
- Database errors (constraint violations, connection failures) propagate via IO
- HTTP errors encoded as statusCode + JSON body (400 for validation, 401/403 for auth, 404 for not found, 500 for server errors)
- Frontend wraps API calls in try/catch, displays error messages via `ErrorBoundary` or inline notifications

## Cross-Cutting Concerns

**Logging:** SLF4J via log4cats; request ID injected into MDC on each HTTP request with method, path, status, duration

**Validation:**
- Input: `InputValidation` object checks army name length
- Domain: `ArmyValidator` object runs 13 exhaustive rules on complete Army object with ReferenceData lookup tables
- HTTP: Tapir endpoint definitions enforce Content-Type and JSON schema

**Authentication:**
- Token-based: JWT-like bearer token stored in session table
- Middleware: `TapirSecurity` provides `required()` and `optional()` security logic for endpoints
- Protected Routes: Frontend `ProtectedRoute` wraps routes requiring auth, redirects to `/login` if no token
- Rate Limiting: `RateLimiter` prevents brute force on `/api/auth/login` (5 attempts per 60 seconds)

---

*Architecture analysis: 2026-03-19*
