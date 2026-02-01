package wahapedia.domain.army

import wahapedia.domain.types.{DatasheetId, FactionId, DetachmentId, Role}
import wahapedia.domain.models.*
import wahapedia.domain.Constants.{Validation, Keywords, Defaults}

case class ReferenceData(
  datasheets: List[Datasheet],
  keywords: List[DatasheetKeyword],
  unitCosts: List[UnitCost],
  enhancements: List[Enhancement],
  leaders: List[DatasheetLeader],
  detachmentAbilities: List[DetachmentAbility]
)

object ArmyValidator {

  def validate(army: Army, ref: ReferenceData): List[ValidationError] = {
    val datasheetIndex = ref.datasheets.groupBy(_.id)
    val keywordIndex = ref.keywords.groupBy(_.datasheetId)
    val costIndex = ref.unitCosts.groupBy(ds => (ds.datasheetId, ds.line))
    val leaderIndex = ref.leaders.groupBy(_.leaderId)
    val enhancementIndex = ref.enhancements.groupBy(_.id)

    List(
      validateFactionKeywords(army, datasheetIndex, keywordIndex),
      validatePoints(army, costIndex, ref.enhancements),
      validateCharacterRequirement(army, datasheetIndex),
      validateWarlord(army, datasheetIndex),
      validateDuplicationLimits(army, datasheetIndex, keywordIndex),
      validateEpicHeroes(army, keywordIndex, datasheetIndex),
      validateLeaderAttachments(army, leaderIndex),
      validateEnhancementCount(army),
      validateEnhancementUniqueness(army),
      validateEnhancementsOnCharacters(army, datasheetIndex),
      validateEnhancementDetachment(army, enhancementIndex)
    ).flatten
  }

  private def validateFactionKeywords(
    army: Army,
    datasheetIndex: Map[DatasheetId, List[Datasheet]],
    keywordIndex: Map[DatasheetId, List[DatasheetKeyword]]
  ): List[ValidationError] = {
    val factionName = FactionId.value(army.factionId)
    army.units.flatMap { unit =>
      val factionKeywords = keywordIndex.getOrElse(unit.datasheetId, Nil)
        .filter(_.isFactionKeyword)
        .flatMap(_.keyword)

      val hasFaction = factionKeywords.exists(_.equalsIgnoreCase(factionName)) ||
        datasheetIndex.get(unit.datasheetId).flatMap(_.headOption).flatMap(_.factionId).contains(army.factionId)

      if (hasFaction) Nil
      else {
        val name = datasheetIndex.get(unit.datasheetId).flatMap(_.headOption).map(_.name).getOrElse(Defaults.UnknownDatasheet)
        List(FactionMismatch(unit.datasheetId, name, army.factionId))
      }
    }
  }

  private def validatePoints(
    army: Army,
    costIndex: Map[(DatasheetId, Int), List[UnitCost]],
    enhancements: List[Enhancement]
  ): List[ValidationError] = {
    val enhancementCosts = enhancements.groupBy(_.id)

    val unitCostErrors = army.units.flatMap { unit =>
      costIndex.get((unit.datasheetId, unit.sizeOptionLine)) match {
        case None => List(UnitCostNotFound(unit.datasheetId, unit.sizeOptionLine))
        case Some(_) => Nil
      }
    }

    if (unitCostErrors.nonEmpty) return unitCostErrors

    val unitTotal = army.units.flatMap { unit =>
      costIndex.get((unit.datasheetId, unit.sizeOptionLine)).flatMap(_.headOption).map(_.cost)
    }.sum

    val enhancementTotal = army.units.flatMap { unit =>
      unit.enhancementId.flatMap(id => enhancementCosts.get(id).flatMap(_.headOption).map(_.cost))
    }.sum

    val total = unitTotal + enhancementTotal
    if (total > army.battleSize.maxPoints) List(PointsExceeded(total, army.battleSize.maxPoints))
    else Nil
  }

  private def validateCharacterRequirement(
    army: Army,
    datasheetIndex: Map[DatasheetId, List[Datasheet]]
  ): List[ValidationError] = {
    val hasCharacter = army.units.exists { unit =>
      datasheetIndex.get(unit.datasheetId)
        .flatMap(_.headOption)
        .flatMap(_.role)
        .contains(Role.Characters)
    }
    if (hasCharacter) Nil
    else List(NoCharacter())
  }

  private def validateWarlord(
    army: Army,
    datasheetIndex: Map[DatasheetId, List[Datasheet]]
  ): List[ValidationError] = {
    val warlordInArmy = army.units.exists(_.datasheetId == army.warlordId)
    if (!warlordInArmy) return List(WarlordNotInArmy(army.warlordId))

    val isCharacter = datasheetIndex.get(army.warlordId)
      .flatMap(_.headOption)
      .flatMap(_.role)
      .contains(Role.Characters)

    if (isCharacter) Nil
    else List(InvalidWarlord(army.warlordId))
  }

  private def validateDuplicationLimits(
    army: Army,
    datasheetIndex: Map[DatasheetId, List[Datasheet]],
    keywordIndex: Map[DatasheetId, List[DatasheetKeyword]]
  ): List[ValidationError] = {
    val counts = army.units.groupBy(_.datasheetId).map { case (id, units) => (id, units.size) }

    counts.flatMap { case (datasheetId, count) =>
      val role = datasheetIndex.get(datasheetId).flatMap(_.headOption).flatMap(_.role)
      val isEpicHero = keywordIndex.getOrElse(datasheetId, Nil)
        .flatMap(_.keyword)
        .exists(_.equalsIgnoreCase(Keywords.EpicHero))

      if (isEpicHero) Nil // handled by validateEpicHeroes
      else {
        val maxAllowed = role match {
          case Some(Role.Battleline) | Some(Role.DedicatedTransports) => Validation.MaxBattlelineDuplicates
          case _ => Validation.MaxDefaultDuplicates
        }
        if (count > maxAllowed) {
          val name = datasheetIndex.get(datasheetId).flatMap(_.headOption).map(_.name).getOrElse(Defaults.UnknownDatasheet)
          List(DuplicateExceeded(datasheetId, name, count, maxAllowed))
        } else Nil
      }
    }.toList
  }

  private def validateEpicHeroes(
    army: Army,
    keywordIndex: Map[DatasheetId, List[DatasheetKeyword]],
    datasheetIndex: Map[DatasheetId, List[Datasheet]]
  ): List[ValidationError] = {
    val counts = army.units.groupBy(_.datasheetId).map { case (id, units) => (id, units.size) }

    counts.flatMap { case (datasheetId, count) =>
      val isEpicHero = keywordIndex.getOrElse(datasheetId, Nil)
        .flatMap(_.keyword)
        .exists(_.equalsIgnoreCase(Keywords.EpicHero))

      if (isEpicHero && count > 1) {
        val name = datasheetIndex.get(datasheetId).flatMap(_.headOption).map(_.name).getOrElse(Defaults.UnknownDatasheet)
        List(DuplicateEpicHero(datasheetId, name))
      } else Nil
    }.toList
  }

  private def validateLeaderAttachments(
    army: Army,
    leaderIndex: Map[DatasheetId, List[DatasheetLeader]]
  ): List[ValidationError] = {
    army.units.flatMap { unit =>
      unit.attachedLeaderId match {
        case None => Nil
        case Some(leaderId) =>
          val validBodyguards = leaderIndex.getOrElse(leaderId, Nil).map(_.attachedId)
          if (validBodyguards.contains(unit.datasheetId)) Nil
          else List(InvalidLeaderAttachment(leaderId, unit.datasheetId))
      }
    }
  }

  private def validateEnhancementCount(army: Army): List[ValidationError] = {
    val enhancementCount = army.units.flatMap(_.enhancementId).size
    if (enhancementCount > Validation.MaxEnhancements) List(TooManyEnhancements(enhancementCount))
    else Nil
  }

  private def validateEnhancementUniqueness(army: Army): List[ValidationError] = {
    val enhancementIds = army.units.flatMap(_.enhancementId)
    val duplicates = enhancementIds.groupBy(identity).filter(_._2.size > 1).keys
    duplicates.map(DuplicateEnhancement(_)).toList
  }

  private def validateEnhancementsOnCharacters(
    army: Army,
    datasheetIndex: Map[DatasheetId, List[Datasheet]]
  ): List[ValidationError] = {
    army.units.flatMap { unit =>
      unit.enhancementId match {
        case None => Nil
        case Some(enhId) =>
          val isCharacter = datasheetIndex.get(unit.datasheetId)
            .flatMap(_.headOption)
            .flatMap(_.role)
            .contains(Role.Characters)
          if (isCharacter) Nil
          else List(EnhancementOnNonCharacter(unit.datasheetId, enhId))
      }
    }
  }

  private def validateEnhancementDetachment(
    army: Army,
    enhancementIndex: Map[EnhancementId, List[Enhancement]]
  ): List[ValidationError] = {
    val armyDetachment = DetachmentId.value(army.detachmentId)
    army.units.flatMap { unit =>
      unit.enhancementId.flatMap { enhId =>
        enhancementIndex.get(enhId).flatMap(_.headOption).flatMap { enh =>
          enh.detachmentId match {
            case Some(detId) if detId != armyDetachment =>
              Some(EnhancementDetachmentMismatch(enhId, army.detachmentId))
            case _ => None
          }
        }
      }
    }
  }
}
