package wahapedia.engine.command

import wahapedia.engine.state.*
import wahapedia.engine.phase.*

object CommandValidator:

  def validate(state: GameState, command: Command): Either[GameError, Unit] =
    command match
      case cmd: MoveUnit =>
        requirePhase(state, Phase.Movement).flatMap(_ => MovementPhase.validateMove(state, cmd))
      case cmd: AdvanceUnit =>
        requirePhase(state, Phase.Movement)
      case cmd: FallBack =>
        requirePhase(state, Phase.Movement).flatMap(_ => MovementPhase.validateFallBack(state, cmd))
      case cmd: DeepStrike =>
        requirePhase(state, Phase.Movement).flatMap(_ => MovementPhase.validateDeepStrike(state, cmd))
      case cmd: SelectShootingUnit =>
        requirePhase(state, Phase.Shooting).flatMap(_ => ShootingPhase.validateSelectUnit(state, cmd))
      case cmd: SelectTarget =>
        requirePhase(state, Phase.Shooting).flatMap(_ => ShootingPhase.validateSelectTarget(state, cmd))
      case cmd: DeclareCharge =>
        requirePhase(state, Phase.Charge).flatMap(_ => ChargePhase.validateDeclareCharge(state, cmd))
      case cmd: ChargeMove =>
        requirePhase(state, Phase.Charge).flatMap(_ => ChargePhase.validateChargeMove(state, cmd))
      case cmd: PileIn =>
        requirePhase(state, Phase.Fight).flatMap(_ => FightPhase.validatePileIn(state, cmd))
      case cmd: FightTarget =>
        requirePhase(state, Phase.Fight).flatMap(_ => FightPhase.validateFightTarget(state, cmd))
      case cmd: Consolidate =>
        requirePhase(state, Phase.Fight).flatMap(_ => FightPhase.validateConsolidate(state, cmd))
      case cmd: EndPhase =>
        if cmd.playerId == state.activePlayer then Right(())
        else Left(NotActivePlayer(cmd.playerId))
      case _: UseStratagem => Right(())
      case _: AllocateWound => Right(())

  private def requirePhase(state: GameState, phase: Phase): Either[GameError, Unit] =
    if state.currentPhase == phase then Right(())
    else Left(InvalidPhase(phase, state.currentPhase))
