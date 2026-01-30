package wahapedia.http.routes

import cats.effect.IO
import io.circe.Json
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*
import org.http4s.headers.{Authorization, `WWW-Authenticate`}
import wahapedia.db.{UserRepository, SessionRepository, InviteRepository}
import wahapedia.domain.types.*
import wahapedia.http.AuthMiddleware
import wahapedia.http.dto.*
import wahapedia.auth.PasswordHasher
import doobie.*

object AuthRoutes {
  private val bearerChallenge = `WWW-Authenticate`(Challenge("Bearer", "api"))

  private def unauthorized(message: String): IO[Response[IO]] =
    Unauthorized(bearerChallenge, Json.obj("error" -> Json.fromString(message)))

  def routes(xa: Transactor[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ POST -> Root / "api" / "auth" / "register" =>
      req.as[RegisterRequest].flatMap { regReq =>
        for {
          userCount <- UserRepository.count(xa)
          existingUser <- UserRepository.findByUsername(regReq.username)(xa)
          result <- existingUser match {
            case Some(_) =>
              Conflict(Json.obj("error" -> Json.fromString("Username already taken")))
            case None =>
              val needsInvite = userCount > 0
              if (needsInvite && regReq.inviteCode.isEmpty) {
                Forbidden(Json.obj("error" -> Json.fromString("Invite code required")))
              } else if (needsInvite) {
                val code = InviteCode(regReq.inviteCode.get)
                InviteRepository.findUnusedByCode(code)(xa).flatMap {
                  case None =>
                    Forbidden(Json.obj("error" -> Json.fromString("Invalid or used invite code")))
                  case Some(_) =>
                    for {
                      hash <- PasswordHasher.hash(regReq.password)
                      user <- UserRepository.create(regReq.username, hash)(xa)
                      _ <- InviteRepository.markUsed(code, user.id)(xa)
                      session <- SessionRepository.create(user.id)(xa)
                      resp <- Created(AuthResponse(
                        SessionToken.value(session.token),
                        UserResponse(UserId.value(user.id), user.username)
                      ))
                    } yield resp
                }
              } else {
                for {
                  hash <- PasswordHasher.hash(regReq.password)
                  user <- UserRepository.create(regReq.username, hash)(xa)
                  session <- SessionRepository.create(user.id)(xa)
                  resp <- Created(AuthResponse(
                    SessionToken.value(session.token),
                    UserResponse(UserId.value(user.id), user.username)
                  ))
                } yield resp
              }
          }
        } yield result
      }

    case req @ POST -> Root / "api" / "auth" / "login" =>
      req.as[LoginRequest].flatMap { loginReq =>
        UserRepository.findByUsername(loginReq.username)(xa).flatMap {
          case None =>
            unauthorized("Invalid credentials")
          case Some(user) =>
            PasswordHasher.verify(loginReq.password, user.passwordHash).flatMap {
              case false =>
                unauthorized("Invalid credentials")
              case true =>
                SessionRepository.create(user.id)(xa).flatMap { session =>
                  Ok(AuthResponse(
                    SessionToken.value(session.token),
                    UserResponse(UserId.value(user.id), user.username)
                  ))
                }
            }
        }
      }

    case req @ POST -> Root / "api" / "auth" / "logout" =>
      AuthMiddleware.extractUser(req, xa).flatMap {
        case None => Ok(Json.obj("message" -> Json.fromString("Logged out")))
        case Some(_) =>
          req.headers.get[Authorization].flatMap { auth =>
            auth.credentials match {
              case Credentials.Token(AuthScheme.Bearer, token) => Some(token)
              case _ => None
            }
          }.orElse(req.cookies.find(_.name == "session").map(_.content)) match {
            case Some(tokenStr) =>
              SessionRepository.delete(SessionToken(tokenStr))(xa) *> Ok(Json.obj("message" -> Json.fromString("Logged out")))
            case None =>
              Ok(Json.obj("message" -> Json.fromString("Logged out")))
          }
      }

    case req @ GET -> Root / "api" / "auth" / "me" =>
      AuthMiddleware.extractUser(req, xa).flatMap {
        case None => unauthorized("Not authenticated")
        case Some(user) => Ok(UserResponse(UserId.value(user.id), user.username))
      }

    case req @ POST -> Root / "api" / "invites" =>
      AuthMiddleware.extractUser(req, xa).flatMap {
        case None => unauthorized("Authentication required")
        case Some(user) =>
          InviteRepository.create(Some(user.id))(xa).flatMap { invite =>
            Created(InviteResponse(InviteCode.value(invite.code), invite.createdAt.toString, false))
          }
      }

    case req @ GET -> Root / "api" / "invites" =>
      AuthMiddleware.extractUser(req, xa).flatMap {
        case None => unauthorized("Authentication required")
        case Some(_) =>
          InviteRepository.listAll(xa).flatMap { invites =>
            Ok(invites.map(i => InviteResponse(InviteCode.value(i.code), i.createdAt.toString, i.usedBy.isDefined)))
          }
      }
  }
}
