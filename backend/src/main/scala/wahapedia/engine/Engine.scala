package wahapedia.engine

import wahapedia.engine.state.*
import wahapedia.engine.command.*
import wahapedia.engine.combat.*
import wahapedia.engine.event.*
import wahapedia.engine.phase.*
import wahapedia.engine.spatial.Vec3
import wahapedia.domain.types.{DatasheetId, FactionId, DetachmentId}

case class GameResult(winner: Option[PlayerId])

trait Engine:
  def execute(state: GameState, command: Command): Either[GameError, (GameState, List[GameEvent])]
  def validCommands(state: GameState): List[Command]
  def isGameOver(state: GameState): Option[GameResult]

class GameEngine(
  dice: DiceRoller,
  weaponResolver: WeaponResolver = DefaultWeaponResolver
) extends Engine:

  def execute(state: GameState, command: Command): Either[GameError, (GameState, List[GameEvent])] =
    for
      _ <- CommandValidator.validate(state, command)
    yield
      val (newState, events) = command match
        case _: EndPhase if state.currentPhase == Phase.Command =>
          val (afterCmd, cmdEvents) = CommandPhase.start(state, dice)
          val (afterAdvance, advanceEvents) = PhaseRunner.advancePhase(afterCmd)
          (afterAdvance, cmdEvents ++ advanceEvents)
        case other =>
          CommandExecutor.execute(state, other, dice, weaponResolver)

      val gameOverState = checkGameOver(newState) match
        case Some(result) =>
          val event = GameEnded(result.winner)
          newState.addEvent(event) -> (events :+ event)
        case None => newState -> events

      gameOverState

  def validCommands(state: GameState): List[Command] =
    state.currentPhase match
      case Phase.Command =>
        List(EndPhase(state.activePlayer))

      case Phase.Movement =>
        val moveCommands = state.board.unitsByPlayer(state.activePlayer)
          .filter(u => !state.phaseState.unitsActed.contains(u.id))
          .flatMap: unit =>
            List(
              MoveUnit(unit.id, unit.models.map(m => m.id -> m.position).toMap),
              AdvanceUnit(unit.id, unit.models.map(m => m.id -> m.position).toMap)
            )
          .toList
        moveCommands :+ EndPhase(state.activePlayer)

      case Phase.Shooting =>
        val shootCommands = state.board.unitsByPlayer(state.activePlayer)
          .filter(u => !u.hasShot && !u.hasFallenBack)
          .map(u => SelectShootingUnit(u.id))
          .toList
        shootCommands :+ EndPhase(state.activePlayer)

      case Phase.Charge =>
        val chargeCommands = state.board.unitsByPlayer(state.activePlayer)
          .filter(u => !u.hasAdvanced && !u.hasFallenBack && !u.hasCharged)
          .map(u => DeclareCharge(u.id, Nil))
          .toList
        chargeCommands :+ EndPhase(state.activePlayer)

      case Phase.Fight =>
        val fightCommands = state.board.unitsByPlayer(state.activePlayer)
          .filter(u => !u.hasFought)
          .map(u => PileIn(u.id, u.models.map(m => m.id -> m.position).toMap))
          .toList
        fightCommands :+ EndPhase(state.activePlayer)

  def isGameOver(state: GameState): Option[GameResult] = checkGameOver(state)

  private def checkGameOver(state: GameState): Option[GameResult] =
    if state.round > 5 then
      val scores = state.players.map((id, ps) => id -> ps.victoryPoints)
      val maxScore = scores.values.max
      val winners = scores.filter(_._2 == maxScore).keys.toList
      if winners.size == 1 then Some(GameResult(Some(winners.head)))
      else Some(GameResult(None))
    else
      val alivePlayers = state.players.keys.filter: pid =>
        state.board.unitsByPlayer(pid).nonEmpty
      if alivePlayers.size <= 1 then
        Some(GameResult(alivePlayers.headOption))
      else None

object GameEngine:
  def setupGame(
    player1Id: PlayerId,
    player1Faction: FactionId,
    player1Detachment: DetachmentId,
    player1Units: List[UnitState],
    player2Id: PlayerId,
    player2Faction: FactionId,
    player2Detachment: DetachmentId,
    player2Units: List[UnitState],
    terrain: List[wahapedia.engine.spatial.TerrainPiece] = Nil
  ): GameState =
    val allUnits = (player1Units ++ player2Units).map(u => u.id -> u).toMap
    GameState(
      round = 1,
      phaseState = PhaseState(Phase.Command, SubPhase.Start),
      activePlayer = player1Id,
      players = Map(
        player1Id -> PlayerState(player1Id, player1Faction, player1Detachment, commandPoints = 0, isPrimaryPlayer = true),
        player2Id -> PlayerState(player2Id, player2Faction, player2Detachment, commandPoints = 0)
      ),
      board = BoardState(
        units = allUnits,
        terrain = terrain
      )
    )
