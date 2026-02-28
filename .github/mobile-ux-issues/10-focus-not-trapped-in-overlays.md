# Focus not trapped in mobile menu and spotlight search overlays

## Priority: P2 — Polish / Accessibility

## Problem

When the mobile menu or spotlight search opens, **focus is not trapped** inside the overlay. This causes several accessibility issues:

1. **Keyboard users** can Tab to elements behind the open overlay
2. **Screen reader users** (VoiceOver, TalkBack) can navigate to and interact with content behind the modal
3. The spotlight search **lacks proper ARIA attributes** — no `role="dialog"`, no `aria-modal="true"`, no `aria-label`
4. When overlays close, focus is not returned to the trigger element

### Spotlight Search

- No `role="dialog"` on the panel
- No `aria-modal="true"` to indicate modal behavior
- No focus trap — users can tab out of the search results
- Backdrop click closes it, but keyboard users have no equivalent except Escape

### Mobile Menu

- Opens inline (no portal), but visually overlays the page
- No `role="menu"` or `aria-expanded` on the toggle button
- Links and buttons behind the menu remain focusable

## Location

- `frontend/src/components/SpotlightSearch.tsx` lines 371–426 (missing ARIA and focus trap)
- `frontend/src/components/Header.tsx` lines 73–100 (missing ARIA and focus trap)

## Expected Behavior

1. Add a **focus trap** when overlays are open (e.g., using a `useFocusTrap` hook or a library like `focus-trap-react`)
2. Add proper ARIA attributes:
   ```tsx
   // Spotlight Search
   <div role="dialog" aria-modal="true" aria-label="Search">

   // Mobile Menu toggle
   <button aria-expanded={menuOpen} aria-controls="mobile-menu">
   <nav id="mobile-menu" role="menu">
   ```
3. Return focus to the trigger element when the overlay closes
4. Ensure all interactive elements within the overlay are reachable via keyboard

## Affected Screens

- All mobile devices
- Desktop users using keyboard navigation
- Screen reader users on any device
