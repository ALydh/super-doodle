package wp40k.domain.army

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import wp40k.domain.types.BattleSize

class AllyRulesSpec extends AnyFlatSpec with Matchers {

  "allowedAllies" should "return Freeblades and AssignedAgents for IMPERIUM keyword" in {
    val allies = AllyRules.allowedAllies(Set("IMPERIUM"))
    allies should contain theSameElementsAs List(AllyRules.FreebladesRule, AllyRules.AssignedAgentsRule)
  }

  it should "return Dreadblades and DaemonicPact for CHAOS keyword" in {
    val allies = AllyRules.allowedAllies(Set("CHAOS"))
    allies should contain theSameElementsAs List(AllyRules.DreadbladesRule, AllyRules.DaemonicPactRule)
  }

  it should "return all allies when both IMPERIUM and CHAOS keywords are present" in {
    val allies = AllyRules.allowedAllies(Set("IMPERIUM", "CHAOS"))
    allies should have size 4
    allies should contain(AllyRules.FreebladesRule)
    allies should contain(AllyRules.AssignedAgentsRule)
    allies should contain(AllyRules.DreadbladesRule)
    allies should contain(AllyRules.DaemonicPactRule)
  }

  it should "return no allies when no relevant keywords are present" in {
    val allies = AllyRules.allowedAllies(Set("Ork", "Infantry"))
    allies shouldBe empty
  }

  it should "match keywords case-insensitively" in {
    val allies = AllyRules.allowedAllies(Set("imperium"))
    allies should contain theSameElementsAs List(AllyRules.FreebladesRule, AllyRules.AssignedAgentsRule)
  }

  it should "return empty for an empty keyword set" in {
    val allies = AllyRules.allowedAllies(Set.empty)
    allies shouldBe empty
  }

  "limitsFor Freeblades" should "allow 1 titanic and 3 non-titanic with no points or unit cap" in {
    val limits = AllyRules.limitsFor(AllyType.Freeblades, BattleSize.StrikeForce)
    limits.maxTitanic shouldBe 1
    limits.maxNonTitanic shouldBe 3
    limits.maxPoints shouldBe None
    limits.maxUnits shouldBe None
  }

  "limitsFor Dreadblades" should "allow 1 titanic and 3 non-titanic with no points or unit cap" in {
    val limits = AllyRules.limitsFor(AllyType.Dreadblades, BattleSize.StrikeForce)
    limits.maxTitanic shouldBe 1
    limits.maxNonTitanic shouldBe 3
    limits.maxPoints shouldBe None
    limits.maxUnits shouldBe None
  }

  "limitsFor DaemonicPact" should "have no titanic, unlimited non-titanic, and points cap based on battle size" in {
    val incursion = AllyRules.limitsFor(AllyType.DaemonicPact, BattleSize.Incursion)
    incursion.maxTitanic shouldBe 0
    incursion.maxPoints shouldBe Some(250)
    incursion.maxUnits shouldBe None

    val strikeForce = AllyRules.limitsFor(AllyType.DaemonicPact, BattleSize.StrikeForce)
    strikeForce.maxPoints shouldBe Some(500)

    val onslaught = AllyRules.limitsFor(AllyType.DaemonicPact, BattleSize.Onslaught)
    onslaught.maxPoints shouldBe Some(750)
  }

  "limitsFor AssignedAgents" should "have no titanic, unlimited non-titanic, and unit cap based on battle size" in {
    val incursion = AllyRules.limitsFor(AllyType.AssignedAgents, BattleSize.Incursion)
    incursion.maxTitanic shouldBe 0
    incursion.maxUnits shouldBe Some(2)
    incursion.maxPoints shouldBe None

    val strikeForce = AllyRules.limitsFor(AllyType.AssignedAgents, BattleSize.StrikeForce)
    strikeForce.maxUnits shouldBe Some(4)

    val onslaught = AllyRules.limitsFor(AllyType.AssignedAgents, BattleSize.Onslaught)
    onslaught.maxUnits shouldBe Some(6)
  }

  "daemonicPactPointsLimit" should "return correct limits for each battle size" in {
    AllyRules.daemonicPactPointsLimit(BattleSize.Incursion) shouldBe 250
    AllyRules.daemonicPactPointsLimit(BattleSize.StrikeForce) shouldBe 500
    AllyRules.daemonicPactPointsLimit(BattleSize.Onslaught) shouldBe 750
  }

  "assignedAgentsUnitLimit" should "return correct limits for each battle size" in {
    AllyRules.assignedAgentsUnitLimit(BattleSize.Incursion) shouldBe 2
    AllyRules.assignedAgentsUnitLimit(BattleSize.StrikeForce) shouldBe 4
    AllyRules.assignedAgentsUnitLimit(BattleSize.Onslaught) shouldBe 6
  }
}
