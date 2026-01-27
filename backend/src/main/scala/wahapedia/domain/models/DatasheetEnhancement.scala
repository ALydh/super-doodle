package wahapedia.domain.models

import wahapedia.domain.types.DatasheetId
import wahapedia.errors.{ParseError, ParseException}
import wahapedia.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO
import cats.syntax.either.*

case class DatasheetEnhancement(
  datasheetId: DatasheetId,
  enhancementId: EnhancementId
)

object DatasheetEnhancementParser extends StreamingCsvParser[DatasheetEnhancement] {

  protected[wahapedia] def parseLineWithContext(
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

  protected[wahapedia] def parseLine(line: String): Either[ParseError, DatasheetEnhancement] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      datasheetId <- CsvParsing.parseWith("datasheet_id", cols(0), DatasheetId.parse)
      enhancementId <- CsvParsing.parseWith("enhancement_id", cols(1), EnhancementId.parse)
    } yield DatasheetEnhancement(datasheetId, enhancementId)
  }
}
