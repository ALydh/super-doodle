package wahapedia

import cats.effect.{IO, IOApp}
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import wahapedia.db.{Schema, DataLoader, ReferenceDataRepository, Database, DatabaseConfig}
import wahapedia.http.HttpServer
import wahapedia.errors.ParseException

object Main extends IOApp.Simple {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def run: IO[Unit] = {
    val program = for {
      _ <- logger.info("Starting wahapedia-api")
      config <- DatabaseConfig.fromEnv
      splitMode = config.refDbPath != DatabaseConfig.default.refDbPath ||
                  config.userDbPath != DatabaseConfig.default.userDbPath
      _ <- if (splitMode) runSplitMode(config) else runSingleMode
    } yield ()

    program.handleErrorWith {
      case pe: ParseException =>
        logger.error(s"Parse error: ${pe.getMessage}")
      case e =>
        logger.error(e)(s"Unexpected error: ${e.getMessage}") *>
          IO.raiseError(e)
    }
  }

  private def runSingleMode: IO[Unit] = {
    val xa = Database.singleTransactor("wahapedia.db")
    for {
      _ <- logger.info("Running in single database mode")
      _ <- logger.info("Initializing database")
      _ <- Schema.initialize(xa)
      tableCounts <- ReferenceDataRepository.counts(xa)
      _ <- DataLoader.loadMissing(xa, tableCounts)
      _ <- printSummary(tableCounts, xa)
      _ <- logger.info("Starting HTTP server on port 8080")
      _ <- HttpServer.createServer(8080, xa, xa, "").useForever
    } yield ()
  }

  private def runSplitMode(config: DatabaseConfig): IO[Unit] = {
    val dbs = Database.transactors(config)
    for {
      _ <- logger.info("Running in split database mode")
      _ <- logger.info(s"Reference DB: ${config.refDbPath}")
      _ <- logger.info(s"User DB: ${config.userDbPath}")
      _ <- logger.info("Initializing user database schema")
      _ <- Schema.initializeUserSchema(dbs.userXa)
      tableCounts <- ReferenceDataRepository.counts(dbs.refXa)
      _ <- printSummary(tableCounts, dbs.refXa)
      _ <- logger.info("Starting HTTP server on port 8080")
      _ <- HttpServer.createServer(8080, dbs.refXa, dbs.userXa, "ref.").useForever
    } yield ()
  }

  private def printSummary(initialCounts: Map[String, Int], xa: Transactor[IO]): IO[Unit] =
    for {
      tableCounts <- ReferenceDataRepository.counts(xa)
      lastUpdate <- ReferenceDataRepository.lastUpdate(xa)
      loaded = tableCounts.exists { case (k, v) => initialCounts.getOrElse(k, 0) < v }
      totalRecords = tableCounts.values.sum
      _ <- logger.info(s"Database ${if (loaded) "loaded" else "ready"}: $totalRecords records, " +
        s"factions=${tableCounts.getOrElse("factions", 0)}, " +
        s"datasheets=${tableCounts.getOrElse("datasheets", 0)}, " +
        s"last_update=${lastUpdate.headOption.map(_.timestamp).getOrElse("unknown")}")
    } yield ()
}
