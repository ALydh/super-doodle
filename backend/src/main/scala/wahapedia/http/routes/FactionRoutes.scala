package wahapedia.http.routes

import cats.effect.IO
import io.circe.Json
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*
import wahapedia.db.{ArmyRepository, ReferenceDataRepository}
import wahapedia.domain.types.*
import doobie.*

object FactionRoutes {
  def routes(xa: Transactor[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "api" / "factions" =>
      ReferenceDataRepository.allFactions(xa).flatMap(Ok(_))

    case GET -> Root / "api" / "factions" / factionIdStr / "datasheets" =>
      FactionId.parse(factionIdStr) match {
        case Right(factionId) =>
          ReferenceDataRepository.datasheetsByFaction(factionId)(xa).flatMap(Ok(_))
        case Left(_) =>
          BadRequest(Json.obj("error" -> Json.fromString(s"Invalid faction ID: $factionIdStr")))
      }

    case GET -> Root / "api" / "factions" / factionIdStr / "detachments" =>
      FactionId.parse(factionIdStr) match {
        case Right(factionId) =>
          ReferenceDataRepository.detachmentsByFaction(factionId)(xa).flatMap(Ok(_))
        case Left(_) =>
          BadRequest(Json.obj("error" -> Json.fromString(s"Invalid faction ID: $factionIdStr")))
      }

    case GET -> Root / "api" / "factions" / factionIdStr / "enhancements" =>
      FactionId.parse(factionIdStr) match {
        case Right(factionId) =>
          ReferenceDataRepository.enhancementsByFaction(factionId)(xa).flatMap(Ok(_))
        case Left(_) =>
          BadRequest(Json.obj("error" -> Json.fromString(s"Invalid faction ID: $factionIdStr")))
      }

    case GET -> Root / "api" / "factions" / factionIdStr / "leaders" =>
      FactionId.parse(factionIdStr) match {
        case Right(factionId) =>
          ReferenceDataRepository.leadersByFaction(factionId)(xa).flatMap(Ok(_))
        case Left(_) =>
          BadRequest(Json.obj("error" -> Json.fromString(s"Invalid faction ID: $factionIdStr")))
      }

    case GET -> Root / "api" / "factions" / factionIdStr / "stratagems" =>
      FactionId.parse(factionIdStr) match {
        case Right(factionId) =>
          ReferenceDataRepository.stratagemsByFaction(factionId)(xa).flatMap(Ok(_))
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
  }
}
