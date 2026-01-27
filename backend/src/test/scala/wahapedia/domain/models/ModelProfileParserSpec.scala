package wahapedia.domain.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import wahapedia.csv.CsvProcessor
import wahapedia.errors.{InvalidFormat, MissingField}
import wahapedia.domain.types.{DatasheetId, Save}
import cats.effect.unsafe.implicits.global

class ModelProfileParserSpec extends AnyFlatSpec with Matchers with EitherValues {

  "parseLine" should "parse complete model profile with all fields" in {
    val line = "000000001|1|Warboss|6\"|5|4+|5|Invuln description|6|6+|1|40mm|Base description|"
    val result = ModelProfileParser.parseLine(line)

    result.value.datasheetId shouldBe DatasheetId("000000001")
    result.value.line shouldBe 1
    result.value.name shouldBe Some("Warboss")
    result.value.movement shouldBe "6\""
    result.value.toughness shouldBe "5"
    result.value.save shouldBe Save(4)
    result.value.invulnerableSave shouldBe Some("5")
    result.value.invulnerableSaveDescription shouldBe Some("Invuln description")
    result.value.wounds shouldBe 6
    result.value.leadership shouldBe "6+"
    result.value.objectiveControl shouldBe 1
    result.value.baseSize shouldBe Some("40mm")
    result.value.baseSizeDescription shouldBe Some("Base description")
  }

  it should "parse model with empty name and base_size" in {
    val line = "000002610|1|Regimental Attachés|6\"|3|5+|-||1|7+|1|||"
    val result = ModelProfileParser.parseLine(line)

    result.value.name shouldBe Some("Regimental Attachés")
    result.value.baseSize shouldBe None
  }

  it should "parse model with empty name" in {
    val line = "000001545|1||-|9|4+|-||10|7+|0|Use model||"
    val result = ModelProfileParser.parseLine(line)

    result.value.name shouldBe None
    result.value.movement shouldBe "-"
  }

  it should "parse model with minimal fields" in {
    val line = "000000002|1|Warboss In Mega Armour|5\"|6|2+|5||7|6+|1|50mm||"
    val result = ModelProfileParser.parseLine(line)

    result.value.invulnerableSaveDescription shouldBe None
    result.value.baseSizeDescription shouldBe None
  }

  it should "parse model with no invulnerable save" in {
    val line = "000000003|1|Gretchin|5\"|2|7+|||1|7+|0|25mm||"
    val result = ModelProfileParser.parseLine(line)

    result.value.invulnerableSave shouldBe None
  }

  it should "parse model with dash invulnerable save" in {
    val line = "000000004|1|Test|-|5|3+|-||10|6+|2|40mm||"
    val result = ModelProfileParser.parseLine(line)

    result.value.movement shouldBe "-"
    result.value.invulnerableSave shouldBe Some("-")
  }

  it should "parse model with variable movement" in {
    val line = "000000005|1|Fast Unit|20+|5|4+|||5|6+|1|40mm||"
    val result = ModelProfileParser.parseLine(line)

    result.value.movement shouldBe "20+"
  }

  it should "parse model with asterisk toughness" in {
    val line = "000000006|1|Special Model|6\"|5*|4+|||5|6+|1|40mm||"
    val result = ModelProfileParser.parseLine(line)

    result.value.toughness shouldBe "5*"
  }

  it should "parse model with asterisk invulnerable save" in {
    val line = "000000007|1|Special Model|6\"|5|4+|4*||5|6+|1|40mm||"
    val result = ModelProfileParser.parseLine(line)

    result.value.invulnerableSave shouldBe Some("4*")
  }

  it should "reject invalid datasheet ID" in {
    val line = "INVALID|1|Test|6\"|5|4+|||6|6+|1|40mm||"
    val result = ModelProfileParser.parseLine(line)

    result.left.value shouldBe a[wahapedia.errors.InvalidId]
  }

  it should "reject invalid line number" in {
    val line = "000000001|ABC|Test|6\"|5|4+|||6|6+|1|40mm||"
    val result = ModelProfileParser.parseLine(line)

    result.left.value shouldBe a[InvalidFormat]
    result.left.value.field shouldBe "line"
  }

  it should "reject invalid save" in {
    val line = "000000001|1|Test|6\"|5|INVALID|||6|6+|1|40mm||"
    val result = ModelProfileParser.parseLine(line)

    result.left.value shouldBe a[InvalidFormat]
    result.left.value.field shouldBe "save"
  }

  it should "reject invalid wounds" in {
    val line = "000000001|1|Test|6\"|5|4+|||ABC|6+|1|40mm||"
    val result = ModelProfileParser.parseLine(line)

    result.left.value shouldBe a[InvalidFormat]
    result.left.value.field shouldBe "wounds"
  }

  "parseStream integration" should "parse all model profiles from Datasheets_models.csv" in {
    val models = CsvProcessor.failFastParse(
      "../data/wahapedia/Datasheets_models.csv",
      ModelProfileParser
    ).unsafeRunSync()

    models.length should be >= 1700
  }

  it should "find Warboss model" in {
    val models = CsvProcessor.failFastParse(
      "../data/wahapedia/Datasheets_models.csv",
      ModelProfileParser
    ).unsafeRunSync()

    val warboss = models.find(_.name.contains("Warboss"))
    warboss shouldBe defined
    warboss.map(_.movement) shouldBe Some("6\"")
  }

  it should "find models with invulnerable saves" in {
    val models = CsvProcessor.failFastParse(
      "../data/wahapedia/Datasheets_models.csv",
      ModelProfileParser
    ).unsafeRunSync()

    val withInvuln = models.filter(_.invulnerableSave.isDefined)
    withInvuln should not be empty
  }

  it should "find models with different leadership values" in {
    val models = CsvProcessor.failFastParse(
      "../data/wahapedia/Datasheets_models.csv",
      ModelProfileParser
    ).unsafeRunSync()

    val leadershipValues = models.map(_.leadership).distinct
    leadershipValues should contain("6+")
    leadershipValues should contain("7+")
  }
}
