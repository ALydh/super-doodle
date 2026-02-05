package wahapedia.domain.army

import wahapedia.domain.types.{FactionId, BattleSize}

sealed trait AllyType
object AllyType {
  case object Freeblades extends AllyType
  case object Dreadblades extends AllyType
  case object DaemonicPact extends AllyType
  case object AssignedAgents extends AllyType
}

case class AlliedFaction(
  factionId: FactionId,
  allyType: AllyType
)

case class AllyLimits(
  maxTitanic: Int,
  maxNonTitanic: Int,
  maxPoints: Option[Int],
  maxUnits: Option[Int]
)

object AllyRules {

  val ImperiumKeyword = "IMPERIUM"
  val ChaosKeyword = "CHAOS"

  val ImperialKnightsFaction: FactionId = FactionId("QI")
  val ChaosKnightsFaction: FactionId = FactionId("QT")
  val ChaosDaemonsFaction: FactionId = FactionId("CD")
  val ImperialAgentsFaction: FactionId = FactionId("AoI")

  val FreebladesRule = AlliedFaction(ImperialKnightsFaction, AllyType.Freeblades)
  val DreadbladesRule = AlliedFaction(ChaosKnightsFaction, AllyType.Dreadblades)
  val DaemonicPactRule = AlliedFaction(ChaosDaemonsFaction, AllyType.DaemonicPact)
  val AssignedAgentsRule = AlliedFaction(ImperialAgentsFaction, AllyType.AssignedAgents)

  def daemonicPactPointsLimit(battleSize: BattleSize): Int = battleSize match {
    case BattleSize.Incursion => 250
    case BattleSize.StrikeForce => 500
    case BattleSize.Onslaught => 750
  }

  def assignedAgentsUnitLimit(battleSize: BattleSize): Int = battleSize match {
    case BattleSize.Incursion => 2
    case BattleSize.StrikeForce => 4
    case BattleSize.Onslaught => 6
  }

  def allowedAllies(superKeywords: Set[String]): List[AlliedFaction] = {
    val hasImperium = superKeywords.exists(_.equalsIgnoreCase(ImperiumKeyword))
    val hasChaos = superKeywords.exists(_.equalsIgnoreCase(ChaosKeyword))

    val imperiumAllies = if (hasImperium) List(FreebladesRule, AssignedAgentsRule) else Nil
    val chaosAllies = if (hasChaos) List(DreadbladesRule, DaemonicPactRule) else Nil

    imperiumAllies ++ chaosAllies
  }

  def limitsFor(allyType: AllyType, battleSize: BattleSize): AllyLimits = allyType match {
    case AllyType.Freeblades =>
      AllyLimits(maxTitanic = 1, maxNonTitanic = 3, maxPoints = None, maxUnits = None)
    case AllyType.Dreadblades =>
      AllyLimits(maxTitanic = 1, maxNonTitanic = 3, maxPoints = None, maxUnits = None)
    case AllyType.DaemonicPact =>
      AllyLimits(maxTitanic = 0, maxNonTitanic = Int.MaxValue, maxPoints = Some(daemonicPactPointsLimit(battleSize)), maxUnits = None)
    case AllyType.AssignedAgents =>
      AllyLimits(maxTitanic = 0, maxNonTitanic = Int.MaxValue, maxPoints = None, maxUnits = Some(assignedAgentsUnitLimit(battleSize)))
  }
}
