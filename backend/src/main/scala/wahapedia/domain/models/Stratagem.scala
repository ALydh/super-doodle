package wahapedia.domain.models

import wahapedia.domain.types.FactionId
import wahapedia.errors.{ParseError, ParseException}
import wahapedia.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO
import cats.syntax.either.*
import io.circe.Encoder

opaque type StratagemId = String

object StratagemId {
  def apply(id: String): StratagemId = id
  def value(id: StratagemId): String = id

  def parse(id: String): Either[ParseError, StratagemId] = {
    if (id.nonEmpty) Right(StratagemId(id))
    else Left(wahapedia.errors.InvalidId(id))
  }

  given Encoder[StratagemId] = Encoder.encodeString.contramap(value)
}

case class Stratagem(
  factionId: Option[FactionId],
  name: String,
  id: StratagemId,
  stratagemType: Option[String],
  cpCost: Option[Int],
  legend: Option[String],
  turn: Option[String],
  phase: Option[String],
  detachment: Option[String],
  detachmentId: Option[String],
  description: String
)

object StratagemParser extends StreamingCsvParser[Stratagem] {

  protected[wahapedia] def parseLineWithContext(
    line: String,
    lineNumber: Int,
    filePath: String
  ): IO[Stratagem] = {
    parseLine(line) match {
      case Right(stratagem) => IO.pure(stratagem)
      case Left(error) =>
        IO.raiseError(ParseException(addContext(error, lineNumber, filePath)))
    }
  }

  protected[wahapedia] def parseLine(line: String): Either[ParseError, Stratagem] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      factionId <- CsvParsing.parseOptWith("faction_id", cols(0), FactionId.parse)
      name <- CsvParsing.parseString("name", cols(1))
      id <- CsvParsing.parseWith("id", cols(2), StratagemId.parse)
      stratagemType = CsvParsing.parseOptString(cols(3))
      cpCost <- CsvParsing.parseOptInt("cp_cost", cols(4))
      legend = CsvParsing.parseOptString(cols(5))
      turn = CsvParsing.parseOptString(cols(6))
      phase = CsvParsing.parseOptString(cols(7))
      detachment = CsvParsing.parseOptString(cols(8))
      detachmentId = CsvParsing.parseOptString(cols(9))
      description <- CsvParsing.parseString("description", cols(10))
    } yield Stratagem(factionId, name, id, stratagemType, cpCost, legend, turn, phase, detachment, detachmentId, description)
  }
}
