package wp40k.db

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import wp40k.domain.types.{UserId, SessionToken}
import DoobieMeta.given
import java.nio.file.{Files, Path}
import java.time.Instant
import java.time.temporal.ChronoUnit

class SessionRepositorySpec extends AnyFlatSpec with Matchers with BeforeAndAfterEach {

  private var xa: Transactor[IO] = _
  private var dbPath: Path = _

  override def beforeEach(): Unit = {
    dbPath = Files.createTempFile("wp40k-test-", ".db")
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

  // Edge case tests

  "findByToken" should "return None for a token that expired 1 second ago" in {
    val userId = createTestUser()
    val expiredTime = Instant.now().minus(1, ChronoUnit.SECONDS).toString
    val token = "just-expired-token"
    sql"""INSERT INTO sessions (token, user_id, created_at, expires_at)
          VALUES ($token, ${UserId.value(userId)}, $expiredTime, $expiredTime)""".update.run.transact(xa).unsafeRunSync()

    SessionRepository.findByToken(SessionToken(token))(xa).unsafeRunSync() shouldBe None
  }

  it should "return Some for a token expiring 1 second in the future" in {
    val userId = createTestUser()
    val now = Instant.now()
    val almostExpired = now.plus(1, ChronoUnit.SECONDS).toString
    val token = "almost-expired-token"
    sql"""INSERT INTO sessions (token, user_id, created_at, expires_at)
          VALUES ($token, ${UserId.value(userId)}, ${now.toString}, $almostExpired)""".update.run.transact(xa).unsafeRunSync()

    SessionRepository.findByToken(SessionToken(token))(xa).unsafeRunSync() shouldBe defined
  }

  "multiple sessions" should "allow deleting one while other remains valid" in {
    val userId = createTestUser()
    val session1 = SessionRepository.create(userId)(xa).unsafeRunSync()
    val session2 = SessionRepository.create(userId)(xa).unsafeRunSync()

    SessionRepository.delete(session1.token)(xa).unsafeRunSync()

    SessionRepository.findByToken(session1.token)(xa).unsafeRunSync() shouldBe None
    SessionRepository.findByToken(session2.token)(xa).unsafeRunSync() shouldBe defined
  }

  "deleteExpired" should "remove 3 expired sessions and keep 2 valid ones" in {
    val userId = createTestUser()
    val valid1 = SessionRepository.create(userId)(xa).unsafeRunSync()
    val valid2 = SessionRepository.create(userId)(xa).unsafeRunSync()

    val expiredTime = Instant.now().minus(1, ChronoUnit.HOURS).toString
    (1 to 3).foreach { i =>
      sql"""INSERT INTO sessions (token, user_id, created_at, expires_at)
            VALUES (${s"expired-$i"}, ${UserId.value(userId)}, $expiredTime, $expiredTime)""".update.run.transact(xa).unsafeRunSync()
    }

    val deleted = SessionRepository.deleteExpired(xa).unsafeRunSync()
    deleted shouldBe 3

    SessionRepository.findByToken(valid1.token)(xa).unsafeRunSync() shouldBe defined
    SessionRepository.findByToken(valid2.token)(xa).unsafeRunSync() shouldBe defined
  }

  it should "return 0 when no sessions are expired" in {
    val userId = createTestUser()
    SessionRepository.create(userId)(xa).unsafeRunSync()
    SessionRepository.create(userId)(xa).unsafeRunSync()

    val deleted = SessionRepository.deleteExpired(xa).unsafeRunSync()
    deleted shouldBe 0
  }

  "expired token lifecycle" should "be invisible to findByToken before cleanup and physically removed after" in {
    val userId = createTestUser()
    val expiredTime = Instant.now().minus(1, ChronoUnit.HOURS).toString
    val token = "lifecycle-token"
    sql"""INSERT INTO sessions (token, user_id, created_at, expires_at)
          VALUES ($token, ${UserId.value(userId)}, $expiredTime, $expiredTime)""".update.run.transact(xa).unsafeRunSync()

    SessionRepository.findByToken(SessionToken(token))(xa).unsafeRunSync() shouldBe None

    val deleted = SessionRepository.deleteExpired(xa).unsafeRunSync()
    deleted shouldBe 1

    val rowCount = sql"SELECT COUNT(*) FROM sessions WHERE token = $token"
      .query[Int].unique.transact(xa).unsafeRunSync()
    rowCount shouldBe 0
  }
}
