# Hover effects cause sticky hover artifacts on touch devices

## Priority: P1 — Significant UX degradation

## Problem

Multiple interactive elements use `:hover` pseudo-class effects without `@media (hover: hover)` media query guards. On touch devices, hover styles **"stick"** after tapping — the element remains in its hovered state until the user taps elsewhere. This causes visual artifacts where cards appear permanently lifted/shadowed.

### Affected Components

1. **Army cards** (`FactionListPage.module.css` lines 91–96):
   ```css
   .armyCard:hover {
     transform: translateY(-4px);  /* Card stays "lifted" after tap */
     box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
   }
   ```

2. **Faction cards** (`FactionListPage.module.css` lines 234–239): Same `translateY(-4px)` + box-shadow issue

3. **Army card icons** (`FactionListPage.module.css` lines 86–89): Icon scale+opacity change sticks

4. **Table rows** (`global.css` lines 111–112): Background color hover sticks on touch

5. **Expandable unit card headers** (`ExpandableUnitCard.module.css` lines 34–35): Background change sticks

6. **Mobile menu items** (`Header.module.css` lines 119–122): Color change sticks

## Expected Behavior

Wrap hover-specific styles in `@media (hover: hover)` to only apply on devices with a true hover capability (mouse/trackpad):

```css
@media (hover: hover) {
  .armyCard:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  }
}
```

Alternatively, use `:active` for touch feedback:
```css
.armyCard:active {
  transform: scale(0.98);
}
```

## Affected Screens

- All touch devices (phones, tablets, touch-enabled laptops)
