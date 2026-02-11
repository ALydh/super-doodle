package wahapedia.engine.combat

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import wahapedia.engine.state.*
import wahapedia.engine.spatial.Vec3
import wahapedia.engine.effect.*
import wahapedia.domain.types.DatasheetId

class AttackPipelineSpec extends AnyFlatSpec with Matchers {

  def makeUnit(id: String, owner: String, pos: Vec3 = Vec3.Zero): UnitState =
    UnitState(
      id = UnitId(id),
      datasheetId = DatasheetId("000000001"),
      owner = PlayerId(owner),
      models = Vector(
        ModelState(
          id = ModelId("m1"),
          profileLine = 1,
          position = pos,
          woundsRemaining = 2,
          wargearSelections = Nil,
          isLeader = false
        )
      )
    )

  def basicWeapon(bs: Int = 3, strength: Int = 4, ap: Int = 0, damage: String = "1", attacks: String = "2"): WeaponProfile =
    WeaponProfile(
      name = "Test Gun",
      attacks = attacks,
      ballisticSkill = Some(bs),
      strength = strength,
      armorPenetration = ap,
      damage = damage
    )

  def makeCtx(
    dice: DiceRoller,
    weapon: WeaponProfile = basicWeapon(),
    toughness: Int = 4,
    save: Int = 3,
    invuln: Option[Int] = None,
    effects: List[Effect] = Nil,
    inCover: Boolean = false
  ): AttackContext =
    AttackContext(
      attacker = makeUnit("a1", "p1"),
      target = makeUnit("t1", "p2"),
      weapon = weapon,
      effects = effects,
      dice = dice,
      targetToughness = toughness,
      targetSave = save,
      targetInvuln = invuln,
      isInCover = inCover
    )

  "woundTarget" should "return 2 when S >= 2T" in {
    AttackPipeline.woundTarget(8, 4) shouldBe 2
    AttackPipeline.woundTarget(10, 4) shouldBe 2
  }

  it should "return 3 when S > T" in {
    AttackPipeline.woundTarget(5, 4) shouldBe 3
    AttackPipeline.woundTarget(7, 4) shouldBe 3
  }

  it should "return 4 when S == T" in {
    AttackPipeline.woundTarget(4, 4) shouldBe 4
  }

  it should "return 5 when S < T" in {
    AttackPipeline.woundTarget(3, 4) shouldBe 5
    AttackPipeline.woundTarget(3, 5) shouldBe 5
  }

  it should "return 6 when S <= T/2" in {
    AttackPipeline.woundTarget(2, 4) shouldBe 6
    AttackPipeline.woundTarget(2, 5) shouldBe 6
    AttackPipeline.woundTarget(3, 8) shouldBe 6
  }

  "resolve" should "handle all hits, all wounds, all failed saves" in {
    val dice = FixedDiceRoller(
      4, 4,
      4, 4,
      1, 1
    )
    val ctx = makeCtx(dice, weapon = basicWeapon(bs = 4, strength = 4, ap = -1, damage = "1"))
    val result = AttackPipeline.resolve(ctx)
    result.totalAttacks shouldBe 2
    result.hits shouldBe 2
    result.wounds shouldBe 2
    result.unsavedWounds shouldBe 2
    result.totalDamage shouldBe 2
  }

  it should "handle all misses" in {
    val dice = FixedDiceRoller(1, 1)
    val ctx = makeCtx(dice, weapon = basicWeapon(bs = 4))
    val result = AttackPipeline.resolve(ctx)
    result.hits shouldBe 0
    result.wounds shouldBe 0
    result.totalDamage shouldBe 0
  }

  it should "handle invulnerable saves" in {
    val dice = FixedDiceRoller(
      4, 4,
      4, 4,
      3, 3
    )
    val ctx = makeCtx(
      dice,
      weapon = basicWeapon(bs = 4, strength = 4, ap = -3, damage = "1"),
      save = 3,
      invuln = Some(4)
    )
    val result = AttackPipeline.resolve(ctx)
    result.unsavedWounds shouldBe 0
  }

  "Torrent (AutoPass HitRoll)" should "auto-hit all attacks" in {
    val dice = FixedDiceRoller(
      4, 4,
      2, 2
    )
    val weapon = basicWeapon(bs = 4).copy(abilities = List(AutoPass(PipelineStep.HitRoll)))
    val ctx = makeCtx(dice, weapon = weapon)
    val result = AttackPipeline.resolve(ctx)
    result.hits shouldBe 2
  }

  "Lethal Hits" should "auto-wound on critical hits" in {
    val dice = FixedDiceRoller(
      6, 3,
      4,
      2, 2
    )
    val weapon = basicWeapon(bs = 4).copy(
      abilities = List(OnCritical(PipelineStep.HitRoll, CriticalEffect.AutoWound))
    )
    val ctx = makeCtx(dice, weapon = weapon)
    val result = AttackPipeline.resolve(ctx)
    result.hits shouldBe 2
    result.wounds shouldBe 2
  }

  "Sustained Hits" should "generate extra hits on crits" in {
    val dice = FixedDiceRoller(
      6, 6,
      4, 4, 4, 4,
      1, 1, 1, 1
    )
    val weapon = basicWeapon(bs = 4).copy(
      abilities = List(OnCritical(PipelineStep.HitRoll, CriticalEffect.ExtraHits(1)))
    )
    val ctx = makeCtx(dice, weapon = weapon)
    val result = AttackPipeline.resolve(ctx)
    result.hits shouldBe 4
  }

  "Devastating Wounds" should "cause mortal wounds on crit wound rolls" in {
    val dice = FixedDiceRoller(
      4, 4,
      6, 6,
      1, 1
    )
    val weapon = basicWeapon(bs = 4).copy(
      abilities = List(OnCritical(PipelineStep.WoundRoll, CriticalEffect.MortalWounds(1)))
    )
    val ctx = makeCtx(dice, weapon = weapon)
    val result = AttackPipeline.resolve(ctx)
    result.mortalWounds shouldBe 2
  }

  "Feel No Pain" should "negate some damage" in {
    val dice = FixedDiceRoller(
      4,
      4,
      1,
      5, 4
    )
    val ctx = makeCtx(
      dice,
      weapon = basicWeapon(bs = 4, attacks = "1", damage = "2"),
      effects = List(FeelNoPainEffect(5))
    )
    val result = AttackPipeline.resolve(ctx)
    result.totalDamage shouldBe 1
  }

  "Heavy" should "give +1 to hit when unit didn't move" in {
    val dice = FixedDiceRoller(
      3, 3,
      4, 4,
      1, 1
    )
    val weapon = basicWeapon(bs = 4).copy(
      abilities = List(ModifyRoll(PipelineStep.HitRoll, 1, DidNotMove()))
    )
    val ctx = makeCtx(dice, weapon = weapon)
    val result = AttackPipeline.resolve(ctx)
    result.hits shouldBe 2
  }

  "Cover" should "improve save by 1" in {
    val dice = FixedDiceRoller(
      4,
      4,
      3
    )
    val ctx = makeCtx(
      dice,
      weapon = basicWeapon(bs = 4, attacks = "1", ap = -1),
      save = 3,
      inCover = true
    )
    val result = AttackPipeline.resolve(ctx)
    result.unsavedWounds shouldBe 0
  }

  "Anti-X (SetMinimumRoll)" should "wound on specified value" in {
    val dice = FixedDiceRoller(
      4,
      2,
      1
    )
    val weapon = basicWeapon(bs = 4, strength = 3, attacks = "1").copy(
      abilities = List(SetMinimumRoll(PipelineStep.WoundRoll, 2))
    )
    val ctx = makeCtx(dice, weapon = weapon, toughness = 10)
    val result = AttackPipeline.resolve(ctx)
    result.wounds shouldBe 1
  }
}
