package wahapedia.db

import doobie.*
import doobie.implicits.*
import cats.effect.IO
import java.time.Instant
import wahapedia.domain.types.{UserId, InviteCode}
import wahapedia.domain.auth.Invite
import DoobieMeta.given

object InviteRepository {

  def create(createdBy: Option[UserId])(xa: Transactor[IO]): IO[Invite] = {
    val code = InviteCode.generate()
    val now = Instant.now()
    sql"""INSERT INTO invites (code, created_by, created_at, used_by, used_at)
          VALUES ($code, $createdBy, ${now.toString}, ${None: Option[String]}, ${None: Option[String]})""".update.run
      .transact(xa)
      .as(Invite(code, createdBy, now, None, None))
  }

  def findUnusedByCode(code: InviteCode)(xa: Transactor[IO]): IO[Option[Invite]] =
    sql"SELECT code, created_by, created_at, used_by, used_at FROM invites WHERE code = $code AND used_by IS NULL"
      .query[(InviteCode, Option[UserId], String, Option[UserId], Option[String])]
      .option
      .transact(xa)
      .map(_.map { case (c, createdBy, createdAt, usedBy, usedAt) =>
        Invite(c, createdBy, Instant.parse(createdAt), usedBy, usedAt.map(Instant.parse))
      })

  def markUsed(code: InviteCode, userId: UserId)(xa: Transactor[IO]): IO[Unit] = {
    val now = Instant.now().toString
    sql"UPDATE invites SET used_by = $userId, used_at = $now WHERE code = $code".update.run
      .transact(xa)
      .void
  }

  def listAll(xa: Transactor[IO]): IO[List[Invite]] =
    sql"SELECT code, created_by, created_at, used_by, used_at FROM invites ORDER BY created_at DESC"
      .query[(InviteCode, Option[UserId], String, Option[UserId], Option[String])]
      .to[List]
      .transact(xa)
      .map(_.map { case (c, createdBy, createdAt, usedBy, usedAt) =>
        Invite(c, createdBy, Instant.parse(createdAt), usedBy, usedAt.map(Instant.parse))
      })
}
