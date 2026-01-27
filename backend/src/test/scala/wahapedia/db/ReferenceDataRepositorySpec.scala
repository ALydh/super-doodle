package wahapedia.db

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import wahapedia.domain.models.*
import wahapedia.domain.types.*
import DoobieMeta.given
import java.nio.file.{Files, Path}

class ReferenceDataRepositorySpec extends AnyFlatSpec with Matchers with BeforeAndAfterEach {

  private var xa: Transactor[IO] = _
  private var dbPath: Path = _

  override def beforeEach(): Unit = {
    dbPath = Files.createTempFile("wahapedia-test-", ".db")
    xa = Transactor.fromDriverManager[IO](
      driver = "org.sqlite.JDBC",
      url = s"jdbc:sqlite:${dbPath.toAbsolutePath}",
      logHandler = None
    )
    Schema.initialize(xa).unsafeRunSync()
  }

  override def afterEach(): Unit =
    Files.deleteIfExists(dbPath)

  private val orkFaction = FactionId("Ork")
  private val smFaction = FactionId("SM")
  private val faction1 = Faction(orkFaction, "Orks", "/orks")
  private val faction2 = Faction(smFaction, "Space Marines", "/sm")

  private val ds1 = Datasheet(
    DatasheetId("000000001"), "Warboss", Some(orkFaction), None, None,
    Some(Role.Characters), None, None, false, None, None, None, None, "/warboss"
  )
  private val ds2 = Datasheet(
    DatasheetId("000000002"), "Boyz", Some(orkFaction), None, None,
    Some(Role.Battleline), None, None, false, None, None, None, None, "/boyz"
  )
  private val ds3 = Datasheet(
    DatasheetId("000000003"), "Captain", Some(smFaction), None, None,
    Some(Role.Characters), None, None, false, None, None, None, None, "/captain"
  )

  private def insertFactions(): Unit =
    (sql"INSERT INTO factions (id, name, link) VALUES (${faction1.id}, ${faction1.name}, ${faction1.link})".update.run *>
      sql"INSERT INTO factions (id, name, link) VALUES (${faction2.id}, ${faction2.name}, ${faction2.link})".update.run)
      .transact(xa).unsafeRunSync()

  private def insertDatasheets(): Unit = {
    insertFactions()
    List(ds1, ds2, ds3).foreach { d =>
      sql"""INSERT INTO datasheets (id, name, faction_id, source_id, legend, role, loadout, transport, virtual, leader_head, leader_footer, damaged_w, damaged_description, link)
            VALUES (${d.id}, ${d.name}, ${d.factionId}, ${d.sourceId}, ${d.legend}, ${d.role.map(Role.asString)}, ${d.loadout}, ${d.transport}, ${if (d.virtual) 1 else 0}, ${d.leaderHead}, ${d.leaderFooter}, ${d.damagedW}, ${d.damagedDescription}, ${d.link})"""
        .update.run.transact(xa).unsafeRunSync()
    }
  }

  "allFactions" should "return all inserted factions" in {
    insertFactions()
    val result = ReferenceDataRepository.allFactions(xa).unsafeRunSync()
    result.map(_.name).toSet shouldBe Set("Orks", "Space Marines")
  }

  "allDatasheets" should "return all inserted datasheets" in {
    insertDatasheets()
    val result = ReferenceDataRepository.allDatasheets(xa).unsafeRunSync()
    result.size shouldBe 3
  }

  "datasheetsByFaction" should "filter by faction" in {
    insertDatasheets()
    val result = ReferenceDataRepository.datasheetsByFaction(orkFaction)(xa).unsafeRunSync()
    result.size shouldBe 2
    result.map(_.name).toSet shouldBe Set("Warboss", "Boyz")
  }

  "allUnitCosts" should "return inserted unit costs" in {
    insertDatasheets()
    val cost = UnitCost(DatasheetId("000000001"), 1, "1 model", 65)
    sql"INSERT INTO unit_cost (datasheet_id, line, description, cost) VALUES (${cost.datasheetId}, ${cost.line}, ${cost.description}, ${cost.cost})"
      .update.run.transact(xa).unsafeRunSync()
    val result = ReferenceDataRepository.allUnitCosts(xa).unsafeRunSync()
    result.size shouldBe 1
    result.head.cost shouldBe 65
  }

  "allKeywords" should "return inserted keywords with boolean mapping" in {
    insertDatasheets()
    sql"INSERT INTO datasheet_keywords (datasheet_id, keyword, model, is_faction_keyword) VALUES ('000000001', 'Ork', NULL, 1)"
      .update.run.transact(xa).unsafeRunSync()
    sql"INSERT INTO datasheet_keywords (datasheet_id, keyword, model, is_faction_keyword) VALUES ('000000001', 'Infantry', NULL, 0)"
      .update.run.transact(xa).unsafeRunSync()
    val result = ReferenceDataRepository.allKeywords(xa).unsafeRunSync()
    result.size shouldBe 2
    result.filter(_.isFactionKeyword).size shouldBe 1
  }

  "counts" should "return correct counts per table" in {
    insertDatasheets()
    val result = ReferenceDataRepository.counts(xa).unsafeRunSync()
    result("factions") shouldBe 2
    result("datasheets") shouldBe 3
  }

  "loadReferenceData" should "assemble ReferenceData from database" in {
    insertDatasheets()
    val ref = ReferenceDataRepository.loadReferenceData(xa).unsafeRunSync()
    ref.datasheets.size shouldBe 3
    ref.keywords shouldBe empty
    ref.unitCosts shouldBe empty
  }
}
