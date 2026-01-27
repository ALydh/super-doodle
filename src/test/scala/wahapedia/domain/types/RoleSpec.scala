package wahapedia.domain.types

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import wahapedia.errors.InvalidFormat

class RoleSpec extends AnyFlatSpec with Matchers with EitherValues {

  "Role.parse" should "parse Battleline" in {
    Role.parse("Battleline").value shouldBe Role.Battleline
  }

  it should "parse Characters" in {
    Role.parse("Characters").value shouldBe Role.Characters
  }

  it should "parse Dedicated Transports" in {
    Role.parse("Dedicated Transports").value shouldBe Role.DedicatedTransports
  }

  it should "parse Fortifications" in {
    Role.parse("Fortifications").value shouldBe Role.Fortifications
  }

  it should "parse Other" in {
    Role.parse("Other").value shouldBe Role.Other
  }

  it should "reject invalid role" in {
    val result = Role.parse("InvalidRole")
    result.left.value shouldBe a[InvalidFormat]
    result.left.value.field shouldBe "role"
  }

  it should "reject empty string" in {
    val result = Role.parse("")
    result.left.value shouldBe a[InvalidFormat]
  }

  "Role.asString" should "convert Battleline to string" in {
    Role.asString(Role.Battleline) shouldBe "Battleline"
  }

  it should "convert Characters to string" in {
    Role.asString(Role.Characters) shouldBe "Characters"
  }

  it should "convert DedicatedTransports to string with space" in {
    Role.asString(Role.DedicatedTransports) shouldBe "Dedicated Transports"
  }

  it should "convert Fortifications to string" in {
    Role.asString(Role.Fortifications) shouldBe "Fortifications"
  }

  it should "convert Other to string" in {
    Role.asString(Role.Other) shouldBe "Other"
  }

  it should "roundtrip all roles" in {
    Role.values.foreach { role =>
      val str = Role.asString(role)
      Role.parse(str).value shouldBe role
    }
  }
}
