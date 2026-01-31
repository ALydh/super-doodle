package wahapedia.domain.models

case class ModelLoadout(modelPattern: String, weapons: List[String])

object LoadoutParser {

  private val everyModelPattern = """(?i)<b>Every model is equipped with:</b>\s*(.+?)(?:\.|$)""".r.unanchored
  private val thisModelPattern = """(?i)<b>This model is equipped with:</b>\s*(.+?)(?:\.|$)""".r.unanchored
  private val eachModelPattern = """(?i)<b>Each model is equipped with:</b>\s*(.+?)(?:\.|$)""".r.unanchored
  private val everySpecificPattern = """(?i)<b>Every\s+(.+?)\s+is equipped with:</b>\s*(.+?)(?:\.|$)""".r.unanchored
  private val theSpecificPattern = """(?i)<b>The\s+(.+?)\s+is equipped with:</b>\s*(.+?)(?:\.|$)""".r.unanchored
  private val eachSpecificPattern = """(?i)<b>Each\s+(.+?)\s+is equipped with:</b>\s*(.+?)(?:\.|$)""".r.unanchored

  def parse(loadoutHtml: String): List[ModelLoadout] = {
    if (loadoutHtml == null || loadoutHtml.isEmpty) return List.empty

    val loadouts = scala.collection.mutable.ListBuffer[ModelLoadout]()
    val cleanHtml = loadoutHtml.replaceAll("<br\\s*/?>", "\n")

    everyModelPattern.findAllMatchIn(cleanHtml).foreach { m =>
      loadouts += ModelLoadout("*", parseWeaponList(m.group(1)))
    }

    thisModelPattern.findAllMatchIn(cleanHtml).foreach { m =>
      loadouts += ModelLoadout("*", parseWeaponList(m.group(1)))
    }

    eachModelPattern.findAllMatchIn(cleanHtml).foreach { m =>
      loadouts += ModelLoadout("*", parseWeaponList(m.group(1)))
    }

    everySpecificPattern.findAllMatchIn(cleanHtml).foreach { m =>
      val modelName = m.group(1).trim
      if (modelName.toLowerCase != "model") {
        val weapons = parseWeaponList(m.group(2))
        loadouts += ModelLoadout(modelName, weapons)
      }
    }

    theSpecificPattern.findAllMatchIn(cleanHtml).foreach { m =>
      val modelName = m.group(1).trim
      val weapons = parseWeaponList(m.group(2))
      loadouts += ModelLoadout(modelName, weapons)
    }

    eachSpecificPattern.findAllMatchIn(cleanHtml).foreach { m =>
      val modelName = m.group(1).trim
      if (modelName.toLowerCase != "model") {
        val weapons = parseWeaponList(m.group(2))
        loadouts += ModelLoadout(modelName, weapons)
      }
    }

    loadouts.toList
  }

  private def parseWeaponList(weaponString: String): List[String] = {
    val cleaned = weaponString.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim
    cleaned
      .split(";")
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(normalizeWeaponName)
      .toList
  }

  private def normalizeWeaponName(name: String): String = {
    val withoutQuantity = """^\d+\s+""".r.replaceFirstIn(name, "")
    withoutQuantity.trim.toLowerCase
  }

  def getBaseEquipmentForModel(loadouts: List[ModelLoadout], modelType: Option[String]): List[String] = {
    val universalLoadout = loadouts.find(_.modelPattern == "*")
    val specificLoadout = modelType.flatMap { mt =>
      loadouts.find(l => l.modelPattern != "*" && l.modelPattern.toLowerCase.contains(mt.toLowerCase))
    }

    specificLoadout.map(_.weapons).orElse(universalLoadout.map(_.weapons)).getOrElse(List.empty)
  }

  def hasUniversalLoadout(loadouts: List[ModelLoadout]): Boolean =
    loadouts.exists(_.modelPattern == "*")

  def hasSpecificLoadouts(loadouts: List[ModelLoadout]): Boolean =
    loadouts.exists(_.modelPattern != "*")
}
