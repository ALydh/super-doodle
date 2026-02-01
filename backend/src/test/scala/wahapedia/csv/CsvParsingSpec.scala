package wahapedia.csv

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import wahapedia.errors.{InvalidFormat, MissingField}

class CsvParsingSpec extends AnyFlatSpec with Matchers with EitherValues {

  "splitCsvLine" should "split pipe-delimited values" in {
    val result = CsvParsing.splitCsvLine("a|b|c")
    result.toArray shouldBe Array("a", "b", "c")
  }

  it should "preserve empty fields" in {
    val result = CsvParsing.splitCsvLine("a||c")
    result.toArray shouldBe Array("a", "", "c")
  }

  it should "handle leading empty fields" in {
    val result = CsvParsing.splitCsvLine("|b|c")
    result.toArray shouldBe Array("", "b", "c")
  }

  it should "handle trailing empty fields" in {
    val result = CsvParsing.splitCsvLine("a|b|")
    result.toArray shouldBe Array("a", "b", "")
  }

  it should "handle all empty fields" in {
    val result = CsvParsing.splitCsvLine("||")
    result.toArray shouldBe Array("", "", "")
  }

  it should "return empty string for out-of-bounds access" in {
    val result = CsvParsing.splitCsvLine("a|b")
    result(0) shouldBe "a"
    result(1) shouldBe "b"
    result(2) shouldBe ""
    result(100) shouldBe ""
    result(-1) shouldBe ""
  }

  "parseInt" should "parse valid integers" in {
    val result = CsvParsing.parseInt("count", "42")
    result.value shouldBe 42
  }

  it should "return MissingField for empty string" in {
    val result = CsvParsing.parseInt("count", "")
    result.left.value shouldBe a[MissingField]
    result.left.value.field shouldBe "count"
  }

  it should "return InvalidFormat for non-numeric string" in {
    val result = CsvParsing.parseInt("count", "abc")
    result.left.value shouldBe a[InvalidFormat]
    result.left.value.field shouldBe "count"
    result.left.value.value shouldBe "abc"
  }

  it should "return InvalidFormat for decimal values" in {
    val result = CsvParsing.parseInt("count", "3.14")
    result.left.value shouldBe a[InvalidFormat]
  }

  "parseLong" should "parse valid longs" in {
    val result = CsvParsing.parseLong("id", "9223372036854775807")
    result.value shouldBe 9223372036854775807L
  }

  it should "return MissingField for empty string" in {
    val result = CsvParsing.parseLong("id", "")
    result.left.value shouldBe a[MissingField]
    result.left.value.field shouldBe "id"
  }

  it should "return InvalidFormat for non-numeric string" in {
    val result = CsvParsing.parseLong("id", "not-a-number")
    result.left.value shouldBe a[InvalidFormat]
  }

  "parseBoolean" should "parse 'true' and 'false'" in {
    CsvParsing.parseBoolean("flag", "true").value shouldBe true
    CsvParsing.parseBoolean("flag", "false").value shouldBe false
  }

  it should "parse 't' and 'f'" in {
    CsvParsing.parseBoolean("flag", "t").value shouldBe true
    CsvParsing.parseBoolean("flag", "f").value shouldBe false
  }

  it should "parse '1' and '0'" in {
    CsvParsing.parseBoolean("flag", "1").value shouldBe true
    CsvParsing.parseBoolean("flag", "0").value shouldBe false
  }

  it should "be case-insensitive" in {
    CsvParsing.parseBoolean("flag", "TRUE").value shouldBe true
    CsvParsing.parseBoolean("flag", "False").value shouldBe false
    CsvParsing.parseBoolean("flag", "T").value shouldBe true
    CsvParsing.parseBoolean("flag", "F").value shouldBe false
  }

  it should "return MissingField for empty string" in {
    val result = CsvParsing.parseBoolean("flag", "")
    result.left.value shouldBe a[MissingField]
  }

  it should "return InvalidFormat for invalid values" in {
    val result = CsvParsing.parseBoolean("flag", "yes")
    result.left.value shouldBe a[InvalidFormat]
    result.left.value.field shouldBe "flag"
    result.left.value.value shouldBe "yes"
  }

  "parseString" should "return non-empty strings" in {
    val result = CsvParsing.parseString("name", "test")
    result.value shouldBe "test"
  }

  it should "return MissingField for empty string" in {
    val result = CsvParsing.parseString("name", "")
    result.left.value shouldBe a[MissingField]
    result.left.value.field shouldBe "name"
  }

  it should "preserve whitespace" in {
    val result = CsvParsing.parseString("name", "  spaces  ")
    result.value shouldBe "  spaces  "
  }

  "parseOptInt" should "parse valid integers as Some" in {
    val result = CsvParsing.parseOptInt("count", "42")
    result.value shouldBe Some(42)
  }

  it should "return None for empty string" in {
    val result = CsvParsing.parseOptInt("count", "")
    result.value shouldBe None
  }

  it should "return InvalidFormat for non-numeric string" in {
    val result = CsvParsing.parseOptInt("count", "abc")
    result.left.value shouldBe a[InvalidFormat]
  }

  "parseOptLong" should "parse valid longs as Some" in {
    val result = CsvParsing.parseOptLong("id", "9223372036854775807")
    result.value shouldBe Some(9223372036854775807L)
  }

  it should "return None for empty string" in {
    val result = CsvParsing.parseOptLong("id", "")
    result.value shouldBe None
  }

  it should "return InvalidFormat for non-numeric string" in {
    val result = CsvParsing.parseOptLong("id", "not-a-number")
    result.left.value shouldBe a[InvalidFormat]
  }

  "parseOptString" should "return Some for non-empty strings" in {
    CsvParsing.parseOptString("test") shouldBe Some("test")
  }

  it should "return None for empty string" in {
    CsvParsing.parseOptString("") shouldBe None
  }

  it should "preserve whitespace in non-empty strings" in {
    CsvParsing.parseOptString("  spaces  ") shouldBe Some("  spaces  ")
  }

  "parseWith" should "use custom parser for valid values" in {
    def customParser(s: String): Either[InvalidFormat, Int] =
      s.toIntOption.toRight(InvalidFormat("custom", s))

    val result = CsvParsing.parseWith("field", "42", customParser)
    result.value shouldBe 42
  }

  it should "return MissingField for empty string" in {
    def customParser(s: String): Either[InvalidFormat, Int] =
      s.toIntOption.toRight(InvalidFormat("custom", s))

    val result = CsvParsing.parseWith("field", "", customParser)
    result.left.value shouldBe a[MissingField]
  }

  it should "propagate parser errors" in {
    def customParser(s: String): Either[InvalidFormat, Int] =
      Left(InvalidFormat("custom", s))

    val result = CsvParsing.parseWith("field", "abc", customParser)
    result.left.value shouldBe a[InvalidFormat]
  }

  "parseOptWith" should "use custom parser for valid values" in {
    def customParser(s: String): Either[InvalidFormat, Int] =
      s.toIntOption.toRight(InvalidFormat("custom", s))

    val result = CsvParsing.parseOptWith("field", "42", customParser)
    result.value shouldBe Some(42)
  }

  it should "return None for empty string" in {
    def customParser(s: String): Either[InvalidFormat, Int] =
      s.toIntOption.toRight(InvalidFormat("custom", s))

    val result = CsvParsing.parseOptWith("field", "", customParser)
    result.value shouldBe None
  }

  it should "propagate parser errors" in {
    def customParser(s: String): Either[InvalidFormat, Int] =
      Left(InvalidFormat("custom", s))

    val result = CsvParsing.parseOptWith("field", "abc", customParser)
    result.left.value shouldBe a[InvalidFormat]
  }
}
