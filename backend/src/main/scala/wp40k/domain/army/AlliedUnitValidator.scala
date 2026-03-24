package wp40k.domain.army

import wp40k.domain.types.DatasheetId
import wp40k.domain.models.*

private[army] object AlliedUnitValidator {

  def validate(
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
}
