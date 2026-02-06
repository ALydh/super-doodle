package wahapedia.db

import doobie.*
import doobie.implicits.*
import doobie.free.connection.ConnectionIO
import doobie.free.{connection => FC}
import cats.effect.IO
import cats.implicits.*

object Schema {

  private val userTables: List[Fragment] = List(
    sql"""CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY,
      username TEXT UNIQUE NOT NULL,
      password_hash TEXT NOT NULL,
      is_admin INTEGER NOT NULL DEFAULT 0,
      created_at TEXT NOT NULL
    )""",

    sql"""CREATE TABLE IF NOT EXISTS sessions (
      token TEXT PRIMARY KEY,
      user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      created_at TEXT NOT NULL,
      expires_at TEXT NOT NULL
    )""",

    sql"""CREATE TABLE IF NOT EXISTS invites (
      code TEXT PRIMARY KEY,
      created_by TEXT,
      created_at TEXT NOT NULL,
      used_by TEXT,
      used_at TEXT
    )""",

    sql"""CREATE TABLE IF NOT EXISTS armies (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      faction_id TEXT NOT NULL,
      battle_size TEXT NOT NULL,
      detachment_id TEXT NOT NULL,
      warlord_id TEXT NOT NULL,
      owner_id TEXT REFERENCES users(id),
      created_at TEXT NOT NULL,
      updated_at TEXT NOT NULL
    )""",

    sql"""CREATE TABLE IF NOT EXISTS army_units (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      army_id TEXT NOT NULL,
      datasheet_id TEXT NOT NULL,
      size_option_line INTEGER NOT NULL,
      enhancement_id TEXT,
      attached_leader_id TEXT,
      FOREIGN KEY (army_id) REFERENCES armies(id) ON DELETE CASCADE
    )""",

    sql"""CREATE TABLE IF NOT EXISTS army_unit_wargear_selections (
      army_unit_id INTEGER NOT NULL,
      option_line INTEGER NOT NULL,
      selected INTEGER NOT NULL,
      notes TEXT,
      PRIMARY KEY (army_unit_id, option_line),
      FOREIGN KEY (army_unit_id) REFERENCES army_units(id) ON DELETE CASCADE
    )""",

    sql"""CREATE TABLE IF NOT EXISTS user_inventory (
      user_id TEXT NOT NULL,
      datasheet_id TEXT NOT NULL,
      quantity INTEGER NOT NULL DEFAULT 1,
      PRIMARY KEY (user_id, datasheet_id),
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    )"""
  )

  private val refTables: List[Fragment] = List(
    sql"""CREATE TABLE IF NOT EXISTS factions (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      link TEXT NOT NULL,
      faction_group TEXT
    )""",

    sql"""CREATE TABLE IF NOT EXISTS sources (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      source_type TEXT NOT NULL,
      edition INTEGER NOT NULL,
      version TEXT,
      errata_date TEXT,
      errata_link TEXT
    )""",

    sql"""CREATE TABLE IF NOT EXISTS abilities (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      legend TEXT,
      faction_id TEXT,
      description TEXT NOT NULL
    )""",

    sql"""CREATE TABLE IF NOT EXISTS datasheets (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      faction_id TEXT,
      source_id TEXT,
      legend TEXT,
      role TEXT,
      loadout TEXT,
      transport TEXT,
      virtual INTEGER NOT NULL,
      leader_head TEXT,
      leader_footer TEXT,
      damaged_w TEXT,
      damaged_description TEXT,
      link TEXT NOT NULL
    )""",

    sql"""CREATE TABLE IF NOT EXISTS model_profiles (
      datasheet_id TEXT NOT NULL,
      line INTEGER NOT NULL,
      name TEXT,
      movement TEXT NOT NULL,
      toughness TEXT NOT NULL,
      save INTEGER NOT NULL,
      invulnerable_save TEXT,
      invulnerable_save_description TEXT,
      wounds INTEGER NOT NULL,
      leadership TEXT NOT NULL,
      objective_control INTEGER NOT NULL,
      base_size TEXT,
      base_size_description TEXT,
      PRIMARY KEY (datasheet_id, line)
    )""",

    sql"""CREATE TABLE IF NOT EXISTS wargear (
      datasheet_id TEXT NOT NULL,
      line INTEGER,
      line_in_wargear INTEGER,
      dice TEXT,
      name TEXT,
      description TEXT,
      range TEXT,
      weapon_type TEXT,
      attacks TEXT,
      ballistic_skill TEXT,
      strength TEXT,
      armor_penetration TEXT,
      damage TEXT
    )""",

    sql"""CREATE TABLE IF NOT EXISTS unit_composition (
      datasheet_id TEXT NOT NULL,
      line INTEGER NOT NULL,
      description TEXT NOT NULL,
      PRIMARY KEY (datasheet_id, line)
    )""",

    sql"""CREATE TABLE IF NOT EXISTS unit_cost (
      datasheet_id TEXT NOT NULL,
      line INTEGER NOT NULL,
      description TEXT NOT NULL,
      cost INTEGER NOT NULL,
      PRIMARY KEY (datasheet_id, line)
    )""",

    sql"""CREATE TABLE IF NOT EXISTS last_update (
      timestamp TEXT NOT NULL
    )""",

    sql"""CREATE TABLE IF NOT EXISTS datasheet_keywords (
      datasheet_id TEXT NOT NULL,
      keyword TEXT,
      model TEXT,
      is_faction_keyword INTEGER NOT NULL
    )""",

    sql"""CREATE TABLE IF NOT EXISTS datasheet_abilities (
      datasheet_id TEXT NOT NULL,
      line INTEGER NOT NULL,
      ability_id TEXT,
      model TEXT,
      name TEXT,
      description TEXT,
      ability_type TEXT,
      parameter TEXT
    )""",

    sql"""CREATE TABLE IF NOT EXISTS datasheet_leaders (
      leader_id TEXT NOT NULL,
      attached_id TEXT NOT NULL,
      PRIMARY KEY (leader_id, attached_id)
    )""",

    sql"""CREATE TABLE IF NOT EXISTS datasheet_options (
      datasheet_id TEXT NOT NULL,
      line INTEGER NOT NULL,
      button TEXT,
      description TEXT NOT NULL
    )""",

    sql"""CREATE TABLE IF NOT EXISTS stratagems (
      id TEXT PRIMARY KEY,
      faction_id TEXT,
      name TEXT NOT NULL,
      stratagem_type TEXT,
      cp_cost INTEGER,
      legend TEXT,
      turn TEXT,
      phase TEXT,
      detachment TEXT,
      detachment_id TEXT,
      description TEXT NOT NULL
    )""",

    sql"""CREATE TABLE IF NOT EXISTS datasheet_stratagems (
      datasheet_id TEXT NOT NULL,
      stratagem_id TEXT NOT NULL,
      PRIMARY KEY (datasheet_id, stratagem_id)
    )""",

    sql"""CREATE TABLE IF NOT EXISTS enhancements (
      id TEXT PRIMARY KEY,
      faction_id TEXT NOT NULL,
      name TEXT NOT NULL,
      cost INTEGER NOT NULL,
      detachment TEXT,
      detachment_id TEXT,
      legend TEXT,
      description TEXT NOT NULL
    )""",

    sql"""CREATE TABLE IF NOT EXISTS datasheet_enhancements (
      datasheet_id TEXT NOT NULL,
      enhancement_id TEXT NOT NULL,
      PRIMARY KEY (datasheet_id, enhancement_id)
    )""",

    sql"""CREATE TABLE IF NOT EXISTS detachment_abilities (
      id TEXT PRIMARY KEY,
      faction_id TEXT NOT NULL,
      name TEXT NOT NULL,
      legend TEXT,
      description TEXT NOT NULL,
      detachment TEXT NOT NULL,
      detachment_id TEXT NOT NULL
    )""",

    sql"""CREATE TABLE IF NOT EXISTS datasheet_detachment_abilities (
      datasheet_id TEXT NOT NULL,
      detachment_ability_id TEXT NOT NULL,
      PRIMARY KEY (datasheet_id, detachment_ability_id)
    )""",

    sql"""CREATE TABLE IF NOT EXISTS weapon_abilities (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      description TEXT NOT NULL
    )""",

    sql"""CREATE TABLE IF NOT EXISTS parsed_wargear_options (
      datasheet_id TEXT NOT NULL,
      option_line INTEGER NOT NULL,
      choice_index INTEGER NOT NULL,
      group_id INTEGER NOT NULL,
      action TEXT NOT NULL,
      weapon_name TEXT NOT NULL,
      model_target TEXT,
      count_per_n_models INTEGER NOT NULL,
      max_count INTEGER NOT NULL
    )""",

    sql"""CREATE TABLE IF NOT EXISTS parsed_loadouts (
      datasheet_id TEXT NOT NULL,
      model_pattern TEXT NOT NULL,
      weapon TEXT NOT NULL,
      PRIMARY KEY (datasheet_id, model_pattern, weapon)
    )""",

    sql"""CREATE TABLE IF NOT EXISTS unit_wargear_defaults (
      datasheet_id TEXT NOT NULL,
      size_line INTEGER NOT NULL,
      weapon TEXT NOT NULL,
      count INTEGER NOT NULL,
      model_type TEXT,
      PRIMARY KEY (datasheet_id, size_line, weapon)
    )""",

    sql"""CREATE TABLE IF NOT EXISTS parsed_unit_composition (
      datasheet_id TEXT NOT NULL,
      line INTEGER NOT NULL,
      group_index INTEGER NOT NULL,
      model_name TEXT NOT NULL,
      min_count INTEGER NOT NULL,
      max_count INTEGER NOT NULL,
      PRIMARY KEY (datasheet_id, line, model_name)
    )"""
  )

  private val migrateArmies: ConnectionIO[Unit] = for {
    hasOwnerCol <- sql"PRAGMA table_info(armies)".query[(Int, String, String, Int, Option[String], Int)].to[List].map(_.exists(_._2 == "owner_id"))
    _ <- if (!hasOwnerCol) {
      sql"DROP TABLE IF EXISTS army_unit_wargear_selections".update.run *>
      sql"DROP TABLE IF EXISTS army_units".update.run *>
      sql"DROP TABLE IF EXISTS armies".update.run
    } else {
      FC.unit
    }
  } yield ()

  private val migrateIsAdmin: ConnectionIO[Unit] = for {
    hasCol <- sql"PRAGMA table_info(users)".query[(Int, String, String, Int, Option[String], Int)].to[List].map(_.exists(_._2 == "is_admin"))
    _ <- if (!hasCol) {
      for {
        firstUserId <- sql"SELECT id FROM users ORDER BY created_at ASC LIMIT 1".query[String].option
        _ <- sql"ALTER TABLE users ADD COLUMN is_admin INTEGER NOT NULL DEFAULT 0".update.run
        _ <- firstUserId match {
          case Some(id) => sql"UPDATE users SET is_admin = 1 WHERE id = $id".update.run
          case None => FC.unit
        }
      } yield ()
    } else {
      FC.unit
    }
  } yield ()

  private val migrateFactionGroup: ConnectionIO[Unit] = for {
    hasCol <- sql"PRAGMA table_info(factions)".query[(Int, String, String, Int, Option[String], Int)].to[List].map(_.exists(_._2 == "faction_group"))
    _ <- if (!hasCol) sql"ALTER TABLE factions ADD COLUMN faction_group TEXT".update.run else FC.unit
  } yield ()

  private val migrateChapterId: ConnectionIO[Unit] = for {
    hasCol <- sql"PRAGMA table_info(armies)".query[(Int, String, String, Int, Option[String], Int)].to[List].map(_.exists(_._2 == "chapter_id"))
    _ <- if (!hasCol) sql"ALTER TABLE armies ADD COLUMN chapter_id TEXT".update.run else FC.unit
  } yield ()

  private val populateFactionGroups: ConnectionIO[Unit] = {
    val imperium = List("AS", "AC", "AdM", "TL", "AM", "GK", "AoI", "QI", "SM")
    val chaos = List("CD", "QT", "CSM", "DG", "EC", "TS", "WE")
    val xenos = List("AE", "DRU", "GC", "LoV", "NEC", "ORK", "TAU", "TYR")

    for {
      _ <- imperium.traverse_(id => sql"UPDATE factions SET faction_group = 'Imperium' WHERE id = $id".update.run)
      _ <- chaos.traverse_(id => sql"UPDATE factions SET faction_group = 'Chaos' WHERE id = $id".update.run)
      _ <- xenos.traverse_(id => sql"UPDATE factions SET faction_group = 'Xenos' WHERE id = $id".update.run)
    } yield ()
  }

  def initializeRefSchema(xa: Transactor[IO]): IO[Unit] =
    (refTables.traverse_(_.update.run) *> migrateFactionGroup *> populateFactionGroups).transact(xa)

  def initializeUserSchema(xa: Transactor[IO]): IO[Unit] =
    (userTables.traverse_(_.update.run) *> migrateArmies *> migrateIsAdmin *> migrateChapterId).transact(xa)

  def initialize(xa: Transactor[IO]): IO[Unit] =
    initializeRefSchema(xa) *> initializeUserSchema(xa)
}
