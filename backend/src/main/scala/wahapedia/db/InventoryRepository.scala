package wahapedia.db

import doobie.*
import doobie.implicits.*
import cats.effect.IO
import cats.implicits.*
import wahapedia.domain.types.*
import DoobieMeta.given

case class InventoryEntry(
  userId: String,
  datasheetId: String,
  quantity: Int
)

object InventoryRepository {

  def getByUser(userId: UserId)(xa: Transactor[IO]): IO[List[InventoryEntry]] =
    sql"SELECT user_id, datasheet_id, quantity FROM user_inventory WHERE user_id = $userId"
      .query[InventoryEntry].to[List].transact(xa)

  def upsert(userId: UserId, datasheetId: DatasheetId, quantity: Int)(xa: Transactor[IO]): IO[InventoryEntry] =
    (if (quantity <= 0)
      sql"DELETE FROM user_inventory WHERE user_id = $userId AND datasheet_id = $datasheetId".update.run
        .as(InventoryEntry(UserId.value(userId), DatasheetId.value(datasheetId), 0))
    else
      sql"""INSERT INTO user_inventory (user_id, datasheet_id, quantity)
            VALUES ($userId, $datasheetId, $quantity)
            ON CONFLICT(user_id, datasheet_id) DO UPDATE SET quantity = $quantity"""
        .update.run
        .as(InventoryEntry(UserId.value(userId), DatasheetId.value(datasheetId), quantity))
    ).transact(xa)

  def deleteEntry(userId: UserId, datasheetId: DatasheetId)(xa: Transactor[IO]): IO[Boolean] =
    sql"DELETE FROM user_inventory WHERE user_id = $userId AND datasheet_id = $datasheetId"
      .update.run.transact(xa).map(_ > 0)

  def bulkUpsert(userId: UserId, entries: List[(DatasheetId, Int)])(xa: Transactor[IO]): IO[List[InventoryEntry]] =
    entries.traverse { case (datasheetId, quantity) =>
      if (quantity <= 0)
        sql"DELETE FROM user_inventory WHERE user_id = $userId AND datasheet_id = $datasheetId".update.run
          .as(InventoryEntry(UserId.value(userId), DatasheetId.value(datasheetId), 0))
      else
        sql"""INSERT INTO user_inventory (user_id, datasheet_id, quantity)
              VALUES ($userId, $datasheetId, $quantity)
              ON CONFLICT(user_id, datasheet_id) DO UPDATE SET quantity = $quantity"""
          .update.run
          .as(InventoryEntry(UserId.value(userId), DatasheetId.value(datasheetId), quantity))
    }.transact(xa).map(_.filter(_.quantity > 0))
}
