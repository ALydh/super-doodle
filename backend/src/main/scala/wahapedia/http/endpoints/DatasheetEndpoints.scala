package wahapedia.http.endpoints

import io.circe.Json
import io.circe.generic.auto.*
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import wahapedia.domain.models.*
import wahapedia.domain.types.*
import wahapedia.http.dto.{DatasheetDetail, FilterWargearRequest, WargearWithQuantity}
import wahapedia.http.CirceCodecs.given

object DatasheetEndpoints {

  val getDatasheet: PublicEndpoint[String, Json, DatasheetDetail, Any] =
    endpoint.get
      .in("api" / "datasheets" / path[String]("id"))
      .errorOut(jsonBody[Json])
      .out(jsonBody[DatasheetDetail])

  val filterWargear: PublicEndpoint[(String, FilterWargearRequest), Json, List[WargearWithQuantity], Any] =
    endpoint.post
      .in("api" / "datasheets" / path[String]("id") / "filter-wargear")
      .in(jsonBody[FilterWargearRequest])
      .errorOut(jsonBody[Json])
      .out(jsonBody[List[WargearWithQuantity]])

  val getDetachmentAbilities: PublicEndpoint[String, Json, List[DetachmentAbility], Any] =
    endpoint.get
      .in("api" / "detachments" / path[String]("id") / "abilities")
      .errorOut(jsonBody[Json])
      .out(jsonBody[List[DetachmentAbility]])

  val getWeaponAbilities: PublicEndpoint[Unit, Unit, List[WeaponAbility], Any] =
    endpoint.get
      .in("api" / "weapon-abilities")
      .out(jsonBody[List[WeaponAbility]])

  val getCoreAbilities: PublicEndpoint[Unit, Unit, List[Ability], Any] =
    endpoint.get
      .in("api" / "core-abilities")
      .out(jsonBody[List[Ability]])

  val listAllDatasheets: PublicEndpoint[Unit, Unit, List[Datasheet], Any] =
    endpoint.get
      .in("api" / "datasheets")
      .out(jsonBody[List[Datasheet]])

  val listAllStratagems: PublicEndpoint[Unit, Unit, List[Stratagem], Any] =
    endpoint.get
      .in("api" / "stratagems")
      .out(jsonBody[List[Stratagem]])

  val listAllEnhancements: PublicEndpoint[Unit, Unit, List[Enhancement], Any] =
    endpoint.get
      .in("api" / "enhancements")
      .out(jsonBody[List[Enhancement]])

  val all = List(getDatasheet, filterWargear, getDetachmentAbilities, getWeaponAbilities, getCoreAbilities, listAllDatasheets, listAllStratagems, listAllEnhancements)
}
