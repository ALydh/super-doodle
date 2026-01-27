package wahapedia.domain.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import wahapedia.csv.CsvProcessor
import wahapedia.domain.types.{DatasheetId, FactionId, AbilityId}
import cats.effect.unsafe.implicits.global

class AssociationParsersSpec extends AnyFlatSpec with Matchers with EitherValues {

  "DatasheetKeywordParser" should "parse keyword associations" in {
    val line = "000000001|Orks||true|"
    val result = DatasheetKeywordParser.parseLine(line)

    result.value.datasheetId shouldBe DatasheetId("000000001")
    result.value.keyword shouldBe Some("Orks")
    result.value.isFactionKeyword shouldBe true
  }

  it should "parse all keywords from CSV" in {
    val keywords = CsvProcessor.failFastParse(
      "data/wahapedia/Datasheets_keywords.csv",
      DatasheetKeywordParser
    ).unsafeRunSync()

    keywords.length should be >= 15000
  }

  "DatasheetAbilityParser" should "parse ability associations" in {
    val line = "000000001|1|000008346||||Core||"
    val result = DatasheetAbilityParser.parseLine(line)

    result.value.datasheetId shouldBe DatasheetId("000000001")
    result.value.abilityId shouldBe Some(AbilityId("000008346"))
    result.value.abilityType shouldBe Some("Core")
  }

  it should "parse all ability associations from CSV" in {
    val abilities = CsvProcessor.failFastParse(
      "data/wahapedia/Datasheets_abilities.csv",
      DatasheetAbilityParser
    ).unsafeRunSync()

    abilities.length should be >= 6000
  }

  "DatasheetOptionParser" should "parse wargear options" in {
    val line = "000000001|1|•|This model's big choppa can be replaced with 1 power klaw.|"
    val result = DatasheetOptionParser.parseLine(line)

    result.value.datasheetId shouldBe DatasheetId("000000001")
    result.value.description shouldBe "This model's big choppa can be replaced with 1 power klaw."
  }

  it should "parse all options from CSV" in {
    val options = CsvProcessor.failFastParse(
      "data/wahapedia/Datasheets_options.csv",
      DatasheetOptionParser
    ).unsafeRunSync()

    options.length should be >= 2700
  }

  "DatasheetLeaderParser" should "parse leader attachments" in {
    val line = "000000022|000003861|"
    val result = DatasheetLeaderParser.parseLine(line)

    result.value.leaderId shouldBe DatasheetId("000000022")
    result.value.attachedId shouldBe DatasheetId("000003861")
  }

  it should "parse all leader rules from CSV" in {
    val leaders = CsvProcessor.failFastParse(
      "data/wahapedia/Datasheets_leader.csv",
      DatasheetLeaderParser
    ).unsafeRunSync()

    leaders.length should be >= 1800
  }

  "StratagemParser" should "parse stratagem" in {
    val line = "|INSANE BRAVERY|000009218005|Boarding Actions – Epic Deed Stratagem|1|Indifferent to their own survival...|Your turn|Command phase|||Description text|"
    val result = StratagemParser.parseLine(line)

    result.value.factionId shouldBe None
    result.value.name shouldBe "INSANE BRAVERY"
    result.value.cpCost shouldBe Some(1)
  }

  it should "parse all stratagems from CSV" in {
    val stratagems = CsvProcessor.failFastParse(
      "data/wahapedia/Stratagems.csv",
      StratagemParser
    ).unsafeRunSync()

    stratagems.length should be >= 1300
  }

  "DatasheetStratagemParser" should "parse stratagem associations" in {
    val line = "000000001|000009796003|"
    val result = DatasheetStratagemParser.parseLine(line)

    result.value.datasheetId shouldBe DatasheetId("000000001")
  }

  it should "parse all stratagem associations from CSV" in {
    val assocs = CsvProcessor.failFastParse(
      "data/wahapedia/Datasheets_stratagems.csv",
      DatasheetStratagemParser
    ).unsafeRunSync()

    assocs.length should be >= 82000
  }

  "EnhancementParser" should "parse enhancement" in {
    val line = "AC|000008395002|Auric Mantle|15|Shield Host|000000765|Legend text|Description text|"
    val result = EnhancementParser.parseLine(line)

    result.value.factionId shouldBe FactionId("AC")
    result.value.name shouldBe "Auric Mantle"
    result.value.cost shouldBe 15
  }

  it should "parse all enhancements from CSV" in {
    val enhancements = CsvProcessor.failFastParse(
      "data/wahapedia/Enhancements.csv",
      EnhancementParser
    ).unsafeRunSync()

    enhancements.length should be >= 800
  }

  "DatasheetEnhancementParser" should "parse enhancement associations" in {
    val line = "000000001|000008885004|"
    val result = DatasheetEnhancementParser.parseLine(line)

    result.value.datasheetId shouldBe DatasheetId("000000001")
  }

  it should "parse all enhancement associations from CSV" in {
    val assocs = CsvProcessor.failFastParse(
      "data/wahapedia/Datasheets_enhancements.csv",
      DatasheetEnhancementParser
    ).unsafeRunSync()

    assocs.length should be >= 10000
  }

  "DetachmentAbilityParser" should "parse detachment ability" in {
    val line = "000008393|AC|Martial Mastery|Legend|Description|Shield Host|000000765|"
    val result = DetachmentAbilityParser.parseLine(line)

    result.value.factionId shouldBe FactionId("AC")
    result.value.name shouldBe "Martial Mastery"
  }

  it should "parse all detachment abilities from CSV" in {
    val abilities = CsvProcessor.failFastParse(
      "data/wahapedia/Detachment_abilities.csv",
      DetachmentAbilityParser
    ).unsafeRunSync()

    abilities.length should be >= 250
  }

  "DatasheetDetachmentAbilityParser" should "parse detachment ability associations" in {
    val line = "000000001|000009990|"
    val result = DatasheetDetachmentAbilityParser.parseLine(line)

    result.value.datasheetId shouldBe DatasheetId("000000001")
  }

  it should "parse all detachment ability associations from CSV" in {
    val assocs = CsvProcessor.failFastParse(
      "data/wahapedia/Datasheets_detachment_abilities.csv",
      DatasheetDetachmentAbilityParser
    ).unsafeRunSync()

    assocs.length should be >= 15000
  }

  "LastUpdateParser" should "parse last update timestamp" in {
    val line = "2026-01-26 19:57:09|"
    val result = LastUpdateParser.parseLine(line)

    result.value.timestamp shouldBe "2026-01-26 19:57:09"
  }

  it should "parse last update from CSV" in {
    val updates = CsvProcessor.failFastParse(
      "data/wahapedia/Last_update.csv",
      LastUpdateParser
    ).unsafeRunSync()

    updates.length shouldBe 1
  }
}
