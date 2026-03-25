package wp40k.http.endpoints

import io.circe.Json
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import wp40k.http.TapirSecurity
import wp40k.http.dto.{RevisionDto, RevisionDiffDto}

object RevisionEndpoints {

  val listRevisions: PublicEndpoint[Unit, Json, List[RevisionDto], Any] =
    endpoint.get
      .in("api" / "revisions")
      .errorOut(jsonBody[Json])
      .out(jsonBody[List[RevisionDto]])

  val activeRevision: PublicEndpoint[Unit, Json, RevisionDto, Any] =
    endpoint.get
      .in("api" / "revisions" / "active")
      .errorOut(jsonBody[Json])
      .out(jsonBody[RevisionDto])

  val activateRevision: Endpoint[(Option[String], Option[String]), String, (StatusCode, Json), Json, Any] =
    endpoint.put
      .securityIn(TapirSecurity.tokenInput)
      .in("api" / "revisions" / path[String]("revisionId") / "activate")
      .errorOut(statusCode.and(jsonBody[Json]))
      .out(jsonBody[Json])

  val checkForUpdate: Endpoint[(Option[String], Option[String]), Unit, (StatusCode, Json), Json, Any] =
    endpoint.post
      .securityIn(TapirSecurity.tokenInput)
      .in("api" / "revisions" / "check")
      .errorOut(statusCode.and(jsonBody[Json]))
      .out(jsonBody[Json])

  val diffRevisions: PublicEndpoint[(String, String), Json, RevisionDiffDto, Any] =
    endpoint.get
      .in("api" / "revisions" / path[String]("oldId") / "diff" / path[String]("newId"))
      .errorOut(jsonBody[Json])
      .out(jsonBody[RevisionDiffDto])

  val all = List(listRevisions, activeRevision, diffRevisions, activateRevision, checkForUpdate)
}
