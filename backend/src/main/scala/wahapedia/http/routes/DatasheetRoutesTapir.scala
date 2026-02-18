package wahapedia.http.routes

import cats.data.Kleisli
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe.generic.auto.*
import org.http4s.HttpRoutes
import org.http4s.headers.`Cache-Control`
import org.http4s.CacheDirective
import sttp.tapir.server.http4s.Http4sServerInterpreter
import wahapedia.db.ReferenceDataRepository
import wahapedia.domain.types.*
import wahapedia.domain.army.{WargearFilter, WargearDefault}
import wahapedia.http.dto.{DatasheetDetail, FilterWargearRequest, WargearWithQuantity}
import wahapedia.http.endpoints.DatasheetEndpoints
import wahapedia.http.CirceCodecs.given
import doobie.*

import scala.concurrent.duration.*

object DatasheetRoutesTapir {
  private val cacheHeaders = `Cache-Control`(CacheDirective.public, CacheDirective.`max-age`(3600.seconds))

  def routes(xa: Transactor[IO]): HttpRoutes[IO] = {
    val getDatasheetRoute = Http4sServerInterpreter[IO]().toRoutes(
      DatasheetEndpoints.getDatasheet.serverLogic { datasheetIdStr =>
        DatasheetId.parse(datasheetIdStr) match {
          case Left(_) =>
            IO.pure(Left(Json.obj("error" -> Json.fromString(s"Invalid datasheet ID: $datasheetIdStr"))))
          case Right(datasheetId) =>
            ReferenceDataRepository.datasheetById(datasheetId)(xa).flatMap {
              case None =>
                IO.pure(Left(Json.obj("error" -> Json.fromString(s"Datasheet not found: $datasheetIdStr"))))
              case Some(datasheet) =>
                for {
                  profiles <- ReferenceDataRepository.modelProfilesForDatasheet(datasheetId)(xa)
                  wargear <- ReferenceDataRepository.wargearForDatasheet(datasheetId)(xa)
                  costs <- ReferenceDataRepository.unitCostsForDatasheet(datasheetId)(xa)
                  keywords <- ReferenceDataRepository.keywordsForDatasheet(datasheetId)(xa)
                  abilities <- ReferenceDataRepository.abilitiesForDatasheet(datasheetId)(xa)
                  stratagems <- ReferenceDataRepository.stratagemsByDatasheet(datasheetId)(xa)
                  options <- ReferenceDataRepository.optionsForDatasheet(datasheetId)(xa)
                  parsedWargearOptions <- ReferenceDataRepository.parsedWargearOptionsForDatasheet(datasheetId)(xa)
                } yield Right(DatasheetDetail(datasheet, profiles, wargear, costs, keywords, abilities, stratagems, options, parsedWargearOptions))
            }
        }
      }
    )

    val filterWargearRoute = Http4sServerInterpreter[IO]().toRoutes(
      DatasheetEndpoints.filterWargear.serverLogic { (datasheetIdStr, filterReq) =>
        DatasheetId.parse(datasheetIdStr) match {
          case Left(_) =>
            IO.pure(Left(Json.obj("error" -> Json.fromString(s"Invalid datasheet ID: $datasheetIdStr"))))
          case Right(datasheetId) =>
            for {
              wargear <- ReferenceDataRepository.wargearForDatasheet(datasheetId)(xa)
              parsedOptions <- ReferenceDataRepository.parsedWargearOptionsForDatasheet(datasheetId)(xa)
              defaults <- ReferenceDataRepository.wargearDefaultsForDatasheet(datasheetId, filterReq.sizeOptionLine)(xa)
              domainDefaults = defaults.map(d => WargearDefault(d.weapon, d.count, d.modelType))
              filtered = WargearFilter.filterWargearWithDefaults(wargear, parsedOptions, filterReq.selections, domainDefaults, filterReq.unitSize)
              dtoFiltered = filtered.map(w => WargearWithQuantity(w.wargear, w.quantity, w.modelType))
            } yield Right(dtoFiltered)
        }
      }
    )

    val getDetachmentAbilitiesRoute = Http4sServerInterpreter[IO]().toRoutes(
      DatasheetEndpoints.getDetachmentAbilities.serverLogic { detachmentIdStr =>
        ReferenceDataRepository.detachmentAbilitiesByDetachmentId(detachmentIdStr)(xa)
          .map(Right(_))
      }
    )

    val getWeaponAbilitiesRoute = Http4sServerInterpreter[IO]().toRoutes(
      DatasheetEndpoints.getWeaponAbilities.serverLogic { _ =>
        ReferenceDataRepository.allWeaponAbilities(xa).map(Right(_))
      }
    )

    val getCoreAbilitiesRoute = Http4sServerInterpreter[IO]().toRoutes(
      DatasheetEndpoints.getCoreAbilities.serverLogic { _ =>
        ReferenceDataRepository.allAbilities(xa).map(Right(_))
      }
    )

    val listAllDatasheetsRoute = Http4sServerInterpreter[IO]().toRoutes(
      DatasheetEndpoints.listAllDatasheets.serverLogic { _ =>
        ReferenceDataRepository.allDatasheets(xa).map(Right(_))
      }
    )

    val listAllStratagemsRoute = Http4sServerInterpreter[IO]().toRoutes(
      DatasheetEndpoints.listAllStratagems.serverLogic { _ =>
        ReferenceDataRepository.allStratagems(xa).map(Right(_))
      }
    )

    val listAllEnhancementsRoute = Http4sServerInterpreter[IO]().toRoutes(
      DatasheetEndpoints.listAllEnhancements.serverLogic { _ =>
        ReferenceDataRepository.allEnhancements(xa).map(Right(_))
      }
    )

    val tapirRoutes = getDatasheetRoute <+> filterWargearRoute <+> getDetachmentAbilitiesRoute <+> getWeaponAbilitiesRoute <+> getCoreAbilitiesRoute <+> listAllDatasheetsRoute <+> listAllStratagemsRoute <+> listAllEnhancementsRoute

    Kleisli { req =>
      tapirRoutes(req).map(_.putHeaders(cacheHeaders))
    }
  }
}
