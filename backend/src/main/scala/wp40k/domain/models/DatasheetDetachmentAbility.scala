package wp40k.domain.models

import wp40k.domain.types.DatasheetId
import wp40k.errors.{ParseError, ParseException}
import wp40k.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO
import cats.syntax.either.*

case class DatasheetDetachmentAbility(
  datasheetId: DatasheetId,
  detachmentAbilityId: DetachmentAbilityId
)

object DatasheetDetachmentAbilityParser extends StreamingCsvParser[DatasheetDetachmentAbility] {

  protected[wp40k] def parseLineWithContext(
    line: String,
    lineNumber: Int,
    filePath: String
  ): IO[DatasheetDetachmentAbility] = {
    parseLine(line) match {
      case Right(detAbility) => IO.pure(detAbility)
      case Left(error) =>
        IO.raiseError(ParseException(addContext(error, lineNumber, filePath)))
    }
  }

  protected[wp40k] def parseLine(line: String): Either[ParseError, DatasheetDetachmentAbility] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      datasheetId <- CsvParsing.parseWith("datasheet_id", cols(0), DatasheetId.parse)
      detachmentAbilityId <- CsvParsing.parseWith("detachment_ability_id", cols(1), DetachmentAbilityId.parse)
    } yield DatasheetDetachmentAbility(datasheetId, detachmentAbilityId)
  }
}
