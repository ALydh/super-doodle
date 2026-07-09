package wp40k

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import wp40k.db.{Database, Schema, DataLoader}
import java.nio.file.{Files, Path}
import java.time.Instant

class RevisionUpdaterSpec extends AnyFlatSpec with Matchers {

  given Logger[IO] = Slf4jLogger.getLogger[IO]

  private def buildBaseRevision(dir: Path, userXa: Transactor[IO]): (String, String) = {
    val baseId = "20260101-000000"
    val dbPath = dir.resolve(s"wp40k-ref-$baseId.db").toString
    val buildXa = Database.singleTransactor(dbPath)
    Schema.initializeRefSchema(buildXa).unsafeRunSync()
    sql"INSERT INTO unit_cost (datasheet_id, line, description, cost) VALUES ('000000231', 1, '5 models', 250)"
      .update.run.transact(buildXa).unsafeRunSync()
    val now = Instant.now().toString
    sql"""INSERT INTO revisions (id, wahapedia_timestamp, db_path, fetched_at, is_active)
          VALUES ($baseId, '2026-01-01', $dbPath, $now, 1)""".update.run.transact(userXa).unsafeRunSync()
    (baseId, dbPath)
  }

  "ensurePatchedRevision" should "register the tiered sibling as the active default and carry the tiers" in {
    val dir = Files.createTempDirectory("wp40k-rev-test")
    val userPath = dir.resolve("user.db").toString
    val userXa = Database.singleTransactor(userPath)
    Schema.initializeUserSchema(userXa).unsafeRunSync()

    val (baseId, _) = buildBaseRevision(dir, userXa)

    val result = RevisionUpdater.ensurePatchedRevision(baseId, dir.resolve(s"wp40k-ref-$baseId.db").toString, dir.toString, userXa, rebuild = false).unsafeRunSync()
    result.map(_.revisionId) shouldBe Some(s"$baseId-mfm-dark-angels")

    val revs = RevisionUpdater.listAll(userXa).unsafeRunSync()
    val patched = revs.find(_.id == s"$baseId-mfm-dark-angels")
    patched.isDefined shouldBe true
    patched.get.isActive shouldBe true
    revs.find(_.id == baseId).get.isActive shouldBe false

    val patchedXa = Database.singleTransactor(patched.get.dbPath)
    val tiers = sql"SELECT cost, min_count, max_count FROM unit_cost WHERE datasheet_id = '000000231' ORDER BY min_count"
      .query[(Int, Int, Option[Int])].to[List].transact(patchedXa).unsafeRunSync()
    tiers shouldBe List((240, 1, Some(1)), (260, 2, None))
  }

  it should "be idempotent and not rebuild an existing sibling" in {
    val dir = Files.createTempDirectory("wp40k-rev-test")
    val userXa = Database.singleTransactor(dir.resolve("user.db").toString)
    Schema.initializeUserSchema(userXa).unsafeRunSync()
    val (baseId, _) = buildBaseRevision(dir, userXa)

    val ensure = RevisionUpdater.ensurePatchedRevision(baseId, dir.resolve(s"wp40k-ref-$baseId.db").toString, dir.toString, userXa, rebuild = false)
    ensure.unsafeRunSync()
    ensure.unsafeRunSync()

    RevisionUpdater.listAll(userXa).unsafeRunSync().count(_.id == s"$baseId-mfm-dark-angels") shouldBe 1
  }

  it should "rebuild an existing sibling in place without changing the active choice" in {
    val dir = Files.createTempDirectory("wp40k-rev-test")
    val userXa = Database.singleTransactor(dir.resolve("user.db").toString)
    Schema.initializeUserSchema(userXa).unsafeRunSync()
    val (baseId, basePath) = buildBaseRevision(dir, userXa)

    RevisionUpdater.ensurePatchedRevision(baseId, basePath, dir.toString, userXa, rebuild = false).unsafeRunSync()
    // user switches back to the vanilla base
    sql"UPDATE revisions SET is_active = 0".update.run.transact(userXa).unsafeRunSync()
    sql"UPDATE revisions SET is_active = 1 WHERE id = $baseId".update.run.transact(userXa).unsafeRunSync()

    val result = RevisionUpdater.ensurePatchedRevision(baseId, basePath, dir.toString, userXa, rebuild = true).unsafeRunSync()
    result shouldBe None

    val revs = RevisionUpdater.listAll(userXa).unsafeRunSync()
    revs.find(_.id == baseId).get.isActive shouldBe true
    val patched = revs.find(_.id == s"$baseId-mfm-dark-angels").get
    patched.isActive shouldBe false
    val patchedXa = Database.singleTransactor(patched.dbPath)
    sql"SELECT cost, min_count, max_count FROM unit_cost WHERE datasheet_id = '000000231' ORDER BY min_count"
      .query[(Int, Int, Option[Int])].to[List].transact(patchedXa).unsafeRunSync() shouldBe List((240, 1, Some(1)), (260, 2, None))
  }
}
