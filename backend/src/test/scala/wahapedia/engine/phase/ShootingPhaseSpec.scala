package wahapedia.engine.phase

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import wahapedia.engine.state.*
import wahapedia.engine.spatial.Vec3
import wahapedia.engine.command.*
import wahapedia.engine.combat.*
import wahapedia.domain.types.{DatasheetId, FactionId, DetachmentId}

class ShootingPhaseSpec extends AnyFlatSpec with Matchers with EitherValues {

  def makeModel(id: String, pos: Vec3, wounds: Int = 2): ModelState =
    ModelState(ModelId(id), 1, pos, wounds, Nil, false)

  def makeUnit(id: String, owner: String, models: Vector[ModelState]): UnitState =
    UnitState(
      id = UnitId(id),
      datasheetId = DatasheetId("000000001"),
      owner = PlayerId(owner),
      models = models
    )

  def makeState(units: UnitState*): GameState =
    GameState(
      round = 1,
      phaseState = PhaseState(Phase.Shooting, SubPhase.SelectShootingUnit),
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

  "validateSelectUnit" should "accept valid shooting unit" in {
    val unit = makeUnit("u1", "p1", Vector(makeModel("m1", Vec3(0, 0, 0))))
    val state = makeState(unit)
    ShootingPhase.validateSelectUnit(state, SelectShootingUnit(UnitId("u1"))).isRight shouldBe true
  }

  it should "reject unit that already shot" in {
    val unit = makeUnit("u1", "p1", Vector(makeModel("m1", Vec3(0, 0, 0)))).copy(hasShot = true)
    val state = makeState(unit)
    ShootingPhase.validateSelectUnit(state, SelectShootingUnit(UnitId("u1"))).isLeft shouldBe true
  }

  "validateSelectTarget" should "accept target in range with LoS" in {
    val attacker = makeUnit("u1", "p1", Vector(makeModel("m1", Vec3(0, 0, 0))))
    val target = makeUnit("t1", "p2", Vector(makeModel("tm1", Vec3(12, 0, 0))))
    val state = makeState(attacker, target)
    val cmd = SelectTarget(UnitId("u1"), UnitId("t1"), Nil)
    ShootingPhase.validateSelectTarget(state, cmd).isRight shouldBe true
  }

  it should "reject target out of range" in {
    val attacker = makeUnit("u1", "p1", Vector(makeModel("m1", Vec3(0, 0, 0))))
    val target = makeUnit("t1", "p2", Vector(makeModel("tm1", Vec3(30, 0, 0))))
    val state = makeState(attacker, target)
    val cmd = SelectTarget(UnitId("u1"), UnitId("t1"), Nil)
    ShootingPhase.validateSelectTarget(state, cmd).isLeft shouldBe true
  }

  it should "reject own unit as target" in {
    val attacker = makeUnit("u1", "p1", Vector(makeModel("m1", Vec3(0, 0, 0))))
    val friendly = makeUnit("f1", "p1", Vector(makeModel("fm1", Vec3(5, 0, 0))))
    val state = makeState(attacker, friendly)
    val cmd = SelectTarget(UnitId("u1"), UnitId("f1"), Nil)
    ShootingPhase.validateSelectTarget(state, cmd).isLeft shouldBe true
  }

  "executeAttack" should "deal damage to target" in {
    val attacker = makeUnit("u1", "p1", Vector(makeModel("m1", Vec3(0, 0, 0))))
    val target = makeUnit("t1", "p2", Vector(makeModel("tm1", Vec3(10, 0, 0), wounds = 5)))
    val state = makeState(attacker, target)
    val cmd = SelectTarget(UnitId("u1"), UnitId("t1"), Nil)
    val weapon = WeaponProfile("Bolt Rifle", "2", Some(3), 4, -1, "1")
    val dice = FixedDiceRoller(
      4, 4,
      4, 4,
      1, 1
    )

    val (newState, events) = ShootingPhase.executeAttack(state, cmd, List(weapon), dice)
    val updatedTarget = newState.board.units(UnitId("t1"))
    updatedTarget.models.head.woundsRemaining should be < 5
  }

  it should "destroy a model with enough damage" in {
    val attacker = makeUnit("u1", "p1", Vector(makeModel("m1", Vec3(0, 0, 0))))
    val target = makeUnit("t1", "p2", Vector(makeModel("tm1", Vec3(10, 0, 0), wounds = 1)))
    val state = makeState(attacker, target)
    val cmd = SelectTarget(UnitId("u1"), UnitId("t1"), Nil)
    val weapon = WeaponProfile("Heavy Bolter", "3", Some(4), 5, -1, "2")
    val dice = FixedDiceRoller(
      4, 4, 4,
      4, 4, 4,
      1, 1, 1
    )

    val (newState, _) = ShootingPhase.executeAttack(state, cmd, List(weapon), dice)
    val updatedTarget = newState.board.units(UnitId("t1"))
    updatedTarget.isDestroyed shouldBe true
  }
}
