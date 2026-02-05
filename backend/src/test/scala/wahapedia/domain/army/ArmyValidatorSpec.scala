package wahapedia.domain.army

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import wahapedia.domain.types.*
import wahapedia.domain.models.*

class ArmyValidatorSpec extends AnyFlatSpec with Matchers {

  val orkFaction: FactionId = FactionId("Ork")
  val smFaction: FactionId = FactionId("SM")
  val ikFaction: FactionId = FactionId("QI")
  val cdFaction: FactionId = FactionId("CD")

  val warbossId: DatasheetId = DatasheetId("000000001")
  val boyzId: DatasheetId = DatasheetId("000000002")
  val trukId: DatasheetId = DatasheetId("000000003")
  val meganobzId: DatasheetId = DatasheetId("000000004")
  val ghazId: DatasheetId = DatasheetId("000000005")
  val painboyId: DatasheetId = DatasheetId("000000006")
  val smCaptainId: DatasheetId = DatasheetId("000000099")
  val knightErrantId: DatasheetId = DatasheetId("000000100")
  val armigerWarglaiveId: DatasheetId = DatasheetId("000000101")
  val bloodlettersId: DatasheetId = DatasheetId("000000102")

  val detId: DetachmentId = DetachmentId("waaagh")
  val otherDetId: DetachmentId = DetachmentId("other-det")

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
  val trukDs: Datasheet = Datasheet(
    trukId, "Trukk", Some(orkFaction), None, None,
    Some(Role.DedicatedTransports), None, None, false, None, None, None, None, ""
  )
  val meganobzDs: Datasheet = Datasheet(
    meganobzId, "Meganobz", Some(orkFaction), None, None,
    Some(Role.Other), None, None, false, None, None, None, None, ""
  )
  val ghazDs: Datasheet = Datasheet(
    ghazId, "Ghazghkull Thraka", Some(orkFaction), None, None,
    Some(Role.Characters), None, None, false, None, None, None, None, ""
  )
  val painboyDs: Datasheet = Datasheet(
    painboyId, "Painboy", Some(orkFaction), None, None,
    Some(Role.Characters), None, None, false, None, None, None, None, ""
  )
  val smCaptainDs: Datasheet = Datasheet(
    smCaptainId, "Space Marine Captain", Some(smFaction), None, None,
    Some(Role.Characters), None, None, false, None, None, None, None, ""
  )
  val knightErrantDs: Datasheet = Datasheet(
    knightErrantId, "Knight Errant", Some(ikFaction), None, None,
    Some(Role.Characters), None, None, false, None, None, None, None, ""
  )
  val armigerWarglaiveDs: Datasheet = Datasheet(
    armigerWarglaiveId, "Armiger Warglaive", Some(ikFaction), None, None,
    Some(Role.Other), None, None, false, None, None, None, None, ""
  )
  val bloodlettersDs: Datasheet = Datasheet(
    bloodlettersId, "Bloodletters", Some(cdFaction), None, None,
    Some(Role.Battleline), None, None, false, None, None, None, None, ""
  )

  val allDatasheets: List[Datasheet] = List(
    warbossDs, boyzDs, trukDs, meganobzDs, ghazDs, painboyDs, smCaptainDs,
    knightErrantDs, armigerWarglaiveDs, bloodlettersDs
  )

  val orkKeywords: List[DatasheetKeyword] = List(
    DatasheetKeyword(warbossId, Some("Ork"), None, true),
    DatasheetKeyword(warbossId, Some("Infantry"), None, false),
    DatasheetKeyword(boyzId, Some("Ork"), None, true),
    DatasheetKeyword(boyzId, Some("Infantry"), None, false),
    DatasheetKeyword(trukId, Some("Ork"), None, true),
    DatasheetKeyword(meganobzId, Some("Ork"), None, true),
    DatasheetKeyword(ghazId, Some("Ork"), None, true),
    DatasheetKeyword(ghazId, Some("Epic Hero"), None, false),
    DatasheetKeyword(painboyId, Some("Ork"), None, true),
    DatasheetKeyword(smCaptainId, Some("SM"), None, true),
    DatasheetKeyword(smCaptainId, Some("IMPERIUM"), None, true),
    DatasheetKeyword(knightErrantId, Some("QI"), None, true),
    DatasheetKeyword(knightErrantId, Some("IMPERIUM"), None, true),
    DatasheetKeyword(knightErrantId, Some("Titanic"), None, false),
    DatasheetKeyword(armigerWarglaiveId, Some("QI"), None, true),
    DatasheetKeyword(armigerWarglaiveId, Some("IMPERIUM"), None, true),
    DatasheetKeyword(bloodlettersId, Some("CD"), None, true),
    DatasheetKeyword(bloodlettersId, Some("CHAOS"), None, true)
  )

  val unitCosts: List[UnitCost] = List(
    UnitCost(warbossId, 1, "1 model", 65),
    UnitCost(boyzId, 1, "10 models", 80),
    UnitCost(boyzId, 2, "20 models", 170),
    UnitCost(trukId, 1, "1 model", 55),
    UnitCost(meganobzId, 1, "2 models", 80),
    UnitCost(ghazId, 1, "1 model", 235),
    UnitCost(painboyId, 1, "1 model", 55),
    UnitCost(smCaptainId, 1, "1 model", 80),
    UnitCost(knightErrantId, 1, "1 model", 400),
    UnitCost(armigerWarglaiveId, 1, "1 model", 150),
    UnitCost(bloodlettersId, 1, "10 models", 130)
  )

  val enhancements: List[Enhancement] = List(
    Enhancement(orkFaction, enhId1, "Follow Me Ladz", 25, Some("Waaagh!"), Some("waaagh"), None, "desc"),
    Enhancement(orkFaction, enhId2, "Headwoppa's Killchoppa", 20, Some("Waaagh!"), Some("waaagh"), None, "desc"),
    Enhancement(orkFaction, enhId3, "Supa-Cybork Body", 15, Some("Waaagh!"), Some("waaagh"), None, "desc"),
    Enhancement(orkFaction, enhId4, "Da Biggest Boss", 10, Some("Waaagh!"), Some("waaagh"), None, "desc"),
    Enhancement(orkFaction, wrongDetEnhId, "Wrong Det Enh", 30, Some("Bully Boyz"), Some("bully-boyz"), None, "desc")
  )

  val leaderMappings: List[DatasheetLeader] = List(
    DatasheetLeader(warbossId, boyzId),
    DatasheetLeader(painboyId, boyzId),
    DatasheetLeader(painboyId, meganobzId)
  )

  val detachmentAbilities: List[DetachmentAbility] = List(
    DetachmentAbility(
      DetachmentAbilityId("da1"), orkFaction, "Waaagh!", None, "desc", "Waaagh!", "waaagh"
    )
  )

  def baseRef: ReferenceData = ReferenceData(
    allDatasheets, orkKeywords, unitCosts, enhancements, leaderMappings, detachmentAbilities
  )

  def validArmy: Army = Army(
    factionId = orkFaction,
    battleSize = BattleSize.StrikeForce,
    detachmentId = detId,
    warlordId = warbossId,
    units = List(
      ArmyUnit(warbossId, 1, None, None),
      ArmyUnit(boyzId, 1, None, None),
      ArmyUnit(boyzId, 1, None, None)
    )
  )

  "validate" should "accept a valid army" in {
    ArmyValidator.validate(validArmy, baseRef) shouldBe empty
  }

  // Rule 1: faction keyword
  "faction keyword validation" should "reject units without the army's faction keyword" in {
    val army = validArmy.copy(units =
      ArmyUnit(smCaptainId, 1, None, None) :: validArmy.units
    )
    val errors = ArmyValidator.validate(army, baseRef)
    errors should contain(FactionMismatch(smCaptainId, "Space Marine Captain", orkFaction))
  }

  it should "accept units that share the army's faction keyword" in {
    val errors = ArmyValidator.validate(validArmy, baseRef)
    errors.collect { case e: FactionMismatch => e } shouldBe empty
  }

  // Rule 2: points limit
  "points validation" should "reject an army that exceeds the points limit" in {
    val army = Army(
      factionId = orkFaction,
      battleSize = BattleSize.Incursion,
      detachmentId = detId,
      warlordId = warbossId,
      units = List(
        ArmyUnit(warbossId, 1, None, None),
        ArmyUnit(boyzId, 2, None, None),
        ArmyUnit(boyzId, 2, None, None),
        ArmyUnit(boyzId, 2, None, None),
        ArmyUnit(boyzId, 2, None, None),
        ArmyUnit(boyzId, 2, None, None),
        ArmyUnit(boyzId, 2, None, None)
      )
    )
    // 65 + 6*170 = 1085 > 1000
    val errors = ArmyValidator.validate(army, baseRef)
    errors should contain(PointsExceeded(1085, 1000))
  }

  it should "accept an army within points limit" in {
    val errors = ArmyValidator.validate(validArmy, baseRef)
    errors.collect { case e: PointsExceeded => e } shouldBe empty
  }

  it should "include enhancement costs in total" in {
    val army = Army(
      factionId = orkFaction,
      battleSize = BattleSize.Incursion,
      detachmentId = detId,
      warlordId = warbossId,
      units = List(
        ArmyUnit(warbossId, 1, Some(enhId1), None), // 65 + 25 = 90
        ArmyUnit(boyzId, 2, None, None),  // 170
        ArmyUnit(boyzId, 2, None, None),  // 170
        ArmyUnit(boyzId, 2, None, None),  // 170
        ArmyUnit(boyzId, 2, None, None),  // 170
        ArmyUnit(boyzId, 2, None, None),  // 170
        ArmyUnit(painboyId, 1, Some(enhId2), None) // 55 + 20 = 75
      )
    )
    // 90 + 5*170 + 75 = 1015 > 1000
    val errors = ArmyValidator.validate(army, baseRef)
    errors should contain(PointsExceeded(1015, 1000))
  }

  it should "report error for unknown unit cost line" in {
    val army = validArmy.copy(units =
      List(ArmyUnit(warbossId, 1, None, None), ArmyUnit(boyzId, 99, None, None))
    )
    val errors = ArmyValidator.validate(army, baseRef)
    errors should contain(UnitCostNotFound(boyzId, 99))
  }

  // Rule 3: at least 1 character
  "character requirement" should "reject an army with no characters" in {
    val army = Army(
      factionId = orkFaction,
      battleSize = BattleSize.StrikeForce,
      detachmentId = detId,
      warlordId = boyzId, // not a character, but tested separately
      units = List(
        ArmyUnit(boyzId, 1, None, None),
        ArmyUnit(meganobzId, 1, None, None)
      )
    )
    val errors = ArmyValidator.validate(army, baseRef)
    errors should contain(NoCharacter())
  }

  it should "accept an army with a character" in {
    val errors = ArmyValidator.validate(validArmy, baseRef)
    errors.collect { case e: NoCharacter => e } shouldBe empty
  }

  // Rule 4: warlord
  "warlord validation" should "reject warlord not in army units" in {
    val army = validArmy.copy(warlordId = painboyId)
    val errors = ArmyValidator.validate(army, baseRef)
    errors should contain(WarlordNotInArmy(painboyId))
  }

  it should "reject non-character warlord" in {
    val army = validArmy.copy(warlordId = boyzId)
    val errors = ArmyValidator.validate(army, baseRef)
    errors should contain(InvalidWarlord(boyzId))
  }

  it should "accept a character unit as warlord" in {
    val errors = ArmyValidator.validate(validArmy, baseRef)
    errors.collect { case e: InvalidWarlord => e } shouldBe empty
    errors.collect { case e: WarlordNotInArmy => e } shouldBe empty
  }

  // Rule 5: duplication limits for non-battleline/non-transport
  "duplication limits" should "reject more than 3 copies of a non-battleline unit" in {
    val army = validArmy.copy(units =
      ArmyUnit(warbossId, 1, None, None) ::
      List.fill(4)(ArmyUnit(meganobzId, 1, None, None))
    )
    val errors = ArmyValidator.validate(army, baseRef)
    errors should contain(DuplicateExceeded(meganobzId, "Meganobz", 4, 3))
  }

  it should "accept up to 3 copies of a non-battleline unit" in {
    val army = validArmy.copy(units =
      ArmyUnit(warbossId, 1, None, None) ::
      List.fill(3)(ArmyUnit(meganobzId, 1, None, None))
    )
    val errors = ArmyValidator.validate(army, baseRef)
    errors.collect { case e: DuplicateExceeded => e } shouldBe empty
  }

  // Rule 6: battleline/transport allow up to 6
  it should "accept up to 6 copies of a battleline unit" in {
    val army = validArmy.copy(units =
      ArmyUnit(warbossId, 1, None, None) ::
      List.fill(6)(ArmyUnit(boyzId, 1, None, None))
    )
    val errors = ArmyValidator.validate(army, baseRef)
    errors.collect { case e: DuplicateExceeded => e } shouldBe empty
  }

  it should "reject more than 6 copies of a battleline unit" in {
    val army = validArmy.copy(units =
      ArmyUnit(warbossId, 1, None, None) ::
      List.fill(7)(ArmyUnit(boyzId, 1, None, None))
    )
    val errors = ArmyValidator.validate(army, baseRef)
    errors should contain(DuplicateExceeded(boyzId, "Boyz", 7, 6))
  }

  it should "accept up to 6 copies of a dedicated transport" in {
    val army = validArmy.copy(units =
      ArmyUnit(warbossId, 1, None, None) ::
      List.fill(6)(ArmyUnit(trukId, 1, None, None))
    )
    val errors = ArmyValidator.validate(army, baseRef)
    errors.collect { case e: DuplicateExceeded => e } shouldBe empty
  }

  it should "reject more than 6 copies of a dedicated transport" in {
    val army = validArmy.copy(units =
      ArmyUnit(warbossId, 1, None, None) ::
      List.fill(7)(ArmyUnit(trukId, 1, None, None))
    )
    val errors = ArmyValidator.validate(army, baseRef)
    errors should contain(DuplicateExceeded(trukId, "Trukk", 7, 6))
  }

  // Rule 7: epic hero max 1
  "epic hero validation" should "reject more than 1 copy of an epic hero" in {
    val army = validArmy.copy(units =
      ArmyUnit(warbossId, 1, None, None) ::
      List.fill(2)(ArmyUnit(ghazId, 1, None, None))
    )
    val errors = ArmyValidator.validate(army, baseRef)
    errors should contain(DuplicateEpicHero(ghazId, "Ghazghkull Thraka"))
  }

  it should "accept exactly 1 copy of an epic hero" in {
    val army = validArmy.copy(units =
      ArmyUnit(warbossId, 1, None, None) ::
      ArmyUnit(ghazId, 1, None, None) ::
      List(ArmyUnit(boyzId, 1, None, None))
    )
    val errors = ArmyValidator.validate(army, baseRef)
    errors.collect { case e: DuplicateEpicHero => e } shouldBe empty
  }

  // Rule 8: leader attachment
  "leader attachment validation" should "accept valid leader-bodyguard pairing" in {
    val army = validArmy.copy(units = List(
      ArmyUnit(warbossId, 1, None, Some(boyzId)),
      ArmyUnit(boyzId, 1, None, None)
    ))
    val errors = ArmyValidator.validate(army, baseRef)
    errors.collect { case e: InvalidLeaderAttachment => e } shouldBe empty
  }

  it should "reject invalid leader-bodyguard pairing" in {
    val army = validArmy.copy(units = List(
      ArmyUnit(warbossId, 1, None, Some(meganobzId)),
      ArmyUnit(meganobzId, 1, None, None)
    ))
    val errors = ArmyValidator.validate(army, baseRef)
    errors should contain(InvalidLeaderAttachment(warbossId, meganobzId))
  }

  it should "accept painboy leading meganobz" in {
    val army = validArmy.copy(units = List(
      ArmyUnit(warbossId, 1, None, None),
      ArmyUnit(painboyId, 1, None, Some(meganobzId)),
      ArmyUnit(meganobzId, 1, None, None)
    ))
    val errors = ArmyValidator.validate(army, baseRef)
    errors.collect { case e: InvalidLeaderAttachment => e } shouldBe empty
  }

  // Rule 9: max 3 enhancements, all different
  "enhancement count validation" should "reject more than 3 enhancements" in {
    val army = validArmy.copy(units = List(
      ArmyUnit(warbossId, 1, Some(enhId1), None),
      ArmyUnit(ghazId, 1, Some(enhId2), None),
      ArmyUnit(painboyId, 1, Some(enhId3), None),
      ArmyUnit(boyzId, 1, None, None),
      ArmyUnit(DatasheetId("000000007"), 1, Some(enhId4), None)
    ))
    // need a 4th character with an enhancement
    val extraCharDs = Datasheet(
      DatasheetId("000000007"), "Weirdboy", Some(orkFaction), None, None,
      Some(Role.Characters), None, None, false, None, None, None, None, ""
    )
    val ref = baseRef.copy(
      datasheets = extraCharDs :: baseRef.datasheets,
      unitCosts = UnitCost(DatasheetId("000000007"), 1, "1 model", 50) :: baseRef.unitCosts,
      keywords = DatasheetKeyword(DatasheetId("000000007"), Some("Ork"), None, true) :: baseRef.keywords
    )
    val errors = ArmyValidator.validate(army, ref)
    errors should contain(TooManyEnhancements(4))
  }

  it should "accept exactly 3 enhancements" in {
    val army = validArmy.copy(units = List(
      ArmyUnit(warbossId, 1, Some(enhId1), None),
      ArmyUnit(ghazId, 1, Some(enhId2), None),
      ArmyUnit(painboyId, 1, Some(enhId3), None),
      ArmyUnit(boyzId, 1, None, None)
    ))
    val errors = ArmyValidator.validate(army, baseRef)
    errors.collect { case e: TooManyEnhancements => e } shouldBe empty
  }

  "enhancement uniqueness" should "reject duplicate enhancements" in {
    val army = validArmy.copy(units = List(
      ArmyUnit(warbossId, 1, Some(enhId1), None),
      ArmyUnit(painboyId, 1, Some(enhId1), None),
      ArmyUnit(boyzId, 1, None, None)
    ))
    val errors = ArmyValidator.validate(army, baseRef)
    errors should contain(DuplicateEnhancement(enhId1))
  }

  it should "accept all different enhancements" in {
    val army = validArmy.copy(units = List(
      ArmyUnit(warbossId, 1, Some(enhId1), None),
      ArmyUnit(painboyId, 1, Some(enhId2), None),
      ArmyUnit(boyzId, 1, None, None)
    ))
    val errors = ArmyValidator.validate(army, baseRef)
    errors.collect { case e: DuplicateEnhancement => e } shouldBe empty
  }

  // Rule 10: enhancements only on characters
  "enhancement on character" should "reject enhancement on non-character unit" in {
    val army = validArmy.copy(units = List(
      ArmyUnit(warbossId, 1, None, None),
      ArmyUnit(boyzId, 1, Some(enhId1), None) // boyz are battleline, not characters
    ))
    val errors = ArmyValidator.validate(army, baseRef)
    errors should contain(EnhancementOnNonCharacter(boyzId, enhId1))
  }

  it should "accept enhancement on character unit" in {
    val army = validArmy.copy(units = List(
      ArmyUnit(warbossId, 1, Some(enhId1), None),
      ArmyUnit(boyzId, 1, None, None)
    ))
    val errors = ArmyValidator.validate(army, baseRef)
    errors.collect { case e: EnhancementOnNonCharacter => e } shouldBe empty
  }

  // Rule 11: enhancements must belong to chosen detachment
  "enhancement detachment" should "reject enhancement from wrong detachment" in {
    val army = validArmy.copy(units = List(
      ArmyUnit(warbossId, 1, Some(wrongDetEnhId), None),
      ArmyUnit(boyzId, 1, None, None)
    ))
    val errors = ArmyValidator.validate(army, baseRef)
    errors should contain(EnhancementDetachmentMismatch(wrongDetEnhId, detId))
  }

  it should "accept enhancement from the correct detachment" in {
    val army = validArmy.copy(units = List(
      ArmyUnit(warbossId, 1, Some(enhId1), None),
      ArmyUnit(boyzId, 1, None, None)
    ))
    val errors = ArmyValidator.validate(army, baseRef)
    errors.collect { case e: EnhancementDetachmentMismatch => e } shouldBe empty
  }

  // Allied units tests
  def imperiumArmy: Army = Army(
    factionId = smFaction,
    battleSize = BattleSize.StrikeForce,
    detachmentId = detId,
    warlordId = smCaptainId,
    units = List(
      ArmyUnit(smCaptainId, 1, None, None)
    )
  )

  "allied unit validation" should "skip faction keyword check for allied units" in {
    val army = imperiumArmy.copy(units = List(
      ArmyUnit(smCaptainId, 1, None, None),
      ArmyUnit(knightErrantId, 1, None, None, List.empty, isAllied = true)
    ))
    val errors = ArmyValidator.validate(army, baseRef)
    errors.collect { case e: FactionMismatch => e } shouldBe empty
  }

  it should "reject allied unit as warlord" in {
    val army = imperiumArmy.copy(
      warlordId = knightErrantId,
      units = List(
        ArmyUnit(smCaptainId, 1, None, None),
        ArmyUnit(knightErrantId, 1, None, None, List.empty, isAllied = true)
      )
    )
    val errors = ArmyValidator.validate(army, baseRef)
    errors should contain(AlliedWarlord(knightErrantId))
  }

  it should "reject enhancement on allied unit" in {
    val army = imperiumArmy.copy(units = List(
      ArmyUnit(smCaptainId, 1, None, None),
      ArmyUnit(knightErrantId, 1, Some(enhId1), None, List.empty, isAllied = true)
    ))
    val errors = ArmyValidator.validate(army, baseRef)
    errors should contain(AlliedEnhancement(knightErrantId, enhId1))
  }

  it should "reject allied units from non-allowed factions" in {
    val army = validArmy.copy(units = List(
      ArmyUnit(warbossId, 1, None, None),
      ArmyUnit(knightErrantId, 1, None, None, List.empty, isAllied = true)
    ))
    val errors = ArmyValidator.validate(army, baseRef)
    errors should contain(AlliedFactionNotAllowed(knightErrantId, ikFaction))
  }

  it should "accept allied Imperial Knights for IMPERIUM army" in {
    val army = imperiumArmy.copy(units = List(
      ArmyUnit(smCaptainId, 1, None, None),
      ArmyUnit(armigerWarglaiveId, 1, None, None, List.empty, isAllied = true)
    ))
    val errors = ArmyValidator.validate(army, baseRef)
    errors.collect { case e: AlliedFactionNotAllowed => e } shouldBe empty
  }

  it should "reject more than 1 Titanic allied unit for Freeblades" in {
    val army = imperiumArmy.copy(units = List(
      ArmyUnit(smCaptainId, 1, None, None),
      ArmyUnit(knightErrantId, 1, None, None, List.empty, isAllied = true),
      ArmyUnit(knightErrantId, 1, None, None, List.empty, isAllied = true)
    ))
    val errors = ArmyValidator.validate(army, baseRef)
    errors.collect { case e: AlliedUnitLimitExceeded => e } should not be empty
  }

  it should "accept up to 3 non-Titanic allied units for Freeblades" in {
    val army = imperiumArmy.copy(units = List(
      ArmyUnit(smCaptainId, 1, None, None),
      ArmyUnit(armigerWarglaiveId, 1, None, None, List.empty, isAllied = true),
      ArmyUnit(armigerWarglaiveId, 1, None, None, List.empty, isAllied = true),
      ArmyUnit(armigerWarglaiveId, 1, None, None, List.empty, isAllied = true)
    ))
    val errors = ArmyValidator.validate(army, baseRef)
    errors.collect { case e: AlliedUnitLimitExceeded => e } shouldBe empty
  }

  it should "reject more than 3 non-Titanic allied units for Freeblades" in {
    val army = imperiumArmy.copy(units = List(
      ArmyUnit(smCaptainId, 1, None, None),
      ArmyUnit(armigerWarglaiveId, 1, None, None, List.empty, isAllied = true),
      ArmyUnit(armigerWarglaiveId, 1, None, None, List.empty, isAllied = true),
      ArmyUnit(armigerWarglaiveId, 1, None, None, List.empty, isAllied = true),
      ArmyUnit(armigerWarglaiveId, 1, None, None, List.empty, isAllied = true)
    ))
    val errors = ArmyValidator.validate(army, baseRef)
    errors.collect { case e: AlliedUnitLimitExceeded => e } should not be empty
  }
}
