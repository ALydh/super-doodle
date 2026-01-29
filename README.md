# New Recruit - Warhammer 40K Army Builder

A full-stack web application for building and managing Warhammer 40,000 army lists. Browse factions, explore datasheets, and create validated armies with point costs.

## Features

- Browse all 40K factions and their units
- View detailed datasheets with stats, abilities, and wargear
- Build armies with detachment selection and enhancement options
- Real-time army validation against game rules
- Persistent army storage

## Tech Stack

| Component | Technology |
|-----------|------------|
| Frontend | React 19, TypeScript, Vite |
| Backend | Scala 3, HTTP4S, Cats Effect |
| Database | SQLite with Doobie |
| Data Source | Wahapedia CSV exports |

## Project Structure

```
.
├── frontend/       # React/TypeScript UI
├── backend/        # Scala/HTTP4S API server
├── data/           # Wahapedia CSV data files
└── scripts/        # Utility scripts
```

## Quick Start

### Prerequisites

- Node.js 18+ and npm
- Java 17+ (JDK)
- sbt (Scala Build Tool)

### Running locally

```bash
# Install dependencies and start both services
make install
make dev
```

Frontend runs at http://localhost:5173, backend at http://localhost:8080.

### Other commands

```bash
make test      # Run all tests
make lint      # Run all linters
make clean     # Remove build artifacts and reset database
make build     # Production build
```

See `frontend/README.md` and `backend/README.md` for detailed setup instructions.

## Data

Game data is sourced from Wahapedia CSV exports stored in `data/wahapedia/`. To refresh:

```bash
./scripts/fetch-wahapedia.sh
```

## License

Private project for personal use.
