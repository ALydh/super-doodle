package wahapedia.domain.types

enum BattleSize(val maxPoints: Int):
  case Incursion extends BattleSize(1000)
  case StrikeForce extends BattleSize(2000)
  case Onslaught extends BattleSize(3000)
