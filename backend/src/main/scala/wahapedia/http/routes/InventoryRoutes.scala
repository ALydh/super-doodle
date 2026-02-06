package wahapedia.http.routes

import cats.effect.IO
import io.circe.{Json, Decoder, Encoder}
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*
import org.http4s.headers.`WWW-Authenticate`
import wahapedia.db.{InventoryRepository, InventoryEntry}
import wahapedia.domain.types.*
import wahapedia.http.AuthMiddleware
import doobie.*

private case class UpsertInventoryRequest(datasheetId: String, quantity: Int)
private case class BulkUpsertInventoryRequest(entries: List[UpsertInventoryRequest])

object InventoryRoutes {
  private val bearerChallenge = `WWW-Authenticate`(Challenge("Bearer", "api"))

  private def unauthorized(message: String): IO[Response[IO]] =
    Unauthorized(bearerChallenge, Json.obj("error" -> Json.fromString(message)))

  given Encoder[InventoryEntry] = Encoder.forProduct3("userId", "datasheetId", "quantity")(
    (e: InventoryEntry) => (e.userId, e.datasheetId, e.quantity)
  )

  def routes(userXa: Transactor[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ GET -> Root / "api" / "inventory" =>
      AuthMiddleware.extractUser(req, userXa).flatMap {
        case None => unauthorized("Authentication required")
        case Some(user) =>
          InventoryRepository.getByUser(user.id)(userXa).flatMap(Ok(_))
      }

    case req @ PUT -> Root / "api" / "inventory" =>
      AuthMiddleware.extractUser(req, userXa).flatMap {
        case None => unauthorized("Authentication required")
        case Some(user) =>
          req.as[UpsertInventoryRequest].flatMap { body =>
            DatasheetId.parse(body.datasheetId) match {
              case Left(_) =>
                BadRequest(Json.obj("error" -> Json.fromString(s"Invalid datasheet ID: ${body.datasheetId}")))
              case Right(dsId) =>
                InventoryRepository.upsert(user.id, dsId, body.quantity)(userXa).flatMap(Ok(_))
            }
          }
      }

    case req @ PUT -> Root / "api" / "inventory" / "bulk" =>
      AuthMiddleware.extractUser(req, userXa).flatMap {
        case None => unauthorized("Authentication required")
        case Some(user) =>
          req.as[BulkUpsertInventoryRequest].flatMap { body =>
            val parsed = body.entries.map(e => DatasheetId.parse(e.datasheetId).map(id => (id, e.quantity)))
            val errors = parsed.collect { case Left(err) => err }
            if (errors.nonEmpty) {
              BadRequest(Json.obj("error" -> Json.fromString("Invalid datasheet IDs in request")))
            } else {
              val entries = parsed.collect { case Right(pair) => pair }
              InventoryRepository.bulkUpsert(user.id, entries)(userXa).flatMap(Ok(_))
            }
          }
      }

    case req @ DELETE -> Root / "api" / "inventory" / datasheetIdStr =>
      AuthMiddleware.extractUser(req, userXa).flatMap {
        case None => unauthorized("Authentication required")
        case Some(user) =>
          DatasheetId.parse(datasheetIdStr) match {
            case Left(_) =>
              BadRequest(Json.obj("error" -> Json.fromString(s"Invalid datasheet ID: $datasheetIdStr")))
            case Right(dsId) =>
              InventoryRepository.deleteEntry(user.id, dsId)(userXa).flatMap {
                case true => NoContent()
                case false => NotFound(Json.obj("error" -> Json.fromString("Inventory entry not found")))
              }
          }
      }
  }
}
