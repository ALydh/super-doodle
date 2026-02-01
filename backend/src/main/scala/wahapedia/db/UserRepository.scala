package wahapedia.db

import doobie.*
import doobie.implicits.*
import cats.effect.IO
import java.time.Instant
import java.sql.SQLException
import wahapedia.domain.types.UserId
import wahapedia.domain.auth.User
import DoobieMeta.given

object UserRepository {

  def create(username: String, passwordHash: String)(xa: Transactor[IO]): IO[Option[User]] = {
    val id = UserId.generate()
    val now = Instant.now()
    sql"""INSERT INTO users (id, username, password_hash, created_at)
          VALUES ($id, $username, $passwordHash, ${now.toString})""".update.run
      .transact(xa)
      .as(Some(User(id, username, passwordHash, now)))
      .recover {
        case _: SQLException => None
      }
  }

  def findByUsername(username: String)(xa: Transactor[IO]): IO[Option[User]] =
    sql"SELECT id, username, password_hash, created_at FROM users WHERE username = $username"
      .query[(UserId, String, String, Instant)]
      .option
      .transact(xa)
      .map(_.map { case (id, uname, hash, createdAt) =>
        User(id, uname, hash, createdAt)
      })

  def findById(id: UserId)(xa: Transactor[IO]): IO[Option[User]] =
    sql"SELECT id, username, password_hash, created_at FROM users WHERE id = $id"
      .query[(UserId, String, String, Instant)]
      .option
      .transact(xa)
      .map(_.map { case (uid, uname, hash, createdAt) =>
        User(uid, uname, hash, createdAt)
      })

  def count(xa: Transactor[IO]): IO[Int] =
    sql"SELECT COUNT(*) FROM users".query[Int].unique.transact(xa)
}
