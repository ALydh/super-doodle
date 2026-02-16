package wahapedia.http.routes

import cats.data.NonEmptyList
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.HttpRoutes
import sttp.model.StatusCode
import sttp.tapir.server.http4s.Http4sServerInterpreter
import wahapedia.db.{ArmyRepository, ReferenceDataRepository}
import wahapedia.domain.types.*
import wahapedia.domain.army.*
import wahapedia.domain.auth.AuthenticatedUser
import wahapedia.http.{InputValidation, TapirSecurity}
import wahapedia.http.CirceCodecs.given
import wahapedia.http.dto.*
import wahapedia.http.endpoints.ArmyEndpoints
import wahapedia.domain.models.EnhancementId
import doobie.*
import java.util.UUID

object ArmyRoutesTapir {
  private val unitSizePattern = """(\d+)\s*model""".r

  private def parseUnitSizeFromDescription(description: String): Option[Int] =
    unitSizePattern.findFirstMatchIn(description.toLowerCase).map(_.group(1).toInt)

  private def isOwner(army: wahapedia.db.PersistedArmy, user: Option[AuthenticatedUser]): Boolean =
    (army.ownerId, user) match {
      case (None, _) => true
      case (Some(ownerId), Some(u)) => ownerId == UserId.value(u.id)
      case _ => false
    }

  private def validateArmy(army: Army, refXa: Transactor[IO]): IO[ValidationResponse] =
    ReferenceDataRepository.loadReferenceData(refXa).map { ref =>
      val errors = ArmyValidator.validate(army, ref).map(ValidationErrorDto.fromDomain)
      ValidationResponse(errors.isEmpty, errors)
    }

  def routes(refXa: Transactor[IO], userXa: Transactor[IO], refPrefix: String): HttpRoutes[IO] = {
    val listArmiesRoute = Http4sServerInterpreter[IO]().toRoutes(
      ArmyEndpoints.listArmies.serverLogic { _ =>
        ArmyRepository.listSummaries(userXa, refPrefix).map(Right(_))
      }
    )

    val getArmyRoute = Http4sServerInterpreter[IO]().toRoutes(
      ArmyEndpoints.getArmy.serverLogic { armyId =>
        ArmyRepository.findById(armyId)(userXa).map {
          case Some(army) => Right(army)
          case None => Left(Json.obj("error" -> Json.fromString(s"Army not found: $armyId")))
        }
      }
    )

    val createArmyRoute = Http4sServerInterpreter[IO]().toRoutes(
      ArmyEndpoints.createArmy
        .serverSecurityLogic(TapirSecurity.required(userXa))
        .serverLogic { user => createReq =>
          InputValidation.validateArmyName(createReq.name) match {
            case Left(err) =>
              IO.pure(Left((StatusCode.BadRequest, Json.obj("error" -> Json.fromString(err.message)))))
            case Right(validName) =>
              val armyId = UUID.randomUUID().toString
              ArmyRepository.create(armyId, validName, createReq.army, Some(user.id))(userXa)
                .map(Right(_))
          }
        }
    )

    val updateArmyRoute = Http4sServerInterpreter[IO]().toRoutes(
      ArmyEndpoints.updateArmy
        .serverSecurityLogic(TapirSecurity.optional(userXa))
        .serverLogic { userOpt => { case (armyId, updateReq) =>
          ArmyRepository.findById(armyId)(userXa).flatMap {
            case None =>
              IO.pure(Left((StatusCode.NotFound, Json.obj("error" -> Json.fromString(s"Army not found: $armyId")))))
            case Some(existingArmy) =>
              if (!isOwner(existingArmy, userOpt)) {
                IO.pure(Left((StatusCode.Forbidden, Json.obj("error" -> Json.fromString("Not authorized to edit this army")))))
              } else {
                InputValidation.validateArmyName(updateReq.name) match {
                  case Left(err) =>
                    IO.pure(Left((StatusCode.BadRequest, Json.obj("error" -> Json.fromString(err.message)))))
                  case Right(validName) =>
                    ArmyRepository.update(armyId, validName, updateReq.army)(userXa).map {
                      case Some(updated) => Right(updated)
                      case None => Left((StatusCode.InternalServerError, Json.obj("error" -> Json.fromString("Failed to update army"))))
                    }
                }
              }
          }
        }}
    )

    val deleteArmyRoute = Http4sServerInterpreter[IO]().toRoutes(
      ArmyEndpoints.deleteArmy
        .serverSecurityLogic(TapirSecurity.optional(userXa))
        .serverLogic { userOpt => armyId =>
          ArmyRepository.findById(armyId)(userXa).flatMap {
            case None =>
              IO.pure(Left((StatusCode.NotFound, Json.obj("error" -> Json.fromString(s"Army not found: $armyId")))))
            case Some(existingArmy) =>
              if (!isOwner(existingArmy, userOpt)) {
                IO.pure(Left((StatusCode.Forbidden, Json.obj("error" -> Json.fromString("Not authorized to delete this army")))))
              } else {
                ArmyRepository.delete(armyId)(userXa).map(_ => Right(()))
              }
          }
        }
    )

    val validateArmyRoute = Http4sServerInterpreter[IO]().toRoutes(
      ArmyEndpoints.validateArmy.serverLogic { army =>
        validateArmy(army, refXa).map(v => Right(v.asJson))
      }
    )

    val getArmyBattleRoute = Http4sServerInterpreter[IO]().toRoutes(
      ArmyEndpoints.getArmyBattle.serverLogic { armyId =>
        ArmyRepository.findById(armyId)(userXa).flatMap {
          case None =>
            IO.pure(Left(Json.obj("error" -> Json.fromString(s"Army not found: $armyId"))))
          case Some(persisted) =>
            val army = persisted.army
            NonEmptyList.fromList(army.units.map(_.datasheetId).distinct) match {
              case None =>
                IO.pure(Right(ArmyBattleData(
                  persisted.id, persisted.name,
                  FactionId.value(army.factionId), army.battleSize.toString,
                  DetachmentId.value(army.detachmentId), DatasheetId.value(army.warlordId),
                  army.chapterId, List.empty
                ).asJson))
              case Some(datasheetIds) =>
                for {
                  datasheets <- ReferenceDataRepository.datasheetsByFaction(army.factionId)(refXa)
                  profiles <- ReferenceDataRepository.modelProfilesForDatasheets(datasheetIds)(refXa)
                  wargear <- ReferenceDataRepository.wargearForDatasheets(datasheetIds)(refXa)
                  abilities <- ReferenceDataRepository.abilitiesForDatasheets(datasheetIds)(refXa)
                  keywords <- ReferenceDataRepository.keywordsForDatasheets(datasheetIds)(refXa)
                  costs <- ReferenceDataRepository.unitCostsForDatasheets(datasheetIds)(refXa)
                  parsedOptions <- ReferenceDataRepository.parsedWargearOptionsForDatasheets(datasheetIds)(refXa)
                  enhancements <- ReferenceDataRepository.enhancementsByFaction(army.factionId)(refXa)
                  wargearDefaults <- ReferenceDataRepository.wargearDefaultsForDatasheets(datasheetIds)(refXa)
                } yield {
                  val datasheetMap = datasheets.map(d => DatasheetId.value(d.id) -> d).toMap
                  val profilesMap = profiles.groupBy(p => DatasheetId.value(p.datasheetId))
                  val wargearMap = wargear.groupBy(w => DatasheetId.value(w.datasheetId))
                  val abilitiesMap = abilities.groupBy(a => DatasheetId.value(a.datasheetId))
                  val keywordsMap = keywords.groupBy(k => DatasheetId.value(k.datasheetId))
                  val costsMap = costs.groupBy(c => DatasheetId.value(c.datasheetId))
                  val parsedOptionsMap = parsedOptions.groupBy(o => DatasheetId.value(o.datasheetId))
                  val enhancementsMap = enhancements.map(e => EnhancementId.value(e.id) -> e).toMap

                  val battleUnits = army.units.flatMap { unit =>
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
                      val filteredAbilities = WargearFilter.filterAbilities(
                        abilitiesMap.getOrElse(dsId, List.empty), parsedOpts, unit.wargearSelections
                      )
                      BattleUnitData(
                        unit, datasheet,
                        profilesMap.getOrElse(dsId, List.empty),
                        wargearDtos,
                        filteredAbilities,
                        keywordsMap.getOrElse(dsId, List.empty),
                        unitCost, enh
                      )
                    }
                  }

                  Right(ArmyBattleData(
                    persisted.id, persisted.name,
                    FactionId.value(army.factionId), army.battleSize.toString,
                    DetachmentId.value(army.detachmentId), DatasheetId.value(army.warlordId),
                    army.chapterId, battleUnits
                  ).asJson)
                }
            }
        }
      }
    )

    listArmiesRoute <+> getArmyRoute <+> createArmyRoute <+> updateArmyRoute <+>
      deleteArmyRoute <+> validateArmyRoute <+> getArmyBattleRoute
  }
}
