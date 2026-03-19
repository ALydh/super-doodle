# Coding Conventions

**Analysis Date:** 2026-03-19

## Naming Patterns

**Files:**
- Components: PascalCase with `.tsx` extension. Example: `SpotlightSearch.tsx`, `BattleUnitCard.tsx`
- Utilities/helpers: camelCase with `.ts` extension. Example: `sanitize.ts`, `api.ts`
- CSS modules: camelCase with `.module.css` suffix. Example: `SpotlightSearch.module.css`
- Scala files: PascalCase matching the primary class/object. Example: `DatasheetParser.scala`, `ArmyValidator.scala`
- Test files (Scala): PascalCase ending with `Spec`. Example: `DatasheetParserSpec.scala`, `ArmyValidatorSpec.scala`

**Functions/Methods:**
- Frontend (TypeScript): camelCase for all functions. Example: `cachedFetch`, `fuzzyScore`, `sanitizeHtml`
- Hooks: camelCase with `use` prefix. Example: `useFocusTrap`, `useAuth`, `useCompactMode`
- Scala: camelCase for methods, PascalCase for companion object methods that act as factories. Example: `parseLineWithContext`, `DatasheetId.parse`
- Event handlers: camelCase with action-based names. Example: `handleKeyDown`, `onClose`, `onSearchClick`

**Variables:**
- Frontend: camelCase. Example: `authToken`, `spotlightOpen`, `maxAttemptsPerSection`
- Constants: UPPER_SNAKE_CASE or inline with descriptive names. Example: `CACHE_TTL_MS`, `TOKEN_KEY`, `MAX_RESULTS_PER_SECTION`
- Scala opaque types: PascalCase. Example: `DatasheetId`, `FactionId`, `DetachmentId`

**Types:**
- Interfaces/types: PascalCase. Example: `AuthContextType`, `SpotlightSearchProps`, `ResultItem`, `CacheEntry`
- Discriminated unions use `type` field. Example: `{ type: "navigate"; ... } | { type: "expand"; ... }`

## Code Style

**Formatting:**
- Frontend: ESLint with TypeScript support (see `eslint.config.js`)
- Backend: Scala, formatted with scalafmt (disabled in `build.sbt` comment)
- Indentation: 2 spaces (frontend), convention follows Scala defaults (backend)
- Max line length: No strict limit observed, but 80-100 characters preferred for readability

**Linting:**
- Frontend: ESLint with plugins: `@eslint/js`, `typescript-eslint`, `react-hooks`, `react-refresh`
- Rules: TypeScript strict checking enabled, React hooks dependencies validated
- No `any` types allowed in TypeScript code
- React refresh warnings for exported components

**Import organization:**
- Frontend imports ordered by: Node modules → internal utilities → types → relative imports
- Example (from `SpotlightSearch.tsx`):
  ```typescript
  import { useCallback, useEffect, useMemo, useRef, useState } from "react";
  import { useNavigate } from "react-router-dom";
  import type { Faction, Datasheet, ... } from "../types";
  import { fetchFactions, ... } from "../api";
  import { useAuth } from "../context/useAuth";
  import { glossarySections } from "../data/glossary";
  import { sanitizeHtml } from "../sanitize";
  import { useFocusTrap } from "../hooks/useFocusTrap";
  import styles from "./SpotlightSearch.module.css";
  ```
- Backend Scala uses qualified imports with `import package.path._` for entire packages

**Path Aliases:**
- Frontend uses relative imports (`../`) - no alias configuration detected

## Error Handling

**Frontend patterns:**
- Try-catch for async operations in event handlers and useEffect
- Thrown errors wrapped with context: `throw new Error(err.error || 'Default message')`
- API responses checked with `if (!res.ok)` pattern
- Example (from `api.ts`):
  ```typescript
  if (!res.ok) {
    const err = await res.json();
    throw new Error(err.error || `Login failed: ${res.status}`);
  }
  return res.json();
  ```

**Backend patterns (Scala):**
- Sealed traits for ADT-based error handling. Example: `ParseError` sealed trait
- `Either[ParseError, T]` return type for parser functions
- `IO.raiseError()` for effect-based error channels (cats-effect)
- Custom error subtypes for specific failures: `InvalidFormat`, `MissingField`, `InvalidId`
- Error formatting with context: `ParseError.formatError()` provides file, line, field context
- Validation errors as `List[ValidationError]` return type, enabling multiple error collection

## Logging

**Framework:**
- Frontend: `console.log()`, `console.error()` used implicitly through browser console
- Backend: `log4cats-slf4j` with logback-classic configuration
- JSON logging available via `logstash-logback-encoder` (configured but usage not sampled)

**Patterns:**
- Frontend: No verbose logging in sampled code; errors logged via thrown exceptions
- Backend: Structured logging through log4cats (SLF4J bridge)

## Comments

**When to Comment:**
- Code should be self-documenting; comments reserved for non-obvious logic
- Example: `fuzzyScore()` function in `SpotlightSearch.tsx` has detailed comment explaining scoring algorithm because it's non-trivial
- HTML/text content explanations included as comments only when genuinely surprising

**JSDoc/TSDoc:**
- Not observed in sampled code
- Function signatures are explicit and clear; complex returns documented in comments only

## Function Design

**Size:**
- Functions typically 5-40 lines in observed code
- Longer functions split into smaller helpers with descriptive names
- Example: `ArmyValidator.validate()` at 40 lines calls 12 private validation functions

**Parameters:**
- Explicit destructuring of props in React components. Example: `function SpotlightSearch({ open, onClose }: SpotlightSearchProps)`
- Objects preferred over multiple parameters for related config. Example: `RateLimitConfig(maxAttempts, windowSeconds)`
- Type annotations required at API boundaries; inferred in internal code

**Return Values:**
- Explicit return types on all public functions (TypeScript) and public methods (Scala)
- Early returns for guard conditions preferred. Example: `if (!existing) return ...`
- Scala uses `Either[Error, T]` for error-prone operations; `IO[T]` for effectful operations

## Module Design

**Exports:**
- Frontend: Named exports for components and utilities; default export avoided
- Example: `export function SpotlightSearch(...)` not `export default SpotlightSearch`
- Contexts export both provider and hook: `AuthContext` + `useAuth()`

**Barrel Files:**
- Not observed in this codebase
- Each module imports directly from source file location

## Type Patterns

**Discriminated unions (Frontend):**
```typescript
type ResultItem =
  | { type: "navigate"; name: string; subtitle?: string; action: () => void }
  | { type: "expand"; name: string; subtitle?: string; description: string };
```

**Opaque types (Backend):**
```scala
opaque type DatasheetId = String
object DatasheetId {
  def apply(id: String): DatasheetId = id
  def value(id: DatasheetId): String = id
  def parse(id: String): Either[ParseError, DatasheetId] = ...
}
```

**Case classes (Backend):**
- Immutable data models with derived equality/hashing. Example: `case class Datasheet(...)`
- Records destructured in pattern matching for validation logic

---

*Convention analysis: 2026-03-19*
