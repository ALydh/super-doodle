package wahapedia.domain.models

case class ParsedCompositionLine(
  modelName: String,
  minCount: Int,
  maxCount: Int,
  groupIndex: Int = 0
) {
  def isLeader: Boolean = minCount == 1 && maxCount == 1
  def isVariable: Boolean = minCount != maxCount
}

object CompositionLineParser {

  private val rangePattern = """^(\d+)-(\d+)\s+(.+)$""".r
  private val singlePattern = """^(\d+)\s+(.+)$""".r
  private val compoundPattern = """^(\d+)\s+(.+?)\s+and\s+(\d+)\s+(.+)$""".r
  private val htmlTagPattern = """<[^>]*>""".r

  def parseLine(description: String): Option[List[ParsedCompositionLine]] = {
    if (description == null || description.isEmpty) return None

    val cleaned = cleanDescription(description)
    if (cleaned.equalsIgnoreCase("OR")) return None

    cleaned match {
      case compoundPattern(count1, name1, count2, name2) =>
        val c1 = count1.toInt
        val c2 = count2.toInt
        Some(List(
          ParsedCompositionLine(name1.trim, c1, c1),
          ParsedCompositionLine(name2.trim, c2, c2)
        ))
      case rangePattern(min, max, name) =>
        Some(List(ParsedCompositionLine(name.trim, min.toInt, max.toInt)))
      case singlePattern(count, name) =>
        val c = count.toInt
        Some(List(ParsedCompositionLine(name.trim, c, c)))
      case _ =>
        None
    }
  }

  def parseAll(descriptions: List[String]): List[ParsedCompositionLine] =
    descriptions.flatMap(parseLine).flatten

  def calculateModelCounts(
    lines: List[ParsedCompositionLine],
    totalSize: Int
  ): Map[String, Int] = {
    if (lines.isEmpty) return Map.empty

    val leaders = lines.filter(_.isLeader)
    val variable = lines.filter(_.isVariable)
    val fixedNonLeaders = lines.filter(l => !l.isLeader && !l.isVariable)

    val leaderCount = leaders.map(_.maxCount).sum
    val fixedCount = fixedNonLeaders.map(_.maxCount).sum
    val remainingForVariable = totalSize - leaderCount - fixedCount

    val counts = scala.collection.mutable.Map[String, Int]()

    leaders.foreach(l => counts(l.modelName.toLowerCase) = l.maxCount)
    fixedNonLeaders.foreach(l => counts(l.modelName.toLowerCase) = l.maxCount)

    if (variable.nonEmpty && remainingForVariable > 0) {
      val totalVariableMin = variable.map(_.minCount).sum
      if (variable.size == 1) {
        counts(variable.head.modelName.toLowerCase) = remainingForVariable
      } else {
        variable.foreach { v =>
          val proportion = if (totalVariableMin > 0) {
            v.minCount.toDouble / totalVariableMin
          } else {
            1.0 / variable.size
          }
          counts(v.modelName.toLowerCase) = (remainingForVariable * proportion).toInt.max(v.minCount)
        }
      }
    }

    counts.toMap
  }

  def matchModelTarget(
    target: String,
    lines: List[ParsedCompositionLine]
  ): Option[ParsedCompositionLine] = {
    val normalizedTarget = target.toLowerCase.trim
    lines.find { line =>
      val normalizedName = line.modelName.toLowerCase
      normalizedName == normalizedTarget ||
        normalizedName.contains(normalizedTarget) ||
        normalizedTarget.contains(normalizedName)
    }
  }

  def findLeaderModel(lines: List[ParsedCompositionLine]): Option[ParsedCompositionLine] =
    lines.find(_.isLeader)

  def findTrooperModels(lines: List[ParsedCompositionLine]): List[ParsedCompositionLine] =
    lines.filterNot(_.isLeader)

  def selectGroupForSize(
    lines: List[ParsedCompositionLine],
    targetSize: Int
  ): List[ParsedCompositionLine] = {
    if (lines.isEmpty) return List.empty

    val byGroup = lines.groupBy(_.groupIndex)
    val sortedGroups = byGroup.toList.sortBy(_._1)

    sortedGroups.find { case (_, groupLines) =>
      val total = groupLines.map(l => if (l.isVariable) l.maxCount else l.maxCount).sum
      targetSize <= total
    }.map(_._2).getOrElse(sortedGroups.lastOption.map(_._2).getOrElse(List.empty))
  }

  private def cleanDescription(description: String): String = {
    val withoutHtml = htmlTagPattern.replaceAllIn(description, "")
    withoutHtml.trim
  }
}
