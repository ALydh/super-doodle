# Frontend (TypeScript/React)

## Stack
- React 19 with functional components.
- Vite for bundling.
- TypeScript in strict mode. No `any`.

## Code Philosophy
- Minimal code, no unnecessary abstractions.
- No comments unless the code is genuinely surprising. Code should be self-documenting.
- Explicit type annotations at API boundaries; infer internals.
- Effects must be explicit and controlled — never hide side effects in pure-looking code.
- Prefer concise, expressive code over ceremony and boilerplate.

## Error Handling
- Discriminated unions or typed Result patterns where practical; standard try/catch otherwise.

## Architecture
- Simple modules and clean layers. Domain-Driven Design: bounded contexts, aggregates, value objects.
- Isolate domain logic from infrastructure concerns.

## Testing
- TDD: write tests first, then implement.
- Jest.
- Test critical behavior and edge cases, not implementation details.

## Build
- `npm run dev` for development, `npm run build` for production.

## Git
- Conventional commits: `feat:`, `fix:`, `chore:`, `refactor:`, `test:`, `docs:`.

## Rules for Claude
- Never refactor code without asking. Suggest improvements, wait for approval.
- Never add features, abstractions, helpers, or config I didn't ask for.
- Never add comments, docstrings, or type annotations to code you didn't change.
- Read existing code before proposing changes.
