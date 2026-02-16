package wahapedia.http.endpoints

import io.circe.Json
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import wahapedia.db.InventoryEntry
import wahapedia.http.TapirSecurity
import wahapedia.http.dto.{UpsertInventoryRequest, BulkUpsertInventoryRequest}

object InventoryEndpoints {

  val listInventory: Endpoint[(Option[String], Option[String]), Unit, (StatusCode, Json), List[InventoryEntry], Any] =
    endpoint.get
      .securityIn(TapirSecurity.tokenInput)
      .in("api" / "inventory")
      .errorOut(statusCode.and(jsonBody[Json]))
      .out(jsonBody[List[InventoryEntry]])

  val upsertInventory: Endpoint[(Option[String], Option[String]), UpsertInventoryRequest, (StatusCode, Json), InventoryEntry, Any] =
    endpoint.put
      .securityIn(TapirSecurity.tokenInput)
      .in("api" / "inventory")
      .in(jsonBody[UpsertInventoryRequest])
      .errorOut(statusCode.and(jsonBody[Json]))
      .out(jsonBody[InventoryEntry])

  val bulkUpsertInventory: Endpoint[(Option[String], Option[String]), BulkUpsertInventoryRequest, (StatusCode, Json), List[InventoryEntry], Any] =
    endpoint.put
      .securityIn(TapirSecurity.tokenInput)
      .in("api" / "inventory" / "bulk")
      .in(jsonBody[BulkUpsertInventoryRequest])
      .errorOut(statusCode.and(jsonBody[Json]))
      .out(jsonBody[List[InventoryEntry]])

  val deleteInventory: Endpoint[(Option[String], Option[String]), String, (StatusCode, Json), Unit, Any] =
    endpoint.delete
      .securityIn(TapirSecurity.tokenInput)
      .in("api" / "inventory" / path[String]("datasheetId"))
      .errorOut(statusCode.and(jsonBody[Json]))
      .out(statusCode(StatusCode.NoContent))

  val all = List(listInventory, upsertInventory, bulkUpsertInventory, deleteInventory)
}
