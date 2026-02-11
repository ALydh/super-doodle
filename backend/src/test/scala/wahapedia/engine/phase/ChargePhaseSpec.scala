package wahapedia.engine.phase

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import wahapedia.engine.state.*
import wahapedia.engine.spatial.Vec3
import wahapedia.engine.command.*
import wahapedia.engine.combat.FixedDiceRoller
import wahapedia.domain.types.{DatasheetId, FactionId, DetachmentId}

class ChargePhaseSpec extends AnyFlatSpec with Matchers with EitherValues {

  def makeModel(id: String, pos: Vec3, wounds: Int = 2): ModelState =
    ModelState(ModelId(id), 1, pos, wounds, Nil, false)

  def makeUnit(id: String, owner: String, pos: Vec3): UnitState =
    UnitState(
      id = UnitId(id),
      datasheetId = DatasheetId("000000001"),
      owner = PlayerId(owner),
      models = Vector(makeModel("m1", pos))
    )

  def makeState(units: UnitState*): GameState =
    GameState(
      round = 1,
      phaseState = PhaseState(Phase.Charge, SubPhase.DeclareCharge),
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

  "validateDeclareCharge" should "accept charge within 12 inches" in {
    val unit = makeUnit("u1", "p1", Vec3(0, 0, 0))
    val target = makeUnit("t1", "p2", Vec3(10, 0, 0))
    val state = makeState(unit, target)
    ChargePhase.validateDeclareCharge(state, DeclareCharge(UnitId("u1"), List(UnitId("t1")))).isRight shouldBe true
  }

  it should "reject charge target beyond 12 inches" in {
    val unit = makeUnit("u1", "p1", Vec3(0, 0, 0))
    val target = makeUnit("t1", "p2", Vec3(15, 0, 0))
    val state = makeState(unit, target)
    ChargePhase.validateDeclareCharge(state, DeclareCharge(UnitId("u1"), List(UnitId("t1")))).isLeft shouldBe true
  }

  it should "reject charge for unit that advanced" in {
    val unit = makeUnit("u1", "p1", Vec3(0, 0, 0)).copy(hasAdvanced = true)
    val target = makeUnit("t1", "p2", Vec3(8, 0, 0))
    val state = makeState(unit, target)
    ChargePhase.validateDeclareCharge(state, DeclareCharge(UnitId("u1"), List(UnitId("t1")))).isLeft shouldBe true
  }

  "rollCharge" should "succeed when roll >= distance" in {
    val unit = makeUnit("u1", "p1", Vec3(0, 0, 0))
    val target = makeUnit("t1", "p2", Vec3(7, 0, 0))
    val state = makeState(unit, target).withPhaseState(
      PhaseState(Phase.Charge, SubPhase.ChargeRoll,
        chargeDeclaredTargets = Map(UnitId("u1") -> List(UnitId("t1"))),
        eligibleChargers = Set(UnitId("u1"))
      )
    )
    val dice = FixedDiceRoller(4, 4)
    val (newState, events) = ChargePhase.rollCharge(state, UnitId("u1"), dice)
    events.head.asInstanceOf[wahapedia.engine.event.ChargeRolled].succeeded shouldBe true
  }

  it should "fail when roll < distance" in {
    val unit = makeUnit("u1", "p1", Vec3(0, 0, 0))
    val target = makeUnit("t1", "p2", Vec3(10, 0, 0))
    val state = makeState(unit, target).withPhaseState(
      PhaseState(Phase.Charge, SubPhase.ChargeRoll,
        chargeDeclaredTargets = Map(UnitId("u1") -> List(UnitId("t1"))),
        eligibleChargers = Set(UnitId("u1"))
      )
    )
    val dice = FixedDiceRoller(3, 3)
    val (newState, events) = ChargePhase.rollCharge(state, UnitId("u1"), dice)
    events.head.asInstanceOf[wahapedia.engine.event.ChargeRolled].succeeded shouldBe false
    newState.phaseState.eligibleChargers should not contain UnitId("u1")
  }
}
