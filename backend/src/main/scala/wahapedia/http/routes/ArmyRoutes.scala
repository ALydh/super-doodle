package wahapedia.http.routes

import cats.effect.IO
import io.circe.Json
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*
import org.http4s.headers.`WWW-Authenticate`
import wahapedia.db.{ArmyRepository, ReferenceDataRepository, PersistedArmy}
import cats.data.NonEmptyList
import wahapedia.domain.types.*
import wahapedia.domain.army.*
import wahapedia.domain.auth.AuthenticatedUser
import wahapedia.http.{AuthMiddleware, InputValidation}
import wahapedia.http.CirceCodecs.given
import wahapedia.http.dto.*
import wahapedia.domain.models.EnhancementId
import doobie.*
import java.util.UUID

object ArmyRoutes {
  private val bearerChallenge = `WWW-Authenticate`(Challenge("Bearer", "api"))

  private val unitSizePattern = """(\d+)\s*model""".r

  private def parseUnitSizeFromDescription(description: String): Option[Int] =
    unitSizePattern.findFirstMatchIn(description.toLowerCase).map(_.group(1).toInt)

  private def unauthorized(message: String): IO[Response[IO]] =
    Unauthorized(bearerChallenge, Json.obj("error" -> Json.fromString(message)))

  private def validateArmy(army: Army, xa: Transactor[IO]): IO[ValidationResponse] =
    ReferenceDataRepository.loadReferenceData(xa).map { ref =>
      val errors = ArmyValidator.validate(army, ref).map(ValidationErrorDto.fromDomain)
      ValidationResponse(errors.isEmpty, errors)
    }

  private def isOwner(army: PersistedArmy, user: Option[AuthenticatedUser]): Boolean =
    (army.ownerId, user) match {
      case (None, _) => true
      case (Some(ownerId), Some(u)) => ownerId == UserId.value(u.id)
      case _ => false
    }

  def routes(xa: Transactor[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "api" / "armies" =>
      ArmyRepository.listSummaries(xa).flatMap(Ok(_))

    case GET -> Root / "api" / "armies" / armyId =>
      ArmyRepository.findById(armyId)(xa).flatMap {
        case Some(army) => Ok(army)
        case None => NotFound(Json.obj("error" -> Json.fromString(s"Army not found: $armyId")))
      }

    case req @ POST -> Root / "api" / "armies" =>
      AuthMiddleware.extractUser(req, xa).flatMap {
        case None => unauthorized("Authentication required")
        case Some(user) =>
          req.as[CreateArmyRequest].flatMap { createReq =>
            InputValidation.validateArmyName(createReq.name) match {
              case Left(err) =>
                BadRequest(Json.obj("error" -> Json.fromString(err.message)))
              case Right(validName) =>
                val armyId = UUID.randomUUID().toString
                ArmyRepository.create(armyId, validName, createReq.army, Some(user.id))(xa).flatMap(Created(_))
            }
          }
      }

    case req @ PUT -> Root / "api" / "armies" / armyId =>
      AuthMiddleware.extractUser(req, xa).flatMap { userOpt =>
        ArmyRepository.findById(armyId)(xa).flatMap {
          case None => NotFound(Json.obj("error" -> Json.fromString(s"Army not found: $armyId")))
          case Some(existingArmy) =>
            if (!isOwner(existingArmy, userOpt)) {
              Forbidden(Json.obj("error" -> Json.fromString("Not authorized to edit this army")))
            } else {
              req.as[CreateArmyRequest].flatMap { updateReq =>
                InputValidation.validateArmyName(updateReq.name) match {
                  case Left(err) =>
                    BadRequest(Json.obj("error" -> Json.fromString(err.message)))
                  case Right(validName) =>
                    ArmyRepository.update(armyId, validName, updateReq.army)(xa).flatMap {
                      case Some(updated) => Ok(updated)
                      case None => InternalServerError(Json.obj("error" -> Json.fromString("Failed to update army")))
                    }
                }
              }
            }
        }
      }

    case req @ DELETE -> Root / "api" / "armies" / armyId =>
      AuthMiddleware.extractUser(req, xa).flatMap { userOpt =>
        ArmyRepository.findById(armyId)(xa).flatMap {
          case None => NotFound(Json.obj("error" -> Json.fromString(s"Army not found: $armyId")))
          case Some(existingArmy) =>
            if (!isOwner(existingArmy, userOpt)) {
              Forbidden(Json.obj("error" -> Json.fromString("Not authorized to delete this army")))
            } else {
              ArmyRepository.delete(armyId)(xa).flatMap(_ => NoContent())
            }
        }
      }

    case req @ POST -> Root / "api" / "armies" / "validate" =>
      req.as[Army].flatMap { army =>
        validateArmy(army, xa).flatMap(Ok(_))
      }

    case GET -> Root / "api" / "armies" / armyId / "battle" =>
      ArmyRepository.findById(armyId)(xa).flatMap {
        case None => NotFound(Json.obj("error" -> Json.fromString(s"Army not found: $armyId")))
        case Some(persisted) =>
          val army = persisted.army
          NonEmptyList.fromList(army.units.map(_.datasheetId).distinct) match {
            case None => Ok(ArmyBattleData(
              persisted.id, persisted.name,
              FactionId.value(army.factionId), army.battleSize.toString,
              DetachmentId.value(army.detachmentId), DatasheetId.value(army.warlordId),
              List.empty
            ))
            case Some(datasheetIds) =>
              for {
                datasheets <- ReferenceDataRepository.datasheetsByFaction(army.factionId)(xa)
                profiles <- ReferenceDataRepository.modelProfilesForDatasheets(datasheetIds)(xa)
                wargear <- ReferenceDataRepository.wargearForDatasheets(datasheetIds)(xa)
                abilities <- ReferenceDataRepository.abilitiesForDatasheets(datasheetIds)(xa)
                keywords <- ReferenceDataRepository.keywordsForDatasheets(datasheetIds)(xa)
                costs <- ReferenceDataRepository.unitCostsForDatasheets(datasheetIds)(xa)
                parsedOptions <- ReferenceDataRepository.parsedWargearOptionsForDatasheets(datasheetIds)(xa)
                enhancements <- ReferenceDataRepository.enhancementsByFaction(army.factionId)(xa)
                wargearDefaults <- ReferenceDataRepository.wargearDefaultsForDatasheets(datasheetIds)(xa)

                datasheetMap = datasheets.map(d => DatasheetId.value(d.id) -> d).toMap
                profilesMap = profiles.groupBy(p => DatasheetId.value(p.datasheetId))
                wargearMap = wargear.groupBy(w => DatasheetId.value(w.datasheetId))
                abilitiesMap = abilities.groupBy(a => DatasheetId.value(a.datasheetId))
                keywordsMap = keywords.groupBy(k => DatasheetId.value(k.datasheetId))
                costsMap = costs.groupBy(c => DatasheetId.value(c.datasheetId))
                parsedOptionsMap = parsedOptions.groupBy(o => DatasheetId.value(o.datasheetId))
                enhancementsMap = enhancements.map(e => EnhancementId.value(e.id) -> e).toMap

                battleUnits = army.units.flatMap { unit =>
                  val dsId = DatasheetId.value(unit.datasheetId)
                  datasheetMap.get(dsId).map { datasheet =>
                    val unitCost = costsMap.getOrElse(dsId, List.empty).find(_.line == unit.sizeOptionLine)
                    val unitSize = unitCost.flatMap(c => parseUnitSizeFromDescription(c.description)).getOrElse(1)
                    val enh = unit.enhancementId.flatMap(eid => enhancementsMap.get(EnhancementId.value(eid)))
                    val allWargear = wargearMap.getOrElse(dsId, List.empty)
                    val parsedOpts = parsedOptionsMap.getOrElse(dsId, List.empty)
                    val defaults = wargearDefaults.getOrElse((dsId, unit.sizeOptionLine), List.empty)
                      .map(d => WargearDefault(d.weapon, d.count, d.modelType))
                    val filteredWargear = WargearFilter.filterWargearWithDefaults(
                      allWargear, parsedOpts, unit.wargearSelections, defaults, unitSize
                    )
                    val wargearDtos = filteredWargear.map(w => wahapedia.http.dto.WargearWithQuantity(w.wargear, w.quantity, w.modelType))
                    BattleUnitData(
                      unit, datasheet,
                      profilesMap.getOrElse(dsId, List.empty),
                      wargearDtos,
                      abilitiesMap.getOrElse(dsId, List.empty),
                      keywordsMap.getOrElse(dsId, List.empty),
                      unitCost, enh
                    )
                  }
                }

                resp <- Ok(ArmyBattleData(
                  persisted.id, persisted.name,
                  FactionId.value(army.factionId), army.battleSize.toString,
                  DetachmentId.value(army.detachmentId), DatasheetId.value(army.warlordId),
                  battleUnits
                ))
              } yield resp
          }
      }
  }
}
