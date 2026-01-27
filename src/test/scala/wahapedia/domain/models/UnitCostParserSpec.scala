package wahapedia.domain.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import wahapedia.csv.CsvProcessor
import wahapedia.domain.types.DatasheetId
import cats.effect.unsafe.implicits.global

class UnitCostParserSpec extends AnyFlatSpec with Matchers with EitherValues {

  "parseLine" should "parse unit cost" in {
    val line = "000000001|1|1 model|75|"
    val result = UnitCostParser.parseLine(line)

    result.value.datasheetId shouldBe DatasheetId("000000001")
    result.value.line shouldBe 1
    result.value.description shouldBe "1 model"
    result.value.cost shouldBe 75
  }

  "parseStream integration" should "parse all costs" in {
    val costs = CsvProcessor.failFastParse(
      "data/wahapedia/Datasheets_models_cost.csv",
      UnitCostParser
    ).unsafeRunSync()

    costs.length should be >= 2000
  }
}
