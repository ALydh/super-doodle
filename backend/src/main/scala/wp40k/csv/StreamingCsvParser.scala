package wp40k.csv

import cats.effect.IO
import fs2.Stream
import fs2.io.file.{Files, Path}
import wp40k.errors.*

trait StreamingCsvParser[T] {

  protected def expectedColumns: Seq[String] = Seq.empty

  def parseStream(path: String): Stream[IO, T] = {
    val filePath = Path(path)
    val lines = Files[IO]
      .readAll(filePath)
      .through(fs2.text.utf8.decode)
      .through(fs2.text.lines)

    val validated = if (expectedColumns.nonEmpty) {
      lines.pull.uncons1.flatMap {
        case Some((headerLine, rest)) =>
          val headers = CsvHeaders.fromLine(headerLine)
          headers.validate(expectedColumns*) match {
            case Left(error) =>
              fs2.Pull.raiseError[IO](ParseException(addContext(error, 1, path)))
            case Right(()) =>
              fs2.Pull.output1(headerLine) >> rest.pull.echo
          }
        case None => fs2.Pull.done
      }.stream.drop(1)
    } else {
      lines.drop(1)
    }

    validated
      .filter(_.trim.nonEmpty)
      .zipWithIndex
      .evalMap { case (line, index) =>
        parseLineWithContext(line, index.toInt + 2, path)
      }
      .handleErrorWith(error => Stream.raiseError(error))
  }
  
  protected def parseLineWithContext(
    line: String, 
    lineNumber: Int,
    filePath: String
  ): IO[T]
  
  protected def parseLine(line: String): Either[ParseError, T]
  
  protected def addContext(error: ParseError, line: Int, file: String): ParseError = {
    error match {
      case InvalidFormat(field, value, _, _, _) => 
        InvalidFormat(field, value, Some(line), Some(file), error.context)
      case MissingField(field, _, _, _) => 
        MissingField(field, Some(line), Some(file), error.context)
      case InvalidId(value, _, _, _) => 
        InvalidId(value, Some(line), Some(file), error.context)
    }
  }
}