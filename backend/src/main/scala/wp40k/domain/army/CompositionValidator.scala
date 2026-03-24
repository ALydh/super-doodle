package wp40k.domain.army

import wp40k.domain.types.{DatasheetId, FactionId, Role}
import wp40k.domain.models.*
import wp40k.domain.Constants.{Validation, Keywords, Defaults, Chapters}

private[army] object CompositionValidator {

  def validateFactionKeywords(
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

  def validateCharacterRequirement(
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

  def validateDuplicationLimits(
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

      if (isEpicHero) Nil
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

  def validateEpicHeroes(
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

  def validateChapterUnits(
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
