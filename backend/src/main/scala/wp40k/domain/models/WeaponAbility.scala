package wp40k.domain.models

import wp40k.errors.{ParseError, ParseException}
import wp40k.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO
import cats.syntax.either.*

case class WeaponAbility(
  id: String,
  name: String,
  description: String
)

object WeaponAbilityParser extends StreamingCsvParser[WeaponAbility] {

  protected[wp40k] def parseLineWithContext(
    line: String,
    lineNumber: Int,
    filePath: String
  ): IO[WeaponAbility] = {
    parseLine(line) match {
      case Right(ability) => IO.pure(ability)
      case Left(error) =>
        IO.raiseError(ParseException(addContext(error, lineNumber, filePath)))
    }
  }

  protected[wp40k] def parseLine(line: String): Either[ParseError, WeaponAbility] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      id <- CsvParsing.parseString("id", cols(0))
      name <- CsvParsing.parseString("name", cols(1))
      description <- CsvParsing.parseString("description", cols(2))
    } yield WeaponAbility(id, name, description)
  }
}
