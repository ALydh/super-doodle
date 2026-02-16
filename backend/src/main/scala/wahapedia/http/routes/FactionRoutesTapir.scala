package wahapedia.http.routes

import cats.data.{Kleisli, NonEmptyList}
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe.generic.auto.*
import org.http4s.HttpRoutes
import org.http4s.headers.`Cache-Control`
import org.http4s.CacheDirective
import sttp.tapir.server.http4s.Http4sServerInterpreter
import wahapedia.db.{ArmyRepository, ReferenceDataRepository}
import wahapedia.domain.types.*
import wahapedia.domain.army.AllyRules
import wahapedia.http.dto.{DatasheetDetail, AlliedFactionInfo}
import wahapedia.http.endpoints.FactionEndpoints
import wahapedia.http.CirceCodecs.given
import doobie.*

import scala.concurrent.duration.*

object FactionRoutesTapir {
  private val cacheHeaders = `Cache-Control`(CacheDirective.public, CacheDirective.`max-age`(60.seconds), CacheDirective.`must-revalidate`)

  def routes(xa: Transactor[IO]): HttpRoutes[IO] = {
    val listFactionsRoute = Http4sServerInterpreter[IO]().toRoutes(
      FactionEndpoints.listFactions.serverLogic { _ =>
        ReferenceDataRepository.allFactions(xa).map(Right(_))
      }
    )

    val listDatasheetsRoute = Http4sServerInterpreter[IO]().toRoutes(
      FactionEndpoints.listDatasheets.serverLogic { factionIdStr =>
        FactionId.parse(factionIdStr) match {
          case Left(_) =>
            IO.pure(Left(Json.obj("error" -> Json.fromString(s"Invalid faction ID: $factionIdStr"))))
          case Right(factionId) =>
            ReferenceDataRepository.datasheetsByFaction(factionId)(xa).map(Right(_))
        }
      }
    )

    val listDatasheetDetailsRoute = Http4sServerInterpreter[IO]().toRoutes(
      FactionEndpoints.listDatasheetDetails.serverLogic { factionIdStr =>
        FactionId.parse(factionIdStr) match {
          case Left(_) =>
            IO.pure(Left(Json.obj("error" -> Json.fromString(s"Invalid faction ID: $factionIdStr"))))
          case Right(factionId) =>
            ReferenceDataRepository.datasheetsByFaction(factionId)(xa).flatMap { datasheets =>
              NonEmptyList.fromList(datasheets.map(_.id)) match {
                case None => IO.pure(Right(List.empty[DatasheetDetail]))
                case Some(ids) =>
                  for {
                    profiles <- ReferenceDataRepository.modelProfilesForDatasheets(ids)(xa)
                    wargear <- ReferenceDataRepository.wargearForDatasheets(ids)(xa)
                    costs <- ReferenceDataRepository.unitCostsForDatasheets(ids)(xa)
                    keywords <- ReferenceDataRepository.keywordsForDatasheets(ids)(xa)
                    abilities <- ReferenceDataRepository.abilitiesForDatasheets(ids)(xa)
                    options <- ReferenceDataRepository.optionsForDatasheets(ids)(xa)
                    stratagemsWithDs <- ReferenceDataRepository.stratagemsByDatasheets(ids)(xa)
                    parsedWargearOpts <- ReferenceDataRepository.parsedWargearOptionsForDatasheets(ids)(xa)
                  } yield {
                    val profilesByDs = profiles.groupBy(_.datasheetId)
                    val wargearByDs = wargear.groupBy(_.datasheetId)
                    val costsByDs = costs.groupBy(_.datasheetId)
                    val keywordsByDs = keywords.groupBy(_.datasheetId)
                    val abilitiesByDs = abilities.groupBy(_.datasheetId)
                    val optionsByDs = options.groupBy(_.datasheetId)
                    val stratagemsByDs = stratagemsWithDs.groupBy(_._1).view.mapValues(_.map(_._2)).toMap
                    val parsedWargearOptsByDs = parsedWargearOpts.groupBy(_.datasheetId)
                    Right(datasheets.map { ds =>
                      DatasheetDetail(
                        ds,
                        profilesByDs.getOrElse(ds.id, Nil),
                        wargearByDs.getOrElse(ds.id, Nil),
                        costsByDs.getOrElse(ds.id, Nil),
                        keywordsByDs.getOrElse(ds.id, Nil),
                        abilitiesByDs.getOrElse(ds.id, Nil),
                        stratagemsByDs.getOrElse(ds.id, Nil),
                        optionsByDs.getOrElse(ds.id, Nil),
                        parsedWargearOptsByDs.getOrElse(ds.id, Nil)
                      )
                    })
                  }
              }
            }
        }
      }
    )

    val listDetachmentsRoute = Http4sServerInterpreter[IO]().toRoutes(
      FactionEndpoints.listDetachments.serverLogic { factionIdStr =>
        FactionId.parse(factionIdStr) match {
          case Left(_) =>
            IO.pure(Left(Json.obj("error" -> Json.fromString(s"Invalid faction ID: $factionIdStr"))))
          case Right(factionId) =>
            ReferenceDataRepository.detachmentsByFaction(factionId)(xa).map(Right(_))
        }
      }
    )

    val listEnhancementsRoute = Http4sServerInterpreter[IO]().toRoutes(
      FactionEndpoints.listEnhancements.serverLogic { factionIdStr =>
        FactionId.parse(factionIdStr) match {
          case Left(_) =>
            IO.pure(Left(Json.obj("error" -> Json.fromString(s"Invalid faction ID: $factionIdStr"))))
          case Right(factionId) =>
            ReferenceDataRepository.enhancementsByFaction(factionId)(xa).map(Right(_))
        }
      }
    )

    val listLeadersRoute = Http4sServerInterpreter[IO]().toRoutes(
      FactionEndpoints.listLeaders.serverLogic { factionIdStr =>
        FactionId.parse(factionIdStr) match {
          case Left(_) =>
            IO.pure(Left(Json.obj("error" -> Json.fromString(s"Invalid faction ID: $factionIdStr"))))
          case Right(factionId) =>
            ReferenceDataRepository.leadersByFaction(factionId)(xa).map(Right(_))
        }
      }
    )

    val listStratagemsRoute = Http4sServerInterpreter[IO]().toRoutes(
      FactionEndpoints.listStratagems.serverLogic { factionIdStr =>
        FactionId.parse(factionIdStr) match {
          case Left(_) =>
            IO.pure(Left(Json.obj("error" -> Json.fromString(s"Invalid faction ID: $factionIdStr"))))
          case Right(factionId) =>
            ReferenceDataRepository.stratagemsByFaction(factionId)(xa).map(Right(_))
        }
      }
    )

    val listArmiesRoute = Http4sServerInterpreter[IO]().toRoutes(
      FactionEndpoints.listArmies.serverLogic { factionIdStr =>
        FactionId.parse(factionIdStr) match {
          case Left(_) =>
            IO.pure(Left(Json.obj("error" -> Json.fromString(s"Invalid faction ID: $factionIdStr"))))
          case Right(factionId) =>
            ArmyRepository.listSummariesByFaction(factionId)(xa).map(Right(_))
        }
      }
    )

    val listAvailableAlliesRoute = Http4sServerInterpreter[IO]().toRoutes(
      FactionEndpoints.listAvailableAllies.serverLogic { factionIdStr =>
        FactionId.parse(factionIdStr) match {
          case Left(_) =>
            IO.pure(Left(Json.obj("error" -> Json.fromString(s"Invalid faction ID: $factionIdStr"))))
          case Right(factionId) =>
            for {
              keywords <- ReferenceDataRepository.factionKeywordsForFaction(factionId)(xa)
              allFactions <- ReferenceDataRepository.allFactions(xa)
              superKeywords = keywords.flatMap(_.keyword).toSet
              allowedAllies = AllyRules.allowedAllies(superKeywords).filterNot(_.factionId == factionId)
              alliedData <- allowedAllies.traverse { ally =>
                ReferenceDataRepository.datasheetsByFaction(ally.factionId)(xa).map { datasheets =>
                  val factionName = allFactions.find(_.id == ally.factionId).map(_.name).getOrElse(FactionId.value(ally.factionId))
                  AlliedFactionInfo(
                    FactionId.value(ally.factionId),
                    factionName,
                    ally.allyType.toString,
                    datasheets.filterNot(_.virtual)
                  )
                }
              }
            } yield Right(alliedData)
        }
      }
    )

    val tapirRoutes = listFactionsRoute <+> listDatasheetsRoute <+> listDatasheetDetailsRoute <+>
      listDetachmentsRoute <+> listEnhancementsRoute <+> listLeadersRoute <+>
      listStratagemsRoute <+> listArmiesRoute <+> listAvailableAlliesRoute

    Kleisli { req =>
      tapirRoutes(req).map(_.putHeaders(cacheHeaders))
    }
  }
}
