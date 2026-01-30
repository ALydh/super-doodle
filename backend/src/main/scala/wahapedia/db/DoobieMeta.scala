package wahapedia.db

import doobie.Meta
import wahapedia.domain.types.*
import wahapedia.domain.models.{StratagemId, EnhancementId, DetachmentAbilityId, WargearAction}

object DoobieMeta {

  given Meta[UserId] = Meta[String].imap(UserId(_))(UserId.value)
  given Meta[SessionToken] = Meta[String].imap(SessionToken(_))(SessionToken.value)
  given Meta[InviteCode] = Meta[String].imap(InviteCode(_))(InviteCode.value)

  given Meta[DatasheetId] = Meta[String].imap(DatasheetId(_))(DatasheetId.value)
  given Meta[FactionId] = Meta[String].imap(FactionId(_))(FactionId.value)
  given Meta[AbilityId] = Meta[String].imap(AbilityId(_))(AbilityId.value)
  given Meta[SourceId] = Meta[String].imap(SourceId(_))(SourceId.value)
  given Meta[DetachmentId] = Meta[String].imap(DetachmentId(_))(DetachmentId.value)
  given Meta[StratagemId] = Meta[String].imap(StratagemId(_))(StratagemId.value)
  given Meta[EnhancementId] = Meta[String].imap(EnhancementId(_))(EnhancementId.value)
  given Meta[DetachmentAbilityId] = Meta[String].imap(DetachmentAbilityId(_))(DetachmentAbilityId.value)
  given Meta[Save] = Meta[Int].imap(Save(_))(Save.value)

  given Meta[Role] = Meta[String].imap(s =>
    Role.parse(s).fold(_ => throw new IllegalArgumentException(s"Invalid role: $s"), identity)
  )(Role.asString)

  given Meta[SourceType] = Meta[String].imap(s =>
    SourceType.parse(s).fold(_ => throw new IllegalArgumentException(s"Invalid source type: $s"), identity)
  )(SourceType.asString)

  given Meta[BattleSize] = Meta[String].imap {
    case "Incursion" => BattleSize.Incursion
    case "StrikeForce" => BattleSize.StrikeForce
    case "Onslaught" => BattleSize.Onslaught
    case s => throw new IllegalArgumentException(s"Invalid battle size: $s")
  }(_.toString)

  given Meta[WargearAction] = Meta[String].imap(s =>
    WargearAction.parse(s).fold(_ => throw new IllegalArgumentException(s"Invalid wargear action: $s"), identity)
  )(WargearAction.asString)
}
