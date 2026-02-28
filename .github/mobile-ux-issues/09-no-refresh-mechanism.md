# No pull-to-refresh or manual refresh mechanism on mobile

## Priority: P2 — Polish

## Problem

Pages fetch data on mount but provide **no way to manually refresh** content. If a request fails on a flaky mobile connection, users see a static error message with no retry option. They must navigate away and come back to trigger a new fetch.

Specific issues:
1. **No pull-to-refresh** gesture — the most common mobile refresh pattern
2. **No refresh/retry button** on error states
3. **Error messages are generic** — `<div className="error-message">{error}</div>` with no action button
4. **No stale data indicator** — if data was cached but the network is unavailable, users don't know they're seeing old data

## Location

- `frontend/src/pages/FactionListPage.tsx` line 83: `if (error) return <div className="error-message">{error}</div>`
- `frontend/src/pages/ArmyViewPage.tsx` line 298: `if (error) return <div className="error-message">{error}</div>`
- `frontend/src/api.ts` — caching layer has no stale indicator

## Expected Behavior

1. Add a **"Try Again" button** to all error states:
   ```tsx
   if (error) return (
     <div className="error-message">
       <p>{error}</p>
       <button onClick={() => window.location.reload()}>Try Again</button>
     </div>
   );
   ```
2. Consider adding a pull-to-refresh library (e.g., `react-pull-to-refresh`) for native-feeling mobile refresh
3. Add a manual refresh button in the header or page toolbar
4. Show a toast/banner when data is loaded from cache on a failed network request

## Affected Screens

- Faction list page, Army view page, Army builder page
- All devices, especially mobile with unreliable connections
