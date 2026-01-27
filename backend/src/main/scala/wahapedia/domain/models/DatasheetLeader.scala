package wahapedia.domain.models

import wahapedia.domain.types.DatasheetId
import wahapedia.errors.{ParseError, ParseException}
import wahapedia.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO
import cats.syntax.either.*

case class DatasheetLeader(
  leaderId: DatasheetId,
  attachedId: DatasheetId
)

object DatasheetLeaderParser extends StreamingCsvParser[DatasheetLeader] {

  protected[wahapedia] def parseLineWithContext(
    line: String,
    lineNumber: Int,
    filePath: String
  ): IO[DatasheetLeader] = {
    parseLine(line) match {
      case Right(leader) => IO.pure(leader)
      case Left(error) =>
        IO.raiseError(ParseException(addContext(error, lineNumber, filePath)))
    }
  }

  protected[wahapedia] def parseLine(line: String): Either[ParseError, DatasheetLeader] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      leaderId <- CsvParsing.parseWith("leader_id", cols(0), DatasheetId.parse)
      attachedId <- CsvParsing.parseWith("attached_id", cols(1), DatasheetId.parse)
    } yield DatasheetLeader(leaderId, attachedId)
  }
}
