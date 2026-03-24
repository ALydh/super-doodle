package wp40k.http.routes

import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import org.http4s.HttpRoutes
import sttp.model.StatusCode
import sttp.model.headers.CookieValueWithMeta
import sttp.tapir.server.http4s.Http4sServerInterpreter
import wp40k.db.{UserRepository, SessionRepository, InviteRepository}
import wp40k.domain.types.*
import wp40k.http.{AuthMiddleware, InputValidation, TapirSecurity}
import wp40k.http.dto.*
import wp40k.http.endpoints.AuthEndpoints
import wp40k.auth.{PasswordHasher, RateLimiter}
import doobie.*

object AuthRoutesTapir {

  private def sessionCookie(token: String): CookieValueWithMeta =
    CookieValueWithMeta.unsafeApply(token, httpOnly = true, path = Some("/"))

  private val clearSessionCookie: CookieValueWithMeta =
    CookieValueWithMeta.unsafeApply("", httpOnly = true, path = Some("/"), maxAge = Some(0))

  def routes(xa: Transactor[IO], loginRateLimiter: RateLimiter): HttpRoutes[IO] = {
    val registerRoute = Http4sServerInterpreter[IO]().toRoutes(
      AuthEndpoints.register.serverLogic { regReq =>
        (InputValidation.validateUsername(regReq.username), InputValidation.validatePassword(regReq.password)) match {
          case (Left(err), _) =>
            IO.pure(Left((StatusCode.BadRequest, Json.obj("error" -> Json.fromString(err.message)))))
          case (_, Left(err)) =>
            IO.pure(Left((StatusCode.BadRequest, Json.obj("error" -> Json.fromString(err.message)))))
          case (Right(username), Right(password)) =>
            for {
              userCount <- UserRepository.count(xa)
              hash <- PasswordHasher.hash(password)
              result <- {
                val isFirstUser = userCount == 0
                if (!isFirstUser && regReq.inviteCode.isEmpty) {
                  IO.pure(Left((StatusCode.Forbidden, Json.obj("error" -> Json.fromString("Invite code required")))))
                } else if (!isFirstUser) {
                  val code = InviteCode(regReq.inviteCode.get)
                  InviteRepository.findUnusedByCode(code)(xa).flatMap {
                    case None =>
                      IO.pure(Left((StatusCode.Forbidden, Json.obj("error" -> Json.fromString("Invalid or used invite code")))))
                    case Some(_) =>
                      UserRepository.create(username, hash, isAdmin = false)(xa).flatMap {
                        case None =>
                          IO.pure(Left((StatusCode.Conflict, Json.obj("error" -> Json.fromString("Username already taken")))))
                        case Some(user) =>
                          InviteRepository.markUsed(code, user.id)(xa) *>
                            SessionRepository.create(user.id)(xa).map { session =>
                              val tokenStr = SessionToken.value(session.token)
                              Right((AuthResponse(tokenStr, UserResponse(UserId.value(user.id), user.username, user.isAdmin)), sessionCookie(tokenStr)))
                            }
                      }
                  }
                } else {
                  UserRepository.create(username, hash, isAdmin = true)(xa).flatMap {
                    case None =>
                      IO.pure(Left((StatusCode.Conflict, Json.obj("error" -> Json.fromString("Username already taken")))))
                    case Some(user) =>
                      SessionRepository.create(user.id)(xa).map { session =>
                        val tokenStr = SessionToken.value(session.token)
                        Right((AuthResponse(tokenStr, UserResponse(UserId.value(user.id), user.username, user.isAdmin)), sessionCookie(tokenStr)))
                      }
                  }
                }
              }
            } yield result
        }
      }
    )

    val loginRoute = Http4sServerInterpreter[IO]().toRoutes(
      AuthEndpoints.login.serverLogic { loginReq =>
        loginRateLimiter.isAllowed(loginReq.username).flatMap {
          case false =>
            IO.pure(Left((StatusCode.TooManyRequests, Json.obj("error" -> Json.fromString("Too many login attempts. Please try again later.")))))
          case true =>
            UserRepository.findByUsername(loginReq.username)(xa).flatMap {
              case None =>
                IO.pure(Left((StatusCode.Unauthorized, Json.obj("error" -> Json.fromString("Invalid credentials")))))
              case Some(user) =>
                PasswordHasher.verify(loginReq.password, user.passwordHash).flatMap {
                  case false =>
                    IO.pure(Left((StatusCode.Unauthorized, Json.obj("error" -> Json.fromString("Invalid credentials")))))
                  case true =>
                    SessionRepository.create(user.id)(xa).map { session =>
                      val tokenStr = SessionToken.value(session.token)
                      Right((AuthResponse(tokenStr, UserResponse(UserId.value(user.id), user.username, user.isAdmin)), sessionCookie(tokenStr)))
                    }
                }
            }
        }
      }
    )

    val logoutRoute = Http4sServerInterpreter[IO]().toRoutes(
      AuthEndpoints.logout
        .serverSecurityLogic { (bearer, sessionCookie) =>
          val tokenOpt = bearer.orElse(sessionCookie)
          tokenOpt match {
            case None => IO.pure(Right(None: Option[String]))
            case Some(tokenStr) =>
              AuthMiddleware.extractUserByToken(tokenStr, xa).map {
                case None => Right(None)
                case Some(_) => Right(Some(tokenStr))
              }
          }
        }
        .serverLogic { tokenOpt => _ =>
          val msg = Json.obj("message" -> Json.fromString("Logged out"))
          tokenOpt match {
            case Some(tokenStr) =>
              SessionRepository.delete(SessionToken(tokenStr))(xa).as(Right((msg, clearSessionCookie)))
            case None =>
              IO.pure(Right((msg, clearSessionCookie)))
          }
        }
    )

    val meRoute = Http4sServerInterpreter[IO]().toRoutes(
      AuthEndpoints.me
        .serverSecurityLogic(TapirSecurity.required(xa))
        .serverLogic { user => _ =>
          IO.pure(Right(UserResponse(UserId.value(user.id), user.username, user.isAdmin)))
        }
    )

    val createInviteRoute = Http4sServerInterpreter[IO]().toRoutes(
      AuthEndpoints.createInvite
        .serverSecurityLogic(TapirSecurity.required(xa))
        .serverLogic { user => _ =>
          if (!user.isAdmin) {
            IO.pure(Left((StatusCode.Forbidden, Json.obj("error" -> Json.fromString("Admin access required")))))
          } else {
            InviteRepository.create(Some(user.id))(xa).map { invite =>
              Right(InviteResponse(InviteCode.value(invite.code), invite.createdAt.toString, false))
            }
          }
        }
    )

    val listInvitesRoute = Http4sServerInterpreter[IO]().toRoutes(
      AuthEndpoints.listInvites
        .serverSecurityLogic(TapirSecurity.required(xa))
        .serverLogic { user => _ =>
          if (!user.isAdmin) {
            IO.pure(Left((StatusCode.Forbidden, Json.obj("error" -> Json.fromString("Admin access required")))))
          } else {
            InviteRepository.listAll(xa).map { invites =>
              Right(invites.map(i => InviteResponse(InviteCode.value(i.code), i.createdAt.toString, i.usedBy.isDefined)))
            }
          }
        }
    )

    registerRoute <+> loginRoute <+> logoutRoute <+> meRoute <+> createInviteRoute <+> listInvitesRoute
  }
}
