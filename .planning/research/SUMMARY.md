# Project Research Summary

**Project:** WH40K 10th Edition Game Engine (wahapedia.engine)
**Domain:** Pure functional tabletop wargame rules engine — Scala 3
**Researched:** 2026-03-19
**Confidence:** HIGH

## Executive Summary

This is a continuation project, not greenfield. A substantial rules engine already exists in `wahapedia.engine` covering the full phase structure (Command, Movement, Shooting, Charge, Fight), an immutable command/event architecture, a complete attack pipeline (hit/wound/save/damage/FNP), a spatial model, and deterministic dice rolling. The stack is fixed: Scala 3.7.4, ScalaTest 3.2.19, cats-effect 3.6.1, and scala-parser-combinators 2.4.0. The only additions needed are ScalaCheck 1.19.0 for property-based testing of rule invariants. No architectural changes are warranted — extend the existing structure.

The recommended approach is to complete the engine in a strict dependency order: wire datasheet stats and keywords into `UnitState` first, then complete weapon ability parsing, then implement the stratagem system, then add fight-phase ordering, then objective/mission scoring, and finally build a greedy bot/AI player. This order is dictated by data dependency: nearly every downstream rule relies on real unit stats and keyword resolution, both of which are currently stubs. The primary goal is a rules-correct 5-round automated game producing a VP-based winner — no UI, no Crusade rules, no points balancing.

The main risks are subtle rules interactions that are easy to get wrong: the Leader/bodyguard joined-unit model (wound allocation routing, targeting restrictions, aura propagation), stratagem out-of-phase effect suppression (Overwatch must not trigger phase-locked detachment abilities), Fights First/Last as a dynamic eligibility queue (not a priority sort), and Devastating Wounds producing non-spilling mortal wounds distinct from standard mortal wounds. These are not implementation complexity problems — they are specification traps where a plausible-looking implementation silently produces wrong results. Verification via `FixedDiceRoller` scenario tests is the primary guard.

## Key Findings

### Recommended Stack

The existing stack requires no changes beyond adding ScalaCheck. The engine is pure Scala 3 with no IO in the domain layer — every public function returns `Either[GameError, (GameState, List[GameEvent])]`. This invariant must be preserved through all new phases and components. The `FixedDiceRoller` pattern (exact die sequences as varargs) is the established test vehicle and should be extended, not replaced.

**Core technologies:**
- Scala 3.7.4: already in use — opaque types for zero-cost domain IDs, enums for Phase/PipelineStep, no changes needed
- ScalaTest 3.2.19 `AnyFlatSpec`: already in use — do not switch frameworks
- ScalaCheck 1.19.0 + scalatest-plus-scalacheck 3.2.19.0: add for property-based rule invariant tests (e.g., "FNP never increases damage", "AP-0 save never worse than base")
- cats-core (already transitive): use `NonEmptyList` for units-must-have-at-least-one-model invariants; use `Validated` for army list validation where all-errors-at-once is needed

**Do not add:** Akka/Pekko (no actor model needed), Cats Effect IO inside the engine package (destroys testability), any external rules-engine library (all abandoned or Scala 2 only), mutable state in phase functions.

### Expected Features

**Must have (table stakes — required for rules-correct v1):**
- Datasheet stats wired to `UnitState` (T, Sv, invuln, wounds, M, Ld, OC) — currently hardcoded literals in combat resolution
- Keyword resolution on `UnitState` — `HasKeyword`/`TargetHasKeyword` are stubs returning `false`, making the entire conditional effect system inert
- BattleShock consequences: OC=0, Desperate Escape on fall back, cannot be stratagem target
- Objective Control scoring at end of Command phase (OC stat comparison within 3"/5" of objectives)
- Primary mission scoring (at minimum one mission type, hold-objectives pattern)
- Stratagem execution: CP deduction, once-per-phase enforcement, core 12 stratagems
- Fights First / Fights Last fight ordering (dynamic eligibility queue, not priority sort)
- Weapon abilities: Blast, Melta, Hazardous, Pistol, Indirect Fire, Precision, Ignores Cover (all parsed as `None` currently)
- Rapid Fire range-check correction (currently triggers on `DidNotMove`, should also check half-range)
- Lone Operative targeting enforcement (defined but not called)
- Deadly Demise trigger on model destruction (defined but not triggered)
- Big Guns Never Tire (Vehicles/Monsters in engagement range)
- Unit coherency enforcement (cylindrical: 2" horizontal / 5" vertical)
- Leader attachment (wound allocation priority, joined-unit model)
- Strategic Reserves arrival rules
- Bot/AI player (greedy: move toward objectives, shoot nearest, charge when able)

**Should have (v1.x — full competitive accuracy):**
- Transport rules (embark/disembark/destroyed-transport sequences)
- Secondary objective scoring (fixed and tactical modes)
- Detachment abilities and enhancements
- Infiltrators deployment timing

**Defer (v2+):**
- Full mission framework (all 9 primary missions, 12+ secondary missions)
- Fall Back and Shoot / Fall Back and Charge (faction-specific)
- All 20+ factions (validate pipeline on 2-3 factions first)
- Narrative/Crusade rules (explicitly out of scope per PROJECT.md)
- Interactive UI/game client (separate milestone, out of scope)

### Architecture Approach

The existing layered architecture is correct and complete in structure. Every command goes through `CommandValidator` (reject invalid) then `CommandExecutor` (dispatch to phase handler) and returns `(GameState, List[GameEvent])`. The `Effect` ADT is the universal modifier language — all faction abilities, weapon keywords, and stratagem effects must be expressed as `Effect` instances loaded at game setup, never as bespoke case logic inside phase handlers or faction-specific branches. Reference data (`wp40k.domain`) is read once at `GameEngine.setupGame` and never queried during play. New packages to add: `transport/`, `leader/`, `stratagem/`, `mission/`, `faction/`, `simulation/`.

**Major components:**
1. `UnitState` + `WeaponResolver` — must be completed first; all downstream rules depend on real stats and weapon profiles from the reference DB
2. `CoreEffects` + `EffectCondition` — weapon ability parsing completions and keyword condition wiring; unlocks the entire conditional effect system
3. `StratagemRegistry` + `StratagemExecutor` — generic effect injection, not a god-object match; must track once-per-phase, once-per-round, and once-per-battle scopes separately
4. `LeaderRules` — `JoinedUnit` projection for targeting and wound allocation; the most conceptually tricky component
5. `ObjectiveScorer` + `Mission` — pure `(GameState, Mission) => Map[PlayerId, Int]` called at round end by `PhaseRunner`
6. `BotPlayer` (simulation package) — consumes `GameEngine.validCommands`; last in the dependency chain

### Critical Pitfalls

1. **Leader/attached unit treated as two separate units** — wound allocation goes to bodyguard first; leader cannot be targeted while bodyguard models remain; auras must not apply twice. Model this as a `JoinedUnit` projection, not a pointer between two `UnitState` entries that gets checked ad-hoc.

2. **Keyword conditions are stubs (`HasKeyword => false`)** — the entire conditional effect system (Anti-X+, Lethal Hits, Sustained Hits, aura filters, detachment abilities) silently produces wrong results. This must be wired before implementing any keyword-dependent weapon ability. Wire keywords into `UnitState` at construction from the reference DB.

3. **Stratagem out-of-phase effect suppression** — Overwatch fires in the opponent's Movement/Charge phase. Phase-locked effects must check a `phaseContext` field (the logical phase of the action) rather than the actual current game phase. Missing this makes detachment abilities trigger incorrectly during Overwatch.

4. **Fights First/Last is a dynamic activation queue, not a sort** — within each step (Fights First, then Remaining), the non-active player picks first. Consolidation can create newly eligible units mid-phase that must be added back to the queue. Model as an explicit loop with eligibility recomputation, not `List.sortBy`.

5. **Devastating Wounds mortal wounds do not spill over** — unlike standard mortal wounds, DW mortal wounds are capped per-attack to the target model's remaining wounds. `AttackResult` needs a separate `devastatingWoundsDamage` field with a distinct no-spillover allocation path in `WoundAllocation`.

## Implications for Roadmap

Based on the build order dependency DAG from ARCHITECTURE.md and the feature dependency tree from FEATURES.md, suggested phase structure:

### Phase 1: Foundation — Stats, Keywords, and Weapon Abilities
**Rationale:** Every other feature depends on real unit stats and keyword resolution. These are the two deepest stubs in the current engine. Weapon ability completions in `CoreEffects` have no dependencies and can proceed in parallel once keywords are wired.
**Delivers:** A rules-correct attack pipeline with real toughness/save/invuln values, working keyword conditions, and all 8 missing weapon abilities (Blast, Melta, Hazardous, Pistol, Indirect Fire, Precision, Ignores Cover, Rapid Fire fix).
**Addresses features:** Datasheet stats on UnitState, keyword resolution, Blast, Melta, Hazardous, Pistol, Indirect Fire, Precision, Ignores Cover, Rapid Fire correction, Lone Operative enforcement, Deadly Demise trigger, Big Guns Never Tire.
**Avoids:** The `HasKeyword => false` stub pitfall (Pitfall 6); hardcoded stats in combat execution.

### Phase 2: Fight Phase Completion
**Rationale:** Fight phase ordering (Fights First/Last) depends only on keyword lookup (wired in Phase 1). It is a contained, high-complexity change to `FightPhase` that must be correct before the stratagem system adds Heroic Intervention (which interacts with fight ordering).
**Delivers:** Rules-correct fight phase with alternating activation (inactive player picks first), Fights First/Last handling, and dynamic mid-phase eligibility recomputation after consolidation.
**Addresses features:** Fights First / Fights Last fight ordering.
**Avoids:** The dynamic-queue pitfall (Pitfall 4); Heroic Intervention not granting Fights First (must be explicit when HI is added later).

### Phase 3: BattleShock Consequences and Coherency
**Rationale:** BattleShock consequences (OC=0, stratagem restriction, Desperate Escape) are required before objective scoring can be correct, and coherency enforcement is required before transport disembark can be correct. Both are self-contained and can be implemented before the larger stratagem and transport phases.
**Delivers:** Rules-correct BattleShock with OC=0, Desperate Escape on fall back, and cylindrical coherency enforcement with end-of-turn model removal.
**Addresses features:** BattleShock consequences, Desperate Escape, Leadership stat, unit coherency enforcement.
**Avoids:** Battle-shock OC=0 "contested vs. unowned" pitfall (Pitfall 8); coherency cylinder vs. 3D Euclidean distance pitfall (Pitfall 5).

### Phase 4: Leader Attachment
**Rationale:** Leader attachment reads `DatasheetLeader` reference data and modifies wound allocation — a contained change with no dependency on the stratagem system. Must be complete before transport rules (leaders and bodyguards share transport capacity as one unit) and before faction abilities (leader auras inject into joined unit).
**Delivers:** Leaders merged with bodyguard units for targeting and wound allocation; bodyguard-first wound routing; leader becomes independent unit when bodyguard is destroyed.
**Addresses features:** Leader attachment rules (wound allocation priority, Lone Operative interaction).
**Avoids:** Joined-unit-as-two-units pitfall (Pitfall 1); aura double-application.

### Phase 5: Stratagem System
**Rationale:** Stratagems depend on completed `EffectCondition` (Phase 1), BattleShock consequences (Phase 3 — shocked units cannot be stratagem targets), and fight ordering (Phase 2 — Heroic Intervention timing). The `UseStratagem` command is already a no-op stub with infrastructure in place (`usedStratagems`, `InsufficientCP` error, CP tracking).
**Delivers:** CP-gated stratagem execution with once-per-phase/round/battle tracking, Overwatch (with out-of-phase effect suppression), Heroic Intervention, Counter-offensive, Command Re-roll, Insane Bravery.
**Addresses features:** Stratagem execution (core stratagems), Overwatch, Heroic Intervention.
**Avoids:** Stratagem timing/out-of-phase suppression pitfall (Pitfall 3); Overwatch eligibility gates pitfall (Pitfall 10); StratagemExecutor as god-object anti-pattern.

### Phase 6: Objective Control and Mission Scoring
**Rationale:** Objective scoring depends on OC stats (Phase 1), BattleShock OC=0 (Phase 3), and Leader attachment (Phase 4 — leaders in attached units share OC with bodyguard). This is the last prerequisite before the bot/AI player can make objective-driven decisions.
**Delivers:** Per-round primary VP scoring based on objective control at end of Command phase; `ObjectiveScorer` pure function; correct "contested" handling when both sides have OC=0.
**Addresses features:** Objective Control scoring, primary mission scoring, CP generation correctness validation.
**Avoids:** Battle-shock contested-objective pitfall (Pitfall 8).

### Phase 7: Strategic Reserves
**Rationale:** Strategic Reserves arrival (round 2+, round 3 deadline, 25% army limit, 6" from board edge) is a self-contained addition to the Movement phase Reinforcements step. Has no dependency on transport or detachment rules.
**Delivers:** Rules-correct Strategic Reserves with arrival timing enforcement and destruction on round 3 non-arrival.
**Addresses features:** Strategic Reserves / Reinforcements arrival.

### Phase 8: Bot/AI Player
**Rationale:** The bot player is the final link in the critical path — it depends on all phase rules being correct and `GameEngine.validCommands` being reliable. A greedy policy (move toward nearest objective, shoot nearest target, charge when able) is sufficient for automated simulation.
**Delivers:** Fully automated simulation driver in `wahapedia.simulation` package; seeded simulation runs output VP-based winners; statistical matchup analysis becomes possible.
**Addresses features:** Bot/AI player for automated play.

### Phase 9: Transport Rules (v1.x)
**Rationale:** Transports add significant cross-cutting state (embarked units invisible to targeting/coherency/auras; disembark restrictions tied to transport's movement history). Deferred until the core simulation loop is validated because transport-capable units are faction-specific — they trigger when adding factions that field transports.
**Delivers:** Embark/disembark/destroyed-transport sequences; transport movement status tracking; correct Desperate Escape on destroyed transport; embarked units excluded from board presence.
**Addresses features:** Transport rules (embark, disembark, destroyed).
**Avoids:** Transport phase-level movement status pitfall (Pitfall 2).

### Phase 10: Detachment Abilities and Faction Expansion (v1.x)
**Rationale:** Detachment abilities require `EffectCondition` completions (Phase 1), keyword resolution (Phase 1), and a survey of all starter-faction ability types before encoding. This prevents the data-driven-ability-contract pitfall where encoding Faction A drives the contract in ways Faction B cannot satisfy.
**Delivers:** `DetachmentAbilityLoader` and `UnitAbilityLoader` translating reference DB ability text to `Effect` instances at game setup; first two factions fully playable.
**Addresses features:** Detachment ability and enhancement system, data-driven faction abilities.
**Avoids:** Faction-specific code branches anti-pattern; data-driven ability contract pitfall (Pitfall 9).

### Phase Ordering Rationale

- **Stats and keywords first** because every rule in the game either reads unit stats or gates on keywords. Nothing else can be correctly implemented with hardcoded literals and stub `false` conditions.
- **Fight phase before stratagems** because Heroic Intervention's correctness depends on "does not grant Fights First" — this must be verifiable once fight ordering is complete.
- **BattleShock before objectives** because OC=0 for shocked units is a required input to objective scoring.
- **Leader attachment before stratagem system** because stratagem targeting must exclude shocked units (BattleShock phase) and must correctly handle joined units (Leader phase) when checking targeting restrictions.
- **Bot/AI last** because it consumes `GameEngine.validCommands` — a rules-incorrect engine produces nonsense AI decisions and wastes test effort.
- **Transports and detachment abilities deferred** because they are triggered by faction expansion, not by the core simulation loop. The core loop (Phases 1-8) produces the primary deliverable: a rules-correct automated game between two generic factions.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 5 (Stratagem System):** Interrupt stratagems (e.g., "use before a unit fights") require a `pendingInterrupt` or command queue concept not yet present in `PhaseState`. The exact mechanism needs design before implementation.
- **Phase 10 (Detachment Abilities):** Requires surveying all abilities for 2-3 starter factions and classifying them against the current `Effect` ADT before writing any data. This survey step should be explicit in the phase plan.

Phases with standard patterns (skip research-phase):
- **Phase 1 (Stats, Keywords, Weapon Abilities):** All patterns are established in the existing codebase. `CoreEffects.fromWeaponAbilityString` and the `FixedDiceRoller` test pattern are well-documented in the codebase.
- **Phase 2 (Fight Phase):** Rules are documented precisely in Goonhammer Ruleshammer sources. Implementation pattern is `List.sortBy`-then-loop with eligibility recheck.
- **Phase 6 (Objective Scoring):** Pure function `(GameState, Mission) => Map[PlayerId, Int]`; pattern is established.
- **Phase 8 (Bot/AI):** Greedy policy on `validCommands` is a standard simulation driver pattern.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Fixed by existing codebase; library versions verified against Maven/GitHub; no ambiguity |
| Features | HIGH | Rules verified against Wahapedia official sources, GW FAQ, and multiple Goonhammer deep dives; existing engine gaps confirmed by codebase analysis |
| Architecture | HIGH | Based on direct codebase analysis, not inference; existing patterns are consistent and well-structured |
| Pitfalls | HIGH | Rules pitfalls sourced from official FAQ and community rule clarifications; engine-specific pitfalls grounded in current code |

**Overall confidence:** HIGH

### Gaps to Address

- **Stratagem interrupt mechanism:** The `PhaseState.pendingInterrupt` concept is identified but not designed. Before implementing Phase 5, decide whether interrupts use a command queue, a pending-action flag on `PhaseState`, or a separate event bus. This is a design decision, not a research gap.
- **Ability text parsability:** The reference DB stores ability descriptions as natural language strings. Some faction abilities may resist encoding as `Effect` instances (e.g., "at the start of the Movement phase, this unit may move D6 inches as if it were the Movement phase"). Validate during Phase 10 survey step. If an ability cannot be expressed as an `Effect`, the ADT must be extended — this is expected and is not a blocker.
- **Starter faction selection:** Research assumes 2-3 starter factions for validation. The specific factions chosen will determine which weapon abilities and detachment rules are hit first. Space Marines (broad weapon keyword coverage) + one xenos faction (different play style) is the recommended starting point per FEATURES.md, but the specific selection is a product decision.

## Sources

### Primary (HIGH confidence)
- `/Users/alexanderlydh/Repositories/super-doodle/backend/build.sbt` — verified Scala 3.7.4, ScalaTest 3.2.19, cats-effect 3.6.1, fs2 3.12.2
- `wahapedia/engine/` codebase (all files, read 2026-03-19) — architecture patterns, existing stubs, hardcoded values
- [Wahapedia Core Rules 10th Edition](https://wahapedia.ru/wh40k10ed/the-rules/core-rules/) — BattleShock, coherency, transport, Lone Operative, Deadly Demise, Big Guns
- [Wahapedia Rules Commentary](https://wahapedia.ru/wh40k10ed/the-rules/rules-commentary/) — FAQ/Errata for stratagem out-of-phase restrictions
- GW Rules Commentary Dec 2024 — official errata source
- [Goonhammer 10th Edition Review Part 1](https://www.goonhammer.com/goonhammer-reviews-warhammer-40000-10th-edition-part-1-the-core-rules/) — phase rules and stratagem system
- [Goonhammer Ruleshammer: Heroic Intervention / Fights First](https://www.goonhammer.com/ruleshammer-heroic-intervention-always-fight-first-and-fight-again-abilities/) — fight ordering algorithm; Heroic Intervention does not grant Fights First
- [Goonhammer Ruleshammer: Transports](https://www.goonhammer.com/ruleshammer-transports/) — transport movement state restrictions
- [Goonhammer: Unusual Fight Phases](https://www.goonhammer.com/ruleshammer-unusual-fight-phases-what-units-come-first-last-in-the-middle/) — Fights First/Last dynamic queue behavior
- [Goonhammer Coherency](https://www.goonhammer.com/ruleshammer-page-2-of-the-40k-rules-coherency-and-avoiding-traps/) — cylinder model, end-of-turn removal
- [Wargamer: Battle-Shock Explained](https://www.wargamer.com/warhammer-40k/battle-shock) — OC=0, stratagem restriction, Desperate Escape
- ScalaCheck 1.19.0 GitHub releases — confirmed Scala 3 support
- scalatest.org — confirmed scalatest-plus-scalacheck version matrix

### Secondary (MEDIUM confidence)
- [Sprues & Brews 10th Edition Deep Dive](https://spruesandbrews.com/2023/06/02/warhammer-40k-10th-edition-core-rules-deep-dive/) — secondary verification across all rule areas
- [Spikeybits: Strategic Reserves](https://spikeybits.com/10th-edition-40k-core-rules-stratagems-reserve-rules/) — reserves arrival timing and 25% limit
- DakkaDakka — Devastating Wounds implementation discussion (community interpretation, cross-checked against core rules)
- WebSearch: Free monad vs ADT tradeoffs — confirmed overkill when one interpretation exists

### Tertiary (LOW confidence)
- BolterandChainsword — Transport disembark from surrounded transport (community forum; rules confirmed against Wahapedia but edge case)
- NewRecruit.eu — Leader rules (community wiki; verified against Wahapedia)

---
*Research completed: 2026-03-19*
*Ready for roadmap: yes*
