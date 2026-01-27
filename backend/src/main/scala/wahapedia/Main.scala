package wahapedia

import cats.effect.{IO, IOApp}
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import wahapedia.db.{Schema, DataLoader, ReferenceDataRepository}
import wahapedia.http.HttpServer
import wahapedia.errors.ParseException

object Main extends IOApp.Simple {

  private val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    driver = "org.sqlite.JDBC",
    url = "jdbc:sqlite:wahapedia.db?foreign_keys=on",
    logHandler = None
  )

  def run: IO[Unit] = {
    val program = for {
      _ <- IO.println("Initializing database...")
      _ <- Schema.initialize(xa)
      tableCounts <- ReferenceDataRepository.counts(xa)
      isPopulated = tableCounts.getOrElse("factions", 0) > 0
      _ <- if (isPopulated) {
        IO.println("Database already populated. Skipping CSV data loading.")
      } else {
        IO.println("Loading CSV data into database...") *> DataLoader.loadAll(xa)
      }
      _ <- printSummary(isPopulated)
      _ <- IO.println("Starting HTTP server on port 8080...")
      _ <- HttpServer.createServer(8080, xa).useForever
    } yield ()

    program.handleErrorWith {
      case pe: ParseException =>
        IO.println(s"Parse error: ${pe.getMessage}")
      case e =>
        IO.println(s"Unexpected error: ${e.getMessage}") *>
          IO.raiseError(e)
    }
  }

  private def printSummary(isPopulated: Boolean): IO[Unit] =
    for {
      tableCounts <- ReferenceDataRepository.counts(xa)
      lastUpdate <- ReferenceDataRepository.lastUpdate(xa)

      _ <- IO.println("\n" + "=" * 60)
      _ <- IO.println(if (isPopulated) "DATABASE STATUS" else "DATABASE LOAD COMPLETE")
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
