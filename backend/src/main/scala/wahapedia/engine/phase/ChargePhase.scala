package wahapedia.engine.phase

import wahapedia.engine.state.*
import wahapedia.engine.command.*
import wahapedia.engine.combat.DiceRoller
import wahapedia.engine.event.*
import wahapedia.engine.spatial.Geometry

object ChargePhase:

  def validateDeclareCharge(state: GameState, cmd: DeclareCharge): Either[GameError, Unit] =
    for
      unit <- getOwnedAliveUnit(state, cmd.unitId, state.activePlayer)
      _ <- Either.cond(!unit.hasAdvanced, (), InvalidCommand("unit advanced and cannot charge"))
      _ <- Either.cond(!unit.hasFallenBack, (), InvalidCommand("unit fell back and cannot charge"))
      _ <- checkTargetsInRange(state, unit, cmd.targets, 12.0)
    yield ()

  def executeDeclareCharge(state: GameState, cmd: DeclareCharge): (GameState, List[GameEvent]) =
    val ps = state.phaseState.copy(
      chargeDeclaredTargets = state.phaseState.chargeDeclaredTargets + (cmd.unitId -> cmd.targets),
      eligibleChargers = state.phaseState.eligibleChargers + cmd.unitId
    )
    val event = ChargeDeclared(cmd.unitId, cmd.targets)
    (state.withPhaseState(ps).addEvent(event), List(event))

  def rollCharge(state: GameState, unitId: UnitId, dice: DiceRoller): (GameState, List[GameEvent]) =
    val roll = dice.roll2D6()
    val unit = state.board.units(unitId)
    val targets = state.phaseState.chargeDeclaredTargets.getOrElse(unitId, Nil)

    val minDistance = targets.flatMap: tid =>
      state.board.units.get(tid).map: target =>
        Geometry.closestModelDistance(unit.modelPositions, target.modelPositions)
    .minOption.getOrElse(Double.MaxValue)

    val succeeded = roll >= minDistance.ceil.toInt
    val event = ChargeRolled(unitId, roll, succeeded)

    val ps = if !succeeded then
      state.phaseState.copy(eligibleChargers = state.phaseState.eligibleChargers - unitId)
    else state.phaseState

    (state.withPhaseState(ps).addEvent(event), List(event))

  def validateChargeMove(state: GameState, cmd: ChargeMove): Either[GameError, Unit] =
    for
      unit <- getOwnedAliveUnit(state, cmd.unitId, state.activePlayer)
      _ <- Either.cond(
        state.phaseState.eligibleChargers.contains(cmd.unitId),
        (),
        InvalidCommand("unit did not successfully charge")
      )
      _ <- checkChargeMoveLegality(state, unit, cmd)
    yield ()

  def executeChargeMove(state: GameState, cmd: ChargeMove): (GameState, List[GameEvent]) =
    val unit = state.board.units(cmd.unitId)
    val updatedModels = unit.models.map: model =>
      cmd.modelPositions.get(model.id).fold(model)(pos => model.copy(position = pos))
    val updatedUnit = unit.copy(models = updatedModels, hasCharged = true)
    val newState = state
      .updateBoard(_.updateUnit(updatedUnit))
      .withPhaseState(state.phaseState.copy(
        unitsActed = state.phaseState.unitsActed + cmd.unitId,
        eligibleChargers = state.phaseState.eligibleChargers - cmd.unitId
      ))
    val event = ChargeMoveCompleted(cmd.unitId)
    (newState.addEvent(event), List(event))

  private def getOwnedAliveUnit(state: GameState, unitId: UnitId, playerId: PlayerId): Either[GameError, UnitState] =
    state.board.units.get(unitId) match
      case None => Left(UnitNotFound(unitId))
      case Some(u) if !u.isAlive => Left(wahapedia.engine.state.UnitDestroyed(unitId))
      case Some(u) if u.owner != playerId => Left(UnitNotOwnedByPlayer(unitId, playerId))
      case Some(u) => Right(u)

  private def checkTargetsInRange(state: GameState, unit: UnitState, targets: List[UnitId], maxRange: Double): Either[GameError, Unit] =
    val outOfRange = targets.find: tid =>
      state.board.units.get(tid).exists: target =>
        Geometry.closestModelDistance(unit.modelPositions, target.modelPositions) > maxRange
    outOfRange match
      case Some(tid) => Left(ChargeOutOfRange(unit.id, tid, maxRange))
      case None => Right(())

  private def checkChargeMoveLegality(state: GameState, unit: UnitState, cmd: ChargeMove): Either[GameError, Unit] =
    val targets = state.phaseState.chargeDeclaredTargets.getOrElse(cmd.unitId, Nil)
    val anyInEngagement = targets.exists: tid =>
      state.board.units.get(tid).exists: target =>
        cmd.modelPositions.values.exists: pos =>
          target.modelPositions.exists(tp => Geometry.isWithinEngagementRange(pos, tp))
    if anyInEngagement then Right(())
    else Left(InvalidMove(cmd.unitId, "charge move must end within engagement range of a target"))
