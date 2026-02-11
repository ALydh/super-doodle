package wahapedia.engine.phase

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import wahapedia.engine.state.*
import wahapedia.engine.spatial.Vec3
import wahapedia.engine.command.*
import wahapedia.engine.combat.*
import wahapedia.domain.types.{DatasheetId, FactionId, DetachmentId}

class FightPhaseSpec extends AnyFlatSpec with Matchers with EitherValues {

  def makeModel(id: String, pos: Vec3, wounds: Int = 2): ModelState =
    ModelState(ModelId(id), 1, pos, wounds, Nil, false)

  def makeUnit(id: String, owner: String, pos: Vec3, wounds: Int = 2): UnitState =
    UnitState(
      id = UnitId(id),
      datasheetId = DatasheetId("000000001"),
      owner = PlayerId(owner),
      models = Vector(makeModel("m1", pos, wounds))
    )

  def makeState(units: UnitState*): GameState =
    GameState(
      round = 1,
      phaseState = PhaseState(Phase.Fight, SubPhase.SelectUnit),
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

  "validatePileIn" should "accept moves within 3 inches" in {
    val unit = makeUnit("u1", "p1", Vec3(0, 0, 0))
    val state = makeState(unit)
    val cmd = PileIn(UnitId("u1"), Map(ModelId("m1") -> Vec3(2.5, 0, 0)))
    FightPhase.validatePileIn(state, cmd).isRight shouldBe true
  }

  it should "reject moves beyond 3 inches" in {
    val unit = makeUnit("u1", "p1", Vec3(0, 0, 0))
    val state = makeState(unit)
    val cmd = PileIn(UnitId("u1"), Map(ModelId("m1") -> Vec3(4, 0, 0)))
    FightPhase.validatePileIn(state, cmd).isLeft shouldBe true
  }

  "validateFightTarget" should "accept target in engagement range" in {
    val unit = makeUnit("u1", "p1", Vec3(0, 0, 0))
    val target = makeUnit("t1", "p2", Vec3(0.5, 0, 0))
    val state = makeState(unit, target)
    FightPhase.validateFightTarget(state, FightTarget(UnitId("u1"), UnitId("t1"), Nil)).isRight shouldBe true
  }

  it should "reject target not in engagement range" in {
    val unit = makeUnit("u1", "p1", Vec3(0, 0, 0))
    val target = makeUnit("t1", "p2", Vec3(5, 0, 0))
    val state = makeState(unit, target)
    FightPhase.validateFightTarget(state, FightTarget(UnitId("u1"), UnitId("t1"), Nil)).isLeft shouldBe true
  }

  "executeFight" should "deal melee damage" in {
    val unit = makeUnit("u1", "p1", Vec3(0, 0, 0))
    val target = makeUnit("t1", "p2", Vec3(0.5, 0, 0), wounds = 5)
    val state = makeState(unit, target)
    val cmd = FightTarget(UnitId("u1"), UnitId("t1"), Nil)
    val weapon = WeaponProfile("Chainsword", "3", Some(4), 4, 0, "1", isMelee = true)
    val dice = FixedDiceRoller(
      4, 4, 4,
      4, 4, 4,
      2, 2, 2
    )

    val (newState, _) = FightPhase.executeFight(state, cmd, List(weapon), dice)
    val updatedTarget = newState.board.units(UnitId("t1"))
    updatedTarget.models.head.woundsRemaining should be < 5
  }
}
