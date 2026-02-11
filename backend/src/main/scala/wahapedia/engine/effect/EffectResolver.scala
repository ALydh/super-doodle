package wahapedia.engine.effect

import wahapedia.engine.state.{UnitState, GameState}
import wahapedia.engine.combat.WeaponProfile
import wahapedia.engine.spatial.Geometry

object EffectResolver:

  def collectAttackEffects(
    state: GameState,
    attacker: UnitState,
    target: UnitState,
    weapon: WeaponProfile
  ): List[Effect] =
    val weaponEffects = weapon.abilities
    val attackerEffects = attacker.activeEffects.map(_.effect)
    val targetDefensiveEffects = collectDefensiveEffects(target)
    val auraEffects = collectAuraEffects(state, attacker, target)

    weaponEffects ++ attackerEffects ++ targetDefensiveEffects ++ auraEffects

  private def collectDefensiveEffects(target: UnitState): List[Effect] =
    target.activeEffects.collect:
      case ActiveEffect(e: ModifyRoll, _, _) if e.step == PipelineStep.HitRoll => e
      case ActiveEffect(e: FeelNoPainEffect, _, _) => e

  private def collectAuraEffects(
    state: GameState,
    attacker: UnitState,
    target: UnitState
  ): List[Effect] =
    state.board.aliveUnits.flatMap: unit =>
      unit.activeEffects.collect:
        case ActiveEffect(AuraEffect(range, filter, granted), _, _)
          if isAuraTarget(unit, attacker, target, range, filter, state) =>
            granted
    .toList

  private def isAuraTarget(
    auraSource: UnitState,
    attacker: UnitState,
    target: UnitState,
    range: Double,
    filter: UnitFilter,
    state: GameState
  ): Boolean =
    filter match
      case UnitFilter.Friendly =>
        auraSource.owner == attacker.owner &&
          Geometry.closestModelDistance(auraSource.modelPositions, attacker.modelPositions) <= range
      case UnitFilter.Enemy =>
        auraSource.owner != target.owner &&
          Geometry.closestModelDistance(auraSource.modelPositions, target.modelPositions) <= range
      case UnitFilter.FriendlyWithKeyword(_) =>
        auraSource.owner == attacker.owner &&
          Geometry.closestModelDistance(auraSource.modelPositions, attacker.modelPositions) <= range
      case UnitFilter.EnemyWithKeyword(_) =>
        auraSource.owner != target.owner &&
          Geometry.closestModelDistance(auraSource.modelPositions, target.modelPositions) <= range
