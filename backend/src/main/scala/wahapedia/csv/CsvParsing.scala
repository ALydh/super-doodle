package wahapedia.csv

import wahapedia.errors.{ParseError, InvalidFormat, MissingField}
import scala.util.Try

class SafeColumns(cols: Array[String]) {
  def apply(index: Int): String =
    if (index >= 0 && index < cols.length) cols(index) else ""

  def length: Int = cols.length

  def toArray: Array[String] = cols
}

object CsvParsing {

  def splitCsvLine(line: String): SafeColumns =
    new SafeColumns(line.split("\\|", -1))

  def parseInt(field: String, value: String): Either[ParseError, Int] =
    if (value.isEmpty) Left(MissingField(field))
    else value.toIntOption.toRight(InvalidFormat(field, value))

  def parseLong(field: String, value: String): Either[ParseError, Long] =
    if (value.isEmpty) Left(MissingField(field))
    else value.toLongOption.toRight(InvalidFormat(field, value))

  def parseBoolean(field: String, value: String): Either[ParseError, Boolean] =
    if (value.isEmpty) Left(MissingField(field))
    else value.toLowerCase match {
      case "true" | "t" | "1" => Right(true)
      case "false" | "f" | "0" => Right(false)
      case _ => Left(InvalidFormat(field, value))
    }

  def parseString(field: String, value: String): Either[ParseError, String] =
    if (value.isEmpty) Left(MissingField(field))
    else Right(value)

  def parseOptInt(field: String, value: String): Either[ParseError, Option[Int]] =
    if (value.isEmpty) Right(None)
    else value.toIntOption.map(Some(_)).toRight(InvalidFormat(field, value))

  def parseOptLong(field: String, value: String): Either[ParseError, Option[Long]] =
    if (value.isEmpty) Right(None)
    else value.toLongOption.map(Some(_)).toRight(InvalidFormat(field, value))

  def parseOptString(value: String): Option[String] =
    if (value.isEmpty) None else Some(value)

  def parseWith[T](
    field: String,
    value: String,
    parser: String => Either[ParseError, T]
  ): Either[ParseError, T] =
    if (value.isEmpty) Left(MissingField(field))
    else parser(value)

  def parseOptWith[T](
    field: String,
    value: String,
    parser: String => Either[ParseError, T]
  ): Either[ParseError, Option[T]] =
    if (value.isEmpty) Right(None)
    else parser(value).map(Some(_))
}
