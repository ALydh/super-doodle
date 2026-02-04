package wahapedia.domain.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CompositionLineParserSpec extends AnyFlatSpec with Matchers {

  "parseLine" should "parse single count pattern" in {
    val result = CompositionLineParser.parseLine("1 Boss Nob")

    result shouldBe defined
    result.get should have length 1
    result.get.head shouldBe ParsedCompositionLine("Boss Nob", 1, 1)
  }

  it should "parse range pattern" in {
    val result = CompositionLineParser.parseLine("9-19 Boyz")

    result shouldBe defined
    result.get should have length 1
    result.get.head shouldBe ParsedCompositionLine("Boyz", 9, 19)
  }

  it should "parse compound pattern with 'and'" in {
    val result = CompositionLineParser.parseLine("1 Runtherd and 10 Gretchin")

    result shouldBe defined
    result.get should have length 2
    result.get(0) shouldBe ParsedCompositionLine("Runtherd", 1, 1)
    result.get(1) shouldBe ParsedCompositionLine("Gretchin", 10, 10)
  }

  it should "return None for 'OR' separator" in {
    CompositionLineParser.parseLine("OR") shouldBe None
    CompositionLineParser.parseLine("or") shouldBe None
  }

  it should "strip HTML tags from model names" in {
    val result = CompositionLineParser.parseLine("""1 Ghazghkull Thraka – <span class="kwb">EPIC</span> <span class="kwb">HERO</span>""")

    result shouldBe defined
    result.get.head.modelName shouldBe "Ghazghkull Thraka – EPIC HERO"
  }

  it should "handle null input" in {
    CompositionLineParser.parseLine(null) shouldBe None
  }

  it should "handle empty input" in {
    CompositionLineParser.parseLine("") shouldBe None
  }

  it should "parse small range pattern" in {
    val result = CompositionLineParser.parseLine("1-2 Spanners")

    result shouldBe defined
    result.get.head shouldBe ParsedCompositionLine("Spanners", 1, 2)
  }

  "parseAll" should "parse multiple composition lines" in {
    val lines = List(
      "1 Boss Nob",
      "9-19 Boyz"
    )
    val result = CompositionLineParser.parseAll(lines)

    result should have length 2
    result(0) shouldBe ParsedCompositionLine("Boss Nob", 1, 1)
    result(1) shouldBe ParsedCompositionLine("Boyz", 9, 19)
  }

  it should "skip OR lines when parsing multiple" in {
    val lines = List(
      "1 Runtherd and 10 Gretchin",
      "OR",
      "2 Runtherds and 20 Gretchin"
    )
    val result = CompositionLineParser.parseAll(lines)

    result should have length 4
  }

  "ParsedCompositionLine.isLeader" should "return true for fixed count of 1" in {
    ParsedCompositionLine("Boss Nob", 1, 1).isLeader shouldBe true
  }

  it should "return false for variable counts" in {
    ParsedCompositionLine("Boyz", 9, 19).isLeader shouldBe false
  }

  it should "return false for fixed count > 1" in {
    ParsedCompositionLine("Gretchin", 10, 10).isLeader shouldBe false
  }

  "calculateModelCounts" should "calculate counts for simple leader + troopers" in {
    val lines = List(
      ParsedCompositionLine("Boss Nob", 1, 1),
      ParsedCompositionLine("Boyz", 9, 19)
    )
    val result = CompositionLineParser.calculateModelCounts(lines, 10)

    result("boss nob") shouldBe 1
    result("boyz") shouldBe 9
  }

  it should "calculate counts for larger unit size" in {
    val lines = List(
      ParsedCompositionLine("Boss Nob", 1, 1),
      ParsedCompositionLine("Boyz", 9, 19)
    )
    val result = CompositionLineParser.calculateModelCounts(lines, 20)

    result("boss nob") shouldBe 1
    result("boyz") shouldBe 19
  }

  it should "handle compound unit" in {
    val lines = List(
      ParsedCompositionLine("Runtherd", 1, 1),
      ParsedCompositionLine("Gretchin", 10, 10)
    )
    val result = CompositionLineParser.calculateModelCounts(lines, 11)

    result("runtherd") shouldBe 1
    result("gretchin") shouldBe 10
  }

  it should "handle single model units" in {
    val lines = List(ParsedCompositionLine("Warboss", 1, 1))
    val result = CompositionLineParser.calculateModelCounts(lines, 1)

    result("warboss") shouldBe 1
  }

  it should "handle empty lines" in {
    val result = CompositionLineParser.calculateModelCounts(List.empty, 10)
    result shouldBe empty
  }

  "matchModelTarget" should "find exact match" in {
    val lines = List(
      ParsedCompositionLine("Boss Nob", 1, 1),
      ParsedCompositionLine("Boyz", 9, 19)
    )
    val result = CompositionLineParser.matchModelTarget("Boss Nob", lines)

    result shouldBe defined
    result.get.modelName shouldBe "Boss Nob"
  }

  it should "find partial match" in {
    val lines = List(
      ParsedCompositionLine("Boss Nob", 1, 1),
      ParsedCompositionLine("Boyz", 9, 19)
    )
    val result = CompositionLineParser.matchModelTarget("Nob", lines)

    result shouldBe defined
    result.get.modelName shouldBe "Boss Nob"
  }

  it should "be case insensitive" in {
    val lines = List(ParsedCompositionLine("Boss Nob", 1, 1))
    val result = CompositionLineParser.matchModelTarget("boss nob", lines)

    result shouldBe defined
  }

  "findLeaderModel" should "find the leader" in {
    val lines = List(
      ParsedCompositionLine("Boss Nob", 1, 1),
      ParsedCompositionLine("Boyz", 9, 19)
    )
    val result = CompositionLineParser.findLeaderModel(lines)

    result shouldBe defined
    result.get.modelName shouldBe "Boss Nob"
  }

  it should "return None when no leader exists" in {
    val lines = List(ParsedCompositionLine("Boyz", 9, 19))
    CompositionLineParser.findLeaderModel(lines) shouldBe None
  }

  "findTrooperModels" should "find non-leader models" in {
    val lines = List(
      ParsedCompositionLine("Boss Nob", 1, 1),
      ParsedCompositionLine("Boyz", 9, 19)
    )
    val result = CompositionLineParser.findTrooperModels(lines)

    result should have length 1
    result.head.modelName shouldBe "Boyz"
  }

  "selectGroupForSize" should "select first group for small unit size" in {
    val lines = List(
      ParsedCompositionLine("Runtherd", 1, 1, groupIndex = 0),
      ParsedCompositionLine("Gretchin", 10, 10, groupIndex = 0),
      ParsedCompositionLine("Runtherds", 2, 2, groupIndex = 1),
      ParsedCompositionLine("Gretchin", 20, 20, groupIndex = 1)
    )
    val result = CompositionLineParser.selectGroupForSize(lines, 11)

    result should have length 2
    result.forall(_.groupIndex == 0) shouldBe true
    result.map(_.modelName) should contain allOf ("Runtherd", "Gretchin")
  }

  it should "select second group for larger unit size" in {
    val lines = List(
      ParsedCompositionLine("Runtherd", 1, 1, groupIndex = 0),
      ParsedCompositionLine("Gretchin", 10, 10, groupIndex = 0),
      ParsedCompositionLine("Runtherds", 2, 2, groupIndex = 1),
      ParsedCompositionLine("Gretchin", 20, 20, groupIndex = 1)
    )
    val result = CompositionLineParser.selectGroupForSize(lines, 22)

    result should have length 2
    result.forall(_.groupIndex == 1) shouldBe true
  }

  it should "select last group when size exceeds all groups" in {
    val lines = List(
      ParsedCompositionLine("Runtherd", 1, 1, groupIndex = 0),
      ParsedCompositionLine("Gretchin", 10, 10, groupIndex = 0)
    )
    val result = CompositionLineParser.selectGroupForSize(lines, 100)

    result should have length 2
    result.forall(_.groupIndex == 0) shouldBe true
  }

  it should "return empty list for empty input" in {
    CompositionLineParser.selectGroupForSize(List.empty, 10) shouldBe empty
  }

  it should "handle single group" in {
    val lines = List(
      ParsedCompositionLine("Boss Nob", 1, 1, groupIndex = 0),
      ParsedCompositionLine("Boyz", 9, 19, groupIndex = 0)
    )
    val result = CompositionLineParser.selectGroupForSize(lines, 10)

    result should have length 2
    result.map(_.modelName) should contain allOf ("Boss Nob", "Boyz")
  }
}
