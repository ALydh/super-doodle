package wp40k.domain.army

import wp40k.domain.types.{DatasheetId, DetachmentId, Role}
import wp40k.domain.models.*
import wp40k.domain.Constants.Validation

private[army] object EnhancementValidator {

  def validateCount(army: Army): List[ValidationError] = {
    val enhancementCount = army.units.flatMap(_.enhancementId).size
    if (enhancementCount > Validation.MaxEnhancements) List(TooManyEnhancements(enhancementCount))
    else Nil
  }

  def validateUniqueness(army: Army): List[ValidationError] = {
    val enhancementIds = army.units.flatMap(_.enhancementId)
    val duplicates = enhancementIds.groupBy(identity).filter(_._2.size > 1).keys
    duplicates.map(DuplicateEnhancement(_)).toList
  }

  def validateOnCharacters(
    army: Army,
    datasheetIndex: Map[DatasheetId, List[Datasheet]]
  ): List[ValidationError] =
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

  def validateDetachment(
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
