package wahapedia.http

import cats.effect.{IO, Resource}
import cats.implicits.*
import com.comcast.ip4s.*
import io.circe.{Json, Encoder, Decoder, jawn}
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.CORS
import org.http4s.headers.`WWW-Authenticate`
import org.http4s.Challenge
import wahapedia.db.{ReferenceDataRepository, UserRepository, SessionRepository, InviteRepository}
import wahapedia.domain.types.*
import wahapedia.domain.models.*
import wahapedia.domain.army.*
import wahapedia.domain.auth.{AuthenticatedUser, Invite}
import wahapedia.auth.PasswordHasher
import doobie.*
import doobie.implicits.*
import wahapedia.db.DoobieMeta.given
import java.util.UUID
import java.time.Instant

case class DatasheetDetail(
  datasheet: Datasheet,
  profiles: List[ModelProfile],
  wargear: List[Wargear],
  costs: List[UnitCost],
  keywords: List[DatasheetKeyword],
  abilities: List[DatasheetAbility],
  stratagems: List[Stratagem],
  options: List[DatasheetOption]
)

case class CreateArmyRequest(name: String, army: Army)
case class ValidationResponse(valid: Boolean, errors: List[ValidationErrorDto])

sealed trait ValidationErrorDto
object ValidationErrorDto {
  case class FactionMismatch(errorType: String, unitDatasheetId: String, unitName: String, expectedFaction: String) extends ValidationErrorDto
  case class PointsExceeded(errorType: String, total: Int, limit: Int) extends ValidationErrorDto
  case class NoCharacter(errorType: String) extends ValidationErrorDto
  case class InvalidWarlord(errorType: String, warlordId: String) extends ValidationErrorDto
  case class WarlordNotInArmy(errorType: String, warlordId: String) extends ValidationErrorDto
  case class DuplicateExceeded(errorType: String, datasheetId: String, unitName: String, count: Int, maxAllowed: Int) extends ValidationErrorDto
  case class DuplicateEpicHero(errorType: String, datasheetId: String, unitName: String) extends ValidationErrorDto
  case class InvalidLeaderAttachment(errorType: String, leaderId: String, attachedToId: String) extends ValidationErrorDto
  case class TooManyEnhancements(errorType: String, count: Int) extends ValidationErrorDto
  case class DuplicateEnhancement(errorType: String, enhancementId: String) extends ValidationErrorDto
  case class EnhancementOnNonCharacter(errorType: String, datasheetId: String, enhancementId: String) extends ValidationErrorDto
  case class MultipleEnhancementsOnUnit(errorType: String, datasheetId: String) extends ValidationErrorDto
  case class EnhancementDetachmentMismatch(errorType: String, enhancementId: String, expectedDetachment: String) extends ValidationErrorDto
  case class UnitCostNotFound(errorType: String, datasheetId: String, sizeOptionLine: Int) extends ValidationErrorDto
  case class DatasheetNotFound(errorType: String, datasheetId: String) extends ValidationErrorDto
  case class Generic(errorType: String, message: String) extends ValidationErrorDto

  def fromDomain(err: ValidationError): ValidationErrorDto = err match {
    case e: wahapedia.domain.army.FactionMismatch =>
      FactionMismatch("FactionMismatch", DatasheetId.value(e.unitDatasheetId), e.unitName, FactionId.value(e.expectedFaction))
    case e: wahapedia.domain.army.PointsExceeded =>
      PointsExceeded("PointsExceeded", e.total, e.limit)
    case _: wahapedia.domain.army.NoCharacter =>
      NoCharacter("NoCharacter")
    case e: wahapedia.domain.army.InvalidWarlord =>
      InvalidWarlord("InvalidWarlord", DatasheetId.value(e.warlordId))
    case e: wahapedia.domain.army.WarlordNotInArmy =>
      WarlordNotInArmy("WarlordNotInArmy", DatasheetId.value(e.warlordId))
    case e: wahapedia.domain.army.DuplicateExceeded =>
      DuplicateExceeded("DuplicateExceeded", DatasheetId.value(e.datasheetId), e.unitName, e.count, e.maxAllowed)
    case e: wahapedia.domain.army.DuplicateEpicHero =>
      DuplicateEpicHero("DuplicateEpicHero", DatasheetId.value(e.datasheetId), e.unitName)
    case e: wahapedia.domain.army.InvalidLeaderAttachment =>
      InvalidLeaderAttachment("InvalidLeaderAttachment", DatasheetId.value(e.leaderId), DatasheetId.value(e.attachedToId))
    case e: wahapedia.domain.army.TooManyEnhancements =>
      TooManyEnhancements("TooManyEnhancements", e.count)
    case e: wahapedia.domain.army.DuplicateEnhancement =>
      DuplicateEnhancement("DuplicateEnhancement", EnhancementId.value(e.enhancementId))
    case e: wahapedia.domain.army.EnhancementOnNonCharacter =>
      EnhancementOnNonCharacter("EnhancementOnNonCharacter", DatasheetId.value(e.datasheetId), EnhancementId.value(e.enhancementId))
    case e: wahapedia.domain.army.MultipleEnhancementsOnUnit =>
      MultipleEnhancementsOnUnit("MultipleEnhancementsOnUnit", DatasheetId.value(e.datasheetId))
    case e: wahapedia.domain.army.EnhancementDetachmentMismatch =>
      EnhancementDetachmentMismatch("EnhancementDetachmentMismatch", EnhancementId.value(e.enhancementId), DetachmentId.value(e.expectedDetachment))
    case e: wahapedia.domain.army.UnitCostNotFound =>
      UnitCostNotFound("UnitCostNotFound", DatasheetId.value(e.datasheetId), e.sizeOptionLine)
    case e: wahapedia.domain.army.DatasheetNotFound =>
      DatasheetNotFound("DatasheetNotFound", DatasheetId.value(e.datasheetId))
  }

  given Encoder[ValidationErrorDto] = Encoder.instance {
    case e: FactionMismatch => e.asJson(using Encoder.AsObject.derived[FactionMismatch])
    case e: PointsExceeded => e.asJson(using Encoder.AsObject.derived[PointsExceeded])
    case e: NoCharacter => e.asJson(using Encoder.AsObject.derived[NoCharacter])
    case e: InvalidWarlord => e.asJson(using Encoder.AsObject.derived[InvalidWarlord])
    case e: WarlordNotInArmy => e.asJson(using Encoder.AsObject.derived[WarlordNotInArmy])
    case e: DuplicateExceeded => e.asJson(using Encoder.AsObject.derived[DuplicateExceeded])
    case e: DuplicateEpicHero => e.asJson(using Encoder.AsObject.derived[DuplicateEpicHero])
    case e: InvalidLeaderAttachment => e.asJson(using Encoder.AsObject.derived[InvalidLeaderAttachment])
    case e: TooManyEnhancements => e.asJson(using Encoder.AsObject.derived[TooManyEnhancements])
    case e: DuplicateEnhancement => e.asJson(using Encoder.AsObject.derived[DuplicateEnhancement])
    case e: EnhancementOnNonCharacter => e.asJson(using Encoder.AsObject.derived[EnhancementOnNonCharacter])
    case e: MultipleEnhancementsOnUnit => e.asJson(using Encoder.AsObject.derived[MultipleEnhancementsOnUnit])
    case e: EnhancementDetachmentMismatch => e.asJson(using Encoder.AsObject.derived[EnhancementDetachmentMismatch])
    case e: UnitCostNotFound => e.asJson(using Encoder.AsObject.derived[UnitCostNotFound])
    case e: DatasheetNotFound => e.asJson(using Encoder.AsObject.derived[DatasheetNotFound])
    case e: Generic => e.asJson(using Encoder.AsObject.derived[Generic])
  }
}

case class PersistedArmy(
  id: String,
  name: String,
  army: Army,
  ownerId: Option[String],
  createdAt: String,
  updatedAt: String
)

case class RegisterRequest(username: String, password: String, inviteCode: Option[String])
case class LoginRequest(username: String, password: String)
case class AuthResponse(token: String, user: UserResponse)
case class UserResponse(id: String, username: String)
case class InviteResponse(code: String, createdAt: String, used: Boolean)

case class ArmyRow(
  id: String,
  name: String,
  factionId: FactionId,
  battleSize: BattleSize,
  detachmentId: DetachmentId,
  warlordId: DatasheetId,
  ownerId: Option[UserId],
  createdAt: String,
  updatedAt: String
)

case class ArmyUnitRow(
  id: Int,
  armyId: String,
  datasheetId: DatasheetId,
  sizeOptionLine: Int,
  enhancementId: Option[EnhancementId],
  attachedLeaderId: Option[DatasheetId],
  wargearSelections: Option[String]
)

case class ArmySummary(id: String, name: String, factionId: String, battleSize: String, updatedAt: String)

object HttpServer {

  private val corsConfig = CORS.policy.withAllowOriginAll

  given Encoder[UUID] = Encoder.encodeString.contramap(_.toString)
  given Decoder[UUID] = Decoder.decodeString.emap(s =>
    scala.util.Try(UUID.fromString(s)).toEither.left.map(_.getMessage)
  )

  given Encoder[WargearSelection] = Encoder.forProduct3("optionLine", "selected", "notes")(
    (w: WargearSelection) => (w.optionLine, w.selected, w.notes)
  )
  given Decoder[WargearSelection] = Decoder.forProduct3("optionLine", "selected", "notes")(
    WargearSelection.apply
  )
  
  given Encoder[ArmyUnit] = Encoder.forProduct5("datasheetId", "sizeOptionLine", "enhancementId", "attachedLeaderId", "wargearSelections")(
    (u: ArmyUnit) => (u.datasheetId, u.sizeOptionLine, u.enhancementId, u.attachedLeaderId, u.wargearSelections)
  )
  given Decoder[ArmyUnit] = Decoder.forProduct5("datasheetId", "sizeOptionLine", "enhancementId", "attachedLeaderId", "wargearSelections")(
    ArmyUnit.apply
  )
  
  given Encoder[Army] = Encoder.forProduct5("factionId", "battleSize", "detachmentId", "warlordId", "units")(
    (a: Army) => (a.factionId, a.battleSize, a.detachmentId, a.warlordId, a.units)
  )
  given Decoder[Army] = Decoder.forProduct5("factionId", "battleSize", "detachmentId", "warlordId", "units")(
    Army.apply
  )

  def createServer(port: Int, xa: Transactor[IO]): Resource[IO, org.http4s.server.Server] =
    EmberServerBuilder.default[IO]
      .withHost(ip"0.0.0.0")
      .withPort(Port.fromInt(port).get)
      .withHttpApp(corsConfig(routes(xa)).orNotFound)
      .build

  private def loadArmy(armyId: String, xa: Transactor[IO]): IO[Option[PersistedArmy]] =
    for {
      rowOpt <- sql"SELECT id, name, faction_id, battle_size, detachment_id, warlord_id, owner_id, created_at, updated_at FROM armies WHERE id = $armyId"
        .query[ArmyRow].option.transact(xa)
      result <- rowOpt match {
        case None => IO.pure(None)
        case Some(row) =>
          sql"SELECT id, army_id, datasheet_id, size_option_line, enhancement_id, attached_leader_id, wargear_selections FROM army_units WHERE army_id = $armyId"
            .query[ArmyUnitRow].to[List].transact(xa).map { unitRows =>
              val units = unitRows.map { u =>
                val wargearSelections = u.wargearSelections match {
                  case None => List.empty[WargearSelection]
                  case Some(jsonStr) =>
                    jawn.decode[List[WargearSelection]](jsonStr) match {
                      case Right(selections) => selections
                      case Left(_) => List.empty[WargearSelection]
                    }
                }
                ArmyUnit(u.datasheetId, u.sizeOptionLine, u.enhancementId, u.attachedLeaderId, wargearSelections)
              }
              val army = Army(row.factionId, row.battleSize, row.detachmentId, row.warlordId, units)
              Some(PersistedArmy(row.id, row.name, army, row.ownerId.map(UserId.value), row.createdAt, row.updatedAt))
            }
      }
    } yield result

  private def saveArmy(armyId: String, name: String, army: Army, ownerId: Option[UserId], now: String, xa: Transactor[IO]): IO[Unit] =
    (for {
      _ <- sql"""INSERT INTO armies (id, name, faction_id, battle_size, detachment_id, warlord_id, owner_id, created_at, updated_at)
                 VALUES ($armyId, $name, ${army.factionId}, ${army.battleSize}, ${army.detachmentId}, ${army.warlordId}, $ownerId, $now, $now)""".update.run
      _ <- army.units.traverse_ { unit =>
        val wargearJson = if (unit.wargearSelections.isEmpty) None else Some(unit.wargearSelections.asJson.noSpaces)
        sql"""INSERT INTO army_units (army_id, datasheet_id, size_option_line, enhancement_id, attached_leader_id, wargear_selections)
              VALUES ($armyId, ${unit.datasheetId}, ${unit.sizeOptionLine}, ${unit.enhancementId}, ${unit.attachedLeaderId}, $wargearJson)""".update.run
      }
    } yield ()).transact(xa)

  private def updateArmy(armyId: String, name: String, army: Army, now: String, xa: Transactor[IO]): IO[Unit] =
    (for {
      _ <- sql"""UPDATE armies SET name = $name, faction_id = ${army.factionId}, battle_size = ${army.battleSize},
                 detachment_id = ${army.detachmentId}, warlord_id = ${army.warlordId}, updated_at = $now
                 WHERE id = $armyId""".update.run
      _ <- sql"DELETE FROM army_units WHERE army_id = $armyId".update.run
      _ <- army.units.traverse_ { unit =>
        val wargearJson = if (unit.wargearSelections.isEmpty) None else Some(unit.wargearSelections.asJson.noSpaces)
        sql"""INSERT INTO army_units (army_id, datasheet_id, size_option_line, enhancement_id, attached_leader_id, wargear_selections)
              VALUES ($armyId, ${unit.datasheetId}, ${unit.sizeOptionLine}, ${unit.enhancementId}, ${unit.attachedLeaderId}, $wargearJson)""".update.run
      }
    } yield ()).transact(xa)

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

  private val bearerChallenge = `WWW-Authenticate`(Challenge("Bearer", "api"))

  private def unauthorized(message: String): IO[Response[IO]] =
    Unauthorized(bearerChallenge, Json.obj("error" -> Json.fromString(message)))

  def routes(xa: Transactor[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "health" =>
      Ok(Json.obj("status" -> Json.fromString("ok")))

    case req @ POST -> Root / "api" / "auth" / "register" =>
      req.as[RegisterRequest].flatMap { regReq =>
        for {
          userCount <- UserRepository.count(xa)
          existingUser <- UserRepository.findByUsername(regReq.username)(xa)
          result <- existingUser match {
            case Some(_) =>
              Conflict(Json.obj("error" -> Json.fromString("Username already taken")))
            case None =>
              val needsInvite = userCount > 0
              if (needsInvite && regReq.inviteCode.isEmpty) {
                Forbidden(Json.obj("error" -> Json.fromString("Invite code required")))
              } else if (needsInvite) {
                val code = InviteCode(regReq.inviteCode.get)
                InviteRepository.findUnusedByCode(code)(xa).flatMap {
                  case None =>
                    Forbidden(Json.obj("error" -> Json.fromString("Invalid or used invite code")))
                  case Some(_) =>
                    for {
                      hash <- PasswordHasher.hash(regReq.password)
                      user <- UserRepository.create(regReq.username, hash)(xa)
                      _ <- InviteRepository.markUsed(code, user.id)(xa)
                      session <- SessionRepository.create(user.id)(xa)
                      resp <- Created(AuthResponse(
                        SessionToken.value(session.token),
                        UserResponse(UserId.value(user.id), user.username)
                      ))
                    } yield resp
                }
              } else {
                for {
                  hash <- PasswordHasher.hash(regReq.password)
                  user <- UserRepository.create(regReq.username, hash)(xa)
                  session <- SessionRepository.create(user.id)(xa)
                  resp <- Created(AuthResponse(
                    SessionToken.value(session.token),
                    UserResponse(UserId.value(user.id), user.username)
                  ))
                } yield resp
              }
          }
        } yield result
      }

    case req @ POST -> Root / "api" / "auth" / "login" =>
      req.as[LoginRequest].flatMap { loginReq =>
        UserRepository.findByUsername(loginReq.username)(xa).flatMap {
          case None =>
            unauthorized("Invalid credentials")
          case Some(user) =>
            PasswordHasher.verify(loginReq.password, user.passwordHash).flatMap {
              case false =>
                unauthorized("Invalid credentials")
              case true =>
                SessionRepository.create(user.id)(xa).flatMap { session =>
                  Ok(AuthResponse(
                    SessionToken.value(session.token),
                    UserResponse(UserId.value(user.id), user.username)
                  ))
                }
            }
        }
      }

    case req @ POST -> Root / "api" / "auth" / "logout" =>
      AuthMiddleware.extractUser(req, xa).flatMap {
        case None => Ok(Json.obj("message" -> Json.fromString("Logged out")))
        case Some(_) =>
          req.headers.get[headers.Authorization].flatMap { auth =>
            auth.credentials match {
              case Credentials.Token(AuthScheme.Bearer, token) => Some(token)
              case _ => None
            }
          }.orElse(req.cookies.find(_.name == "session").map(_.content)) match {
            case Some(tokenStr) =>
              SessionRepository.delete(SessionToken(tokenStr))(xa) *> Ok(Json.obj("message" -> Json.fromString("Logged out")))
            case None =>
              Ok(Json.obj("message" -> Json.fromString("Logged out")))
          }
      }

    case req @ GET -> Root / "api" / "auth" / "me" =>
      AuthMiddleware.extractUser(req, xa).flatMap {
        case None => unauthorized("Not authenticated")
        case Some(user) => Ok(UserResponse(UserId.value(user.id), user.username))
      }

    case req @ POST -> Root / "api" / "invites" =>
      AuthMiddleware.extractUser(req, xa).flatMap {
        case None => unauthorized("Authentication required")
        case Some(user) =>
          InviteRepository.create(Some(user.id))(xa).flatMap { invite =>
            Created(InviteResponse(InviteCode.value(invite.code), invite.createdAt.toString, false))
          }
      }

    case req @ GET -> Root / "api" / "invites" =>
      AuthMiddleware.extractUser(req, xa).flatMap {
        case None => unauthorized("Authentication required")
        case Some(_) =>
          InviteRepository.listAll(xa).flatMap { invites =>
            Ok(invites.map(i => InviteResponse(InviteCode.value(i.code), i.createdAt.toString, i.usedBy.isDefined)))
          }
      }

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
    case GET -> Root / "api" / "armies" =>
      sql"SELECT id, name, faction_id, battle_size, updated_at FROM armies ORDER BY updated_at DESC"
        .query[ArmySummary].to[List].transact(xa).flatMap(Ok(_))
    case GET -> Root / "api" / "factions" / factionIdStr / "armies" =>
      FactionId.parse(factionIdStr) match {
        case Right(factionId) =>
          sql"SELECT id, name, faction_id, battle_size, updated_at FROM armies WHERE faction_id = $factionId ORDER BY updated_at DESC"
            .query[ArmySummary].to[List].transact(xa).flatMap(Ok(_))
        case Left(_) =>
          BadRequest(Json.obj("error" -> Json.fromString(s"Invalid faction ID: $factionIdStr")))
      }
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
    case req @ POST -> Root / "api" / "armies" / "validate" =>
      req.as[Army].flatMap { army =>
        validateArmy(army, xa).flatMap(Ok(_))
      }
    case req @ POST -> Root / "api" / "armies" =>
      AuthMiddleware.extractUser(req, xa).flatMap {
        case None => unauthorized("Authentication required")
        case Some(user) =>
          req.as[CreateArmyRequest].flatMap { createReq =>
            val armyId = UUID.randomUUID().toString
            val now = Instant.now().toString
            saveArmy(armyId, createReq.name, createReq.army, Some(user.id), now, xa).flatMap { _ =>
              loadArmy(armyId, xa).flatMap {
                case Some(persisted) => Created(persisted)
                case None => InternalServerError(Json.obj("error" -> Json.fromString("Failed to load created army")))
              }
            }
          }
      }
    case GET -> Root / "api" / "armies" / armyId =>
      loadArmy(armyId, xa).flatMap {
        case Some(army) => Ok(army)
        case None => NotFound(Json.obj("error" -> Json.fromString(s"Army not found: $armyId")))
      }
    case req @ PUT -> Root / "api" / "armies" / armyId =>
      AuthMiddleware.extractUser(req, xa).flatMap { userOpt =>
        loadArmy(armyId, xa).flatMap {
          case None => NotFound(Json.obj("error" -> Json.fromString(s"Army not found: $armyId")))
          case Some(existingArmy) =>
            if (!isOwner(existingArmy, userOpt)) {
              Forbidden(Json.obj("error" -> Json.fromString("Not authorized to edit this army")))
            } else {
              req.as[CreateArmyRequest].flatMap { updateReq =>
                val now = Instant.now().toString
                updateArmy(armyId, updateReq.name, updateReq.army, now, xa).flatMap { _ =>
                  loadArmy(armyId, xa).flatMap {
                    case Some(persisted) => Ok(persisted)
                    case None => InternalServerError(Json.obj("error" -> Json.fromString("Failed to load updated army")))
                  }
                }
              }
            }
        }
      }
    case req @ DELETE -> Root / "api" / "armies" / armyId =>
      AuthMiddleware.extractUser(req, xa).flatMap { userOpt =>
        loadArmy(armyId, xa).flatMap {
          case None => NotFound(Json.obj("error" -> Json.fromString(s"Army not found: $armyId")))
          case Some(existingArmy) =>
            if (!isOwner(existingArmy, userOpt)) {
              Forbidden(Json.obj("error" -> Json.fromString("Not authorized to delete this army")))
            } else {
              sql"DELETE FROM armies WHERE id = $armyId".update.run.transact(xa).flatMap { _ =>
                NoContent()
              }
            }
        }
      }
    case _ =>
      NotFound("Not Found")
  }
}
