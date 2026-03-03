package wp40k.domain.models

import wp40k.domain.types.DatasheetId
import wp40k.errors.{ParseError, ParseException}
import wp40k.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO
import cats.syntax.either.*

case class DatasheetStratagem(
  datasheetId: DatasheetId,
  stratagemId: StratagemId
)

object DatasheetStratagemParser extends StreamingCsvParser[DatasheetStratagem] {

  protected[wp40k] def parseLineWithContext(
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

  protected[wp40k] def parseLine(line: String): Either[ParseError, DatasheetStratagem] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      datasheetId <- CsvParsing.parseWith("datasheet_id", cols(0), DatasheetId.parse)
      stratagemId <- CsvParsing.parseWith("stratagem_id", cols(1), StratagemId.parse)
    } yield DatasheetStratagem(datasheetId, stratagemId)
  }
}
