package wp40k.domain.models

import wp40k.domain.types.DatasheetId
import wp40k.errors.{ParseError, ParseException}
import wp40k.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO

case class UnitComposition(
  datasheetId: DatasheetId,
  line: Int,
  description: String
)

object UnitCompositionParser extends StreamingCsvParser[UnitComposition] {

  protected[wp40k] def parseLineWithContext(
    line: String,
    lineNumber: Int,
    filePath: String
  ): IO[UnitComposition] = {
    parseLine(line) match {
      case Right(comp) => IO.pure(comp)
      case Left(error) =>
        IO.raiseError(ParseException(addContext(error, lineNumber, filePath)))
    }
  }

  protected[wp40k] def parseLine(line: String): Either[ParseError, UnitComposition] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      datasheetId <- CsvParsing.parseWith("datasheet_id", cols(0), DatasheetId.parse)
      lineNum <- CsvParsing.parseInt("line", cols(1))
      description <- CsvParsing.parseString("description", cols(2))
    } yield UnitComposition(datasheetId, lineNum, description)
  }
}
