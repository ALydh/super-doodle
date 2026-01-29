package wahapedia.db

import doobie.*
import doobie.implicits.*
import cats.effect.IO
import java.time.Instant
import java.time.temporal.ChronoUnit
import wahapedia.domain.types.{UserId, SessionToken}
import wahapedia.domain.auth.Session
import DoobieMeta.given

object SessionRepository {

  def create(userId: UserId)(xa: Transactor[IO]): IO[Session] = {
    val token = SessionToken.generate()
    val now = Instant.now()
    val expiresAt = now.plus(7, ChronoUnit.DAYS)
    sql"""INSERT INTO sessions (token, user_id, created_at, expires_at)
          VALUES ($token, $userId, ${now.toString}, ${expiresAt.toString})""".update.run
      .transact(xa)
      .as(Session(token, userId, now, expiresAt))
  }

  def findByToken(token: SessionToken)(xa: Transactor[IO]): IO[Option[Session]] =
    sql"SELECT token, user_id, created_at, expires_at FROM sessions WHERE token = $token"
      .query[(SessionToken, UserId, String, String)]
      .option
      .transact(xa)
      .map(_.map { case (t, uid, createdAt, expiresAt) =>
        Session(t, uid, Instant.parse(createdAt), Instant.parse(expiresAt))
      })

  def delete(token: SessionToken)(xa: Transactor[IO]): IO[Unit] =
    sql"DELETE FROM sessions WHERE token = $token".update.run.transact(xa).void

  def deleteExpired(xa: Transactor[IO]): IO[Int] = {
    val now = Instant.now().toString
    sql"DELETE FROM sessions WHERE expires_at < $now".update.run.transact(xa)
  }
}
