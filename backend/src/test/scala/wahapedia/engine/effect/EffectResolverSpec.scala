package wahapedia.engine.effect

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import wahapedia.engine.state.*
import wahapedia.engine.spatial.Vec3
import wahapedia.engine.combat.WeaponProfile
import wahapedia.domain.types.{DatasheetId, FactionId, DetachmentId}

class EffectResolverSpec extends AnyFlatSpec with Matchers {

  def makeUnit(id: String, owner: String, pos: Vec3 = Vec3.Zero, effects: List[ActiveEffect] = Nil): UnitState =
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
      ),
      activeEffects = effects
    )

  def makeState(units: UnitState*): GameState =
    GameState(
      round = 1,
      phaseState = PhaseState(Phase.Shooting, SubPhase.ResolveAttacks),
      activePlayer = PlayerId("p1"),
      players = Map(
        PlayerId("p1") -> PlayerState(PlayerId("p1"), FactionId("SM"), DetachmentId("gladius")),
        PlayerId("p2") -> PlayerState(PlayerId("p2"), FactionId("TY"), DetachmentId("invasion"))
      ),
      board = BoardState(
        units = units.map(u => u.id -> u).toMap,
        terrain = Nil
      )
    )

  val basicWeapon = WeaponProfile("Bolt Rifle", "2", Some(3), 4, -1, "1")

  "collectAttackEffects" should "include weapon abilities" in {
    val weapon = basicWeapon.copy(abilities = List(AutoPass(PipelineStep.HitRoll)))
    val attacker = makeUnit("a1", "p1")
    val target = makeUnit("t1", "p2")
    val state = makeState(attacker, target)

    val effects = EffectResolver.collectAttackEffects(state, attacker, target, weapon)
    effects should contain(AutoPass(PipelineStep.HitRoll))
  }

  it should "include attacker active effects" in {
    val stealthEffect = ActiveEffect(
      ModifyRoll(PipelineStep.HitRoll, 1),
      EffectDuration.UntilEndOfPhase,
      "stratagem"
    )
    val attacker = makeUnit("a1", "p1", effects = List(stealthEffect))
    val target = makeUnit("t1", "p2")
    val state = makeState(attacker, target)

    val effects = EffectResolver.collectAttackEffects(state, attacker, target, basicWeapon)
    effects should contain(ModifyRoll(PipelineStep.HitRoll, 1))
  }

  it should "include target defensive effects" in {
    val stealthDebuff = ActiveEffect(
      ModifyRoll(PipelineStep.HitRoll, -1),
      EffectDuration.Permanent,
      "stealth"
    )
    val attacker = makeUnit("a1", "p1")
    val target = makeUnit("t1", "p2", effects = List(stealthDebuff))
    val state = makeState(attacker, target)

    val effects = EffectResolver.collectAttackEffects(state, attacker, target, basicWeapon)
    effects should contain(ModifyRoll(PipelineStep.HitRoll, -1))
  }
}
