package wahapedia.engine.effect

import wahapedia.engine.state.{Phase, UnitId}

enum PipelineStep:
  case HitRoll
  case WoundRoll
  case SaveRoll
  case DamageStep
  case FeelNoPain

enum RerollTarget:
  case Ones
  case Failed
  case All

enum CriticalEffect:
  case AutoWound
  case ExtraHits(n: Int)
  case MortalWounds(dmgPerWound: Int)

sealed trait Effect

case class ModifyRoll(
  step: PipelineStep,
  modifier: Int,
  condition: EffectCondition = Always
) extends Effect

case class RerollDice(
  step: PipelineStep,
  which: RerollTarget,
  condition: EffectCondition = Always
) extends Effect

case class OnCritical(
  step: PipelineStep,
  effect: CriticalEffect,
  condition: EffectCondition = Always
) extends Effect

case class SetMinimumRoll(
  step: PipelineStep,
  target: Int,
  condition: EffectCondition = Always
) extends Effect

case class AutoPass(
  step: PipelineStep,
  condition: EffectCondition = Always
) extends Effect

case class ExtraAttacks(
  n: Int,
  condition: EffectCondition = Always
) extends Effect

case class MortalWoundsOnDamage(
  amount: Int,
  condition: EffectCondition = Always
) extends Effect

case class FeelNoPainEffect(
  target: Int,
  condition: EffectCondition = Always
) extends Effect

case class AuraEffect(
  rangeInches: Double,
  targetFilter: UnitFilter,
  grantedEffect: Effect
) extends Effect

sealed trait EffectCondition
case object Always extends EffectCondition
case class HasKeyword(keyword: String) extends EffectCondition
case class TargetHasKeyword(keyword: String) extends EffectCondition
case class DidNotMove(dummy: Unit = ()) extends EffectCondition
case class DidAdvance(dummy: Unit = ()) extends EffectCondition
case class PhaseIs(phase: Phase) extends EffectCondition
case class And(conditions: List[EffectCondition]) extends EffectCondition
case class Or(conditions: List[EffectCondition]) extends EffectCondition

enum UnitFilter:
  case Friendly
  case Enemy
  case FriendlyWithKeyword(keyword: String)
  case EnemyWithKeyword(keyword: String)
