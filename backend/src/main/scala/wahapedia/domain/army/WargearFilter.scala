package wahapedia.domain.army

import wahapedia.domain.models.{Wargear, ParsedWargearOption, WargearAction, ModelLoadout, LoadoutParser}

case class WargearWithQuantity(
  wargear: Wargear,
  quantity: Int,
  modelType: Option[String]
)

case class WargearDefault(weapon: String, count: Int, modelType: Option[String])

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

    val initialWeaponMap = baseWeapons.flatMap(w => w.name.map(n => n.toLowerCase -> w)).toMap

    val activeSelections = selections.filter(_.selected)
    val finalWeaponMap = activeSelections.foldLeft(initialWeaponMap) { (weaponMap, selection) =>
      val selectedChoiceIndex = getSelectedChoiceIndex(selection.notes, selection.optionLine, parsedOptions)

      val parsed = parsedOptions.filter { p =>
        p.optionLine == selection.optionLine &&
          (p.choiceIndex == 0 || selectedChoiceIndex.contains(p.choiceIndex))
      }

      parsed.foldLeft(weaponMap) { (wm, p) =>
        val targetName = p.weaponName.toLowerCase
        p.action match {
          case WargearAction.Remove =>
            wm.filterNot { case (_, w) =>
              w.name.exists(n => matchesWeaponPrefix(n, targetName))
            }
          case WargearAction.Add =>
            val weapons = allWargear.filter(w => w.name.exists(n => matchesWeaponPrefix(n, targetName)))
            weapons.foldLeft(wm) { (m, w) =>
              w.name.fold(m)(n => m + (n.toLowerCase -> w))
            }
        }
      }
    }

    finalWeaponMap.values.toList
  }

  def filterWargearWithQuantities(
    allWargear: List[Wargear],
    parsedOptions: List[ParsedWargearOption],
    selections: List[WargearSelection],
    loadouts: List[ModelLoadout],
    unitSize: Int
  ): List[WargearWithQuantity] = {
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

  def filterWargearWithDefaults(
    allWargear: List[Wargear],
    parsedOptions: List[ParsedWargearOption],
    selections: List[WargearSelection],
    defaults: List[WargearDefault],
    unitSize: Int
  ): List[WargearWithQuantity] = {
    if (defaults.isEmpty || unitSize <= 0) {
      val filteredWargear = filterWargear(allWargear, parsedOptions, selections)
      return filteredWargear.map(w => WargearWithQuantity(w, unitSize.max(1), None))
    }

    val baseWeaponCounts = defaults.map(d => d.weapon -> d.count).toMap
    val modelTypes = defaults.flatMap(d => d.modelType.map(d.weapon -> _)).toMap
    val (weaponRemovals, weaponAdditions) = calculateSelectionChanges(parsedOptions, selections)

    allWargear.filter(_.name.isDefined).flatMap { wargear =>
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
        val modelType = modelTypes.find { case (pattern, _) =>
          matchesWeaponPrefix(weaponName, pattern)
        }.map(_._2)
        Some(WargearWithQuantity(wargear, finalCount, modelType))
      } else {
        None
      }
    }
  }

  private def calculateBaseWeaponCounts(
    loadouts: List[ModelLoadout],
    unitSize: Int,
    hasUniversal: Boolean,
    hasSpecific: Boolean
  ): Map[String, Int] = {
    if (hasUniversal && !hasSpecific) {
      loadouts.find(_.modelPattern == "*")
        .map(l => l.weapons.map(_ -> unitSize).toMap)
        .getOrElse(Map.empty)
    } else if (hasSpecific) {
      val sergeantCount = 1
      val trooperCount = unitSize - sergeantCount

      loadouts.foldLeft(Map.empty[String, Int]) { (counts, l) =>
        val modelCount = if (l.modelPattern == "*") {
          unitSize
        } else if (isSergeantPattern(l.modelPattern)) {
          sergeantCount
        } else {
          trooperCount
        }

        l.weapons.foldLeft(counts) { (c, w) =>
          c + (w -> (c.getOrElse(w, 0) + modelCount))
        }
      }
    } else {
      Map.empty
    }
  }

  private def isSergeantPattern(pattern: String): Boolean = {
    val lower = pattern.toLowerCase
    lower.contains("sergeant") || lower.contains("champion") || lower.contains("leader") ||
      lower.contains("captain") ||
      lower.contains("boss nob") || lower.contains("nob") ||
      lower.contains("shas'ui") || lower.contains("shas'vre") ||
      lower.contains("exarch") ||
      lower.contains("superior") ||
      lower.contains("acothyst") || lower.contains("sybarite") || lower.contains("hekatrix") ||
      lower.contains("helliarch") || lower.contains("solarite") || lower.contains("klaivex") ||
      lower.contains("alpha") || lower.contains("princeps") ||
      lower.contains("theyn") || lower.contains("hesyr") ||
      lower.contains("felarch") ||
      lower.contains("sorcerer") ||
      lower.contains("kill-broker")
  }

  private def calculateSelectionChanges(
    parsedOptions: List[ParsedWargearOption],
    selections: List[WargearSelection]
  ): (Map[String, Int], Map[String, Int]) = {
    val activeSelections = selections.filter(_.selected)

    activeSelections.foldLeft((Map.empty[String, Int], Map.empty[String, Int])) { case ((removals, additions), selection) =>
      val selectedChoiceIndex = getSelectedChoiceIndex(selection.notes, selection.optionLine, parsedOptions)

      val parsed = parsedOptions.filter { p =>
        p.optionLine == selection.optionLine &&
          (p.choiceIndex == 0 || selectedChoiceIndex.contains(p.choiceIndex))
      }

      parsed.foldLeft((removals, additions)) { case ((rem, add), p) =>
        val weaponName = p.weaponName.toLowerCase
        val count = if (p.maxCount > 0) p.maxCount else 1

        p.action match {
          case WargearAction.Remove =>
            (rem + (weaponName -> (rem.getOrElse(weaponName, 0) + count)), add)
          case WargearAction.Add =>
            (rem, add + (weaponName -> (add.getOrElse(weaponName, 0) + count)))
        }
      }
    }
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
