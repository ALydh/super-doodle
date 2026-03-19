# WH40K Game Engine — Full 10th Edition Rules Coverage

## What This Is

A Warhammer 40,000 10th Edition game engine that enforces the complete core rules. Built in Scala as a pure domain model on top of the existing `wahapedia.engine` package, it will be used first for statistical simulations (automated games to analyze matchups and unit efficiency) and eventually as the backbone for interactive play through a UI.

## Core Value

Every core rule from the 10th Edition rulebook is encoded and tested against known scenarios — the engine is a rules-correct referee.

## Requirements

### Validated

- Phase structure (Command, Movement, Shooting, Charge, Fight) — existing
- Command/event architecture with immutable state — existing
- Combat pipeline: hit rolls, wound rolls, saves, damage, Feel No Pain — existing
- Spatial model: distance, engagement range, line of sight, terrain cover — existing
- Effects system: modifiers, rerolls, criticals, auras, conditional effects — existing
- Dice roller with deterministic seeding — existing
- Unit state tracking: wounds, status flags, model positions — existing
- Movement: normal move, advance, fall back, deep strike — existing
- Charge: declaration, roll, charge move — existing
- Fight: pile in, fight, consolidate — existing
- Wound allocation and model destruction — existing
- Game over: round 5 scoring or tabled — existing

### Active

- [ ] Complete Battleshock phase/rules (Leadership tests, OC modification)
- [ ] Command phase: CP generation, Battle-forged rules
- [ ] Strategic Reserves and Reinforcements rules
- [ ] Transport rules (embark, disembark, capacity, destroyed transport)
- [ ] Leader attachment rules (bodyguard, joined units)
- [ ] Unit coherency enforcement (2" / 5" rules)
- [ ] Objective control and scoring
- [ ] Mission framework (deployment, primary/secondary objectives, scoring rounds)
- [ ] Stratagem system (CP cost, timing, restrictions, once-per-phase)
- [ ] Detachment rules and faction abilities for 2-3 starter factions
- [ ] Unit-specific abilities (from datasheets) encoded for starter factions
- [ ] Weapon abilities: full keyword coverage (Sustained Hits, Lethal Hits, Devastating Wounds, Anti-X, Hazardous, Blast, Torrent, Twin-linked, Indirect Fire, Precision, Pistol, Assault, Heavy, Rapid Fire, Melta, Lance, etc.)
- [ ] Overwatch
- [ ] Heroic Intervention
- [ ] Deadly Demise
- [ ] Fights First / Fights Last ordering
- [ ] Lone Operative targeting rules
- [ ] Stealth keyword interaction with hit rolls
- [ ] Big Guns Never Tire (Monsters/Vehicles shooting in engagement)
- [ ] Fall Back and Shoot / Fall Back and Charge abilities
- [ ] Bot/AI player for automated simulation play

### Out of Scope

- Frontend game UI — separate project/milestone after engine is rules-complete
- All 20+ factions at once — start with 2-3 to validate, expand later
- Crusade/narrative rules — competitive matched play only
- Points balancing or meta analysis tools — engine simulates, doesn't analyze
- Terrain placement or list-building — use existing army builder for lists

## Context

- Existing engine lives in `wahapedia.engine` package (Scala, pure domain, no IO)
- Reference data (datasheets, stratagems, abilities, factions) available via `wp40k.domain` and SQLite reference DB
- MCP server provides tool access to faction data for development reference
- 10th Edition simplified the rules significantly vs 9th (no psychic phase, streamlined stratagems)
- The engine uses Either-based error handling and immutable GameState

## Constraints

- **Tech stack**: Scala 3 / Typelevel ecosystem — must stay consistent with existing codebase
- **Pure domain**: Engine must remain effect-free (no IO) — pure functions over immutable state
- **Data-driven**: Faction/unit rules should be driven by reference data, not hardcoded per faction
- **Testability**: Every rule must be testable in isolation with deterministic dice

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Start with 2-3 factions | Validate engine completeness before scaling to all factions | -- Pending |
| Stats engine before interactive play | Proves rules correctness without UI complexity | -- Pending |
| Data-driven faction rules | Avoids per-faction code explosion, leverages existing reference DB | -- Pending |

---
*Last updated: 2026-03-19 after initialization*
