package wahapedia.domain.models

import wahapedia.errors.{ParseError, ParseException}
import wahapedia.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO
import cats.syntax.either.*

case class LastUpdate(
  timestamp: String
)

object LastUpdateParser extends StreamingCsvParser[LastUpdate] {

  protected[wahapedia] def parseLineWithContext(
    line: String,
    lineNumber: Int,
    filePath: String
  ): IO[LastUpdate] = {
    parseLine(line) match {
      case Right(update) => IO.pure(update)
      case Left(error) =>
        IO.raiseError(ParseException(addContext(error, lineNumber, filePath)))
    }
  }

  protected[wahapedia] def parseLine(line: String): Either[ParseError, LastUpdate] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      timestamp <- CsvParsing.parseString("last_update", cols(0))
    } yield LastUpdate(timestamp)
  }
}
