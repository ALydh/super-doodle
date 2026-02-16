package wahapedia.http

import cats.effect.IO
import cats.data.OptionT
import org.http4s.*
import org.http4s.headers.Authorization
import doobie.Transactor
import java.time.Instant
import wahapedia.domain.types.SessionToken
import wahapedia.domain.auth.AuthenticatedUser
import wahapedia.db.{SessionRepository, UserRepository}

object AuthMiddleware {

  def extractUser(req: Request[IO], xa: Transactor[IO]): IO[Option[AuthenticatedUser]] =
    extractToken(req) match {
      case None => IO.pure(None)
      case Some(tokenStr) => extractUserByToken(tokenStr, xa)
    }

  def extractUserByToken(tokenStr: String, xa: Transactor[IO]): IO[Option[AuthenticatedUser]] = {
    val token = SessionToken(tokenStr)
    for {
      sessionOpt <- SessionRepository.findByToken(token)(xa)
      result <- sessionOpt match {
        case None => IO.pure(None)
        case Some(session) =>
          if (session.expiresAt.isBefore(Instant.now())) {
            SessionRepository.delete(token)(xa).as(None)
          } else {
            UserRepository.findById(session.userId)(xa).map(_.map(u =>
              AuthenticatedUser(u.id, u.username, u.isAdmin)
            ))
          }
      }
    } yield result
  }

  private def extractToken(req: Request[IO]): Option[String] = {
    req.headers.get[Authorization].flatMap { auth =>
      auth.credentials match {
        case Credentials.Token(AuthScheme.Bearer, token) => Some(token)
        case _ => None
      }
    }.orElse {
      req.cookies.find(_.name == "session").map(_.content)
    }
  }
}
