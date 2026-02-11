package wahapedia.engine.phase

import wahapedia.engine.state.*
import wahapedia.engine.command.*
import wahapedia.engine.combat.*
import wahapedia.engine.effect.*
import wahapedia.engine.event.*
import wahapedia.engine.spatial.{Geometry, Vec3}

object FightPhase:

  def validatePileIn(state: GameState, cmd: PileIn): Either[GameError, Unit] =
    for
      unit <- getOwnedAliveUnit(state, cmd.unitId, state.activePlayer)
      _ <- checkPileInDistance(unit, cmd.modelPositions, 3.0)
    yield ()

  def executePileIn(state: GameState, cmd: PileIn): (GameState, List[GameEvent]) =
    val unit = state.board.units(cmd.unitId)
    val updatedModels = unit.models.map: model =>
      cmd.modelPositions.get(model.id).fold(model)(pos => model.copy(position = pos))
    val updatedUnit = unit.copy(models = updatedModels)
    val event = PileInCompleted(cmd.unitId)
    (state.updateBoard(_.updateUnit(updatedUnit)).addEvent(event), List(event))

  def validateFightTarget(state: GameState, cmd: FightTarget): Either[GameError, Unit] =
    for
      unit <- getOwnedAliveUnit(state, cmd.unitId, state.activePlayer)
      target <- getEnemyAliveUnit(state, cmd.targetUnit, state.activePlayer)
      _ <- checkEngagement(unit, target)
    yield ()

  def executeFight(
    state: GameState,
    cmd: FightTarget,
    weaponProfiles: List[WeaponProfile],
    dice: DiceRoller
  ): (GameState, List[GameEvent]) =
    val attacker = state.board.units(cmd.unitId)
    val target = state.board.units(cmd.targetUnit)

    var currentState = state
    var allEvents = List.empty[GameEvent]

    weaponProfiles.foreach: weapon =>
      val effects = EffectResolver.collectAttackEffects(currentState, attacker, target, weapon)
      val ctx = AttackContext(
        attacker = attacker,
        target = currentState.board.units(cmd.targetUnit),
        weapon = weapon,
        effects = effects,
        dice = dice,
        targetToughness = 4,
        targetSave = 3,
        targetInvuln = None,
        isInCover = false
      )

      val result = AttackPipeline.resolve(ctx)
      val attackEvent = AttackSequence(
        attackerUnit = cmd.unitId,
        targetUnit = cmd.targetUnit,
        weaponName = weapon.name,
        attacks = result.totalAttacks,
        hits = result.hits,
        wounds = result.wounds,
        unsavedWounds = result.unsavedWounds,
        damageDealt = result.totalDamage,
        mortalWoundsDealt = result.mortalWounds
      )

      val currentTarget = currentState.board.units(cmd.targetUnit)
      val allocation = WoundAllocation.allocateDamage(currentTarget, result.totalDamage, result.mortalWounds)
      currentState = currentState.updateBoard(_.updateUnit(allocation.updatedUnit))
      allEvents = allEvents :+ attackEvent
      allEvents = allEvents ++ allocation.events

    val updatedAttacker = currentState.board.units(cmd.unitId).copy(hasFought = true)
    currentState = currentState
      .updateBoard(_.updateUnit(updatedAttacker))
      .withPhaseState(currentState.phaseState.copy(unitsActed = currentState.phaseState.unitsActed + cmd.unitId))

    (currentState.addEvents(allEvents), allEvents)

  def validateConsolidate(state: GameState, cmd: Consolidate): Either[GameError, Unit] =
    for
      unit <- getOwnedAliveUnit(state, cmd.unitId, state.activePlayer)
      _ <- checkPileInDistance(unit, cmd.modelPositions, 3.0)
    yield ()

  def executeConsolidate(state: GameState, cmd: Consolidate): (GameState, List[GameEvent]) =
    val unit = state.board.units(cmd.unitId)
    val updatedModels = unit.models.map: model =>
      cmd.modelPositions.get(model.id).fold(model)(pos => model.copy(position = pos))
    val updatedUnit = unit.copy(models = updatedModels)
    val event = ConsolidateCompleted(cmd.unitId)
    (state.updateBoard(_.updateUnit(updatedUnit)).addEvent(event), List(event))

  private def getOwnedAliveUnit(state: GameState, unitId: UnitId, playerId: PlayerId): Either[GameError, UnitState] =
    state.board.units.get(unitId) match
      case None => Left(UnitNotFound(unitId))
      case Some(u) if !u.isAlive => Left(wahapedia.engine.state.UnitDestroyed(unitId))
      case Some(u) if u.owner != playerId => Left(UnitNotOwnedByPlayer(unitId, playerId))
      case Some(u) => Right(u)

  private def getEnemyAliveUnit(state: GameState, unitId: UnitId, activePlayer: PlayerId): Either[GameError, UnitState] =
    state.board.units.get(unitId) match
      case None => Left(UnitNotFound(unitId))
      case Some(u) if !u.isAlive => Left(wahapedia.engine.state.UnitDestroyed(unitId))
      case Some(u) if u.owner == activePlayer => Left(InvalidTarget("cannot fight own unit"))
      case Some(u) => Right(u)

  private def checkEngagement(attacker: UnitState, target: UnitState): Either[GameError, Unit] =
    if Geometry.anyModelInEngagementRange(attacker.modelPositions, target.modelPositions) then Right(())
    else Left(NotInEngagementRange(attacker.id))

  private def checkPileInDistance(unit: UnitState, positions: Map[ModelId, Vec3], maxInches: Double): Either[GameError, Unit] =
    val tooFar = unit.models.exists: model =>
      positions.get(model.id).exists(_.horizontalDistanceTo(model.position) > maxInches)
    if tooFar then Left(InvalidMove(unit.id, s"exceeds $maxInches inch pile-in/consolidate"))
    else Right(())
