package wahapedia.engine.combat

import wahapedia.engine.effect.*
import wahapedia.engine.state.{UnitState, ModelState}

case class WeaponProfile(
  name: String,
  attacks: String,
  ballisticSkill: Option[Int],
  strength: Int,
  armorPenetration: Int,
  damage: String,
  abilities: List[Effect] = Nil,
  isMelee: Boolean = false
)

case class AttackContext(
  attacker: UnitState,
  target: UnitState,
  weapon: WeaponProfile,
  effects: List[Effect],
  dice: DiceRoller,
  targetToughness: Int,
  targetSave: Int,
  targetInvuln: Option[Int] = None,
  isInCover: Boolean = false
)

case class AttackResult(
  totalAttacks: Int,
  hits: Int,
  wounds: Int,
  unsavedWounds: Int,
  totalDamage: Int,
  mortalWounds: Int,
  weaponName: String
)

object AttackPipeline:

  def resolve(ctx: AttackContext): AttackResult =
    val allEffects = ctx.weapon.abilities ++ ctx.effects
    val numAttacks = resolveAttackCount(ctx, allEffects)
    val (hits, autoWounds, mortalsFromHits) = rollHits(ctx, allEffects, numAttacks)
    val (wounds, mortalsFromWounds) = rollWounds(ctx, allEffects, hits, autoWounds)
    val unsaved = rollSaves(ctx, allEffects, wounds)
    val rawDamage = rollDamage(ctx, allEffects, unsaved)
    val afterFnp = applyFeelNoPain(ctx, allEffects, rawDamage)
    val totalMortals = mortalsFromHits + mortalsFromWounds

    AttackResult(
      totalAttacks = numAttacks,
      hits = hits + autoWounds,
      wounds = wounds,
      unsavedWounds = unsaved,
      totalDamage = afterFnp,
      mortalWounds = totalMortals,
      weaponName = ctx.weapon.name
    )

  private def resolveAttackCount(ctx: AttackContext, effects: List[Effect]): Int =
    val base = ctx.dice.rollNotation(ctx.weapon.attacks)
    val extra = effects.collect { case ExtraAttacks(n, cond) if conditionMet(cond, ctx) => n }.sum
    base + extra

  private def rollHits(
    ctx: AttackContext,
    effects: List[Effect],
    numAttacks: Int
  ): (Int, Int, Int) =
    val autoHit = effects.exists:
      case AutoPass(PipelineStep.HitRoll, cond) => conditionMet(cond, ctx)
      case _ => false

    if autoHit then return (numAttacks, 0, 0)

    val bs = ctx.weapon.ballisticSkill.getOrElse(4)
    val modifier = rollModifier(PipelineStep.HitRoll, effects, ctx)
    val reroll = getReroll(PipelineStep.HitRoll, effects, ctx)
    val crits = getCriticals(PipelineStep.HitRoll, effects, ctx)

    var hits = 0
    var autoWounds = 0
    var mortalWounds = 0

    (1 to numAttacks).foreach: _ =>
      val roll = ctx.dice.rollD6()
      val isCrit = roll == 6

      val effectiveRoll = if isCrit then 6 else (roll + modifier).max(1).min(6)
      val target = bs

      val finalRoll =
        if effectiveRoll >= target then effectiveRoll
        else
          reroll match
            case Some(RerollTarget.All) => ctx.dice.rollD6() + modifier
            case Some(RerollTarget.Failed) => ctx.dice.rollD6() + modifier
            case Some(RerollTarget.Ones) if roll == 1 => ctx.dice.rollD6() + modifier
            case _ => effectiveRoll

      val finalIsCrit = roll == 6 || (finalRoll != effectiveRoll && finalRoll == 6)
      val passed = if finalIsCrit then true else finalRoll >= target

      if passed then
        if finalIsCrit then
          crits.foreach:
            case CriticalEffect.AutoWound => autoWounds += 1
            case CriticalEffect.ExtraHits(n) => hits += n
            case CriticalEffect.MortalWounds(dmg) => mortalWounds += dmg
          hits += 1
        else
          hits += 1

    (hits, autoWounds, mortalWounds)

  private def rollWounds(
    ctx: AttackContext,
    effects: List[Effect],
    hits: Int,
    autoWounds: Int
  ): (Int, Int) =
    val baseTarget = woundTarget(ctx.weapon.strength, ctx.targetToughness)
    val modifier = rollModifier(PipelineStep.WoundRoll, effects, ctx)
    val reroll = getReroll(PipelineStep.WoundRoll, effects, ctx)
    val crits = getCriticals(PipelineStep.WoundRoll, effects, ctx)

    val overrideTarget = effects.collectFirst:
      case SetMinimumRoll(PipelineStep.WoundRoll, t, cond) if conditionMet(cond, ctx) => t

    val target = overrideTarget.map(_.min(baseTarget)).getOrElse(baseTarget)

    var wounds = autoWounds
    var mortalWounds = 0

    (1 to hits).foreach: _ =>
      val roll = ctx.dice.rollD6()
      val isCrit = roll == 6
      val effectiveRoll = if isCrit then 6 else (roll + modifier).max(1).min(6)

      val finalRoll =
        if effectiveRoll >= target then effectiveRoll
        else
          reroll match
            case Some(RerollTarget.All) => ctx.dice.rollD6() + modifier
            case Some(RerollTarget.Failed) => ctx.dice.rollD6() + modifier
            case Some(RerollTarget.Ones) if roll == 1 => ctx.dice.rollD6() + modifier
            case _ => effectiveRoll

      val finalIsCrit = roll == 6 || (finalRoll != effectiveRoll && finalRoll == 6)
      val passed = if finalIsCrit then true else finalRoll >= target

      if passed then
        if finalIsCrit then
          crits.foreach:
            case CriticalEffect.MortalWounds(dmg) => mortalWounds += dmg
            case CriticalEffect.AutoWound => wounds += 1
            case CriticalEffect.ExtraHits(_) =>
          wounds += 1
        else
          wounds += 1

    (wounds, mortalWounds)

  private def rollSaves(ctx: AttackContext, effects: List[Effect], wounds: Int): Int =
    val ap = ctx.weapon.armorPenetration
    val saveTarget = ctx.targetSave + ap + (if ctx.isInCover then -1 else 0)
    val invulnTarget = ctx.targetInvuln

    val ignoresCover = effects.exists:
      case ModifyRoll(PipelineStep.SaveRoll, _, _) => false
      case _ => false

    val modifier = rollModifier(PipelineStep.SaveRoll, effects, ctx)

    var failed = 0
    (1 to wounds).foreach: _ =>
      val roll = ctx.dice.rollD6()
      val normalSave = roll + modifier >= saveTarget
      val invulnSave = invulnTarget.exists(roll + modifier >= _)
      val bestSave = normalSave || invulnSave

      if !bestSave then failed += 1

    failed

  private def rollDamage(ctx: AttackContext, effects: List[Effect], unsaved: Int): Int =
    (1 to unsaved).map(_ => ctx.dice.rollNotation(ctx.weapon.damage)).sum

  private def applyFeelNoPain(ctx: AttackContext, effects: List[Effect], damage: Int): Int =
    val fnpTarget = effects.collect:
      case FeelNoPainEffect(t, cond) if conditionMet(cond, ctx) => t
    .minOption

    fnpTarget match
      case None => damage
      case Some(target) =>
        (1 to damage).count(_ => ctx.dice.rollD6() < target)

  def woundTarget(strength: Int, toughness: Int): Int =
    if strength >= toughness * 2 then 2
    else if strength > toughness then 3
    else if strength == toughness then 4
    else if strength * 2 <= toughness then 6
    else 5

  private def rollModifier(step: PipelineStep, effects: List[Effect], ctx: AttackContext): Int =
    effects.collect:
      case ModifyRoll(s, mod, cond) if s == step && conditionMet(cond, ctx) => mod
    .sum.max(-1).min(1)

  private def getReroll(step: PipelineStep, effects: List[Effect], ctx: AttackContext): Option[RerollTarget] =
    effects.collect:
      case RerollDice(s, which, cond) if s == step && conditionMet(cond, ctx) => which
    .sortBy:
      case RerollTarget.All => 0
      case RerollTarget.Failed => 1
      case RerollTarget.Ones => 2
    .headOption

  private def getCriticals(step: PipelineStep, effects: List[Effect], ctx: AttackContext): List[CriticalEffect] =
    effects.collect:
      case OnCritical(s, eff, cond) if s == step && conditionMet(cond, ctx) => eff

  private def conditionMet(cond: EffectCondition, ctx: AttackContext): Boolean =
    cond match
      case Always => true
      case DidNotMove(_) => !ctx.attacker.hasMoved
      case DidAdvance(_) => ctx.attacker.hasAdvanced
      case HasKeyword(kw) => false
      case TargetHasKeyword(kw) => false
      case PhaseIs(phase) => false
      case And(conds) => conds.forall(conditionMet(_, ctx))
      case Or(conds) => conds.exists(conditionMet(_, ctx))
