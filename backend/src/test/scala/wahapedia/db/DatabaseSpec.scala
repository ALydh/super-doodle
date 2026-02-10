package wahapedia.db

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import java.nio.file.{Files, Path}

class DatabaseSpec extends AnyFlatSpec with Matchers {

  "singleTransactor" should "execute pragmas without error" in {
    val path = Files.createTempFile("wahapedia-test-", ".db")
    try {
      val xa = Database.singleTransactor(path.toAbsolutePath.toString)
      sql"SELECT 1".query[Int].unique.transact(xa).unsafeRunSync() shouldBe 1
    } finally {
      Files.deleteIfExists(path)
    }
  }

  "transactors" should "execute pragmas and attach without error" in {
    val refPath = Files.createTempFile("wahapedia-test-ref-", ".db")
    val userPath = Files.createTempFile("wahapedia-test-user-", ".db")
    try {
      val config = DatabaseConfig(refPath.toAbsolutePath.toString, userPath.toAbsolutePath.toString)
      val dbs = Database.transactors(config)
      sql"SELECT 1".query[Int].unique.transact(dbs.refXa).unsafeRunSync() shouldBe 1
      sql"SELECT 1".query[Int].unique.transact(dbs.userXa).unsafeRunSync() shouldBe 1
    } finally {
      Files.deleteIfExists(refPath)
      Files.deleteIfExists(userPath)
    }
  }
}
