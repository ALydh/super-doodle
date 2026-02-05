package wahapedia.domain.army

import wahapedia.domain.types.{DatasheetId, FactionId, DetachmentId, Role}
import wahapedia.domain.models.*
import wahapedia.domain.Constants.{Validation, Keywords, Defaults, Chapters, Leaders}
import wahapedia.domain.army.AllyRules.{ImperialKnightsFaction, ChaosKnightsFaction, ChaosDaemonsFaction, ImperialAgentsFaction}

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
      validateLeaderAttachments(army, leaderIndex, datasheetIndex),
      validateEnhancementCount(army),
      validateEnhancementUniqueness(army),
      validateEnhancementsOnCharacters(army, datasheetIndex),
      validateEnhancementDetachment(army, enhancementIndex),
      validateAlliedUnits(army, datasheetIndex, keywordIndex, costIndex),
      validateChapterUnits(army, datasheetIndex, keywordIndex)
    ).flatten
  }

  private def validateFactionKeywords(
    army: Army,
    datasheetIndex: Map[DatasheetId, List[Datasheet]],
    keywordIndex: Map[DatasheetId, List[DatasheetKeyword]]
  ): List[ValidationError] = {
    val factionName = FactionId.value(army.factionId)
    army.units.filterNot(_.isAllied).flatMap { unit =>
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
    val warlordUnit = army.units.find(_.datasheetId == army.warlordId)
    if (warlordUnit.isEmpty) return List(WarlordNotInArmy(army.warlordId))

    if (warlordUnit.exists(_.isAllied)) return List(AlliedWarlord(army.warlordId))

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
    leaderIndex: Map[DatasheetId, List[DatasheetLeader]],
    datasheetIndex: Map[DatasheetId, List[Datasheet]]
  ): List[ValidationError] = {
    val pairingErrors = army.units.flatMap { unit =>
      unit.attachedLeaderId match {
        case None => Nil
        case Some(bodyguardId) =>
          val validBodyguards = leaderIndex.getOrElse(unit.datasheetId, Nil).map(_.attachedId)
          if (validBodyguards.contains(bodyguardId)) Nil
          else List(InvalidLeaderAttachment(unit.datasheetId, bodyguardId))
      }
    }

    val leadersAttached = army.units.filter(_.attachedLeaderId.isDefined)
    val grouped = leadersAttached.groupBy(_.attachedLeaderId.get)

    val countErrors = grouped.flatMap { case (bodyguardId, leaders) =>
      if (leaders.size <= 1) Nil
      else {
        val isSpecialBodyguard = Leaders.SpecialBodyguards.get(bodyguardId).exists { maxLeaders =>
          leaders.headOption.exists(_ => army.units.exists(u =>
            u.datasheetId == bodyguardId && u.sizeOptionLine >= Leaders.SpecialBodyguardSizeLine
          ))
        }

        val hasCoLeader = leaders.exists { unit =>
          datasheetIndex.get(unit.datasheetId)
            .flatMap(_.headOption)
            .flatMap(_.leaderFooter)
            .exists(_.toLowerCase.contains("even if"))
        }

        val max = if (isSpecialBodyguard)
          Leaders.SpecialBodyguards(bodyguardId)
        else if (hasCoLeader) 2
        else 1

        if (leaders.size > max) List(TooManyLeaders(bodyguardId, leaders.size, max))
        else Nil
      }
    }.toList

    pairingErrors ++ countErrors
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

  private def validateAlliedUnits(
    army: Army,
    datasheetIndex: Map[DatasheetId, List[Datasheet]],
    keywordIndex: Map[DatasheetId, List[DatasheetKeyword]],
    costIndex: Map[(DatasheetId, Int), List[UnitCost]]
  ): List[ValidationError] = {
    val alliedUnits = army.units.filter(_.isAllied)
    if (alliedUnits.isEmpty) return Nil

    val armyKeywords = army.units.filterNot(_.isAllied).flatMap { unit =>
      keywordIndex.getOrElse(unit.datasheetId, Nil)
        .flatMap(_.keyword)
    }.toSet

    val allowedAllies = AllyRules.allowedAllies(armyKeywords)
    val allowedFactions = allowedAllies.map(_.factionId).toSet

    val enhancementErrors = alliedUnits.flatMap { unit =>
      unit.enhancementId.map(enhId => AlliedEnhancement(unit.datasheetId, enhId))
    }

    val factionErrors = alliedUnits.flatMap { unit =>
      val unitFactionId = datasheetIndex.get(unit.datasheetId).flatMap(_.headOption).flatMap(_.factionId)
      unitFactionId match {
        case Some(fid) if !allowedFactions.contains(fid) =>
          List(AlliedFactionNotAllowed(unit.datasheetId, fid))
        case _ => Nil
      }
    }

    val alliedByFaction = alliedUnits.groupBy { unit =>
      datasheetIndex.get(unit.datasheetId).flatMap(_.headOption).flatMap(_.factionId)
    }

    val limitErrors = allowedAllies.flatMap { ally =>
      val unitsForAlly = alliedByFaction.getOrElse(Some(ally.factionId), Nil)
      if (unitsForAlly.isEmpty) Nil
      else {
        val limits = AllyRules.limitsFor(ally.allyType, army.battleSize)
        val allyTypeName = ally.allyType.toString

        val titanicKeyword = "Titanic"
        val titanicUnits = unitsForAlly.filter { unit =>
          keywordIndex.getOrElse(unit.datasheetId, Nil)
            .flatMap(_.keyword)
            .exists(_.equalsIgnoreCase(titanicKeyword))
        }
        val nonTitanicUnits = unitsForAlly.filterNot(titanicUnits.contains)

        val titanicError = if (titanicUnits.size > limits.maxTitanic)
          List(AlliedUnitLimitExceeded(allyTypeName, s"Maximum ${ limits.maxTitanic } Titanic unit(s) allowed"))
        else Nil

        val nonTitanicError = if (nonTitanicUnits.size > limits.maxNonTitanic)
          List(AlliedUnitLimitExceeded(allyTypeName, s"Maximum ${ limits.maxNonTitanic } non-Titanic unit(s) allowed"))
        else Nil

        val unitCountError = limits.maxUnits match {
          case Some(max) if unitsForAlly.size > max =>
            List(AlliedUnitLimitExceeded(allyTypeName, s"Maximum $max allied unit(s) allowed for ${army.battleSize}"))
          case _ => Nil
        }

        val pointsError = limits.maxPoints match {
          case Some(max) =>
            val alliedPoints = unitsForAlly.flatMap { unit =>
              costIndex.get((unit.datasheetId, unit.sizeOptionLine)).flatMap(_.headOption).map(_.cost)
            }.sum
            if (alliedPoints > max) List(AlliedPointsExceeded(allyTypeName, alliedPoints, max))
            else Nil
          case None => Nil
        }

        titanicError ++ nonTitanicError ++ unitCountError ++ pointsError
      }
    }

    enhancementErrors ++ factionErrors ++ limitErrors
  }

  private def validateChapterUnits(
    army: Army,
    datasheetIndex: Map[DatasheetId, List[Datasheet]],
    keywordIndex: Map[DatasheetId, List[DatasheetKeyword]]
  ): List[ValidationError] = {
    val chapterKeyword = army.chapterId
      .filter(_ => FactionId.value(army.factionId) == Chapters.FactionId)
      .flatMap(Chapters.Keywords.get)

    chapterKeyword match {
      case None => Nil
      case Some(selected) =>
        army.units.filterNot(_.isAllied).flatMap { unit =>
          val factionKws = keywordIndex.getOrElse(unit.datasheetId, Nil)
            .filter(_.isFactionKeyword)
            .flatMap(_.keyword)

          val wrongChapter = factionKws.find(kw => Chapters.AllKeywords.contains(kw) && kw != selected)
          wrongChapter match {
            case Some(kw) =>
              val name = datasheetIndex.get(unit.datasheetId).flatMap(_.headOption).map(_.name).getOrElse(Defaults.UnknownDatasheet)
              List(ChapterMismatch(unit.datasheetId, name, selected, kw))
            case None => Nil
          }
        }
    }
  }
}
