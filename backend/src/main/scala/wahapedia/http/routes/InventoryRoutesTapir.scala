package wahapedia.http.routes

import cats.effect.IO
import cats.implicits.*
import io.circe.{Json, Encoder}
import io.circe.generic.auto.*
import org.http4s.HttpRoutes
import sttp.model.StatusCode
import sttp.tapir.server.http4s.Http4sServerInterpreter
import wahapedia.db.{InventoryRepository, InventoryEntry}
import wahapedia.domain.types.*
import wahapedia.http.TapirSecurity
import wahapedia.http.dto.{UpsertInventoryRequest, BulkUpsertInventoryRequest}
import wahapedia.http.endpoints.InventoryEndpoints
import doobie.*

object InventoryRoutesTapir {

  given Encoder[InventoryEntry] = Encoder.forProduct3("userId", "datasheetId", "quantity")(
    (e: InventoryEntry) => (e.userId, e.datasheetId, e.quantity)
  )

  def routes(userXa: Transactor[IO]): HttpRoutes[IO] = {
    val listRoute = Http4sServerInterpreter[IO]().toRoutes(
      InventoryEndpoints.listInventory
        .serverSecurityLogic(TapirSecurity.required(userXa))
        .serverLogic { user => _ =>
          InventoryRepository.getByUser(user.id)(userXa).map(Right(_))
        }
    )

    val upsertRoute = Http4sServerInterpreter[IO]().toRoutes(
      InventoryEndpoints.upsertInventory
        .serverSecurityLogic(TapirSecurity.required(userXa))
        .serverLogic { user => body =>
          DatasheetId.parse(body.datasheetId) match {
            case Left(_) =>
              IO.pure(Left((StatusCode.BadRequest, Json.obj("error" -> Json.fromString(s"Invalid datasheet ID: ${body.datasheetId}")))))
            case Right(dsId) =>
              InventoryRepository.upsert(user.id, dsId, body.quantity)(userXa).map(Right(_))
          }
        }
    )

    val bulkUpsertRoute = Http4sServerInterpreter[IO]().toRoutes(
      InventoryEndpoints.bulkUpsertInventory
        .serverSecurityLogic(TapirSecurity.required(userXa))
        .serverLogic { user => body =>
          val parsed = body.entries.map(e => DatasheetId.parse(e.datasheetId).map(id => (id, e.quantity)))
          val errors = parsed.collect { case Left(err) => err }
          if (errors.nonEmpty) {
            IO.pure(Left((StatusCode.BadRequest, Json.obj("error" -> Json.fromString("Invalid datasheet IDs in request")))))
          } else {
            val entries = parsed.collect { case Right(pair) => pair }
            InventoryRepository.bulkUpsert(user.id, entries)(userXa).map(Right(_))
          }
        }
    )

    val deleteRoute = Http4sServerInterpreter[IO]().toRoutes(
      InventoryEndpoints.deleteInventory
        .serverSecurityLogic(TapirSecurity.required(userXa))
        .serverLogic { user => datasheetIdStr =>
          DatasheetId.parse(datasheetIdStr) match {
            case Left(_) =>
              IO.pure(Left((StatusCode.BadRequest, Json.obj("error" -> Json.fromString(s"Invalid datasheet ID: $datasheetIdStr")))))
            case Right(dsId) =>
              InventoryRepository.deleteEntry(user.id, dsId)(userXa).map {
                case true => Right(())
                case false => Left((StatusCode.NotFound, Json.obj("error" -> Json.fromString("Inventory entry not found"))))
              }
          }
        }
    )

    listRoute <+> upsertRoute <+> bulkUpsertRoute <+> deleteRoute
  }
}
