package wahapedia.engine.phase

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import wahapedia.engine.state.*
import wahapedia.engine.spatial.Vec3
import wahapedia.engine.command.*
import wahapedia.domain.types.{DatasheetId, FactionId, DetachmentId}

class MovementPhaseSpec extends AnyFlatSpec with Matchers with EitherValues {

  def makeModel(id: String, pos: Vec3, wounds: Int = 2): ModelState =
    ModelState(ModelId(id), 1, pos, wounds, Nil, false)

  def makeUnit(id: String, owner: String, models: Vector[ModelState], inReserve: Boolean = false): UnitState =
    UnitState(
      id = UnitId(id),
      datasheetId = DatasheetId("000000001"),
      owner = PlayerId(owner),
      models = models,
      isInReserve = inReserve
    )

  def makeState(units: UnitState*): GameState =
    GameState(
      round = 1,
      phaseState = PhaseState(Phase.Movement, SubPhase.SelectUnit),
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

  "validateMove" should "accept a legal move within 6 inches" in {
    val unit = makeUnit("u1", "p1", Vector(makeModel("m1", Vec3(0, 0, 0))))
    val state = makeState(unit)
    val cmd = MoveUnit(UnitId("u1"), Map(ModelId("m1") -> Vec3(5, 0, 0)))
    MovementPhase.validateMove(state, cmd).isRight shouldBe true
  }

  it should "reject move exceeding 6 inches" in {
    val unit = makeUnit("u1", "p1", Vector(makeModel("m1", Vec3(0, 0, 0))))
    val state = makeState(unit)
    val cmd = MoveUnit(UnitId("u1"), Map(ModelId("m1") -> Vec3(7, 0, 0)))
    MovementPhase.validateMove(state, cmd).isLeft shouldBe true
  }

  it should "reject move for enemy unit" in {
    val unit = makeUnit("u1", "p2", Vector(makeModel("m1", Vec3(0, 0, 0))))
    val state = makeState(unit)
    val cmd = MoveUnit(UnitId("u1"), Map(ModelId("m1") -> Vec3(3, 0, 0)))
    MovementPhase.validateMove(state, cmd).isLeft shouldBe true
  }

  it should "reject move for unit in engagement range" in {
    val unit = makeUnit("u1", "p1", Vector(makeModel("m1", Vec3(0, 0, 0))))
    val enemy = makeUnit("e1", "p2", Vector(makeModel("em1", Vec3(0.5, 0, 0))))
    val state = makeState(unit, enemy)
    val cmd = MoveUnit(UnitId("u1"), Map(ModelId("m1") -> Vec3(5, 0, 0)))
    MovementPhase.validateMove(state, cmd).isLeft shouldBe true
  }

  "executeMove" should "update unit position and mark as moved" in {
    val unit = makeUnit("u1", "p1", Vector(makeModel("m1", Vec3(0, 0, 0))))
    val state = makeState(unit)
    val cmd = MoveUnit(UnitId("u1"), Map(ModelId("m1") -> Vec3(4, 0, 0)))
    val (newState, events) = MovementPhase.executeMove(state, cmd)
    val movedUnit = newState.board.units(UnitId("u1"))
    movedUnit.models.head.position shouldBe Vec3(4, 0, 0)
    movedUnit.hasMoved shouldBe true
    events should not be empty
  }

  "validateDeepStrike" should "reject if not in reserve" in {
    val unit = makeUnit("u1", "p1", Vector(makeModel("m1", Vec3(0, 0, 0))))
    val state = makeState(unit)
    val cmd = DeepStrike(UnitId("u1"), Map(ModelId("m1") -> Vec3(20, 20, 0)))
    MovementPhase.validateDeepStrike(state, cmd).isLeft shouldBe true
  }

  it should "accept reserve unit placed far from enemies" in {
    val unit = makeUnit("u1", "p1", Vector(makeModel("m1", Vec3(0, 0, 0))), inReserve = true)
    val enemy = makeUnit("e1", "p2", Vector(makeModel("em1", Vec3(30, 30, 0))))
    val state = makeState(unit, enemy)
    val cmd = DeepStrike(UnitId("u1"), Map(ModelId("m1") -> Vec3(10, 10, 0)))
    MovementPhase.validateDeepStrike(state, cmd).isRight shouldBe true
  }

  it should "reject placement within 9 inches of enemy" in {
    val unit = makeUnit("u1", "p1", Vector(makeModel("m1", Vec3(0, 0, 0))), inReserve = true)
    val enemy = makeUnit("e1", "p2", Vector(makeModel("em1", Vec3(10, 0, 0))))
    val state = makeState(unit, enemy)
    val cmd = DeepStrike(UnitId("u1"), Map(ModelId("m1") -> Vec3(5, 0, 0)))
    MovementPhase.validateDeepStrike(state, cmd).isLeft shouldBe true
  }
}
