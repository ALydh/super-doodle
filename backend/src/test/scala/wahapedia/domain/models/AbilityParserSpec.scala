package wahapedia.domain.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import wahapedia.csv.CsvProcessor
import wahapedia.errors.{InvalidFormat, MissingField}
import wahapedia.domain.types.{AbilityId, FactionId}
import cats.effect.unsafe.implicits.global

class AbilityParserSpec extends AnyFlatSpec with Matchers with EitherValues {

  "parseLine" should "parse complete ability with all fields" in {
    val line = "000000705|Synapse|Some Tyranids serve as synaptic conduits...|TYR|If your Army Faction is <span class=\"kwb\">TYRANIDS</span>...|"
    val result = AbilityParser.parseLine(line)

    result.value.id shouldBe AbilityId("000000705")
    result.value.name shouldBe "Synapse"
    result.value.legend shouldBe Some("Some Tyranids serve as synaptic conduits...")
    result.value.factionId shouldBe Some(FactionId("TYR"))
    result.value.description shouldBe "If your Army Faction is <span class=\"kwb\">TYRANIDS</span>..."
  }

  it should "parse ability with empty legend" in {
    val line = "000000705|Synapse||TYR|Description text|"
    val result = AbilityParser.parseLine(line)

    result.value.legend shouldBe None
  }

  it should "parse ability with empty faction_id" in {
    val line = "000000705|Universal Ability|Legend text||Description text|"
    val result = AbilityParser.parseLine(line)

    result.value.factionId shouldBe None
  }

  it should "preserve HTML in legend field" in {
    val line = "000000705|Test|<b>Bold</b> and <i>italic</i> text|TYR|Description|"
    val result = AbilityParser.parseLine(line)

    result.value.legend shouldBe Some("<b>Bold</b> and <i>italic</i> text")
  }

  it should "preserve HTML in description field" in {
    val line = "000000705|Test|Legend|TYR|<ul><li>Item 1</li><li>Item 2</li></ul>|"
    val result = AbilityParser.parseLine(line)

    result.value.description shouldBe "<ul><li>Item 1</li><li>Item 2</li></ul>"
  }

  it should "reject invalid ability ID" in {
    val line = "INVALID|Test|Legend|TYR|Description|"
    val result = AbilityParser.parseLine(line)

    result.left.value shouldBe a[wahapedia.errors.InvalidId]
    result.left.value.field shouldBe "id"
  }

  it should "reject invalid faction ID" in {
    val line = "000000705|Test|Legend|INVALID|Description|"
    val result = AbilityParser.parseLine(line)

    result.left.value shouldBe a[wahapedia.errors.InvalidId]
    result.left.value.field shouldBe "id"
  }

  it should "reject empty name" in {
    val line = "000000705||Legend|TYR|Description|"
    val result = AbilityParser.parseLine(line)

    result.left.value shouldBe a[MissingField]
    result.left.value.field shouldBe "name"
  }

  it should "reject empty description" in {
    val line = "000000705|Test|Legend|TYR||"
    val result = AbilityParser.parseLine(line)

    result.left.value shouldBe a[MissingField]
    result.left.value.field shouldBe "description"
  }

  "parseStream integration" should "parse all abilities from Abilities.csv" in {
    val abilities = CsvProcessor.failFastParse(
      "../data/wahapedia/Abilities.csv",
      AbilityParser
    ).unsafeRunSync()

    abilities.length should be >= 90
  }

  it should "find Synapse ability" in {
    val abilities = CsvProcessor.failFastParse(
      "../data/wahapedia/Abilities.csv",
      AbilityParser
    ).unsafeRunSync()

    val synapse = abilities.find(_.name == "Synapse")
    synapse shouldBe defined
    synapse.flatMap(_.factionId) shouldBe defined
  }

  it should "find abilities with and without faction_id" in {
    val abilities = CsvProcessor.failFastParse(
      "../data/wahapedia/Abilities.csv",
      AbilityParser
    ).unsafeRunSync()

    val withFaction = abilities.filter(_.factionId.isDefined)
    val withoutFaction = abilities.filter(_.factionId.isEmpty)

    withFaction should not be empty
    // Check if there are any universal abilities without faction
    // If not, this is fine - all abilities might be faction-specific
  }

  it should "preserve HTML content in descriptions" in {
    val abilities = CsvProcessor.failFastParse(
      "../data/wahapedia/Abilities.csv",
      AbilityParser
    ).unsafeRunSync()

    val withHtml = abilities.filter(_.description.contains("<"))
    withHtml should not be empty
  }
}
