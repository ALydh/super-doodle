package wahapedia.db

import doobie.*
import doobie.implicits.*
import cats.effect.IO
import cats.implicits.*
import java.util.UUID
import java.time.Instant
import wahapedia.domain.army.{Army, ArmyUnit}
import wahapedia.domain.types.*
import wahapedia.domain.models.EnhancementId
import DoobieMeta.given

case class PersistedArmy(
  id: UUID,
  name: String,
  army: Army,
  createdAt: String,
  updatedAt: String
)

object ArmyRepository {

  def create(name: String, army: Army)(xa: Transactor[IO]): IO[PersistedArmy] = {
    val id = UUID.randomUUID()
    val now = Instant.now().toString
    val insert = for {
      _ <- sql"""INSERT INTO armies (id, name, faction_id, battle_size, detachment_id, warlord_id, created_at, updated_at)
                 VALUES (${id.toString}, $name, ${army.factionId}, ${army.battleSize.toString}, ${army.detachmentId}, ${army.warlordId}, $now, $now)""".update.run
      _ <- army.units.traverse_ { unit =>
        sql"""INSERT INTO army_units (army_id, datasheet_id, size_option_line, enhancement_id, attached_leader_id)
              VALUES (${id.toString}, ${unit.datasheetId}, ${unit.sizeOptionLine}, ${unit.enhancementId}, ${unit.attachedLeaderId})""".update.run
      }
    } yield ()
    insert.transact(xa).as(PersistedArmy(id, name, army, now, now))
  }

  def findById(id: UUID)(xa: Transactor[IO]): IO[Option[PersistedArmy]] =
    (for {
      armyRow <- sql"""SELECT id, name, faction_id, battle_size, detachment_id, warlord_id, created_at, updated_at
                       FROM armies WHERE id = ${id.toString}"""
        .query[(String, String, FactionId, String, DetachmentId, DatasheetId, String, String)]
        .option
      result <- armyRow.traverse { case (_, name, factionId, battleSizeStr, detachmentId, warlordId, createdAt, updatedAt) =>
        val battleSize = parseBattleSize(battleSizeStr)
        sql"""SELECT datasheet_id, size_option_line, enhancement_id, attached_leader_id
              FROM army_units WHERE army_id = ${id.toString}"""
          .query[ArmyUnit].to[List].map { units =>
            PersistedArmy(id, name, Army(factionId, battleSize, detachmentId, warlordId, units), createdAt, updatedAt)
          }
      }
    } yield result).transact(xa)

  def listByFaction(factionId: FactionId)(xa: Transactor[IO]): IO[List[PersistedArmy]] =
    (for {
      rows <- sql"""SELECT id, name, faction_id, battle_size, detachment_id, warlord_id, created_at, updated_at
                    FROM armies WHERE faction_id = $factionId"""
        .query[(String, String, FactionId, String, DetachmentId, DatasheetId, String, String)]
        .to[List]
      armies <- rows.traverse { case (idStr, name, fId, battleSizeStr, detachmentId, warlordId, createdAt, updatedAt) =>
        val id = UUID.fromString(idStr)
        val battleSize = parseBattleSize(battleSizeStr)
        sql"""SELECT datasheet_id, size_option_line, enhancement_id, attached_leader_id
              FROM army_units WHERE army_id = $idStr"""
          .query[ArmyUnit].to[List].map { units =>
            PersistedArmy(id, name, Army(fId, battleSize, detachmentId, warlordId, units), createdAt, updatedAt)
          }
      }
    } yield armies).transact(xa)

  def update(id: UUID, name: String, army: Army)(xa: Transactor[IO]): IO[Option[PersistedArmy]] = {
    val now = Instant.now().toString
    val op = for {
      existing <- sql"SELECT id FROM armies WHERE id = ${id.toString}".query[String].option
      result <- existing.traverse { _ =>
        for {
          _ <- sql"DELETE FROM army_units WHERE army_id = ${id.toString}".update.run
          _ <- sql"""UPDATE armies SET name = $name, faction_id = ${army.factionId},
                     battle_size = ${army.battleSize.toString}, detachment_id = ${army.detachmentId},
                     warlord_id = ${army.warlordId}, updated_at = $now
                     WHERE id = ${id.toString}""".update.run
          _ <- army.units.traverse_ { unit =>
            sql"""INSERT INTO army_units (army_id, datasheet_id, size_option_line, enhancement_id, attached_leader_id)
                  VALUES (${id.toString}, ${unit.datasheetId}, ${unit.sizeOptionLine}, ${unit.enhancementId}, ${unit.attachedLeaderId})""".update.run
          }
          createdAt <- sql"SELECT created_at FROM armies WHERE id = ${id.toString}".query[String].unique
        } yield PersistedArmy(id, name, army, createdAt, now)
      }
    } yield result
    op.transact(xa)
  }

  def delete(id: UUID)(xa: Transactor[IO]): IO[Boolean] =
    sql"DELETE FROM armies WHERE id = ${id.toString}".update.run.transact(xa).map(_ > 0)

  private def parseBattleSize(s: String): BattleSize = s match {
    case "Incursion" => BattleSize.Incursion
    case "StrikeForce" => BattleSize.StrikeForce
    case "Onslaught" => BattleSize.Onslaught
    case other => throw new IllegalArgumentException(s"Invalid battle size: $other")
  }
}
