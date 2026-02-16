package wahapedia.http

import cats.effect.IO
import io.circe.Json
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.model.UsernamePassword
import doobie.Transactor
import wahapedia.domain.auth.AuthenticatedUser

object TapirSecurity {

  val tokenInput: EndpointInput[(Option[String], Option[String])] =
    auth.bearer[Option[String]]().and(cookie[Option[String]]("session"))

  def required(xa: Transactor[IO])(input: (Option[String], Option[String])): IO[Either[(StatusCode, Json), AuthenticatedUser]] = {
    val tokenOpt = input._1.orElse(input._2)
    tokenOpt match {
      case None =>
        IO.pure(Left((StatusCode.Unauthorized, Json.obj("error" -> Json.fromString("Authentication required")))))
      case Some(tokenStr) =>
        AuthMiddleware.extractUserByToken(tokenStr, xa).map {
          case None => Left((StatusCode.Unauthorized, Json.obj("error" -> Json.fromString("Invalid or expired token"))))
          case Some(user) => Right(user)
        }
    }
  }

  def optional(xa: Transactor[IO])(input: (Option[String], Option[String])): IO[Either[(StatusCode, Json), Option[AuthenticatedUser]]] = {
    val tokenOpt = input._1.orElse(input._2)
    tokenOpt match {
      case None => IO.pure(Right(None))
      case Some(tokenStr) =>
        AuthMiddleware.extractUserByToken(tokenStr, xa).map(user => Right(user))
    }
  }
}
