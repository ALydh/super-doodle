package wahapedia.db

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import wahapedia.domain.types.UserId
import DoobieMeta.given
import java.nio.file.{Files, Path}

class UserRepositorySpec extends AnyFlatSpec with Matchers with BeforeAndAfterEach {

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

  "create" should "persist a user and return it" in {
    val userOpt = UserRepository.create("testuser", "hashedpassword")(xa).unsafeRunSync()
    userOpt shouldBe defined
    val user = userOpt.get
    user.username shouldBe "testuser"
    user.passwordHash shouldBe "hashedpassword"
    UserId.value(user.id) should not be empty
  }

  "findByUsername" should "return a previously created user" in {
    UserRepository.create("alice", "hash123")(xa).unsafeRunSync()
    val found = UserRepository.findByUsername("alice")(xa).unsafeRunSync()
    found shouldBe defined
    found.get.username shouldBe "alice"
  }

  it should "return None for non-existent username" in {
    val found = UserRepository.findByUsername("nobody")(xa).unsafeRunSync()
    found shouldBe None
  }

  "findById" should "return a previously created user" in {
    val created = UserRepository.create("bob", "hash456")(xa).unsafeRunSync().get
    val found = UserRepository.findById(created.id)(xa).unsafeRunSync()
    found shouldBe defined
    found.get.username shouldBe "bob"
  }

  it should "return None for non-existent id" in {
    val found = UserRepository.findById(UserId.generate())(xa).unsafeRunSync()
    found shouldBe None
  }

  "count" should "return the number of users" in {
    UserRepository.count(xa).unsafeRunSync() shouldBe 0
    UserRepository.create("user1", "hash")(xa).unsafeRunSync() shouldBe defined
    UserRepository.count(xa).unsafeRunSync() shouldBe 1
    UserRepository.create("user2", "hash")(xa).unsafeRunSync() shouldBe defined
    UserRepository.count(xa).unsafeRunSync() shouldBe 2
  }

  "username" should "be unique" in {
    UserRepository.create("duplicate", "hash1")(xa).unsafeRunSync() shouldBe defined
    UserRepository.create("duplicate", "hash2")(xa).unsafeRunSync() shouldBe None
  }
}
