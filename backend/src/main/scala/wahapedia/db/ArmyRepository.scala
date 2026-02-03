package wahapedia.db

import doobie.*
import doobie.implicits.*
import cats.effect.IO
import cats.implicits.*
import java.time.{Instant, LocalDateTime}
import java.time.format.DateTimeFormatter
import wahapedia.domain.army.{Army, ArmyUnit, WargearSelection}
import wahapedia.domain.types.*
import wahapedia.domain.models.EnhancementId
import DoobieMeta.given

case class PersistedArmy(
  id: String,
  name: String,
  army: Army,
  ownerId: Option[String],
  createdAt: String,
  updatedAt: String
)

case class ArmySummary(
  id: String,
  name: String,
  factionId: String,
  battleSize: String,
  updatedAt: String,
  warlordName: Option[String],
  totalPoints: Int
)

private case class ArmyRow(
  id: String,
  name: String,
  factionId: FactionId,
  battleSize: BattleSize,
  detachmentId: DetachmentId,
  warlordId: DatasheetId,
  ownerId: Option[UserId],
  createdAt: String,
  updatedAt: String
)

private case class ArmyUnitRow(
  id: Long,
  datasheetId: DatasheetId,
  sizeOptionLine: Int,
  enhancementId: Option[EnhancementId],
  attachedLeaderId: Option[DatasheetId]
)

private case class WargearSelectionRow(
  armyUnitId: Long,
  optionLine: Int,
  selected: Boolean,
  notes: Option[String]
)

object ArmyRepository {

  private val logTimeFmt = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
  private def now: String = LocalDateTime.now.format(logTimeFmt)

  def findById(id: String)(xa: Transactor[IO]): IO[Option[PersistedArmy]] =
    for {
      _ <- IO.println(s"[$now] [ArmyRepository.findById] Loading army $id")
      rowOpt <- sql"SELECT id, name, faction_id, battle_size, detachment_id, warlord_id, owner_id, created_at, updated_at FROM armies WHERE id = $id"
        .query[ArmyRow].option.transact(xa)
      result <- rowOpt match {
        case None =>
          IO.println(s"[$now] [ArmyRepository.findById] Army not found").as(None)
        case Some(row) =>
          (for {
            unitRows <- sql"SELECT id, datasheet_id, size_option_line, enhancement_id, attached_leader_id FROM army_units WHERE army_id = $id"
              .query[ArmyUnitRow].to[List]
            selectionRows <- sql"""SELECT army_unit_id, option_line, selected, notes FROM army_unit_wargear_selections
                                   WHERE army_unit_id IN (SELECT id FROM army_units WHERE army_id = $id)"""
              .query[WargearSelectionRow].to[List]
          } yield (unitRows, selectionRows)).transact(xa).flatMap { case (unitRows, selectionRows) =>
            val selectionsByUnit = selectionRows.groupBy(_.armyUnitId)
            val units = unitRows.map { u =>
              val wargear = selectionsByUnit.getOrElse(u.id, List.empty).map { s =>
                WargearSelection(s.optionLine, s.selected, s.notes)
              }
              ArmyUnit(u.datasheetId, u.sizeOptionLine, u.enhancementId, u.attachedLeaderId, wargear)
            }
            val army = Army(row.factionId, row.battleSize, row.detachmentId, row.warlordId, units)
            val persisted = PersistedArmy(row.id, row.name, army, row.ownerId.map(UserId.value), row.createdAt, row.updatedAt)
            IO.println(s"[$now] [ArmyRepository.findById] Found army: ${row.name} with ${units.size} units").as(Some(persisted))
          }
      }
    } yield result

  def create(id: String, name: String, army: Army, ownerId: Option[UserId])(xa: Transactor[IO]): IO[PersistedArmy] = {
    val now = Instant.now().toString
    for {
      _ <- IO.println(s"[$now] [ArmyRepository.create] Creating army $name with ${army.units.size} units")
      _ <- (for {
        _ <- sql"""INSERT INTO armies (id, name, faction_id, battle_size, detachment_id, warlord_id, owner_id, created_at, updated_at)
                   VALUES ($id, $name, ${army.factionId}, ${army.battleSize}, ${army.detachmentId}, ${army.warlordId}, $ownerId, $now, $now)""".update.run
        _ <- army.units.traverse_ { unit =>
          for {
            _ <- sql"""INSERT INTO army_units (army_id, datasheet_id, size_option_line, enhancement_id, attached_leader_id)
                  VALUES ($id, ${unit.datasheetId}, ${unit.sizeOptionLine}, ${unit.enhancementId}, ${unit.attachedLeaderId})""".update.run
            unitId <- sql"SELECT last_insert_rowid()".query[Long].unique
            _ <- unit.wargearSelections.traverse_ { sel =>
              sql"""INSERT INTO army_unit_wargear_selections (army_unit_id, option_line, selected, notes)
                    VALUES ($unitId, ${sel.optionLine}, ${if (sel.selected) 1 else 0}, ${sel.notes})""".update.run
            }
          } yield ()
        }
      } yield ()).transact(xa)
      _ <- IO.println(s"[$now] [ArmyRepository.create] Army created successfully")
    } yield PersistedArmy(id, name, army, ownerId.map(UserId.value), now, now)
  }

  def update(id: String, name: String, army: Army)(xa: Transactor[IO]): IO[Option[PersistedArmy]] = {
    val now = Instant.now().toString
    for {
      _ <- IO.println(s"[$now] [ArmyRepository.update] Updating army $id")
      result <- (for {
        existing <- sql"SELECT created_at, owner_id FROM armies WHERE id = $id".query[(String, Option[UserId])].option
        updated <- existing.traverse { case (createdAt, ownerId) =>
          for {
            _ <- sql"DELETE FROM army_units WHERE army_id = $id".update.run
            _ <- sql"""UPDATE armies SET name = $name, faction_id = ${army.factionId}, battle_size = ${army.battleSize},
                       detachment_id = ${army.detachmentId}, warlord_id = ${army.warlordId}, updated_at = $now
                       WHERE id = $id""".update.run
            _ <- army.units.traverse_ { unit =>
              for {
                _ <- sql"""INSERT INTO army_units (army_id, datasheet_id, size_option_line, enhancement_id, attached_leader_id)
                      VALUES ($id, ${unit.datasheetId}, ${unit.sizeOptionLine}, ${unit.enhancementId}, ${unit.attachedLeaderId})""".update.run
                unitId <- sql"SELECT last_insert_rowid()".query[Long].unique
                _ <- unit.wargearSelections.traverse_ { sel =>
                  sql"""INSERT INTO army_unit_wargear_selections (army_unit_id, option_line, selected, notes)
                        VALUES ($unitId, ${sel.optionLine}, ${if (sel.selected) 1 else 0}, ${sel.notes})""".update.run
                }
              } yield ()
            }
          } yield PersistedArmy(id, name, army, ownerId.map(UserId.value), createdAt, now)
        }
      } yield updated).transact(xa)
      _ <- IO.println(s"[$now] [ArmyRepository.update] Update ${if (result.isDefined) "successful" else "failed - not found"}")
    } yield result
  }

  def delete(id: String)(xa: Transactor[IO]): IO[Boolean] =
    for {
      _ <- IO.println(s"[$now] [ArmyRepository.delete] Deleting army $id")
      deleted <- sql"DELETE FROM armies WHERE id = $id".update.run.transact(xa).map(_ > 0)
      _ <- IO.println(s"[$now] [ArmyRepository.delete] Delete ${if (deleted) "successful" else "failed - not found"}")
    } yield deleted

  def listSummaries(xa: Transactor[IO], refPrefix: String = ""): IO[List[ArmySummary]] =
    for {
      _ <- IO.println("[ArmyRepository.listSummaries] Fetching all armies")
      summaries <- listSummariesQuery(None, refPrefix).query[ArmySummary].to[List].transact(xa)
      _ <- IO.println(s"[$now] [ArmyRepository.listSummaries] Found ${summaries.size} armies")
      _ <- summaries.traverse_(a => IO.println(s"  - ${a.name}: warlord=${a.warlordName.getOrElse("none")}, points=${a.totalPoints}"))
    } yield summaries

  def listSummariesByFaction(factionId: FactionId)(xa: Transactor[IO], refPrefix: String = ""): IO[List[ArmySummary]] =
    for {
      _ <- IO.println(s"[$now] [ArmyRepository.listSummariesByFaction] Fetching armies for faction $factionId")
      summaries <- listSummariesQuery(Some(factionId), refPrefix).query[ArmySummary].to[List].transact(xa)
      _ <- IO.println(s"[$now] [ArmyRepository.listSummariesByFaction] Found ${summaries.size} armies")
      _ <- summaries.traverse_(a => IO.println(s"  - ${a.name}: warlord=${a.warlordName.getOrElse("none")}, points=${a.totalPoints}"))
    } yield summaries

  private def listSummariesQuery(factionFilter: Option[FactionId], refPrefix: String): Fragment = {
    val datasheets = Fragment.const(s"${refPrefix}datasheets")
    val unitCost = Fragment.const(s"${refPrefix}unit_cost")
    val enhancements = Fragment.const(s"${refPrefix}enhancements")

    val baseQuery = fr"""SELECT
          a.id, a.name, a.faction_id, a.battle_size, a.updated_at,
          d.name,
          COALESCE((SELECT SUM(uc.cost) FROM army_units au JOIN """ ++ unitCost ++ fr""" uc ON au.datasheet_id = uc.datasheet_id AND au.size_option_line = uc.line WHERE au.army_id = a.id), 0)
          + COALESCE((SELECT SUM(e.cost) FROM army_units au JOIN """ ++ enhancements ++ fr""" e ON au.enhancement_id = e.id WHERE au.army_id = a.id), 0)
        FROM armies a
        LEFT JOIN """ ++ datasheets ++ fr""" d ON a.warlord_id = d.id"""

    val withFilter = factionFilter match {
      case Some(fid) => baseQuery ++ fr" WHERE a.faction_id = $fid"
      case None => baseQuery
    }

    withFilter ++ fr" ORDER BY a.updated_at DESC"
  }
}
