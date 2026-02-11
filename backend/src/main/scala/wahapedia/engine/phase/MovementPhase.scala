package wahapedia.engine.phase

import wahapedia.engine.state.*
import wahapedia.engine.command.*
import wahapedia.engine.combat.DiceRoller
import wahapedia.engine.event.*
import wahapedia.engine.spatial.{Vec3, Geometry}

object MovementPhase:

  def validateMove(state: GameState, cmd: MoveUnit): Either[GameError, Unit] =
    for
      unit <- getOwnedAliveUnit(state, cmd.unitId)
      _ <- checkNotActed(state, cmd.unitId)
      _ <- checkNotInEngagement(state, unit)
      _ <- checkMoveDistance(unit, cmd.modelPositions, 6.0)
    yield ()

  def executeMove(state: GameState, cmd: MoveUnit): (GameState, List[GameEvent]) =
    val unit = state.board.units(cmd.unitId)
    val updatedModels = unit.models.map: model =>
      cmd.modelPositions.get(model.id).fold(model)(pos => model.copy(position = pos))
    val updatedUnit = unit.copy(models = updatedModels, hasMoved = true)
    val newState = state
      .updateBoard(_.updateUnit(updatedUnit))
      .withPhaseState(state.phaseState.copy(unitsActed = state.phaseState.unitsActed + cmd.unitId))
    val event = UnitMoved(cmd.unitId, cmd.modelPositions, isAdvance = false, isFallBack = false)
    (newState.addEvent(event), List(event))

  def validateAdvance(state: GameState, cmd: AdvanceUnit, dice: DiceRoller): Either[GameError, Int] =
    for
      unit <- getOwnedAliveUnit(state, cmd.unitId)
      _ <- checkNotActed(state, cmd.unitId)
      _ <- checkNotInEngagement(state, unit)
    yield dice.rollD6()

  def executeAdvance(state: GameState, cmd: AdvanceUnit, advanceRoll: Int): (GameState, List[GameEvent]) =
    val unit = state.board.units(cmd.unitId)
    val updatedModels = unit.models.map: model =>
      cmd.modelPositions.get(model.id).fold(model)(pos => model.copy(position = pos))
    val updatedUnit = unit.copy(models = updatedModels, hasMoved = true, hasAdvanced = true)
    val newState = state
      .updateBoard(_.updateUnit(updatedUnit))
      .withPhaseState(state.phaseState.copy(unitsActed = state.phaseState.unitsActed + cmd.unitId))
    val event = UnitMoved(cmd.unitId, cmd.modelPositions, isAdvance = true, isFallBack = false)
    (newState.addEvent(event), List(event))

  def validateFallBack(state: GameState, cmd: FallBack): Either[GameError, Unit] =
    for
      unit <- getOwnedAliveUnit(state, cmd.unitId)
      _ <- checkNotActed(state, cmd.unitId)
    yield ()

  def executeFallBack(state: GameState, cmd: FallBack): (GameState, List[GameEvent]) =
    val unit = state.board.units(cmd.unitId)
    val updatedModels = unit.models.map: model =>
      cmd.modelPositions.get(model.id).fold(model)(pos => model.copy(position = pos))
    val updatedUnit = unit.copy(models = updatedModels, hasMoved = true, hasFallenBack = true)
    val newState = state
      .updateBoard(_.updateUnit(updatedUnit))
      .withPhaseState(state.phaseState.copy(unitsActed = state.phaseState.unitsActed + cmd.unitId))
    val event = UnitMoved(cmd.unitId, cmd.modelPositions, isAdvance = false, isFallBack = true)
    (newState.addEvent(event), List(event))

  def validateDeepStrike(state: GameState, cmd: DeepStrike): Either[GameError, Unit] =
    for
      unit <- getOwnedAliveUnit(state, cmd.unitId)
      _ <- Either.cond(unit.isInReserve, (), InvalidMove(cmd.unitId, "unit is not in reserve"))
      _ <- checkDeepStrikePositions(state, cmd.modelPositions)
    yield ()

  def executeDeepStrike(state: GameState, cmd: DeepStrike): (GameState, List[GameEvent]) =
    val unit = state.board.units(cmd.unitId)
    val updatedModels = unit.models.map: model =>
      cmd.modelPositions.get(model.id).fold(model)(pos => model.copy(position = pos))
    val updatedUnit = unit.copy(models = updatedModels, isInReserve = false, hasMoved = true)
    val newState = state
      .updateBoard(_.updateUnit(updatedUnit))
      .withPhaseState(state.phaseState.copy(unitsActed = state.phaseState.unitsActed + cmd.unitId))
    val event = UnitDeepStruck(cmd.unitId, cmd.modelPositions)
    (newState.addEvent(event), List(event))

  private def getOwnedAliveUnit(state: GameState, unitId: UnitId): Either[GameError, UnitState] =
    state.board.units.get(unitId) match
      case None => Left(UnitNotFound(unitId))
      case Some(u) if !u.isAlive => Left(wahapedia.engine.state.UnitDestroyed(unitId))
      case Some(u) if u.owner != state.activePlayer => Left(UnitNotOwnedByPlayer(unitId, state.activePlayer))
      case Some(u) => Right(u)

  private def checkNotActed(state: GameState, unitId: UnitId): Either[GameError, Unit] =
    if state.phaseState.unitsActed.contains(unitId) then Left(UnitAlreadyActed(unitId))
    else Right(())

  private def checkNotInEngagement(state: GameState, unit: UnitState): Either[GameError, Unit] =
    val enemies = state.board.units.values.filter(e => e.owner != unit.owner && e.isAlive)
    val inEngagement = enemies.exists(e => Geometry.anyModelInEngagementRange(unit.modelPositions, e.modelPositions))
    if inEngagement then Left(InvalidMove(unit.id, "unit is within engagement range"))
    else Right(())

  private def checkMoveDistance(unit: UnitState, positions: Map[ModelId, Vec3], maxInches: Double): Either[GameError, Unit] =
    val tooFar = unit.models.exists: model =>
      positions.get(model.id).exists(_.horizontalDistanceTo(model.position) > maxInches)
    if tooFar then Left(InvalidMove(unit.id, s"exceeds $maxInches inch movement"))
    else Right(())

  private def checkDeepStrikePositions(state: GameState, positions: Map[ModelId, Vec3]): Either[GameError, Unit] =
    val enemies = state.board.units.values.filter(e => e.owner != state.activePlayer && e.isAlive)
    val tooClose = positions.values.exists: pos =>
      enemies.exists(e => e.modelPositions.exists(_.horizontalDistanceTo(pos) < 9.0))
    if tooClose then Left(InvalidMove(UnitId(""), "deep strike within 9 inches of enemy"))
    else Right(())
