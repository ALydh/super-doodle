package wahapedia.domain.army

import wahapedia.domain.types.{DatasheetId, FactionId, DetachmentId, BattleSize}
import wahapedia.domain.models.EnhancementId

case class ArmyUnit(
  datasheetId: DatasheetId,
  sizeOptionLine: Int,
  enhancementId: Option[EnhancementId],
  attachedLeaderId: Option[DatasheetId]
)

case class Army(
  factionId: FactionId,
  battleSize: BattleSize,
  detachmentId: DetachmentId,
  warlordId: DatasheetId,
  units: List[ArmyUnit]
)
