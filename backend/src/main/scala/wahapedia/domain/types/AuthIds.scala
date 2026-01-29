package wahapedia.domain.types

import io.circe.{Encoder, Decoder}
import java.util.UUID
import scala.util.Random

opaque type UserId = String
opaque type SessionToken = String
opaque type InviteCode = String

object UserId {
  def apply(id: String): UserId = id
  def value(id: UserId): String = id
  def generate(): UserId = UUID.randomUUID().toString

  given Encoder[UserId] = Encoder.encodeString.contramap(value)
  given Decoder[UserId] = Decoder.decodeString.map(apply)
}

object SessionToken {
  def apply(token: String): SessionToken = token
  def value(token: SessionToken): String = token

  def generate(): SessionToken = {
    val bytes = new Array[Byte](32)
    new java.security.SecureRandom().nextBytes(bytes)
    bytes.map("%02x".format(_)).mkString
  }

  given Encoder[SessionToken] = Encoder.encodeString.contramap(value)
  given Decoder[SessionToken] = Decoder.decodeString.map(apply)
}

object InviteCode {
  def apply(code: String): InviteCode = code
  def value(code: InviteCode): String = code

  def generate(): InviteCode = {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    val random = new java.security.SecureRandom()
    (1 to 16).map(_ => chars.charAt(random.nextInt(chars.length))).mkString
  }

  given Encoder[InviteCode] = Encoder.encodeString.contramap(value)
  given Decoder[InviteCode] = Decoder.decodeString.map(apply)
}
