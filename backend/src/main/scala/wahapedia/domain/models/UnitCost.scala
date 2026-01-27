package wahapedia.domain.models

import wahapedia.domain.types.DatasheetId
import wahapedia.errors.{ParseError, ParseException}
import wahapedia.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO
import cats.syntax.either.*

case class UnitCost(
  datasheetId: DatasheetId,
  line: Int,
  description: String,
  cost: Int
)

object UnitCostParser extends StreamingCsvParser[UnitCost] {

  protected[wahapedia] def parseLineWithContext(
    line: String,
    lineNumber: Int,
    filePath: String
  ): IO[UnitCost] = {
    parseLine(line) match {
      case Right(cost) => IO.pure(cost)
      case Left(error) =>
        IO.raiseError(ParseException(addContext(error, lineNumber, filePath)))
    }
  }

  protected[wahapedia] def parseLine(line: String): Either[ParseError, UnitCost] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      datasheetId <- CsvParsing.parseWith("datasheet_id", cols(0), DatasheetId.parse)
      lineNum <- CsvParsing.parseInt("line", cols(1))
      description <- CsvParsing.parseString("description", cols(2))
      cost <- CsvParsing.parseInt("cost", cols(3))
    } yield UnitCost(datasheetId, lineNum, description, cost)
  }
}
