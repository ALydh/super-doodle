package wahapedia.domain.models

import wahapedia.domain.types.DatasheetId
import wahapedia.errors.{ParseError, ParseException}
import wahapedia.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO
import cats.syntax.either.*

case class Wargear(
  datasheetId: DatasheetId,
  line: Option[Int],
  lineInWargear: Option[Int],
  dice: Option[String],
  name: Option[String],
  description: Option[String],
  range: Option[String],
  weaponType: Option[String],
  attacks: Option[String],
  ballisticSkill: Option[String],
  strength: Option[String],
  armorPenetration: Option[String],
  damage: Option[String]
)

object WargearParser extends StreamingCsvParser[Wargear] {

  protected[wahapedia] def parseLineWithContext(
    line: String,
    lineNumber: Int,
    filePath: String
  ): IO[Wargear] = {
    parseLine(line) match {
      case Right(wargear) => IO.pure(wargear)
      case Left(error) =>
        IO.raiseError(ParseException(addContext(error, lineNumber, filePath)))
    }
  }

  protected[wahapedia] def parseLine(line: String): Either[ParseError, Wargear] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      datasheetId <- CsvParsing.parseWith("datasheet_id", cols(0), DatasheetId.parse)
      lineNum <- CsvParsing.parseOptInt("line", cols(1))
      lineInWargear <- CsvParsing.parseOptInt("line_in_wargear", cols(2))
      dice = CsvParsing.parseOptString(cols(3))
      name = CsvParsing.parseOptString(cols(4))
      description = CsvParsing.parseOptString(cols(5))
      range = CsvParsing.parseOptString(cols(6))
      weaponType = CsvParsing.parseOptString(cols(7))
      attacks = CsvParsing.parseOptString(cols(8))
      ballisticSkill = CsvParsing.parseOptString(cols(9))
      strength = CsvParsing.parseOptString(cols(10))
      armorPenetration = CsvParsing.parseOptString(cols(11))
      damage = CsvParsing.parseOptString(cols(12))
    } yield Wargear(
      datasheetId, lineNum, lineInWargear, dice, name, description,
      range, weaponType, attacks, ballisticSkill, strength,
      armorPenetration, damage
    )
  }
}
