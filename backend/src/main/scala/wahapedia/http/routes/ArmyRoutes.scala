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
import wahapedia.domain.types.*
import wahapedia.domain.army.*
import wahapedia.domain.auth.AuthenticatedUser
import wahapedia.http.AuthMiddleware
import wahapedia.http.CirceCodecs.given
import wahapedia.http.dto.*
import doobie.*
import java.util.UUID

object ArmyRoutes {
  private val bearerChallenge = `WWW-Authenticate`(Challenge("Bearer", "api"))

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
            val armyId = UUID.randomUUID().toString
            ArmyRepository.create(armyId, createReq.name, createReq.army, Some(user.id))(xa).flatMap(Created(_))
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
                ArmyRepository.update(armyId, updateReq.name, updateReq.army)(xa).flatMap {
                  case Some(updated) => Ok(updated)
                  case None => InternalServerError(Json.obj("error" -> Json.fromString("Failed to update army")))
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
  }
}
