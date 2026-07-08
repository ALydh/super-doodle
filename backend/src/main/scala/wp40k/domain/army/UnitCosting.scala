package wp40k.domain.army

import wp40k.domain.types.DatasheetId
import wp40k.domain.models.UnitCost

object UnitCosting {

  def withOrdinals(units: List[ArmyUnit]): List[(ArmyUnit, Int)] =
    units
      .foldLeft((Map.empty[DatasheetId, Int], List.empty[(ArmyUnit, Int)])) {
        case ((counts, acc), unit) =>
          val ordinal = counts.getOrElse(unit.datasheetId, 0) + 1
          (counts.updated(unit.datasheetId, ordinal), (unit, ordinal) :: acc)
      }
      ._2
      .reverse

  def costFor(costs: List[UnitCost], line: Int, ordinal: Int): Option[UnitCost] = {
    val forLine = costs.filter(_.line == line)
    forLine
      .find(c => ordinal >= c.minCount && c.maxCount.forall(ordinal <= _))
      .orElse(forLine.headOption)
  }
}
