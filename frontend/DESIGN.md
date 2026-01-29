# Design Guidelines

## Buttons
- **Icon buttons**: 32x32px square with 4px rounded corners
- **Icons**: `+` for add, `⧉` for copy, `×` for remove
- **Colors**: Use faction colors - `emphasis` for copy, `danger` for remove, `primary` for add
- **Hover**: Use `-dark` variant of faction colors

## Pills/Badges
- Rounded pill style (`border-radius: 10px`)
- Small font (`0.75rem`), padding `2px 8px`
- Background: `--surface-border`, text: `--text-secondary`
- Used for points display in unit cards and picker

## Layout
- **Desktop (>1200px)**: 3-column layout (Info | Units | Picker)
- **Mobile/narrow**: Single column, full width
- Sticky sidebars on desktop

## Header
- Clean minimal bar with subtle bottom border
- Dot separators (`·`) between nav items
- Logout as subtle text link, not button

## Points Counter
- Progress bar that fills based on points used
- Gradient from `faction-secondary` to `faction-primary`
- Turns red (`faction-danger`) when over budget

## Unit Lists
- Grouped by role (Characters → Battleline → Dedicated Transport → Other)
- Alphabetically sorted within each role

## Faction Background Icon
- Responsive using viewport units (`vw`)
- Desktop: Large centered (70vw, 0.03 opacity)
- Mobile: Bottom-right corner (60vw, 0.06 opacity)

## Faction Colors
- 5 colors: `primary`, `secondary`, `trim`, `emphasis`, `danger`
- Each has a `-dark` variant for hover states

## General
- Hide controls with only 1 option (e.g., size dropdown)
- No redundant information (e.g., removed role display when role is a heading)
