package wahapedia.engine.event

import wahapedia.engine.state.{UnitId, ModelId, PlayerId, Phase}
import wahapedia.engine.spatial.Vec3

sealed trait GameEvent

case class PhaseStarted(round: Int, phase: Phase, activePlayer: PlayerId) extends GameEvent
case class PhaseEnded(round: Int, phase: Phase, activePlayer: PlayerId) extends GameEvent
case class RoundStarted(round: Int) extends GameEvent
case class RoundEnded(round: Int) extends GameEvent

case class CommandPointsGained(playerId: PlayerId, amount: Int, newTotal: Int) extends GameEvent
case class BattleShockTested(unitId: UnitId, roll: Int, leadership: Int, passed: Boolean) extends GameEvent

case class UnitMoved(unitId: UnitId, newPositions: Map[ModelId, Vec3], isAdvance: Boolean, isFallBack: Boolean) extends GameEvent
case class UnitDeepStruck(unitId: UnitId, positions: Map[ModelId, Vec3]) extends GameEvent

case class ShootingUnitSelected(unitId: UnitId) extends GameEvent
case class TargetSelected(attackerUnit: UnitId, targetUnit: UnitId) extends GameEvent

case class AttackSequence(
  attackerUnit: UnitId,
  targetUnit: UnitId,
  weaponName: String,
  attacks: Int,
  hits: Int,
  wounds: Int,
  unsavedWounds: Int,
  damageDealt: Int,
  mortalWoundsDealt: Int
) extends GameEvent

case class ModelDestroyed(unitId: UnitId, modelId: ModelId) extends GameEvent
case class UnitDestroyed(unitId: UnitId) extends GameEvent
case class WoundAllocated(unitId: UnitId, modelId: ModelId, damage: Int, woundsRemaining: Int) extends GameEvent

case class ChargeDeclared(unitId: UnitId, targets: List[UnitId]) extends GameEvent
case class ChargeRolled(unitId: UnitId, roll: Int, succeeded: Boolean) extends GameEvent
case class ChargeMoveCompleted(unitId: UnitId) extends GameEvent

case class PileInCompleted(unitId: UnitId) extends GameEvent
case class ConsolidateCompleted(unitId: UnitId) extends GameEvent

case class HazardousTestFailed(unitId: UnitId, modelId: ModelId, mortalWounds: Int) extends GameEvent
case class DeadlyDemiseTriggered(unitId: UnitId, roll: Int, mortalWounds: Int) extends GameEvent

case class GameEnded(winner: Option[PlayerId]) extends GameEvent
