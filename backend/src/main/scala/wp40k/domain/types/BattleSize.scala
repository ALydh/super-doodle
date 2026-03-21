package wp40k.domain.types

import io.circe.{Encoder, Decoder}
import sttp.tapir.Schema

enum BattleSize(val maxPoints: Int):
  case Incursion extends BattleSize(1000)
  case StrikeForce extends BattleSize(2000)
  case Onslaught extends BattleSize(3000)

object BattleSize {
  def parse(s: String): Either[String, BattleSize] = s.toLowerCase match {
    case "incursion" => Right(BattleSize.Incursion)
    case "strikeforce" | "strike force" => Right(BattleSize.StrikeForce)
    case "onslaught" => Right(BattleSize.Onslaught)
    case _ => Left(s"Invalid battleSize '$s'. Valid values: Incursion, StrikeForce, Onslaught")
  }

  given Encoder[BattleSize] = Encoder.encodeString.contramap(_.toString)
  given Decoder[BattleSize] = Decoder.decodeString.emap(parse)
  given Schema[BattleSize] = Schema.string
}
