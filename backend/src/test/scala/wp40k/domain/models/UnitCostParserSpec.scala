package wp40k.domain.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import wp40k.csv.CsvProcessor
import wp40k.domain.types.DatasheetId
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

  it should "default to a single open-ended tier when tier columns are absent" in {
    val result = UnitCostParser.parseLine("000000001|1|1 model|75|")

    result.value.minCount shouldBe 1
    result.value.maxCount shouldBe None
  }

  it should "parse an open-ended ordinal tier when tier columns are present" in {
    val result = UnitCostParser.parseLine("000000231|1|5 models|260|2||")

    result.value.cost shouldBe 260
    result.value.minCount shouldBe 2
    result.value.maxCount shouldBe None
  }

  it should "parse a bounded tier range" in {
    val result = UnitCostParser.parseLine("000003698|1|3 models|80|1|2|")

    result.value.minCount shouldBe 1
    result.value.maxCount shouldBe Some(2)
  }

  "parseStream integration" should "parse all costs" in {
    val costs = CsvProcessor.failFastParse(
      "../data/wp40k/Datasheets_models_cost.csv",
      UnitCostParser
    ).unsafeRunSync()

    costs.length should be >= 2000
  }
}
