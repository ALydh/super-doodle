package wahapedia.domain.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import wahapedia.csv.CsvProcessor
import wahapedia.errors.{InvalidFormat, MissingField}
import wahapedia.domain.types.{SourceId, SourceType}
import cats.effect.unsafe.implicits.global

class SourceParserSpec extends AnyFlatSpec with Matchers with EitherValues {

  "parseLine" should "parse valid source with all fields" in {
    val line = "000000285|Boarding Actions|Expansion|10|1.1|09.07.2025 0:00:00|https://example.com/errata.pdf|"
    val result = SourceParser.parseLine(line)

    result.value.id shouldBe SourceId("000000285")
    result.value.name shouldBe "Boarding Actions"
    result.value.sourceType shouldBe SourceType.Expansion
    result.value.edition shouldBe 10
    result.value.version shouldBe Some("1.1")
    result.value.errataDate shouldBe Some("09.07.2025 0:00:00")
    result.value.errataLink shouldBe Some("https://example.com/errata.pdf")
  }

  it should "parse source with empty optional fields" in {
    val line = "000000166|Balance Dataslate|Rulebook|10|3.3|||"
    val result = SourceParser.parseLine(line)

    result.value.version shouldBe Some("3.3")
    result.value.errataDate shouldBe None
    result.value.errataLink shouldBe None
  }

  it should "parse source with empty version" in {
    val line = "000000271|Legends: Unaligned Forces|Datasheet|10||17.09.2024 0:00:00|https://example.com|"
    val result = SourceParser.parseLine(line)

    result.value.version shouldBe None
    result.value.errataDate shouldBe Some("17.09.2024 0:00:00")
  }

  it should "parse all source types" in {
    SourceParser.parseLine("000000001|Test|Expansion|10|1.0|||").value.sourceType shouldBe SourceType.Expansion
    SourceParser.parseLine("000000001|Test|Rulebook|10|1.0|||").value.sourceType shouldBe SourceType.Rulebook
    SourceParser.parseLine("000000001|Test|Faction Pack|10|1.0|||").value.sourceType shouldBe SourceType.FactionPack
    SourceParser.parseLine("000000001|Test|Codex|10|1.0|||").value.sourceType shouldBe SourceType.Codex
    SourceParser.parseLine("000000001|Test|Index|10|1.0|||").value.sourceType shouldBe SourceType.Index
  }

  it should "reject invalid source ID" in {
    val line = "INVALID|Test|Expansion|10|1.0|||"
    val result = SourceParser.parseLine(line)

    result.left.value.field shouldBe "id"
  }

  it should "reject invalid source type" in {
    val line = "000000001|Test|InvalidType|10|1.0|||"
    val result = SourceParser.parseLine(line)

    result.left.value shouldBe a[InvalidFormat]
    result.left.value.field shouldBe "source_type"
  }

  it should "reject empty name" in {
    val line = "000000001||Expansion|10|1.0|||"
    val result = SourceParser.parseLine(line)

    result.left.value shouldBe a[MissingField]
    result.left.value.field shouldBe "name"
  }

  it should "reject invalid edition" in {
    val line = "000000001|Test|Expansion|ABC|1.0|||"
    val result = SourceParser.parseLine(line)

    result.left.value shouldBe a[InvalidFormat]
    result.left.value.field shouldBe "edition"
  }

  "parseStream integration" should "parse all sources from Source.csv" in {
    val sources = CsvProcessor.failFastParse(
      "data/wahapedia/Source.csv",
      SourceParser
    ).unsafeRunSync()

    sources.length should be >= 70
  }

  it should "find Boarding Actions source" in {
    val sources = CsvProcessor.failFastParse(
      "data/wahapedia/Source.csv",
      SourceParser
    ).unsafeRunSync()

    sources.exists(_.name == "Boarding Actions") shouldBe true
    sources.find(_.name == "Boarding Actions").map(_.sourceType) shouldBe Some(SourceType.Expansion)
  }

  it should "find sources with different types" in {
    val sources = CsvProcessor.failFastParse(
      "data/wahapedia/Source.csv",
      SourceParser
    ).unsafeRunSync()

    sources.exists(_.sourceType == SourceType.Expansion) shouldBe true
    sources.exists(_.sourceType == SourceType.Rulebook) shouldBe true
    sources.exists(_.sourceType == SourceType.FactionPack) shouldBe true
  }
}
