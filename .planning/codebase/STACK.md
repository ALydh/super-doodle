# Technology Stack

**Analysis Date:** 2026-03-19

## Languages

**Primary:**
- Scala 3.7.4 - Backend HTTP API, database, domain logic, and MCP server
- TypeScript 5.7.0 - Frontend React components and utilities (strict mode, no `any`)
- HTML/CSS - UI rendering via React

**Secondary:**
- JavaScript (via Node.js for frontend tooling)
- SQL - SQLite database queries via Doobie

## Runtime

**Environment:**
- Scala JVM runtime (GraalVM for native image compilation)
- Node.js (for frontend development and build tools)

**Package Manager:**
- npm (Node.js) - Frontend dependencies
- sbt (Scala Build Tool) - Backend dependencies
- Lockfile: `frontend/package-lock.json` (present), sbt dependency resolution (automatic)

## Frameworks

**Core:**
- React 19.0.0 - Frontend UI framework with functional components
- http4s 0.23.30 - Scala HTTP server framework
- Tapir 1.11.11 - API endpoint DSL and OpenAPI support

**Testing:**
- ScalaTest 3.2.19 - Scala unit/integration tests
- Playwright 1.58.0 - Frontend E2E testing

**Build/Dev:**
- Vite 6.0.0 - Frontend bundler and development server
- sbt with plugins:
  - sbt-native-image 0.3.2 - GraalVM native image compilation
  - sbt-assembly 2.1.1 - JAR packaging
  - sbt-native-packager 1.9.16 - Distribution packaging
  - sbt-bloop 2.0.8 - Build server integration
  - sbt-scalafix 0.12.0 - Code linting and refactoring

## Key Dependencies

**Critical (Backend):**
- cats-effect 3.6.1 - Functional effect system and async/await
- fs2 3.12.2 - Streaming and I/O library
- doobie 1.0.0-RC6 - SQL database access with type safety
- circe 0.14.10 - JSON encoding/decoding
- sqlite-jdbc 3.44.1.0 - SQLite JDBC driver
- log4cats-slf4j 2.7.0 - Logging facade
- jbcrypt 0.4 - Password hashing

**Critical (Frontend):**
- react-router-dom 7.13.0 - Client-side routing
- dompurify 3.3.1 - HTML sanitization (XSS prevention)

**MCP Integration:**
- mcp-server 0.3.3 - Model Context Protocol server implementation
- mcp-server-http4s 0.3.3 - HTTP4S integration for MCP

**Utilities:**
- logstash-logback-encoder 7.4 - Structured logging output
- jsoup 1.17.2 - HTML parsing for data extraction
- scala-parser-combinators 2.4.0 - Parser combinator library
- scala-json-schema 0.2.0 - JSON schema generation
- ip4s-core 3.7.0 - IP address/port handling

## Configuration

**Environment:**
- Variables configured via `sys.env` in Scala:
  - `REF_DB_PATH` - Reference database location (default: `wp40k-ref.db`)
  - `USER_DB_PATH` - User database location (default: `wp40k-user.db`)
  - `GRAALVM_HOME` - GraalVM installation path for native image build
- Frontend uses Vite environment with proxy to backend at `http://127.0.0.1:8080`

**Build:**
- Backend: `build.sbt` with main class `wp40k.Main`
- Frontend: `vite.config.ts` with React plugin
- TypeScript: `tsconfig.json` targeting ES2020, strict mode enabled
- ESLint: `eslint.config.js` with TypeScript and React Hooks support
- Logback: `backend/src/main/resources/logback.xml` for structured logging

## Platform Requirements

**Development:**
- Node.js (for frontend)
- JVM/GraalVM (for Scala backend)
- sbt 1.x (Scala Build Tool)
- Playwright browsers (installed via npm)

**Production:**
- Runs as JAR (`sbt assembly`) or native image (`sbt nativeImage`)
- HTTP server on port 8080
- Supports both single database mode and split database mode (reference + user DBs)
- Can be containerized via Docker (see `deploy/`)

## Notable Build Options

**Native Image:**
- GraalVM compilation enabled with `--no-fallback`
- Resource inclusion for CSV data and configuration
- Memory optimized with `--gc=serial`
- Custom initialization at runtime for database config

**Assembly/JAR:**
- Fallback strategy when native image unavailable
- META-INF merging configured

---

*Stack analysis: 2026-03-19*
