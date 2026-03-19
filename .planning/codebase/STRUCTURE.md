# Codebase Structure

**Analysis Date:** 2026-03-19

## Directory Layout

```
super-doodle/
├── backend/                         # Scala/HTTP4S API server
│   ├── src/main/scala/wp40k/
│   │   ├── Main.scala               # Entry point; database initialization
│   │   ├── domain/                  # Domain logic (pure, no side effects)
│   │   │   ├── army/                # Army validation and composition models
│   │   │   ├── auth/                # Authentication types and logic
│   │   │   ├── models/              # Game domain types (Datasheet, Faction, etc.)
│   │   │   └── types/               # Value types (DatasheetId, FactionId, enums)
│   │   ├── db/                      # Database access layer
│   │   │   ├── Database.scala       # Transactor configuration
│   │   │   ├── Schema.scala         # Table definitions
│   │   │   ├── ArmyRepository.scala # Army CRUD operations
│   │   │   ├── ReferenceDataRepository.scala  # Read-only game data queries
│   │   │   └── [other repositories]
│   │   ├── http/                    # HTTP layer
│   │   │   ├── HttpServer.scala     # Server setup, middleware composition
│   │   │   ├── endpoints/           # Tapir endpoint definitions
│   │   │   ├── routes/              # Route implementations (Tapir interpreters)
│   │   │   ├── CirceCodecs.scala    # JSON encoder/decoder instances
│   │   │   ├── dto/                 # Data transfer objects (HTTP request/response)
│   │   │   ├── AuthMiddleware.scala # Token validation
│   │   │   └── InputValidation.scala # Request sanitization
│   │   ├── csv/                     # CSV parsing for game data import
│   │   ├── auth/                    # Authentication utilities
│   │   ├── mcp/                     # Claude MCP server (tool definitions)
│   │   └── errors/                  # Custom error types
│   ├── src/test/scala/              # ScalaTest tests (mirror structure)
│   ├── build.sbt                    # sbt build configuration
│   └── Makefile                     # Build targets (test, lint, run)
│
├── frontend/                        # React/TypeScript UI
│   ├── src/
│   │   ├── main.tsx                 # React entry point
│   │   ├── App.tsx                  # Root router component
│   │   ├── api.ts                   # HTTP client with reference data cache
│   │   ├── types.ts                 # TypeScript type definitions
│   │   ├── pages/                   # Page-level components
│   │   │   ├── ArmyBuilderPage.tsx  # Army building interface
│   │   │   ├── ArmyViewPage.tsx     # Army view/edit page
│   │   │   ├── FactionDetailPage.tsx# Browse faction datasheets
│   │   │   ├── FactionListPage.tsx  # Browse all factions
│   │   │   ├── InventoryPage.tsx    # User model/unit collection
│   │   │   ├── LoginPage.tsx        # Authentication form
│   │   │   └── [other pages]
│   │   ├── components/              # Reusable UI components
│   │   │   ├── Header.tsx           # Top navigation
│   │   │   ├── UnitCardDetail.tsx   # Datasheet display
│   │   │   ├── WargearSelector.tsx  # Wargear option selection UI
│   │   │   ├── EnhancementSelector.tsx
│   │   │   ├── ExpandableUnitCard.tsx
│   │   │   ├── SpotlightSearch.tsx  # Cmd+K search modal
│   │   │   └── [other components]
│   │   ├── context/                 # React context providers
│   │   │   ├── AuthContext.tsx      # Authentication state
│   │   │   └── CompactModeContext.tsx # Mobile UI toggle
│   │   ├── hooks/                   # Custom React hooks
│   │   └── data/                    # Static data (enums, constants)
│   ├── vite.config.ts               # Vite build config
│   ├── tsconfig.json                # TypeScript config (strict mode)
│   └── package.json                 # npm dependencies
│
├── data/                            # Game data files
│   └── wp40k/                       # Warhammer 40K CSV exports
│       └── [faction CSV files]
│
├── scripts/                         # Utility scripts
│   └── fetch-wp40k.sh               # Download latest game data
│
├── Makefile                         # Top-level dev commands
└── README.md                        # Project overview
```

## Directory Purposes

**backend/src/main/scala/wp40k/domain/**
- Purpose: Pure domain logic without side effects
- Contains: Army validation rules, game rule enumerations, type-safe IDs
- Key files: `ArmyValidator.scala`, `Army.scala`, `AllyRules.scala`

**backend/src/main/scala/wp40k/db/**
- Purpose: SQLite database access via Doobie
- Contains: Schema definitions, CRUD repositories, transactor configuration
- Key files: `Schema.scala`, `ArmyRepository.scala`, `ReferenceDataRepository.scala`, `Database.scala`

**backend/src/main/scala/wp40k/http/**
- Purpose: HTTP API layer with Tapir + HTTP4S
- Contains: Endpoint declarations, route implementations, middleware, JSON codecs
- Key files: `HttpServer.scala`, `endpoints/*.scala`, `routes/*.scala`

**frontend/src/pages/**
- Purpose: Page-level components mapped to routes
- Contains: Full page layouts with data fetching and complex state
- Key files: `ArmyBuilderPage.tsx`, `ArmyViewPage.tsx`, `FactionDetailPage.tsx`

**frontend/src/components/**
- Purpose: Reusable UI components
- Contains: Cards, selectors, modals, forms
- Key files: `WargearSelector.tsx`, `ExpandableUnitCard.tsx`, `SpotlightSearch.tsx`

**frontend/src/context/**
- Purpose: Global state providers
- Contains: Authentication context, UI mode toggle
- Key files: `AuthContext.tsx`

## Key File Locations

**Entry Points:**
- `backend/src/main/scala/wp40k/Main.scala`: JVM startup; reads config, initializes schema, starts server
- `frontend/src/main.tsx`: React DOM mount point
- `frontend/src/App.tsx`: Root router component

**Configuration:**
- `backend/build.sbt`: Scala dependencies (cats-effect, http4s, doobie, tapir)
- `frontend/vite.config.ts`: Vite bundler config with API proxy to localhost:8080
- `frontend/tsconfig.json`: TypeScript strict mode, module resolution

**Core Logic:**
- `backend/src/main/scala/wp40k/domain/army/ArmyValidator.scala`: 13 validation rules
- `backend/src/main/scala/wp40k/db/ArmyRepository.scala`: Army persistence
- `backend/src/main/scala/wp40k/http/HttpServer.scala`: Route composition
- `frontend/src/api.ts`: HTTP client with reference data caching
- `frontend/src/pages/ArmyBuilderPage.tsx`: Army building UI logic

**Testing:**
- `backend/src/test/scala/`: Mirror structure with test implementations
- `frontend/`: Jest tests alongside source files (*.test.tsx)

## Naming Conventions

**Files:**
- Scala: PascalCase for files (ArmyValidator.scala, Main.scala)
- TypeScript/React: PascalCase for components (ArmyBuilderPage.tsx), camelCase for utils (api.ts, types.ts)
- CSS Modules: PascalCase matching component (UnitCardDetail.module.css)

**Directories:**
- Scala packages: kebab-case in filesystem but camelCase in package declarations (db/ contains package wp40k.db)
- React: camelCase (pages/, components/, context/)

**Functions:**
- Scala: camelCase (createArmy, validateArmy, listSummaries)
- TypeScript: camelCase (fetchArmy, validateArmy)

**Types:**
- Scala: PascalCase for case classes and sealed traits (Army, ValidationError, ReferenceData)
- TypeScript: PascalCase for interfaces and types (Army, ValidationError, Datasheet)

**IDs:**
- Type-safe wrappers: DatasheetId, FactionId, UserId, EnhancementId, DetachmentId
- Implemented as opaque types in Scala (`type DatasheetId = String` with a companion `object DatasheetId`)

## Where to Add New Code

**New Feature (e.g., stratagem filtering):**
- Primary code: `backend/src/main/scala/wp40k/domain/` for logic, `backend/src/main/scala/wp40k/db/` for queries
- API layer: `backend/src/main/scala/wp40k/http/endpoints/` for endpoint, `backend/src/main/scala/wp40k/http/routes/` for route
- Frontend: `frontend/src/pages/` if full page, `frontend/src/components/` if reusable component
- Tests: `backend/src/test/scala/wp40k/domain/` or `backend/src/test/scala/wp40k/db/` (mirror structure)

**New Component (e.g., StrategySelector):**
- Implementation: `frontend/src/components/StrategySelector.tsx`
- Styles: `frontend/src/components/StrategySelector.module.css`
- Tests: `frontend/src/components/StrategySelector.test.tsx`
- Export from: `frontend/src/components/` (no index.ts barrel file currently used)

**New Database Table:**
- Schema: Add fragment to appropriate list in `backend/src/main/scala/wp40k/db/Schema.scala` (userTables or refTables)
- Repository: Create `backend/src/main/scala/wp40k/db/NewRepository.scala`
- Call from: `backend/src/main/scala/wp40k/db/DataLoader.scala` or Main

**Utilities:**
- Shared backend helpers: `backend/src/main/scala/wp40k/domain/` or new module at `backend/src/main/scala/wp40k/util/`
- Shared frontend helpers: `frontend/src/` directly (constants.ts, chapters.ts, colors.css)

## Special Directories

**backend/src/main/resources/:**
- Purpose: Static assets and configuration
- Generated: META-INF/native-image/ (GraalVM native image config)
- Committed: Yes

**data/wp40k/:**
- Purpose: Game data CSV files (Warhammer 40K reference)
- Generated: No (committed manually or via fetch-wp40k.sh)
- Committed: Not in git (.gitignore excludes)

**frontend/dist/:**
- Purpose: Built production assets
- Generated: Yes (by vite build)
- Committed: No (.gitignore excludes)

**backend/target/**
- Purpose: Compiled Scala bytecode and jars
- Generated: Yes (by sbt)
- Committed: No (.gitignore excludes)

**frontend/node_modules/:**
- Purpose: npm package dependencies
- Generated: Yes (by npm install)
- Committed: No (.gitignore excludes)

---

*Structure analysis: 2026-03-19*
