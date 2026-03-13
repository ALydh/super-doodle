package wp40k.domain.army

import wp40k.domain.types.{DatasheetId, FactionId, DetachmentId, BattleSize}
import wp40k.domain.models.EnhancementId

case class WargearSelection(
  optionLine: Int,
  selected: Boolean,
  notes: Option[String]
)

case class ArmyUnit(
  datasheetId: DatasheetId,
  sizeOptionLine: Int,
  enhancementId: Option[EnhancementId],
  attachedLeaderId: Option[DatasheetId],
  attachedToUnitIndex: Option[Int] = None,
  wargearSelections: List[WargearSelection] = List.empty,
  isAllied: Boolean = false
)

case class Army(
  factionId: FactionId,
  battleSize: BattleSize,
  detachmentId: DetachmentId,
  warlordId: DatasheetId,
  units: List[ArmyUnit],
  chapterId: Option[String] = None
)
