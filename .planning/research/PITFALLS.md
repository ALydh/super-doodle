# Pitfalls Research

**Domain:** WH40K 10th Edition rules engine (Scala, pure domain)
**Researched:** 2026-03-19
**Confidence:** HIGH (rules sourced from official Wahapedia, GW FAQ docs, Goonhammer Ruleshammer; engine issues grounded in existing codebase analysis)

---

## Critical Pitfalls

### Pitfall 1: Leader/Attached Unit Treated as Two Units

**What goes wrong:**
The engine tracks `attachedLeader: Option[UnitId]` on a bodyguard unit, but the rules require the joined unit to behave as *a single unit* for almost all rules purposes. Common failure modes: treating the leader as a separate target for targeting, applying auras to each independently, running coherency checks on leader models separately, and failing to use bodyguard Toughness when the joined unit is attacked.

**Why it happens:**
The data model naturally represents leader and bodyguard as separate `UnitState` entries with a pointer between them. Developers write rules that iterate `board.units` and forget to check attachment status, or handle the units independently where they should be unified.

**How to avoid:**
Model "joined unit" as a first-class concept with a dedicated accessor that returns the combined entity when targeting, wound allocation, coherency, and effects resolution happens. The rule: *until the last bodyguard is destroyed, attacks cannot be allocated to the Leader model*. Wound allocation must route to bodyguard models first. Only when all bodyguard models are gone can attacks be allocated to the Leader.

**Warning signs:**
- Tests where a leader can be killed while bodyguards still have wounds
- Aura effects applying twice (once for leader, once for unit)
- Leader and bodyguard unit appearing as separate target options in shooting
- Two separate coherency checks running on an attached unit

**Phase to address:** Leader Attachment phase

---

### Pitfall 2: Transport State Is a Phase-Level Concern, Not Just a Unit Flag

**What goes wrong:**
Embarked units are invisible to movement, coherency, targeting, and charge resolution — but only while embarked. The transport's movement status propagates to embarked units with specific restrictions: a unit that disembarks from a transport that made a Normal Move cannot move further or declare a charge that turn. A unit cannot disembark from an Advanced or Fallen Back transport at all. A destroyed transport triggers a special disembark sequence with D6 rolls.

**Why it happens:**
Developers add `isEmbarked: Boolean` to `UnitState` and embark/disembark commands, but don't track *how the transport moved* when disembark validation runs. The boarding state interacts with four separate phases and the transport's per-phase movement record.

**How to avoid:**
Track per-turn transport movement status (Stationary, NormalMoved, Advanced, FalledBack, Destroyed). Disembark validation must check this status and enforce restrictions. Destroyed-transport disembark is a distinct command path: position units within 3" of wreck, roll D6 per model (1 = destroyed), then prohibit charging that turn. Units that disembarked from a destroyed transport cannot declare charges or perform Heroic Interventions that turn.

**Warning signs:**
- Units disembarking from a transport that advanced (should be prohibited)
- Disembarked units charging after the transport made a Normal Move (should be prohibited)
- No D6 roll sequence when a transport is destroyed while occupied
- Embarked units appearing in LOS/targeting checks

**Phase to address:** Transport Rules phase

---

### Pitfall 3: Stratagem Timing and Out-of-Phase Restrictions Are Conflated

**What goes wrong:**
The engine's `usedStratagems: Set[(PlayerId, StratagemId, Int)]` tracks per-round usage, but stratagem restrictions have three separate scopes: once per phase, once per battle round, once per battle. More critically, when a stratagem grants an out-of-phase action (e.g., Fire Overwatch allowing shooting in the opponent's Movement or Charge phase), *no other rules that are normally triggered in that phase apply to those attacks*. This means phase-conditional effects, other stratagems, and abilities that fire "after this unit shoots in the Shooting phase" cannot trigger.

**Why it happens:**
The out-of-phase restriction is buried in the FAQ/Rules Commentary and not in the base Core Rules. Developers implement Overwatch as "allow a shoot action" without plumbing the out-of-phase context that suppresses phase-locked effects.

**How to avoid:**
Add a `phaseContext: Option[Phase]` to the attack context that represents the "logical phase" the action belongs to (distinct from the actual current game phase). Effect conditions referencing `PhaseIs(phase)` must compare against `phaseContext`, and when an out-of-phase action fires, `phaseContext` is set to the triggering phase while the actual phase remains the opponent's. Additionally, implement `once per battle` stratagem tracking separate from the round-scoped set currently in `GameState`.

**Warning signs:**
- `PhaseIs(phase)` condition is always `false` (currently hardcoded as `false` in `conditionMet`)
- Detachment abilities triggering during Overwatch shooting
- The same stratagem being usable twice in one phase
- No `once per battle` stratagem tracking

**Phase to address:** Stratagem System phase; also critical for Overwatch implementation

---

### Pitfall 4: Fights First / Fights Last Creates a Multi-Step Activation Queue, Not Just Priority

**What goes wrong:**
The fight phase has two distinct steps: Fights First (charged units + units with the keyword), then Remaining Combats. Within each step, players alternate activating units — starting with the *non-active player*. After each Consolidation move, units that *become newly eligible* (just entered engagement range via consolidation) are added back to the eligible pool and must be fought before the phase ends. This is not a simple sort by priority; it's a dynamic eligibility queue.

**Why it happens:**
Developers implement fight ordering as sorting `hasFought == false` units by a priority flag. They miss that: (a) the non-active player selects first within each step, (b) eligibility can change mid-phase due to consolidation moves, and (c) Fights Last units cannot be selected while there are other eligible units.

**How to avoid:**
Model the fight phase as an explicit activation loop with dynamic eligible set recomputation after each consolidation. Track `fightsFirst: Boolean` and `fightsLast: Boolean` flags per unit. After each fight action + consolidation, recheck all units for new engagement range eligibility and add newly eligible units to the current step's queue.

**Warning signs:**
- Active player always picking first in fight phase (should be inactive player)
- Units that consolidated into engagement range not getting to fight
- Fights Last units fighting when other eligible units remain
- No mechanism for mid-phase eligibility changes

**Phase to address:** Fight Phase refinements (Fights First/Last)

---

### Pitfall 5: Coherency Enforcement Is Passive (End-of-Move) and Has an Explicit Removal Rule

**What goes wrong:**
Coherency is checked at the end of every kind of move and at the end of every turn. If a unit is out of coherency at end-of-turn, models are removed one-at-a-time until only a single coherent group remains. This is a fundamentally different mechanism from "invalid move" — it is forced model destruction. A unit with 7+ models must be within 2" horizontally and 5" vertically of *at least two* other models (not just one). The 5" vertical component is commonly ignored.

**Why it happens:**
The spatial model (`Geometry.anyModelInEngagementRange`) enforces horizontal distance checks but the engine has no mechanism for the "end of turn coherency removal" rule. Developers either skip it (treating coherency as purely a movement validator) or enforce it incorrectly by using a flat 2" sphere rather than the 2" horizontal / 5" vertical cylinder.

**How to avoid:**
Model coherency as a cylinder: `horizontalDistance <= 2.0 && abs(deltaZ) <= 5.0`. Implement end-of-turn coherency resolution as a distinct game step that identifies out-of-coherency models and removes them. For 7+ model units, use the "within 2" of at least two other models" variant. This should run in `PhaseRunner` at the transition to the next turn.

**Warning signs:**
- Coherency check using `Vec3.distanceTo` (3D Euclidean) instead of horizontal distance + separate vertical check
- No end-of-turn coherency removal step in `PhaseRunner`
- Large units using same coherency rule as small units
- No difference in behavior between 6-model and 7-model units

**Phase to address:** Coherency Enforcement phase

---

### Pitfall 6: Keyword-Based Conditions Are Unimplemented Stubs

**What goes wrong:**
`conditionMet` in `AttackPipeline` has `HasKeyword(kw) => false` and `TargetHasKeyword(kw) => false` hardcoded. Weapon abilities (Anti-X+, Lethal Hits, Devastating Wounds interactions with FNP, Sustained Hits), detachment auras, and faction abilities almost all rely on keyword matching. As long as these return `false`, the entire conditional effect system is inert.

**Why it happens:**
Keyword data requires a lookup from the reference DB (datasheetId → keywords) that wasn't wired during initial engine construction. The stubs were left in as deferred work.

**How to avoid:**
Pass unit keywords into `UnitState` (or resolve via `AttackContext`) so `conditionMet` can check them without a DB call. Keywords are static per-datasheet and should be loaded when a unit is constructed, not looked up at attack resolution time. Wire `HasKeyword` and `TargetHasKeyword` before implementing any weapon ability that depends on them (Anti-X+, Lethal Hits, Sustained Hits all use keyword conditions).

**Warning signs:**
- `HasKeyword(kw) => false` stub still present in `conditionMet`
- Anti-X+ weapons dealing same damage against non-target-keyword units
- Lethal Hits not triggering on 6s
- Aura effects not filtering by keyword

**Phase to address:** Weapon Abilities phase — must be addressed first, before anti-keyword and sustained hits

---

### Pitfall 7: Devastating Wounds Mortal Wound Spillover Is Not Standard Mortal Wounds

**What goes wrong:**
Devastating Wounds inflict mortal wounds equal to the weapon's Damage characteristic on a Critical Wound — but these mortal wounds do *not* spill over to other models. If the target model is destroyed partway through the damage from one Devastating Wounds attack, the remaining mortal wounds from that attack are lost. Standard mortal wounds (from abilities, psychic, etc.) *do* spill over. Implementing both as the same mortal wounds path causes incorrect results.

**Why it happens:**
`AttackPipeline` produces `mortalWounds` as a single integer merged with normal damage. The wound allocation step (`WoundAllocation.allocateDamage`) cannot distinguish "mortal wounds from Devastating Wounds (no spillover)" from "standard mortal wounds (spillover)" because that context is lost.

**How to avoid:**
Track Devastating Wounds damage separately in `AttackResult` — either as `devastatingWoundsDamage` with a no-spillover allocation path, or attach provenance to each damage instance. Wound allocation must respect the per-attack damage cap for Devastating Wounds packets.

**Warning signs:**
- Devastating Wounds mortal wounds spilling to adjacent models
- `AttackResult.mortalWounds` aggregates Devastating Wounds and ability-based mortal wounds into one bucket
- No test case demonstrating non-spillover behavior

**Phase to address:** Weapon Abilities phase (Devastating Wounds)

---

### Pitfall 8: Battle-Shock OC=0 Means Contested, Not Unowned — And It Resets at the Wrong Time

**What goes wrong:**
A battle-shocked unit has OC 0. If both players have only battle-shocked units on an objective, the objective is contested (both OCs sum to 0, neither side is greater — the objective is not controlled by either player). This is not the same as the objective being uncontrolled. Additionally, battle-shock resets at the *start of the controlling player's Command phase*, meaning a battle-shock applied during the opponent's turn recovers before scoring matters.

**Why it happens:**
Objective scoring logic typically checks "does player A have more OC than player B at this marker" — but the "neither controls it" case (both 0) must be handled explicitly, not as defaulting to some previous owner.

**How to avoid:**
Objective control is `ControlledBy(playerId)` or `Contested` — never assume previous ownership persists when OC drops to 0. Battle-shock reset must run at the start of the Command phase for the player who *owns* those units, not at the end of a turn.

**Warning signs:**
- Battle-shocked units on an objective still scoring points
- `boardState.objectives` tracking `controlledBy` as persistent state without re-evaluating per scoring window
- No test for the "both sides OC 0 = contested" case

**Phase to address:** Objective Control & Scoring phase

---

### Pitfall 9: Data-Driven Faction Rules Require a Stable Ability Encoding Contract

**What goes wrong:**
Faction abilities, detachment rules, and unit-specific abilities are described in natural language in the reference DB. If the engine tries to interpret raw ability text, it will either hardcode faction-by-faction logic (code explosion) or fail silently when text doesn't parse. The risk is building encoding structures that work for Faction A's abilities but cannot represent Faction B's without changes to `Effect`, `EffectCondition`, or the stratagem model.

**Why it happens:**
Developers implement faction A's abilities by extending `Effect` with new variants, only to discover faction B needs different variants when that faction is added. Without an upfront survey of the ability *types* across 2-3 factions, the first implementation drives the contract.

**How to avoid:**
Before implementing any faction-specific ability, survey all abilities for the 2-3 starter factions and classify them into types that the existing `Effect` ADT can cover or needs to cover. Extend `Effect` to cover all needed cases before writing any faction-specific data. This is especially critical for abilities that modify game state outside the attack pipeline (e.g., "at the start of the Movement phase, move D6"").

**Warning signs:**
- Faction-specific logic in `PhaseRunner` or phase objects (should be in `Effect` data)
- `Effect` variants added one per faction ability (symptom of discover-as-you-go encoding)
- Abilities that cannot be represented without a new sealed trait variant for every faction added

**Phase to address:** Detachment & Faction Abilities phase — survey first, encode second

---

### Pitfall 10: Overwatch Eligibility Has Multi-Condition Gates

**What goes wrong:**
Overwatch (Fire Overwatch stratagem) can only be used when an enemy unit is set up, or starts/ends a Normal, Advance, Fall Back, or Charge move within range. It cannot be used if the firing unit is already in engagement range. Monsters and Vehicles that have been charged cannot use Overwatch (once enemy is in engagement range with them, Big Guns Never Tire handles shooting, not Overwatch). Units that used Overwatch cannot use *other* shooting-phase-locked rules during that Overwatch.

**Why it happens:**
Overwatch is treated as "just let this unit shoot" without encoding the trigger conditions, eligibility gates, and out-of-phase effect suppression as a unified system.

**How to avoid:**
Model Overwatch as a reaction event with: trigger condition (enemy move event), eligibility predicate (not in engagement range, not Aircraft, 2 CP available), and an `outOfPhase = true` flag that suppresses phase-locked effects. The stratagem target validation must check engagement range at the moment of use, not unit state at start of turn.

**Warning signs:**
- Overwatch allowed from within engagement range
- Phase-conditional weapon abilities triggering during Overwatch
- No 2CP cost deducted for Heroic Intervention / Overwatch
- Aircraft units eligible for Overwatch

**Phase to address:** Overwatch & Heroic Intervention phase

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Hardcode toughness/save in `executeFight` (currently `targetToughness = 4, targetSave = 3`) | Fast to test basic combat | Every test uses wrong stats; stat-dependent abilities never trigger correctly | Never — wire up actual stats before any weapon ability work |
| Leave `HasKeyword => false` stub | Avoids DB wiring | Entire conditional effect system is silently inert for keyword conditions | Only acceptable in the very first engine skeleton, not once abilities are being added |
| Single `mortalWounds` field in `AttackResult` | Simpler result type | Devastating Wounds spillover rule cannot be correctly implemented | Never once Devastating Wounds is implemented |
| Inline `phaseState.unitsActed` tracking per phase | Tracks "has fought/moved" per phase | Does not capture mid-phase eligibility changes (needed for fight phase re-eligibility) | Acceptable for movement/shooting; fight phase needs dynamic recomputation |
| No `outOfPhase` context in `AttackContext` | Simpler attack resolution | Phase-conditional effects fire incorrectly during Overwatch and Heroic Intervention | Never once Overwatch is implemented |

---

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Wahapedia reference DB — unit stats | Querying per-attack from DB (IO in pure domain) | Load unit stats at army-list construction time; pass as `UnitProfile` alongside `UnitState` |
| Wahapedia reference DB — keywords | Same: querying keywords during `conditionMet` | Store keywords in `UnitState` at construction; they are static per datasheet |
| Stratagems from reference DB | Assuming natural-language ability text is machine-readable | Abilities need curated `Effect` encodings per stratagem; raw text is for display only |
| Multiple leaders in same unit | Treating second leader attachment as invalid | Rules allow two Leaders in one unit; both Lead abilities apply simultaneously |

---

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Rebuilding full board state clone on every model position update in multi-model moves | Slow simulation with 20+ model units | Batch position updates; `BoardState.updateUnits` already supports this | With 10+ model units doing pile-in moves |
| Re-resolving aura effects each attack roll | Slow when multiple overlapping auras | Collect all active effects once per unit pair at start of attack sequence; already done in `EffectResolver.collectAttackEffects` — ensure it stays out of per-roll inner loop | With 5+ aura sources active simultaneously |
| Event log unbounded growth during long simulations | Memory pressure after round 3+ with many attacks | Keep events for current turn only in simulation mode; full log only in interactive/replay mode | After 500+ attack events per round |

---

## "Looks Done But Isn't" Checklist

- [ ] **Leader attachment:** Verify that attacking an attached unit uses the bodyguard's Toughness, not the leader's — even if the leader has different T
- [ ] **Transports:** Verify that embarked units do not appear in `board.aliveUnits` for targeting, coherency, or aura range calculations
- [ ] **Stratagem once-per-phase:** Verify that `usedStratagems` tracks phase-scope separately from round-scope and battle-scope
- [ ] **Coherency:** Verify the check uses horizontal distance (not Euclidean 3D), and applies the "2 other models" rule for 7+ model units
- [ ] **Fight phase activation order:** Verify inactive player picks first within each step of the fight phase
- [ ] **Devastating Wounds:** Verify that mortal wounds from DW do not spill over to a second model when the first is destroyed
- [ ] **Keyword conditions:** Verify that `HasKeyword`/`TargetHasKeyword` are wired before any keyword-dependent weapon ability test is written
- [ ] **FNP on mortals:** Verify FNP applies individually to every mortal wound, including those from Devastating Wounds
- [ ] **Objective control:** Verify that two battle-shocked units contesting an objective results in "Contested", not "controlled by last owner"
- [ ] **Battle-shock timing:** Verify battle-shock recovery runs at the correct point in the Command phase (before CP generation, after battleshock tests from prior turn)

---

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Leader/unit separation model wrong | HIGH | Introduce `JoinedUnit` projection; update targeting, coherency, wound allocation to use it; fix all related tests |
| `HasKeyword => false` discovered late | MEDIUM | Wire keyword lookup into `UnitState`; revisit all tests that passed with stub (may reveal silent failures) |
| Devastating Wounds spillover mixed with standard mortals | MEDIUM | Add `devastatingWoundsDamage` field to `AttackResult`; add no-spillover allocation path in `WoundAllocation` |
| Hardcoded stats in fight/shoot execution | LOW-MEDIUM | Replace literal values with unit profile lookup; tests will need updating to provide realistic profiles |
| Out-of-phase context missing when Overwatch is added | HIGH | Requires threading `OutOfPhase` context through effect resolution; touches `AttackContext`, `EffectResolver`, `conditionMet` |

---

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Leader/joined unit behavior | Leader Attachment | Test: last bodyguard removed → leader becomes targetable; attacks before that point cannot wound leader |
| Transport movement state propagation | Transport Rules | Test: disembark-after-advance is illegal; destroyed transport triggers D6 model loss; no charging after destroyed transport disembark |
| Stratagem out-of-phase suppression | Stratagem System | Test: Overwatch attack cannot trigger detachment abilities marked as Shooting phase; once-per-phase tracking per scope |
| Fights First / Fights Last queue | Fight Phase (Fights First/Last) | Test: inactive player activates first; consolidation into range creates new eligible unit mid-phase; Fights Last units wait |
| Coherency cylinder model + removal | Coherency Enforcement | Test: model at 2.1" horizontal passes standard check; model at 1.9" horizontal but 5.1" vertical fails; end-of-turn removal removes correct models |
| Keyword stubs unimplemented | Weapon Abilities (pre-work) | All `HasKeyword` / `TargetHasKeyword` branches return correct value; Anti-X+ triggers on correct target type |
| Devastating Wounds no-spillover | Weapon Abilities (Devastating Wounds) | Test: 3 DW damage kills 1W model, remaining 2 damage is lost — not applied to next model |
| Battle-shock OC = 0 contested | Objective Control & Scoring | Test: both players with OC 0 on objective = Contested, not owned |
| Data-driven ability contract | Detachment & Faction Abilities (survey step) | All starter faction abilities classifiable without new sealed trait variants |
| Overwatch eligibility gates | Overwatch & Heroic Intervention | Test: Overwatch from engagement range fails; Overwatch shooting suppresses phase-locked effects |

---

## Sources

- Official Wahapedia 10th Edition Core Rules: https://wahapedia.ru/wh40k10ed/the-rules/core-rules/
- Wahapedia Rules Commentary (FAQ/Errata): https://wahapedia.ru/wh40k10ed/the-rules/rules-commentary/
- GW Rules Commentary Dec 2024: https://assets.warhammer-community.com/eng_wh40k_core&key_core_rules_updates_commentary_dec2024-q3wavde393-kabutntfbt.pdf
- Goonhammer Ruleshammer — Transports: https://www.goonhammer.com/ruleshammer-transports/
- Goonhammer Ruleshammer — 10th Edition Commentary: https://www.goonhammer.com/ruleshammer-40k-the-10th-edition-commentary-has-an-answer-for-that/
- Goonhammer — Unusual Fight Phases: https://www.goonhammer.com/ruleshammer-unusual-fight-phases-what-units-come-first-last-in-the-middle/
- Goonhammer Coherency: https://www.goonhammer.com/ruleshammer-page-2-of-the-40k-rules-coherency-and-avoiding-traps/
- DakkaDakka — Devastating Wounds implementation discussion: https://www.dakkadakka.com/dakkaforum/posts/list/810390.page
- BolterandChainsword — Transport disembark from surrounded transport: https://bolterandchainsword.com/topic/382352-unit-disembark-from-a-surrounded-transporter/
- NewRecruit.eu — Leader rules: https://www.newrecruit.eu/wiki/wh40k-10e/warhammer-40,000-10th-edition/rules/leader
- Codebase analysis: wahapedia.engine package (existing `AttackPipeline`, `FightPhase`, `UnitState`, `GameState`)

---
*Pitfalls research for: WH40K 10th Edition rules engine (Scala pure domain)*
*Researched: 2026-03-19*
