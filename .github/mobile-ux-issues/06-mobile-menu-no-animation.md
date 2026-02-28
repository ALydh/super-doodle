# Mobile hamburger menu has no open/close animation

## Priority: P1 — Significant UX degradation

## Problem

The mobile hamburger menu uses conditional rendering (`{menuOpen && <div>...</div>}`), causing it to **pop in/out instantly** with no transition. A polished mobile experience would include a slide-down or fade animation. The `×`/`☰` toggle character also switches instantly with no visual transition.

This makes the menu feel abrupt and unfinished compared to the smooth transitions used elsewhere in the app (card hovers, focus rings, etc.).

## Location

- `frontend/src/components/Header.tsx` lines 81–100 (conditional rendering)
- `frontend/src/components/Header.module.css` lines 95–143 (mobile menu styles — no transitions)

## Code

```tsx
// Current: instant show/hide
{menuOpen && (
  <div className={styles.mobileMenu}>
    ...
  </div>
)}
```

## Expected Behavior

1. **Always render** the mobile menu in the DOM with a CSS class toggle for visibility
2. Add a **slide-down** or **fade** CSS transition (150–250ms)
3. Animate the `×`/`☰` toggle with a rotation or cross-fade transition
4. Consider adding a subtle **backdrop overlay** when the menu is open to indicate it's a modal layer

Example approach:
```css
.mobileMenu {
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.2s ease;
}

.mobileMenu.open {
  max-height: 300px;
}
```

## Affected Screens

- All mobile devices (<600px)
