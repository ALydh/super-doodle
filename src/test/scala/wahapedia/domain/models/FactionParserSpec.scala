package wahapedia.domain.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import wahapedia.csv.CsvProcessor
import wahapedia.errors.{InvalidId, MissingField}
import wahapedia.domain.types.FactionId
import cats.effect.unsafe.implicits.global

class FactionParserSpec extends AnyFlatSpec with Matchers with EitherValues {

  "parseLine" should "parse valid faction CSV line" in {
    val line = "AoI|Imperial Agents|https://wahapedia.ru/wh40k10ed/factions/imperial-agents|"
    val result = FactionParser.parseLine(line)

    result.value.id shouldBe FactionId("AoI")
    result.value.name shouldBe "Imperial Agents"
    result.value.link shouldBe "https://wahapedia.ru/wh40k10ed/factions/imperial-agents"
  }

  it should "parse faction with 2-letter ID" in {
    val line = "AM|Astra Militarum|https://wahapedia.ru/wh40k10ed/factions/astra-militarum|"
    val result = FactionParser.parseLine(line)

    result.value.id shouldBe FactionId("AM")
    result.value.name shouldBe "Astra Militarum"
  }

  it should "parse faction with 3-letter ID" in {
    val line = "GC|Genestealer Cults|https://wahapedia.ru/wh40k10ed/factions/genestealer-cults|"
    val result = FactionParser.parseLine(line)

    result.value.id shouldBe FactionId("GC")
    result.value.name shouldBe "Genestealer Cults"
  }

  it should "handle trailing pipe delimiter" in {
    val line = "NEC|Necrons|https://wahapedia.ru/wh40k10ed/factions/necrons|"
    val result = FactionParser.parseLine(line)

    result.isRight shouldBe true
  }

  it should "reject invalid faction ID format" in {
    val line = "invalid|Test Faction|https://test.com|"
    val result = FactionParser.parseLine(line)

    result.left.value shouldBe a[InvalidId]
    result.left.value.value shouldBe "invalid"
  }

  it should "accept mixed case faction ID" in {
    val line = "LoV|League of Votann|https://test.com|"
    val result = FactionParser.parseLine(line)

    result.isRight shouldBe true
  }

  it should "reject faction ID with numbers" in {
    val line = "A1|Test Faction|https://test.com|"
    val result = FactionParser.parseLine(line)

    result.left.value shouldBe a[InvalidId]
  }

  it should "reject empty faction ID" in {
    val line = "|Test Faction|https://test.com|"
    val result = FactionParser.parseLine(line)

    result.left.value shouldBe a[MissingField]
    result.left.value.field shouldBe "id"
  }

  it should "reject empty name" in {
    val line = "AoI||https://test.com|"
    val result = FactionParser.parseLine(line)

    result.left.value shouldBe a[MissingField]
    result.left.value.field shouldBe "name"
  }

  it should "reject empty link" in {
    val line = "AoI|Test Faction||"
    val result = FactionParser.parseLine(line)

    result.left.value shouldBe a[MissingField]
    result.left.value.field shouldBe "link"
  }

  "parseLineWithContext" should "provide line number in error" in {
    val line = "invalid|Test Faction|https://test.com|"
    val result = FactionParser.parseLineWithContext(line, 5, "test.csv").attempt.unsafeRunSync()

    result.isLeft shouldBe true
    val error = result.left.value
    error.getMessage should include("line 5")
  }

  it should "provide file path in error" in {
    val line = "invalid|Test Faction|https://test.com|"
    val result = FactionParser.parseLineWithContext(line, 5, "test.csv").attempt.unsafeRunSync()

    result.isLeft shouldBe true
    val error = result.left.value
    error.getMessage should include("test.csv")
  }

  "parseStream integration" should "parse all factions from Factions.csv" in {
    val factions = CsvProcessor.failFastParse(
      "data/wahapedia/Factions.csv",
      FactionParser
    ).unsafeRunSync()

    factions should not be empty
    factions.length should be >= 26
  }

  it should "find Imperial Agents faction" in {
    val factions = CsvProcessor.failFastParse(
      "data/wahapedia/Factions.csv",
      FactionParser
    ).unsafeRunSync()

    factions.exists(_.name == "Imperial Agents") shouldBe true
    factions.find(_.name == "Imperial Agents").map(_.id) shouldBe Some(FactionId("AoI"))
  }

  it should "find Astra Militarum faction" in {
    val factions = CsvProcessor.failFastParse(
      "data/wahapedia/Factions.csv",
      FactionParser
    ).unsafeRunSync()

    factions.exists(_.name == "Astra Militarum") shouldBe true
  }

  it should "skip header row" in {
    val factions = CsvProcessor.failFastParse(
      "data/wahapedia/Factions.csv",
      FactionParser
    ).unsafeRunSync()

    factions.exists(_.name == "name") shouldBe false
    factions.exists(f => FactionId.value(f.id) == "id") shouldBe false
  }
}
