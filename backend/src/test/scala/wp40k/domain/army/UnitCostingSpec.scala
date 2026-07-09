package wp40k.domain.army

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import wp40k.domain.types.DatasheetId
import wp40k.domain.models.UnitCost

class UnitCostingSpec extends AnyFlatSpec with Matchers {

  private def unit(ds: String, line: Int = 1): ArmyUnit =
    ArmyUnit(DatasheetId(ds), line, None, None)

  private def cost(ds: String, line: Int, c: Int, min: Int, max: Option[Int]): UnitCost =
    UnitCost(DatasheetId(ds), line, s"$line models", c, min, max)

  "withOrdinals" should "number repeated datasheets per occurrence" in {
    val ordinals = UnitCosting.withOrdinals(List(unit("A"), unit("B"), unit("A"), unit("A"))).map(_._2)
    ordinals shouldBe List(1, 1, 2, 3)
  }

  "costFor" should "pick the first tier for the 1st unit" in {
    val costs = List(cost("A", 1, 240, 1, Some(1)), cost("A", 1, 260, 2, None))
    UnitCosting.costFor(costs, 1, 1).map(_.cost) shouldBe Some(240)
  }

  it should "pick the escalated tier for the 2nd and later units" in {
    val costs = List(cost("A", 1, 240, 1, Some(1)), cost("A", 1, 260, 2, None))
    UnitCosting.costFor(costs, 1, 2).map(_.cost) shouldBe Some(260)
    UnitCosting.costFor(costs, 1, 9).map(_.cost) shouldBe Some(260)
  }

  it should "handle 1st-to-2nd versus 3rd+ tiers" in {
    val costs = List(cost("A", 1, 80, 1, Some(2)), cost("A", 1, 90, 3, None))
    UnitCosting.costFor(costs, 1, 2).map(_.cost) shouldBe Some(80)
    UnitCosting.costFor(costs, 1, 3).map(_.cost) shouldBe Some(90)
  }

  it should "fall back to the base row when no tier range matches" in {
    val costs = List(cost("A", 1, 100, 1, None))
    UnitCosting.costFor(costs, 1, 5).map(_.cost) shouldBe Some(100)
  }

  it should "return None when the line is unknown" in {
    val costs = List(cost("A", 1, 100, 1, None))
    UnitCosting.costFor(costs, 2, 1) shouldBe None
  }
}
