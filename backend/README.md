# Backend

Scala 3 + HTTP4S backend API server with SQLite database.

## Prerequisites

### Java 17+

**macOS:**
```bash
brew install openjdk@17
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt install openjdk-17-jdk
```

**Windows:**

Download from https://adoptium.net/ or use winget:
```powershell
winget install EclipseAdoptium.Temurin.17.JDK
```

Verify installation:
```bash
java -version
```

### sbt (Scala Build Tool)

**macOS:**
```bash
brew install sbt
```

**Linux:**
```bash
echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list
curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo apt-key add
sudo apt-get update
sudo apt-get install sbt
```

**Windows:**

Download from https://www.scala-sbt.org/download.html or use winget:
```powershell
winget install sbt.sbt
```

Verify installation:
```bash
sbt --version
```

## Setup

```bash
cd backend
sbt compile
```

First compilation downloads dependencies and may take a few minutes.

## Development

```bash
sbt run
```

Starts server at http://localhost:8080.

On first run, the application automatically:
1. Creates SQLite database (`wahapedia.db`)
2. Imports data from `../data/wahapedia/*.csv`

## Commands

| Command | Description |
|---------|-------------|
| `sbt run` | Start development server |
| `sbt compile` | Compile sources |
| `sbt test` | Run tests |
| `sbt assembly` | Build fat JAR |
| `sbt nativeImage` | Build GraalVM native image |

## API Endpoints

### Factions & Data
- `GET /api/factions` - List all factions
- `GET /api/factions/{id}/datasheets` - Units for faction
- `GET /api/factions/{id}/detachments` - Detachment options
- `GET /api/factions/{id}/enhancements` - Available enhancements
- `GET /api/factions/{id}/stratagems` - Faction stratagems
- `GET /api/datasheets/{id}` - Unit details

### Army Management
- `GET /api/armies/{id}` - Get army
- `GET /api/factions/{id}/armies` - List armies for faction
- `POST /api/armies` - Create army
- `PUT /api/armies/{id}` - Update army
- `DELETE /api/armies/{id}` - Delete army
- `POST /api/armies/validate` - Validate army list

### Health
- `GET /health` - Health check

## Stack

- **Scala 3.3** - Language
- **HTTP4S** - HTTP server (Ember)
- **Cats Effect** - Functional effects
- **Doobie** - Database access
- **SQLite** - Embedded database
- **Circe** - JSON serialization
- **ScalaTest** - Testing

## Project Structure

```
src/main/scala/wahapedia/
├── domain/
│   ├── models/     # Domain entities
│   ├── army/       # Army aggregates
│   └── types/      # Type definitions
├── db/             # Database layer
├── csv/            # CSV parsing
├── http/           # HTTP routes
├── errors/         # Error types
└── Main.scala      # Entry point
```

## Database

SQLite database stored at `backend/wahapedia.db`. To reset:

```bash
rm wahapedia.db
sbt run  # Recreates and reimports data
```
