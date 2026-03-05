package wp40k.domain.models

import wp40k.domain.types.DatasheetId
import wp40k.errors.{ParseError, ParseException}
import wp40k.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO

case class DatasheetKeyword(
  datasheetId: DatasheetId,
  keyword: Option[String],
  model: Option[String],
  isFactionKeyword: Boolean
)

object DatasheetKeywordParser extends StreamingCsvParser[DatasheetKeyword] {

  protected[wp40k] def parseLineWithContext(
    line: String,
    lineNumber: Int,
    filePath: String
  ): IO[DatasheetKeyword] = {
    parseLine(line) match {
      case Right(keyword) => IO.pure(keyword)
      case Left(error) =>
        IO.raiseError(ParseException(addContext(error, lineNumber, filePath)))
    }
  }

  protected[wp40k] def parseLine(line: String): Either[ParseError, DatasheetKeyword] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      datasheetId <- CsvParsing.parseWith("datasheet_id", cols(0), DatasheetId.parse)
      keyword = CsvParsing.parseOptString(cols(1))
      model = CsvParsing.parseOptString(cols(2))
      isFactionKeyword <- CsvParsing.parseBoolean("is_faction_keyword", cols(3))
    } yield DatasheetKeyword(datasheetId, keyword, model, isFactionKeyword)
  }
}
