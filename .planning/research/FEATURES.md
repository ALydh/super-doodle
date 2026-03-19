# Feature Research

**Domain:** WH40K 10th Edition Rules Engine
**Researched:** 2026-03-19
**Confidence:** HIGH (rules verified against wahapedia.ru core rules, goonhammer deep dives, and official GW sources)

---

## Existing Engine Baseline

The following are already implemented and not listed as features to build:

- Phase structure (Command, Movement, Shooting, Charge, Fight) with subphase state machine
- Command/event architecture with immutable GameState
- Combat pipeline: hit rolls, wound rolls, saves, damage, Feel No Pain
- Spatial model: Vec3, distance, engagement range, LoS, terrain cover
- Effects system: modifiers, rerolls, criticals, auras, conditional effects, active effect durations
- Dice roller with deterministic seeding
- Unit/model state tracking: wounds, status flags, positions
- Movement: normal move, advance, fall back, deep strike (9" restriction enforced)
- Charge: declaration, 2D6 roll, charge move
- Fight: pile-in, fight, consolidate
- Wound allocation and model destruction
- Game-over detection: round 5 or tabled
- BattleShock test structure exists (roll vs Leadership) but effects are stub
- UseStratagem command exists as stub (no-op)
- Objectives data structure exists on BoardState but no scoring logic

---

## Feature Landscape

### Table Stakes (Users Expect These)

Features that must be correct for the engine to be a valid WH40K 10th edition rules referee. Missing any of these makes the engine rules-incorrect for competitive play.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| BattleShock consequences | BattleShock test already runs but has no downstream effects — engine is currently a stub | MEDIUM | Three consequences: OC becomes 0, cannot be targeted by Stratagems, Desperate Escape required on fall back. `parseLeadership` returns hardcoded 7; needs real LD from datasheet |
| Desperate Escape test on fall back | Battleshocked units must test D6 per model when falling back; models destroyed on 1-2 | MEDIUM | Requires battleShocked flag check in MovementPhase.validateFallBack; add DesperateEscape event |
| Objective Control (OC) scoring | BoardState has `objectives` list but no scoring logic. OC stat not modeled on units | HIGH | Each unit needs OC characteristic. Scoring: active player controls objective if their OC total > enemy OC total within 3" horizontally / 5" vertically. Battleshocked units contribute OC 0 |
| Leadership stat on UnitState | `parseLeadership` hardcoded to 7; all battleshock tests use wrong value | LOW | Add `leadership: Int` and `oc: Int` to UnitState or resolve from datasheet reference |
| Stratagem execution (core stratagems) | UseStratagem command is a no-op stub | HIGH | 12 core stratagems + 6 detachment stratagems per player. Must enforce: CP cost deduction, once-per-phase-per-stratagem restriction (already tracked via `usedStratagems`), battleshocked unit targeting restriction, correct timing windows (any phase, before/during/after specific steps) |
| Weapon ability: Blast | Parsed as None in CoreEffects — no effect applied | LOW | Minimum of 3 attacks when targeting units of 6+ models, minimum 6 attacks for 11+ models |
| Weapon ability: Melta | Parsed as None | LOW | Roll additional D3 (or parameterised value) for damage when at half range or closer |
| Weapon ability: Hazardous | Parsed as None | LOW | After attacking, roll D6 per Hazardous weapon; on 1, the attacking unit suffers 1 mortal wound per model (characters/leaders can absorb on 2+) |
| Weapon ability: Indirect Fire | Parsed as None | LOW | Ignore LoS; target always treated as in cover; attacker suffers -1 to hit |
| Weapon ability: Pistol | Parsed as None | LOW | Can be used in Shooting phase even when attacker is in engagement range; target must be the engaged unit |
| Weapon ability: Precision | Parsed as None | MEDIUM | On a critical wound roll, allocate the wound to a Character model in the target unit (bypasses normal wound allocation priority) |
| Weapon ability: Ignores Cover | Parsed as None | LOW | Target does not benefit from cover save bonus (cancel the -1 AP reduction from cover) |
| Weapon ability: Rapid Fire | Parsed incorrectly | LOW | Extra attacks only when attacker did not move (current: `DidNotMove`) — but also triggers at half range. Needs range-check condition |
| Unit coherency enforcement | Not implemented; models can be placed in any configuration | HIGH | 2" horizontal / 5" vertical to at least 1 other model. 7+ model units: must be within 2"/5" of at least 2 other models. Must be checked on move, advance, fall back, pile-in, consolidate, deep strike, disembark |
| Leader attachment rules | `attachedLeader` field exists on UnitState but no attachment logic | HIGH | Leader joins a Bodyguard unit: becomes 1 combined unit. Leader model cannot be targeted separately (Lone Operative exception aside). Wound allocation sequence: wounds go to Bodyguard models first; Leader is last to die. When Leader's Bodyguard is destroyed, Leader becomes its own unit |
| Transport rules: embark | No embark/disembark system | HIGH | Unit can embark if within 3" of transport at start of move; unit cannot have moved this phase; capacity tracked on transport; embarked units cannot act until they disembark |
| Transport rules: disembark | Disembark = set up wholly within 3" of transport; unit counts as having moved | HIGH | If disembark after Normal Move of transport: models set up and can act normally. If transport Advances: passengers can still disembark but count as having Advanced. Cannot disembark if transport has Fallen Back |
| Transport rules: destroyed transport | Surviving passengers take Desperate Escape; place survivors wholly within 3" of wreck; place wreck | HIGH | If transport Explodes (Deadly Demise), roll before disembark; result affects all units within 6" including embarked |
| Fights First / Fights Last fight ordering | Current engine: active player picks who fights, no Fights First logic | HIGH | Correct sequence: (1) all eligible Fights First units, alternating starting with non-active player; (2) all remaining eligible units, alternating starting with non-active player. Fights Last units activate after all normal units. Units with both are treated as normal |
| Overwatch | Not implemented | MEDIUM | Core Stratagem (1CP): after enemy unit declares charge, target unit fires at the charging unit before charge roll. Uses normal shooting rules; no Advance or LoS restrictions; hit rolls of 6+ required unless Torrent. Happens after charge is declared, before charge roll |
| Heroic Intervention | Not implemented | MEDIUM | Core Stratagem (2CP): after enemy unit ends a charge move, a friendly unit within 6" can be moved up to 3" (ending closer to the charger). Does not grant Fights First. Must end within engagement range of the charging unit |
| Lone Operative targeting restriction | Defined in CoreEffects as a constant but no enforcement in ShootingPhase | LOW | Ranged attacks can only target a unit with Lone Operative if attacker is within 12". Applies only when unit is not part of an Attached (Leader+Bodyguard) unit |
| Deadly Demise | Defined in CoreEffects constants but no trigger in WoundAllocation | LOW | When model is destroyed, roll D6; on 6, deal X mortal wounds to all units within 6" (X from ability parameter). Must happen before model is removed |
| Big Guns Never Tire | Not implemented | LOW | Vehicles and Monsters in engagement range: -1 to hit rolls with ranged weapons, but can still shoot (any target not in engagement range with them). Can be targeted by enemy ranged attacks |
| Strategic Reserves / Reinforcements | Deep strike exists; no Strategic Reserves framework | MEDIUM | Units can be held in Strategic Reserves (a type of Reserve). Cannot arrive in round 1. Arrive in Reinforcements step of Movement phase. Must set up wholly within 6" of any battlefield edge and not within 9" of enemy. If not arrived by end of round 3 → destroyed. Max 25% of army points |
| Mission framework: deployment | No deployment phase; game starts with units pre-placed | HIGH | Two deployment zones (standard: opposite long edges). Each player sets up units alternately. Units with Deep Strike / Infiltrators / Strategic Reserves have special setup rules. Second player to deploy gets first turn (or roll off) |
| Mission framework: primary objective scoring | VP scoring is a stub (victoryPoints on PlayerState but nothing increments it) | HIGH | Primary mission scoring happens at end of Command phase (active player scores). Each mission card defines scoring differently. Minimum engine support: control-objective-count-at-end-of-command-phase pattern |
| Mission framework: secondary objectives | Not implemented | HIGH | Each player has 2 secret secondary objectives (Fixed or Tactical mode). Score up to 40VP total from secondaries. Tactical: draw and discard each round. Fixed: same objectives all game up to 20VP each |
| CP generation correctness | Currently +1 CP per Command phase regardless of player / round | LOW | Correct: both players start at 0 CP, gain 1 CP at the start of their Command phase. Current implementation is structurally correct but should be validated against round 1 behavior |
| Datasheet stats on UnitState | Target profile hardcoded (T4/3+/no invuln) in ShootingPhase and FightPhase | HIGH | WeaponResolver returns a default profile. Real rules need: toughness, save, invuln save, wounds per model, attacks, WS/BS, movement, leadership, OC — all from datasheet reference data |
| Keyword resolution in EffectConditions | `HasKeyword` and `TargetHasKeyword` always return false in conditionMet | MEDIUM | Unit needs a `keywords: Set[String]` field. Used by Anti-X weapon ability, aura filters with keyword, INFANTRY/VEHICLE/MONSTER distinctions for Big Guns, Lone Operative, etc. |
| Fall Back and Shoot / Charge abilities | Referenced in PROJECT.md as active requirement | LOW | Some units have abilities granting them permission to shoot or charge after Falling Back. Requires a flag/effect that overrides the default restriction in ShootingPhase and ChargePhase validators |

### Differentiators (Competitive Advantage)

Features that provide unique value beyond rules correctness — things other WH40K simulators lack or do poorly.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Deterministic simulation with seeded dice | Reproducible game replays for statistical analysis — the primary use case (automated matchup simulation) | LOW | Already implemented. Needs to be preserved through all new features |
| Bot/AI player for automated play | Enables fully automated simulation without human input — essential for statistical matchup analysis | HIGH | Needs a decision policy for every Command type. Minimum viable AI: greedy (always attack nearest, always advance toward objectives). Plug into `Engine.validCommands` to pick actions |
| Data-driven faction abilities (no hardcoding) | Encoding every faction separately is O(n) work per faction; data-driven means engine scales to all factions | HIGH | Requires CoreEffects parsing to cover all weapon abilities and a unit ability registry that maps datasheet ability strings to Effect instances. WeaponResolver must use reference DB not defaults |
| Full weapon keyword coverage with correct semantics | Most community simulators get Blast, Melta, Precision, Hazardous wrong or ignore them | MEDIUM | Blast minimum attack count, Melta range check, Precision's wound allocation bypass, Hazardous self-damage with Character absorption — these are the most frequently mis-implemented abilities |
| Detachment ability and enhancement system | Detachment rules are the primary army-identity mechanic in 10th edition; engines without them can only simulate generic play | HIGH | Each detachment grants: 1 army-wide rule, 6 stratagems, 4 enhancements. Engine needs a `DetachmentAbility` type and resolution point at the correct timing windows |
| Event log as full audit trail | Every game decision is in the event log; makes debugging rules correctness trivial and enables replay | LOW | Already modeled via `events: Vector[GameEvent]`. Extend to include all missing events (stratagem use, OC scoring, BattleShock consequences, etc.) |
| Infiltrators deployment timing | Infiltrators set up after both armies deploy but before first turn — frequently wrong in other engines | LOW | Mark units with Infiltrators keyword; resolve their placement in a post-deployment setup step before round 1 Movement phase |

### Anti-Features (Commonly Requested, Often Problematic)

Features that seem desirable but would damage the engine's integrity or create scope creep.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| All 20+ factions encoded at launch | "More factions = more complete product" | Each faction has unique abilities that require testing against known scenarios. Shipping untested faction rules creates a confidence problem: the engine becomes untrustworthy as a referee | Start with 2-3 factions (Space Marines + 1 xenos). Validate thoroughly. Add factions incrementally once the data-driven pipeline is proven |
| Interactive UI / player-facing game client | "A visual board would make it easier to test" | Adds a full frontend development project on top of the engine. Every hour spent on UI is an hour not spent on rules correctness | Build simulation harness (automated games, stats output) first. UI is a separate milestone explicitly excluded from PROJECT.md scope |
| Narrative / Crusade rules | "Crusade is popular with casual players" | Crusade adds campaign progression, battle scars, requisition — a separate rules layer. Engine needs to be rules-correct for matched play first | Explicitly deferred per PROJECT.md. Keep engine bounded to matched play core rules |
| Points balancing / meta analysis | "The engine could output tier lists" | That is an analytics product on top of the simulation engine. Conflating the two muddies responsibilities | Engine outputs game state and event logs. Analysis tools (win rates, damage efficiency) are consumers of the engine, not part of it |
| Psychic phase | "9th edition had it, some community resources assume it" | 10th edition removed the psychic phase entirely. Implementing it would be rules-incorrect and confusing | Psychic abilities in 10th are unit abilities that trigger in specific phases — model them as Effects with appropriate timing |
| Terrain placement / scenario generation | "Randomized board setup would make simulations more realistic" | Terrain is a separate concern from rules enforcement. Hardcoded terrain configurations are sufficient for simulation | Accept terrain as setup parameter (already the case via `GameEngine.setupGame`). Standard terrain layouts can be provided as test fixtures |
| Per-unit hardcoded ability implementations | "Faster to hardcode Guilliman's +1 to hit aura" | Every hardcoded unit creates a maintenance burden and proves nothing about the data-driven pipeline | All unit abilities must go through the Effect/EffectResolver system. If an ability cannot be expressed as Effects, extend the Effect ADT |

---

## Feature Dependencies

```
Datasheet Stats on UnitState
    └──required by──> Target profile in ShootingPhase / FightPhase
    └──required by──> Leadership stat for BattleShock
    └──required by──> OC stat for Objective scoring
    └──required by──> Wounds per model (multi-wound model allocation)

Keyword resolution
    └──required by──> Anti-X weapon ability (correct behavior)
    └──required by──> Lone Operative targeting restriction
    └──required by──> Big Guns Never Tire (VEHICLE/MONSTER check)
    └──required by──> Detachment / aura filter keyword matching

BattleShock consequences (OC=0, no stratagem target, Desperate Escape)
    └──required by──> Objective Control scoring (OC=0 when shocked)
    └──required by──> Stratagem system (cannot target shocked unit)
    └──required by──> Desperate Escape (fall back logic)

Unit Coherency enforcement
    └──required by──> Move / Advance / Fall Back (validate final positions)
    └──required by──> Transport disembark (positions must be coherent)
    └──required by──> Leader attachment (7+ model coherency with leader counted)

Leader Attachment
    └──required by──> Wound allocation priority (bodyguard before leader)
    └──required by──> Lone Operative (not applied when in attached unit)
    └──required by──> Transport capacity (attached unit counted as one unit)

Transport rules
    └──required by──> Deadly Demise on transports (must disembark before removal)
    └──required by──> Strategic Reserves (embarked units share reserve status)

Stratagem execution
    └──required by──> Overwatch (Fire Overwatch is a core stratagem)
    └──required by──> Heroic Intervention (core stratagem)
    └──required by──> Counter-offensive / Command Re-roll / Insane Bravery
    └──required by──> Detachment stratagems
    └──required by──> BattleShock consequence (shocked units cannot be stratagem targets)

Objective Control scoring
    └──required by──> Primary mission scoring (end of Command phase VP award)
    └──required by──> Mission framework (which primary mission is active)

Mission framework (deployment + primary + secondary)
    └──required by──> Bot/AI player (AI needs to know what objectives to contest)
    └──required by──> Automated simulation output (win condition is VP, not just tabling)

Fights First / Fights Last ordering
    └──required by──> Heroic Intervention (does NOT grant Fights First — must be explicit)
    └──required by──> Charge bonus (charged units get Fights First)

Bot / AI player
    └──required by──> Automated simulation (the stated primary use case)
    └──requires──> Mission framework (to make objective-driven decisions)
    └──requires──> All phase commands to be valid (engine must not have stubs)
```

### Dependency Notes

- **Datasheet stats require WeaponResolver to use reference data:** The DefaultWeaponResolver returns hardcoded profiles. This must be replaced with a resolver that queries the `wp40k.domain` reference DB before any faction can be meaningfully simulated.
- **Keyword resolution unlocks multiple features in parallel:** Adding `keywords: Set[String]` to UnitState unblocks Anti-X, Lone Operative, Big Guns Never Tire, and aura filter correctness simultaneously.
- **Stratagem system is gated by CP correctness:** CP generation looks correct (1 per Command phase), but stratagem execution must deduct CP, check `usedStratagems`, and respect timing windows. The timing window problem is the main complexity.
- **Fights First ordering requires FightPhase refactor:** Current FightPhase lets the active player pick any unit. The correct model is alternating selection starting with the non-active player, with Fights First units going in a separate first pass.
- **Bot/AI is the last dependency in the critical path:** It cannot function until all phases are rules-correct, because it needs to call `validCommands` on a rules-correct engine state.

---

## MVP Definition

### Launch With (v1 — statistically valid automated simulation)

The minimum engine that can run a rules-correct 5-round game between two factions and output a VP-based winner.

- [ ] Datasheet stats wired to UnitState (T, Sv, invuln, wounds, M, Ld, OC) — without this nothing else is rules-correct
- [ ] Keyword resolution on UnitState — unblocks many downstream features
- [ ] BattleShock consequences: OC=0, Desperate Escape on fall back, stratagem restriction
- [ ] Objective Control scoring at end of Command phase
- [ ] Primary mission scoring (one mission type sufficient for v1)
- [ ] Stratagem execution: CP deduction, once-per-phase enforcement, core stratagems (Command Re-roll, Insane Bravery, Counter-offensive, Fire Overwatch, Heroic Intervention)
- [ ] Fights First / Fights Last fight ordering
- [ ] Weapon abilities: Blast, Melta, Hazardous, Pistol, Indirect Fire, Precision, Ignores Cover (these are parsed as None currently)
- [ ] Rapid Fire range-check condition (currently wrong: triggers on DidNotMove instead of within half range)
- [ ] Lone Operative targeting enforcement
- [ ] Deadly Demise trigger
- [ ] Big Guns Never Tire
- [ ] Unit coherency enforcement (at minimum on move/advance/pile-in/consolidate)
- [ ] Leader attachment (wound allocation priority + Lone Operative interaction)
- [ ] Strategic Reserves arrival rules (round 2+ arrival, round 3 deadline)
- [ ] Bot/AI player (greedy policy sufficient: move toward nearest objective, shoot nearest target, charge when able)

### Add After Validation (v1.x)

- [ ] Transport rules (embark/disembark/destroyed) — triggers once first transport unit is in a faction list
- [ ] Secondary objective scoring — triggers once win-rate simulation is the primary output
- [ ] Detachment abilities and enhancements — triggers when expanding beyond 1 faction
- [ ] Infiltrators deployment timing — triggers when a faction with Infiltrators units is added

### Future Consideration (v2+)

- [ ] Full mission framework with all 9 primary missions and 12+ secondary missions — high complexity, needed only for competitive accuracy
- [ ] Fall back and shoot / fall back and charge abilities — faction-specific; deferred until those factions are in scope
- [ ] Overwatch by non-stratagem means (some faction abilities) — deferred to faction expansion
- [ ] All 20+ factions — after pipeline is validated on 2-3

---

## Feature Prioritization Matrix

| Feature | Engine Value | Implementation Cost | Priority |
|---------|-------------|---------------------|----------|
| Datasheet stats on UnitState | HIGH | LOW | P1 |
| Keyword resolution | HIGH | LOW | P1 |
| BattleShock consequences | HIGH | LOW | P1 |
| Objective Control scoring | HIGH | MEDIUM | P1 |
| Fights First / Last ordering | HIGH | MEDIUM | P1 |
| Stratagem execution (core) | HIGH | HIGH | P1 |
| Weapon abilities: Blast, Melta, Hazardous | HIGH | LOW | P1 |
| Weapon abilities: Pistol, Precision, Ignores Cover, Indirect | MEDIUM | LOW | P1 |
| Rapid Fire range correction | MEDIUM | LOW | P1 |
| Lone Operative enforcement | MEDIUM | LOW | P1 |
| Deadly Demise trigger | MEDIUM | LOW | P1 |
| Big Guns Never Tire | MEDIUM | LOW | P1 |
| Unit coherency enforcement | HIGH | HIGH | P1 |
| Leader attachment | HIGH | HIGH | P1 |
| Strategic Reserves | MEDIUM | MEDIUM | P1 |
| Bot/AI player (greedy) | HIGH | HIGH | P1 |
| Primary mission scoring | HIGH | MEDIUM | P1 |
| Transport rules | MEDIUM | HIGH | P2 |
| Secondary objectives | MEDIUM | HIGH | P2 |
| Detachment abilities | HIGH | HIGH | P2 |
| Full mission framework (9 missions) | MEDIUM | HIGH | P3 |
| Infiltrators deployment | LOW | LOW | P3 |
| Fall Back and Shoot / Charge | LOW | LOW | P3 |

**Priority key:**
- P1: Required for rules-correct automated simulation (v1)
- P2: Required for full competitive matched play accuracy (v1.x)
- P3: Required for complete tournament-legal engine (v2)

---

## Competitor Feature Analysis

| Feature | rzem-ai-tabletop-simulator (boardgame.io, JS) | coldmayo/40kAI (Python) | This Engine |
|---------|----------------------------------------------|-------------------------|-------------|
| Phase structure | Full | Partial | Full |
| BattleShock consequences | Unknown | Unknown | Stub — to implement |
| Objective scoring | Unknown | No | Stub — to implement |
| Stratagem system | Unknown | No | Stub — to implement |
| Transport rules | Unknown | No | Not implemented |
| Leader attachment | Unknown | No | Stub — to implement |
| Weapon abilities coverage | Unknown | Partial | Partial — expanding |
| Deterministic dice / simulation mode | No (real-time play) | Partial | Yes — core differentiator |
| Data-driven faction rules | No (21 factions hardcoded) | No | Target — prevents code explosion |
| Bot/AI simulation player | No (human vs human) | Partial | To implement |
| Pure domain (no IO side effects) | No | No | Yes — Scala pure functions |

The primary competitive advantage of this engine is: deterministic reproducible simulation with pure domain modeling, enabling statistical analysis at scale. No existing open tool combines WH40K 10th edition rules correctness with seeded simulation.

---

## Sources

- [Wahapedia Core Rules (10th Edition)](https://wahapedia.ru/wh40k10ed/the-rules/core-rules/) — authoritative reference, used for BattleShock, coherency, transport, Lone Operative, Deadly Demise, Big Guns
- [Goonhammer 10th Edition Review Part 1: Core Rules](https://www.goonhammer.com/goonhammer-reviews-warhammer-40000-10th-edition-part-1-the-core-rules/) — verified phase rules and stratagem system
- [Goonhammer 10th Edition Review Part 2: Playing the Game](https://www.goonhammer.com/goonhammer-reviews-warhammer-40000-10th-edition-part-2-playing-the-game/) — verified fight phase ordering, Overwatch, Heroic Intervention, Big Guns, Lone Operative
- [Goonhammer Ruleshammer: Heroic Intervention, Fights First](https://www.goonhammer.com/ruleshammer-heroic-intervention-always-fight-first-and-fight-again-abilities/) — verified fight ordering algorithm and Heroic Intervention not granting Fights First
- [Sprues & Brews 10th Edition Core Rules Deep Dive](https://spruesandbrews.com/2023/06/02/warhammer-40k-10th-edition-core-rules-deep-dive/) — secondary verification across all major rule areas
- [Wargamer: Battle-Shock Rules Explained](https://www.wargamer.com/warhammer-40k/battle-shock) — verified OC=0, stratagem restriction, Desperate Escape on fall back
- [Spikeybits: Strategic Reserves & Stratagems](https://spikeybits.com/10th-edition-40k-core-rules-stratagems-reserve-rules/) — verified reserves arrival timing and 25% limit

---

*Feature research for: WH40K 10th Edition Rules Engine (Scala, wahapedia.engine)*
*Researched: 2026-03-19*
