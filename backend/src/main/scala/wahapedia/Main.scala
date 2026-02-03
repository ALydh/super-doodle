package wahapedia

import cats.effect.{IO, IOApp}
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import wahapedia.db.{Schema, DataLoader, ReferenceDataRepository, Database, DatabaseConfig}
import wahapedia.http.HttpServer
import wahapedia.errors.ParseException

object Main extends IOApp.Simple {

  def run: IO[Unit] = {
    val program = for {
      config <- DatabaseConfig.fromEnv
      splitMode = config.refDbPath != DatabaseConfig.default.refDbPath ||
                  config.userDbPath != DatabaseConfig.default.userDbPath
      _ <- if (splitMode) runSplitMode(config) else runSingleMode
    } yield ()

    program.handleErrorWith {
      case pe: ParseException =>
        IO.println(s"Parse error: ${pe.getMessage}")
      case e =>
        IO.println(s"Unexpected error: ${e.getMessage}") *>
          IO.raiseError(e)
    }
  }

  private def runSingleMode: IO[Unit] = {
    val xa = Database.singleTransactor("wahapedia.db")
    for {
      _ <- IO.println("Running in single database mode...")
      _ <- IO.println("Initializing database...")
      _ <- Schema.initialize(xa)
      tableCounts <- ReferenceDataRepository.counts(xa)
      _ <- DataLoader.loadMissing(xa, tableCounts)
      _ <- printSummary(tableCounts, xa)
      _ <- IO.println("Starting HTTP server on port 8080...")
      _ <- HttpServer.createServer(8080, xa, xa, "").useForever
    } yield ()
  }

  private def runSplitMode(config: DatabaseConfig): IO[Unit] = {
    val dbs = Database.transactors(config)
    for {
      _ <- IO.println("Running in split database mode...")
      _ <- IO.println(s"Reference DB: ${config.refDbPath}")
      _ <- IO.println(s"User DB: ${config.userDbPath}")
      _ <- IO.println("Initializing user database schema...")
      _ <- Schema.initializeUserSchema(dbs.userXa)
      _ <- IO.println("Attaching reference database...")
      _ <- Database.attachRefDb(dbs.userXa, config.refDbPath)
      tableCounts <- ReferenceDataRepository.counts(dbs.refXa)
      _ <- printSummary(tableCounts, dbs.refXa)
      _ <- IO.println("Starting HTTP server on port 8080...")
      _ <- HttpServer.createServer(8080, dbs.refXa, dbs.userXa, "ref.").useForever
    } yield ()
  }

  private def printSummary(initialCounts: Map[String, Int], xa: Transactor[IO]): IO[Unit] =
    for {
      tableCounts <- ReferenceDataRepository.counts(xa)
      lastUpdate <- ReferenceDataRepository.lastUpdate(xa)
      loaded = tableCounts.exists { case (k, v) => initialCounts.getOrElse(k, 0) < v }

      _ <- IO.println("\n" + "=" * 60)
      _ <- IO.println(if (loaded) "DATABASE LOAD COMPLETE" else "DATABASE STATUS")
      _ <- IO.println("=" * 60)

      _ <- IO.println(s"\nCORE ENTITIES:")
      _ <- IO.println(s"  Factions: ${tableCounts.getOrElse("factions", 0)}")
      _ <- IO.println(s"  Sources: ${tableCounts.getOrElse("sources", 0)}")
      _ <- IO.println(s"  Abilities: ${tableCounts.getOrElse("abilities", 0)}")
      _ <- IO.println(s"  Datasheets: ${tableCounts.getOrElse("datasheets", 0)}")

      _ <- IO.println(s"\nUNIT DATA:")
      _ <- IO.println(s"  Model Profiles: ${tableCounts.getOrElse("model_profiles", 0)}")
      _ <- IO.println(s"  Wargear: ${tableCounts.getOrElse("wargear", 0)}")
      _ <- IO.println(s"  Unit Composition: ${tableCounts.getOrElse("unit_composition", 0)}")
      _ <- IO.println(s"  Unit Costs: ${tableCounts.getOrElse("unit_cost", 0)}")

      _ <- IO.println(s"\nASSOCIATIONS:")
      _ <- IO.println(s"  Keywords: ${tableCounts.getOrElse("datasheet_keywords", 0)}")
      _ <- IO.println(s"  Datasheet Abilities: ${tableCounts.getOrElse("datasheet_abilities", 0)}")
      _ <- IO.println(s"  Options: ${tableCounts.getOrElse("datasheet_options", 0)}")
      _ <- IO.println(s"  Leaders: ${tableCounts.getOrElse("datasheet_leaders", 0)}")

      _ <- IO.println(s"\nSTRATEGEMS:")
      _ <- IO.println(s"  Stratagems: ${tableCounts.getOrElse("stratagems", 0)}")
      _ <- IO.println(s"  Datasheet Stratagems: ${tableCounts.getOrElse("datasheet_stratagems", 0)}")

      _ <- IO.println(s"\nENHANCEMENTS:")
      _ <- IO.println(s"  Enhancements: ${tableCounts.getOrElse("enhancements", 0)}")
      _ <- IO.println(s"  Datasheet Enhancements: ${tableCounts.getOrElse("datasheet_enhancements", 0)}")

      _ <- IO.println(s"\nDETACHMENT ABILITIES:")
      _ <- IO.println(s"  Detachment Abilities: ${tableCounts.getOrElse("detachment_abilities", 0)}")
      _ <- IO.println(s"  Datasheet Detachment Abilities: ${tableCounts.getOrElse("datasheet_detachment_abilities", 0)}")

      _ <- IO.println(s"\nMETADATA:")
      _ <- IO.println(s"  Last Update: ${lastUpdate.headOption.map(_.timestamp).getOrElse("Unknown")}")

      totalRecords = tableCounts.values.sum
      _ <- IO.println(s"\nTOTAL RECORDS IN DATABASE: $totalRecords")
      _ <- IO.println("=" * 60)
    } yield ()
}
