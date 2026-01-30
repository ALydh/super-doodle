package wahapedia.domain.models

import wahapedia.domain.types.FactionId
import wahapedia.errors.{ParseError, ParseException}
import wahapedia.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO
import cats.syntax.either.*

case class Faction(
  id: FactionId,
  name: String,
  link: String,
  group: Option[String]
)

object FactionParser extends StreamingCsvParser[Faction] {

  protected[wahapedia] def parseLineWithContext(
    line: String,
    lineNumber: Int,
    filePath: String
  ): IO[Faction] = {
    parseLine(line) match {
      case Right(faction) => IO.pure(faction)
      case Left(error) =>
        IO.raiseError(ParseException(addContext(error, lineNumber, filePath)))
    }
  }

  protected[wahapedia] def parseLine(line: String): Either[ParseError, Faction] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      id <- CsvParsing.parseWith("id", cols(0), FactionId.parse)
      name <- CsvParsing.parseString("name", cols(1))
      link <- CsvParsing.parseString("link", cols(2))
    } yield Faction(id, name, link, None)
  }
}
