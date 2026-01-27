package wahapedia.csv

import cats.effect.IO
import fs2.Stream
import fs2.io.file.{Files, Path}
import wahapedia.errors.*

trait StreamingCsvParser[T] {
  def parseStream(path: String): Stream[IO, T] = {
    val filePath = Path(path)
    Files[IO]
      .readAll(filePath)
      .through(fs2.text.utf8.decode)
      .through(fs2.text.lines)
      .drop(1)
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