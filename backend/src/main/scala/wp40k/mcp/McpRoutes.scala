package wp40k.mcp

import cats.effect.IO
import cats.effect.kernel.Resource
import ch.linkyard.mcp.jsonrpc2.transport.http4s.{McpServerRoute, SessionStore}
import doobie.Transactor
import org.http4s.HttpRoutes
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.*

object McpRoutes:
  private given Logger[IO] = Slf4jLogger.getLogger[IO]

  def create(refXa: Transactor[IO], userXa: Transactor[IO], refPrefix: String): Resource[IO, HttpRoutes[IO]] =
    for
      given SessionStore[IO] <- SessionStore.inMemory[IO](30.minutes)
    yield
      val server = Wp40kMcpServer(refXa, userXa, refPrefix)
      val handler = server.jsonRpcConnectionHandler(e => Logger[IO].warn(e)("MCP error"))
      McpServerRoute.route(handler, root = org.http4s.Uri.Path.Root / "api")
