package wahapedia.engine.state

import wahapedia.domain.models.StratagemId
import wahapedia.engine.event.GameEvent

case class GameState(
  round: Int,
  phaseState: PhaseState,
  activePlayer: PlayerId,
  players: Map[PlayerId, PlayerState],
  board: BoardState,
  events: Vector[GameEvent] = Vector.empty,
  usedStratagems: Set[(PlayerId, StratagemId, Int)] = Set.empty
):
  def currentPhase: Phase = phaseState.phase

  def activePlayerState: PlayerState = players(activePlayer)

  def inactivePlayer: PlayerId =
    players.keys.find(_ != activePlayer).get

  def inactivePlayerState: PlayerState = players(inactivePlayer)

  def updateBoard(f: BoardState => BoardState): GameState =
    copy(board = f(board))

  def addEvent(event: GameEvent): GameState =
    copy(events = events :+ event)

  def addEvents(newEvents: List[GameEvent]): GameState =
    copy(events = events ++ newEvents)

  def updatePlayer(playerId: PlayerId, f: PlayerState => PlayerState): GameState =
    copy(players = players.updated(playerId, f(players(playerId))))

  def withPhaseState(ps: PhaseState): GameState =
    copy(phaseState = ps)
