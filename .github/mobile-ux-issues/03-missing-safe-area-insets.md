# Missing safe area insets for modern iPhones (notch/home indicator)

## Priority: P1 — Significant UX degradation

## Problem

The app does not account for **safe area insets** on modern iPhones with notches, Dynamic Island, or home indicators.

1. The viewport meta tag in `index.html` uses `width=device-width, initial-scale=1.0` but **lacks `viewport-fit=cover`**, so the app cannot access safe area inset values
2. No CSS anywhere uses `env(safe-area-inset-*)` to pad content away from hardware obstructions
3. The **sticky points bar** in the army builder (`shared.module.css` lines 241–251) sits at `bottom: 0` on mobile, which **overlaps with the home indicator** on Face ID iPhones

## Location

- `frontend/index.html` line 5 (viewport meta tag)
- `frontend/src/global.css` line 19 (`#root` padding)
- `frontend/src/shared.module.css` lines 241–251 (sticky points bar on mobile)

## Expected Behavior

1. Add `viewport-fit=cover` to viewport meta tag:
   ```html
   <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover" />
   ```
2. Add safe area padding to `#root`:
   ```css
   #root {
     padding-bottom: max(24px, env(safe-area-inset-bottom));
     padding-left: max(24px, env(safe-area-inset-left));
     padding-right: max(24px, env(safe-area-inset-right));
   }
   ```
3. Add bottom safe area to sticky points bar on mobile

## Affected Devices

- iPhone X and later (notch/Dynamic Island models)
- iPads with rounded corners
- Any device with a home indicator bar
