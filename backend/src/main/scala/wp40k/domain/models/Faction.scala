package wp40k.domain.models

import wp40k.domain.types.FactionId
import wp40k.errors.{ParseError, ParseException}
import wp40k.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO
import cats.syntax.either.*

case class Faction(
  id: FactionId,
  name: String,
  link: String,
  group: Option[String]
)

object FactionParser extends StreamingCsvParser[Faction] {

  protected[wp40k] def parseLineWithContext(
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

  protected[wp40k] def parseLine(line: String): Either[ParseError, Faction] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      id <- CsvParsing.parseWith("id", cols(0), FactionId.parse)
      name <- CsvParsing.parseString("name", cols(1))
      link <- CsvParsing.parseString("link", cols(2))
    } yield Faction(id, name, link, None)
  }
}
