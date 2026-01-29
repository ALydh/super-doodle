package wahapedia.db

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import wahapedia.domain.types.{UserId, InviteCode}
import DoobieMeta.given
import java.nio.file.{Files, Path}

class InviteRepositorySpec extends AnyFlatSpec with Matchers with BeforeAndAfterEach {

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

  private def createTestUser(username: String): UserId =
    UserRepository.create(username, "hash")(xa).unsafeRunSync().id

  "create" should "persist an invite with a 16-char code" in {
    val userId = createTestUser("admin")
    val invite = InviteRepository.create(Some(userId))(xa).unsafeRunSync()
    InviteCode.value(invite.code).length shouldBe 16
    invite.createdBy shouldBe Some(userId)
    invite.usedBy shouldBe None
    invite.usedAt shouldBe None
  }

  it should "allow creating invites without a creator" in {
    val invite = InviteRepository.create(None)(xa).unsafeRunSync()
    invite.createdBy shouldBe None
  }

  "findUnusedByCode" should "return an unused invite" in {
    val invite = InviteRepository.create(None)(xa).unsafeRunSync()
    val found = InviteRepository.findUnusedByCode(invite.code)(xa).unsafeRunSync()
    found shouldBe defined
    found.get.code shouldBe invite.code
  }

  it should "return None for non-existent code" in {
    val found = InviteRepository.findUnusedByCode(InviteCode("nonexistent1234"))(xa).unsafeRunSync()
    found shouldBe None
  }

  it should "return None for used invite" in {
    val invite = InviteRepository.create(None)(xa).unsafeRunSync()
    val userId = createTestUser("newuser")
    InviteRepository.markUsed(invite.code, userId)(xa).unsafeRunSync()

    val found = InviteRepository.findUnusedByCode(invite.code)(xa).unsafeRunSync()
    found shouldBe None
  }

  "markUsed" should "mark an invite as used" in {
    val invite = InviteRepository.create(None)(xa).unsafeRunSync()
    val userId = createTestUser("newuser")
    InviteRepository.markUsed(invite.code, userId)(xa).unsafeRunSync()

    val invites = InviteRepository.listAll(xa).unsafeRunSync()
    val used = invites.find(_.code == invite.code).get
    used.usedBy shouldBe Some(userId)
    used.usedAt shouldBe defined
  }

  "listAll" should "return all invites" in {
    InviteRepository.create(None)(xa).unsafeRunSync()
    InviteRepository.create(None)(xa).unsafeRunSync()
    InviteRepository.create(None)(xa).unsafeRunSync()

    val invites = InviteRepository.listAll(xa).unsafeRunSync()
    invites.size shouldBe 3
  }
}
