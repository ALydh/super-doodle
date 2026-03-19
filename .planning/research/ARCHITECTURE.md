# Architecture Research

**Domain:** Tabletop wargame rules engine (WH40K 10th Edition)
**Researched:** 2026-03-19
**Confidence:** HIGH — based on direct codebase analysis, not external sources

---

## Standard Architecture

### System Overview

```
┌───────────────────────────────────────────────────────────────────┐
│                      Engine Entry Point                            │
│   GameEngine.execute(GameState, Command)                           │
│      Either[GameError, (GameState, List[GameEvent])]               │
├───────────────────────────────────────────────────────────────────┤
│   CommandValidator         CommandExecutor                         │
│   (rejects invalid)        (dispatches to phase handlers)         │
├───────────────────────────────────────────────────────────────────┤
│             Phase Handlers (pure, stateless objects)              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐  │
│  │ Command  │ │Movement  │ │ Shooting │ │  Charge  │ │ Fight  │  │
│  │  Phase   │ │  Phase   │ │  Phase   │ │  Phase   │ │ Phase  │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └────────┘  │
│                    PhaseRunner (transitions)                       │
├───────────────────────────────────────────────────────────────────┤
│             Combat / Ability Resolution Layer                      │
│  ┌─────────────────┐   ┌──────────────┐   ┌──────────────────┐   │
│  │  AttackPipeline │   │EffectResolver│   │  WoundAllocation │   │
│  │  (hit/wound/    │   │(collect &    │   │  (model-level    │   │
│  │   save/damage)  │   │ apply)       │   │   damage)        │   │
│  └─────────────────┘   └──────────────┘   └──────────────────┘   │
│                        WeaponResolver                              │
│               (reference DB → WeaponProfile)                      │
├───────────────────────────────────────────────────────────────────┤
│                    State / Event Layer                             │
│  ┌──────────┐ ┌───────────┐ ┌──────────┐ ┌────────────────────┐  │
│  │GameState │ │ BoardState│ │UnitState │ │    GameEvent log   │  │
│  │(immutable│ │ (spatial  │ │(flags,   │ │(append-only vector)│  │
│  │  record) │ │  model)   │ │ effects) │ │                    │  │
│  └──────────┘ └───────────┘ └──────────┘ └────────────────────┘  │
├───────────────────────────────────────────────────────────────────┤
│              Reference Data Bridge (wp40k.domain)                 │
│  Datasheet / Stratagem / DetachmentAbility / DatasheetLeader      │
│  (CSV-parsed, SQLite-backed, read-only during game)               │
└───────────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Communicates With |
|-----------|---------------|-------------------|
| `GameEngine` | Single entry point; validate then execute; check game-over | All subsystems |
| `CommandValidator` | Reject commands that violate phase/player/ownership rules | `GameState`, `PhaseState` |
| `CommandExecutor` | Pattern-match command → dispatch to phase handler | All phase objects |
| `PhaseRunner` | Advance phase order; reset per-turn flags; emit phase events | `GameState`, `BoardState` |
| `CommandPhase` | CP gain; battleshock tests | `DiceRoller`, `GameState` |
| `MovementPhase` | Normal move, advance, fall back, deep strike validation + execution | `Geometry`, `GameState` |
| `ShootingPhase` | Unit selection, target selection, attack execution | `AttackPipeline`, `EffectResolver`, `WeaponResolver` |
| `ChargePhase` | Declare, roll, charge move | `Geometry`, `DiceRoller` |
| `FightPhase` | Fight ordering, pile in, melee attack, consolidate | `AttackPipeline`, `EffectResolver` |
| `AttackPipeline` | Full hit → wound → save → damage → FNP pipeline | `DiceRoller`, `Effect` system |
| `EffectResolver` | Gather weapon, unit, target, aura effects for an attack context | `GameState`, `BoardState`, `Geometry` |
| `WoundAllocation` | Distribute damage across model pool; trigger model death | `UnitState`, `BoardState` |
| `CoreEffects` | Parse ability strings into typed `Effect` ADT instances | (pure) |
| `WeaponResolver` | Translate `DatasheetId` + wargear selection → `WeaponProfile` list | `wp40k.domain` reference DB |
| `DiceRoller` | Seeded deterministic or live random dice | (pure) |
| `GameState` | Immutable record of everything: round, phases, players, board, events | All read; none mutate directly |

---

## Recommended Project Structure

The existing structure is correct and should be extended, not reorganized:

```
wahapedia/engine/
├── command/
│   ├── Command.scala              — sealed trait; all player-issued commands
│   ├── CommandValidator.scala     — Either-based validation routing
│   └── CommandExecutor.scala      — dispatch to phase handlers
├── phase/
│   ├── PhaseRunner.scala          — phase advancement, flag resets
│   ├── CommandPhase.scala         — CP + battleshock
│   ├── MovementPhase.scala        — move / advance / fall back / deep strike
│   ├── ShootingPhase.scala        — select unit, select target, resolve
│   ├── ChargePhase.scala          — declare, roll, move
│   └── FightPhase.scala           — pile in, fight, consolidate
├── combat/
│   ├── AttackPipeline.scala       — hit/wound/save/damage/FNP
│   ├── WoundAllocation.scala      — model-level damage distribution
│   ├── DiceRoller.scala           — seeded RNG
│   └── WeaponResolver.scala       — trait + DatasheetWeaponResolver impl
├── effect/
│   ├── Effect.scala               — sealed ADT: all modifier/reroll/critical types
│   ├── EffectCondition.scala      — condition ADT (Always, HasKeyword, PhaseIs…)
│   ├── EffectTiming.scala         — duration enum (UntilEndOfPhase, etc.)
│   ├── EffectResolver.scala       — collect effects for an attack context
│   └── CoreEffects.scala          — ability string → Effect parser
├── state/
│   ├── GameState.scala            — top-level immutable record
│   ├── BoardState.scala           — unit map + objectives + terrain
│   ├── UnitState.scala            — per-unit flags, models, effects
│   ├── PlayerState.scala          — CP, VP, faction/detachment IDs
│   ├── Phase.scala                — Phase/SubPhase enums + PhaseState
│   └── GameError.scala            — sealed error ADT
├── event/
│   └── GameEvent.scala            — append-only event log entries
└── spatial/
    ├── Geometry.scala             — distance, engagement range, coherency
    ├── LineOfSight.scala          — terrain-aware LoS checks
    ├── Terrain.scala              — TerrainPiece, terrain type keywords
    └── Vec3.scala                 — 3D position value object

— NEW components to add (see below) —
├── transport/
│   ├── TransportRules.scala       — embark / disembark / capacity validation
│   └── TransportState.scala       — passengers field on UnitState or BoardState
├── leader/
│   └── LeaderRules.scala          — attach / detach, bodyguard wounds, coherency
├── stratagem/
│   ├── StratagemRegistry.scala    — load from reference DB, index by phase/timing
│   └── StratagemExecutor.scala    — resolve a stratagem against current state
├── mission/
│   ├── Mission.scala              — deployment zones, objective positions, VP rules
│   ├── MissionObjective.scala     — primary / secondary / fixed / tactical
│   └── ObjectiveScorer.scala      — scoring per-round and end-of-game
├── faction/
│   ├── DetachmentAbilityLoader.scala — load passive abilities by detachment ID
│   └── UnitAbilityLoader.scala    — datasheet abilities → engine Effects
└── simulation/
    └── BotPlayer.scala            — auto-select valid commands (greedy or random)
```

### Structure Rationale

- **transport/ and leader/ are separate packages** because they both introduce cross-unit state (a unit "inside" another; a leader "attached" to another) that the existing single-unit `UnitState` model does not accommodate. Keeping them isolated makes the invariant clear.
- **stratagem/ is separate from phase/** because stratagems are cross-cutting — they fire across multiple phases with complex timing windows, CP gating, and once-per-phase locks. Mixing them into phase handlers creates spaghetti.
- **mission/ is separate from state/** because mission rules are above-game-state: they determine how VP is computed and when rounds end, but they do not drive command resolution.
- **faction/ is a bridge package** between `wp40k.domain` reference data and `wahapedia.engine` runtime Effects. It is read-only at game start and does not mutate state.

---

## Architectural Patterns

### Pattern 1: Command → Validate → Execute → (State, Events)

**What:** Every player action is a data value (`Command` ADT). `CommandValidator` returns `Either[GameError, Unit]`. If valid, `CommandExecutor` returns `(GameState, List[GameEvent])`. `GameEngine.execute` composes them.

**When to use:** Every new capability — transports, stratagems, heroic interventions — must be expressed as `Command` variants that go through this pipeline. Do not add side-channel mutation.

**Trade-offs:** Forces explicit modeling of every legal action. Slightly verbose for trivial commands, but enables deterministic replay and AI/bot use.

```scala
// Adding a new command follows this exact shape:
case class EmbarkUnit(unitId: UnitId, transportId: UnitId) extends Command

// Validator checks: phase, ownership, capacity, distance
// Executor updates both units and emits UnitEmbarked event
```

### Pattern 2: Effect ADT as the Universal Modifier Language

**What:** All rule modifications (rerolls, +1 to hit, lethal hits, FNP) are instances of `Effect`. `EffectResolver.collectAttackEffects` gathers them from weapon, unit, target, and auras. `AttackPipeline` applies them.

**When to use:** Every new weapon keyword, faction ability, stratagem, or enhancement that modifies the attack pipeline must be expressed as `Effect` instances — not as bespoke case logic inside the pipeline.

**Trade-offs:** Requires `EffectCondition` to grow to cover all cases (e.g., `IsTransportPassenger`, `UnitHasLeader`). The ADT composition approach scales, but conditions must be kept honest — `HasKeyword` is currently a no-op stub.

```scala
// Faction ability that grants +1 to wound vs INFANTRY:
AuraEffect(
  rangeInches = 6.0,
  targetFilter = UnitFilter.Friendly,
  grantedEffect = ModifyRoll(PipelineStep.WoundRoll, 1, TargetHasKeyword("INFANTRY"))
)
```

### Pattern 3: Reference Data Loaded at Game Start, Not At Rule Resolution Time

**What:** `WeaponResolver`, `DetachmentAbilityLoader`, and `UnitAbilityLoader` translate `wp40k.domain` reference data (datasheets, stratagems, abilities) into engine-native types (`WeaponProfile`, `Effect` lists) at game setup time. During play the engine never touches the reference DB.

**When to use:** Every new domain-data-driven component (faction abilities, stratagem effects, transport capacity, leader eligibility) should load into engine types during `GameEngine.setupGame` or a `GameContext` object, not lazily during command execution.

**Trade-offs:** Requires a `GameContext` or enriched `GameState` that carries pre-loaded, engine-native representations. Avoids coupling the pure domain to IO during play.

---

## Data Flow

### Command Resolution Flow

```
Player issues Command
    ↓
GameEngine.execute(state, command)
    ↓
CommandValidator.validate(state, command)
    → Left(GameError)  →  return error, state unchanged
    → Right(())
    ↓
CommandExecutor.execute(state, command, dice, weaponResolver)
    ↓
  (for attack commands)
  WeaponResolver.resolveWeapons(state, cmd)    → List[WeaponProfile]
  EffectResolver.collectAttackEffects(...)     → List[Effect]
  AttackPipeline.resolve(AttackContext)        → AttackResult
  WoundAllocation.apply(board, unit, result)  → (BoardState, List[GameEvent])
    ↓
(GameState with mutations applied, List[GameEvent])
    ↓
GameEngine checks isGameOver
    ↓
Return Either[GameError, (GameState, List[GameEvent])]
```

### Stratagem Flow (new)

```
Player issues UseStratagem(id, playerId, targets)
    ↓
CommandValidator: check CP available, phase matches, not already used this phase
    ↓
StratagemRegistry.lookup(id)    → Stratagem domain record
    ↓
StratagemExecutor.resolve(state, stratagem, targets, dice)
    → inject effects into target unit's activeEffects (duration = UntilEndOfPhase)
    → or immediately modify state (e.g., interrupt fight order)
    → deduct CP from PlayerState
    → emit StratagemUsed event
    ↓
GameState.usedStratagems updated with (playerId, stratagemId, round)
```

### Transport Flow (new)

```
Player issues EmbarkUnit(unitId, transportId)
    ↓
Validate: transport has TRANSPORT keyword, capacity not exceeded,
          unit is within 3" of transport, unit has not moved
    ↓
TransportRules.embark(state, unit, transport)
    → unit.isInReserve = true (hidden from board targeting)
    → transport carries passenger list in TransportState
    → emit UnitEmbarked event
    ↓
Player issues DisembarkUnit(unitId, transportId, positions)
    ↓
TransportRules.disembark(state, unit, transport, positions)
    → positions must be within 3" of transport
    → unit counts as having moved (not advanced)
    → if transport destroyed: emergency disembark with mortal wound test
```

### Leader Attachment Flow (new)

```
At game setup (pre-game):
    AttachLeader(leaderId, bodyguardId)
    ↓
LeaderRules.validate(datasheetLeaders, leaderId, bodyguardId)
    → check DatasheetLeader table: leaderId.attachedId == bodyguardId
    ↓
LeaderRules.attach(state, leaderId, bodyguardId)
    → bodyguard unit gains leader's activeEffects as permanent aura
    → leader models merged into bodyguard unit for targeting purposes
    → bodyguard.attachedLeader = Some(leaderId)
    ↓
During targeting:
    leader models have Lone Operative restriction (if applicable)
    wounds directed to bodyguard models first (leader protected)
```

### Phase Transition and Objective Scoring Flow (new)

```
EndPhase command at end of Fight phase
    ↓
PhaseRunner.advancePhase → switches active player or increments round
    ↓
(at round end, after player 2 completes Fight phase)
ObjectiveScorer.scoreRound(state, mission)
    → for each objective: count OC of controlling player
    → award primary VP per mission rules
    → emit ObjectiveScored events
    ↓
PlayerState.victoryPoints updated
    ↓
GameEngine.checkGameOver: round > 5 or tabled
```

### Key Data Flows Summary

1. **Ability loading (setup):** `DatasheetAbility` records → `CoreEffects.fromCoreAbility` → `ActiveEffect` injected into `UnitState.activeEffects` at game start
2. **Weapon resolution (per attack):** `WargearSelection` on model → `WeaponResolver` → `WeaponProfile` with pre-parsed `List[Effect]`
3. **Stratagem eligibility (per phase):** `GameState.usedStratagems` set + current phase + player CP → gate `UseStratagem` commands
4. **Objective control (per round end):** Sum of `OC` values of alive units within 3" of objective → assign to player with higher total

---

## Missing Components: Integration Guidance

### Transport Rules

**State changes required:**
- `UnitState` needs a `passengers: Vector[UnitId]` field (or a dedicated `TransportState` in `BoardState`)
- Embarked units must be excluded from targeting, coherency, and movement rules
- `BoardState.unitsByPlayer` should exclude embarked units from combat-eligible set

**Command variants to add:**
- `EmbarkUnit(unitId, transportId)`
- `DisembarkUnit(unitId, transportId, positions)`

**Validation boundary:** Capacity is a datasheet property stored as a string in `Datasheet.transport`. `TransportRules` must parse this text (e.g., "12 INFANTRY models") and enforce it.

### Leader Attachment

**State changes required:**
- `UnitState.attachedLeader` already exists as `Option[UnitId]`
- Wound allocation needs to respect the bodyguard-first rule: when an attached unit is targeted, wounds go to non-leader models first
- `WoundAllocation` must be aware of leader protection

**Integration point:** `LeaderRules` validates attachment using `DatasheetLeader` reference data (which already maps `leaderId` → `attachedId`). This is pre-game setup, not a mid-game command.

**Effect propagation:** Leader abilities expressed as `AuraEffect` with `range = 0` (or direct injection) populate the joined unit's `activeEffects` at attachment time.

### Stratagem System

**State already present:** `GameState.usedStratagems: Set[(PlayerId, StratagemId, Int)]` and `InsufficientCP`/`StratagemAlreadyUsed` errors are already defined. The `UseStratagem` command exists but executes as no-op.

**What's needed:**
- `StratagemRegistry`: map from `StratagemId` → `(Stratagem, List[Effect] | StratagemAction)`
- `StratagemExecutor`: interpret the effect (inject `ActiveEffect` with `UntilEndOfPhase` duration, or modify state directly for structural stratagems like "interrupt")
- Phase/timing validation: `Stratagem.phase` and `Stratagem.turn` fields must constrain when `UseStratagem` is valid
- The hardest stratagem type is the interrupt (e.g., "use in enemy Fight phase before a unit fights") — this requires `PhaseState.pendingInterrupt` or a command queue concept

### Mission Framework

**Not yet in state:** `BoardState.objectives` exists as `List[Objective]` but VP scoring is hardcoded to round 5.

**What's needed:**
- `Mission` type: deployment zones, objective positions, primary scoring rule (hold more = +5 VP/round, etc.), secondary objective slots
- `ObjectiveScorer`: pure function `(GameState, Mission) => Map[PlayerId, Int]` called at round end
- `PlayerState.victoryPoints` already exists; just needs to be updated by the scorer
- Coherency check on OC: only units not battleshocked contribute OC

### Faction Abilities / Detachment Abilities

**Reference data exists:** `DetachmentAbility` records in the domain layer carry description strings. `DatasheetAbility` similarly.

**What's needed:**
- `DetachmentAbilityLoader`: parse description text → `List[Effect]` or `ActiveEffect` to inject into all player units at game start
- Most detachment abilities are passive auras best expressed as `AuraEffect` instances in `PlayerState` or injected into `UnitState.activeEffects` with `Permanent` duration
- Unit-specific abilities from datasheets map directly to the existing `CoreEffects` parsing path

---

## Scaling Considerations

This is a pure domain engine for simulation, not a networked service. Scaling concerns are computational, not distributional.

| Concern | At 1 game | At 1000 parallel simulations |
|---------|-----------|------------------------------|
| State size | Small (~50 units, ~200 models) | Memory multiplies linearly; immutable GameState means no sharing — use value types |
| Dice determinism | Seeded per game is sufficient | Each simulation thread needs its own `DiceRoller` seed |
| Effect collection | O(n_units) aura scan per attack | Cache active aura sources per phase; re-collect only when units change |
| Stratagem lookup | O(1) with HashMap | Pre-build `StratagemRegistry` once at game setup |
| Reference data | Load from SQLite once | Load once, share as read-only across simulations (all immutable case classes) |

---

## Anti-Patterns

### Anti-Pattern 1: Faction-Specific Code Branches in Phase Handlers

**What people do:** Add `if factionId == "space-marines" then ...` inside `ShootingPhase` or `AttackPipeline`.

**Why it's wrong:** Combinatorial explosion as factions grow. Untestable in isolation. Violates the data-driven design intent.

**Do this instead:** Express all faction behavior as `Effect` instances loaded by `UnitAbilityLoader`/`DetachmentAbilityLoader` at game start. The pipeline never knows faction identity — it only evaluates effects.

### Anti-Pattern 2: Lazy Reference Data Lookup Inside Command Execution

**What people do:** Call `datasheetRepository.findById(unit.datasheetId)` inside `ShootingPhase.executeAttack` to get toughness/save values.

**Why it's wrong:** Mixes IO-bound reference lookups into pure domain logic. Breaks the "engine is pure, no IO" invariant. Makes simulation non-deterministic in execution order.

**Do this instead:** Resolve all datasheet stats into `UnitState` fields (toughness, save, invuln, leadership) during `GameEngine.setupGame`. `UnitState` should be self-contained for the duration of the game.

### Anti-Pattern 3: Mutating PhaseState in Phase Handlers Directly

**What people do:** Have phase handlers reach into `state.phaseState` and modify fields the handler doesn't own (e.g., `FightPhase` modifying `SubPhase.Reinforcements`).

**Why it's wrong:** Phase handlers are already responsible for their subphase progression. Cross-phase mutations create hidden coupling.

**Do this instead:** Each phase handler returns a new `(GameState, List[GameEvent])`. `PhaseRunner.advancePhase` owns the inter-phase transition. Intra-phase state (e.g., `fightingOrder`, `eligibleChargers`) is set exclusively by the owning phase handler.

### Anti-Pattern 4: StratagemExecutor as a God Object

**What people do:** Build a single `StratagemExecutor` that handles every stratagem via a giant match on name/id with bespoke logic per stratagem.

**Why it's wrong:** Hundreds of stratagems across factions. Untestable. Breaks data-driven design.

**Do this instead:** Stratagems have structured effect payloads (same `Effect` ADT) loaded from reference data. `StratagemExecutor` is generic — it applies whatever effects the loaded `Stratagem` carries. Only structural stratagems (interrupt, fight again) need special cases, and those are a small, enumerable set.

---

## Integration Points

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| `wahapedia.engine` ↔ `wp40k.domain` | One-way: engine reads domain at setup only | Engine never imports IO types; domain never imports engine types |
| Phase handlers ↔ `AttackPipeline` | Direct call: `AttackPipeline.resolve(AttackContext)` | Pipeline is pure; context assembled by phase handler |
| Phase handlers ↔ `EffectResolver` | Direct call: returns `List[Effect]` | EffectResolver reads `GameState`; has no write access |
| `GameEngine` ↔ `ObjectiveScorer` (new) | Called at round end in `PhaseRunner.advancePhase` | Scorer returns VP deltas; PhaseRunner applies them |
| `StratagemExecutor` ↔ Phase handlers | Stratagems inject `ActiveEffect` into `UnitState`; phase handlers read from `activeEffects` | No direct coupling; effects are the interface |
| `TransportRules` ↔ `MovementPhase` | Movement phase checks `unit.passengers` to prevent transport-occupied units from moving independently | One-directional read |

### Build Order (Dependency Sequence)

The components have a clear dependency DAG. Build in this order:

1. **`UnitState` stats fields** — add toughness, save, invuln, leadership, OC to `UnitState` (currently hardcoded in combat; needed by everything downstream)
2. **`WeaponResolver` — real implementation** — translate datasheet wargear to `WeaponProfile`; required before any faction data is useful
3. **`CoreEffects` completions** — implement the no-op weapon keywords (Hazardous, Blast, Melta, Pistol, Indirect Fire, Precision, Assault); these have no external dependencies
4. **`EffectCondition` completions** — `HasKeyword`, `TargetHasKeyword`, `PhaseIs` are stubs; needed before faction abilities work
5. **Leader attachment** — reads `DatasheetLeader` reference data; modifies `WoundAllocation`; no dependency on stratagem or mission
6. **Unit coherency** — pure spatial check on `UnitState.models` positions; no new dependencies
7. **Stratagem system** — depends on completed `EffectCondition`, `EffectDuration`; requires `StratagemRegistry` and `StratagemExecutor`; CP infrastructure already exists
8. **Transport rules** — depends on movement rules being stable; requires `UnitState` passengers field
9. **Mission framework** — depends on `BoardState.objectives` (exists), OC computation (needs stats fields), and `PlayerState.victoryPoints` (exists)
10. **Detachment / faction abilities** — depends on `DetachmentAbilityLoader` parsing ability text into `Effect` instances; requires `EffectCondition` completions
11. **Unit-specific datasheet abilities** — depends on `UnitAbilityLoader`; same parser path as detachment abilities
12. **Overwatch / Heroic Intervention** — interrupts to the normal phase flow; depends on stratagem timing infrastructure
13. **Fights First / Fights Last ordering** — modifies `PhaseState.fightingOrder`; depends on keyword lookup
14. **Bot/AI player** — consumes `GameEngine.validCommands`; depends on all rules being complete

---

## Sources

- Direct codebase analysis: `wahapedia/engine/` package (all files read, 2026-03-19)
- WH40K 10th Edition Core Rules (domain knowledge, HIGH confidence)
- `wp40k/domain/models/` reference data schema (CSV-parsed domain types, read directly)
- `.planning/codebase/ARCHITECTURE.md` (existing full-stack architecture, read directly)

---

*Architecture research for: WH40K 10th Edition game engine*
*Researched: 2026-03-19*
