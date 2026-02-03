package wahapedia

import cats.effect.{IO, IOApp, ExitCode}
import doobie.*
import wahapedia.db.{Schema, DataLoader}

object BuildRefDb extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    val (dataDir, outputPath) = args match {
      case dir :: out :: _ => (dir, out)
      case _               => ("../data/wahapedia", "wahapedia-ref.db")
    }

    val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
      driver = "org.sqlite.JDBC",
      url = s"jdbc:sqlite:$outputPath",
      logHandler = None
    )

    val program = for {
      _ <- IO.println(s"Building reference database: $outputPath")
      _ <- IO.println(s"Data source: $dataDir")
      _ <- Schema.initializeRefSchema(xa)
      _ <- DataLoader.loadAllRef(xa, dataDir)
      _ <- IO.println("Reference database built successfully")
    } yield ExitCode.Success

    program.handleErrorWith { e =>
      IO.println(s"Error building reference database: ${e.getMessage}") *>
        IO.raiseError(e)
    }
  }
}
