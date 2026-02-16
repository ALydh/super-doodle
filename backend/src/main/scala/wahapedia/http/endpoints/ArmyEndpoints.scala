package wahapedia.http.endpoints

import io.circe.Json
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import wahapedia.domain.army.{Army, ArmyUnit, WargearSelection}
import wahapedia.domain.auth.AuthenticatedUser
import wahapedia.domain.types.*
import wahapedia.db.{ArmySummary, PersistedArmy}
import wahapedia.http.TapirSecurity
import wahapedia.http.dto.CreateArmyRequest
import wahapedia.http.CirceCodecs.given

object ArmyEndpoints {
  given Schema[WargearSelection] = Schema.derived
  given Schema[ArmyUnit] = Schema.derived
  given Schema[Army] = Schema.derived

  val listArmies: PublicEndpoint[Unit, Json, List[ArmySummary], Any] =
    endpoint.get
      .in("api" / "armies")
      .errorOut(jsonBody[Json])
      .out(jsonBody[List[ArmySummary]])

  val getArmy: PublicEndpoint[String, Json, PersistedArmy, Any] =
    endpoint.get
      .in("api" / "armies" / path[String]("id"))
      .errorOut(jsonBody[Json])
      .out(jsonBody[PersistedArmy])

  val createArmy: Endpoint[(Option[String], Option[String]), CreateArmyRequest, (StatusCode, Json), PersistedArmy, Any] =
    endpoint.post
      .securityIn(TapirSecurity.tokenInput)
      .in("api" / "armies")
      .in(jsonBody[CreateArmyRequest])
      .errorOut(statusCode.and(jsonBody[Json]))
      .out(statusCode(StatusCode.Created).and(jsonBody[PersistedArmy]))

  val updateArmy: Endpoint[(Option[String], Option[String]), (String, CreateArmyRequest), (StatusCode, Json), PersistedArmy, Any] =
    endpoint.put
      .securityIn(TapirSecurity.tokenInput)
      .in("api" / "armies" / path[String]("id"))
      .in(jsonBody[CreateArmyRequest])
      .errorOut(statusCode.and(jsonBody[Json]))
      .out(jsonBody[PersistedArmy])

  val deleteArmy: Endpoint[(Option[String], Option[String]), String, (StatusCode, Json), Unit, Any] =
    endpoint.delete
      .securityIn(TapirSecurity.tokenInput)
      .in("api" / "armies" / path[String]("id"))
      .errorOut(statusCode.and(jsonBody[Json]))
      .out(statusCode(StatusCode.NoContent))

  val validateArmy: PublicEndpoint[Army, Json, Json, Any] =
    endpoint.post
      .in("api" / "armies" / "validate")
      .in(jsonBody[Army])
      .errorOut(jsonBody[Json])
      .out(jsonBody[Json])

  val getArmyBattle: PublicEndpoint[String, Json, Json, Any] =
    endpoint.get
      .in("api" / "armies" / path[String]("id") / "battle")
      .errorOut(jsonBody[Json])
      .out(jsonBody[Json])

  val all = List(listArmies, getArmy, createArmy, updateArmy, deleteArmy, validateArmy, getArmyBattle)
}
