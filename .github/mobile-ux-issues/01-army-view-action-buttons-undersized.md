# Army View action buttons are undersized for touch targets (32x32px)

## Priority: P0 — Critical for mobile usability

## Problem

The action buttons (Export, Text, Copy, Edit, Delete) on the Army View page are only **32×32px** with `min-height: 32px` and **8px gap** between them. Apple's Human Interface Guidelines and WCAG 2.5.5 recommend a minimum **44×44px** touch target.

On mobile, these buttons are **absolutely positioned** at the top-right of the header and rendered as **icon-only** buttons using CSS `::before` pseudo-elements (`↓`, `T`, `⧉`, `✎`, `×`). Five unlabeled icon buttons in a tight row creates two problems:

1. **Hard to tap accurately** — fingers easily hit the wrong button, especially Delete (destructive action) which sits adjacent to Edit
2. **Hard to understand** — no text labels or tooltips, so users must guess what `T` or `⧉` means

## Location

- `frontend/src/pages/ArmyViewPage.module.css` lines 58–76 (button sizing)
- `frontend/src/pages/ArmyViewPage.module.css` lines 246–256 (mobile override — buttons still 32px)
- `frontend/src/pages/ArmyViewPage.tsx` lines 382–394 (button rendering)

## Expected Behavior

- Buttons should be at least **44×44px** on mobile (can use padding to expand touch area without changing visual size)
- Consider using an overflow/action menu (⋮) on mobile instead of 5 separate icon buttons
- Add `aria-label` attributes to each button for screen reader support
- Consider adding text labels on mobile or at minimum a tooltip

## Affected Screens

- All mobile devices (<600px)
- All tablet devices (600–899px)
