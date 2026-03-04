package wp40k.domain.auth

import wp40k.domain.types.{UserId, SessionToken, InviteCode}
import java.time.Instant

case class User(
  id: UserId,
  username: String,
  passwordHash: String,
  isAdmin: Boolean,
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

case class AuthenticatedUser(id: UserId, username: String, isAdmin: Boolean)
