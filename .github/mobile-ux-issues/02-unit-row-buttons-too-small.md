# Unit row copy/remove buttons shrink to 22x22px on mobile

## Priority: P0 — Critical for mobile usability

## Problem

In the army builder, the **Copy** and **Remove** buttons on each unit row shrink to **22×22px** on mobile with only **2px gap** between them. This is **half** the recommended 44×44px minimum touch target.

The Remove button is a **destructive action** (removes a unit from the army) that sits immediately adjacent to Copy with almost no gap. Accidental taps are very likely on mobile, and there is no undo mechanism.

## Location

- `frontend/src/pages/UnitRow.module.css` lines 586–592 (`@media (max-width: 599px)`)
- `frontend/src/pages/UnitRow.module.css` lines 545–603 (`@container (max-width: 280px)` — same issue)
- Button elements at `UnitRow.module.css` lines 193–208 (base styles)

## Code

```css
/* Current mobile styles */
.actions .btnCopy,
.actions .btnRemove {
  width: 22px;
  height: 22px;
  min-height: 22px;
  font-size: 0.8rem;
}
```

## Expected Behavior

- Buttons should maintain at least **44×44px** touch target area on mobile
- Increase gap between Copy and Remove to at least **8px**
- Consider adding a confirmation step for Remove on mobile (e.g., swipe-to-delete or long-press)
- The warlord button similarly shrinks to 24×24px and should be addressed

## Affected Screens

- All mobile devices (<600px)
- Narrow container contexts (<280px)
