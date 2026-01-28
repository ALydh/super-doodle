package wahapedia.domain.types

import wahapedia.errors.{ParseError, InvalidFormat}
import io.circe.{Encoder, Decoder}
import cats.syntax.either.*

enum Role:
  case Battleline
  case Characters
  case DedicatedTransports
  case Fortifications
  case Other

object Role {
  def parse(s: String): Either[ParseError, Role] = s match {
    case "Battleline" => Right(Role.Battleline)
    case "Characters" => Right(Role.Characters)
    case "Dedicated Transports" => Right(Role.DedicatedTransports)
    case "Fortifications" => Right(Role.Fortifications)
    case "Other" => Right(Role.Other)
    case _ => Left(InvalidFormat("role", s))
  }

  def asString(role: Role): String = role match {
    case Role.Battleline => "Battleline"
    case Role.Characters => "Characters"
    case Role.DedicatedTransports => "Dedicated Transports"
    case Role.Fortifications => "Fortifications"
    case Role.Other => "Other"
  }

  given Encoder[Role] = Encoder.encodeString.contramap(asString)
  given Decoder[Role] = Decoder.decodeString.emap(str => parse(str).leftMap(err => ParseError.formatError(err)))
}
