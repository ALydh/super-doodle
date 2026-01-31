package wahapedia.domain.army

import wahapedia.domain.models.{Wargear, ParsedWargearOption, WargearAction, LoadoutParser, ModelLoadout}

case class WargearWithQuantity(
  wargear: Wargear,
  quantity: Int,
  modelType: Option[String]
)

object WargearFilter {

  def filterWargear(
    allWargear: List[Wargear],
    parsedOptions: List[ParsedWargearOption],
    selections: List[WargearSelection]
  ): List[Wargear] = {
    if (parsedOptions.isEmpty) {
      return allWargear.filter(_.name.isDefined)
    }

    val addedWeapons = parsedOptions.collect { case p if p.action == WargearAction.Add => p.weaponName.toLowerCase }.toSet
    val removedWeapons = parsedOptions.collect { case p if p.action == WargearAction.Remove => p.weaponName.toLowerCase }.toSet

    val baseWeapons = allWargear.filter { w =>
      w.name.exists { name =>
        val isOptional = addedWeapons.exists(added => matchesWeaponPrefix(name, added)) &&
          !removedWeapons.exists(removed => matchesWeaponPrefix(name, removed))
        !isOptional
      }
    }

    var weaponMap = baseWeapons.flatMap(w => w.name.map(n => n.toLowerCase -> w)).toMap

    val activeSelections = selections.filter(_.selected)
    for (selection <- activeSelections) {
      val selectedChoiceIndex = getSelectedChoiceIndex(selection.notes, selection.optionLine, parsedOptions)

      val parsed = parsedOptions.filter { p =>
        p.optionLine == selection.optionLine &&
          (p.choiceIndex == 0 || selectedChoiceIndex.contains(p.choiceIndex))
      }

      for (p <- parsed) {
        val targetName = p.weaponName.toLowerCase
        p.action match {
          case WargearAction.Remove =>
            weaponMap = weaponMap.filterNot { case (_, w) =>
              w.name.exists(n => matchesWeaponPrefix(n, targetName))
            }
          case WargearAction.Add =>
            val weapons = allWargear.filter(w => w.name.exists(n => matchesWeaponPrefix(n, targetName)))
            for (w <- weapons) {
              w.name.foreach(n => weaponMap = weaponMap + (n.toLowerCase -> w))
            }
        }
      }
    }

    weaponMap.values.toList
  }

  def filterWargearWithQuantities(
    allWargear: List[Wargear],
    parsedOptions: List[ParsedWargearOption],
    selections: List[WargearSelection],
    loadout: Option[String],
    unitSize: Int
  ): List[WargearWithQuantity] = {
    val loadouts = loadout.map(LoadoutParser.parse).getOrElse(List.empty)
    val hasUniversal = LoadoutParser.hasUniversalLoadout(loadouts)
    val hasSpecific = LoadoutParser.hasSpecificLoadouts(loadouts)

    if (loadouts.isEmpty || unitSize <= 0) {
      val filteredWargear = filterWargear(allWargear, parsedOptions, selections)
      return filteredWargear.map(w => WargearWithQuantity(w, unitSize.max(1), None))
    }

    val baseWeaponCounts = calculateBaseWeaponCounts(loadouts, unitSize, hasUniversal, hasSpecific)
    val (weaponRemovals, weaponAdditions) = calculateSelectionChanges(parsedOptions, selections)

    val wargearWithQuantities = allWargear.filter(_.name.isDefined).flatMap { wargear =>
      val weaponName = wargear.name.map(_.toLowerCase).getOrElse("")

      val baseCount = baseWeaponCounts.find { case (pattern, _) =>
        matchesWeaponPrefix(weaponName, pattern)
      }.map(_._2).getOrElse(0)

      val removedCount = weaponRemovals.find { case (pattern, _) =>
        matchesWeaponPrefix(weaponName, pattern)
      }.map(_._2).getOrElse(0)

      val addedCount = weaponAdditions.find { case (pattern, _) =>
        matchesWeaponPrefix(weaponName, pattern)
      }.map(_._2).getOrElse(0)

      val finalCount = if (baseCount > 0) {
        (baseCount - removedCount + addedCount).max(0)
      } else if (addedCount > 0) {
        addedCount
      } else {
        0
      }

      if (finalCount > 0) {
        val modelType = determineModelType(weaponName, loadouts, weaponAdditions)
        Some(WargearWithQuantity(wargear, finalCount, modelType))
      } else {
        None
      }
    }

    wargearWithQuantities
  }

  private def calculateBaseWeaponCounts(
    loadouts: List[ModelLoadout],
    unitSize: Int,
    hasUniversal: Boolean,
    hasSpecific: Boolean
  ): Map[String, Int] = {
    val counts = scala.collection.mutable.Map[String, Int]()

    if (hasUniversal && !hasSpecific) {
      loadouts.find(_.modelPattern == "*").foreach { l =>
        l.weapons.foreach(w => counts(w) = unitSize)
      }
    } else if (hasSpecific) {
      val sergeantCount = 1
      val trooperCount = unitSize - sergeantCount

      loadouts.foreach { l =>
        val modelCount = if (l.modelPattern == "*") {
          unitSize
        } else if (isSergeantPattern(l.modelPattern)) {
          sergeantCount
        } else {
          trooperCount
        }

        l.weapons.foreach { w =>
          counts(w) = counts.getOrElse(w, 0) + modelCount
        }
      }
    }

    counts.toMap
  }

  private def isSergeantPattern(pattern: String): Boolean = {
    val lower = pattern.toLowerCase
    lower.contains("sergeant") || lower.contains("champion") || lower.contains("leader") ||
      lower.contains("captain") || lower.contains("veteran sergeant")
  }

  private def calculateSelectionChanges(
    parsedOptions: List[ParsedWargearOption],
    selections: List[WargearSelection]
  ): (Map[String, Int], Map[String, Int]) = {
    val removals = scala.collection.mutable.Map[String, Int]()
    val additions = scala.collection.mutable.Map[String, Int]()

    val activeSelections = selections.filter(_.selected)
    for (selection <- activeSelections) {
      val selectedChoiceIndex = getSelectedChoiceIndex(selection.notes, selection.optionLine, parsedOptions)

      val parsed = parsedOptions.filter { p =>
        p.optionLine == selection.optionLine &&
          (p.choiceIndex == 0 || selectedChoiceIndex.contains(p.choiceIndex))
      }

      for (p <- parsed) {
        val weaponName = p.weaponName.toLowerCase
        val count = if (p.maxCount > 0) p.maxCount else 1

        p.action match {
          case WargearAction.Remove =>
            removals(weaponName) = removals.getOrElse(weaponName, 0) + count
          case WargearAction.Add =>
            additions(weaponName) = additions.getOrElse(weaponName, 0) + count
        }
      }
    }

    (removals.toMap, additions.toMap)
  }

  private def determineModelType(
    weaponName: String,
    loadouts: List[ModelLoadout],
    additions: Map[String, Int]
  ): Option[String] = {
    val fromLoadout = loadouts.find { l =>
      l.modelPattern != "*" && l.weapons.exists(w => matchesWeaponPrefix(weaponName, w))
    }.map(_.modelPattern)

    if (fromLoadout.isDefined) fromLoadout
    else if (additions.keys.exists(k => matchesWeaponPrefix(weaponName, k))) None
    else None
  }

  private def matchesWeaponPrefix(weaponName: String, prefix: String): Boolean = {
    val normalizedWeapon = weaponName.toLowerCase
    val normalizedPrefix = prefix.toLowerCase
    normalizedWeapon == normalizedPrefix || normalizedWeapon.startsWith(normalizedPrefix + " ")
  }

  private def getSelectedChoiceIndex(
    notes: Option[String],
    optionLine: Int,
    parsedOptions: List[ParsedWargearOption]
  ): Option[Int] = {
    notes.flatMap { n =>
      val normalizedNotes = n.toLowerCase.trim
      val addOptions = parsedOptions.filter { p =>
        p.optionLine == optionLine && p.action == WargearAction.Add && p.choiceIndex > 0
      }
      addOptions.find(opt => normalizedNotes.contains(opt.weaponName.toLowerCase)).map(_.choiceIndex)
    }
  }
}
