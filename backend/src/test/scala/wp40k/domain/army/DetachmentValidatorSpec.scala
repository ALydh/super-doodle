package wp40k.domain.army

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import wp40k.domain.types.*
import wp40k.domain.models.Detachment

class DetachmentValidatorSpec extends AnyFlatSpec with Matchers {

  val faction: FactionId = FactionId("SM")
  val warlordId: DatasheetId = DatasheetId("000000001")

  val gladius: Detachment = Detachment(DetachmentId("gladius"), faction, "Gladius", 3, Some("Codex"), List("Attackers"))
  val ironstorm: Detachment = Detachment(DetachmentId("ironstorm"), faction, "Ironstorm", 2, Some("Armoured"), List("Vanguard"))
  val anvil: Detachment = Detachment(DetachmentId("anvil"), faction, "Anvil", 2, Some("Armoured"), List("Defenders"))
  val firestorm: Detachment = Detachment(DetachmentId("firestorm"), faction, "Firestorm", 1, Some("Assault"), List("Attackers"))

  val index: Map[DetachmentId, Detachment] =
    List(gladius, ironstorm, anvil, firestorm).map(d => d.id -> d).toMap

  def army(detachments: List[DetachmentId], size: BattleSize = BattleSize.StrikeForce): Army =
    Army(faction, size, detachments, warlordId, List.empty)

  "validate" should "accept a single detachment within budget" in {
    DetachmentValidator.validate(army(List(gladius.id)), index) shouldBe empty
  }

  it should "reject an army with no detachment" in {
    DetachmentValidator.validate(army(Nil), index) should contain(NoDetachment())
  }

  it should "accept two cheap detachments that fit the budget" in {
    val errors = DetachmentValidator.validate(army(List(ironstorm.id, firestorm.id)), index)
    errors shouldBe empty
  }

  it should "reject detachments exceeding the DP budget" in {
    val errors = DetachmentValidator.validate(army(List(gladius.id, ironstorm.id)), index)
    errors should contain(DetachmentPointsExceeded(5, 3))
  }

  it should "reject detachments sharing a keyword" in {
    val errors = DetachmentValidator.validate(army(List(ironstorm.id, anvil.id)), index)
    errors should contain(DetachmentKeywordConflict("Armoured"))
  }

  it should "allow a single 3 DP detachment at Strike Force" in {
    DetachmentValidator.validate(army(List(gladius.id)), index) shouldBe empty
  }

  it should "reject a 3 DP detachment at Incursion" in {
    val errors = DetachmentValidator.validate(army(List(gladius.id), BattleSize.Incursion), index)
    errors should contain(DetachmentPointsExceeded(3, 2))
  }

  it should "default unseeded detachments to the default DP cost" in {
    val errors = DetachmentValidator.validate(army(List(DetachmentId("unknown"))), index)
    errors shouldBe empty
  }
}
