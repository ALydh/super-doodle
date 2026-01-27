package wahapedia.domain.types

import wahapedia.errors.ParseError
import wahapedia.errors.InvalidFormat
import cats.syntax.either.*

opaque type Save = Int

object Save {
  def apply(modifier: Int): Save = modifier
  def value(save: Save): Int = save
  
  def parse(s: String): Either[ParseError, Save] = {
    if (s.endsWith("+")) {
      scala.util.Try(s.dropRight(1).toInt)
        .map(Save(_))
        .toEither
        .leftMap(_ => InvalidFormat("save", s))
    } else Left(InvalidFormat("save", s))
  }
  
  extension (s: Save) {
    def asString: String = s"${s}+"
  }
}