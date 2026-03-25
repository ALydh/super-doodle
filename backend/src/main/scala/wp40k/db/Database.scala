package wp40k.db

import cats.effect.{IO, Ref, Resource}
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.util.log.LogHandler
import java.sql.DriverManager

case class DatabaseConfig(
  refDbPath: String,
  userDbPath: String,
  revisionsDir: String
)

object DatabaseConfig {
  val default: DatabaseConfig = DatabaseConfig(
    refDbPath = "wp40k-ref.db",
    userDbPath = "wp40k-user.db",
    revisionsDir = "../data/revisions"
  )

  def fromEnv: IO[DatabaseConfig] = IO {
    DatabaseConfig(
      refDbPath = sys.env.getOrElse("REF_DB_PATH", default.refDbPath),
      userDbPath = sys.env.getOrElse("USER_DB_PATH", default.userDbPath),
      revisionsDir = sys.env.getOrElse("REVISIONS_DIR", default.revisionsDir)
    )
  }
}

case class RevisionState(revisionId: String, refDbPath: String)

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

  def plainUserTransactor(userDbPath: String): Transactor[IO] = {
    val base = Transactor.fromDriverManager[IO](
      driver = "org.sqlite.JDBC",
      url = s"jdbc:sqlite:file:$userDbPath?foreign_keys=on",
      logHandler = None
    )
    Transactor.before.modify(base, _ *> readWritePragmas)
  }

  def refTransactor(refDbPath: String): Transactor[IO] = {
    val base = Transactor.fromDriverManager[IO](
      driver = "org.sqlite.JDBC",
      url = s"jdbc:sqlite:file:$refDbPath?mode=ro",
      logHandler = None
    )
    Transactor.before.modify(base, _ *> readOnlyPragmas)
  }

  def dynamicSplitTransactors(userDbPath: String, activeRef: Ref[IO, RevisionState]): Databases = {
    val refConnect = (_: Unit) => Resource.make(
      activeRef.get.flatMap { state =>
        IO {
          Class.forName("org.sqlite.JDBC")
          DriverManager.getConnection(s"jdbc:sqlite:file:${state.refDbPath}?mode=ro")
        }
      }
    )(conn => IO(conn.close()))

    val baseRefXa = Transactor(
      (),
      refConnect,
      doobie.free.KleisliInterpreter[IO](LogHandler.noop).ConnectionInterpreter,
      doobie.util.transactor.Strategy.default
    )
    val refXa = Transactor.before.modify(baseRefXa, _ *> readOnlyPragmas)

    val userConnect = (_: Unit) => Resource.make(
      activeRef.get.flatMap { state =>
        IO {
          val conn = DriverManager.getConnection(s"jdbc:sqlite:file:$userDbPath?foreign_keys=on")
          val stmt = conn.createStatement()
          stmt.execute(s"ATTACH DATABASE '${state.refDbPath}' AS ref")
          stmt.close()
          conn
        }
      }
    )(conn => IO(conn.close()))

    val baseUserXa = Transactor(
      (),
      userConnect,
      doobie.free.KleisliInterpreter[IO](LogHandler.noop).ConnectionInterpreter,
      doobie.util.transactor.Strategy.default
    )
    val userXa = Transactor.before.modify(baseUserXa, _ *> readWritePragmas)

    Databases(refXa, userXa)
  }
}
