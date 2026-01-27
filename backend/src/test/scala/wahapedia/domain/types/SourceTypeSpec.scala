package wahapedia.domain.types

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import wahapedia.errors.InvalidFormat

class SourceTypeSpec extends AnyFlatSpec with Matchers with EitherValues {

  "SourceType.parse" should "parse Expansion" in {
    SourceType.parse("Expansion").value shouldBe SourceType.Expansion
  }

  it should "parse Rulebook" in {
    SourceType.parse("Rulebook").value shouldBe SourceType.Rulebook
  }

  it should "parse Faction Pack" in {
    SourceType.parse("Faction Pack").value shouldBe SourceType.FactionPack
  }

  it should "parse Codex" in {
    SourceType.parse("Codex").value shouldBe SourceType.Codex
  }

  it should "parse Index" in {
    SourceType.parse("Index").value shouldBe SourceType.Index
  }

  it should "parse Boxset" in {
    SourceType.parse("Boxset").value shouldBe SourceType.Boxset
  }

  it should "parse Datasheet" in {
    SourceType.parse("Datasheet").value shouldBe SourceType.Datasheet
  }

  it should "reject invalid source type" in {
    val result = SourceType.parse("InvalidType")
    result.left.value shouldBe a[InvalidFormat]
    result.left.value.field shouldBe "source_type"
  }

  it should "reject empty string" in {
    val result = SourceType.parse("")
    result.left.value shouldBe a[InvalidFormat]
  }

  "SourceType.asString" should "convert Expansion to string" in {
    SourceType.asString(SourceType.Expansion) shouldBe "Expansion"
  }

  it should "convert Rulebook to string" in {
    SourceType.asString(SourceType.Rulebook) shouldBe "Rulebook"
  }

  it should "convert FactionPack to string with space" in {
    SourceType.asString(SourceType.FactionPack) shouldBe "Faction Pack"
  }

  it should "convert Codex to string" in {
    SourceType.asString(SourceType.Codex) shouldBe "Codex"
  }

  it should "convert Index to string" in {
    SourceType.asString(SourceType.Index) shouldBe "Index"
  }

  it should "convert Boxset to string" in {
    SourceType.asString(SourceType.Boxset) shouldBe "Boxset"
  }

  it should "convert Datasheet to string" in {
    SourceType.asString(SourceType.Datasheet) shouldBe "Datasheet"
  }

  it should "roundtrip all source types" in {
    SourceType.values.foreach { sourceType =>
      val str = SourceType.asString(sourceType)
      SourceType.parse(str).value shouldBe sourceType
    }
  }
}
