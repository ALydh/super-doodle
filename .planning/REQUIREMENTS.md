# Requirements: WH40K Game Engine — Full 10th Edition Rules Coverage

**Defined:** 2026-03-19
**Core Value:** Every core rule from the 10th Edition rulebook is encoded and tested against known scenarios

## v1 Requirements

Requirements for rules-correct automated simulation. Each maps to roadmap phases.

### Foundation

- [ ] **FOUN-01**: UnitState carries real datasheet stats (T, Sv, invuln, wounds, M, WS, BS, A, Ld, OC) resolved from reference data
- [ ] **FOUN-02**: UnitState carries keywords as `Set[String]` resolved from reference data
- [ ] **FOUN-03**: `HasKeyword` and `TargetHasKeyword` effect conditions evaluate against real unit keywords
- [ ] **FOUN-04**: `PhaseIs` effect condition evaluates against current game phase
- [ ] **FOUN-05**: WeaponResolver uses reference DB to resolve real weapon profiles (replace hardcoded defaults)
- [ ] **FOUN-06**: ShootingPhase and FightPhase use real target stats from UnitState (not hardcoded T4/3+)

### Weapon Abilities

- [ ] **WEAP-01**: Blast — minimum 3 attacks vs 6+ models, minimum 6 attacks vs 11+ models
- [ ] **WEAP-02**: Melta — extra D3 (or parameterized) damage at half range or closer
- [ ] **WEAP-03**: Hazardous — D6 per Hazardous weapon after attacking; mortal wound on 1 (character absorption on 2+)
- [ ] **WEAP-04**: Indirect Fire — ignore LoS, target treated as in cover, -1 to hit
- [ ] **WEAP-05**: Pistol — can fire in Shooting phase while in engagement range, must target engaged unit
- [ ] **WEAP-06**: Precision — on critical wound, allocate to Character model in target unit
- [ ] **WEAP-07**: Ignores Cover — target does not benefit from cover save bonus
- [ ] **WEAP-08**: Rapid Fire — extra attacks at half range (fix current DidNotMove condition)
- [ ] **WEAP-09**: Sustained Hits — on critical hit, generate extra hits (parameterized count)
- [ ] **WEAP-10**: Lethal Hits — on critical hit, auto-wound (skip wound roll)
- [ ] **WEAP-11**: Devastating Wounds — on critical wound, mortal wounds with no-spillover rule
- [ ] **WEAP-12**: Anti-X+ — on wound roll of X+, auto-wound against targets with keyword
- [ ] **WEAP-13**: Twin-linked — reroll wound rolls
- [ ] **WEAP-14**: Assault — can fire after Advancing (with -1 to hit)
- [ ] **WEAP-15**: Heavy — +1 to hit if unit did not move
- [ ] **WEAP-16**: Lance — +1 to wound on charge turn
- [ ] **WEAP-17**: Torrent — auto-hit (skip hit roll)

### Phase Rules

- [ ] **PHSE-01**: BattleShock consequences — battleshocked units have OC 0
- [ ] **PHSE-02**: BattleShock consequences — battleshocked units cannot be targeted by stratagems
- [ ] **PHSE-03**: Desperate Escape — battleshocked units falling back roll D6 per model, destroyed on 1-2
- [ ] **PHSE-04**: Fights First / Fights Last ordering — alternating activation starting with non-active player, Fights First units go in separate first pass
- [ ] **PHSE-05**: Unit coherency enforcement — 2" horizontal / 5" vertical to at least 1 other model (2 others for 7+ model units)
- [ ] **PHSE-06**: Coherency checked on move, advance, fall back, pile-in, consolidate, deep strike
- [ ] **PHSE-07**: Big Guns Never Tire — Vehicles/Monsters can shoot while in engagement (-1 to hit with ranged), can be targeted by ranged attacks
- [ ] **PHSE-08**: Lone Operative — ranged attacks only within 12" unless unit is part of attached unit
- [ ] **PHSE-09**: Deadly Demise — when model destroyed, roll D6; on 6, deal X mortal wounds to units within 6"
- [ ] **PHSE-10**: Overwatch — core stratagem (1CP); after charge declaration, target fires at charging unit (hit on 6+ only)
- [ ] **PHSE-11**: Heroic Intervention — core stratagem (2CP); after charge move, friendly unit within 6" moves up to 3" (no Fights First)

### Leader & Unit Composition

- [ ] **LEAD-01**: Leader attachment — leader joins bodyguard unit as single combined unit
- [ ] **LEAD-02**: Wound allocation priority — wounds go to bodyguard models first, leader last
- [ ] **LEAD-03**: Leader detachment — when bodyguard destroyed, leader becomes own unit
- [ ] **LEAD-04**: Lone Operative interaction — not applied when leader is in attached unit

### Stratagem System

- [ ] **STRT-01**: Stratagem execution deducts CP cost from player
- [ ] **STRT-02**: Once-per-phase restriction enforced per stratagem
- [ ] **STRT-03**: Battleshocked units cannot be targeted by stratagems
- [ ] **STRT-04**: Core stratagems implemented: Command Re-roll, Insane Bravery, Counter-offensive, Fire Overwatch, Heroic Intervention, Go to Ground, Smokescreen, Grenades, Tank Shock, Rapid Ingress, Epic Challenge, Armour of Contempt
- [ ] **STRT-05**: Stratagem timing windows enforced (before/during/after specific steps)

### Objective & Mission

- [ ] **MISS-01**: Objective Control scoring — unit OC total within range of objective determines control
- [ ] **MISS-02**: Primary mission scoring at end of Command phase (active player scores)
- [ ] **MISS-03**: At least one primary mission type fully implemented
- [ ] **MISS-04**: Victory determined by VP comparison after round 5

### Strategic Reserves

- [ ] **RSRV-01**: Units can be placed in Strategic Reserves during setup
- [ ] **RSRV-02**: Strategic Reserve units cannot arrive in round 1
- [ ] **RSRV-03**: Arrival in Reinforcements step — within 6" of battlefield edge, not within 9" of enemy
- [ ] **RSRV-04**: Units not arrived by end of round 3 are destroyed

### Simulation

- [ ] **SIMU-01**: Bot/AI player with greedy decision policy for all command types
- [ ] **SIMU-02**: Bot moves toward nearest objective, shoots nearest target, charges when able
- [ ] **SIMU-03**: Simulation harness can run N automated games and collect results
- [ ] **SIMU-04**: Mission framework with deployment zones (at least one standard deployment)

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Transport

- **TRNS-01**: Embark — unit within 3" of transport can embark (capacity tracked)
- **TRNS-02**: Disembark — set up wholly within 3" of transport, counts as moved
- **TRNS-03**: Destroyed transport — Desperate Escape for passengers, place survivors

### Missions Extended

- **MEXT-01**: Secondary objective scoring (Fixed and Tactical modes)
- **MEXT-02**: Full mission framework with all primary mission types

### Faction Expansion

- **FACT-01**: Detachment abilities and enhancements system
- **FACT-02**: Infiltrators deployment timing
- **FACT-03**: Fall Back and Shoot / Fall Back and Charge abilities
- **FACT-04**: Expand to all factions beyond starter 2-3

## Out of Scope

| Feature | Reason |
|---------|--------|
| Interactive game UI | Separate milestone after engine is rules-complete |
| All 20+ factions at launch | Start with 2-3, expand after pipeline validated |
| Crusade / narrative rules | Matched play only for v1 |
| Points balancing / meta analysis | Engine simulates; analysis tools are separate consumers |
| Psychic phase | Removed in 10th Edition; psychic abilities are unit abilities modeled as Effects |
| Terrain placement / scenario generation | Accept terrain as setup parameter (already supported) |
| Per-unit hardcoded ability implementations | All abilities must go through Effect/EffectResolver system |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| (populated during roadmap creation) | | |

**Coverage:**
- v1 requirements: 48 total
- Mapped to phases: 0
- Unmapped: 48

---
*Requirements defined: 2026-03-19*
*Last updated: 2026-03-19 after initial definition*
