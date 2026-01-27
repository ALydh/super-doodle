package wahapedia.domain.types

import wahapedia.errors.ParseError
import wahapedia.errors.InvalidFormat
import cats.syntax.either.*

opaque type ArmorPenetration = Int

object ArmorPenetration {
  def apply(value: Int): ArmorPenetration = value
  def value(ap: ArmorPenetration): Int = ap
  
  def parse(s: String): Either[ParseError, ArmorPenetration] = {
    scala.util.Try(s.replaceAll("[+-]", "").toInt)
      .map(ArmorPenetration(_))
      .toEither
      .leftMap(_ => InvalidFormat("armor_penetration", s))
  }
  
  extension (ap: ArmorPenetration) {
    def asString: String = if (ap >= 0) s"+$ap" else ap.toString
  }
}