package wahapedia.domain.auth

import wahapedia.domain.types.{UserId, SessionToken, InviteCode}
import java.time.Instant

case class User(
  id: UserId,
  username: String,
  passwordHash: String,
  createdAt: Instant
)

case class Session(
  token: SessionToken,
  userId: UserId,
  createdAt: Instant,
  expiresAt: Instant
)

case class Invite(
  code: InviteCode,
  createdBy: Option[UserId],
  createdAt: Instant,
  usedBy: Option[UserId],
  usedAt: Option[Instant]
)

case class AuthenticatedUser(id: UserId, username: String)
