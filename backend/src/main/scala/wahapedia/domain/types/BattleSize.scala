package wahapedia.domain.types

import io.circe.{Encoder, Decoder}
import sttp.tapir.Schema

enum BattleSize(val maxPoints: Int):
  case Incursion extends BattleSize(1000)
  case StrikeForce extends BattleSize(2000)
  case Onslaught extends BattleSize(3000)

object BattleSize {
  def parse(s: String): Either[String, BattleSize] = s match {
    case "Incursion" => Right(BattleSize.Incursion)
    case "StrikeForce" => Right(BattleSize.StrikeForce)
    case "Onslaught" => Right(BattleSize.Onslaught)
    case _ => Left(s"Invalid battle size: $s")
  }

  given Encoder[BattleSize] = Encoder.encodeString.contramap(_.toString)
  given Decoder[BattleSize] = Decoder.decodeString.emap(parse)
  given Schema[BattleSize] = Schema.string
}
