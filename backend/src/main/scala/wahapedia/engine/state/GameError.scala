package wahapedia.engine.state

sealed trait GameError:
  def message: String

case class InvalidPhase(expected: Phase, actual: Phase) extends GameError:
  def message = s"Expected phase $expected but in $actual"

case class NotActivePlayer(playerId: PlayerId) extends GameError:
  def message = s"Player ${PlayerId.value(playerId)} is not the active player"

case class UnitNotFound(unitId: UnitId) extends GameError:
  def message = s"Unit ${UnitId.value(unitId)} not found"

case class UnitNotOwnedByPlayer(unitId: UnitId, playerId: PlayerId) extends GameError:
  def message = s"Unit ${UnitId.value(unitId)} not owned by player ${PlayerId.value(playerId)}"

case class UnitAlreadyActed(unitId: UnitId) extends GameError:
  def message = s"Unit ${UnitId.value(unitId)} has already acted this phase"

case class UnitDestroyed(unitId: UnitId) extends GameError:
  def message = s"Unit ${UnitId.value(unitId)} is destroyed"

case class InvalidMove(unitId: UnitId, reason: String) extends GameError:
  def message = s"Invalid move for ${UnitId.value(unitId)}: $reason"

case class InvalidTarget(reason: String) extends GameError:
  def message = s"Invalid target: $reason"

case class InvalidCommand(reason: String) extends GameError:
  def message = s"Invalid command: $reason"

case class OutOfRange(attackerUnit: UnitId, targetUnit: UnitId, distance: Double, range: Double) extends GameError:
  def message = s"Target out of range: $distance > $range"

case class NoLineOfSight(attackerUnit: UnitId, targetUnit: UnitId) extends GameError:
  def message = s"No line of sight to target"

case class ChargeOutOfRange(unitId: UnitId, targetUnit: UnitId, distance: Double) extends GameError:
  def message = s"Charge target too far away: $distance"

case class InsufficientChargeRoll(unitId: UnitId, roll: Int, required: Double) extends GameError:
  def message = s"Charge roll $roll insufficient, needed $required"

case class NotInEngagementRange(unitId: UnitId) extends GameError:
  def message = s"Unit ${UnitId.value(unitId)} is not in engagement range"

case class StratagemAlreadyUsed(reason: String) extends GameError:
  def message = s"Stratagem already used: $reason"

case class InsufficientCP(available: Int, required: Int) extends GameError:
  def message = s"Not enough CP: have $available, need $required"
