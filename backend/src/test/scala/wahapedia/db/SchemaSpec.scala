package wahapedia.db

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import java.util.UUID
import java.nio.file.{Files, Path}

class SchemaSpec extends AnyFlatSpec with Matchers {

  private def withTempDb[A](f: Transactor[IO] => A): A = {
    val path = Files.createTempFile("wahapedia-test-", ".db")
    try {
      val xa = Transactor.fromDriverManager[IO](
        driver = "org.sqlite.JDBC",
        url = s"jdbc:sqlite:${path.toAbsolutePath}",
        logHandler = None
      )
      f(xa)
    } finally {
      Files.deleteIfExists(path)
    }
  }

  "Schema.initialize" should "create all 26 tables" in withTempDb { xa =>
    val tableNames = (for {
      _ <- Schema.initialize(xa)
      names <- sql"SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"
        .query[String].to[List].transact(xa)
    } yield names).unsafeRunSync()

    val expected = Set(
      "factions", "sources", "abilities", "datasheets", "model_profiles",
      "wargear", "unit_composition", "unit_cost", "last_update",
      "datasheet_keywords", "datasheet_abilities", "datasheet_leaders",
      "datasheet_options", "stratagems", "datasheet_stratagems",
      "enhancements", "datasheet_enhancements", "detachment_abilities",
      "datasheet_detachment_abilities", "armies", "army_units",
      "weapon_abilities", "users", "sessions", "invites", "parsed_wargear_options"
    )
    tableNames.toSet shouldBe expected
  }

  it should "be idempotent" in withTempDb { xa =>
    val result = (for {
      _ <- Schema.initialize(xa)
      _ <- Schema.initialize(xa)
      names <- sql"SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"
        .query[String].to[List].transact(xa)
    } yield names).unsafeRunSync()

    result.size shouldBe 26
  }
}
