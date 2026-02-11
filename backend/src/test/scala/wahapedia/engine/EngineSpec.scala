package wahapedia.engine

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import wahapedia.engine.state.*
import wahapedia.engine.spatial.Vec3
import wahapedia.engine.command.*
import wahapedia.engine.combat.*
import wahapedia.engine.event.*
import wahapedia.engine.phase.PhaseRunner
import wahapedia.domain.types.{DatasheetId, FactionId, DetachmentId}

class EngineSpec extends AnyFlatSpec with Matchers with EitherValues {

  val p1 = PlayerId("p1")
  val p2 = PlayerId("p2")

  def makeModel(id: String, pos: Vec3, wounds: Int = 1): ModelState =
    ModelState(ModelId(id), 1, pos, wounds, Nil, false)

  def makeSquad(unitId: String, owner: PlayerId, basePos: Vec3, count: Int, wounds: Int = 1): UnitState =
    val models = (0 until count).map: i =>
      makeModel(s"${unitId}_m$i", basePos + Vec3(i * 1.0, 0, 0), wounds)
    UnitState(
      id = UnitId(unitId),
      datasheetId = DatasheetId("000000001"),
      owner = owner,
      models = models.toVector
    )

  def setupBasicGame(): (GameEngine, GameState) =
    val dice = FixedDiceRoller()
    val engine = GameEngine(dice)

    val intercessors = makeSquad("intercessors", p1, Vec3(10, 22, 0), 5, wounds = 2)
    val termagants = makeSquad("termagants", p2, Vec3(50, 22, 0), 10, wounds = 1)

    val state = GameEngine.setupGame(
      player1Id = p1,
      player1Faction = FactionId("SM"),
      player1Detachment = DetachmentId("gladius"),
      player1Units = List(intercessors),
      player2Id = p2,
      player2Faction = FactionId("TY"),
      player2Detachment = DetachmentId("invasion"),
      player2Units = List(termagants)
    )

    (engine, state)

  "GameEngine" should "setup initial state correctly" in {
    val (_, state) = setupBasicGame()
    state.round shouldBe 1
    state.currentPhase shouldBe Phase.Command
    state.activePlayer shouldBe p1
    state.board.units should have size 2
    state.board.units(UnitId("intercessors")).models should have size 5
    state.board.units(UnitId("termagants")).models should have size 10
  }

  it should "gain CP in command phase" in {
    val (engine, state) = setupBasicGame()
    val result = engine.execute(state, EndPhase(p1))
    result.isRight shouldBe true
    val (newState, events) = result.value
    newState.players(p1).commandPoints shouldBe 1
  }

  it should "reject commands from wrong player" in {
    val (engine, state) = setupBasicGame()
    val result = engine.execute(state, EndPhase(p2))
    result.isLeft shouldBe true
  }

  it should "advance through phases" in {
    val (engine, state) = setupBasicGame()

    val Right((afterCmd, _)) = engine.execute(state, EndPhase(p1)): @unchecked
    afterCmd.currentPhase shouldBe Phase.Movement

    val Right((afterMov, _)) = engine.execute(afterCmd, EndPhase(p1)): @unchecked
    afterMov.currentPhase shouldBe Phase.Shooting

    val Right((afterShoot, _)) = engine.execute(afterMov, EndPhase(p1)): @unchecked
    afterShoot.currentPhase shouldBe Phase.Charge

    val Right((afterCharge, _)) = engine.execute(afterShoot, EndPhase(p1)): @unchecked
    afterCharge.currentPhase shouldBe Phase.Fight

    val Right((afterFight, _)) = engine.execute(afterCharge, EndPhase(p1)): @unchecked
    afterFight.currentPhase shouldBe Phase.Command
    afterFight.activePlayer shouldBe p2
  }

  it should "move units in movement phase" in {
    val (engine, state) = setupBasicGame()
    val Right((afterCmd, _)) = engine.execute(state, EndPhase(p1)): @unchecked

    val moveCmd = MoveUnit(
      UnitId("intercessors"),
      Map(
        ModelId("intercessors_m0") -> Vec3(16, 22, 0),
        ModelId("intercessors_m1") -> Vec3(17, 22, 0),
        ModelId("intercessors_m2") -> Vec3(18, 22, 0),
        ModelId("intercessors_m3") -> Vec3(19, 22, 0),
        ModelId("intercessors_m4") -> Vec3(20, 22, 0)
      )
    )
    val Right((afterMove, events)) = engine.execute(afterCmd, moveCmd): @unchecked
    val movedUnit = afterMove.board.units(UnitId("intercessors"))
    movedUnit.hasMoved shouldBe true
    movedUnit.models.head.position shouldBe Vec3(16, 22, 0)
  }

  it should "reject invalid phase commands" in {
    val (engine, state) = setupBasicGame()
    val result = engine.execute(state, MoveUnit(UnitId("intercessors"), Map.empty))
    result.isLeft shouldBe true
    result.left.value shouldBe a[InvalidPhase]
  }

  it should "provide valid commands" in {
    val (engine, state) = setupBasicGame()
    val commands = engine.validCommands(state)
    commands should contain(EndPhase(p1))
  }

  it should "detect game over when all units destroyed" in {
    val dice = FixedDiceRoller()
    val engine = GameEngine(dice)

    val unit1 = makeSquad("u1", p1, Vec3(10, 10, 0), 1, wounds = 1)
    val unit2 = makeSquad("u2", p2, Vec3(50, 10, 0), 1, wounds = 1).copy(isDestroyed = true, models = Vector(makeModel("u2_m0", Vec3(50, 10, 0), wounds = 0)))

    val state = GameEngine.setupGame(
      player1Id = p1, player1Faction = FactionId("SM"), player1Detachment = DetachmentId("gladius"),
      player1Units = List(unit1),
      player2Id = p2, player2Faction = FactionId("TY"), player2Detachment = DetachmentId("invasion"),
      player2Units = List(unit2)
    )

    engine.isGameOver(state) shouldBe defined
    engine.isGameOver(state).get.winner shouldBe Some(p1)
  }

  it should "run a multi-round game advancing through all phases" in {
    val dice = FixedDiceRoller()
    (1 to 100).foreach(_ => dice.enqueue(4))
    val engine = GameEngine(dice)
    val state = setupBasicGame()._2

    var current = state
    for round <- 1 to 2 do
      for player <- List(p1, p2) do
        val Right((s1, _)) = engine.execute(current, EndPhase(player)): @unchecked
        val Right((s2, _)) = engine.execute(s1, EndPhase(player)): @unchecked
        val Right((s3, _)) = engine.execute(s2, EndPhase(player)): @unchecked
        val Right((s4, _)) = engine.execute(s3, EndPhase(player)): @unchecked
        val Right((s5, _)) = engine.execute(s4, EndPhase(player)): @unchecked
        current = s5

    current.round should be > 1
    current.events should not be empty
  }

  "PhaseRunner" should "start a game with proper events" in {
    val (_, state) = setupBasicGame()
    val (started, events) = PhaseRunner.startGame(state)
    events should have size 2
    events.head shouldBe a[RoundStarted]
    events(1) shouldBe a[PhaseStarted]
  }

  it should "reset unit flags on turn change" in {
    val (engine, state) = setupBasicGame()
    val Right((afterCmd, _)) = engine.execute(state, EndPhase(p1)): @unchecked

    val moveCmd = MoveUnit(
      UnitId("intercessors"),
      Map(
        ModelId("intercessors_m0") -> Vec3(14, 22, 0),
        ModelId("intercessors_m1") -> Vec3(15, 22, 0),
        ModelId("intercessors_m2") -> Vec3(16, 22, 0),
        ModelId("intercessors_m3") -> Vec3(17, 22, 0),
        ModelId("intercessors_m4") -> Vec3(18, 22, 0)
      )
    )
    val Right((afterMove, _)) = engine.execute(afterCmd, moveCmd): @unchecked
    afterMove.board.units(UnitId("intercessors")).hasMoved shouldBe true

    val Right((s2, _)) = engine.execute(afterMove, EndPhase(p1)): @unchecked
    val Right((s3, _)) = engine.execute(s2, EndPhase(p1)): @unchecked
    val Right((s4, _)) = engine.execute(s3, EndPhase(p1)): @unchecked
    val Right((s5, _)) = engine.execute(s4, EndPhase(p1)): @unchecked

    s5.activePlayer shouldBe p2
    s5.board.units(UnitId("intercessors")).hasMoved shouldBe false
  }
}
