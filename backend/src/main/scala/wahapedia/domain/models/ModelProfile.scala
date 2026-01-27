package wahapedia.domain.models

import wahapedia.domain.types.{DatasheetId, Save}
import wahapedia.errors.{ParseError, ParseException}
import wahapedia.csv.{StreamingCsvParser, CsvParsing}
import cats.effect.IO
import cats.syntax.either.*

case class ModelProfile(
  datasheetId: DatasheetId,
  line: Int,
  name: Option[String],
  movement: String,
  toughness: String,
  save: Save,
  invulnerableSave: Option[String],
  invulnerableSaveDescription: Option[String],
  wounds: Int,
  leadership: String,
  objectiveControl: Int,
  baseSize: Option[String],
  baseSizeDescription: Option[String]
)

object ModelProfileParser extends StreamingCsvParser[ModelProfile] {

  protected[wahapedia] def parseLineWithContext(
    line: String,
    lineNumber: Int,
    filePath: String
  ): IO[ModelProfile] = {
    parseLine(line) match {
      case Right(model) => IO.pure(model)
      case Left(error) =>
        IO.raiseError(ParseException(addContext(error, lineNumber, filePath)))
    }
  }

  protected[wahapedia] def parseLine(line: String): Either[ParseError, ModelProfile] = {
    val cols = CsvParsing.splitCsvLine(line)

    for {
      datasheetId <- CsvParsing.parseWith("datasheet_id", cols(0), DatasheetId.parse)
      lineNum <- CsvParsing.parseInt("line", cols(1))
      name = CsvParsing.parseOptString(cols(2))
      movement <- CsvParsing.parseString("movement", cols(3))
      toughness <- CsvParsing.parseString("toughness", cols(4))
      save <- CsvParsing.parseWith("save", cols(5), Save.parse)
      invulnerableSave = CsvParsing.parseOptString(cols(6))
      invulnerableSaveDescription = CsvParsing.parseOptString(cols(7))
      wounds <- CsvParsing.parseInt("wounds", cols(8))
      leadership <- CsvParsing.parseString("leadership", cols(9))
      objectiveControl <- CsvParsing.parseInt("objective_control", cols(10))
      baseSize = CsvParsing.parseOptString(cols(11))
      baseSizeDescription = CsvParsing.parseOptString(cols(12))
    } yield ModelProfile(
      datasheetId, lineNum, name, movement, toughness, save,
      invulnerableSave, invulnerableSaveDescription,
      wounds, leadership, objectiveControl, baseSize, baseSizeDescription
    )
  }
}
