package wahapedia.engine.effect

enum EffectDuration:
  case Permanent
  case UntilEndOfPhase
  case UntilEndOfTurn
  case UntilEndOfBattleRound
  case WhileAttacking
  case WhileBeingAttacked

case class ActiveEffect(
  effect: Effect,
  duration: EffectDuration,
  sourceId: String
)
