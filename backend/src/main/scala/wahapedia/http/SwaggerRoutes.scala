package wahapedia.http

import cats.effect.IO
import org.http4s.HttpRoutes
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.server.http4s.Http4sServerInterpreter
import wahapedia.http.endpoints.*

object SwaggerRoutes {

  def routes: HttpRoutes[IO] = {
    val allEndpoints =
      DatasheetEndpoints.all ++
      FactionEndpoints.all ++
      ArmyEndpoints.all ++
      AuthEndpoints.all ++
      InventoryEndpoints.all

    val swaggerEndpoints = SwaggerInterpreter()
      .fromEndpoints[IO](allEndpoints, "Wahapedia API", "1.0.0")

    Http4sServerInterpreter[IO]().toRoutes(swaggerEndpoints)
  }
}
