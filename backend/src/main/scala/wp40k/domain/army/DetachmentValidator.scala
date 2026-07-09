package wp40k.domain.army

import wp40k.domain.types.DetachmentId
import wp40k.domain.models.Detachment
import wp40k.domain.Constants.Detachments

private[army] object DetachmentValidator {

  def validate(army: Army, detachmentIndex: Map[DetachmentId, Detachment]): List[ValidationError] =
    validatePresence(army) ++ validatePointsBudget(army, detachmentIndex) ++ validateKeywordConflict(army, detachmentIndex)

  private def validatePresence(army: Army): List[ValidationError] =
    if (army.detachments.isEmpty) List(NoDetachment()) else Nil

  private def validatePointsBudget(
    army: Army,
    detachmentIndex: Map[DetachmentId, Detachment]
  ): List[ValidationError] = {
    val spent = army.detachments.map(id => detachmentIndex.get(id).map(_.dpCost).getOrElse(Detachments.DefaultDpCost)).sum
    val limit = army.battleSize.detachmentPoints
    if (spent > limit) List(DetachmentPointsExceeded(spent, limit)) else Nil
  }

  private def validateKeywordConflict(
    army: Army,
    detachmentIndex: Map[DetachmentId, Detachment]
  ): List[ValidationError] = {
    val keywords = army.detachments.flatMap(id => detachmentIndex.get(id)).flatMap(_.keyword)
    keywords.groupBy(identity).filter(_._2.size > 1).keys.map(DetachmentKeywordConflict(_)).toList
  }
}
