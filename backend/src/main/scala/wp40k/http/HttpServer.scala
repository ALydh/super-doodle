package wp40k.http

import cats.effect.{IO, Ref, Resource}
import cats.implicits.*
import com.comcast.ip4s.*
import io.circe.Json
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.CORS
import org.http4s.client.Client
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.slf4j.MDC
import wp40k.db.RevisionState
import wp40k.http.routes.*
import wp40k.auth.{RateLimiter, RateLimitConfig}
import wp40k.mcp.McpRoutes
import doobie.*
import java.util.UUID

case class RevisionContext(
  activeRef: Ref[IO, RevisionState],
  client: Client[IO],
  revisionUserXa: Transactor[IO],
  revisionsDir: String
)

object HttpServer {
  private val corsConfig = CORS.policy.withAllowOriginAll
  private val loginRateLimitConfig = RateLimitConfig(maxAttempts = 5, windowSeconds = 60)

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def createServer(
    port: Int,
    refXa: Transactor[IO],
    userXa: Transactor[IO],
    refPrefix: String,
    revisionCtx: Option[RevisionContext] = None
  ): Resource[IO, org.http4s.server.Server] =
    for
      loginRateLimiter <- Resource.eval(RateLimiter.create(loginRateLimitConfig))
      mcpRoutes <- McpRoutes.create(refXa, userXa, refPrefix)
      revisionRoutes = revisionCtx.map(ctx =>
        RevisionRoutesTapir.routes(ctx.revisionUserXa, ctx.activeRef, ctx.client, ctx.revisionsDir)
      ).getOrElse(HttpRoutes.empty[IO])
      allRoutes = routes(refXa, userXa, refPrefix, loginRateLimiter) <+> revisionRoutes <+> mcpRoutes
      server <- EmberServerBuilder.default[IO]
        .withHost(ip"0.0.0.0")
        .withPort(Port.fromInt(port).get)
        .withHttpApp(corsConfig(withLogging(allRoutes)).orNotFound)
        .build
    yield server

  private def withLogging(routes: HttpRoutes[IO])(using log: Logger[IO]): HttpRoutes[IO] =
    HttpRoutes { req =>
      val requestId = UUID.randomUUID().toString.take(8)
      val method = req.method.name
      val path = req.uri.path.renderString

      val setMdc = IO {
        MDC.put("request_id", requestId)
        MDC.put("method", method)
        MDC.put("path", path)
      }
      val clearMdc = IO(MDC.clear())

      cats.data.OptionT.liftF(setMdc *> IO.realTime.map(_.toMillis)).flatMap { startTime =>
        cats.data.OptionT.liftF(log.info(s"Request started")) *>
          routes(req).semiflatMap { resp =>
            for {
              endTime <- IO.realTime.map(_.toMillis)
              duration = endTime - startTime
              _ <- IO(MDC.put("status", resp.status.code.toString))
              _ <- IO(MDC.put("duration_ms", duration.toString))
              _ <- log.info(s"Request completed")
              _ <- clearMdc
            } yield resp
          }.recoverWith { case e: Throwable =>
            cats.data.OptionT.liftF(
              for {
                endTime <- IO.realTime.map(_.toMillis)
                duration = endTime - startTime
                _ <- IO(MDC.put("duration_ms", duration.toString))
                _ <- IO(MDC.put("error", e.getClass.getSimpleName))
                _ <- log.error(e)(s"Request failed: ${e.getMessage}")
                _ <- clearMdc
              } yield Response[IO](Status.InternalServerError)
            )
          }.orElse(
            cats.data.OptionT.liftF(
              for {
                endTime <- IO.realTime.map(_.toMillis)
                duration = endTime - startTime
                _ <- IO(MDC.put("status", "404"))
                _ <- IO(MDC.put("duration_ms", duration.toString))
                _ <- log.info(s"Request not found")
                _ <- clearMdc
              } yield Response[IO](Status.NotFound)
            )
          )
      }
    }

  private def healthRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "health" =>
      Ok(Json.obj("status" -> Json.fromString("ok")))
  }

  def routes(refXa: Transactor[IO], userXa: Transactor[IO], refPrefix: String, loginRateLimiter: RateLimiter): HttpRoutes[IO] =
    healthRoute <+>
    AuthRoutesTapir.routes(userXa, loginRateLimiter) <+>
    FactionRoutesTapir.routes(refXa) <+>
    ArmyRoutesTapir.routes(refXa, userXa, refPrefix) <+>
    DatasheetRoutesTapir.routes(refXa) <+>
    InventoryRoutesTapir.routes(userXa) <+>
    SwaggerRoutes.routes
}
