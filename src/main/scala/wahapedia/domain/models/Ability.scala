package wahapedia.domain.models

import wahapedia.domain.types.{AbilityId, FactionId}
import wahapedia.errors.{ParseError, ParseException}
import wahapedia.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO
import cats.syntax.either.*

case class Ability(
  id: AbilityId,
  name: String,
  legend: Option[String],
  factionId: Option[FactionId],
  description: String
)

object AbilityParser extends StreamingCsvParser[Ability] {

  protected[wahapedia] def parseLineWithContext(
    line: String,
    lineNumber: Int,
    filePath: String
  ): IO[Ability] = {
    parseLine(line) match {
      case Right(ability) => IO.pure(ability)
      case Left(error) =>
        IO.raiseError(ParseException(addContext(error, lineNumber, filePath)))
    }
  }

  protected[wahapedia] def parseLine(line: String): Either[ParseError, Ability] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      id <- CsvParsing.parseWith("id", cols(0), AbilityId.parse)
      name <- CsvParsing.parseString("name", cols(1))
      legend = CsvParsing.parseOptString(cols(2))
      factionId <- CsvParsing.parseOptWith("faction_id", cols(3), FactionId.parse)
      description <- CsvParsing.parseString("description", cols(4))
    } yield Ability(id, name, legend, factionId, description)
  }
}
