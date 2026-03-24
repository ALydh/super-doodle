package wp40k.domain.army

import wp40k.domain.types.{DatasheetId, FactionId, DetachmentId, Role}
import wp40k.domain.models.*
import wp40k.domain.Constants.Leaders

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
      CompositionValidator.validateFactionKeywords(army, datasheetIndex, keywordIndex),
      validatePoints(army, costIndex, ref.enhancements),
      CompositionValidator.validateCharacterRequirement(army, datasheetIndex),
      validateWarlord(army, datasheetIndex),
      CompositionValidator.validateDuplicationLimits(army, datasheetIndex, keywordIndex),
      CompositionValidator.validateEpicHeroes(army, keywordIndex, datasheetIndex),
      validateLeaderAttachments(army, leaderIndex, datasheetIndex),
      EnhancementValidator.validateCount(army),
      EnhancementValidator.validateUniqueness(army),
      EnhancementValidator.validateOnCharacters(army, datasheetIndex),
      EnhancementValidator.validateDetachment(army, enhancementIndex),
      AlliedUnitValidator.validate(army, datasheetIndex, keywordIndex, costIndex),
      CompositionValidator.validateChapterUnits(army, datasheetIndex, keywordIndex)
    ).flatten
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
}
