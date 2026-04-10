package wp40k.domain.army

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import wp40k.domain.types.*
import wp40k.domain.models.*

class EnhancementValidatorSpec extends AnyFlatSpec with Matchers {

  val orkFaction: FactionId = FactionId("Ork")
  val detId: DetachmentId = DetachmentId("waaagh")

  val warbossId: DatasheetId = DatasheetId("000000001")
  val boyzId: DatasheetId = DatasheetId("000000002")
  val painboyId: DatasheetId = DatasheetId("000000006")

  val enhId1: EnhancementId = EnhancementId("enh1")
  val enhId2: EnhancementId = EnhancementId("enh2")
  val enhId3: EnhancementId = EnhancementId("enh3")
  val enhId4: EnhancementId = EnhancementId("enh4")
  val wrongDetEnhId: EnhancementId = EnhancementId("enh-wrong")

  val warbossDs: Datasheet = Datasheet(
    warbossId, "Warboss", Some(orkFaction), None, None,
    Some(Role.Characters), None, None, false, None, None, None, None, ""
  )
  val boyzDs: Datasheet = Datasheet(
    boyzId, "Boyz", Some(orkFaction), None, None,
    Some(Role.Battleline), None, None, false, None, None, None, None, ""
  )
  val painboyDs: Datasheet = Datasheet(
    painboyId, "Painboy", Some(orkFaction), None, None,
    Some(Role.Characters), None, None, false, None, None, None, None, ""
  )

  val datasheetIndex: Map[DatasheetId, List[Datasheet]] = List(
    warbossDs, boyzDs, painboyDs
  ).groupBy(_.id)

  val enhancements: List[Enhancement] = List(
    Enhancement(orkFaction, enhId1, "Follow Me Ladz", 25, Some("Waaagh!"), Some("waaagh"), None, "desc"),
    Enhancement(orkFaction, enhId2, "Headwoppa's Killchoppa", 20, Some("Waaagh!"), Some("waaagh"), None, "desc"),
    Enhancement(orkFaction, enhId3, "Supa-Cybork Body", 15, Some("Waaagh!"), Some("waaagh"), None, "desc"),
    Enhancement(orkFaction, enhId4, "Da Biggest Boss", 10, Some("Waaagh!"), Some("waaagh"), None, "desc"),
    Enhancement(orkFaction, wrongDetEnhId, "Wrong Det Enh", 30, Some("Bully Boyz"), Some("bully-boyz"), None, "desc")
  )

  val enhancementIndex: Map[EnhancementId, List[Enhancement]] = enhancements.groupBy(_.id)

  def baseArmy: Army = Army(
    factionId = orkFaction,
    battleSize = BattleSize.StrikeForce,
    detachmentId = detId,
    warlordId = warbossId,
    units = List(
      ArmyUnit(warbossId, 1, None, None),
      ArmyUnit(boyzId, 1, None, None)
    )
  )

  "validateCount" should "accept up to 3 enhancements" in {
    val army = baseArmy.copy(units = List(
      ArmyUnit(warbossId, 1, Some(enhId1), None),
      ArmyUnit(painboyId, 1, Some(enhId2), None),
      ArmyUnit(boyzId, 1, None, None)
    ))
    val errors = EnhancementValidator.validateCount(army)
    errors shouldBe empty
  }

  it should "accept exactly 3 enhancements" in {
    val thirdCharId: DatasheetId = DatasheetId("000000007")
    val army = baseArmy.copy(units = List(
      ArmyUnit(warbossId, 1, Some(enhId1), None),
      ArmyUnit(painboyId, 1, Some(enhId2), None),
      ArmyUnit(thirdCharId, 1, Some(enhId3), None)
    ))
    val errors = EnhancementValidator.validateCount(army)
    errors shouldBe empty
  }

  it should "reject more than 3 enhancements" in {
    val thirdCharId: DatasheetId = DatasheetId("000000007")
    val fourthCharId: DatasheetId = DatasheetId("000000008")
    val army = baseArmy.copy(units = List(
      ArmyUnit(warbossId, 1, Some(enhId1), None),
      ArmyUnit(painboyId, 1, Some(enhId2), None),
      ArmyUnit(thirdCharId, 1, Some(enhId3), None),
      ArmyUnit(fourthCharId, 1, Some(enhId4), None)
    ))
    val errors = EnhancementValidator.validateCount(army)
    errors should contain(TooManyEnhancements(4))
  }

  it should "accept an army with no enhancements" in {
    val errors = EnhancementValidator.validateCount(baseArmy)
    errors shouldBe empty
  }

  "validateUniqueness" should "accept unique enhancements" in {
    val army = baseArmy.copy(units = List(
      ArmyUnit(warbossId, 1, Some(enhId1), None),
      ArmyUnit(painboyId, 1, Some(enhId2), None)
    ))
    val errors = EnhancementValidator.validateUniqueness(army)
    errors shouldBe empty
  }

  it should "reject duplicate enhancements" in {
    val army = baseArmy.copy(units = List(
      ArmyUnit(warbossId, 1, Some(enhId1), None),
      ArmyUnit(painboyId, 1, Some(enhId1), None)
    ))
    val errors = EnhancementValidator.validateUniqueness(army)
    errors should contain(DuplicateEnhancement(enhId1))
  }

  it should "accept an army with no enhancements" in {
    val errors = EnhancementValidator.validateUniqueness(baseArmy)
    errors shouldBe empty
  }

  "validateOnCharacters" should "accept enhancements on character units" in {
    val army = baseArmy.copy(units = List(
      ArmyUnit(warbossId, 1, Some(enhId1), None),
      ArmyUnit(boyzId, 1, None, None)
    ))
    val errors = EnhancementValidator.validateOnCharacters(army, datasheetIndex)
    errors shouldBe empty
  }

  it should "reject enhancements on non-character units" in {
    val army = baseArmy.copy(units = List(
      ArmyUnit(warbossId, 1, None, None),
      ArmyUnit(boyzId, 1, Some(enhId1), None)
    ))
    val errors = EnhancementValidator.validateOnCharacters(army, datasheetIndex)
    errors should contain(EnhancementOnNonCharacter(boyzId, enhId1))
  }

  it should "skip units without enhancements" in {
    val errors = EnhancementValidator.validateOnCharacters(baseArmy, datasheetIndex)
    errors shouldBe empty
  }

  "validateDetachment" should "accept enhancements matching the army detachment" in {
    val army = baseArmy.copy(units = List(
      ArmyUnit(warbossId, 1, Some(enhId1), None),
      ArmyUnit(boyzId, 1, None, None)
    ))
    val errors = EnhancementValidator.validateDetachment(army, enhancementIndex)
    errors shouldBe empty
  }

  it should "reject enhancements from a different detachment" in {
    val army = baseArmy.copy(units = List(
      ArmyUnit(warbossId, 1, Some(wrongDetEnhId), None),
      ArmyUnit(boyzId, 1, None, None)
    ))
    val errors = EnhancementValidator.validateDetachment(army, enhancementIndex)
    errors should contain(EnhancementDetachmentMismatch(wrongDetEnhId, detId))
  }

  it should "accept enhancements with no detachment restriction" in {
    val noDetEnh = Enhancement(orkFaction, EnhancementId("free"), "Free Enh", 10, None, None, None, "desc")
    val idx = enhancementIndex + (noDetEnh.id -> List(noDetEnh))
    val army = baseArmy.copy(units = List(
      ArmyUnit(warbossId, 1, Some(noDetEnh.id), None),
      ArmyUnit(boyzId, 1, None, None)
    ))
    val errors = EnhancementValidator.validateDetachment(army, idx)
    errors shouldBe empty
  }

  it should "skip units without enhancements" in {
    val errors = EnhancementValidator.validateDetachment(baseArmy, enhancementIndex)
    errors shouldBe empty
  }
}
