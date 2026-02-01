package wahapedia.db

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import wahapedia.domain.types.{UserId, SessionToken}
import DoobieMeta.given
import java.nio.file.{Files, Path}
import java.time.Instant
import java.time.temporal.ChronoUnit

class SessionRepositorySpec extends AnyFlatSpec with Matchers with BeforeAndAfterEach {

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

  private def createTestUser(): UserId =
    UserRepository.create("testuser", "hash")(xa).unsafeRunSync().get.id

  "create" should "persist a session with 7 day expiry" in {
    val userId = createTestUser()
    val session = SessionRepository.create(userId)(xa).unsafeRunSync()
    SessionToken.value(session.token).length shouldBe 64
    session.userId shouldBe userId
    session.expiresAt.isAfter(Instant.now().plus(6, ChronoUnit.DAYS)) shouldBe true
    session.expiresAt.isBefore(Instant.now().plus(8, ChronoUnit.DAYS)) shouldBe true
  }

  "findByToken" should "return a previously created session" in {
    val userId = createTestUser()
    val created = SessionRepository.create(userId)(xa).unsafeRunSync()
    val found = SessionRepository.findByToken(created.token)(xa).unsafeRunSync()
    found shouldBe defined
    found.get.userId shouldBe userId
  }

  it should "return None for non-existent token" in {
    val found = SessionRepository.findByToken(SessionToken.generate())(xa).unsafeRunSync()
    found shouldBe None
  }

  "delete" should "remove a session" in {
    val userId = createTestUser()
    val session = SessionRepository.create(userId)(xa).unsafeRunSync()
    SessionRepository.delete(session.token)(xa).unsafeRunSync()
    SessionRepository.findByToken(session.token)(xa).unsafeRunSync() shouldBe None
  }

  "deleteExpired" should "remove only expired sessions" in {
    val userId = createTestUser()
    val validSession = SessionRepository.create(userId)(xa).unsafeRunSync()

    val expiredTime = Instant.now().minus(1, ChronoUnit.DAYS).toString
    sql"""INSERT INTO sessions (token, user_id, created_at, expires_at)
          VALUES ('expiredtoken123', ${UserId.value(userId)}, $expiredTime, $expiredTime)""".update.run.transact(xa).unsafeRunSync()

    val deleted = SessionRepository.deleteExpired(xa).unsafeRunSync()
    deleted shouldBe 1

    SessionRepository.findByToken(validSession.token)(xa).unsafeRunSync() shouldBe defined
    SessionRepository.findByToken(SessionToken("expiredtoken123"))(xa).unsafeRunSync() shouldBe None
  }
}
