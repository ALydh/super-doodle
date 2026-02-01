package wahapedia.auth

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import cats.effect.unsafe.implicits.global

class RateLimiterSpec extends AnyFlatSpec with Matchers {

  "isAllowed" should "allow requests within limit" in {
    val limiter = RateLimiter.create(RateLimitConfig(maxAttempts = 3, windowSeconds = 60)).unsafeRunSync()

    limiter.isAllowed("user1").unsafeRunSync() shouldBe true
    limiter.isAllowed("user1").unsafeRunSync() shouldBe true
    limiter.isAllowed("user1").unsafeRunSync() shouldBe true
  }

  it should "block requests after limit exceeded" in {
    val limiter = RateLimiter.create(RateLimitConfig(maxAttempts = 2, windowSeconds = 60)).unsafeRunSync()

    limiter.isAllowed("user1").unsafeRunSync() shouldBe true
    limiter.isAllowed("user1").unsafeRunSync() shouldBe true
    limiter.isAllowed("user1").unsafeRunSync() shouldBe false
    limiter.isAllowed("user1").unsafeRunSync() shouldBe false
  }

  it should "track different keys independently" in {
    val limiter = RateLimiter.create(RateLimitConfig(maxAttempts = 1, windowSeconds = 60)).unsafeRunSync()

    limiter.isAllowed("user1").unsafeRunSync() shouldBe true
    limiter.isAllowed("user1").unsafeRunSync() shouldBe false
    limiter.isAllowed("user2").unsafeRunSync() shouldBe true
    limiter.isAllowed("user2").unsafeRunSync() shouldBe false
  }
}
