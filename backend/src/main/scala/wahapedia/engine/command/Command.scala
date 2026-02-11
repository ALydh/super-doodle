package wahapedia.engine.command

import wahapedia.engine.state.{UnitId, ModelId, PlayerId}
import wahapedia.engine.spatial.Vec3
import wahapedia.domain.models.StratagemId

case class WeaponSelection(wargearLine: Int, lineInWargear: Int)

sealed trait Command

case class MoveUnit(unitId: UnitId, modelPositions: Map[ModelId, Vec3]) extends Command
case class AdvanceUnit(unitId: UnitId, modelPositions: Map[ModelId, Vec3]) extends Command
case class FallBack(unitId: UnitId, modelPositions: Map[ModelId, Vec3]) extends Command
case class DeepStrike(unitId: UnitId, modelPositions: Map[ModelId, Vec3]) extends Command
case class SelectShootingUnit(unitId: UnitId) extends Command
case class SelectTarget(attackerUnit: UnitId, targetUnit: UnitId, weapons: List[WeaponSelection]) extends Command
case class DeclareCharge(unitId: UnitId, targets: List[UnitId]) extends Command
case class ChargeMove(unitId: UnitId, modelPositions: Map[ModelId, Vec3]) extends Command
case class PileIn(unitId: UnitId, modelPositions: Map[ModelId, Vec3]) extends Command
case class FightTarget(unitId: UnitId, targetUnit: UnitId, weapons: List[WeaponSelection]) extends Command
case class Consolidate(unitId: UnitId, modelPositions: Map[ModelId, Vec3]) extends Command
case class UseStratagem(stratagemId: StratagemId, playerId: PlayerId, targets: List[UnitId]) extends Command
case class EndPhase(playerId: PlayerId) extends Command
case class AllocateWound(unitId: UnitId, modelId: ModelId) extends Command
