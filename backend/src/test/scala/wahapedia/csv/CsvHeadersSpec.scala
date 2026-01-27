package wahapedia.csv

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import wahapedia.errors.MissingField

class CsvHeadersSpec extends AnyFlatSpec with Matchers with EitherValues {

  "fromLine" should "parse header line with pipe delimiter" in {
    val headers = CsvHeaders.fromLine("id|name|description")
    headers.headers shouldBe Array("id", "name", "description")
  }

  it should "strip UTF-8 BOM from first column" in {
    val bom = "\uFEFF"
    val headers = CsvHeaders.fromLine(s"${bom}id|name|description")
    headers.headers shouldBe Array("id", "name", "description")
  }

  it should "handle headers with empty fields" in {
    val headers = CsvHeaders.fromLine("id||description")
    headers.headers shouldBe Array("id", "", "description")
  }

  "indexOf" should "return correct index for existing column" in {
    val headers = CsvHeaders.fromLine("id|name|description")
    headers.indexOf("name").value shouldBe 1
  }

  it should "return index 0 for first column" in {
    val headers = CsvHeaders.fromLine("id|name|description")
    headers.indexOf("id").value shouldBe 0
  }

  it should "return correct index for last column" in {
    val headers = CsvHeaders.fromLine("id|name|description")
    headers.indexOf("description").value shouldBe 2
  }

  it should "return MissingField for non-existent column" in {
    val headers = CsvHeaders.fromLine("id|name|description")
    val result = headers.indexOf("missing")
    result.left.value shouldBe a[MissingField]
    result.left.value.field shouldBe "missing"
  }

  "getColumn" should "return value for existing column" in {
    val headers = CsvHeaders.fromLine("id|name|description")
    val columns = Array("1", "Test", "Description text")
    headers.getColumn(columns, "name").value shouldBe "Test"
  }

  it should "return value for first column" in {
    val headers = CsvHeaders.fromLine("id|name|description")
    val columns = Array("1", "Test", "Description text")
    headers.getColumn(columns, "id").value shouldBe "1"
  }

  it should "return value for last column" in {
    val headers = CsvHeaders.fromLine("id|name|description")
    val columns = Array("1", "Test", "Description text")
    headers.getColumn(columns, "description").value shouldBe "Description text"
  }

  it should "return empty string for empty field" in {
    val headers = CsvHeaders.fromLine("id|name|description")
    val columns = Array("1", "", "Description text")
    headers.getColumn(columns, "name").value shouldBe ""
  }

  it should "return MissingField for non-existent column" in {
    val headers = CsvHeaders.fromLine("id|name|description")
    val columns = Array("1", "Test", "Description text")
    val result = headers.getColumn(columns, "missing")
    result.left.value shouldBe a[MissingField]
  }

  "validate" should "succeed when all expected headers exist" in {
    val headers = CsvHeaders.fromLine("id|name|description|link")
    headers.validate("id", "name", "description").value shouldBe ()
  }

  it should "succeed when expected headers match exactly" in {
    val headers = CsvHeaders.fromLine("id|name|description")
    headers.validate("id", "name", "description").value shouldBe ()
  }

  it should "succeed with single expected header" in {
    val headers = CsvHeaders.fromLine("id|name|description")
    headers.validate("id").value shouldBe ()
  }

  it should "fail when expected header is missing" in {
    val headers = CsvHeaders.fromLine("id|name|description")
    val result = headers.validate("id", "name", "missing")
    result.left.value shouldBe a[MissingField]
    result.left.value.field shouldBe "missing"
  }

  it should "report first missing header when multiple are missing" in {
    val headers = CsvHeaders.fromLine("id|name")
    val result = headers.validate("id", "missing1", "missing2")
    result.left.value shouldBe a[MissingField]
    result.left.value.field shouldBe "missing1"
  }

  it should "allow headers in any order" in {
    val headers = CsvHeaders.fromLine("description|id|name")
    headers.validate("id", "name", "description").value shouldBe ()
  }
}
