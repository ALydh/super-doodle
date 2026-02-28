# Missing theme-color meta tag causes visual disconnect on mobile browsers

## Priority: P1 — Significant UX degradation

## Problem

The `index.html` file lacks a `<meta name="theme-color">` tag. On mobile browsers (Chrome for Android, Safari on iOS), the browser toolbar and status bar default to the system/white color, creating a **jarring visual disconnect** with the app's dark theme.

The app uses a dark color scheme (`--surface-app` is a near-black), but the bright white browser chrome on top breaks the immersive dark theme experience.

## Location

- `frontend/index.html` — missing meta tag in `<head>`

## Expected Behavior

Add a theme-color meta tag matching the app's dark background:

```html
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <meta name="theme-color" content="#0d0d12" />
  <title>WP40k</title>
</head>
```

This single line change will:
- Color the browser toolbar on Chrome Android
- Color the status bar on Safari iOS (with `apple-mobile-web-app-status-bar-style`)
- Create a seamless dark theme experience from edge to edge

## Affected Screens

- All mobile browsers on all devices
