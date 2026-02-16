package wahapedia.domain.types

import wahapedia.errors.ParseError
import wahapedia.errors.InvalidFormat
import cats.syntax.either.*
import io.circe.{Encoder, Decoder}
import sttp.tapir.Schema

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

  given Encoder[Save] = Encoder.encodeString.contramap(_.asString)
  given Decoder[Save] = Decoder.decodeString.emap(str => parse(str).leftMap(err => ParseError.formatError(err)))
  given Schema[Save] = Schema.string
}