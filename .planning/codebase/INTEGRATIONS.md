# External Integrations

**Analysis Date:** 2026-03-19

## APIs & External Services

**Model Context Protocol (MCP):**
- Service: Model Context Protocol server for LLM integrations
  - Endpoint: HTTP at `http://localhost:8080/api/mcp`
  - SDK/Client: `mcp-server` 0.3.3, `mcp-server-http4s` 0.3.3
  - Configuration: `.mcp.json` declares server location
  - Implementation: `backend/src/main/scala/wp40k/mcp/`

**Data Source (CSV):**
- Service: Warhammer 40K CSV files (loaded at startup from `data/wp40k/`)
- Parser: JSoup 1.17.2 for HTML parsing, custom parser combinators
- Loading: `DataLoader` component initializes reference database on startup
- Location: `backend/src/main/scala/wp40k/csv/`

## Data Storage

**Databases:**
- Type/Provider: SQLite (two-database system)
  - Reference DB: Read-only reference data (factions, datasheets, etc.)
    - Default: `wp40k-ref.db` (or `REF_DB_PATH` env var)
    - Client: Doobie 1.0.0-RC6
    - Mode: Read-only (`mode=ro` in JDBC URL)
  - User DB: Writable user and army data
    - Default: `wp40k-user.db` (or `USER_DB_PATH` env var)
    - Client: Doobie 1.0.0-RC6
    - Mode: Read-write with foreign key constraints enabled
    - Uses `ATTACH DATABASE` to access reference data
  - Connection: JDBC SQLite driver
  - Configuration: `backend/src/main/scala/wp40k/db/Database.scala`
  - Pragmas: WAL mode (write-ahead logging), memory-mapped I/O, cache optimization

**File Storage:**
- Local filesystem only (no cloud storage)
- Data files: `data/wp40k/` directory (CSV files for game data)
- Databases: SQLite files in working directory or env-configured paths

**Caching:**
- In-memory cache for reference data fetches (5-minute TTL)
- Implementation: `frontend/src/api.ts` caches faction, datasheet, and ability data
- Cache keys: `factions`, `datasheets`, `detachments`, `enhancements`, etc.

## Authentication & Identity

**Auth Provider:**
- Custom JWT-based authentication
  - Implementation: `backend/src/main/scala/wp40k/http/routes/AuthRoutesTapir.scala`
  - Token storage: Browser localStorage
  - Token format: Bearer token in `Authorization` header
  - Password hashing: bcrypt (jbcrypt 0.4)

**Endpoints:**
- POST `/api/auth/register` - Create account with optional invite code
- POST `/api/auth/login` - Authenticate and receive JWT token
- POST `/api/auth/logout` - Invalidate session
- GET `/api/auth/me` - Get current authenticated user

**Invite System:**
- POST `/api/invites` - Create invite code (authenticated users only)
- GET `/api/invites` - List user's invites
- Integration: Required for user registration via `inviteCode`

## Rate Limiting

**Login Rate Limiting:**
- Configuration: 5 attempts per 60 seconds
- Implementation: `backend/src/main/scala/wp40k/auth/RateLimiter.scala`
- Applies to: POST `/api/auth/login` endpoint

## Monitoring & Observability

**Error Tracking:**
- None detected - errors logged via Logback

**Logs:**
- Framework: Logback with Slf4j
  - Configuration: `backend/src/main/resources/logback.xml`
  - Output: Structured logging with logstash encoder
  - MDC (Mapped Diagnostic Context) tracking:
    - `request_id` - Unique per request
    - `method` - HTTP method
    - `path` - Request path
    - `status` - Response status code
    - `duration_ms` - Request duration

**Observability Stack (Optional - Deployed):**
- Loki 2.9.0 - Log aggregation
- Promtail 2.9.0 - Log collector from systemd journal
- Grafana 10.2.0 - Dashboards and visualization
- Configuration: `deploy/docker-compose.observability.yml`
- Available via `deploy/loki-config.yml` and `deploy/promtail-config.yml`

## CI/CD & Deployment

**Hosting:**
- Development: localhost (frontend on 5173, backend on 8080)
- Production: Runs as JAR or native image on port 8080
- Docker support: Deployable via containers (orchestration stack available)

**CI Pipeline:**
- GitHub Actions workflows:
  - `backend-ci.yml` - Backend compile/test on push
  - `frontend-ci.yml` - Frontend linting/tests on push
  - `deploy.yml` - Production deployment pipeline

**Build Outputs:**
- Frontend: Vite bundle to `dist/` directory
- Backend: JAR assembly (`sbt assembly`) or native image (`sbt nativeImage`)

## Environment Configuration

**Required env vars:**
- `REF_DB_PATH` - Reference database path (optional, defaults to `wp40k-ref.db`)
- `USER_DB_PATH` - User database path (optional, defaults to `wp40k-user.db`)
- `GRAALVM_HOME` - GraalVM home for native compilation (optional, defaults to `/usr/lib/jvm/graalvm`)

**Optional (for observability stack):**
- `GRAFANA_ADMIN_PASSWORD` - Grafana admin password (default: `admin`)

**Secrets location:**
- Not currently detected in codebase
- Frontend auth token: stored in browser localStorage
- Database connections: file-based (no remote credentials needed)

## Webhooks & Callbacks

**Incoming:**
- None detected

**Outgoing:**
- None detected

## API Specification

**API Server:**
- Base URL: `http://localhost:8080` (production) or proxied via Vite
- Port: 8080
- CORS: Enabled for all origins (`withAllowOriginAll`)
- Swagger UI: Available via Tapir integration
- API Endpoints:
  - Authentication: `/api/auth/*`
  - Factions: `/api/factions/*`
  - Datasheets: `/api/datasheets/*`
  - Armies: `/api/armies/*`
  - Detachments: `/api/detachments/*`
  - Weapon abilities: `/api/weapon-abilities`
  - Core abilities: `/api/core-abilities`
  - Stratagems: `/api/stratagems`
  - Enhancements: `/api/enhancements`
  - Invites: `/api/invites`
  - Inventory: `/api/inventory`
  - MCP: `/api/mcp` (Model Context Protocol)
  - Health: `/health`

---

*Integration audit: 2026-03-19*
