package wp40k

import cats.effect.{IO, Ref}
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import org.http4s.client.Client
import org.http4s.implicits.*
import org.typelevel.log4cats.Logger
import wp40k.db.{Database, RevisionState, Schema, DataLoader}
import wp40k.domain.models.Revision
import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.time.Instant

object RevisionUpdater {

  private val wahapediaBaseUrl = "https://wahapedia.ru/wh40k10ed"

  private val csvFiles = List(
    "Factions.csv", "Source.csv", "Last_update.csv", "Datasheets.csv",
    "Datasheets_models.csv", "Datasheets_models_cost.csv", "Datasheets_wargear.csv",
    "Datasheets_options.csv", "Datasheets_unit_composition.csv", "Datasheets_keywords.csv",
    "Datasheets_abilities.csv", "Datasheets_leader.csv", "Datasheets_stratagems.csv",
    "Datasheets_enhancements.csv", "Datasheets_detachment_abilities.csv",
    "Stratagems.csv", "Abilities.csv", "Enhancements.csv", "Detachment_abilities.csv"
  )

  def timestampToRevisionId(timestamp: String): String = {
    val parts = timestamp.trim.split(" ")
    val date = parts(0).replace("-", "")
    val time = if (parts.length > 1) parts(1).replace(":", "") else "000000"
    s"$date-$time"
  }

  def initialize(
    revisionsDir: String,
    existingRefDbPath: String,
    userXa: Transactor[IO]
  )(using Logger[IO]): IO[RevisionState] =
    for {
      _ <- IO(Files.createDirectories(Paths.get(revisionsDir)))
      active <- findActiveRevision(userXa)
      state <- active match {
        case Some(rev) =>
          val dbPath = if (Paths.get(rev.dbPath).isAbsolute) rev.dbPath
                       else Paths.get(revisionsDir, Paths.get(rev.dbPath).getFileName.toString).toString
          for {
            exists <- IO(Files.exists(Paths.get(dbPath)))
            result <- if (exists) {
              Logger[IO].info(s"Active revision ${rev.id} at $dbPath") *>
                IO.pure(RevisionState(rev.id, dbPath))
            } else {
              Logger[IO].warn(s"Active revision DB not found at $dbPath, re-migrating") *>
                migrateExisting(revisionsDir, existingRefDbPath, userXa)
            }
          } yield result
        case None =>
          Logger[IO].info("No active revision found, migrating existing ref DB") *>
            migrateExisting(revisionsDir, existingRefDbPath, userXa)
      }
    } yield state

  private def migrateExisting(
    revisionsDir: String,
    existingRefDbPath: String,
    userXa: Transactor[IO]
  )(using Logger[IO]): IO[RevisionState] = {
    val existingPath = Paths.get(existingRefDbPath)
    for {
      exists <- IO(Files.exists(existingPath))
      state <- if (exists) migrateFromExistingDb(revisionsDir, existingPath, userXa)
               else migrateFromCsvs(revisionsDir, userXa)
    } yield state
  }

  private def migrateFromExistingDb(
    revisionsDir: String,
    existingPath: Path,
    userXa: Transactor[IO]
  )(using Logger[IO]): IO[RevisionState] = {
    val tempXa = Database.refTransactor(existingPath.toString)
    for {
      timestamp <- sql"SELECT timestamp FROM last_update LIMIT 1"
        .query[String].option.transact(tempXa)
        .map(_.getOrElse("unknown"))
      revisionId = timestampToRevisionId(timestamp)
      dbFileName = s"wp40k-ref-$revisionId.db"
      dbPath = Paths.get(revisionsDir, dbFileName)
      _ <- IO(Files.copy(existingPath, dbPath, StandardCopyOption.REPLACE_EXISTING))
      _ <- registerRevision(revisionId, timestamp, dbPath.toString, userXa)
      _ <- Logger[IO].info(s"Migrated existing ref DB as revision $revisionId")
    } yield RevisionState(revisionId, dbPath.toString)
  }

  private def migrateFromCsvs(
    revisionsDir: String,
    userXa: Transactor[IO]
  )(using Logger[IO]): IO[RevisionState] = {
    val dataDir = "../data/wp40k"
    for {
      csvExists <- IO(Files.exists(Paths.get(s"$dataDir/Last_update.csv")))
      _ <- if (!csvExists) IO.raiseError(new RuntimeException(
        s"No existing ref DB or CSV data found. Place CSV files in $dataDir or provide a ref DB."
      )) else IO.unit
      content <- IO(new String(Files.readAllBytes(Paths.get(s"$dataDir/Last_update.csv"))))
      timestamp = parseTimestampFromCsv(content)
      revisionId = timestampToRevisionId(timestamp)
      dbFileName = s"wp40k-ref-$revisionId.db"
      dbPath = Paths.get(revisionsDir, dbFileName).toString
      buildXa = Database.singleTransactor(dbPath)
      _ <- Schema.initializeRefSchema(buildXa)
      _ <- DataLoader.loadAllRef(buildXa, dataDir)
      _ <- registerRevision(revisionId, timestamp, dbPath, userXa)
      _ <- Logger[IO].info(s"Built revision $revisionId from CSV data")
    } yield RevisionState(revisionId, dbPath)
  }

  def checkAndUpdate(
    client: Client[IO],
    revisionsDir: String,
    userXa: Transactor[IO],
    activeRef: Ref[IO, RevisionState]
  )(using Logger[IO]): IO[Unit] =
    for {
      _ <- Logger[IO].info("Checking wahapedia for updates...")
      content <- client.expect[String](uri"https://wahapedia.ru/wh40k10ed/Last_update.csv")
      remoteTimestamp = parseTimestampFromCsv(content)
      remoteId = timestampToRevisionId(remoteTimestamp)
      currentState <- activeRef.get
      _ <- if (remoteId == currentState.revisionId) {
        Logger[IO].info(s"Already on latest revision $remoteId")
      } else {
        for {
          existing <- findRevision(remoteId, userXa)
          _ <- existing match {
            case Some(rev) =>
              Logger[IO].info(s"Revision $remoteId already exists, activating") *>
                activateRevision(rev.id, rev.dbPath, userXa, activeRef)
            case None =>
              fetchAndBuild(client, revisionsDir, remoteTimestamp, userXa, activeRef)
          }
          _ <- cleanup(revisionsDir, userXa, activeRef)
        } yield ()
      }
    } yield ()

  private def fetchAndBuild(
    client: Client[IO],
    revisionsDir: String,
    timestamp: String,
    userXa: Transactor[IO],
    activeRef: Ref[IO, RevisionState]
  )(using Logger[IO]): IO[Unit] = {
    val revisionId = timestampToRevisionId(timestamp)
    val dbFileName = s"wp40k-ref-$revisionId.db"
    val dbPath = Paths.get(revisionsDir, dbFileName).toString

    for {
      tempDir <- IO(Files.createTempDirectory("wp40k-fetch"))
      _ <- Logger[IO].info(s"Downloading CSVs for revision $revisionId...")
      _ <- csvFiles.traverse_ { file =>
        val url = org.http4s.Uri.unsafeFromString(s"$wahapediaBaseUrl/$file")
        client.expect[String](url).flatMap { content =>
          IO(Files.writeString(tempDir.resolve(file), content))
        }
      }
      _ <- Logger[IO].info(s"Building ref DB at $dbPath...")
      buildXa = Database.singleTransactor(dbPath)
      _ <- Schema.initializeRefSchema(buildXa)
      _ <- DataLoader.loadAllRef(buildXa, tempDir.toString)
      _ <- registerRevision(revisionId, timestamp, dbPath, userXa)
      _ <- activateRevision(revisionId, dbPath, userXa, activeRef)
      _ <- IO(deleteDirectory(tempDir))
      _ <- Logger[IO].info(s"Activated revision $revisionId")
    } yield ()
  }

  private def activateRevision(
    revisionId: String,
    dbPath: String,
    userXa: Transactor[IO],
    activeRef: Ref[IO, RevisionState]
  ): IO[Unit] =
    for {
      _ <- (sql"UPDATE revisions SET is_active = 0".update.run *>
           sql"UPDATE revisions SET is_active = 1 WHERE id = $revisionId".update.run).transact(userXa)
      _ <- activeRef.set(RevisionState(revisionId, dbPath))
    } yield ()

  def cleanup(
    revisionsDir: String,
    userXa: Transactor[IO],
    activeRef: Ref[IO, RevisionState],
    keep: Int = 10
  )(using Logger[IO]): IO[Unit] =
    for {
      all <- sql"SELECT id, db_path, is_active FROM revisions ORDER BY fetched_at DESC"
        .query[(String, String, Boolean)].to[List].transact(userXa)
      toDelete = all.filterNot(_._3).drop(keep - 1)
      _ <- toDelete.traverse_ { case (id, dbPath, _) =>
        IO(Files.deleteIfExists(Paths.get(dbPath))) *>
          sql"DELETE FROM revisions WHERE id = $id".update.run.transact(userXa) *>
          Logger[IO].info(s"Cleaned up old revision $id")
      }
    } yield ()

  private def registerRevision(
    revisionId: String,
    timestamp: String,
    dbPath: String,
    userXa: Transactor[IO]
  ): IO[Unit] = {
    val now = Instant.now().toString
    (sql"UPDATE revisions SET is_active = 0".update.run *>
     sql"""INSERT OR REPLACE INTO revisions (id, wahapedia_timestamp, db_path, fetched_at, is_active)
           VALUES ($revisionId, $timestamp, $dbPath, $now, 1)""".update.run).transact(userXa).void
  }

  private def findActiveRevision(xa: Transactor[IO]): IO[Option[Revision]] =
    sql"SELECT id, wahapedia_timestamp, db_path, fetched_at, is_active FROM revisions WHERE is_active = 1"
      .query[Revision].option.transact(xa)

  private def findRevision(id: String, xa: Transactor[IO]): IO[Option[Revision]] =
    sql"SELECT id, wahapedia_timestamp, db_path, fetched_at, is_active FROM revisions WHERE id = $id"
      .query[Revision].option.transact(xa)

  def listAll(xa: Transactor[IO]): IO[List[Revision]] =
    sql"SELECT id, wahapedia_timestamp, db_path, fetched_at, is_active FROM revisions ORDER BY fetched_at DESC"
      .query[Revision].to[List].transact(xa)

  private def parseTimestampFromCsv(content: String): String = {
    val lines = content.linesIterator.toList.map(_.trim).filter(_.nonEmpty)
    lines.drop(1).headOption
      .map(_.stripPrefix("\uFEFF").stripSuffix("|").trim)
      .getOrElse("unknown")
  }

  private def deleteDirectory(path: Path): Unit = {
    if (Files.isDirectory(path)) {
      Files.list(path).forEach(p => deleteDirectory(p))
    }
    Files.deleteIfExists(path)
  }

  given Read[Revision] = Read[(String, String, String, String, Boolean)].map {
    case (id, ts, path, fetched, active) => Revision(id, ts, path, fetched, active)
  }

  given Read[Boolean] = Read[Int].map(_ != 0)
}
