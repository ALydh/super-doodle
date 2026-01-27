package wahapedia.domain.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import wahapedia.csv.CsvProcessor
import wahapedia.domain.types.DatasheetId
import cats.effect.unsafe.implicits.global

class UnitCompositionParserSpec extends AnyFlatSpec with Matchers with EitherValues {

  "parseLine" should "parse unit composition" in {
    val line = "000000001|1|1 Warboss|"
    val result = UnitCompositionParser.parseLine(line)

    result.value.datasheetId shouldBe DatasheetId("000000001")
    result.value.line shouldBe 1
    result.value.description shouldBe "1 Warboss"
  }

  "parseStream integration" should "parse all compositions" in {
    val comps = CsvProcessor.failFastParse(
      "data/wahapedia/Datasheets_unit_composition.csv",
      UnitCompositionParser
    ).unsafeRunSync()

    comps.length should be >= 2000
  }
}
