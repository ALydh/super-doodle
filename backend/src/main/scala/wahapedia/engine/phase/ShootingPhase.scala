package wahapedia.engine.phase

import wahapedia.engine.state.*
import wahapedia.engine.command.*
import wahapedia.engine.combat.*
import wahapedia.engine.effect.*
import wahapedia.engine.event.*
import wahapedia.engine.spatial.{Geometry, LineOfSight}

object ShootingPhase:

  def validateSelectUnit(state: GameState, cmd: SelectShootingUnit): Either[GameError, Unit] =
    for
      unit <- getOwnedAliveUnit(state, cmd.unitId, state.activePlayer)
      _ <- Either.cond(!unit.hasShot, (), UnitAlreadyActed(cmd.unitId))
      _ <- Either.cond(!unit.hasFallenBack, (), InvalidCommand("unit fell back and cannot shoot"))
      _ <- Either.cond(!unit.hasAdvanced || hasAssaultWeapons(unit), (), InvalidCommand("unit advanced and has no assault weapons"))
    yield ()

  def executeSelectUnit(state: GameState, cmd: SelectShootingUnit): (GameState, List[GameEvent]) =
    val ps = state.phaseState.copy(actingUnit = Some(cmd.unitId))
    val event = ShootingUnitSelected(cmd.unitId)
    (state.withPhaseState(ps).addEvent(event), List(event))

  def validateSelectTarget(
    state: GameState,
    cmd: SelectTarget
  ): Either[GameError, Unit] =
    for
      attacker <- getOwnedAliveUnit(state, cmd.attackerUnit, state.activePlayer)
      target <- getEnemyAliveUnit(state, cmd.targetUnit, state.activePlayer)
      _ <- checkRange(attacker, target, 24.0)
      _ <- checkLineOfSight(state, attacker, target)
    yield ()

  def executeAttack(
    state: GameState,
    cmd: SelectTarget,
    weaponProfiles: List[WeaponProfile],
    dice: DiceRoller
  ): (GameState, List[GameEvent]) =
    val attacker = state.board.units(cmd.attackerUnit)
    val target = state.board.units(cmd.targetUnit)

    var currentState = state
    var allEvents = List.empty[GameEvent]

    weaponProfiles.foreach: weapon =>
      val effects = EffectResolver.collectAttackEffects(currentState, attacker, target, weapon)
      val targetProfile = resolveTargetProfile(target)
      val inCover = checkCover(currentState, attacker, target)

      val ctx = AttackContext(
        attacker = attacker,
        target = target,
        weapon = weapon,
        effects = effects,
        dice = dice,
        targetToughness = targetProfile.toughness,
        targetSave = targetProfile.save,
        targetInvuln = targetProfile.invuln,
        isInCover = inCover
      )

      val result = AttackPipeline.resolve(ctx)

      val attackEvent = AttackSequence(
        attackerUnit = cmd.attackerUnit,
        targetUnit = cmd.targetUnit,
        weaponName = weapon.name,
        attacks = result.totalAttacks,
        hits = result.hits,
        wounds = result.wounds,
        unsavedWounds = result.unsavedWounds,
        damageDealt = result.totalDamage,
        mortalWoundsDealt = result.mortalWounds
      )

      val totalDamage = result.totalDamage + result.mortalWounds
      val currentTarget = currentState.board.units(cmd.targetUnit)
      val allocation = WoundAllocation.allocateDamage(currentTarget, result.totalDamage, result.mortalWounds)
      currentState = currentState.updateBoard(_.updateUnit(allocation.updatedUnit))
      allEvents = allEvents :+ attackEvent
      allEvents = allEvents ++ allocation.events

    val updatedAttacker = currentState.board.units(cmd.attackerUnit).copy(hasShot = true)
    currentState = currentState
      .updateBoard(_.updateUnit(updatedAttacker))
      .withPhaseState(currentState.phaseState.copy(unitsActed = currentState.phaseState.unitsActed + cmd.attackerUnit))

    val targetEvent = TargetSelected(cmd.attackerUnit, cmd.targetUnit)
    allEvents = targetEvent :: allEvents

    (currentState.addEvents(allEvents), allEvents)

  private case class TargetProfile(toughness: Int, save: Int, invuln: Option[Int])

  private def resolveTargetProfile(target: UnitState): TargetProfile =
    TargetProfile(toughness = 4, save = 3, invuln = None)

  private def checkCover(state: GameState, attacker: UnitState, target: UnitState): Boolean =
    attacker.modelPositions.headOption.exists: shooterPos =>
      target.modelPositions.headOption.exists: targetPos =>
        LineOfSight.isInCover(shooterPos, targetPos, state.board.terrain)

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
      case Some(u) if u.owner == activePlayer => Left(InvalidTarget("cannot target own unit"))
      case Some(u) => Right(u)

  private def checkRange(attacker: UnitState, target: UnitState, maxRange: Double): Either[GameError, Unit] =
    val dist = Geometry.closestModelDistance(attacker.modelPositions, target.modelPositions)
    if dist <= maxRange then Right(())
    else Left(OutOfRange(attacker.id, target.id, dist, maxRange))

  private def checkLineOfSight(state: GameState, attacker: UnitState, target: UnitState): Either[GameError, Unit] =
    if LineOfSight.canSeeAnyModel(attacker.modelPositions, target.modelPositions, state.board.terrain) then Right(())
    else Left(NoLineOfSight(attacker.id, target.id))

  private def hasAssaultWeapons(unit: UnitState): Boolean = true
