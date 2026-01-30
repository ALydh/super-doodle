package wahapedia.http

import cats.effect.{IO, Resource}
import cats.implicits.*
import com.comcast.ip4s.*
import io.circe.Json
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.CORS
import wahapedia.http.routes.*
import doobie.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object HttpServer {
  private val corsConfig = CORS.policy.withAllowOriginAll

  def createServer(port: Int, xa: Transactor[IO]): Resource[IO, org.http4s.server.Server] =
    EmberServerBuilder.default[IO]
      .withHost(ip"0.0.0.0")
      .withPort(Port.fromInt(port).get)
      .withHttpApp(corsConfig(withLogging(routes(xa))).orNotFound)
      .build

  private val logTimeFmt = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
  private def now: String = LocalDateTime.now.format(logTimeFmt)

  private def withLogging(routes: HttpRoutes[IO]): HttpRoutes[IO] =
    HttpRoutes { req =>
      cats.data.OptionT.liftF(IO.println(s"[$now] --> ${req.method} ${req.uri}")) *>
        routes(req).semiflatMap { resp =>
          IO.println(s"[$now] <-- ${req.method} ${req.uri} ${resp.status}").as(resp)
        }.recoverWith { case e: Throwable =>
          cats.data.OptionT.liftF(
            IO.println(s"[$now] !!! ${req.method} ${req.uri} ERROR: ${e.getClass.getSimpleName}: ${e.getMessage}") *>
            IO.println(e.getStackTrace.take(10).mkString("\n")) *>
            IO.pure(Response[IO](Status.InternalServerError))
          )
        }.orElse(
          cats.data.OptionT.liftF(IO.println(s"[$now] <-- ${req.method} ${req.uri} (no match)")).as(Response[IO](Status.NotFound))
        )
    }

  private def healthRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "health" =>
      Ok(Json.obj("status" -> Json.fromString("ok")))
  }

  def routes(xa: Transactor[IO]): HttpRoutes[IO] =
    healthRoute <+>
    AuthRoutes.routes(xa) <+>
    FactionRoutes.routes(xa) <+>
    ArmyRoutes.routes(xa) <+>
    DatasheetRoutes.routes(xa)
}
