package wp40k.domain.models

import wp40k.domain.types.FactionId
import wp40k.errors.{ParseError, ParseException}
import wp40k.csv.{StreamingCsvParser, CsvParsing}
import io.circe.{Encoder, Decoder}
import sttp.tapir.Schema
import cats.effect.IO
import cats.syntax.either.*

opaque type EnhancementId = String

object EnhancementId {
  def apply(id: String): EnhancementId = id
  def value(id: EnhancementId): String = id

  def parse(id: String): Either[ParseError, EnhancementId] = {
    if (id.nonEmpty) Right(EnhancementId(id))
    else Left(wp40k.errors.InvalidId(id))
  }

  given Encoder[EnhancementId] = Encoder.encodeString.contramap(value)
  given Decoder[EnhancementId] = Decoder.decodeString.emap(str => parse(str).leftMap(err => ParseError.formatError(err)))
  given Schema[EnhancementId] = Schema.string
}

case class Enhancement(
  factionId: FactionId,
  id: EnhancementId,
  name: String,
  cost: Int,
  detachment: Option[String],
  detachmentId: Option[String],
  legend: Option[String],
  description: String
)

object EnhancementParser extends StreamingCsvParser[Enhancement] {

  protected[wp40k] def parseLineWithContext(
    line: String,
    lineNumber: Int,
    filePath: String
  ): IO[Enhancement] = {
    parseLine(line) match {
      case Right(enhancement) => IO.pure(enhancement)
      case Left(error) =>
        IO.raiseError(ParseException(addContext(error, lineNumber, filePath)))
    }
  }

  protected[wp40k] def parseLine(line: String): Either[ParseError, Enhancement] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      factionId <- CsvParsing.parseWith("faction_id", cols(0), FactionId.parse)
      id <- CsvParsing.parseWith("id", cols(1), EnhancementId.parse)
      name <- CsvParsing.parseString("name", cols(2))
      cost <- CsvParsing.parseInt("cost", cols(3))
      detachment = CsvParsing.parseOptString(cols(4))
      detachmentId = CsvParsing.parseOptString(cols(5))
      legend = CsvParsing.parseOptString(cols(6))
      description <- CsvParsing.parseString("description", cols(7))
    } yield Enhancement(factionId, id, name, cost, detachment, detachmentId, legend, description)
  }
}
