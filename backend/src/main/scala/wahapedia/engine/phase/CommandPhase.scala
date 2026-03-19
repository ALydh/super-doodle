package wahapedia.engine.phase

import wahapedia.engine.state.*
import wahapedia.engine.command.*
import wahapedia.engine.combat.DiceRoller
import wahapedia.engine.event.*

object CommandPhase:

  def start(state: GameState, dice: DiceRoller): (GameState, List[GameEvent]) =
    val (afterCp, cpEvents) = gainCommandPoints(state)
    val (afterShock, shockEvents) = testBattleShock(afterCp, dice)
    (afterShock, cpEvents ++ shockEvents)

  private def gainCommandPoints(state: GameState): (GameState, List[GameEvent]) =
    val playerId = state.activePlayer
    val newCp = state.activePlayerState.commandPoints + 1
    val updated = state.updatePlayer(playerId, _.copy(commandPoints = newCp))
    val event = CommandPointsGained(playerId, 1, newCp)
    (updated, List(event))

  private def testBattleShock(state: GameState, dice: DiceRoller): (GameState, List[GameEvent]) =
    val playerId = state.activePlayer
    val units = state.board.unitsByPlayer(playerId)
    val belowHalf = units.filter: unit =>
      val totalWounds = unit.models.size
      val aliveCount = unit.aliveModels.size
      aliveCount > 0 && aliveCount < (totalWounds + 1) / 2

    var currentState = state
    var events = List.empty[GameEvent]

    belowHalf.foreach: unit =>
      val roll = dice.roll2D6()
      val ld = parseLeadership(unit)
      val passed = roll >= ld
      val updatedUnit = if passed then unit else unit.copy(battleShocked = true)
      currentState = currentState.updateBoard(_.updateUnit(updatedUnit))
      events = events :+ BattleShockTested(unit.id, roll, ld, passed)

    (currentState, events)

  private def parseLeadership(unit: UnitState): Int = 7
