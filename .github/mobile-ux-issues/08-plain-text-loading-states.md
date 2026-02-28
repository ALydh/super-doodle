# Plain text "Loading..." states feel unpolished on mobile

## Priority: P2 — Polish

## Problem

Loading states across the app are bare `<div>Loading...</div>` strings with no styling, skeleton screens, or loading indicators. On mobile where network connections are often slower (cellular, poor WiFi), users see an unstyled text flash that:

1. **Provides no layout stability** — the page jumps when content loads in
2. **Feels unfinished** — no visual feedback that something is happening
3. **Gives no progress indication** — users can't tell if the app is working or frozen

## Location

- `frontend/src/pages/ArmyBuilderPage.tsx` line 240: `<div>Loading...</div>` (auth loading)
- `frontend/src/pages/ArmyBuilderPage.tsx` line 251: `<div>Loading...</div>` (data loading)
- `frontend/src/pages/ArmyViewPage.tsx` line 299: `<div>Loading...</div>` (battle data loading)

## Expected Behavior

1. Add a **loading spinner** or **skeleton screen** component that maintains the expected page layout
2. Skeleton screens should match the approximate shape of the content that will load (cards, headers, etc.)
3. Use a subtle **pulse animation** on skeleton elements to indicate activity
4. Consider showing the page header/shell immediately and only skeleton the data-dependent content

Example skeleton for army card:
```css
.skeleton {
  background: linear-gradient(90deg, var(--surface-card) 25%, var(--surface-panel) 50%, var(--surface-card) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 8px;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
```

## Affected Screens

- All pages on all devices, especially on slow mobile connections
