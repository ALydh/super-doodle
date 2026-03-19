package wahapedia.engine.state

enum Phase:
  case Command
  case Movement
  case Shooting
  case Charge
  case Fight

enum SubPhase:
  case Start
  case BattleShockTests
  case SelectUnit
  case MoveUnit
  case Reinforcements
  case SelectShootingUnit
  case SelectTarget
  case ResolveAttacks
  case DeclareCharge
  case ChargeRoll
  case ChargeMove
  case PileIn
  case SelectFightTarget
  case ResolveFight
  case Consolidate

case class PhaseState(
  phase: Phase,
  subPhase: SubPhase,
  actingUnit: Option[UnitId] = None,
  unitsActed: Set[UnitId] = Set.empty,
  chargeDeclaredTargets: Map[UnitId, List[UnitId]] = Map.empty,
  fightingOrder: List[UnitId] = Nil,
  eligibleChargers: Set[UnitId] = Set.empty
)
