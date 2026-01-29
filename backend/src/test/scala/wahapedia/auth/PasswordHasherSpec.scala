package wahapedia.auth

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import cats.effect.unsafe.implicits.global

class PasswordHasherSpec extends AnyFlatSpec with Matchers {

  "hash" should "produce a bcrypt hash" in {
    val hash = PasswordHasher.hash("mypassword").unsafeRunSync()
    hash should startWith("$2a$")
    hash.length should be > 50
  }

  it should "produce different hashes for the same password" in {
    val hash1 = PasswordHasher.hash("password").unsafeRunSync()
    val hash2 = PasswordHasher.hash("password").unsafeRunSync()
    hash1 should not be hash2
  }

  "verify" should "return true for correct password" in {
    val hash = PasswordHasher.hash("secret123").unsafeRunSync()
    PasswordHasher.verify("secret123", hash).unsafeRunSync() shouldBe true
  }

  it should "return false for incorrect password" in {
    val hash = PasswordHasher.hash("correct").unsafeRunSync()
    PasswordHasher.verify("wrong", hash).unsafeRunSync() shouldBe false
  }

  it should "handle empty passwords" in {
    val hash = PasswordHasher.hash("").unsafeRunSync()
    PasswordHasher.verify("", hash).unsafeRunSync() shouldBe true
    PasswordHasher.verify("notempty", hash).unsafeRunSync() shouldBe false
  }

  it should "handle special characters" in {
    val password = "p@ssw0rd!#$%^&*()"
    val hash = PasswordHasher.hash(password).unsafeRunSync()
    PasswordHasher.verify(password, hash).unsafeRunSync() shouldBe true
  }

  it should "handle unicode characters" in {
    val password = "пароль密码🔐"
    val hash = PasswordHasher.hash(password).unsafeRunSync()
    PasswordHasher.verify(password, hash).unsafeRunSync() shouldBe true
  }
}
