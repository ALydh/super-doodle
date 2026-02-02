package wahapedia.http

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class InputValidationSpec extends AnyFlatSpec with Matchers {

  "validateUsername" should "accept valid usernames" in {
    InputValidation.validateUsername("john_doe") shouldBe Right("john_doe")
    InputValidation.validateUsername("user123") shouldBe Right("user123")
    InputValidation.validateUsername("test-user") shouldBe Right("test-user")
  }

  it should "trim whitespace" in {
    InputValidation.validateUsername("  john_doe  ") shouldBe Right("john_doe")
  }

  it should "reject empty username" in {
    InputValidation.validateUsername("") shouldBe Left(InputValidation.EmptyUsername)
    InputValidation.validateUsername("   ") shouldBe Left(InputValidation.EmptyUsername)
  }

  it should "reject username shorter than 3 characters" in {
    InputValidation.validateUsername("ab") shouldBe Left(InputValidation.UsernameTooShort)
  }

  it should "reject username longer than 50 characters" in {
    val longUsername = "a" * 51
    InputValidation.validateUsername(longUsername) shouldBe Left(InputValidation.UsernameTooLong)
  }

  it should "reject usernames with invalid characters" in {
    InputValidation.validateUsername("user@name") shouldBe Left(InputValidation.InvalidUsernameChars)
    InputValidation.validateUsername("user name") shouldBe Left(InputValidation.InvalidUsernameChars)
    InputValidation.validateUsername("user.name") shouldBe Left(InputValidation.InvalidUsernameChars)
  }

  "validatePassword" should "accept valid passwords" in {
    InputValidation.validatePassword("secret123") shouldBe Right("secret123")
    InputValidation.validatePassword("password") shouldBe Right("password")
  }

  it should "reject empty password" in {
    InputValidation.validatePassword("") shouldBe Left(InputValidation.EmptyPassword)
  }

  it should "reject password shorter than 6 characters" in {
    InputValidation.validatePassword("short") shouldBe Left(InputValidation.PasswordTooShort)
  }

  "validateArmyName" should "accept valid army names" in {
    InputValidation.validateArmyName("My Army") shouldBe Right("My Army")
    InputValidation.validateArmyName("Death Guard 2000pts") shouldBe Right("Death Guard 2000pts")
  }

  it should "trim whitespace" in {
    InputValidation.validateArmyName("  My Army  ") shouldBe Right("My Army")
  }

  it should "reject empty army name" in {
    InputValidation.validateArmyName("") shouldBe Left(InputValidation.EmptyArmyName)
    InputValidation.validateArmyName("   ") shouldBe Left(InputValidation.EmptyArmyName)
  }

  it should "reject army name longer than 100 characters" in {
    val longName = "a" * 101
    InputValidation.validateArmyName(longName) shouldBe Left(InputValidation.ArmyNameTooLong)
  }
}
