package wahapedia.domain.models

import wahapedia.domain.types.{DatasheetId, AbilityId}
import wahapedia.errors.{ParseError, ParseException}
import wahapedia.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO
import cats.syntax.either.*

case class DatasheetAbility(
  datasheetId: DatasheetId,
  line: Int,
  abilityId: Option[AbilityId],
  model: Option[String],
  name: Option[String],
  description: Option[String],
  abilityType: Option[String],
  parameter: Option[String]
)

object DatasheetAbilityParser extends StreamingCsvParser[DatasheetAbility] {

  protected[wahapedia] def parseLineWithContext(
    line: String,
    lineNumber: Int,
    filePath: String
  ): IO[DatasheetAbility] = {
    parseLine(line) match {
      case Right(ability) => IO.pure(ability)
      case Left(error) =>
        IO.raiseError(ParseException(addContext(error, lineNumber, filePath)))
    }
  }

  protected[wahapedia] def parseLine(line: String): Either[ParseError, DatasheetAbility] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      datasheetId <- CsvParsing.parseWith("datasheet_id", cols(0), DatasheetId.parse)
      lineNum <- CsvParsing.parseInt("line", cols(1))
      abilityId <- CsvParsing.parseOptWith("ability_id", cols(2), AbilityId.parse)
      model = CsvParsing.parseOptString(cols(3))
      name = CsvParsing.parseOptString(cols(4))
      description = CsvParsing.parseOptString(cols(5))
      abilityType = CsvParsing.parseOptString(cols(6))
      parameter = CsvParsing.parseOptString(cols(7))
    } yield DatasheetAbility(datasheetId, lineNum, abilityId, model, name, description, abilityType, parameter)
  }
}
