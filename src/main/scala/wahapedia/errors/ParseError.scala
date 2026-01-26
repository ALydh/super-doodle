package wahapedia.errors

sealed trait ParseError {
  def field: String
  def value: String
  def line: Option[Int]
  def file: Option[String]
  def context: String
}

case class InvalidFormat(
  field: String, 
  value: String, 
  line: Option[Int] = None,
  file: Option[String] = None,
  context: String = ""
) extends ParseError

case class MissingField(
  field: String,
  line: Option[Int] = None,
  file: Option[String] = None,
  context: String = ""
) extends ParseError {
  def value: String = ""
}

case class InvalidId(
  value: String,
  line: Option[Int] = None,
  file: Option[String] = None,
  context: String = ""
) extends ParseError {
  def field: String = "id"
}

case class ParseException(error: ParseError) 
  extends RuntimeException(ParseError.formatError(error))

object ParseError {
  def formatError(error: ParseError): String = {
    val location = error.line.fold("")(l => s" at line $l")
    val file = error.file.fold("")(f => s" in $f")
    val context = if (error.context.nonEmpty) s"\nContext: ${error.context}" else ""
    s"Parse error in ${error.field}: '${error.value}'$location$file$context"
  }
}