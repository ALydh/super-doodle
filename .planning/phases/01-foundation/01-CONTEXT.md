# Phase 1: Foundation - Context

**Gathered:** 2026-03-19
**Status:** Ready for planning

<domain>
## Phase Boundary

Wire real datasheet stats and keywords into UnitState so the attack pipeline operates on reference data instead of hardcoded literals. Complete all 17 weapon ability parsers in CoreEffects to produce rules-correct combat outcomes. Implement Big Guns Never Tire, Lone Operative targeting, and Deadly Demise as keyword-driven engine rules.

</domain>

<decisions>
## Implementation Decisions

### Starter Factions
- Use Space Marines, Orks, and Tyranids as the 3 starter factions for engine validation
- Curated subset of ~5-8 units per faction, selected to maximize coverage of all 17 weapon abilities, key keywords (VEHICLE, MONSTER, INFANTRY, CHARACTER), and stat diversity
- Claude selects the specific units based on keyword/weapon coverage needs

### Stats Resolution
- Embed stats directly on UnitState: T, Sv, invuln, W, M, WS, BS, A, Ld, OC as fields
- Keywords stored on UnitState as Set[String] — Claude's discretion whether per-unit or shared lookup
- Stats resolved eagerly at game setup from the SQLite reference DB, not lazily
- resolveTargetProfile in ShootingPhase (currently returning hardcoded T4/3+/None) replaced with real UnitState fields
- conditionMet for HasKeyword, TargetHasKeyword, and PhaseIs must evaluate against real data

### Weapon Ability Edge Cases
- Devastating Wounds: strict no-spillover — mortal wounds from DW kill the allocated model, excess is lost. Requires separate devastatingWoundsDamage field in AttackResult
- Hazardous: test happens per Hazardous weapon fired (not per model). On roll of 1, firing model takes mortal wound; Characters can pass off on 2+
- Indirect Fire: no cover stacking — grants benefit of cover (if not already in cover) plus -1 to hit. Single cover benefit cap
- Anti-X+: silently does not apply against targets lacking the keyword. Not an error — it's a conditional effect per rules
- Rapid Fire: currently uses DidNotMove condition (incorrect). Must be changed to half-range check
- All other weapon abilities: implement strictly per 10th Edition rulebook

### Claude's Discretion
- Keywords storage: per-unit Set[String] vs shared lookup by datasheetId — pick what fits the immutable state pattern
- Specific curated unit selection for each faction
- Indirect Fire cover interaction details (single cover benefit, per FAQ)
- Internal structure of the stats resolution bridge between wp40k.domain.models and wahapedia.engine.state
- Weapon ability parsing implementation details for the 8 stubbed abilities (Blast, Melta, Hazardous, Indirect Fire, Pistol, Precision, Ignores Cover)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Engine core
- `backend/src/main/scala/wahapedia/engine/state/UnitState.scala` — Current UnitState with no stats fields; target for stat embedding
- `backend/src/main/scala/wahapedia/engine/combat/AttackPipeline.scala` — Full attack pipeline; conditionMet returns false for keyword/phase checks
- `backend/src/main/scala/wahapedia/engine/effect/Effect.scala` — Effect system with EffectCondition ADT (HasKeyword, TargetHasKeyword, PhaseIs)
- `backend/src/main/scala/wahapedia/engine/effect/CoreEffects.scala` — Weapon ability parser with 8 None stubs to complete
- `backend/src/main/scala/wahapedia/engine/phase/ShootingPhase.scala` — resolveTargetProfile at line 99 returns hardcoded T4/3+/None

### Reference data models
- `backend/src/main/scala/wp40k/domain/models/ModelProfile.scala` — Has all stats (T, Sv, invuln, W, Ld, OC, M) as parsed from CSV
- `backend/src/main/scala/wp40k/domain/models/Datasheet.scala` — Unit datasheet with datasheetId, factionId, role
- `backend/src/main/scala/wp40k/domain/models/Wargear.scala` — Weapon profiles from reference data
- `backend/src/main/scala/wp40k/domain/models/DatasheetKeyword.scala` — Keywords per datasheet from reference data
- `backend/src/main/scala/wp40k/db/ReferenceDataRepository.scala` — SQLite queries for reference data

### Engine infrastructure
- `backend/src/main/scala/wahapedia/engine/combat/DiceRoller.scala` — DiceRoller trait with FixedDiceRoller for deterministic tests
- `backend/src/main/scala/wahapedia/engine/combat/WoundAllocation.scala` — Wound allocation; needs no-spillover path for Devastating Wounds
- `backend/src/main/scala/wahapedia/engine/state/GameState.scala` — Immutable game state container
- `backend/src/main/scala/wahapedia/engine/effect/EffectResolver.scala` — Collects effects for attack context

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `CoreEffects.fromWeaponAbilityString`: Already parses 9 abilities correctly (Lethal Hits, Sustained Hits, Devastating Wounds, Torrent, Twin-linked, Lance, Heavy, Anti-X+, Stealth). 8 more need completing
- `ModelProfile`: Already has all stat fields parsed from CSV — these map directly to UnitState fields
- `DatasheetKeyword` parser: Keywords already parsed from reference data
- `FixedDiceRoller`: Deterministic dice for exact-outcome testing
- `ReferenceDataRepository`: Existing SQLite queries for datasheets, keywords, weapons

### Established Patterns
- Immutable state with copy-on-change: `unit.copy(hasShot = true)` — same pattern for stat fields
- Effect ADT: `sealed trait Effect` with case classes — weapon abilities must produce Effect instances
- `conditionMet` in AttackPipeline: pattern match on EffectCondition — needs real keyword/phase data
- Either-based error handling: `Either[GameError, Unit]` for validation

### Integration Points
- `ShootingPhase.resolveTargetProfile` (line 99): Currently hardcoded — must read from UnitState
- `ShootingPhase.executeAttack` (line 36): Constructs AttackContext with target stats — must use real values
- `FightPhase`: Same pattern as ShootingPhase — needs same stat resolution
- `AttackPipeline.conditionMet` (line 225): HasKeyword/TargetHasKeyword/PhaseIs return false — must use real data

</code_context>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches. All decisions are rules-driven by 10th Edition.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 01-foundation*
*Context gathered: 2026-03-19*
