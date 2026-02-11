package wahapedia.engine.effect

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CoreEffectsSpec extends AnyFlatSpec with Matchers {

  "fromWeaponAbilityString" should "parse lethal hits" in {
    val effects = CoreEffects.fromWeaponAbilityString("lethal hits")
    effects should have size 1
    effects.head shouldBe OnCritical(PipelineStep.HitRoll, CriticalEffect.AutoWound)
  }

  it should "parse sustained hits 1" in {
    val effects = CoreEffects.fromWeaponAbilityString("sustained hits 1")
    effects should have size 1
    effects.head shouldBe OnCritical(PipelineStep.HitRoll, CriticalEffect.ExtraHits(1))
  }

  it should "parse sustained hits 2" in {
    val effects = CoreEffects.fromWeaponAbilityString("sustained hits 2")
    effects.head shouldBe OnCritical(PipelineStep.HitRoll, CriticalEffect.ExtraHits(2))
  }

  it should "parse devastating wounds" in {
    val effects = CoreEffects.fromWeaponAbilityString("devastating wounds")
    effects.head shouldBe OnCritical(PipelineStep.WoundRoll, CriticalEffect.MortalWounds(1))
  }

  it should "parse torrent" in {
    val effects = CoreEffects.fromWeaponAbilityString("torrent")
    effects.head shouldBe AutoPass(PipelineStep.HitRoll)
  }

  it should "parse twin-linked" in {
    val effects = CoreEffects.fromWeaponAbilityString("twin-linked")
    effects.head shouldBe RerollDice(PipelineStep.WoundRoll, RerollTarget.Failed)
  }

  it should "parse heavy" in {
    val effects = CoreEffects.fromWeaponAbilityString("heavy")
    effects.head shouldBe ModifyRoll(PipelineStep.HitRoll, 1, DidNotMove())
  }

  it should "parse lance" in {
    val effects = CoreEffects.fromWeaponAbilityString("lance")
    effects.head shouldBe ModifyRoll(PipelineStep.WoundRoll, 1, DidNotMove())
  }

  it should "parse anti-infantry 4+" in {
    val effects = CoreEffects.fromWeaponAbilityString("anti-infantry 4+")
    effects should have size 1
    effects.head shouldBe SetMinimumRoll(PipelineStep.WoundRoll, 4)
  }

  it should "parse anti-vehicle 2+" in {
    val effects = CoreEffects.fromWeaponAbilityString("anti-vehicle 2+")
    effects.head shouldBe SetMinimumRoll(PipelineStep.WoundRoll, 2)
  }

  it should "parse multiple abilities" in {
    val effects = CoreEffects.fromWeaponAbilityString("lethal hits, sustained hits 1")
    effects should have size 2
  }

  it should "parse complex combo string" in {
    val effects = CoreEffects.fromWeaponAbilityString("anti-infantry 4+, devastating wounds, rapid fire 1")
    effects should have size 3
  }

  it should "handle assault with no effect" in {
    val effects = CoreEffects.fromWeaponAbilityString("assault")
    effects shouldBe empty
  }

  "fromCoreAbility" should "parse stealth" in {
    val effect = CoreEffects.fromCoreAbility("Stealth")
    effect shouldBe defined
    effect.get shouldBe ModifyRoll(PipelineStep.HitRoll, -1)
  }

  it should "parse feel no pain 5+" in {
    val effect = CoreEffects.fromCoreAbility("Feel No Pain 5+")
    effect shouldBe defined
    effect.get shouldBe FeelNoPainEffect(5)
  }
}
