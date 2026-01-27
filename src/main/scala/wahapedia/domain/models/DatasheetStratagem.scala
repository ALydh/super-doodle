package wahapedia.domain.models

import wahapedia.domain.types.DatasheetId
import wahapedia.errors.{ParseError, ParseException}
import wahapedia.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO
import cats.syntax.either.*

case class DatasheetStratagem(
  datasheetId: DatasheetId,
  stratagemId: StratagemId
)

object DatasheetStratagemParser extends StreamingCsvParser[DatasheetStratagem] {

  protected[wahapedia] def parseLineWithContext(
    line: String,
    lineNumber: Int,
    filePath: String
  ): IO[DatasheetStratagem] = {
    parseLine(line) match {
      case Right(strat) => IO.pure(strat)
      case Left(error) =>
        IO.raiseError(ParseException(addContext(error, lineNumber, filePath)))
    }
  }

  protected[wahapedia] def parseLine(line: String): Either[ParseError, DatasheetStratagem] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      datasheetId <- CsvParsing.parseWith("datasheet_id", cols(0), DatasheetId.parse)
      stratagemId <- CsvParsing.parseWith("stratagem_id", cols(1), StratagemId.parse)
    } yield DatasheetStratagem(datasheetId, stratagemId)
  }
}
