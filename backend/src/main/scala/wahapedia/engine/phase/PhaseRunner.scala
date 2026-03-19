package wahapedia.engine.phase

import wahapedia.engine.state.*
import wahapedia.engine.event.*
import wahapedia.engine.effect.EffectDuration

object PhaseRunner:

  def advancePhase(state: GameState): (GameState, List[GameEvent]) =
    val endEvent = PhaseEnded(state.round, state.currentPhase, state.activePlayer)

    val nextPhaseState = state.currentPhase match
      case Phase.Command => PhaseState(Phase.Movement, SubPhase.SelectUnit)
      case Phase.Movement => PhaseState(Phase.Shooting, SubPhase.SelectShootingUnit)
      case Phase.Shooting => PhaseState(Phase.Charge, SubPhase.DeclareCharge)
      case Phase.Charge => PhaseState(Phase.Fight, SubPhase.SelectUnit)
      case Phase.Fight => PhaseState(Phase.Command, SubPhase.Start)

    val switchPlayer = state.currentPhase == Phase.Fight
    val newActivePlayer = if switchPlayer then state.inactivePlayer else state.activePlayer
    val playerIds = state.players.keys.toList.map(PlayerId.value).sorted
    val isSecondPlayer = PlayerId.value(state.activePlayer) == playerIds.last
    val newRound = if switchPlayer && isSecondPlayer then state.round + 1 else state.round

    val cleaned = cleanupPhaseEffects(state)

    val resetBoard = if switchPlayer then resetUnitsForTurn(cleaned.board) else cleaned.board

    val startEvent = PhaseStarted(newRound, nextPhaseState.phase, newActivePlayer)

    val events = List(endEvent, startEvent)
    val newState = cleaned.copy(
      round = newRound,
      phaseState = nextPhaseState,
      activePlayer = newActivePlayer,
      board = resetBoard
    ).addEvents(events)

    (newState, events)

  def startGame(state: GameState): (GameState, List[GameEvent]) =
    val events = List(
      RoundStarted(1),
      PhaseStarted(1, Phase.Command, state.activePlayer)
    )
    (state.addEvents(events), events)

  private def cleanupPhaseEffects(state: GameState): GameState =
    val updatedUnits = state.board.units.view.mapValues: unit =>
      unit.copy(activeEffects = unit.activeEffects.filterNot(_.duration == EffectDuration.UntilEndOfPhase))
    state.copy(board = state.board.copy(units = updatedUnits.toMap))

  private def resetUnitsForTurn(board: BoardState): BoardState =
    val reset = board.units.view.mapValues: unit =>
      unit.copy(
        hasMoved = false,
        hasAdvanced = false,
        hasShot = false,
        hasCharged = false,
        hasFought = false,
        hasFallenBack = false,
        battleShocked = false,
        activeEffects = unit.activeEffects.filterNot(_.duration == EffectDuration.UntilEndOfTurn)
      )
    board.copy(units = reset.toMap)
