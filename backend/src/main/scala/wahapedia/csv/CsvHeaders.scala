package wahapedia.csv

import wahapedia.errors.{ParseError, MissingField}

case class CsvHeaders(headers: Array[String]) {

  private val indexMap: Map[String, Int] = headers.zipWithIndex.toMap

  def indexOf(columnName: String): Either[ParseError, Int] =
    indexMap.get(columnName).toRight(MissingField(columnName))

  def getColumn(columns: Array[String], columnName: String): Either[ParseError, String] =
    indexOf(columnName).map(columns(_))

  def validate(expected: String*): Either[ParseError, Unit] =
    expected.find(!indexMap.contains(_)) match {
      case Some(missing) => Left(MissingField(missing))
      case None => Right(())
    }
}

object CsvHeaders {

  private val UTF8_BOM = "\uFEFF"

  def fromLine(line: String): CsvHeaders = {
    val stripped = if (line.startsWith(UTF8_BOM)) line.substring(1) else line
    CsvHeaders(CsvParsing.splitCsvLine(stripped).toArray)
  }
}
