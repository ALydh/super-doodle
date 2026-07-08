package wp40k.db

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import java.nio.file.Files

class DataLoaderSpec extends AnyFlatSpec with Matchers {

  "applyTierPatch" should "overlay escalating tiers onto base unit costs" in {
    val path = Files.createTempFile("wp40k-tier-", ".db")
    try {
      val xa = Database.singleTransactor(path.toAbsolutePath.toString)
      Schema.initializeRefSchema(xa).unsafeRunSync()

      // Deathwing Knights base row, single open-ended tier
      sql"INSERT INTO unit_cost (datasheet_id, line, description, cost) VALUES ('000000231', 1, '5 models', 250)"
        .update.run.transact(xa).unsafeRunSync()

      DataLoader.applyTierPatch(xa).unsafeRunSync()

      val rows = sql"SELECT cost, min_count, max_count FROM unit_cost WHERE datasheet_id = '000000231' ORDER BY min_count"
        .query[(Int, Int, Option[Int])].to[List].transact(xa).unsafeRunSync()

      rows shouldBe List((240, 1, Some(1)), (260, 2, None))
    } finally {
      Files.deleteIfExists(path)
    }
  }

  it should "leave datasheets absent from the patch untouched" in {
    val path = Files.createTempFile("wp40k-tier-", ".db")
    try {
      val xa = Database.singleTransactor(path.toAbsolutePath.toString)
      Schema.initializeRefSchema(xa).unsafeRunSync()

      sql"INSERT INTO unit_cost (datasheet_id, line, description, cost) VALUES ('000000218', 1, '1 model', 125)"
        .update.run.transact(xa).unsafeRunSync()

      DataLoader.applyTierPatch(xa).unsafeRunSync()

      val rows = sql"SELECT cost, min_count, max_count FROM unit_cost WHERE datasheet_id = '000000218'"
        .query[(Int, Int, Option[Int])].to[List].transact(xa).unsafeRunSync()

      rows shouldBe List((125, 1, None))
    } finally {
      Files.deleteIfExists(path)
    }
  }
}
