package wahapedia.db

import doobie.*
import doobie.implicits.*
import cats.effect.IO
import cats.implicits.*
import wahapedia.domain.models.*
import wahapedia.domain.types.*
import wahapedia.domain.army.ReferenceData
import DoobieMeta.given

object ReferenceDataRepository {

  def allFactions(xa: Transactor[IO]): IO[List[Faction]] =
    sql"SELECT id, name, link FROM factions"
      .query[Faction].to[List].transact(xa)

  def allSources(xa: Transactor[IO]): IO[List[Source]] =
    sql"SELECT id, name, source_type, edition, version, errata_date, errata_link FROM sources"
      .query[Source].to[List].transact(xa)

  def allAbilities(xa: Transactor[IO]): IO[List[Ability]] =
    sql"SELECT id, name, legend, faction_id, description FROM abilities"
      .query[Ability].to[List].transact(xa)

  def allDatasheets(xa: Transactor[IO]): IO[List[Datasheet]] =
    sql"""SELECT id, name, faction_id, source_id, legend, role, loadout, transport,
           virtual, leader_head, leader_footer, damaged_w, damaged_description, link
           FROM datasheets"""
      .query[Datasheet].to[List].transact(xa)

  def datasheetById(datasheetId: DatasheetId)(xa: Transactor[IO]): IO[Option[Datasheet]] =
    sql"""SELECT id, name, faction_id, source_id, legend, role, loadout, transport,
           virtual, leader_head, leader_footer, damaged_w, damaged_description, link
           FROM datasheets WHERE id = $datasheetId"""
      .query[Datasheet].option.transact(xa)

  def datasheetsByFaction(factionId: FactionId)(xa: Transactor[IO]): IO[List[Datasheet]] =
    sql"""SELECT id, name, faction_id, source_id, legend, role, loadout, transport,
           virtual, leader_head, leader_footer, damaged_w, damaged_description, link
           FROM datasheets WHERE faction_id = $factionId"""
      .query[Datasheet].to[List].transact(xa)

  def allModelProfiles(xa: Transactor[IO]): IO[List[ModelProfile]] =
    sql"""SELECT datasheet_id, line, name, movement, toughness, save,
           invulnerable_save, invulnerable_save_description, wounds, leadership,
           objective_control, base_size, base_size_description
           FROM model_profiles"""
      .query[ModelProfile].to[List].transact(xa)

  def modelProfilesForDatasheet(datasheetId: DatasheetId)(xa: Transactor[IO]): IO[List[ModelProfile]] =
    sql"""SELECT datasheet_id, line, name, movement, toughness, save,
           invulnerable_save, invulnerable_save_description, wounds, leadership,
           objective_control, base_size, base_size_description
           FROM model_profiles WHERE datasheet_id = $datasheetId"""
      .query[ModelProfile].to[List].transact(xa)

  def allWargear(xa: Transactor[IO]): IO[List[Wargear]] =
    sql"""SELECT datasheet_id, line, line_in_wargear, dice, name, description,
           range, weapon_type, attacks, ballistic_skill, strength, armor_penetration, damage
           FROM wargear"""
      .query[Wargear].to[List].transact(xa)

  def wargearForDatasheet(datasheetId: DatasheetId)(xa: Transactor[IO]): IO[List[Wargear]] =
    sql"""SELECT datasheet_id, line, line_in_wargear, dice, name, description,
           range, weapon_type, attacks, ballistic_skill, strength, armor_penetration, damage
           FROM wargear WHERE datasheet_id = $datasheetId"""
      .query[Wargear].to[List].transact(xa)

  def allUnitCompositions(xa: Transactor[IO]): IO[List[UnitComposition]] =
    sql"SELECT datasheet_id, line, description FROM unit_composition"
      .query[UnitComposition].to[List].transact(xa)

  def allUnitCosts(xa: Transactor[IO]): IO[List[UnitCost]] =
    sql"SELECT datasheet_id, line, description, cost FROM unit_cost"
      .query[UnitCost].to[List].transact(xa)

  def unitCostsForDatasheet(datasheetId: DatasheetId)(xa: Transactor[IO]): IO[List[UnitCost]] =
    sql"SELECT datasheet_id, line, description, cost FROM unit_cost WHERE datasheet_id = $datasheetId"
      .query[UnitCost].to[List].transact(xa)

  def allKeywords(xa: Transactor[IO]): IO[List[DatasheetKeyword]] =
    sql"SELECT datasheet_id, keyword, model, is_faction_keyword FROM datasheet_keywords"
      .query[DatasheetKeyword].to[List].transact(xa)

  def keywordsForDatasheet(datasheetId: DatasheetId)(xa: Transactor[IO]): IO[List[DatasheetKeyword]] =
    sql"SELECT datasheet_id, keyword, model, is_faction_keyword FROM datasheet_keywords WHERE datasheet_id = $datasheetId"
      .query[DatasheetKeyword].to[List].transact(xa)

  def allDatasheetAbilities(xa: Transactor[IO]): IO[List[DatasheetAbility]] =
    sql"SELECT datasheet_id, line, ability_id, model, name, description, ability_type, parameter FROM datasheet_abilities"
      .query[DatasheetAbility].to[List].transact(xa)

  def abilitiesForDatasheet(datasheetId: DatasheetId)(xa: Transactor[IO]): IO[List[DatasheetAbility]] =
    sql"SELECT datasheet_id, line, ability_id, model, name, description, ability_type, parameter FROM datasheet_abilities WHERE datasheet_id = $datasheetId"
      .query[DatasheetAbility].to[List].transact(xa)

  def allDatasheetOptions(xa: Transactor[IO]): IO[List[DatasheetOption]] =
    sql"SELECT datasheet_id, line, button, description FROM datasheet_options"
      .query[DatasheetOption].to[List].transact(xa)

  def optionsForDatasheet(datasheetId: DatasheetId)(xa: Transactor[IO]): IO[List[DatasheetOption]] =
    sql"SELECT datasheet_id, line, button, description FROM datasheet_options WHERE datasheet_id = $datasheetId"
      .query[DatasheetOption].to[List].transact(xa)

  def allDatasheetLeaders(xa: Transactor[IO]): IO[List[DatasheetLeader]] =
    sql"SELECT leader_id, attached_id FROM datasheet_leaders"
      .query[DatasheetLeader].to[List].transact(xa)

  def allStratagems(xa: Transactor[IO]): IO[List[Stratagem]] =
    sql"""SELECT faction_id, name, id, stratagem_type, cp_cost, legend, turn, phase,
           detachment, detachment_id, description FROM stratagems"""
      .query[Stratagem].to[List].transact(xa)

  def stratagemsByFaction(factionId: FactionId)(xa: Transactor[IO]): IO[List[Stratagem]] =
    sql"""SELECT faction_id, name, id, stratagem_type, cp_cost, legend, turn, phase,
           detachment, detachment_id, description FROM stratagems WHERE faction_id = $factionId"""
      .query[Stratagem].to[List].transact(xa)

  def allDatasheetStratagems(xa: Transactor[IO]): IO[List[DatasheetStratagem]] =
    sql"SELECT datasheet_id, stratagem_id FROM datasheet_stratagems"
      .query[DatasheetStratagem].to[List].transact(xa)

  def stratagemsByDatasheet(datasheetId: DatasheetId)(xa: Transactor[IO]): IO[List[Stratagem]] =
    sql"""SELECT s.faction_id, s.name, s.id, s.stratagem_type, s.cp_cost, s.legend, s.turn, s.phase,
           s.detachment, s.detachment_id, s.description
           FROM stratagems s
           JOIN datasheet_stratagems ds ON ds.stratagem_id = s.id
           WHERE ds.datasheet_id = $datasheetId"""
      .query[Stratagem].to[List].transact(xa)

  def allEnhancements(xa: Transactor[IO]): IO[List[Enhancement]] =
    sql"SELECT faction_id, id, name, cost, detachment, detachment_id, legend, description FROM enhancements"
      .query[Enhancement].to[List].transact(xa)

  def enhancementsByFaction(factionId: FactionId)(xa: Transactor[IO]): IO[List[Enhancement]] =
    sql"SELECT faction_id, id, name, cost, detachment, detachment_id, legend, description FROM enhancements WHERE faction_id = $factionId"
      .query[Enhancement].to[List].transact(xa)

  def allDatasheetEnhancements(xa: Transactor[IO]): IO[List[DatasheetEnhancement]] =
    sql"SELECT datasheet_id, enhancement_id FROM datasheet_enhancements"
      .query[DatasheetEnhancement].to[List].transact(xa)

  def allDetachmentAbilities(xa: Transactor[IO]): IO[List[DetachmentAbility]] =
    sql"SELECT id, faction_id, name, legend, description, detachment, detachment_id FROM detachment_abilities"
      .query[DetachmentAbility].to[List].transact(xa)

  def detachmentAbilitiesByDetachmentId(detachmentId: String)(xa: Transactor[IO]): IO[List[DetachmentAbility]] =
    sql"SELECT id, faction_id, name, legend, description, detachment, detachment_id FROM detachment_abilities WHERE detachment_id = $detachmentId"
      .query[DetachmentAbility].to[List].transact(xa)

  def allDatasheetDetachmentAbilities(xa: Transactor[IO]): IO[List[DatasheetDetachmentAbility]] =
    sql"SELECT datasheet_id, detachment_ability_id FROM datasheet_detachment_abilities"
      .query[DatasheetDetachmentAbility].to[List].transact(xa)

  def lastUpdate(xa: Transactor[IO]): IO[List[LastUpdate]] =
    sql"SELECT timestamp FROM last_update"
      .query[LastUpdate].to[List].transact(xa)

  case class DetachmentInfo(name: String, detachmentId: String)

  def detachmentsByFaction(factionId: FactionId)(xa: Transactor[IO]): IO[List[DetachmentInfo]] =
    sql"SELECT DISTINCT detachment, detachment_id FROM detachment_abilities WHERE faction_id = $factionId"
      .query[DetachmentInfo].to[List].transact(xa)

  def leadersByFaction(factionId: FactionId)(xa: Transactor[IO]): IO[List[DatasheetLeader]] =
    sql"""SELECT dl.leader_id, dl.attached_id
          FROM datasheet_leaders dl
          JOIN datasheets d ON dl.leader_id = d.id
          WHERE d.faction_id = $factionId"""
      .query[DatasheetLeader].to[List].transact(xa)

  def loadReferenceData(xa: Transactor[IO]): IO[ReferenceData] =
    for {
      datasheets <- allDatasheets(xa)
      keywords <- allKeywords(xa)
      unitCosts <- allUnitCosts(xa)
      enhancements <- allEnhancements(xa)
      leaders <- allDatasheetLeaders(xa)
      detachmentAbilities <- allDetachmentAbilities(xa)
    } yield ReferenceData(datasheets, keywords, unitCosts, enhancements, leaders, detachmentAbilities)

  def counts(xa: Transactor[IO]): IO[Map[String, Int]] = {
    val tables = List(
      "factions", "sources", "abilities", "datasheets", "model_profiles",
      "wargear", "unit_composition", "unit_cost", "datasheet_keywords",
      "datasheet_abilities", "datasheet_options", "datasheet_leaders",
      "stratagems", "datasheet_stratagems", "enhancements",
      "datasheet_enhancements", "detachment_abilities",
      "datasheet_detachment_abilities"
    )
    tables.traverse { table =>
      Fragment.const(s"SELECT COUNT(*) FROM $table").query[Int].unique.transact(xa).map(table -> _)
    }.map(_.toMap)
  }
}
