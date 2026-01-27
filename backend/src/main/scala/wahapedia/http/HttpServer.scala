package wahapedia.http

import cats.effect.{IO, Resource}
import cats.implicits.*
import com.comcast.ip4s.*
import io.circe.Json
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.CORS
import wahapedia.db.ReferenceDataRepository
import doobie.Transactor

object HttpServer {
  
  private val corsConfig = CORS.policy.withAllowOriginAll
  
  def createServer(port: Int, xa: Transactor[IO]): Resource[IO, org.http4s.server.Server] = 
    EmberServerBuilder.default[IO]
      .withHost(ip"0.0.0.0")
      .withPort(Port.fromInt(port).get)
      .withHttpApp(corsConfig(routes(xa)).orNotFound)
      .build
  
  def routes(xa: Transactor[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "health" =>
      Ok(Json.obj("status" -> Json.fromString("ok")))
    case GET -> Root / "api" / "factions" =>
      ReferenceDataRepository.allFactions(xa).flatMap(Ok(_))
    case _ =>
      NotFound("Not Found")
  }
}