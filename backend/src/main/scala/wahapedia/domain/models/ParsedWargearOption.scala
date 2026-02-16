package wahapedia.domain.models

import wahapedia.domain.types.DatasheetId
import wahapedia.errors.{ParseError, ParseException, InvalidFormat}
import wahapedia.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO
import cats.syntax.either.*
import io.circe.{Encoder, Decoder}
import sttp.tapir.Schema

enum WargearAction:
  case Remove, Add

object WargearAction:
  def parse(s: String): Either[ParseError, WargearAction] = s.toLowerCase match {
    case "remove" | "replace" => Right(WargearAction.Remove)
    case "add" => Right(WargearAction.Add)
    case _ => Left(InvalidFormat("action", s))
  }

  def asString(a: WargearAction): String = a match {
    case Remove => "remove"
    case Add => "add"
  }

  given Encoder[WargearAction] = Encoder.encodeString.contramap(asString)
  given Decoder[WargearAction] = Decoder.decodeString.emap(s => parse(s).left.map(_.toString))
  given Schema[WargearAction] = Schema.string

case class ParsedWargearOption(
  datasheetId: DatasheetId,
  optionLine: Int,
  choiceIndex: Int,
  groupId: Int,
  action: WargearAction,
  weaponName: String,
  modelTarget: Option[String],
  countPerNModels: Int,
  maxCount: Int
)

object ParsedWargearOptionParser extends StreamingCsvParser[ParsedWargearOption] {

  protected[wahapedia] def parseLineWithContext(
    line: String,
    lineNumber: Int,
    filePath: String
  ): IO[ParsedWargearOption] = {
    parseLine(line) match {
      case Right(option) => IO.pure(option)
      case Left(error) =>
        IO.raiseError(ParseException(addContext(error, lineNumber, filePath)))
    }
  }

  private def parseIntOrZero(value: String): Int =
    if (value.isEmpty) 0 else value.toIntOption.getOrElse(0)

  protected[wahapedia] def parseLine(line: String): Either[ParseError, ParsedWargearOption] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      datasheetId <- CsvParsing.parseWith("datasheet_id", cols(0), DatasheetId.parse)
      optionLine <- CsvParsing.parseInt("option_line", cols(1))
      choiceIndex <- CsvParsing.parseInt("choice_index", cols(2))
      groupId <- CsvParsing.parseInt("group_id", cols(3))
      action <- CsvParsing.parseWith("action", cols(4), WargearAction.parse)
      weaponName <- CsvParsing.parseString("weapon_name", cols(5))
      modelTarget = CsvParsing.parseOptString(cols(6))
      countPerNModels = parseIntOrZero(cols(7))
      maxCount = parseIntOrZero(cols(8))
    } yield ParsedWargearOption(datasheetId, optionLine, choiceIndex, groupId, action, weaponName, modelTarget, countPerNModels, maxCount)
  }
}
