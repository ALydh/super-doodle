package wp40k.domain.models

import wp40k.domain.types.DatasheetId
import wp40k.errors.{ParseError, ParseException}
import wp40k.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO

case class DatasheetEnhancement(
  datasheetId: DatasheetId,
  enhancementId: EnhancementId
)

object DatasheetEnhancementParser extends StreamingCsvParser[DatasheetEnhancement] {

  protected[wp40k] def parseLineWithContext(
    line: String,
    lineNumber: Int,
    filePath: String
  ): IO[DatasheetEnhancement] = {
    parseLine(line) match {
      case Right(enh) => IO.pure(enh)
      case Left(error) =>
        IO.raiseError(ParseException(addContext(error, lineNumber, filePath)))
    }
  }

  protected[wp40k] def parseLine(line: String): Either[ParseError, DatasheetEnhancement] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      datasheetId <- CsvParsing.parseWith("datasheet_id", cols(0), DatasheetId.parse)
      enhancementId <- CsvParsing.parseWith("enhancement_id", cols(1), EnhancementId.parse)
    } yield DatasheetEnhancement(datasheetId, enhancementId)
  }
}
