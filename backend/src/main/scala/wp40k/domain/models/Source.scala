package wp40k.domain.models

import wp40k.domain.types.{SourceId, SourceType}
import wp40k.errors.{ParseError, ParseException}
import wp40k.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO

case class Source(
  id: SourceId,
  name: String,
  sourceType: SourceType,
  edition: Int,
  version: Option[String],
  errataDate: Option[String],
  errataLink: Option[String]
)

object SourceParser extends StreamingCsvParser[Source] {

  protected[wp40k] def parseLineWithContext(
    line: String,
    lineNumber: Int,
    filePath: String
  ): IO[Source] = {
    parseLine(line) match {
      case Right(source) => IO.pure(source)
      case Left(error) =>
        IO.raiseError(ParseException(addContext(error, lineNumber, filePath)))
    }
  }

  protected[wp40k] def parseLine(line: String): Either[ParseError, Source] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      id <- CsvParsing.parseWith("id", cols(0), SourceId.parse)
      name <- CsvParsing.parseString("name", cols(1))
      sourceType <- CsvParsing.parseWith("source_type", cols(2), SourceType.parse)
      edition <- CsvParsing.parseInt("edition", cols(3))
      version = CsvParsing.parseOptString(cols(4))
      errataDate = CsvParsing.parseOptString(cols(5))
      errataLink = CsvParsing.parseOptString(cols(6))
    } yield Source(id, name, sourceType, edition, version, errataDate, errataLink)
  }
}
