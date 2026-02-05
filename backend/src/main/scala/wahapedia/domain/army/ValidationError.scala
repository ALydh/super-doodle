package wahapedia.domain.army

import wahapedia.domain.types.{DatasheetId, FactionId, DetachmentId}
import wahapedia.domain.models.EnhancementId

sealed trait ValidationError

case class FactionMismatch(
  unitDatasheetId: DatasheetId,
  unitName: String,
  expectedFaction: FactionId
) extends ValidationError

case class PointsExceeded(
  total: Int,
  limit: Int
) extends ValidationError

case class NoCharacter() extends ValidationError

case class InvalidWarlord(
  warlordId: DatasheetId
) extends ValidationError

case class WarlordNotInArmy(
  warlordId: DatasheetId
) extends ValidationError

case class DuplicateExceeded(
  datasheetId: DatasheetId,
  unitName: String,
  count: Int,
  maxAllowed: Int
) extends ValidationError

case class DuplicateEpicHero(
  datasheetId: DatasheetId,
  unitName: String
) extends ValidationError

case class InvalidLeaderAttachment(
  leaderId: DatasheetId,
  attachedToId: DatasheetId
) extends ValidationError

case class TooManyEnhancements(count: Int) extends ValidationError

case class DuplicateEnhancement(enhancementId: EnhancementId) extends ValidationError

case class EnhancementOnNonCharacter(
  datasheetId: DatasheetId,
  enhancementId: EnhancementId
) extends ValidationError

case class MultipleEnhancementsOnUnit(
  datasheetId: DatasheetId
) extends ValidationError

case class EnhancementDetachmentMismatch(
  enhancementId: EnhancementId,
  expectedDetachment: DetachmentId
) extends ValidationError

case class UnitCostNotFound(
  datasheetId: DatasheetId,
  sizeOptionLine: Int
) extends ValidationError

case class DatasheetNotFound(
  datasheetId: DatasheetId
) extends ValidationError

case class AlliedWarlord(
  datasheetId: DatasheetId
) extends ValidationError

case class AlliedEnhancement(
  datasheetId: DatasheetId,
  enhancementId: EnhancementId
) extends ValidationError

case class AlliedUnitLimitExceeded(
  allyType: String,
  message: String
) extends ValidationError

case class AlliedPointsExceeded(
  allyType: String,
  used: Int,
  limit: Int
) extends ValidationError

case class AlliedFactionNotAllowed(
  datasheetId: DatasheetId,
  factionId: FactionId
) extends ValidationError

case class ChapterMismatch(
  datasheetId: DatasheetId,
  unitName: String,
  selectedChapter: String,
  unitChapter: String
) extends ValidationError
