# Roadmap: WH40K Game Engine — Full 10th Edition Rules Coverage

## Overview

Starting from an existing engine that covers phase structure, an immutable command/event architecture, a complete attack pipeline, a spatial model, and deterministic dice rolling, this roadmap completes the rules to produce a fully automated, rules-correct 5-round game with VP-based winner determination. Phases follow a strict dependency order: real unit stats and keyword resolution must be wired before any keyword-dependent rule can be correct; fight ordering must be complete before stratagem timing can be verified; BattleShock consequences must be live before objective scoring is accurate. The final phase delivers a greedy bot player that drives the completed engine in automated simulation runs.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [ ] **Phase 1: Foundation** - Wire real datasheet stats and keywords into UnitState; complete all weapon ability parsing
- [ ] **Phase 2: Fight Phase Completion** - Implement Fights First / Fights Last dynamic activation queue
- [ ] **Phase 3: BattleShock and Coherency** - Enforce BattleShock consequences and unit coherency rules
- [ ] **Phase 4: Leader Attachment** - Implement joined-unit model with wound allocation priority
- [ ] **Phase 5: Stratagem System** - CP-gated stratagem execution with timing and phase restrictions
- [ ] **Phase 6: Objective Control and Mission Scoring** - Objective scoring and primary mission VP accumulation
- [ ] **Phase 7: Strategic Reserves** - Arrival timing, round deadline, and destruction rules
- [ ] **Phase 8: Bot and Simulation** - Greedy AI player and automated simulation harness

## Phase Details

### Phase 1: Foundation
**Goal**: The attack pipeline operates on real unit stats and keywords; all weapon abilities produce rules-correct results
**Depends on**: Nothing (first phase)
**Requirements**: FOUN-01, FOUN-02, FOUN-03, FOUN-04, FOUN-05, FOUN-06, WEAP-01, WEAP-02, WEAP-03, WEAP-04, WEAP-05, WEAP-06, WEAP-07, WEAP-08, WEAP-09, WEAP-10, WEAP-11, WEAP-12, WEAP-13, WEAP-14, WEAP-15, WEAP-16, WEAP-17, PHSE-07, PHSE-08, PHSE-09
**Success Criteria** (what must be TRUE):
  1. A combat scenario using a unit with real datasheet stats (resolved from reference DB) produces different hit/wound/save outcomes than the same scenario with hardcoded T4/3+ literals
  2. A unit carrying the `VEHICLE` or `MONSTER` keyword can shoot while in engagement range with -1 to hit applied
  3. A ranged attack targeting a unit with Lone Operative keyword is rejected unless the attacker is within 12" or part of an attached unit
  4. Deadly Demise triggers and deals mortal wounds to units within 6" when a model with the ability is destroyed
  5. Each weapon ability (Blast, Melta, Hazardous, Indirect Fire, Pistol, Precision, Ignores Cover, Rapid Fire, Sustained Hits, Lethal Hits, Devastating Wounds, Anti-X+, Twin-linked, Assault, Heavy, Lance, Torrent) produces the rules-correct outcome in a deterministic test scenario using FixedDiceRoller
**Plans**: TBD

### Phase 2: Fight Phase Completion
**Goal**: The Fight phase activates units in the correct rules order with dynamic eligibility recomputation after each activation
**Depends on**: Phase 1
**Requirements**: PHSE-04
**Success Criteria** (what must be TRUE):
  1. Units with Fights First activate in a separate first pass before units without Fights First
  2. Within each pass, the non-active player selects which eligible unit fights next (alternating activation)
  3. A unit that moves into engagement range during consolidation becomes eligible for activation in the same Fight phase
  4. Units with Fights Last activate only after all other eligible units have fought
**Plans**: TBD

### Phase 3: BattleShock and Coherency
**Goal**: Battleshocked units are correctly penalized and coherency violations prevent illegal unit positions
**Depends on**: Phase 1
**Requirements**: PHSE-01, PHSE-02, PHSE-03, PHSE-05, PHSE-06
**Success Criteria** (what must be TRUE):
  1. A battleshocked unit has its OC set to 0 for objective control purposes
  2. Attempting to target a battleshocked unit with a stratagem returns an error
  3. A battleshocked unit falling back must roll Desperate Escape for each model; models failing (1-2) are destroyed
  4. A movement command that would leave any model more than 2" horizontally (or 5" vertically) from all other models in the unit is rejected
  5. Coherency is checked after every move, advance, fall back, pile-in, consolidate, and deep strike
**Plans**: TBD

### Phase 4: Leader Attachment
**Goal**: Leaders and bodyguard units form a single joined unit with correct wound allocation and targeting rules
**Depends on**: Phase 1
**Requirements**: LEAD-01, LEAD-02, LEAD-03, LEAD-04
**Success Criteria** (what must be TRUE):
  1. A leader assigned to a bodyguard unit is treated as one combined unit for movement, targeting, and aura purposes
  2. Wounds dealt to the joined unit are allocated to bodyguard models first; the leader only takes wounds once all bodyguard models are destroyed
  3. When the last bodyguard model is destroyed, the leader becomes an independent unit
  4. Lone Operative targeting restriction does not apply to a leader that is part of an attached unit
**Plans**: TBD

### Phase 5: Stratagem System
**Goal**: Stratagems are executed at correct timing windows, cost CP, and cannot be used on ineligible targets
**Depends on**: Phase 2, Phase 3
**Requirements**: STRT-01, STRT-02, STRT-03, STRT-04, STRT-05, PHSE-10, PHSE-11
**Success Criteria** (what must be TRUE):
  1. Using a stratagem deducts the correct CP cost; using a stratagem with insufficient CP returns an error
  2. A stratagem used once in a phase cannot be used again in the same phase
  3. Overwatch fires correctly when a charge is declared: the target unit shoots at the charging unit hitting only on 6+
  4. Heroic Intervention moves a friendly unit up to 3" after a charge move completes; the intervening unit does not gain Fights First
  5. All 12 core stratagems (Command Re-roll, Insane Bravery, Counter-offensive, Fire Overwatch, Heroic Intervention, Go to Ground, Smokescreen, Grenades, Tank Shock, Rapid Ingress, Epic Challenge, Armour of Contempt) execute correctly with phase-locked effect suppression
**Plans**: TBD

### Phase 6: Objective Control and Mission Scoring
**Goal**: VP accumulates each round based on objective control; a 5-round game concludes with a VP-based winner
**Depends on**: Phase 3, Phase 4
**Requirements**: MISS-01, MISS-02, MISS-03, MISS-04
**Success Criteria** (what must be TRUE):
  1. At the end of each Command phase, the active player scores VP for each objective they control (higher total OC within range)
  2. A battleshocked unit contributes OC 0 to objective control, potentially flipping control to the opponent
  3. When both sides have OC 0 on an objective the objective is contested (neither player scores)
  4. After round 5, the game returns a winner (or draw) based on final VP totals
**Plans**: TBD

### Phase 7: Strategic Reserves
**Goal**: Units in Strategic Reserves arrive with correct timing restrictions and are destroyed if they do not arrive by round 3
**Depends on**: Phase 6
**Requirements**: RSRV-01, RSRV-02, RSRV-03, RSRV-04
**Success Criteria** (what must be TRUE):
  1. Units can be placed in Strategic Reserves during game setup
  2. A Strategic Reserve unit attempting to arrive in round 1 returns an error
  3. Strategic Reserve units arrive in the Reinforcements step, placed within 6" of a battlefield edge and not within 9" of any enemy unit
  4. Any Strategic Reserve unit that has not arrived by the end of round 3 is removed from the game
**Plans**: TBD

### Phase 8: Bot and Simulation
**Goal**: Two bot players can play a complete automated 5-round game, producing a VP-based result
**Depends on**: Phase 7
**Requirements**: SIMU-01, SIMU-02, SIMU-03, SIMU-04
**Success Criteria** (what must be TRUE):
  1. The bot player selects a legal command for every decision point using a greedy policy (move toward nearest objective, shoot nearest target, charge when able)
  2. Two bot players can complete a 5-round game without producing an illegal command or GameError
  3. A simulation harness runs N games with a seeded dice roller and collects per-game VP results
  4. The simulation harness uses a deployment zone configuration placing both armies in their respective halves of the board
**Plans**: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation | 0/TBD | Not started | - |
| 2. Fight Phase Completion | 0/TBD | Not started | - |
| 3. BattleShock and Coherency | 0/TBD | Not started | - |
| 4. Leader Attachment | 0/TBD | Not started | - |
| 5. Stratagem System | 0/TBD | Not started | - |
| 6. Objective Control and Mission Scoring | 0/TBD | Not started | - |
| 7. Strategic Reserves | 0/TBD | Not started | - |
| 8. Bot and Simulation | 0/TBD | Not started | - |
