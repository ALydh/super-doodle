package wp40k.domain.models

import wp40k.domain.types.DatasheetId
import wp40k.errors.{ParseError, ParseException}
import wp40k.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO
import cats.syntax.either.*

case class DatasheetOption(
  datasheetId: DatasheetId,
  line: Int,
  button: Option[String],
  description: String
)

object DatasheetOptionParser extends StreamingCsvParser[DatasheetOption] {

  protected[wp40k] def parseLineWithContext(
    line: String,
    lineNumber: Int,
    filePath: String
  ): IO[DatasheetOption] = {
    parseLine(line) match {
      case Right(option) => IO.pure(option)
      case Left(error) =>
        IO.raiseError(ParseException(addContext(error, lineNumber, filePath)))
    }
  }

  protected[wp40k] def parseLine(line: String): Either[ParseError, DatasheetOption] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      datasheetId <- CsvParsing.parseWith("datasheet_id", cols(0), DatasheetId.parse)
      lineNum <- CsvParsing.parseInt("line", cols(1))
      button = CsvParsing.parseOptString(cols(2))
      description <- CsvParsing.parseString("description", cols(3))
    } yield DatasheetOption(datasheetId, lineNum, button, description)
  }
}
