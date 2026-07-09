package wp40k.domain.models

import wp40k.domain.types.{FactionId, DetachmentId}
import wp40k.errors.{ParseError, ParseException}
import wp40k.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO
import io.circe.{Encoder, Decoder}
import sttp.tapir.Schema

case class Detachment(
  id: DetachmentId,
  factionId: FactionId,
  name: String,
  dpCost: Int,
  keyword: Option[String],
  forceDispositions: List[String]
)

object DetachmentParser extends StreamingCsvParser[Detachment] {

  protected[wp40k] def parseLineWithContext(
    line: String,
    lineNumber: Int,
    filePath: String
  ): IO[Detachment] = {
    parseLine(line) match {
      case Right(detachment) => IO.pure(detachment)
      case Left(error) =>
        IO.raiseError(ParseException(addContext(error, lineNumber, filePath)))
    }
  }

  protected[wp40k] def parseLine(line: String): Either[ParseError, Detachment] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      id <- CsvParsing.parseWith("id", cols(0), DetachmentId.parse)
      factionId <- CsvParsing.parseWith("faction_id", cols(1), FactionId.parse)
      name <- CsvParsing.parseString("name", cols(2))
      dpCost <- CsvParsing.parseInt("dp_cost", cols(3))
      keyword = CsvParsing.parseOptString(cols(4))
      forceDispositions = CsvParsing.parseOptString(cols(5))
        .map(_.split(",").toList.map(_.trim).filter(_.nonEmpty))
        .getOrElse(List.empty)
    } yield Detachment(id, factionId, name, dpCost, keyword, forceDispositions)
  }
}
