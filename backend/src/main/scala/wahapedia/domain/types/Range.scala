package wahapedia.domain.types

import wahapedia.errors.ParseError
import wahapedia.errors.InvalidFormat
import cats.syntax.either.*

opaque type Range = Int

object Range {
  def apply(inches: Int): Range = inches
  def value(range: Range): Int = range
  
  def parse(s: String): Either[ParseError, Range] = {
    if (s.endsWith("\"")) {
      scala.util.Try(s.dropRight(1).toInt)
        .map(Range(_))
        .toEither
        .leftMap(_ => InvalidFormat("range", s))
    } else Left(InvalidFormat("range", s))
  }
  
  extension (r: Range) {
    def asString: String = s"${r}\""
  }
}