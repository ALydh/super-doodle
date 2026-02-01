package wahapedia.auth

import cats.effect.{IO, Ref}
import java.time.Instant
import scala.concurrent.duration._

case class RateLimitConfig(
  maxAttempts: Int,
  windowSeconds: Long
)

object RateLimiter {

  private case class Attempts(timestamps: List[Instant])

  def create(config: RateLimitConfig): IO[RateLimiter] =
    Ref.of[IO, Map[String, Attempts]](Map.empty).map(new RateLimiter(_, config))
}

class RateLimiter private (
  state: Ref[IO, Map[String, RateLimiter.Attempts]],
  config: RateLimitConfig
) {
  import RateLimiter.Attempts

  def isAllowed(key: String): IO[Boolean] = {
    val now = Instant.now()
    val windowStart = now.minusSeconds(config.windowSeconds)

    state.modify { attempts =>
      val current = attempts.getOrElse(key, Attempts(Nil))
      val recent = current.timestamps.filter(_.isAfter(windowStart))

      if (recent.size >= config.maxAttempts) {
        (attempts.updated(key, Attempts(recent)), false)
      } else {
        (attempts.updated(key, Attempts(now :: recent)), true)
      }
    }
  }

  def cleanup: IO[Unit] = {
    val now = Instant.now()
    val windowStart = now.minusSeconds(config.windowSeconds)

    state.update { attempts =>
      attempts.view.mapValues { a =>
        Attempts(a.timestamps.filter(_.isAfter(windowStart)))
      }.filter(_._2.timestamps.nonEmpty).toMap
    }
  }
}
