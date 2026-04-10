package wp40k.domain.army

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import wp40k.domain.types.*
import wp40k.domain.models.*

class AlliedUnitValidatorSpec extends AnyFlatSpec with Matchers {

  val orkFaction: FactionId = FactionId("Ork")
  val smFaction: FactionId = FactionId("SM")
  val ikFaction: FactionId = FactionId("QI")
  val ckFaction: FactionId = FactionId("QT")
  val cdFaction: FactionId = FactionId("CD")
  val aoiFaction: FactionId = FactionId("AoI")

  val warbossId: DatasheetId = DatasheetId("000000001")
  val boyzId: DatasheetId = DatasheetId("000000002")
  val smCaptainId: DatasheetId = DatasheetId("000000099")
  val knightErrantId: DatasheetId = DatasheetId("000000100")
  val armigerWarglaiveId: DatasheetId = DatasheetId("000000101")
  val bloodlettersId: DatasheetId = DatasheetId("000000102")
  val agentId: DatasheetId = DatasheetId("000000103")
  val ckWarDogId: DatasheetId = DatasheetId("000000104")
  val ckTitanicId: DatasheetId = DatasheetId("000000105")

  val detId: DetachmentId = DetachmentId("waaagh")
  val enhId1: EnhancementId = EnhancementId("enh1")

  val warbossDs: Datasheet = Datasheet(
    warbossId, "Warboss", Some(orkFaction), None, None,
    Some(Role.Characters), None, None, false, None, None, None, None, ""
  )
  val boyzDs: Datasheet = Datasheet(
    boyzId, "Boyz", Some(orkFaction), None, None,
    Some(Role.Battleline), None, None, false, None, None, None, None, ""
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
  val agentDs: Datasheet = Datasheet(
    agentId, "Imperial Agent", Some(aoiFaction), None, None,
    Some(Role.Characters), None, None, false, None, None, None, None, ""
  )
  val ckWarDogDs: Datasheet = Datasheet(
    ckWarDogId, "War Dog", Some(ckFaction), None, None,
    Some(Role.Other), None, None, false, None, None, None, None, ""
  )
  val ckTitanicDs: Datasheet = Datasheet(
    ckTitanicId, "Chaos Knight Titanic", Some(ckFaction), None, None,
    Some(Role.Other), None, None, false, None, None, None, None, ""
  )

  val datasheetIndex: Map[DatasheetId, List[Datasheet]] = List(
    warbossDs, boyzDs, smCaptainDs, knightErrantDs, armigerWarglaiveDs,
    bloodlettersDs, agentDs, ckWarDogDs, ckTitanicDs
  ).groupBy(_.id)

  val allKeywords: List[DatasheetKeyword] = List(
    DatasheetKeyword(warbossId, Some("Ork"), None, true),
    DatasheetKeyword(boyzId, Some("Ork"), None, true),
    DatasheetKeyword(smCaptainId, Some("SM"), None, true),
    DatasheetKeyword(smCaptainId, Some("IMPERIUM"), None, true),
    DatasheetKeyword(knightErrantId, Some("QI"), None, true),
    DatasheetKeyword(knightErrantId, Some("IMPERIUM"), None, true),
    DatasheetKeyword(knightErrantId, Some("Titanic"), None, false),
    DatasheetKeyword(armigerWarglaiveId, Some("QI"), None, true),
    DatasheetKeyword(armigerWarglaiveId, Some("IMPERIUM"), None, true),
    DatasheetKeyword(bloodlettersId, Some("CD"), None, true),
    DatasheetKeyword(bloodlettersId, Some("CHAOS"), None, true),
    DatasheetKeyword(agentId, Some("AoI"), None, true),
    DatasheetKeyword(agentId, Some("IMPERIUM"), None, true),
    DatasheetKeyword(ckWarDogId, Some("QT"), None, true),
    DatasheetKeyword(ckWarDogId, Some("CHAOS"), None, true),
    DatasheetKeyword(ckTitanicId, Some("QT"), None, true),
    DatasheetKeyword(ckTitanicId, Some("CHAOS"), None, true),
    DatasheetKeyword(ckTitanicId, Some("Titanic"), None, false)
  )

  val keywordIndex: Map[DatasheetId, List[DatasheetKeyword]] = allKeywords.groupBy(_.datasheetId)

  val unitCosts: List[UnitCost] = List(
    UnitCost(warbossId, 1, "1 model", 65),
    UnitCost(boyzId, 1, "10 models", 80),
    UnitCost(smCaptainId, 1, "1 model", 80),
    UnitCost(knightErrantId, 1, "1 model", 400),
    UnitCost(armigerWarglaiveId, 1, "1 model", 150),
    UnitCost(bloodlettersId, 1, "10 models", 130),
    UnitCost(agentId, 1, "1 model", 50),
    UnitCost(ckWarDogId, 1, "1 model", 140),
    UnitCost(ckTitanicId, 1, "1 model", 400)
  )

  val costIndex: Map[(DatasheetId, Int), List[UnitCost]] = unitCosts.groupBy(c => (c.datasheetId, c.line))

  def imperiumArmy(alliedUnits: List[ArmyUnit] = Nil): Army = Army(
    factionId = smFaction,
    battleSize = BattleSize.StrikeForce,
    detachmentId = detId,
    warlordId = smCaptainId,
    units = List(ArmyUnit(smCaptainId, 1, None, None)) ++ alliedUnits
  )

  def chaosArmy(alliedUnits: List[ArmyUnit] = Nil): Army = Army(
    factionId = orkFaction,
    battleSize = BattleSize.StrikeForce,
    detachmentId = detId,
    warlordId = warbossId,
    units = List(
      ArmyUnit(warbossId, 1, None, None),
      ArmyUnit(boyzId, 1, None, None)
    ) ++ alliedUnits
  )

  "validate" should "return no errors when there are no allied units" in {
    val errors = AlliedUnitValidator.validate(imperiumArmy(), datasheetIndex, keywordIndex, costIndex)
    errors shouldBe empty
  }

  it should "reject enhancements on allied units" in {
    val army = imperiumArmy(List(
      ArmyUnit(armigerWarglaiveId, 1, Some(enhId1), None, isAllied = true)
    ))
    val errors = AlliedUnitValidator.validate(army, datasheetIndex, keywordIndex, costIndex)
    errors should contain(AlliedEnhancement(armigerWarglaiveId, enhId1))
  }

  it should "reject allied units from factions not in allowed allies" in {
    val army = imperiumArmy(List(
      ArmyUnit(bloodlettersId, 1, None, None, isAllied = true)
    ))
    val errors = AlliedUnitValidator.validate(army, datasheetIndex, keywordIndex, costIndex)
    errors should contain(AlliedFactionNotAllowed(bloodlettersId, cdFaction))
  }

  it should "accept allied units from allowed factions" in {
    val army = imperiumArmy(List(
      ArmyUnit(armigerWarglaiveId, 1, None, None, isAllied = true)
    ))
    val errors = AlliedUnitValidator.validate(army, datasheetIndex, keywordIndex, costIndex)
    errors.collect { case e: AlliedFactionNotAllowed => e } shouldBe empty
  }

  it should "enforce Freeblades titanic limit of 1" in {
    val army = imperiumArmy(List(
      ArmyUnit(knightErrantId, 1, None, None, isAllied = true),
      ArmyUnit(knightErrantId, 1, None, None, isAllied = true)
    ))
    val errors = AlliedUnitValidator.validate(army, datasheetIndex, keywordIndex, costIndex)
    errors.collect { case e: AlliedUnitLimitExceeded => e } should not be empty
  }

  it should "accept 1 titanic and up to 3 non-titanic Freeblade allies" in {
    val army = imperiumArmy(List(
      ArmyUnit(knightErrantId, 1, None, None, isAllied = true),
      ArmyUnit(armigerWarglaiveId, 1, None, None, isAllied = true),
      ArmyUnit(armigerWarglaiveId, 1, None, None, isAllied = true),
      ArmyUnit(armigerWarglaiveId, 1, None, None, isAllied = true)
    ))
    val errors = AlliedUnitValidator.validate(army, datasheetIndex, keywordIndex, costIndex)
    errors.collect { case e: AlliedUnitLimitExceeded => e } shouldBe empty
  }

  it should "reject more than 3 non-titanic Freeblade allies" in {
    val army = imperiumArmy(List(
      ArmyUnit(armigerWarglaiveId, 1, None, None, isAllied = true),
      ArmyUnit(armigerWarglaiveId, 1, None, None, isAllied = true),
      ArmyUnit(armigerWarglaiveId, 1, None, None, isAllied = true),
      ArmyUnit(armigerWarglaiveId, 1, None, None, isAllied = true)
    ))
    val errors = AlliedUnitValidator.validate(army, datasheetIndex, keywordIndex, costIndex)
    errors.collect { case e: AlliedUnitLimitExceeded => e } should not be empty
  }

  it should "enforce AssignedAgents unit limit based on battle size" in {
    val army = imperiumArmy(List(
      ArmyUnit(agentId, 1, None, None, isAllied = true),
      ArmyUnit(agentId, 1, None, None, isAllied = true),
      ArmyUnit(agentId, 1, None, None, isAllied = true),
      ArmyUnit(agentId, 1, None, None, isAllied = true),
      ArmyUnit(agentId, 1, None, None, isAllied = true)
    ))
    val errors = AlliedUnitValidator.validate(army, datasheetIndex, keywordIndex, costIndex)
    errors.collect { case e: AlliedUnitLimitExceeded => e } should not be empty
  }

  it should "accept AssignedAgents within unit limit" in {
    val army = imperiumArmy(List(
      ArmyUnit(agentId, 1, None, None, isAllied = true),
      ArmyUnit(agentId, 1, None, None, isAllied = true),
      ArmyUnit(agentId, 1, None, None, isAllied = true),
      ArmyUnit(agentId, 1, None, None, isAllied = true)
    ))
    val errors = AlliedUnitValidator.validate(army, datasheetIndex, keywordIndex, costIndex)
    errors.collect { case e: AlliedUnitLimitExceeded => e } shouldBe empty
  }

  it should "enforce DaemonicPact points limit" in {
    val expensiveBloodletters = UnitCost(bloodlettersId, 2, "20 models", 600)
    val updatedCostIndex = costIndex + ((bloodlettersId, 2) -> List(expensiveBloodletters))

    val chaosKws = List(
      DatasheetKeyword(warbossId, Some("CHAOS"), None, true),
      DatasheetKeyword(boyzId, Some("CHAOS"), None, true)
    )
    val updatedKwIndex = keywordIndex ++ chaosKws.groupBy(_.datasheetId).map {
      case (id, kws) => id -> (keywordIndex.getOrElse(id, Nil) ++ kws)
    }

    val army = chaosArmy(List(
      ArmyUnit(bloodlettersId, 2, None, None, isAllied = true)
    ))
    val errors = AlliedUnitValidator.validate(army, datasheetIndex, updatedKwIndex, updatedCostIndex)
    errors.collect { case e: AlliedPointsExceeded => e } should not be empty
  }

  it should "accept DaemonicPact allies within points limit" in {
    val chaosKws = List(
      DatasheetKeyword(warbossId, Some("CHAOS"), None, true),
      DatasheetKeyword(boyzId, Some("CHAOS"), None, true)
    )
    val updatedKwIndex = keywordIndex ++ chaosKws.groupBy(_.datasheetId).map {
      case (id, kws) => id -> (keywordIndex.getOrElse(id, Nil) ++ kws)
    }

    val army = chaosArmy(List(
      ArmyUnit(bloodlettersId, 1, None, None, isAllied = true)
    ))
    val errors = AlliedUnitValidator.validate(army, datasheetIndex, updatedKwIndex, costIndex)
    errors.collect { case e: AlliedPointsExceeded => e } shouldBe empty
  }

  it should "reject titanic units for DaemonicPact allies" in {
    val titanicDaemonId = DatasheetId("000000106")
    val titanicDaemonDs = Datasheet(titanicDaemonId, "Great Unclean One", Some(cdFaction), None, None,
      Some(Role.Characters), None, None, false, None, None, None, None, "")
    val titanicDaemonKws = List(
      DatasheetKeyword(titanicDaemonId, Some("CD"), None, true),
      DatasheetKeyword(titanicDaemonId, Some("CHAOS"), None, true),
      DatasheetKeyword(titanicDaemonId, Some("Titanic"), None, false)
    )
    val titanicDaemonCost = UnitCost(titanicDaemonId, 1, "1 model", 200)
    val updatedDsIdx = datasheetIndex + (titanicDaemonId -> List(titanicDaemonDs))
    val updatedKwIdx = (keywordIndex ++ titanicDaemonKws.groupBy(_.datasheetId)) ++ List(
      DatasheetKeyword(warbossId, Some("CHAOS"), None, true),
      DatasheetKeyword(boyzId, Some("CHAOS"), None, true)
    ).groupBy(_.datasheetId).map {
      case (id, kws) => id -> (keywordIndex.getOrElse(id, Nil) ++ kws)
    }
    val updatedCostIdx = costIndex + ((titanicDaemonId, 1) -> List(titanicDaemonCost))

    val army = chaosArmy(List(
      ArmyUnit(titanicDaemonId, 1, None, None, isAllied = true)
    ))
    val errors = AlliedUnitValidator.validate(army, updatedDsIdx, updatedKwIdx, updatedCostIdx)
    errors.collect { case e: AlliedUnitLimitExceeded => e } should not be empty
  }

  it should "enforce Dreadblades titanic limit of 1" in {
    val chaosKws = List(
      DatasheetKeyword(warbossId, Some("CHAOS"), None, true),
      DatasheetKeyword(boyzId, Some("CHAOS"), None, true)
    )
    val updatedKwIndex = keywordIndex ++ chaosKws.groupBy(_.datasheetId).map {
      case (id, kws) => id -> (keywordIndex.getOrElse(id, Nil) ++ kws)
    }

    val army = chaosArmy(List(
      ArmyUnit(ckTitanicId, 1, None, None, isAllied = true),
      ArmyUnit(ckTitanicId, 1, None, None, isAllied = true)
    ))
    val errors = AlliedUnitValidator.validate(army, datasheetIndex, updatedKwIndex, costIndex)
    errors.collect { case e: AlliedUnitLimitExceeded => e } should not be empty
  }

  it should "accept valid Dreadblades allies with 1 titanic and non-titanic units" in {
    val chaosKws = List(
      DatasheetKeyword(warbossId, Some("CHAOS"), None, true),
      DatasheetKeyword(boyzId, Some("CHAOS"), None, true)
    )
    val updatedKwIndex = keywordIndex ++ chaosKws.groupBy(_.datasheetId).map {
      case (id, kws) => id -> (keywordIndex.getOrElse(id, Nil) ++ kws)
    }

    val army = chaosArmy(List(
      ArmyUnit(ckTitanicId, 1, None, None, isAllied = true),
      ArmyUnit(ckWarDogId, 1, None, None, isAllied = true),
      ArmyUnit(ckWarDogId, 1, None, None, isAllied = true)
    ))
    val errors = AlliedUnitValidator.validate(army, datasheetIndex, updatedKwIndex, costIndex)
    errors.collect { case e: AlliedUnitLimitExceeded => e } shouldBe empty
  }
}
