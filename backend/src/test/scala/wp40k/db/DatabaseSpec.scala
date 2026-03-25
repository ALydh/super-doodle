package wp40k.db

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import java.nio.file.{Files, Path}

class DatabaseSpec extends AnyFlatSpec with Matchers {

  "singleTransactor" should "execute pragmas without error" in {
    val path = Files.createTempFile("wp40k-test-", ".db")
    try {
      val xa = Database.singleTransactor(path.toAbsolutePath.toString)
      sql"SELECT 1".query[Int].unique.transact(xa).unsafeRunSync() shouldBe 1
    } finally {
      Files.deleteIfExists(path)
    }
  }

  "transactors" should "execute pragmas and attach without error" in {
    val refPath = Files.createTempFile("wp40k-test-ref-", ".db")
    val userPath = Files.createTempFile("wp40k-test-user-", ".db")
    try {
      val config = DatabaseConfig(refPath.toAbsolutePath.toString, userPath.toAbsolutePath.toString, "data/revisions")
      val dbs = Database.transactors(config)
      sql"SELECT 1".query[Int].unique.transact(dbs.refXa).unsafeRunSync() shouldBe 1
      sql"SELECT 1".query[Int].unique.transact(dbs.userXa).unsafeRunSync() shouldBe 1
    } finally {
      Files.deleteIfExists(refPath)
      Files.deleteIfExists(userPath)
    }
  }
}
