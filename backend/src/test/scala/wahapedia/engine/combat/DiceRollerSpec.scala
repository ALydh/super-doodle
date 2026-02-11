package wahapedia.engine.combat

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DiceRollerSpec extends AnyFlatSpec with Matchers {

  "FixedDiceRoller" should "return values in order" in {
    val roller = FixedDiceRoller(1, 2, 3, 4, 5, 6)
    roller.rollD6() shouldBe 1
    roller.rollD6() shouldBe 2
    roller.rollD6() shouldBe 3
    roller.rollD6() shouldBe 4
    roller.rollD6() shouldBe 5
    roller.rollD6() shouldBe 6
  }

  it should "return 4 when queue is exhausted" in {
    val roller = FixedDiceRoller(1)
    roller.rollD6() shouldBe 1
    roller.rollD6() shouldBe 4
  }

  it should "roll 2D6 by consuming two values" in {
    val roller = FixedDiceRoller(3, 4)
    roller.roll2D6() shouldBe 7
  }

  "RandomDiceRoller" should "produce values between 1 and 6" in {
    val roller = RandomDiceRoller(Some(42L))
    val rolls = (1 to 100).map(_ => roller.rollD6())
    rolls.foreach { r =>
      r should be >= 1
      r should be <= 6
    }
  }

  it should "produce reproducible results with same seed" in {
    val roller1 = RandomDiceRoller(Some(12345L))
    val roller2 = RandomDiceRoller(Some(12345L))
    val rolls1 = (1 to 20).map(_ => roller1.rollD6())
    val rolls2 = (1 to 20).map(_ => roller2.rollD6())
    rolls1 shouldBe rolls2
  }

  "rollNotation" should "parse fixed values" in {
    val roller = FixedDiceRoller()
    roller.rollNotation("3") shouldBe 3
    roller.rollNotation("12") shouldBe 12
  }

  it should "parse D6" in {
    val roller = FixedDiceRoller(4)
    roller.rollNotation("D6") shouldBe 4
  }

  it should "parse 2D6" in {
    val roller = FixedDiceRoller(3, 5)
    roller.rollNotation("2D6") shouldBe 8
  }

  it should "parse D6+3" in {
    val roller = FixedDiceRoller(2)
    roller.rollNotation("D6+3") shouldBe 5
  }

  it should "parse D3" in {
    val roller = FixedDiceRoller(5)
    roller.rollNotation("D3") shouldBe 3
  }
}
