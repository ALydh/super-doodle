package wp40k.http.routes

import cats.effect.{IO, Ref}
import cats.implicits.*
import io.circe.Json
import org.http4s.HttpRoutes
import org.http4s.client.Client
import sttp.model.StatusCode
import sttp.tapir.server.http4s.Http4sServerInterpreter
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import wp40k.{RevisionUpdater, RevisionDiff}
import wp40k.db.RevisionState
import wp40k.http.TapirSecurity
import wp40k.http.dto.*
import wp40k.http.endpoints.RevisionEndpoints
import doobie.*
import doobie.implicits.*

object RevisionRoutesTapir {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def routes(
    userXa: Transactor[IO],
    activeRef: Ref[IO, RevisionState],
    client: Client[IO],
    revisionsDir: String
  ): HttpRoutes[IO] = {

    val listRoute = Http4sServerInterpreter[IO]().toRoutes(
      RevisionEndpoints.listRevisions.serverLogic { _ =>
        RevisionUpdater.listAll(userXa).map { revisions =>
          Right(revisions.map(r => RevisionDto(r.id, r.wahapediaTimestamp, r.fetchedAt, r.isActive)))
        }
      }
    )

    val activeRoute = Http4sServerInterpreter[IO]().toRoutes(
      RevisionEndpoints.activeRevision.serverLogic { _ =>
        RevisionUpdater.listAll(userXa).map { revisions =>
          revisions.find(_.isActive) match {
            case Some(r) => Right(RevisionDto(r.id, r.wahapediaTimestamp, r.fetchedAt, r.isActive))
            case None => Left(Json.obj("error" -> Json.fromString("No active revision")))
          }
        }
      }
    )

    val diffRoute = Http4sServerInterpreter[IO]().toRoutes(
      RevisionEndpoints.diffRevisions.serverLogic { (oldId, newId) =>
        RevisionUpdater.listAll(userXa).flatMap { revisions =>
          val oldRev = revisions.find(_.id == oldId)
          val newRev = revisions.find(_.id == newId)
          (oldRev, newRev) match {
            case (Some(old), Some(nw)) =>
              RevisionDiff.compute(old.dbPath, nw.dbPath, oldId, newId).map { diff =>
                Right(RevisionDiffDto(
                  diff.oldRevisionId,
                  diff.newRevisionId,
                  diff.pointChanges.map(p => PointChangeDto(p.datasheetId, p.datasheetName, p.line, p.description, p.oldCost, p.newCost)),
                  diff.unitChanges.map(u => UnitChangeDto(u.datasheetId, u.name, u.factionId, u.changeType)),
                  diff.statChanges.map(s => StatChangeDto(s.datasheetId, s.datasheetName, s.field, s.oldValue, s.newValue)),
                  diff.enhancementChanges.map(e => EnhancementChangeDto(e.id, e.name, e.factionId, e.oldCost, e.newCost, e.changeType)),
                  diff.stratagemChanges.map(s => StratagemChangeDto(s.id, s.name, s.factionId, s.changeType, s.oldCpCost, s.newCpCost))
                ))
              }
            case _ =>
              IO.pure(Left(Json.obj("error" -> Json.fromString("One or both revisions not found"))))
          }
        }
      }
    )

    val activateRoute = Http4sServerInterpreter[IO]().toRoutes(
      RevisionEndpoints.activateRevision
        .serverSecurityLogic(TapirSecurity.required(userXa))
        .serverLogic { user => revisionId =>
          if (!user.isAdmin)
            IO.pure(Left((StatusCode.Forbidden, Json.obj("error" -> Json.fromString("Admin access required")))))
          else
            RevisionUpdater.listAll(userXa).flatMap { revisions =>
              revisions.find(_.id == revisionId) match {
                case None =>
                  IO.pure(Left((StatusCode.NotFound, Json.obj("error" -> Json.fromString("Revision not found")))))
                case Some(rev) =>
                  val activate =
                    sql"UPDATE revisions SET is_active = 0".update.run *>
                    sql"UPDATE revisions SET is_active = 1 WHERE id = $revisionId".update.run
                  activate.transact(userXa) *>
                    activeRef.set(RevisionState(rev.id, rev.dbPath)) *>
                    IO.pure(Right(Json.obj("status" -> Json.fromString("activated"), "revisionId" -> Json.fromString(revisionId))))
              }
            }
        }
    )

    val checkRoute = Http4sServerInterpreter[IO]().toRoutes(
      RevisionEndpoints.checkForUpdate
        .serverSecurityLogic(TapirSecurity.required(userXa))
        .serverLogic { user => _ =>
          if (!user.isAdmin)
            IO.pure(Left((StatusCode.Forbidden, Json.obj("error" -> Json.fromString("Admin access required")))))
          else
            RevisionUpdater.checkAndUpdate(client, revisionsDir, userXa, activeRef)
              .as(Right(Json.obj("status" -> Json.fromString("check completed"))))
              .handleErrorWith { e =>
                IO.pure(Right(Json.obj(
                  "status" -> Json.fromString("check failed"),
                  "error" -> Json.fromString(e.getMessage)
                )))
              }
        }
    )

    listRoute <+> activeRoute <+> diffRoute <+> activateRoute <+> checkRoute
  }
}
