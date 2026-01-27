package wahapedia.domain.models

import wahapedia.domain.types.DatasheetId
import wahapedia.errors.{ParseError, ParseException}
import wahapedia.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO
import cats.syntax.either.*

case class UnitComposition(
  datasheetId: DatasheetId,
  line: Int,
  description: String
)

object UnitCompositionParser extends StreamingCsvParser[UnitComposition] {

  protected[wahapedia] def parseLineWithContext(
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

  protected[wahapedia] def parseLine(line: String): Either[ParseError, UnitComposition] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      datasheetId <- CsvParsing.parseWith("datasheet_id", cols(0), DatasheetId.parse)
      lineNum <- CsvParsing.parseInt("line", cols(1))
      description <- CsvParsing.parseString("description", cols(2))
    } yield UnitComposition(datasheetId, lineNum, description)
  }
}
