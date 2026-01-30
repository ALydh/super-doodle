package wahapedia.http.routes

import cats.effect.IO
import io.circe.Json
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*
import wahapedia.db.ReferenceDataRepository
import wahapedia.domain.types.*
import wahapedia.http.dto.DatasheetDetail
import doobie.*

object DatasheetRoutes {
  def routes(xa: Transactor[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "api" / "datasheets" / datasheetIdStr =>
      DatasheetId.parse(datasheetIdStr) match {
        case Right(datasheetId) =>
          ReferenceDataRepository.datasheetById(datasheetId)(xa).flatMap {
            case None =>
              NotFound(Json.obj("error" -> Json.fromString(s"Datasheet not found: $datasheetIdStr")))
            case Some(datasheet) =>
              for {
                profiles <- ReferenceDataRepository.modelProfilesForDatasheet(datasheetId)(xa)
                wargear <- ReferenceDataRepository.wargearForDatasheet(datasheetId)(xa)
                costs <- ReferenceDataRepository.unitCostsForDatasheet(datasheetId)(xa)
                keywords <- ReferenceDataRepository.keywordsForDatasheet(datasheetId)(xa)
                abilities <- ReferenceDataRepository.abilitiesForDatasheet(datasheetId)(xa)
                stratagems <- ReferenceDataRepository.stratagemsByDatasheet(datasheetId)(xa)
                options <- ReferenceDataRepository.optionsForDatasheet(datasheetId)(xa)
                resp <- Ok(DatasheetDetail(datasheet, profiles, wargear, costs, keywords, abilities, stratagems, options))
              } yield resp
          }
        case Left(_) =>
          BadRequest(Json.obj("error" -> Json.fromString(s"Invalid datasheet ID: $datasheetIdStr")))
      }

    case GET -> Root / "api" / "detachments" / detachmentIdStr / "abilities" =>
      ReferenceDataRepository.detachmentAbilitiesByDetachmentId(detachmentIdStr)(xa).flatMap(Ok(_))

    case GET -> Root / "api" / "weapon-abilities" =>
      ReferenceDataRepository.allWeaponAbilities(xa).flatMap(Ok(_))
  }
}
