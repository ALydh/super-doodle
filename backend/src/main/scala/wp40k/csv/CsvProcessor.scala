package wp40k.csv

import cats.effect.IO

object CsvProcessor {
  def failFastParse[T](
    path: String,
    parser: StreamingCsvParser[T]
  ): IO[List[T]] =
    parser.parseStream(path).compile.toList
}