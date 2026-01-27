package wahapedia.db

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import wahapedia.domain.army.{Army, ArmyUnit}
import wahapedia.domain.types.*
import wahapedia.domain.models.EnhancementId
import DoobieMeta.given
import java.util.UUID
import java.nio.file.{Files, Path}

class ArmyRepositorySpec extends AnyFlatSpec with Matchers with BeforeAndAfterEach {

  private var xa: Transactor[IO] = _
  private var dbPath: Path = _

  override def beforeEach(): Unit = {
    dbPath = Files.createTempFile("wahapedia-test-", ".db")
    xa = Transactor.fromDriverManager[IO](
      driver = "org.sqlite.JDBC",
      url = s"jdbc:sqlite:${dbPath.toAbsolutePath}?foreign_keys=on",
      logHandler = None
    )
    Schema.initialize(xa).unsafeRunSync()
  }

  override def afterEach(): Unit =
    Files.deleteIfExists(dbPath)

  private val orkFaction = FactionId("Ork")
  private val warbossId = DatasheetId("000000001")
  private val boyzId = DatasheetId("000000002")
  private val enhId = EnhancementId("enh1")
  private val detId = DetachmentId("waaagh")

  private val testArmy = Army(
    factionId = orkFaction,
    battleSize = BattleSize.StrikeForce,
    detachmentId = detId,
    warlordId = warbossId,
    units = List(
      ArmyUnit(warbossId, 1, Some(enhId), None),
      ArmyUnit(boyzId, 1, None, Some(warbossId))
    )
  )

  "create" should "persist an army and return it with an id" in {
    val persisted = ArmyRepository.create("My Ork Army", testArmy)(xa).unsafeRunSync()
    persisted.name shouldBe "My Ork Army"
    persisted.army shouldBe testArmy
    persisted.createdAt should not be empty
    persisted.updatedAt should not be empty
  }

  "findById" should "return a previously created army" in {
    val created = ArmyRepository.create("My Ork Army", testArmy)(xa).unsafeRunSync()
    val found = ArmyRepository.findById(created.id)(xa).unsafeRunSync()
    found shouldBe defined
    found.get.name shouldBe "My Ork Army"
    found.get.army.factionId shouldBe orkFaction
    found.get.army.units.size shouldBe 2
  }

  it should "return None for non-existent id" in {
    val found = ArmyRepository.findById(UUID.randomUUID())(xa).unsafeRunSync()
    found shouldBe None
  }

  it should "preserve unit details including enhancement and leader" in {
    val created = ArmyRepository.create("Test", testArmy)(xa).unsafeRunSync()
    val found = ArmyRepository.findById(created.id)(xa).unsafeRunSync().get
    val warbossUnit = found.army.units.find(_.datasheetId == warbossId).get
    warbossUnit.enhancementId shouldBe Some(enhId)
    val boyzUnit = found.army.units.find(_.datasheetId == boyzId).get
    boyzUnit.attachedLeaderId shouldBe Some(warbossId)
  }

  "listByFaction" should "return only armies for the given faction" in {
    ArmyRepository.create("Ork Army 1", testArmy)(xa).unsafeRunSync()
    ArmyRepository.create("Ork Army 2", testArmy)(xa).unsafeRunSync()

    val smArmy = testArmy.copy(factionId = FactionId("SM"))
    ArmyRepository.create("SM Army", smArmy)(xa).unsafeRunSync()

    val orks = ArmyRepository.listByFaction(orkFaction)(xa).unsafeRunSync()
    orks.size shouldBe 2

    val sm = ArmyRepository.listByFaction(FactionId("SM"))(xa).unsafeRunSync()
    sm.size shouldBe 1
  }

  "update" should "modify an existing army" in {
    val created = ArmyRepository.create("Old Name", testArmy)(xa).unsafeRunSync()
    val newArmy = testArmy.copy(battleSize = BattleSize.Onslaught, units = List(ArmyUnit(warbossId, 1, None, None)))
    val updated = ArmyRepository.update(created.id, "New Name", newArmy)(xa).unsafeRunSync()
    updated shouldBe defined
    updated.get.name shouldBe "New Name"
    updated.get.army.battleSize shouldBe BattleSize.Onslaught
    updated.get.army.units.size shouldBe 1
    updated.get.createdAt shouldBe created.createdAt
    updated.get.updatedAt should not be created.updatedAt
  }

  it should "return None for non-existent id" in {
    val result = ArmyRepository.update(UUID.randomUUID(), "Name", testArmy)(xa).unsafeRunSync()
    result shouldBe None
  }

  "delete" should "remove an existing army" in {
    val created = ArmyRepository.create("To Delete", testArmy)(xa).unsafeRunSync()
    val deleted = ArmyRepository.delete(created.id)(xa).unsafeRunSync()
    deleted shouldBe true
    ArmyRepository.findById(created.id)(xa).unsafeRunSync() shouldBe None
  }

  it should "return false for non-existent id" in {
    val deleted = ArmyRepository.delete(UUID.randomUUID())(xa).unsafeRunSync()
    deleted shouldBe false
  }

  it should "cascade delete army units" in {
    val created = ArmyRepository.create("Cascade Test", testArmy)(xa).unsafeRunSync()
    ArmyRepository.delete(created.id)(xa).unsafeRunSync()
    val unitCount = sql"SELECT COUNT(*) FROM army_units WHERE army_id = ${created.id.toString}"
      .query[Int].unique.transact(xa).unsafeRunSync()
    unitCount shouldBe 0
  }
}
