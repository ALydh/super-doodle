package wahapedia.db

import doobie.*
import doobie.implicits.*
import cats.effect.IO
import cats.implicits.*

object Schema {

  private val tables: List[Fragment] = List(
    sql"""CREATE TABLE IF NOT EXISTS factions (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      link TEXT NOT NULL
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

    sql"""CREATE TABLE IF NOT EXISTS armies (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      faction_id TEXT NOT NULL,
      battle_size TEXT NOT NULL,
      detachment_id TEXT NOT NULL,
      warlord_id TEXT NOT NULL,
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
      wargear_selections TEXT,
      FOREIGN KEY (army_id) REFERENCES armies(id) ON DELETE CASCADE
    )"""
  )

  def initialize(xa: Transactor[IO]): IO[Unit] =
    tables.traverse_(_.update.run).transact(xa)
}
