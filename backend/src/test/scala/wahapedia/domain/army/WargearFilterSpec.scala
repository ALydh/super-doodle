package wahapedia.domain.army

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import wahapedia.domain.types.*
import wahapedia.domain.models.{Wargear, ParsedWargearOption, WargearAction, LoadoutParser}

class WargearFilterSpec extends AnyFlatSpec with Matchers {

  val dsId: DatasheetId = DatasheetId("000000001")

  def wargear(name: String): Wargear = Wargear(
    dsId, Some(1), Some(1), None, Some(name), None, None, None, None, None, None, None, None
  )

  def parsedOption(line: Int, action: WargearAction, weapon: String, choice: Int = 0): ParsedWargearOption =
    ParsedWargearOption(dsId, line, choice, 0, action, weapon, None, 0, 0)

  "filterWargear" should "return all wargear when no parsed options exist" in {
    val allWargear = List(wargear("Bolt Rifle"), wargear("Frag Grenades"))
    val result = WargearFilter.filterWargear(allWargear, List.empty, List.empty)
    result.map(_.name) should contain theSameElementsAs List(Some("Bolt Rifle"), Some("Frag Grenades"))
  }

  it should "exclude optional weapons (those in add actions) by default" in {
    val allWargear = List(wargear("Bolt Rifle"), wargear("Power Fist"))
    val parsed = List(parsedOption(1, WargearAction.Add, "Power Fist"))
    val result = WargearFilter.filterWargear(allWargear, parsed, List.empty)
    result.map(_.name) should contain only Some("Bolt Rifle")
  }

  it should "include optional weapons when selection is active" in {
    val allWargear = List(wargear("Bolt Rifle"), wargear("Power Fist"))
    val parsed = List(parsedOption(1, WargearAction.Add, "Power Fist"))
    val selections = List(WargearSelection(1, true, None))
    val result = WargearFilter.filterWargear(allWargear, parsed, selections)
    result.map(_.name) should contain theSameElementsAs List(Some("Bolt Rifle"), Some("Power Fist"))
  }

  it should "remove weapons when selection with remove action is active" in {
    val allWargear = List(wargear("Bolt Rifle"), wargear("Frag Grenades"))
    val parsed = List(parsedOption(1, WargearAction.Remove, "Bolt Rifle"))
    val selections = List(WargearSelection(1, true, None))
    val result = WargearFilter.filterWargear(allWargear, parsed, selections)
    result.map(_.name) should contain only Some("Frag Grenades")
  }

  it should "handle weapon prefix matching" in {
    val allWargear = List(wargear("Power Fist"), wargear("Power Fist (melee)"), wargear("Bolt Rifle"))
    val parsed = List(parsedOption(1, WargearAction.Add, "Power Fist"))
    val selections = List(WargearSelection(1, true, None))
    val result = WargearFilter.filterWargear(allWargear, parsed, selections)
    result.map(_.name) should contain theSameElementsAs List(Some("Power Fist"), Some("Power Fist (melee)"), Some("Bolt Rifle"))
  }

  it should "handle choice-based selections via notes" in {
    val allWargear = List(wargear("Bolt Rifle"), wargear("Melta Gun"), wargear("Plasma Gun"))
    val parsed = List(
      parsedOption(1, WargearAction.Remove, "Bolt Rifle", 0),
      parsedOption(1, WargearAction.Add, "Melta Gun", 1),
      parsedOption(1, WargearAction.Add, "Plasma Gun", 2)
    )
    val selections = List(WargearSelection(1, true, Some("Melta Gun")))
    val result = WargearFilter.filterWargear(allWargear, parsed, selections)
    result.map(_.name) should contain only Some("Melta Gun")
  }

  it should "not include inactive selections" in {
    val allWargear = List(wargear("Bolt Rifle"), wargear("Power Fist"))
    val parsed = List(parsedOption(1, WargearAction.Add, "Power Fist"))
    val selections = List(WargearSelection(1, false, None))
    val result = WargearFilter.filterWargear(allWargear, parsed, selections)
    result.map(_.name) should contain only Some("Bolt Rifle")
  }

  it should "filter out wargear with no name" in {
    val allWargear = List(wargear("Bolt Rifle"), Wargear(dsId, None, None, None, None, None, None, None, None, None, None, None, None))
    val result = WargearFilter.filterWargear(allWargear, List.empty, List.empty)
    result.map(_.name) should contain only Some("Bolt Rifle")
  }

  def parsedOptionWithTarget(line: Int, action: WargearAction, weapon: String, choice: Int = 0, modelTarget: Option[String] = None, maxCount: Int = 0): ParsedWargearOption =
    ParsedWargearOption(dsId, line, choice, 0, action, weapon, modelTarget, 0, maxCount)

  def parseLoadout(html: String): List[wahapedia.domain.models.ModelLoadout] =
    LoadoutParser.parse(html)

  "filterWargearWithQuantities" should "return all weapons with unit size quantity when no loadout" in {
    val allWargear = List(wargear("Bolt Rifle"), wargear("Frag Grenades"))
    val result = WargearFilter.filterWargearWithQuantities(allWargear, List.empty, List.empty, List.empty, 5)
    result.map(w => (w.wargear.name, w.quantity)) should contain theSameElementsAs List(
      (Some("Bolt Rifle"), 5),
      (Some("Frag Grenades"), 5)
    )
  }

  it should "calculate quantities based on universal loadout" in {
    val allWargear = List(wargear("Heavy bolt pistol"), wargear("Astartes chainsword"))
    val loadouts = parseLoadout("<b>Every model is equipped with:</b> heavy bolt pistol; Astartes chainsword.")
    val result = WargearFilter.filterWargearWithQuantities(allWargear, List.empty, List.empty, loadouts, 5)
    result.map(w => (w.wargear.name, w.quantity)) should contain theSameElementsAs List(
      (Some("Heavy bolt pistol"), 5),
      (Some("Astartes chainsword"), 5)
    )
  }

  it should "reduce quantity when weapon is replaced via selection" in {
    val allWargear = List(wargear("Heavy bolt pistol"), wargear("Plasma pistol"), wargear("Astartes chainsword"))
    val loadouts = parseLoadout("<b>Every model is equipped with:</b> heavy bolt pistol; Astartes chainsword.")
    val parsed = List(
      parsedOptionWithTarget(1, WargearAction.Remove, "heavy bolt pistol", maxCount = 1),
      parsedOptionWithTarget(1, WargearAction.Add, "plasma pistol", maxCount = 1)
    )
    val selections = List(WargearSelection(1, true, None))
    val result = WargearFilter.filterWargearWithQuantities(allWargear, parsed, selections, loadouts, 5)

    val pistolResult = result.find(_.wargear.name.contains("Heavy bolt pistol"))
    val plasmaResult = result.find(_.wargear.name.contains("Plasma pistol"))

    pistolResult.map(_.quantity) shouldBe Some(4)
    plasmaResult.map(_.quantity) shouldBe Some(1)
  }

  it should "return minimum quantity of 1 when calculated quantity is 0" in {
    val allWargear = List(wargear("Power Fist"))
    val loadouts = parseLoadout("<b>Every model is equipped with:</b> bolt pistol.")
    val parsed = List(parsedOptionWithTarget(1, WargearAction.Add, "power fist", maxCount = 1))
    val selections = List(WargearSelection(1, true, None))
    val result = WargearFilter.filterWargearWithQuantities(allWargear, parsed, selections, loadouts, 5)

    result.find(_.wargear.name.contains("Power Fist")).map(_.quantity) shouldBe Some(1)
  }

  it should "handle empty loadout" in {
    val allWargear = List(wargear("Bolt Rifle"))
    val result = WargearFilter.filterWargearWithQuantities(allWargear, List.empty, List.empty, List.empty, 5)
    result.map(w => (w.wargear.name, w.quantity)) should contain theSameElementsAs List(
      (Some("Bolt Rifle"), 5)
    )
  }

  it should "replace all weapons when maxCount is 0 (all models replacement)" in {
    val allWargear = List(wargear("Mace of absolution"), wargear("Power weapon"))
    val defaults = List(WargearDefault("mace of absolution", 3, None))
    val parsed = List(
      parsedOptionWithTarget(1, WargearAction.Remove, "mace of absolution", maxCount = 0),
      parsedOptionWithTarget(1, WargearAction.Add, "power weapon", maxCount = 0)
    )
    val selections = List(WargearSelection(1, true, None))
    val result = WargearFilter.filterWargearWithDefaults(allWargear, parsed, selections, defaults, 5)

    result.find(_.wargear.name.contains("Mace of absolution")) shouldBe None
    result.find(_.wargear.name.contains("Power weapon")).map(_.quantity) shouldBe Some(3)
  }

  it should "handle unit size of 1" in {
    val allWargear = List(wargear("Guardian spear"))
    val loadouts = parseLoadout("<b>This model is equipped with:</b> guardian spear.")
    val result = WargearFilter.filterWargearWithQuantities(allWargear, List.empty, List.empty, loadouts, 1)
    result.map(w => (w.wargear.name, w.quantity)) should contain theSameElementsAs List(
      (Some("Guardian spear"), 1)
    )
  }
}
