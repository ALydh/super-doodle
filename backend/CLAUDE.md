# Backend (Scala)

## Stack
- Typelevel ecosystem: cats-effect, fs2, http4s, doobie.
- Tagless final where appropriate.

## Code Philosophy
- Minimal code, no unnecessary abstractions. Inspired by functional and reactive domain modeling (Ghosh).
- No comments unless the code is genuinely surprising. Code should be self-documenting.
- Explicit type annotations at API boundaries; infer internals.
- Effects must be explicit and controlled — never hide side effects in pure-looking code.
- Prefer concise, expressive code over ceremony and boilerplate.

## Error Handling
- ADTs with sealed traits, Either, and IO error channels. No thrown exceptions.

## Architecture
- Simple modules and clean layers. Domain-Driven Design: bounded contexts, aggregates, value objects.
- Isolate domain logic from infrastructure concerns.

## Testing
- TDD: write tests first, then implement.
- ScalaTest.
- Test critical behavior and edge cases, not implementation details.

## Build
- sbt. Run from `backend/` directory.
- Data files live at `../data/wahapedia/` (relative to `backend/`).

## Git
- Use `gh` CLI for all git operations.
- Conventional commits: `feat:`, `fix:`, `chore:`, `refactor:`, `test:`, `docs:`.

## Rules for Claude
- Never refactor code without asking. Suggest improvements, wait for approval.
- Never add features, abstractions, helpers, or config I didn't ask for.
- Never add comments, docstrings, or type annotations to code you didn't change.
- Read existing code before proposing changes.
