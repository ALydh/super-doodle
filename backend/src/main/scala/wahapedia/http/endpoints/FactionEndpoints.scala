package wahapedia.http.endpoints

import io.circe.Json
import io.circe.generic.auto.*
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import wahapedia.domain.models.*
import wahapedia.domain.types.*
import wahapedia.db.{ArmySummary, ReferenceDataRepository}
import wahapedia.http.dto.{DatasheetDetail, AlliedFactionInfo}
import wahapedia.http.CirceCodecs.given

object FactionEndpoints {

  val listFactions: PublicEndpoint[Unit, Json, List[Faction], Any] =
    endpoint.get
      .in("api" / "factions")
      .errorOut(jsonBody[Json])
      .out(jsonBody[List[Faction]])

  val listDatasheets: PublicEndpoint[String, Json, List[Datasheet], Any] =
    endpoint.get
      .in("api" / "factions" / path[String]("factionId") / "datasheets")
      .errorOut(jsonBody[Json])
      .out(jsonBody[List[Datasheet]])

  val listDatasheetDetails: PublicEndpoint[String, Json, List[DatasheetDetail], Any] =
    endpoint.get
      .in("api" / "factions" / path[String]("factionId") / "datasheets" / "details")
      .errorOut(jsonBody[Json])
      .out(jsonBody[List[DatasheetDetail]])

  val listDetachments: PublicEndpoint[String, Json, List[ReferenceDataRepository.DetachmentInfo], Any] =
    endpoint.get
      .in("api" / "factions" / path[String]("factionId") / "detachments")
      .errorOut(jsonBody[Json])
      .out(jsonBody[List[ReferenceDataRepository.DetachmentInfo]])

  val listEnhancements: PublicEndpoint[String, Json, List[Enhancement], Any] =
    endpoint.get
      .in("api" / "factions" / path[String]("factionId") / "enhancements")
      .errorOut(jsonBody[Json])
      .out(jsonBody[List[Enhancement]])

  val listLeaders: PublicEndpoint[String, Json, List[DatasheetLeader], Any] =
    endpoint.get
      .in("api" / "factions" / path[String]("factionId") / "leaders")
      .errorOut(jsonBody[Json])
      .out(jsonBody[List[DatasheetLeader]])

  val listStratagems: PublicEndpoint[String, Json, List[Stratagem], Any] =
    endpoint.get
      .in("api" / "factions" / path[String]("factionId") / "stratagems")
      .errorOut(jsonBody[Json])
      .out(jsonBody[List[Stratagem]])

  val listArmies: PublicEndpoint[String, Json, List[ArmySummary], Any] =
    endpoint.get
      .in("api" / "factions" / path[String]("factionId") / "armies")
      .errorOut(jsonBody[Json])
      .out(jsonBody[List[ArmySummary]])

  val listAvailableAllies: PublicEndpoint[String, Json, List[AlliedFactionInfo], Any] =
    endpoint.get
      .in("api" / "factions" / path[String]("factionId") / "available-allies")
      .errorOut(jsonBody[Json])
      .out(jsonBody[List[AlliedFactionInfo]])

  val all = List(
    listFactions, listDatasheets, listDatasheetDetails, listDetachments,
    listEnhancements, listLeaders, listStratagems, listArmies, listAvailableAllies
  )
}
