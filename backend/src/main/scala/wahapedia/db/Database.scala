package wahapedia.db

import cats.effect.IO
import doobie.*
import doobie.implicits.*

case class DatabaseConfig(
  refDbPath: String,
  userDbPath: String
)

object DatabaseConfig {
  val default: DatabaseConfig = DatabaseConfig(
    refDbPath = "wahapedia-ref.db",
    userDbPath = "wahapedia-user.db"
  )

  def fromEnv: IO[DatabaseConfig] = IO {
    DatabaseConfig(
      refDbPath = sys.env.getOrElse("REF_DB_PATH", default.refDbPath),
      userDbPath = sys.env.getOrElse("USER_DB_PATH", default.userDbPath)
    )
  }
}

case class Databases(refXa: Transactor[IO], userXa: Transactor[IO])

object Database {

  def transactors(config: DatabaseConfig): Databases = {
    val refXa = Transactor.fromDriverManager[IO](
      driver = "org.sqlite.JDBC",
      url = s"jdbc:sqlite:file:${config.refDbPath}?mode=ro",
      logHandler = None
    )

    val userXa = Transactor.fromDriverManager[IO](
      driver = "org.sqlite.JDBC",
      url = s"jdbc:sqlite:file:${config.userDbPath}?foreign_keys=on",
      logHandler = None
    )

    Databases(refXa, userXa)
  }

  def attachRefDb(userXa: Transactor[IO], refDbPath: String): IO[Unit] =
    sql"ATTACH DATABASE $refDbPath AS ref".update.run.transact(userXa).void

  def singleTransactor(path: String): Transactor[IO] =
    Transactor.fromDriverManager[IO](
      driver = "org.sqlite.JDBC",
      url = s"jdbc:sqlite:file:$path?foreign_keys=on",
      logHandler = None
    )
}
