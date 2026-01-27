package wahapedia.domain.models

import wahapedia.domain.types.{DatasheetId, FactionId, SourceId, Role}
import wahapedia.errors.{ParseError, ParseException}
import wahapedia.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO
import cats.syntax.either.*

case class Datasheet(
  id: DatasheetId,
  name: String,
  factionId: Option[FactionId],
  sourceId: Option[SourceId],
  legend: Option[String],
  role: Option[Role],
  loadout: Option[String],
  transport: Option[String],
  virtual: Boolean,
  leaderHead: Option[String],
  leaderFooter: Option[String],
  damagedW: Option[String],
  damagedDescription: Option[String],
  link: String
)

object DatasheetParser extends StreamingCsvParser[Datasheet] {

  protected[wahapedia] def parseLineWithContext(
    line: String,
    lineNumber: Int,
    filePath: String
  ): IO[Datasheet] = {
    parseLine(line) match {
      case Right(datasheet) => IO.pure(datasheet)
      case Left(error) =>
        IO.raiseError(ParseException(addContext(error, lineNumber, filePath)))
    }
  }

  protected[wahapedia] def parseLine(line: String): Either[ParseError, Datasheet] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      id <- CsvParsing.parseWith("id", cols(0), DatasheetId.parse)
      name <- CsvParsing.parseString("name", cols(1))
      factionId <- CsvParsing.parseOptWith("faction_id", cols(2), FactionId.parse)
      sourceId <- CsvParsing.parseOptWith("source_id", cols(3), SourceId.parse)
      legend = CsvParsing.parseOptString(cols(4))
      role <- CsvParsing.parseOptWith("role", cols(5), Role.parse)
      loadout = CsvParsing.parseOptString(cols(6))
      transport = CsvParsing.parseOptString(cols(7))
      virtual <- CsvParsing.parseBoolean("virtual", cols(8))
      leaderHead = CsvParsing.parseOptString(cols(9))
      leaderFooter = CsvParsing.parseOptString(cols(10))
      damagedW = CsvParsing.parseOptString(cols(11))
      damagedDescription = CsvParsing.parseOptString(cols(12))
      link <- CsvParsing.parseString("link", cols(13))
    } yield Datasheet(
      id, name, factionId, sourceId, legend, role, loadout,
      transport, virtual, leaderHead, leaderFooter,
      damagedW, damagedDescription, link
    )
  }
}
