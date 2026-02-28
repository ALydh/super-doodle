# Spotlight Search results obscured by virtual keyboard on mobile

## Priority: P1 — Significant UX degradation

## Problem

On mobile, the Spotlight Search panel starts at `padding-top: 8vh` with `max-height: 80vh`. When the virtual keyboard appears (~50% of the screen height on most phones), the visible area for search results becomes very small.

On an **iPhone SE** (667px viewport height):
- 8vh padding = ~53px from top
- Keyboard takes ~335px
- That leaves roughly **100px** for visible search results — barely 1-2 items

The search input also isn't positioned at the very top of the screen, wasting valuable space above it.

## Location

- `frontend/src/components/SpotlightSearch.module.css` lines 25–38 (mobile overrides)
- `frontend/src/components/SpotlightSearch.tsx` line 373 (backdrop container)

## Code

```css
/* Current mobile styles */
@media (max-width: 599px) {
  .backdrop {
    padding-top: 8vh;  /* Wastes space above search */
  }

  .panel {
    max-height: 80vh;  /* Doesn't account for keyboard */
  }
}
```

## Expected Behavior

1. On mobile, position the search panel at the **very top** of the screen (`padding-top: 0` or `env(safe-area-inset-top)`)
2. Use `max-height: 100dvh` (dynamic viewport height) or `100%` to account for the keyboard
3. Consider using `visualViewport` API to detect keyboard presence and adjust panel height dynamically
4. The panel could also be full-screen on mobile for maximum usable space

## Affected Screens

- All mobile devices, especially smaller phones (iPhone SE, Galaxy S series)
