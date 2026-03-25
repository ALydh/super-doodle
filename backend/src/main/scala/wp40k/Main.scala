package wp40k

import cats.effect.{IO, IOApp, Ref}
import doobie.*
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import wp40k.db.{Schema, DataLoader, ReferenceDataRepository, Database, DatabaseConfig, RevisionState, SessionRepository}
import wp40k.http.{HttpServer, RevisionContext}
import wp40k.errors.ParseException
import scala.concurrent.duration.*

object Main extends IOApp.Simple {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def run: IO[Unit] = {
    val program = for {
      _ <- logger.info("Starting wp40k-api")
      config <- DatabaseConfig.fromEnv
      splitMode = java.io.File(config.refDbPath).exists()
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

  private def startSessionCleanup(xa: Transactor[IO]): IO[Unit] =
    fs2.Stream.fixedRate[IO](1.hour)
      .evalMap(_ => SessionRepository.deleteExpired(xa).flatMap(n => logger.info(s"Cleaned up $n expired sessions")))
      .compile.drain.start.void

  private def startRevisionChecker(
    activeRef: Ref[IO, RevisionState],
    revisionsDir: String,
    userXa: Transactor[IO]
  ): IO[Unit] =
    EmberClientBuilder.default[IO].build.use { client =>
      fs2.Stream.fixedRate[IO](6.hours)
        .evalMap { _ =>
          RevisionUpdater.checkAndUpdate(client, revisionsDir, userXa, activeRef)
            .handleErrorWith(e => logger.error(e)(s"Revision check failed: ${e.getMessage}"))
        }
        .compile.drain
    }.start.void

  private def runSingleMode: IO[Unit] = {
    val xa = Database.singleTransactor("wp40k.db")
    for {
      _ <- logger.info("Running in single database mode")
      _ <- logger.info("Initializing database")
      _ <- Schema.initialize(xa)
      tableCounts <- ReferenceDataRepository.counts(xa)
      _ <- DataLoader.loadMissing(xa, tableCounts)
      _ <- printSummary(tableCounts, xa)
      _ <- startSessionCleanup(xa)
      _ <- logger.info("Starting HTTP server on port 8080")
      _ <- HttpServer.createServer(8080, xa, xa, "").useForever
    } yield ()
  }

  private def runSplitMode(config: DatabaseConfig): IO[Unit] =
    runWithRevisions(config, config.revisionsDir)

  private def runWithRevisions(config: DatabaseConfig, revisionsDir: String): IO[Unit] = {
    val plainUserXa = Database.plainUserTransactor(config.userDbPath)
    EmberClientBuilder.default[IO].build.use { client =>
      for {
        _ <- logger.info("Running in split database mode with revision tracking")
        _ <- logger.info(s"Revisions dir: $revisionsDir")
        _ <- logger.info(s"User DB: ${config.userDbPath}")
        _ <- Schema.initializeUserSchema(plainUserXa)
        initialState <- RevisionUpdater.initialize(revisionsDir, config.refDbPath, plainUserXa)
        activeRef <- Ref.of[IO, RevisionState](initialState)
        dbs = Database.dynamicSplitTransactors(config.userDbPath, activeRef)
        tableCounts <- ReferenceDataRepository.counts(dbs.refXa)
        _ <- printSummary(tableCounts, dbs.refXa)
        _ <- startSessionCleanup(plainUserXa)
        _ <- startRevisionChecker(activeRef, revisionsDir, plainUserXa)
        revCtx = RevisionContext(activeRef, client, plainUserXa, revisionsDir)
        _ <- logger.info("Starting HTTP server on port 8080")
        _ <- HttpServer.createServer(8080, dbs.refXa, dbs.userXa, "ref.", Some(revCtx)).useForever
      } yield ()
    }
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
