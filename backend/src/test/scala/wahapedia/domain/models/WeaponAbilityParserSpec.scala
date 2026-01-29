package wahapedia.domain.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import wahapedia.csv.CsvProcessor
import cats.effect.unsafe.implicits.global

class WeaponAbilityParserSpec extends AnyFlatSpec with Matchers with EitherValues {

  "parseLine" should "parse a weapon ability entry" in {
    val line = "devastating-wounds|Devastating Wounds|Saving throws cannot be made against Critical Wounds."
    val result = WeaponAbilityParser.parseLine(line)

    result.value.id shouldBe "devastating-wounds"
    result.value.name shouldBe "Devastating Wounds"
    result.value.description shouldBe "Saving throws cannot be made against Critical Wounds."
  }

  "parseStream integration" should "parse all weapon abilities from CSV" in {
    val abilities = CsvProcessor.failFastParse(
      "../data/wahapedia/Weapon_abilities.csv",
      WeaponAbilityParser
    ).unsafeRunSync()

    abilities.length should be >= 15
  }

  it should "find Devastating Wounds" in {
    val abilities = CsvProcessor.failFastParse(
      "../data/wahapedia/Weapon_abilities.csv",
      WeaponAbilityParser
    ).unsafeRunSync()

    val devastating = abilities.find(_.id == "devastating-wounds")
    devastating shouldBe defined
    devastating.get.name shouldBe "Devastating Wounds"
  }
}
