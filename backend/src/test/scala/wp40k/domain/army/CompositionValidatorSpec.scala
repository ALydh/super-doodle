package wp40k.domain.army

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import wp40k.domain.types.*
import wp40k.domain.models.*

class CompositionValidatorSpec extends AnyFlatSpec with Matchers {

  val orkFaction: FactionId = FactionId("Ork")
  val smFaction: FactionId = FactionId("SM")

  val warbossId: DatasheetId = DatasheetId("000000001")
  val boyzId: DatasheetId = DatasheetId("000000002")
  val trukId: DatasheetId = DatasheetId("000000003")
  val meganobzId: DatasheetId = DatasheetId("000000004")
  val ghazId: DatasheetId = DatasheetId("000000005")
  val smCaptainId: DatasheetId = DatasheetId("000000099")
  val unknownId: DatasheetId = DatasheetId("000000999")

  val detId: DetachmentId = DetachmentId("waaagh")

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
  val smCaptainDs: Datasheet = Datasheet(
    smCaptainId, "Space Marine Captain", Some(smFaction), None, None,
    Some(Role.Characters), None, None, false, None, None, None, None, ""
  )

  val datasheetIndex: Map[DatasheetId, List[Datasheet]] = List(
    warbossDs, boyzDs, trukDs, meganobzDs, ghazDs, smCaptainDs
  ).groupBy(_.id)

  val orkKeywords: List[DatasheetKeyword] = List(
    DatasheetKeyword(warbossId, Some("Ork"), None, true),
    DatasheetKeyword(boyzId, Some("Ork"), None, true),
    DatasheetKeyword(trukId, Some("Ork"), None, true),
    DatasheetKeyword(meganobzId, Some("Ork"), None, true),
    DatasheetKeyword(ghazId, Some("Ork"), None, true),
    DatasheetKeyword(ghazId, Some("Epic Hero"), None, false),
    DatasheetKeyword(smCaptainId, Some("SM"), None, true)
  )

  val keywordIndex: Map[DatasheetId, List[DatasheetKeyword]] = orkKeywords.groupBy(_.datasheetId)

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

  "validateFactionKeywords" should "accept units with the army's faction keyword" in {
    val errors = CompositionValidator.validateFactionKeywords(baseArmy, datasheetIndex, keywordIndex)
    errors shouldBe empty
  }

  it should "reject units that lack the army's faction keyword" in {
    val army = baseArmy.copy(units = baseArmy.units :+ ArmyUnit(smCaptainId, 1, None, None))
    val errors = CompositionValidator.validateFactionKeywords(army, datasheetIndex, keywordIndex)
    errors should contain(FactionMismatch(smCaptainId, "Space Marine Captain", orkFaction))
  }

  it should "skip allied units" in {
    val army = baseArmy.copy(units = baseArmy.units :+ ArmyUnit(smCaptainId, 1, None, None, isAllied = true))
    val errors = CompositionValidator.validateFactionKeywords(army, datasheetIndex, keywordIndex)
    errors shouldBe empty
  }

  it should "accept units that match via datasheet factionId fallback" in {
    val emptyKeywordIndex: Map[DatasheetId, List[DatasheetKeyword]] = Map.empty
    val army = baseArmy.copy(units = List(ArmyUnit(warbossId, 1, None, None)))
    val errors = CompositionValidator.validateFactionKeywords(army, datasheetIndex, emptyKeywordIndex)
    errors shouldBe empty
  }

  it should "use Unknown name for missing datasheet" in {
    val army = baseArmy.copy(units = List(ArmyUnit(unknownId, 1, None, None)))
    val errors = CompositionValidator.validateFactionKeywords(army, datasheetIndex, keywordIndex)
    errors should contain(FactionMismatch(unknownId, "Unknown", orkFaction))
  }

  "validateCharacterRequirement" should "accept an army with a character" in {
    val errors = CompositionValidator.validateCharacterRequirement(baseArmy, datasheetIndex)
    errors shouldBe empty
  }

  it should "reject an army with no characters" in {
    val army = baseArmy.copy(units = List(ArmyUnit(boyzId, 1, None, None), ArmyUnit(meganobzId, 1, None, None)))
    val errors = CompositionValidator.validateCharacterRequirement(army, datasheetIndex)
    errors should contain(NoCharacter())
  }

  it should "reject an army with an empty unit list" in {
    val army = baseArmy.copy(units = Nil)
    val errors = CompositionValidator.validateCharacterRequirement(army, datasheetIndex)
    errors should contain(NoCharacter())
  }

  "validateDuplicationLimits" should "accept up to 3 copies of a non-battleline unit" in {
    val army = baseArmy.copy(units = ArmyUnit(warbossId, 1, None, None) :: List.fill(3)(ArmyUnit(meganobzId, 1, None, None)))
    val errors = CompositionValidator.validateDuplicationLimits(army, datasheetIndex, keywordIndex)
    errors shouldBe empty
  }

  it should "reject more than 3 copies of a non-battleline unit" in {
    val army = baseArmy.copy(units = ArmyUnit(warbossId, 1, None, None) :: List.fill(4)(ArmyUnit(meganobzId, 1, None, None)))
    val errors = CompositionValidator.validateDuplicationLimits(army, datasheetIndex, keywordIndex)
    errors should contain(DuplicateExceeded(meganobzId, "Meganobz", 4, 3))
  }

  it should "accept up to 6 copies of a battleline unit" in {
    val army = baseArmy.copy(units = ArmyUnit(warbossId, 1, None, None) :: List.fill(6)(ArmyUnit(boyzId, 1, None, None)))
    val errors = CompositionValidator.validateDuplicationLimits(army, datasheetIndex, keywordIndex)
    errors shouldBe empty
  }

  it should "reject more than 6 copies of a battleline unit" in {
    val army = baseArmy.copy(units = ArmyUnit(warbossId, 1, None, None) :: List.fill(7)(ArmyUnit(boyzId, 1, None, None)))
    val errors = CompositionValidator.validateDuplicationLimits(army, datasheetIndex, keywordIndex)
    errors should contain(DuplicateExceeded(boyzId, "Boyz", 7, 6))
  }

  it should "accept up to 6 copies of a dedicated transport" in {
    val army = baseArmy.copy(units = ArmyUnit(warbossId, 1, None, None) :: List.fill(6)(ArmyUnit(trukId, 1, None, None)))
    val errors = CompositionValidator.validateDuplicationLimits(army, datasheetIndex, keywordIndex)
    errors shouldBe empty
  }

  it should "skip epic heroes from duplication checks" in {
    val army = baseArmy.copy(units = List(ArmyUnit(warbossId, 1, None, None), ArmyUnit(ghazId, 1, None, None)))
    val errors = CompositionValidator.validateDuplicationLimits(army, datasheetIndex, keywordIndex)
    errors shouldBe empty
  }

  "validateEpicHeroes" should "accept a single copy of an epic hero" in {
    val army = baseArmy.copy(units = List(ArmyUnit(warbossId, 1, None, None), ArmyUnit(ghazId, 1, None, None)))
    val errors = CompositionValidator.validateEpicHeroes(army, keywordIndex, datasheetIndex)
    errors shouldBe empty
  }

  it should "reject duplicate epic heroes" in {
    val army = baseArmy.copy(units = List(ArmyUnit(warbossId, 1, None, None), ArmyUnit(ghazId, 1, None, None), ArmyUnit(ghazId, 1, None, None)))
    val errors = CompositionValidator.validateEpicHeroes(army, keywordIndex, datasheetIndex)
    errors should contain(DuplicateEpicHero(ghazId, "Ghazghkull Thraka"))
  }

  it should "allow duplicates of non-epic-hero units" in {
    val army = baseArmy.copy(units = List(ArmyUnit(warbossId, 1, None, None), ArmyUnit(warbossId, 1, None, None)))
    val errors = CompositionValidator.validateEpicHeroes(army, keywordIndex, datasheetIndex)
    errors shouldBe empty
  }

  "validateChapterUnits" should "return no errors when no chapter is selected" in {
    val army = baseArmy.copy(factionId = smFaction, chapterId = None)
    val errors = CompositionValidator.validateChapterUnits(army, datasheetIndex, keywordIndex)
    errors shouldBe empty
  }

  it should "return no errors for a non-SM faction even with chapterId set" in {
    val army = baseArmy.copy(chapterId = Some("ultramarines"))
    val errors = CompositionValidator.validateChapterUnits(army, datasheetIndex, keywordIndex)
    errors shouldBe empty
  }

  it should "return no errors when all units match the selected chapter" in {
    val ultraDs = Datasheet(
      DatasheetId("000000200"), "Calgar", Some(smFaction), None, None,
      Some(Role.Characters), None, None, false, None, None, None, None, ""
    )
    val ultraKws = List(
      DatasheetKeyword(DatasheetId("000000200"), Some("SM"), None, true),
      DatasheetKeyword(DatasheetId("000000200"), Some("Ultramarines"), None, true)
    )
    val dsIdx = datasheetIndex + (DatasheetId("000000200") -> List(ultraDs))
    val kwIdx = keywordIndex ++ ultraKws.groupBy(_.datasheetId)
    val army = Army(
      factionId = smFaction,
      battleSize = BattleSize.StrikeForce,
      detachmentId = detId,
      warlordId = DatasheetId("000000200"),
      units = List(ArmyUnit(DatasheetId("000000200"), 1, None, None)),
      chapterId = Some("ultramarines")
    )
    val errors = CompositionValidator.validateChapterUnits(army, dsIdx, kwIdx)
    errors shouldBe empty
  }

  it should "reject units from a different chapter" in {
    val baId = DatasheetId("000000201")
    val baDs = Datasheet(baId, "Mephiston", Some(smFaction), None, None,
      Some(Role.Characters), None, None, false, None, None, None, None, "")
    val baKws = List(
      DatasheetKeyword(baId, Some("SM"), None, true),
      DatasheetKeyword(baId, Some("Blood Angels"), None, true)
    )
    val dsIdx = datasheetIndex + (baId -> List(baDs))
    val kwIdx = keywordIndex ++ baKws.groupBy(_.datasheetId)
    val army = Army(
      factionId = smFaction,
      battleSize = BattleSize.StrikeForce,
      detachmentId = detId,
      warlordId = baId,
      units = List(ArmyUnit(baId, 1, None, None)),
      chapterId = Some("ultramarines")
    )
    val errors = CompositionValidator.validateChapterUnits(army, dsIdx, kwIdx)
    errors should contain(ChapterMismatch(baId, "Mephiston", "Ultramarines", "Blood Angels"))
  }

  it should "skip allied units for chapter checks" in {
    val baId = DatasheetId("000000201")
    val baDs = Datasheet(baId, "Mephiston", Some(smFaction), None, None,
      Some(Role.Characters), None, None, false, None, None, None, None, "")
    val baKws = List(
      DatasheetKeyword(baId, Some("SM"), None, true),
      DatasheetKeyword(baId, Some("Blood Angels"), None, true)
    )
    val dsIdx = datasheetIndex + (baId -> List(baDs))
    val kwIdx = keywordIndex ++ baKws.groupBy(_.datasheetId)
    val army = Army(
      factionId = smFaction,
      battleSize = BattleSize.StrikeForce,
      detachmentId = detId,
      warlordId = smCaptainId,
      units = List(ArmyUnit(smCaptainId, 1, None, None), ArmyUnit(baId, 1, None, None, isAllied = true)),
      chapterId = Some("ultramarines")
    )
    val errors = CompositionValidator.validateChapterUnits(army, dsIdx, kwIdx)
    errors shouldBe empty
  }
}
