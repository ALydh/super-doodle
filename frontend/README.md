# Frontend

React 19 + TypeScript frontend built with Vite.

## Prerequisites

### Node.js (v18+)

**macOS:**
```bash
brew install node
```

**Linux (Ubuntu/Debian):**
```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs
```

**Windows:**

Download from https://nodejs.org/ or use winget:
```powershell
winget install OpenJS.NodeJS
```

## Setup

```bash
cd frontend
npm install
```

## Development

```bash
npm run dev
```

Starts dev server at http://localhost:5173 with hot reload.

The frontend expects the backend API at http://localhost:8080 (configured in `vite.config.ts`).

## Commands

| Command | Description |
|---------|-------------|
| `npm run dev` | Start development server |
| `npm run build` | Production build (outputs to `dist/`) |
| `npm run preview` | Preview production build locally |
| `npm run lint` | Run ESLint |
| `npm run test:e2e` | Run Playwright E2E tests |
| `npm run test:e2e:ui` | Run E2E tests with interactive UI |

## Testing

E2E tests use Playwright. First run requires browser installation:

```bash
npx playwright install
```

Run tests:
```bash
npm run test:e2e
```

## Stack

- **React 19** - UI framework
- **TypeScript 5.7** - Type safety (strict mode)
- **Vite 6** - Build tool and dev server
- **React Router 7** - Client-side routing
- **Playwright** - E2E testing
- **ESLint** - Linting

## Project Structure

```
src/
├── components/     # Reusable UI components
├── pages/          # Route page components
├── api.ts          # Backend API client
├── types.ts        # TypeScript type definitions
├── App.tsx         # App root and routing
└── main.tsx        # Entry point
```
