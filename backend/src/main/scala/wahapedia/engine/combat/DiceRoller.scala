package wahapedia.engine.combat

import scala.util.Random
import scala.collection.mutable

trait DiceRoller:
  def rollD6(): Int
  def rollD3(): Int = (rollD6() + 1) / 2
  def roll2D6(): Int = rollD6() + rollD6()
  def rollD6s(n: Int): List[Int] = List.fill(n)(rollD6())

  def rollNotation(notation: String): Int =
    notation.trim match
      case s if s.forall(_.isDigit) => s.toInt
      case DiceRoller.NotationPattern(countStr, sides, plusStr) =>
        val count = if countStr == null || countStr.isEmpty then 1 else countStr.toInt
        val plus = if plusStr == null || plusStr.isEmpty then 0 else plusStr.toInt
        val rolls = (1 to count).map(_ => rollDie(sides.toInt))
        rolls.sum + plus
      case other => other.toIntOption.getOrElse(0)

  private def rollDie(sides: Int): Int =
    if sides == 6 then rollD6()
    else if sides == 3 then rollD3()
    else ((rollD6().toDouble / 6.0) * sides).ceil.toInt.max(1).min(sides)

object DiceRoller:
  val NotationPattern = """(\d*)D(\d+)(?:\+(\d+))?""".r

class RandomDiceRoller(seed: Option[Long] = None) extends DiceRoller:
  private val rng = seed.fold(new Random())(new Random(_))
  def rollD6(): Int = rng.nextInt(6) + 1

class FixedDiceRoller(results: Int*) extends DiceRoller:
  private val queue = mutable.Queue.from(results)
  def rollD6(): Int =
    if queue.isEmpty then 4
    else queue.dequeue()
  def remaining: Int = queue.size
  def enqueue(results: Int*): Unit = queue.enqueueAll(results)
