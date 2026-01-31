package wahapedia.http.dto

import io.circe.{Encoder, Decoder}
import io.circe.generic.auto.*
import io.circe.syntax.*
import wahapedia.domain.types.*
import wahapedia.domain.models.*
import wahapedia.domain.army.*

case class DatasheetDetail(
  datasheet: Datasheet,
  profiles: List[ModelProfile],
  wargear: List[Wargear],
  costs: List[UnitCost],
  keywords: List[DatasheetKeyword],
  abilities: List[DatasheetAbility],
  stratagems: List[Stratagem],
  options: List[DatasheetOption],
  parsedWargearOptions: List[ParsedWargearOption]
)

case class CreateArmyRequest(name: String, army: Army)
case class ValidationResponse(valid: Boolean, errors: List[ValidationErrorDto])

sealed trait ValidationErrorDto
object ValidationErrorDto {
  case class FactionMismatch(errorType: String, unitDatasheetId: String, unitName: String, expectedFaction: String) extends ValidationErrorDto
  case class PointsExceeded(errorType: String, total: Int, limit: Int) extends ValidationErrorDto
  case class NoCharacter(errorType: String) extends ValidationErrorDto
  case class InvalidWarlord(errorType: String, warlordId: String) extends ValidationErrorDto
  case class WarlordNotInArmy(errorType: String, warlordId: String) extends ValidationErrorDto
  case class DuplicateExceeded(errorType: String, datasheetId: String, unitName: String, count: Int, maxAllowed: Int) extends ValidationErrorDto
  case class DuplicateEpicHero(errorType: String, datasheetId: String, unitName: String) extends ValidationErrorDto
  case class InvalidLeaderAttachment(errorType: String, leaderId: String, attachedToId: String) extends ValidationErrorDto
  case class TooManyEnhancements(errorType: String, count: Int) extends ValidationErrorDto
  case class DuplicateEnhancement(errorType: String, enhancementId: String) extends ValidationErrorDto
  case class EnhancementOnNonCharacter(errorType: String, datasheetId: String, enhancementId: String) extends ValidationErrorDto
  case class MultipleEnhancementsOnUnit(errorType: String, datasheetId: String) extends ValidationErrorDto
  case class EnhancementDetachmentMismatch(errorType: String, enhancementId: String, expectedDetachment: String) extends ValidationErrorDto
  case class UnitCostNotFound(errorType: String, datasheetId: String, sizeOptionLine: Int) extends ValidationErrorDto
  case class DatasheetNotFound(errorType: String, datasheetId: String) extends ValidationErrorDto
  case class Generic(errorType: String, message: String) extends ValidationErrorDto

  def fromDomain(err: ValidationError): ValidationErrorDto = err match {
    case e: wahapedia.domain.army.FactionMismatch =>
      FactionMismatch("FactionMismatch", DatasheetId.value(e.unitDatasheetId), e.unitName, FactionId.value(e.expectedFaction))
    case e: wahapedia.domain.army.PointsExceeded =>
      PointsExceeded("PointsExceeded", e.total, e.limit)
    case _: wahapedia.domain.army.NoCharacter =>
      NoCharacter("NoCharacter")
    case e: wahapedia.domain.army.InvalidWarlord =>
      InvalidWarlord("InvalidWarlord", DatasheetId.value(e.warlordId))
    case e: wahapedia.domain.army.WarlordNotInArmy =>
      WarlordNotInArmy("WarlordNotInArmy", DatasheetId.value(e.warlordId))
    case e: wahapedia.domain.army.DuplicateExceeded =>
      DuplicateExceeded("DuplicateExceeded", DatasheetId.value(e.datasheetId), e.unitName, e.count, e.maxAllowed)
    case e: wahapedia.domain.army.DuplicateEpicHero =>
      DuplicateEpicHero("DuplicateEpicHero", DatasheetId.value(e.datasheetId), e.unitName)
    case e: wahapedia.domain.army.InvalidLeaderAttachment =>
      InvalidLeaderAttachment("InvalidLeaderAttachment", DatasheetId.value(e.leaderId), DatasheetId.value(e.attachedToId))
    case e: wahapedia.domain.army.TooManyEnhancements =>
      TooManyEnhancements("TooManyEnhancements", e.count)
    case e: wahapedia.domain.army.DuplicateEnhancement =>
      DuplicateEnhancement("DuplicateEnhancement", EnhancementId.value(e.enhancementId))
    case e: wahapedia.domain.army.EnhancementOnNonCharacter =>
      EnhancementOnNonCharacter("EnhancementOnNonCharacter", DatasheetId.value(e.datasheetId), EnhancementId.value(e.enhancementId))
    case e: wahapedia.domain.army.MultipleEnhancementsOnUnit =>
      MultipleEnhancementsOnUnit("MultipleEnhancementsOnUnit", DatasheetId.value(e.datasheetId))
    case e: wahapedia.domain.army.EnhancementDetachmentMismatch =>
      EnhancementDetachmentMismatch("EnhancementDetachmentMismatch", EnhancementId.value(e.enhancementId), DetachmentId.value(e.expectedDetachment))
    case e: wahapedia.domain.army.UnitCostNotFound =>
      UnitCostNotFound("UnitCostNotFound", DatasheetId.value(e.datasheetId), e.sizeOptionLine)
    case e: wahapedia.domain.army.DatasheetNotFound =>
      DatasheetNotFound("DatasheetNotFound", DatasheetId.value(e.datasheetId))
  }

  given Encoder[ValidationErrorDto] = Encoder.instance {
    case e: FactionMismatch => e.asJson(using Encoder.AsObject.derived[FactionMismatch])
    case e: PointsExceeded => e.asJson(using Encoder.AsObject.derived[PointsExceeded])
    case e: NoCharacter => e.asJson(using Encoder.AsObject.derived[NoCharacter])
    case e: InvalidWarlord => e.asJson(using Encoder.AsObject.derived[InvalidWarlord])
    case e: WarlordNotInArmy => e.asJson(using Encoder.AsObject.derived[WarlordNotInArmy])
    case e: DuplicateExceeded => e.asJson(using Encoder.AsObject.derived[DuplicateExceeded])
    case e: DuplicateEpicHero => e.asJson(using Encoder.AsObject.derived[DuplicateEpicHero])
    case e: InvalidLeaderAttachment => e.asJson(using Encoder.AsObject.derived[InvalidLeaderAttachment])
    case e: TooManyEnhancements => e.asJson(using Encoder.AsObject.derived[TooManyEnhancements])
    case e: DuplicateEnhancement => e.asJson(using Encoder.AsObject.derived[DuplicateEnhancement])
    case e: EnhancementOnNonCharacter => e.asJson(using Encoder.AsObject.derived[EnhancementOnNonCharacter])
    case e: MultipleEnhancementsOnUnit => e.asJson(using Encoder.AsObject.derived[MultipleEnhancementsOnUnit])
    case e: EnhancementDetachmentMismatch => e.asJson(using Encoder.AsObject.derived[EnhancementDetachmentMismatch])
    case e: UnitCostNotFound => e.asJson(using Encoder.AsObject.derived[UnitCostNotFound])
    case e: DatasheetNotFound => e.asJson(using Encoder.AsObject.derived[DatasheetNotFound])
    case e: Generic => e.asJson(using Encoder.AsObject.derived[Generic])
  }
}

case class RegisterRequest(username: String, password: String, inviteCode: Option[String])
case class LoginRequest(username: String, password: String)
case class AuthResponse(token: String, user: UserResponse)
case class UserResponse(id: String, username: String)
case class InviteResponse(code: String, createdAt: String, used: Boolean)

case class WargearWithQuantity(
  wargear: wahapedia.domain.models.Wargear,
  quantity: Int,
  modelType: Option[String]
)

case class BattleUnitData(
  unit: wahapedia.domain.army.ArmyUnit,
  datasheet: wahapedia.domain.models.Datasheet,
  profiles: List[wahapedia.domain.models.ModelProfile],
  wargear: List[WargearWithQuantity],
  abilities: List[wahapedia.domain.models.DatasheetAbility],
  keywords: List[wahapedia.domain.models.DatasheetKeyword],
  cost: Option[wahapedia.domain.models.UnitCost],
  enhancement: Option[wahapedia.domain.models.Enhancement]
)

case class FilterWargearRequest(
  selections: List[wahapedia.domain.army.WargearSelection],
  unitSize: Int
)

case class ArmyBattleData(
  id: String,
  name: String,
  factionId: String,
  battleSize: String,
  detachmentId: String,
  warlordId: String,
  units: List[BattleUnitData]
)
