package wahapedia.http

import io.circe.{Encoder, Decoder, Json}
import io.circe.syntax.*
import wahapedia.domain.army.{Army, ArmyUnit, WargearSelection}
import wahapedia.domain.types.*
import wahapedia.domain.models.*
import wahapedia.http.dto.*
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

  given Encoder[Datasheet] = Encoder.forProduct14(
    "id", "name", "factionId", "sourceId", "legend", "role", "loadout", "transport",
    "virtual", "leaderHead", "leaderFooter", "damagedW", "damagedDescription", "link"
  )((d: Datasheet) => (
    d.id, d.name, d.factionId, d.sourceId, d.legend, d.role, d.loadout, d.transport,
    d.virtual, d.leaderHead, d.leaderFooter, d.damagedW, d.damagedDescription, d.link
  ))

  given Encoder[ModelProfile] = Encoder.forProduct13(
    "datasheetId", "line", "name", "movement", "toughness", "save",
    "invulnerableSave", "invulnerableSaveDescription", "wounds", "leadership",
    "objectiveControl", "baseSize", "baseSizeDescription"
  )((p: ModelProfile) => (
    p.datasheetId, p.line, p.name, p.movement, p.toughness, p.save,
    p.invulnerableSave, p.invulnerableSaveDescription, p.wounds, p.leadership,
    p.objectiveControl, p.baseSize, p.baseSizeDescription
  ))

  given Encoder[Wargear] = Encoder.forProduct13(
    "datasheetId", "line", "lineInWargear", "dice", "name", "description",
    "range", "weaponType", "attacks", "ballisticSkill", "strength", "armorPenetration", "damage"
  )((w: Wargear) => (
    w.datasheetId, w.line, w.lineInWargear, w.dice, w.name, w.description,
    w.range, w.weaponType, w.attacks, w.ballisticSkill, w.strength, w.armorPenetration, w.damage
  ))

  given Encoder[DatasheetAbility] = Encoder.forProduct8(
    "datasheetId", "line", "abilityId", "model", "name", "description", "abilityType", "parameter"
  )((a: DatasheetAbility) => (
    a.datasheetId, a.line, a.abilityId, a.model, a.name, a.description, a.abilityType, a.parameter
  ))

  given Encoder[DatasheetKeyword] = Encoder.forProduct4(
    "datasheetId", "keyword", "model", "isFactionKeyword"
  )((k: DatasheetKeyword) => (k.datasheetId, k.keyword, k.model, k.isFactionKeyword))

  given Encoder[UnitCost] = Encoder.forProduct4(
    "datasheetId", "line", "description", "cost"
  )((c: UnitCost) => (c.datasheetId, c.line, c.description, c.cost))

  given Encoder[ParsedWargearOption] = Encoder.forProduct9(
    "datasheetId", "optionLine", "choiceIndex", "groupId", "action", "weaponName", "modelTarget", "countPerNModels", "maxCount"
  )((o: ParsedWargearOption) => (
    o.datasheetId, o.optionLine, o.choiceIndex, o.groupId, o.action.toString, o.weaponName, o.modelTarget, o.countPerNModels, o.maxCount
  ))

  given Encoder[Enhancement] = Encoder.forProduct8(
    "factionId", "id", "name", "cost", "detachment", "detachmentId", "legend", "description"
  )((e: Enhancement) => (
    e.factionId, e.id, e.name, e.cost, e.detachment, e.detachmentId, e.legend, e.description
  ))

  given Encoder[BattleUnitData] = Encoder.instance { b =>
    Json.obj(
      "unit" -> b.unit.asJson,
      "datasheet" -> b.datasheet.asJson,
      "profiles" -> b.profiles.asJson,
      "wargear" -> b.wargear.asJson,
      "abilities" -> b.abilities.asJson,
      "keywords" -> b.keywords.asJson,
      "parsedWargearOptions" -> b.parsedWargearOptions.asJson,
      "cost" -> b.cost.asJson,
      "enhancement" -> b.enhancement.asJson
    )
  }

  given Encoder[ArmyBattleData] = Encoder.forProduct7(
    "id", "name", "factionId", "battleSize", "detachmentId", "warlordId", "units"
  )((a: ArmyBattleData) => (a.id, a.name, a.factionId, a.battleSize, a.detachmentId, a.warlordId, a.units))
}
