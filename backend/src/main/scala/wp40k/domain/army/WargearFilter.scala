package wp40k.domain.army

import wp40k.domain.models.{Wargear, ParsedWargearOption, WargearAction, ModelLoadout, LoadoutParser, CompositionLineParser, ParsedCompositionLine, DatasheetAbility}

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
      val selectedChoiceIndexes = getSelectedChoiceIndex(selection.notes, selection.optionLine, parsedOptions)

      val parsed = parsedOptions.filter { p =>
        p.optionLine == selection.optionLine &&
          (p.choiceIndex == 0 || selectedChoiceIndexes.contains(p.choiceIndex))
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
    val (weaponRemovals, weaponAdditions) = calculateSelectionChanges(parsedOptions, selections, baseWeaponCounts)

    applyQuantities(allWargear, baseWeaponCounts, weaponRemovals, weaponAdditions) { weaponName =>
      determineModelType(weaponName, loadouts, weaponAdditions)
    }
  }

  def filterWargearWithDefaults(
    allWargear: List[Wargear],
    parsedOptions: List[ParsedWargearOption],
    selections: List[WargearSelection],
    defaults: List[WargearDefault],
    unitSize: Int,
    modelCountsByType: Map[String, Int] = Map.empty
  ): List[WargearWithQuantity] = {
    if (defaults.isEmpty || unitSize <= 0) {
      val filteredWargear = filterWargear(allWargear, parsedOptions, selections)
      return filteredWargear.map(w => WargearWithQuantity(w, unitSize.max(1), None))
    }

    val baseWeaponCounts = defaults.map(d => d.weapon -> d.count).toMap
    val modelTypes = defaults.flatMap(d => d.modelType.map(d.weapon -> _)).toMap
    val (weaponRemovals, weaponAdditions) = calculateSelectionChanges(parsedOptions, selections, baseWeaponCounts, modelCountsByType)

    applyQuantities(allWargear, baseWeaponCounts, weaponRemovals, weaponAdditions) { weaponName =>
      modelTypes.find { case (pattern, _) => matchesWeaponPrefix(weaponName, pattern) }.map(_._2)
    }
  }

  private def applyQuantities(
    allWargear: List[Wargear],
    baseWeaponCounts: Map[String, Int],
    weaponRemovals: Map[String, Int],
    weaponAdditions: Map[String, Int]
  )(resolveModelType: String => Option[String]): List[WargearWithQuantity] =
    allWargear.filter(_.name.isDefined).flatMap { wargear =>
      val weaponName = wargear.name.map(_.toLowerCase).getOrElse("")
      val baseCount = findCountByWeaponMatch(weaponName, baseWeaponCounts)
      val removedCount = findCountByWeaponMatch(weaponName, weaponRemovals)
      val addedCount = findCountByWeaponMatch(weaponName, weaponAdditions)
      val finalCount = calculateFinalCount(baseCount, removedCount, addedCount)

      Option.when(finalCount > 0) {
        WargearWithQuantity(wargear, finalCount, resolveModelType(weaponName))
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
        .map(l => weaponsToCountMap(l.weapons, unitSize))
        .getOrElse(Map.empty)
    } else if (hasSpecific) {
      val compositionLines = loadouts.filter(_.modelPattern != "*").map { l =>
        CompositionLineParser.parseLine(l.modelPattern)
          .flatMap(_.headOption)
          .getOrElse(ParsedCompositionLine(l.modelPattern, 1, 1))
      }

      val modelCounts = CompositionLineParser.calculateModelCounts(compositionLines, unitSize)

      loadouts.foldLeft(Map.empty[String, Int]) { (counts, l) =>
        val modelCount = if (l.modelPattern == "*") {
          unitSize
        } else {
          val matched = CompositionLineParser.matchModelTarget(l.modelPattern, compositionLines)
          matched.map(m => modelCounts.getOrElse(m.modelName.toLowerCase, 1)).getOrElse(1)
        }

        l.weapons.foldLeft(counts) { (c, w) =>
          c + (w -> (c.getOrElse(w, 0) + modelCount))
        }
      }
    } else {
      Map.empty
    }
  }

  private def weaponsToCountMap(weapons: List[String], count: Int): Map[String, Int] =
    weapons.map(_ -> count).toMap

  private def calculateSelectionChanges(
    parsedOptions: List[ParsedWargearOption],
    selections: List[WargearSelection],
    baseWeaponCounts: Map[String, Int] = Map.empty,
    modelCountsByType: Map[String, Int] = Map.empty
  ): (Map[String, Int], Map[String, Int]) = {
    val activeSelections = selections.filter(_.selected)

    activeSelections.foldLeft((Map.empty[String, Int], Map.empty[String, Int])) { case ((removals, additions), selection) =>
      val selectedChoiceIndexes = getSelectedChoiceIndex(selection.notes, selection.optionLine, parsedOptions)

      val parsed = parsedOptions.filter { p =>
        p.optionLine == selection.optionLine &&
          (p.choiceIndex == 0 || selectedChoiceIndexes.contains(p.choiceIndex))
      }

      val removeAllCount = resolveRemoveAllCount(parsed, baseWeaponCounts, modelCountsByType)
      applyParsedActions(parsed, removals, additions, removeAllCount, baseWeaponCounts)
    }
  }

  private def resolveRemoveAllCount(
    parsed: List[ParsedWargearOption],
    baseWeaponCounts: Map[String, Int],
    modelCountsByType: Map[String, Int]
  ): Option[Int] =
    parsed
      .find(p => p.action == WargearAction.Remove && p.maxCount == 0)
      .map { removeOpt =>
        removeOpt.modelTarget match {
          case Some(target) =>
            val targetLower = target.toLowerCase
            modelCountsByType
              .find { case (k, _) => k.contains(targetLower) || targetLower.contains(k) }
              .map(_._2)
              .getOrElse(findCountByWeaponMatch(removeOpt.weaponName.toLowerCase, baseWeaponCounts))
          case None =>
            findCountByWeaponMatch(removeOpt.weaponName.toLowerCase, baseWeaponCounts)
        }
      }
      .filter(_ > 0)

  private def applyParsedActions(
    parsed: List[ParsedWargearOption],
    removals: Map[String, Int],
    additions: Map[String, Int],
    removeAllCount: Option[Int],
    baseWeaponCounts: Map[String, Int]
  ): (Map[String, Int], Map[String, Int]) =
    parsed.foldLeft((removals, additions)) { case ((rem, add), p) =>
      val weaponName = p.weaponName.toLowerCase
      val count = if (p.maxCount > 0) p.maxCount else removeAllCount.getOrElse(1)

      p.action match {
        case WargearAction.Remove =>
          val current = rem.getOrElse(weaponName, 0)
          val base = findCountByWeaponMatch(weaponName, baseWeaponCounts)
          val newCount = if (base > 0) (current + count).min(base) else current + count
          (rem + (weaponName -> newCount), add)
        case WargearAction.Add =>
          (rem, add + (weaponName -> (add.getOrElse(weaponName, 0) + count)))
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

  private def findCountByWeaponMatch(weaponName: String, counts: Map[String, Int]): Int =
    counts.find { case (pattern, _) => matchesWeaponPrefix(weaponName, pattern) }
      .map(_._2)
      .getOrElse(0)

  private def calculateFinalCount(baseCount: Int, removedCount: Int, addedCount: Int): Int =
    if (baseCount > 0) (baseCount - removedCount + addedCount).max(0)
    else if (addedCount > 0) addedCount
    else 0

  private def matchesWeaponPrefix(weaponName: String, prefix: String): Boolean = {
    val normalizedWeapon = weaponName.toLowerCase
    val normalizedPrefix = prefix.toLowerCase
    normalizedWeapon == normalizedPrefix || normalizedWeapon.startsWith(normalizedPrefix + " ")
  }

  def filterAbilities(
    abilities: List[DatasheetAbility],
    parsedOptions: List[ParsedWargearOption],
    selections: List[WargearSelection]
  ): List[DatasheetAbility] = {
    if (parsedOptions.isEmpty) return abilities

    val allOptionNames = parsedOptions.map(_.weaponName.toLowerCase).toSet
    val (_, activeAdds) = calculateSelectionChanges(parsedOptions, selections)

    abilities.filter { a =>
      a.abilityType match {
        case Some("Wargear") =>
          a.name match {
            case Some(name) =>
              val lowerName = name.toLowerCase
              if (allOptionNames.exists(opt => matchesWeaponPrefix(lowerName, opt) || matchesWeaponPrefix(opt, lowerName)))
                activeAdds.keys.exists(add => matchesWeaponPrefix(lowerName, add) || matchesWeaponPrefix(add, lowerName))
              else
                true
            case None => true
          }
        case _ => true
      }
    }
  }

  private def getSelectedChoiceIndex(
    notes: Option[String],
    optionLine: Int,
    parsedOptions: List[ParsedWargearOption]
  ): List[Int] = {
    notes.fold(List.empty[Int]) { n =>
      val weapons = n.split('|').map(_.toLowerCase.trim.replaceAll("^\\d+\\s+", "")).toList
      val addOptions = parsedOptions.filter { p =>
        p.optionLine == optionLine && p.action == WargearAction.Add && p.choiceIndex > 0
      }
      weapons.flatMap(w => addOptions.find(opt => opt.weaponName.toLowerCase == w || opt.weaponName.toLowerCase.startsWith(w + " ")).map(_.choiceIndex)).distinct
    }
  }
}
