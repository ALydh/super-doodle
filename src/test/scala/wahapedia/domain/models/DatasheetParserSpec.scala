package wahapedia.domain.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import wahapedia.csv.CsvProcessor
import wahapedia.errors.{InvalidFormat, MissingField}
import wahapedia.domain.types.{DatasheetId, FactionId, SourceId, Role}
import cats.effect.unsafe.implicits.global

class DatasheetParserSpec extends AnyFlatSpec with Matchers with EitherValues {

  "parseLine" should "parse complete datasheet with all fields" in {
    val line = "000000884|Venerable Land Raider|AC|000000024|Land Raiders are heavily armed...|Other|<b>This model is equipped with:</b> 2 godhammer lascannons.|This model has a transport capacity of 6.|false|||1-5|While this model has 1-5 wounds remaining...|https://wahapedia.ru/test|"
    val result = DatasheetParser.parseLine(line)

    result.value.id shouldBe DatasheetId("000000884")
    result.value.name shouldBe "Venerable Land Raider"
    result.value.factionId shouldBe Some(FactionId("AC"))
    result.value.sourceId shouldBe Some(SourceId("000000024"))
    result.value.legend shouldBe Some("Land Raiders are heavily armed...")
    result.value.role shouldBe Some(Role.Other)
    result.value.transport shouldBe Some("This model has a transport capacity of 6.")
    result.value.virtual shouldBe false
    result.value.damagedW shouldBe Some("1-5")
    result.value.damagedDescription shouldBe Some("While this model has 1-5 wounds remaining...")
  }

  it should "parse datasheet with minimal fields" in {
    val line = "000000882|Custodian Guard|AC|000000024|Legend text|Battleline|Loadout text||false|||||https://test.com|"
    val result = DatasheetParser.parseLine(line)

    result.value.legend shouldBe Some("Legend text")
    result.value.transport shouldBe None
    result.value.leaderHead shouldBe None
    result.value.leaderFooter shouldBe None
    result.value.damagedW shouldBe None
    result.value.damagedDescription shouldBe None
  }

  it should "parse datasheet with empty legend" in {
    val line = "000001822|X-101|AdM|000000371||Characters|<b>This model is equipped with:</b> grav-gun; hydraulic claw.||false|||||https://wahapedia.ru/test|"
    val result = DatasheetParser.parseLine(line)

    result.value.legend shouldBe None
  }

  it should "parse virtual datasheet with empty fields" in {
    val line = "000003708|Example Wargear|SM||||||true|||||https://wahapedia.ru/test|"
    val result = DatasheetParser.parseLine(line)

    result.value.factionId shouldBe Some(FactionId("SM"))
    result.value.sourceId shouldBe None
    result.value.legend shouldBe None
    result.value.role shouldBe None
    result.value.loadout shouldBe None
    result.value.virtual shouldBe true
  }

  it should "parse all role types" in {
    DatasheetParser.parseLine("000000001|Test|AC|000000024|Legend|Battleline|Loadout||false|||||https://test.com|")
      .value.role shouldBe Some(Role.Battleline)
    DatasheetParser.parseLine("000000001|Test|AC|000000024|Legend|Characters|Loadout||false|||||https://test.com|")
      .value.role shouldBe Some(Role.Characters)
    DatasheetParser.parseLine("000000001|Test|AC|000000024|Legend|Dedicated Transports|Loadout||false|||||https://test.com|")
      .value.role shouldBe Some(Role.DedicatedTransports)
    DatasheetParser.parseLine("000000001|Test|AC|000000024|Legend|Other|Loadout||false|||||https://test.com|")
      .value.role shouldBe Some(Role.Other)
  }

  it should "parse virtual flag as true" in {
    val line = "000000001|Test|AC|000000024|Legend|Other|Loadout||true|||||https://test.com|"
    val result = DatasheetParser.parseLine(line)

    result.value.virtual shouldBe true
  }

  it should "preserve HTML in legend field" in {
    val line = "000000001|Test|AC|000000024|<b>Bold</b> and <i>italic</i> text|Other|Loadout||false|||||https://test.com|"
    val result = DatasheetParser.parseLine(line)

    result.value.legend shouldBe Some("<b>Bold</b> and <i>italic</i> text")
  }

  it should "preserve HTML in loadout field" in {
    val line = "000000001|Test|AC|000000024|Legend|Other|<b>Every model is equipped with:</b> weapon.||false|||||https://test.com|"
    val result = DatasheetParser.parseLine(line)

    result.value.loadout shouldBe Some("<b>Every model is equipped with:</b> weapon.")
  }

  it should "parse datasheet with empty loadout" in {
    val line = "000002619|Aegis Defence Line|AM|000000016|Aegis Defence Lines are barricades...|Fortifications|||false|||||https://wahapedia.ru/test|"
    val result = DatasheetParser.parseLine(line)

    result.value.loadout shouldBe None
  }

  it should "reject invalid datasheet ID" in {
    val line = "INVALID|Test|AC|000000024|Legend|Other|Loadout||false|||||https://test.com|"
    val result = DatasheetParser.parseLine(line)

    result.left.value shouldBe a[wahapedia.errors.InvalidId]
    result.left.value.field shouldBe "id"
  }

  it should "reject invalid faction ID" in {
    val line = "000000001|Test|INVALID|000000024|Legend|Other|Loadout||false|||||https://test.com|"
    val result = DatasheetParser.parseLine(line)

    result.left.value shouldBe a[wahapedia.errors.InvalidId]
    result.left.value.field shouldBe "id"
  }

  it should "reject invalid source ID" in {
    val line = "000000001|Test|AC|INVALID|Legend|Other|Loadout||false|||||https://test.com|"
    val result = DatasheetParser.parseLine(line)

    result.left.value shouldBe a[wahapedia.errors.InvalidId]
    result.left.value.field shouldBe "id"
  }

  it should "reject invalid role" in {
    val line = "000000001|Test|AC|000000024|Legend|InvalidRole|Loadout||false|||||https://test.com|"
    val result = DatasheetParser.parseLine(line)

    result.left.value shouldBe a[InvalidFormat]
    result.left.value.field shouldBe "role"
  }

  it should "reject invalid virtual flag" in {
    val line = "000000001|Test|AC|000000024|Legend|Other|Loadout||maybe|||||https://test.com|"
    val result = DatasheetParser.parseLine(line)

    result.left.value shouldBe a[InvalidFormat]
    result.left.value.field shouldBe "virtual"
  }

  "parseStream integration" should "parse all datasheets from Datasheets.csv" in {
    val datasheets = CsvProcessor.failFastParse(
      "data/wahapedia/Datasheets.csv",
      DatasheetParser
    ).unsafeRunSync()

    datasheets.length should be >= 1679
  }

  it should "find Custodian Guard datasheet" in {
    val datasheets = CsvProcessor.failFastParse(
      "data/wahapedia/Datasheets.csv",
      DatasheetParser
    ).unsafeRunSync()

    val custodianGuard = datasheets.find(_.name == "Custodian Guard")
    custodianGuard shouldBe defined
    custodianGuard.flatMap(_.role) shouldBe Some(Role.Battleline)
    custodianGuard.flatMap(_.factionId) shouldBe Some(FactionId("AC"))
  }

  it should "find datasheets with transport capacity" in {
    val datasheets = CsvProcessor.failFastParse(
      "data/wahapedia/Datasheets.csv",
      DatasheetParser
    ).unsafeRunSync()

    val withTransport = datasheets.filter(_.transport.isDefined)
    withTransport should not be empty
  }

  it should "find datasheets with damage degradation" in {
    val datasheets = CsvProcessor.failFastParse(
      "data/wahapedia/Datasheets.csv",
      DatasheetParser
    ).unsafeRunSync()

    val withDamage = datasheets.filter(_.damagedW.isDefined)
    withDamage should not be empty
  }
}
