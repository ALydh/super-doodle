package wahapedia.domain.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import wahapedia.csv.CsvProcessor
import wahapedia.domain.types.DatasheetId
import cats.effect.unsafe.implicits.global

class WargearParserSpec extends AnyFlatSpec with Matchers with EitherValues {

  "parseLine" should "parse complete wargear entry" in {
    val line = "000000001|1|1||Kombi-weapon|anti-infantry 4+, devastating wounds, rapid fire 1|24|Ranged|1|5|4|0|1|"
    val result = WargearParser.parseLine(line)

    result.value.datasheetId shouldBe DatasheetId("000000001")
    result.value.line shouldBe Some(1)
    result.value.lineInWargear shouldBe Some(1)
    result.value.dice shouldBe None
    result.value.name shouldBe Some("Kombi-weapon")
    result.value.description shouldBe Some("anti-infantry 4+, devastating wounds, rapid fire 1")
    result.value.range shouldBe Some("24")
    result.value.weaponType shouldBe Some("Ranged")
    result.value.attacks shouldBe Some("1")
    result.value.ballisticSkill shouldBe Some("5")
    result.value.strength shouldBe Some("4")
    result.value.armorPenetration shouldBe Some("0")
    result.value.damage shouldBe Some("1")
  }

  it should "parse wargear with dice notation" in {
    val line = "000000123|1|1|D6|Test Weapon|Abilities|12|Ranged|D6|4|5|-1|2|"
    val result = WargearParser.parseLine(line)

    result.value.dice shouldBe Some("D6")
  }

  it should "parse wargear with empty fields" in {
    val line = "000000087||1||||||0|-|-|0|-|"
    val result = WargearParser.parseLine(line)

    result.value.line shouldBe None
    result.value.name shouldBe None
  }

  "parseStream integration" should "parse all wargear from Datasheets_wargear.csv" in {
    val wargear = CsvProcessor.failFastParse(
      "../data/wahapedia/Datasheets_wargear.csv",
      WargearParser
    ).unsafeRunSync()

    wargear.length should be >= 9000
  }

  it should "find Kombi-weapon" in {
    val wargear = CsvProcessor.failFastParse(
      "../data/wahapedia/Datasheets_wargear.csv",
      WargearParser
    ).unsafeRunSync()

    val kombi = wargear.find(_.name.contains("Kombi-weapon"))
    kombi shouldBe defined
  }
}
