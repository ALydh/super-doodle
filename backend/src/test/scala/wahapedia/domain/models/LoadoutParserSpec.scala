package wahapedia.domain.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LoadoutParserSpec extends AnyFlatSpec with Matchers {

  "LoadoutParser" should "parse 'Every model is equipped with' pattern" in {
    val loadout = "<b>Every model is equipped with:</b> heavy bolt pistol; Astartes chainsword."
    val result = LoadoutParser.parse(loadout)

    result should have length 1
    result.head.modelPattern shouldBe "*"
    result.head.weapons should contain theSameElementsAs List("heavy bolt pistol", "astartes chainsword")
  }

  it should "parse 'This model is equipped with' pattern" in {
    val loadout = "<b>This model is equipped with:</b> guardian spear."
    val result = LoadoutParser.parse(loadout)

    result should have length 1
    result.head.modelPattern shouldBe "*"
    result.head.weapons should contain only "guardian spear"
  }

  it should "parse 'Each model is equipped with' pattern" in {
    val loadout = "<b>Each model is equipped with:</b> heavy bolt pistol; Astartes chainsword."
    val result = LoadoutParser.parse(loadout)

    result should have length 1
    result.head.modelPattern shouldBe "*"
    result.head.weapons should contain theSameElementsAs List("heavy bolt pistol", "astartes chainsword")
  }

  it should "parse multiple model types with 'Every X is equipped with' pattern" in {
    val loadout = """<b>Every Kill Team Intercessor is equipped with:</b> bolt pistol; bolt rifle; close combat weapon.<br><br><b>Every Kill Team Outrider is equipped with:</b> bolt pistol; twin bolt rifle; Astartes chainsword."""
    val result = LoadoutParser.parse(loadout)

    result should have length 2
    val intercessor = result.find(_.modelPattern.toLowerCase.contains("intercessor")).get
    val outrider = result.find(_.modelPattern.toLowerCase.contains("outrider")).get

    intercessor.weapons should contain theSameElementsAs List("bolt pistol", "bolt rifle", "close combat weapon")
    outrider.weapons should contain theSameElementsAs List("bolt pistol", "twin bolt rifle", "astartes chainsword")
  }

  it should "parse 'The Sergeant is equipped with' pattern" in {
    val loadout = """<b>The Kill Team Sergeant is equipped with:</b> plasma pistol; power weapon.<br><br><b>Each Gravis Veteran is equipped with:</b> infernus heavy bolter; bolt pistol; close combat weapon."""
    val result = LoadoutParser.parse(loadout)

    result should have length 2
    val sergeant = result.find(_.modelPattern.toLowerCase.contains("sergeant")).get
    val gravis = result.find(_.modelPattern.toLowerCase.contains("gravis")).get

    sergeant.weapons should contain theSameElementsAs List("plasma pistol", "power weapon")
    gravis.weapons should contain theSameElementsAs List("infernus heavy bolter", "bolt pistol", "close combat weapon")
  }

  it should "handle empty loadout" in {
    val result = LoadoutParser.parse("")
    result shouldBe empty
  }

  it should "handle null loadout" in {
    val result = LoadoutParser.parse(null)
    result shouldBe empty
  }

  it should "strip HTML tags from weapon names" in {
    val loadout = "<b>Every model is equipped with:</b> <i>bolt</i> pistol; boltgun."
    val result = LoadoutParser.parse(loadout)

    result.head.weapons should contain theSameElementsAs List("bolt pistol", "boltgun")
  }

  it should "normalize weapon names with quantities" in {
    val loadout = "<b>This model is equipped with:</b> 2 godhammer lascannons; twin heavy bolter."
    val result = LoadoutParser.parse(loadout)

    result.head.weapons should contain theSameElementsAs List("godhammer lascannons", "twin heavy bolter")
  }

  "getBaseEquipmentForModel" should "return universal loadout when model type is not specified" in {
    val loadouts = List(ModelLoadout("*", List("bolt pistol", "boltgun")))
    val result = LoadoutParser.getBaseEquipmentForModel(loadouts, None)

    result should contain theSameElementsAs List("bolt pistol", "boltgun")
  }

  it should "return specific loadout for matching model type" in {
    val loadouts = List(
      ModelLoadout("Kill Team Intercessor", List("bolt pistol", "bolt rifle")),
      ModelLoadout("Kill Team Outrider", List("twin bolt rifle"))
    )
    val result = LoadoutParser.getBaseEquipmentForModel(loadouts, Some("Intercessor"))

    result should contain theSameElementsAs List("bolt pistol", "bolt rifle")
  }

  it should "fall back to universal loadout when specific model not found" in {
    val loadouts = List(
      ModelLoadout("*", List("bolt pistol", "boltgun")),
      ModelLoadout("Sergeant", List("plasma pistol", "power weapon"))
    )
    val result = LoadoutParser.getBaseEquipmentForModel(loadouts, Some("Trooper"))

    result should contain theSameElementsAs List("bolt pistol", "boltgun")
  }

  "hasUniversalLoadout" should "return true when universal loadout exists" in {
    val loadouts = List(ModelLoadout("*", List("bolt pistol")))
    LoadoutParser.hasUniversalLoadout(loadouts) shouldBe true
  }

  it should "return false when only specific loadouts exist" in {
    val loadouts = List(ModelLoadout("Sergeant", List("plasma pistol")))
    LoadoutParser.hasUniversalLoadout(loadouts) shouldBe false
  }

  "hasSpecificLoadouts" should "return true when specific loadouts exist" in {
    val loadouts = List(ModelLoadout("Sergeant", List("plasma pistol")))
    LoadoutParser.hasSpecificLoadouts(loadouts) shouldBe true
  }

  it should "return false when only universal loadout exists" in {
    val loadouts = List(ModelLoadout("*", List("bolt pistol")))
    LoadoutParser.hasSpecificLoadouts(loadouts) shouldBe false
  }
}
