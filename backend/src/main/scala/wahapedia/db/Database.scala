package wahapedia.db

import cats.effect.IO
import cats.implicits.*
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

  private def execPragma(frag: Fragment): ConnectionIO[Unit] =
    frag.execWith(FPS.execute.void)

  private val readOnlyPragmas: ConnectionIO[Unit] =
    FC.setAutoCommit(true) *>
      List(
        sql"PRAGMA busy_timeout=5000",
        sql"PRAGMA cache_size=-20000",
        sql"PRAGMA mmap_size=268435456",
        sql"PRAGMA temp_store=MEMORY"
      ).traverse_(execPragma) *>
      FC.setAutoCommit(false)

  private val readWritePragmas: ConnectionIO[Unit] =
    FC.setAutoCommit(true) *>
      execPragma(sql"PRAGMA journal_mode=WAL") *>
      execPragma(sql"PRAGMA synchronous=NORMAL") *>
      FC.setAutoCommit(false) *>
      readOnlyPragmas

  def transactors(config: DatabaseConfig): Databases = {
    val baseRefXa = Transactor.fromDriverManager[IO](
      driver = "org.sqlite.JDBC",
      url = s"jdbc:sqlite:file:${config.refDbPath}?mode=ro",
      logHandler = None
    )
    val refXa = Transactor.before.modify(baseRefXa, _ *> readOnlyPragmas)

    val baseUserXa = Transactor.fromDriverManager[IO](
      driver = "org.sqlite.JDBC",
      url = s"jdbc:sqlite:file:${config.userDbPath}?foreign_keys=on",
      logHandler = None
    )

    val attachRefDb: ConnectionIO[Unit] =
      execPragma(sql"ATTACH DATABASE ${config.refDbPath} AS ref")

    val userXa = Transactor.before.modify(baseUserXa, _ *> readWritePragmas *> attachRefDb)

    Databases(refXa, userXa)
  }

  def singleTransactor(path: String): Transactor[IO] = {
    val base = Transactor.fromDriverManager[IO](
      driver = "org.sqlite.JDBC",
      url = s"jdbc:sqlite:file:$path?foreign_keys=on",
      logHandler = None
    )
    Transactor.before.modify(base, _ *> readWritePragmas)
  }
}
