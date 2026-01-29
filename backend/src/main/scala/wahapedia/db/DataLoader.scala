package wahapedia.db

import doobie.*
import doobie.implicits.*
import cats.effect.IO
import cats.implicits.*
import wahapedia.domain.models.*
import wahapedia.domain.types.*
import wahapedia.csv.{CsvProcessor, StreamingCsvParser}
import java.nio.file.{Files, Paths}
import DoobieMeta.given

object DataLoader {

  private val dataDir = "../data/wahapedia"

  def loadAll(xa: Transactor[IO]): IO[Unit] =
    for {
      _ <- clearAll(xa)
      _ <- loadFile("Factions.csv", FactionParser, insertFaction)(xa)
      _ <- loadFile("Source.csv", SourceParser, insertSource)(xa)
      _ <- loadFile("Abilities.csv", AbilityParser, insertAbility)(xa)
      _ <- loadFile("Datasheets.csv", DatasheetParser, insertDatasheet)(xa)
      _ <- loadFile("Datasheets_models.csv", ModelProfileParser, insertModelProfile)(xa)
      _ <- loadFile("Datasheets_wargear.csv", WargearParser, insertWargear)(xa)
      _ <- loadFile("Datasheets_unit_composition.csv", UnitCompositionParser, insertUnitComposition)(xa)
      _ <- loadFile("Datasheets_models_cost.csv", UnitCostParser, insertUnitCost)(xa)
      _ <- loadFile("Datasheets_keywords.csv", DatasheetKeywordParser, insertDatasheetKeyword)(xa)
      _ <- loadFile("Datasheets_abilities.csv", DatasheetAbilityParser, insertDatasheetAbility)(xa)
      _ <- loadFile("Datasheets_options.csv", DatasheetOptionParser, insertDatasheetOption)(xa)
      _ <- loadFile("Datasheets_leader.csv", DatasheetLeaderParser, insertDatasheetLeader)(xa)
      _ <- loadFile("Stratagems.csv", StratagemParser, insertStratagem)(xa)
      _ <- loadFile("Datasheets_stratagems.csv", DatasheetStratagemParser, insertDatasheetStratagem)(xa)
      _ <- loadFile("Enhancements.csv", EnhancementParser, insertEnhancement)(xa)
      _ <- loadFile("Datasheets_enhancements.csv", DatasheetEnhancementParser, insertDatasheetEnhancement)(xa)
      _ <- loadFile("Detachment_abilities.csv", DetachmentAbilityParser, insertDetachmentAbility)(xa)
      _ <- loadFile("Datasheets_detachment_abilities.csv", DatasheetDetachmentAbilityParser, insertDatasheetDetachmentAbility)(xa)
      _ <- loadFile("Last_update.csv", LastUpdateParser, insertLastUpdate)(xa)
      _ <- loadFileIfExists("Weapon_abilities.csv", WeaponAbilityParser, insertWeaponAbility)(xa)
    } yield ()

  private def loadFile[A](
    filename: String,
    parser: StreamingCsvParser[A],
    insert: A => ConnectionIO[Int]
  )(xa: Transactor[IO]): IO[Unit] =
    for {
      _ <- IO.println(s"Loading $filename...")
      records <- CsvProcessor.failFastParse(s"$dataDir/$filename", parser)
      _ <- records.traverse_(insert).transact(xa)
      _ <- IO.println(s"  Loaded ${records.length} records from $filename")
    } yield ()

  private def loadFileIfExists[A](
    filename: String,
    parser: StreamingCsvParser[A],
    insert: A => ConnectionIO[Int]
  )(xa: Transactor[IO]): IO[Unit] = {
    val path = Paths.get(s"$dataDir/$filename")
    IO(Files.exists(path)).flatMap {
      case true  => loadFile(filename, parser, insert)(xa)
      case false => IO.println(s"Skipping $filename (file not found)")
    }
  }

  private def clearAll(xa: Transactor[IO]): IO[Unit] = {
    val deletes = List(
      sql"DELETE FROM weapon_abilities",
      sql"DELETE FROM datasheet_detachment_abilities",
      sql"DELETE FROM detachment_abilities",
      sql"DELETE FROM datasheet_enhancements",
      sql"DELETE FROM enhancements",
      sql"DELETE FROM datasheet_stratagems",
      sql"DELETE FROM stratagems",
      sql"DELETE FROM datasheet_options",
      sql"DELETE FROM datasheet_leaders",
      sql"DELETE FROM datasheet_abilities",
      sql"DELETE FROM datasheet_keywords",
      sql"DELETE FROM last_update",
      sql"DELETE FROM unit_cost",
      sql"DELETE FROM unit_composition",
      sql"DELETE FROM wargear",
      sql"DELETE FROM model_profiles",
      sql"DELETE FROM datasheets",
      sql"DELETE FROM abilities",
      sql"DELETE FROM sources",
      sql"DELETE FROM factions"
    )
    deletes.traverse_(_.update.run).transact(xa)
  }

  private def insertFaction(f: Faction): ConnectionIO[Int] =
    sql"INSERT INTO factions (id, name, link) VALUES (${f.id}, ${f.name}, ${f.link})".update.run

  private def insertSource(s: Source): ConnectionIO[Int] =
    sql"""INSERT INTO sources (id, name, source_type, edition, version, errata_date, errata_link)
          VALUES (${s.id}, ${s.name}, ${SourceType.asString(s.sourceType)}, ${s.edition}, ${s.version}, ${s.errataDate}, ${s.errataLink})""".update.run

  private def insertAbility(a: Ability): ConnectionIO[Int] =
    sql"""INSERT OR REPLACE INTO abilities (id, name, legend, faction_id, description)
          VALUES (${a.id}, ${a.name}, ${a.legend}, ${a.factionId}, ${a.description})""".update.run

  private def insertDatasheet(d: Datasheet): ConnectionIO[Int] =
    sql"""INSERT INTO datasheets (id, name, faction_id, source_id, legend, role, loadout, transport, virtual, leader_head, leader_footer, damaged_w, damaged_description, link)
          VALUES (${d.id}, ${d.name}, ${d.factionId}, ${d.sourceId}, ${d.legend}, ${d.role.map(Role.asString)}, ${d.loadout}, ${d.transport}, ${if (d.virtual) 1 else 0}, ${d.leaderHead}, ${d.leaderFooter}, ${d.damagedW}, ${d.damagedDescription}, ${d.link})""".update.run

  private def insertModelProfile(m: ModelProfile): ConnectionIO[Int] =
    sql"""INSERT INTO model_profiles (datasheet_id, line, name, movement, toughness, save, invulnerable_save, invulnerable_save_description, wounds, leadership, objective_control, base_size, base_size_description)
          VALUES (${m.datasheetId}, ${m.line}, ${m.name}, ${m.movement}, ${m.toughness}, ${m.save}, ${m.invulnerableSave}, ${m.invulnerableSaveDescription}, ${m.wounds}, ${m.leadership}, ${m.objectiveControl}, ${m.baseSize}, ${m.baseSizeDescription})""".update.run

  private def insertWargear(w: Wargear): ConnectionIO[Int] =
    sql"""INSERT INTO wargear (datasheet_id, line, line_in_wargear, dice, name, description, range, weapon_type, attacks, ballistic_skill, strength, armor_penetration, damage)
          VALUES (${w.datasheetId}, ${w.line}, ${w.lineInWargear}, ${w.dice}, ${w.name}, ${w.description}, ${w.range}, ${w.weaponType}, ${w.attacks}, ${w.ballisticSkill}, ${w.strength}, ${w.armorPenetration}, ${w.damage})""".update.run

  private def insertUnitComposition(uc: UnitComposition): ConnectionIO[Int] =
    sql"""INSERT INTO unit_composition (datasheet_id, line, description)
          VALUES (${uc.datasheetId}, ${uc.line}, ${uc.description})""".update.run

  private def insertUnitCost(uc: UnitCost): ConnectionIO[Int] =
    sql"""INSERT INTO unit_cost (datasheet_id, line, description, cost)
          VALUES (${uc.datasheetId}, ${uc.line}, ${uc.description}, ${uc.cost})""".update.run

  private def insertDatasheetKeyword(dk: DatasheetKeyword): ConnectionIO[Int] =
    sql"""INSERT INTO datasheet_keywords (datasheet_id, keyword, model, is_faction_keyword)
          VALUES (${dk.datasheetId}, ${dk.keyword}, ${dk.model}, ${if (dk.isFactionKeyword) 1 else 0})""".update.run

  private def insertDatasheetAbility(da: DatasheetAbility): ConnectionIO[Int] =
    sql"""INSERT INTO datasheet_abilities (datasheet_id, line, ability_id, model, name, description, ability_type, parameter)
          VALUES (${da.datasheetId}, ${da.line}, ${da.abilityId}, ${da.model}, ${da.name}, ${da.description}, ${da.abilityType}, ${da.parameter})""".update.run

  private def insertDatasheetOption(o: DatasheetOption): ConnectionIO[Int] =
    sql"""INSERT INTO datasheet_options (datasheet_id, line, button, description)
          VALUES (${o.datasheetId}, ${o.line}, ${o.button}, ${o.description})""".update.run

  private def insertDatasheetLeader(dl: DatasheetLeader): ConnectionIO[Int] =
    sql"""INSERT OR REPLACE INTO datasheet_leaders (leader_id, attached_id)
          VALUES (${dl.leaderId}, ${dl.attachedId})""".update.run

  private def insertStratagem(s: Stratagem): ConnectionIO[Int] =
    sql"""INSERT INTO stratagems (id, faction_id, name, stratagem_type, cp_cost, legend, turn, phase, detachment, detachment_id, description)
          VALUES (${s.id}, ${s.factionId}, ${s.name}, ${s.stratagemType}, ${s.cpCost}, ${s.legend}, ${s.turn}, ${s.phase}, ${s.detachment}, ${s.detachmentId}, ${s.description})""".update.run

  private def insertDatasheetStratagem(ds: DatasheetStratagem): ConnectionIO[Int] =
    sql"""INSERT INTO datasheet_stratagems (datasheet_id, stratagem_id)
          VALUES (${ds.datasheetId}, ${ds.stratagemId})""".update.run

  private def insertEnhancement(e: Enhancement): ConnectionIO[Int] =
    sql"""INSERT INTO enhancements (id, faction_id, name, cost, detachment, detachment_id, legend, description)
          VALUES (${e.id}, ${e.factionId}, ${e.name}, ${e.cost}, ${e.detachment}, ${e.detachmentId}, ${e.legend}, ${e.description})""".update.run

  private def insertDatasheetEnhancement(de: DatasheetEnhancement): ConnectionIO[Int] =
    sql"""INSERT INTO datasheet_enhancements (datasheet_id, enhancement_id)
          VALUES (${de.datasheetId}, ${de.enhancementId})""".update.run

  private def insertDetachmentAbility(da: DetachmentAbility): ConnectionIO[Int] =
    sql"""INSERT INTO detachment_abilities (id, faction_id, name, legend, description, detachment, detachment_id)
          VALUES (${da.id}, ${da.factionId}, ${da.name}, ${da.legend}, ${da.description}, ${da.detachment}, ${da.detachmentId})""".update.run

  private def insertDatasheetDetachmentAbility(dda: DatasheetDetachmentAbility): ConnectionIO[Int] =
    sql"""INSERT INTO datasheet_detachment_abilities (datasheet_id, detachment_ability_id)
          VALUES (${dda.datasheetId}, ${dda.detachmentAbilityId})""".update.run

  private def insertLastUpdate(lu: LastUpdate): ConnectionIO[Int] =
    sql"INSERT INTO last_update (timestamp) VALUES (${lu.timestamp})".update.run

  private def insertWeaponAbility(wa: WeaponAbility): ConnectionIO[Int] =
    sql"INSERT INTO weapon_abilities (id, name, description) VALUES (${wa.id}, ${wa.name}, ${wa.description})".update.run

  def loadWeaponAbilities(xa: Transactor[IO]): IO[Unit] =
    loadFileIfExists("Weapon_abilities.csv", WeaponAbilityParser, insertWeaponAbility)(xa)
}
