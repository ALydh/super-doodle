package wahapedia.domain.types

import wahapedia.errors.ParseError
import wahapedia.errors.InvalidId
import io.circe.{Encoder, Decoder}
import cats.syntax.either.*

opaque type DatasheetId = String
opaque type FactionId = String
opaque type AbilityId = String
opaque type SourceId = String
opaque type DetachmentId = String

object DatasheetId {
  def apply(id: String): DatasheetId = id
  def value(id: DatasheetId): String = id

  def parse(id: String): Either[ParseError, DatasheetId] = {
    if (id.matches("\\d{9}")) Right(DatasheetId(id))
    else Left(InvalidId(id))
  }

  given Encoder[DatasheetId] = Encoder.encodeString.contramap(value)
  given Decoder[DatasheetId] = Decoder.decodeString.emap(str => parse(str).leftMap(err => ParseError.formatError(err)))
}

object FactionId {
  def apply(id: String): FactionId = id
  def value(id: FactionId): String = id

  def parse(id: String): Either[ParseError, FactionId] = {
    if (id.nonEmpty && id.matches("[A-Za-z]{2,3}")) Right(FactionId(id))
    else Left(InvalidId(id))
  }

  given Encoder[FactionId] = Encoder.encodeString.contramap(value)
  given Decoder[FactionId] = Decoder.decodeString.emap(str => parse(str).leftMap(err => ParseError.formatError(err)))
}

object AbilityId {
  def apply(id: String): AbilityId = id
  def value(id: AbilityId): String = id

  def parse(id: String): Either[ParseError, AbilityId] = {
    if (id.matches("\\d{9}")) Right(AbilityId(id))
    else Left(InvalidId(id))
  }

  given Encoder[AbilityId] = Encoder.encodeString.contramap(value)
}

object SourceId {
  def apply(id: String): SourceId = id
  def value(id: SourceId): String = id

  def parse(id: String): Either[ParseError, SourceId] = {
    if (id.matches("\\d{9}")) Right(SourceId(id))
    else Left(InvalidId(id))
  }

  given Encoder[SourceId] = Encoder.encodeString.contramap(value)
  given Decoder[SourceId] = Decoder.decodeString.emap(str => parse(str).leftMap(err => ParseError.formatError(err)))
}

object DetachmentId {
  def apply(id: String): DetachmentId = id
  def value(id: DetachmentId): String = id

  def parse(id: String): Either[ParseError, DetachmentId] = {
    if (id.nonEmpty) Right(DetachmentId(id))
    else Left(InvalidId(id))
  }

  given Encoder[DetachmentId] = Encoder.encodeString.contramap(value)
  given Decoder[DetachmentId] = Decoder.decodeString.emap(str => parse(str).leftMap(err => ParseError.formatError(err)))
}