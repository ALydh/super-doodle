package wp40k

import cats.effect.IO
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import java.sql.DriverManager

case class PointChange(datasheetId: String, datasheetName: String, line: Int, description: String, oldCost: Option[Int], newCost: Option[Int])
case class UnitChange(datasheetId: String, name: String, factionId: String, changeType: String)
case class StatChange(datasheetId: String, datasheetName: String, field: String, oldValue: String, newValue: String)
case class EnhancementChange(id: String, name: String, factionId: String, oldCost: Option[Int], newCost: Option[Int], changeType: String, oldDescription: Option[String], newDescription: Option[String])
case class StratagemChange(id: String, name: String, factionId: String, changeType: String, oldCpCost: Option[Int], newCpCost: Option[Int], oldDescription: Option[String], newDescription: Option[String])
case class AbilityChange(id: String, name: String, factionId: String, changeType: String, oldDescription: Option[String], newDescription: Option[String])

case class RevisionDiffResult(
  oldRevisionId: String,
  newRevisionId: String,
  pointChanges: List[PointChange],
  unitChanges: List[UnitChange],
  statChanges: List[StatChange],
  enhancementChanges: List[EnhancementChange],
  stratagemChanges: List[StratagemChange],
  abilityChanges: List[AbilityChange]
)

object RevisionDiff {

  def compute(oldDbPath: String, newDbPath: String, oldId: String, newId: String): IO[RevisionDiffResult] = {
    val xa = buildDiffTransactor(oldDbPath, newDbPath)
    for {
      points <- pointChanges.transact(xa)
      units <- unitChanges.transact(xa)
      stats <- statChanges.transact(xa)
      enhancements <- enhancementChanges.transact(xa)
      stratagems <- stratagemChanges.transact(xa)
      abilities <- abilityChanges.transact(xa)
    } yield RevisionDiffResult(oldId, newId, points, units, stats, enhancements, stratagems, abilities)
  }

  private def buildDiffTransactor(oldDbPath: String, newDbPath: String): Transactor[IO] = {
    val connect = (_: Unit) => cats.effect.Resource.make(
      IO {
        val conn = DriverManager.getConnection("jdbc:sqlite::memory:")
        val stmt = conn.createStatement()
        stmt.execute(s"ATTACH DATABASE '$oldDbPath' AS old_rev")
        stmt.execute(s"ATTACH DATABASE '$newDbPath' AS new_rev")
        stmt.close()
        conn
      }
    )(conn => IO(conn.close()))

    Transactor(
      (),
      connect,
      doobie.free.KleisliInterpreter[IO](doobie.util.log.LogHandler.noop).ConnectionInterpreter,
      doobie.util.transactor.Strategy.default
    )
  }

  private val pointChanges: ConnectionIO[List[PointChange]] =
    sql"""
      SELECT
        COALESCE(n.datasheet_id, o.datasheet_id),
        COALESCE(nd.name, od.name, ''),
        COALESCE(n.line, o.line),
        COALESCE(n.description, o.description, ''),
        o.cost,
        n.cost
      FROM new_rev.unit_cost n
      FULL OUTER JOIN old_rev.unit_cost o
        ON n.datasheet_id = o.datasheet_id AND n.line = o.line
      LEFT JOIN new_rev.datasheets nd ON nd.id = n.datasheet_id
      LEFT JOIN old_rev.datasheets od ON od.id = o.datasheet_id
      WHERE o.cost IS NULL OR n.cost IS NULL OR n.cost != o.cost
      ORDER BY COALESCE(nd.name, od.name)
    """.query[PointChange].to[List]

  private val unitChanges: ConnectionIO[List[UnitChange]] = {
    val added =
      sql"""
        SELECT n.id, n.name, COALESCE(n.faction_id, ''), 'added'
        FROM new_rev.datasheets n
        LEFT JOIN old_rev.datasheets o ON n.id = o.id
        WHERE o.id IS NULL AND n.virtual = 0
      """.query[UnitChange].to[List]

    val removed =
      sql"""
        SELECT o.id, o.name, COALESCE(o.faction_id, ''), 'removed'
        FROM old_rev.datasheets o
        LEFT JOIN new_rev.datasheets n ON o.id = n.id
        WHERE n.id IS NULL AND o.virtual = 0
      """.query[UnitChange].to[List]

    (added, removed).mapN(_ ++ _)
  }

  private val statChanges: ConnectionIO[List[StatChange]] =
    sql"""
      SELECT n.datasheet_id, d.name,
        'movement', o.movement, n.movement
      FROM new_rev.model_profiles n
      JOIN old_rev.model_profiles o ON n.datasheet_id = o.datasheet_id AND n.line = o.line
      JOIN new_rev.datasheets d ON d.id = n.datasheet_id
      WHERE n.movement != o.movement
      UNION ALL
      SELECT n.datasheet_id, d.name,
        'toughness', o.toughness, n.toughness
      FROM new_rev.model_profiles n
      JOIN old_rev.model_profiles o ON n.datasheet_id = o.datasheet_id AND n.line = o.line
      JOIN new_rev.datasheets d ON d.id = n.datasheet_id
      WHERE n.toughness != o.toughness
      UNION ALL
      SELECT n.datasheet_id, d.name,
        'wounds', CAST(o.wounds AS TEXT), CAST(n.wounds AS TEXT)
      FROM new_rev.model_profiles n
      JOIN old_rev.model_profiles o ON n.datasheet_id = o.datasheet_id AND n.line = o.line
      JOIN new_rev.datasheets d ON d.id = n.datasheet_id
      WHERE n.wounds != o.wounds
      UNION ALL
      SELECT n.datasheet_id, d.name,
        'save', CAST(o.save AS TEXT), CAST(n.save AS TEXT)
      FROM new_rev.model_profiles n
      JOIN old_rev.model_profiles o ON n.datasheet_id = o.datasheet_id AND n.line = o.line
      JOIN new_rev.datasheets d ON d.id = n.datasheet_id
      WHERE n.save != o.save
      UNION ALL
      SELECT n.datasheet_id, d.name,
        'leadership', o.leadership, n.leadership
      FROM new_rev.model_profiles n
      JOIN old_rev.model_profiles o ON n.datasheet_id = o.datasheet_id AND n.line = o.line
      JOIN new_rev.datasheets d ON d.id = n.datasheet_id
      WHERE n.leadership != o.leadership
      UNION ALL
      SELECT n.datasheet_id, d.name,
        'objective_control', CAST(o.objective_control AS TEXT), CAST(n.objective_control AS TEXT)
      FROM new_rev.model_profiles n
      JOIN old_rev.model_profiles o ON n.datasheet_id = o.datasheet_id AND n.line = o.line
      JOIN new_rev.datasheets d ON d.id = n.datasheet_id
      WHERE n.objective_control != o.objective_control
      ORDER BY 2, 1
    """.query[StatChange].to[List]

  private val enhancementChanges: ConnectionIO[List[EnhancementChange]] = {
    val modified =
      sql"""
        SELECT n.id, n.name, n.faction_id, o.cost, n.cost, 'modified', o.description, n.description
        FROM new_rev.enhancements n
        JOIN old_rev.enhancements o ON n.id = o.id
        WHERE n.cost != o.cost OR n.name != o.name OR n.description != o.description
      """.query[EnhancementChange].to[List]

    val added =
      sql"""
        SELECT n.id, n.name, n.faction_id, NULL, n.cost, 'added', NULL, n.description
        FROM new_rev.enhancements n
        LEFT JOIN old_rev.enhancements o ON n.id = o.id
        WHERE o.id IS NULL
      """.query[EnhancementChange].to[List]

    val removed =
      sql"""
        SELECT o.id, o.name, o.faction_id, o.cost, NULL, 'removed', o.description, NULL
        FROM old_rev.enhancements o
        LEFT JOIN new_rev.enhancements n ON o.id = n.id
        WHERE n.id IS NULL
      """.query[EnhancementChange].to[List]

    (modified, added, removed).mapN(_ ++ _ ++ _)
  }

  private val stratagemChanges: ConnectionIO[List[StratagemChange]] = {
    val modified =
      sql"""
        SELECT n.id, n.name, COALESCE(n.faction_id, ''), 'modified', o.cp_cost, n.cp_cost, o.description, n.description
        FROM new_rev.stratagems n
        JOIN old_rev.stratagems o ON n.id = o.id
        WHERE n.cp_cost != o.cp_cost OR n.name != o.name OR n.description != o.description
      """.query[StratagemChange].to[List]

    val added =
      sql"""
        SELECT n.id, n.name, COALESCE(n.faction_id, ''), 'added', NULL, n.cp_cost, NULL, n.description
        FROM new_rev.stratagems n
        LEFT JOIN old_rev.stratagems o ON n.id = o.id
        WHERE o.id IS NULL
      """.query[StratagemChange].to[List]

    val removed =
      sql"""
        SELECT o.id, o.name, COALESCE(o.faction_id, ''), 'removed', o.cp_cost, NULL, o.description, NULL
        FROM old_rev.stratagems o
        LEFT JOIN new_rev.stratagems n ON o.id = n.id
        WHERE n.id IS NULL
      """.query[StratagemChange].to[List]

    (modified, added, removed).mapN(_ ++ _ ++ _)
  }

  private val abilityChanges: ConnectionIO[List[AbilityChange]] = {
    val modified =
      sql"""
        SELECT n.id, n.name, COALESCE(n.faction_id, ''), 'modified', o.description, n.description
        FROM new_rev.abilities n
        JOIN old_rev.abilities o ON n.id = o.id
        WHERE n.description != o.description OR n.name != o.name
      """.query[AbilityChange].to[List]

    val added =
      sql"""
        SELECT n.id, n.name, COALESCE(n.faction_id, ''), 'added', NULL, n.description
        FROM new_rev.abilities n
        LEFT JOIN old_rev.abilities o ON n.id = o.id
        WHERE o.id IS NULL
      """.query[AbilityChange].to[List]

    val removed =
      sql"""
        SELECT o.id, o.name, COALESCE(o.faction_id, ''), 'removed', o.description, NULL
        FROM old_rev.abilities o
        LEFT JOIN new_rev.abilities n ON o.id = n.id
        WHERE n.id IS NULL
      """.query[AbilityChange].to[List]

    (modified, added, removed).mapN(_ ++ _ ++ _)
  }
}
