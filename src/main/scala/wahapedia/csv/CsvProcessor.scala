package wahapedia.csv

import cats.effect.IO
import fs2.Stream
import wahapedia.errors.*

object CsvProcessor {
  def failFastParse[T](
    path: String,
    parser: StreamingCsvParser[T]
  ): IO[List[T]] = {
    parser.parseStream(path)
      .compile
      .toList
      .handleErrorWith {
        case ParseException(error) => IO.raiseError(ParseException(error))
        case other => IO.raiseError(other)
      }
  }
}