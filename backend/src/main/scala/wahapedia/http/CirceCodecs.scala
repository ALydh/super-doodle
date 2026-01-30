package wahapedia.http

import io.circe.{Encoder, Decoder}
import wahapedia.domain.army.{Army, ArmyUnit, WargearSelection}
import java.util.UUID

object CirceCodecs {
  given Encoder[UUID] = Encoder.encodeString.contramap(_.toString)
  given Decoder[UUID] = Decoder.decodeString.emap(s =>
    scala.util.Try(UUID.fromString(s)).toEither.left.map(_.getMessage)
  )

  given Encoder[WargearSelection] = Encoder.forProduct3("optionLine", "selected", "notes")(
    (w: WargearSelection) => (w.optionLine, w.selected, w.notes)
  )
  given Decoder[WargearSelection] = Decoder.forProduct3("optionLine", "selected", "notes")(
    WargearSelection.apply
  )

  given Encoder[ArmyUnit] = Encoder.forProduct5("datasheetId", "sizeOptionLine", "enhancementId", "attachedLeaderId", "wargearSelections")(
    (u: ArmyUnit) => (u.datasheetId, u.sizeOptionLine, u.enhancementId, u.attachedLeaderId, u.wargearSelections)
  )
  given Decoder[ArmyUnit] = Decoder.forProduct5("datasheetId", "sizeOptionLine", "enhancementId", "attachedLeaderId", "wargearSelections")(
    ArmyUnit.apply
  )

  given Encoder[Army] = Encoder.forProduct5("factionId", "battleSize", "detachmentId", "warlordId", "units")(
    (a: Army) => (a.factionId, a.battleSize, a.detachmentId, a.warlordId, a.units)
  )
  given Decoder[Army] = Decoder.forProduct5("factionId", "battleSize", "detachmentId", "warlordId", "units")(
    Army.apply
  )
}
