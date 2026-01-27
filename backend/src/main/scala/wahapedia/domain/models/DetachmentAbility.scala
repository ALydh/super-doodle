package wahapedia.domain.models

import wahapedia.domain.types.FactionId
import wahapedia.errors.{ParseError, ParseException}
import wahapedia.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO
import cats.syntax.either.*

opaque type DetachmentAbilityId = String

object DetachmentAbilityId {
  def apply(id: String): DetachmentAbilityId = id
  def value(id: DetachmentAbilityId): String = id

  def parse(id: String): Either[ParseError, DetachmentAbilityId] = {
    if (id.nonEmpty) Right(DetachmentAbilityId(id))
    else Left(wahapedia.errors.InvalidId(id))
  }
}

case class DetachmentAbility(
  id: DetachmentAbilityId,
  factionId: FactionId,
  name: String,
  legend: Option[String],
  description: String,
  detachment: String,
  detachmentId: String
)

object DetachmentAbilityParser extends StreamingCsvParser[DetachmentAbility] {

  protected[wahapedia] def parseLineWithContext(
    line: String,
    lineNumber: Int,
    filePath: String
  ): IO[DetachmentAbility] = {
    parseLine(line) match {
      case Right(ability) => IO.pure(ability)
      case Left(error) =>
        IO.raiseError(ParseException(addContext(error, lineNumber, filePath)))
    }
  }

  protected[wahapedia] def parseLine(line: String): Either[ParseError, DetachmentAbility] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      id <- CsvParsing.parseWith("id", cols(0), DetachmentAbilityId.parse)
      factionId <- CsvParsing.parseWith("faction_id", cols(1), FactionId.parse)
      name <- CsvParsing.parseString("name", cols(2))
      legend = CsvParsing.parseOptString(cols(3))
      description <- CsvParsing.parseString("description", cols(4))
      detachment <- CsvParsing.parseString("detachment", cols(5))
      detachmentId <- CsvParsing.parseString("detachment_id", cols(6))
    } yield DetachmentAbility(id, factionId, name, legend, description, detachment, detachmentId)
  }
}
