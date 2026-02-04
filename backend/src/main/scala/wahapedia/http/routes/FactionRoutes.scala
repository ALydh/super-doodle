package wahapedia.http.routes

import cats.effect.IO
import cats.data.NonEmptyList
import cats.implicits.*
import io.circe.Json
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*
import org.http4s.headers.`Cache-Control`
import org.http4s.CacheDirective
import wahapedia.db.{ArmyRepository, ReferenceDataRepository}
import wahapedia.domain.types.*
import wahapedia.domain.army.AllyRules
import wahapedia.http.dto.{DatasheetDetail, AlliedFactionInfo}
import doobie.*

import scala.concurrent.duration.*

object FactionRoutes {
  private val cacheHeaders = `Cache-Control`(CacheDirective.public, CacheDirective.`max-age`(60.seconds), CacheDirective.`must-revalidate`)

  def routes(xa: Transactor[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "api" / "factions" =>
      ReferenceDataRepository.allFactions(xa).flatMap(Ok(_)).map(_.putHeaders(cacheHeaders))

    case GET -> Root / "api" / "factions" / factionIdStr / "datasheets" =>
      FactionId.parse(factionIdStr) match {
        case Right(factionId) =>
          ReferenceDataRepository.datasheetsByFaction(factionId)(xa).flatMap(Ok(_)).map(_.putHeaders(cacheHeaders))
        case Left(_) =>
          BadRequest(Json.obj("error" -> Json.fromString(s"Invalid faction ID: $factionIdStr")))
      }

    case GET -> Root / "api" / "factions" / factionIdStr / "datasheets" / "details" =>
      FactionId.parse(factionIdStr) match {
        case Right(factionId) =>
          ReferenceDataRepository.datasheetsByFaction(factionId)(xa).flatMap { datasheets =>
            NonEmptyList.fromList(datasheets.map(_.id)) match {
              case None => Ok(List.empty[DatasheetDetail]).map(_.putHeaders(cacheHeaders))
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
                  profilesByDs = profiles.groupBy(_.datasheetId)
                  wargearByDs = wargear.groupBy(_.datasheetId)
                  costsByDs = costs.groupBy(_.datasheetId)
                  keywordsByDs = keywords.groupBy(_.datasheetId)
                  abilitiesByDs = abilities.groupBy(_.datasheetId)
                  optionsByDs = options.groupBy(_.datasheetId)
                  stratagemsByDs = stratagemsWithDs.groupBy(_._1).view.mapValues(_.map(_._2)).toMap
                  parsedWargearOptsByDs = parsedWargearOpts.groupBy(_.datasheetId)
                  details = datasheets.map { ds =>
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
                  }
                  resp <- Ok(details)
                } yield resp.putHeaders(cacheHeaders)
            }
          }
        case Left(_) =>
          BadRequest(Json.obj("error" -> Json.fromString(s"Invalid faction ID: $factionIdStr")))
      }

    case GET -> Root / "api" / "factions" / factionIdStr / "detachments" =>
      FactionId.parse(factionIdStr) match {
        case Right(factionId) =>
          ReferenceDataRepository.detachmentsByFaction(factionId)(xa).flatMap(Ok(_)).map(_.putHeaders(cacheHeaders))
        case Left(_) =>
          BadRequest(Json.obj("error" -> Json.fromString(s"Invalid faction ID: $factionIdStr")))
      }

    case GET -> Root / "api" / "factions" / factionIdStr / "enhancements" =>
      FactionId.parse(factionIdStr) match {
        case Right(factionId) =>
          ReferenceDataRepository.enhancementsByFaction(factionId)(xa).flatMap(Ok(_)).map(_.putHeaders(cacheHeaders))
        case Left(_) =>
          BadRequest(Json.obj("error" -> Json.fromString(s"Invalid faction ID: $factionIdStr")))
      }

    case GET -> Root / "api" / "factions" / factionIdStr / "leaders" =>
      FactionId.parse(factionIdStr) match {
        case Right(factionId) =>
          ReferenceDataRepository.leadersByFaction(factionId)(xa).flatMap(Ok(_)).map(_.putHeaders(cacheHeaders))
        case Left(_) =>
          BadRequest(Json.obj("error" -> Json.fromString(s"Invalid faction ID: $factionIdStr")))
      }

    case GET -> Root / "api" / "factions" / factionIdStr / "stratagems" =>
      FactionId.parse(factionIdStr) match {
        case Right(factionId) =>
          ReferenceDataRepository.stratagemsByFaction(factionId)(xa).flatMap(Ok(_)).map(_.putHeaders(cacheHeaders))
        case Left(_) =>
          BadRequest(Json.obj("error" -> Json.fromString(s"Invalid faction ID: $factionIdStr")))
      }

    case GET -> Root / "api" / "factions" / factionIdStr / "armies" =>
      FactionId.parse(factionIdStr) match {
        case Right(factionId) =>
          ArmyRepository.listSummariesByFaction(factionId)(xa).flatMap(Ok(_))
        case Left(_) =>
          BadRequest(Json.obj("error" -> Json.fromString(s"Invalid faction ID: $factionIdStr")))
      }

    case GET -> Root / "api" / "factions" / factionIdStr / "available-allies" =>
      FactionId.parse(factionIdStr) match {
        case Right(factionId) =>
          for {
            keywords <- ReferenceDataRepository.factionKeywordsForFaction(factionId)(xa)
            allFactions <- ReferenceDataRepository.allFactions(xa)
            superKeywords = keywords.flatMap(_.keyword).toSet
            allowedAllies = AllyRules.allowedAllies(superKeywords)
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
            resp <- Ok(alliedData)
          } yield resp.putHeaders(cacheHeaders)
        case Left(_) =>
          BadRequest(Json.obj("error" -> Json.fromString(s"Invalid faction ID: $factionIdStr")))
      }
  }
}
