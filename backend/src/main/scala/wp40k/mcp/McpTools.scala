package wp40k.mcp

import cats.effect.IO
import cats.implicits.*
import ch.linkyard.mcp.server.{CallContext, ToolFunction}
import com.melvinlow.json.schema.JsonSchemaEncoder
import com.melvinlow.json.schema.generic.auto.given
import io.circe.Json
import io.circe.generic.auto.given
import io.circe.syntax.*
import doobie.Transactor
import cats.data.NonEmptyList
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import wp40k.db.{ArmyRepository, InventoryRepository, ReferenceDataRepository}
import wp40k.domain.models.*
import wp40k.domain.types.*
import wp40k.domain.army.*
import wp40k.http.AuthMiddleware
import java.util.UUID

object McpTools:

  private given Logger[IO] = Slf4jLogger.getLogger[IO]

  private def logErrors[I, O](toolName: String)(f: (I, CallContext[IO]) => IO[O])(using log: Logger[IO]): (I, CallContext[IO]) => IO[O] =
    (in, ctx) =>
      log.info(s"MCP tool: $toolName") *>
        f(in, ctx).onError(e => log.error(e)(s"MCP tool failed [$toolName]: ${e.getMessage}"))


  given optionSchema[A](using inner: JsonSchemaEncoder[A]): JsonSchemaEncoder[Option[A]] =
    new JsonSchemaEncoder[Option[A]]:
      def schema: Json = inner.schema

  def allTools(refXa: Transactor[IO], userXa: Transactor[IO], refPrefix: String): List[ToolFunction[IO]] = List(
    listFactions(refXa),
    getFactionDatasheets(refXa),
    getDatasheetDetail(refXa),
    getFactionStratagems(refXa),
    getFactionEnhancements(refXa),
    getFactionDetachments(refXa),
    searchDatasheets(refXa),
    getCoreAbilities(refXa),
    getWeaponAbilities(refXa),
    getInventory(userXa),
    listArmies(userXa, refPrefix),
    getArmy(userXa),
    createArmy(userXa),
    updateArmy(userXa),
    deleteArmy(userXa),
    validateArmy(refXa),
  )

  private case class EmptyInput()

  private def listFactions(xa: Transactor[IO]): ToolFunction[IO] = ToolFunction.text(
    ToolFunction.Info("list_factions", "List Factions".some, "List all available Warhammer 40K factions with their IDs and names".some, ToolFunction.Effect.ReadOnly, isOpenWorld = false),
    logErrors("list_factions") { (_: EmptyInput, _: CallContext[IO]) =>
      ReferenceDataRepository.allFactions(xa).map(_.map(FactionOut.from).asJson.noSpaces)
    },
  )

  private def enrichDatasheets(datasheets: List[Datasheet], factionId: FactionId, xa: Transactor[IO]): IO[List[DatasheetSummaryOut]] =
    NonEmptyList.fromList(datasheets.map(_.id)) match
      case None => IO.pure(Nil)
      case Some(ids) =>
        for
          costs    <- ReferenceDataRepository.unitCostsForDatasheets(ids)(xa)
          keywords <- ReferenceDataRepository.keywordsForDatasheets(ids)(xa)
          leaders  <- ReferenceDataRepository.leadersByFaction(factionId)(xa)
        yield
          val costMap    = costs.groupBy(_.datasheetId)
          val kwMap      = keywords.groupBy(_.datasheetId)
          val dsNameMap  = datasheets.map(d => d.id -> d.name).toMap
          val leadsMap   = leaders.groupBy(_.leaderId).map((k, v) => k -> v.flatMap(l => dsNameMap.get(l.attachedId)))
          val ledByMap   = leaders.groupBy(_.attachedId).map((k, v) => k -> v.flatMap(l => dsNameMap.get(l.leaderId)))
          datasheets.map { d =>
            DatasheetSummaryOut.enriched(
              d,
              costMap.getOrElse(d.id, Nil),
              kwMap.getOrElse(d.id, Nil),
              leadsMap.getOrElse(d.id, Nil),
              ledByMap.getOrElse(d.id, Nil)
            )
          }

  private def getFactionDatasheets(xa: Transactor[IO]): ToolFunction[IO] = ToolFunction.text(
    ToolFunction.Info("get_faction_datasheets", "Get Faction Datasheets".some, "Get all datasheets (unit entries) for a specific faction. Returns summary info for each unit including costs, keywords, leaders, and transport.".some, ToolFunction.Effect.ReadOnly, isOpenWorld = false),
    logErrors("get_faction_datasheets") { (in: FactionDatasheetsInput, _: CallContext[IO]) =>
      val fid = FactionId(in.factionId)
      for
        all      <- ReferenceDataRepository.datasheetsByFaction(fid)(xa)
        filtered  = all.filterNot(_.virtual)
        enriched <- enrichDatasheets(filtered, fid, xa)
      yield
        val result = enriched
          .filter(d => in.role.forall(r => d.role.exists(_.equalsIgnoreCase(r))))
          .filter(d => in.keyword.forall(k => d.keywords.exists(_.toLowerCase.contains(k.toLowerCase)) || d.factionKeywords.exists(_.toLowerCase.contains(k.toLowerCase))))
          .filter(d => in.search.forall(s => d.name.toLowerCase.contains(s.toLowerCase)))
        result.asJson.noSpaces
    },
  )

  private def getDatasheetDetail(xa: Transactor[IO]): ToolFunction[IO] = ToolFunction.structured(
    ToolFunction.Info("get_datasheet_detail", "Get Datasheet Detail".some, "Get full details for a specific datasheet including profiles, wargear, abilities, costs, and keywords.".some, ToolFunction.Effect.ReadOnly, isOpenWorld = false),
    logErrors("get_datasheet_detail") { (in: DatasheetInput, _: CallContext[IO]) =>
      val dsId = DatasheetId(in.datasheetId)
      for
        dsOpt <- ReferenceDataRepository.datasheetById(dsId)(xa)
        ds <- dsOpt.liftTo[IO](new RuntimeException(s"Datasheet not found: ${in.datasheetId}"))
        profiles <- ReferenceDataRepository.modelProfilesForDatasheet(dsId)(xa)
        wargear <- ReferenceDataRepository.wargearForDatasheet(dsId)(xa)
        abilities <- ReferenceDataRepository.abilitiesForDatasheet(dsId)(xa)
        costs <- ReferenceDataRepository.unitCostsForDatasheet(dsId)(xa)
        keywords <- ReferenceDataRepository.keywordsForDatasheet(dsId)(xa)
      yield DatasheetDetailOut(
        DatasheetId.value(ds.id), ds.name, ds.factionId.map(FactionId.value), ds.role.map(_.toString), stripHtmlOpt(ds.legend),
        stripHtmlOpt(ds.loadout), stripHtmlOpt(ds.transport),
        profiles.map(ModelProfileOut.from), wargear.map(WargearOut.from),
        abilities.map(AbilityOut.from), costs.map(UnitCostOut.from), keywords.map(KeywordOut.from)
      )
    },
  )

  private def getFactionStratagems(xa: Transactor[IO]): ToolFunction[IO] = ToolFunction.text(
    ToolFunction.Info("get_faction_stratagems", "Get Faction Stratagems".some, "Get all stratagems available to a faction, including core stratagems.".some, ToolFunction.Effect.ReadOnly, isOpenWorld = false),
    logErrors("get_faction_stratagems") { (in: StratagemInput, _: CallContext[IO]) =>
      ReferenceDataRepository.stratagemsByFaction(FactionId(in.factionId))(xa).map { strats =>
        val filtered = strats
          .filter(s => in.detachmentId.forall(d => s.detachmentId.contains(d) || s.detachmentId.isEmpty))
          .filter(s => !in.excludeCore.getOrElse(false) || s.factionId.isDefined)
        filtered.map(StratagemOut.from).asJson.noSpaces
      }
    },
  )

  private def getFactionEnhancements(xa: Transactor[IO]): ToolFunction[IO] = ToolFunction.text(
    ToolFunction.Info("get_faction_enhancements", "Get Faction Enhancements".some, "Get all enhancements available to a faction.".some, ToolFunction.Effect.ReadOnly, isOpenWorld = false),
    logErrors("get_faction_enhancements") { (in: FactionInput, _: CallContext[IO]) =>
      val fid = FactionId(in.factionId)
      for
        enhancements <- ReferenceDataRepository.enhancementsByFaction(fid)(xa)
        eligible     <- ReferenceDataRepository.enhancementEligibleDatasheets(fid)(xa)
      yield enhancements.map(e => EnhancementOut.from(e, eligible.getOrElse(e.id, Nil))).asJson.noSpaces
    },
  )

  private def getFactionDetachments(xa: Transactor[IO]): ToolFunction[IO] = ToolFunction.text(
    ToolFunction.Info("get_faction_detachments", "Get Faction Detachments".some, "Get all detachments available to a faction, including full rule text for each detachment ability.".some, ToolFunction.Effect.ReadOnly, isOpenWorld = false),
    logErrors("get_faction_detachments") { (in: FactionInput, _: CallContext[IO]) =>
      ReferenceDataRepository.detachmentAbilitiesByFaction(FactionId(in.factionId))(xa)
        .map(abs => DetachmentDetailOut.fromAbilities(abs).asJson.noSpaces)
    },
  )

  private def searchDatasheets(xa: Transactor[IO]): ToolFunction[IO] = ToolFunction.text(
    ToolFunction.Info("search_datasheets", "Search Datasheets".some, "Search datasheets by name. Optionally filter by faction ID. When faction is provided, returns enriched results with costs, keywords, and leaders.".some, ToolFunction.Effect.ReadOnly, isOpenWorld = false),
    logErrors("search_datasheets") { (in: SearchInput, _: CallContext[IO]) =>
      val query = in.query.toLowerCase
      in.factionId match
        case Some(fid) =>
          val factionId = FactionId(fid)
          for
            all      <- ReferenceDataRepository.datasheetsByFaction(factionId)(xa)
            matched   = all.filterNot(_.virtual).filter(_.name.toLowerCase.contains(query))
            enriched <- enrichDatasheets(matched, factionId, xa)
          yield enriched.asJson.noSpaces
        case None =>
          ReferenceDataRepository.allDatasheets(xa).map { all =>
            all.filterNot(_.virtual)
              .filter(_.name.toLowerCase.contains(query))
              .map(DatasheetSummaryOut.from)
              .asJson.noSpaces
          }
    },
  )

  private def getCoreAbilities(xa: Transactor[IO]): ToolFunction[IO] = ToolFunction.text(
    ToolFunction.Info("get_core_abilities", "Get Core Abilities".some, "Get all core abilities (e.g., Deadly Demise, Feel No Pain).".some, ToolFunction.Effect.ReadOnly, isOpenWorld = false),
    logErrors("get_core_abilities") { (_: EmptyInput, _: CallContext[IO]) =>
      ReferenceDataRepository.allAbilities(xa).map(_.map(CoreAbilityOut.from).asJson.noSpaces)
    },
  )

  private def getWeaponAbilities(xa: Transactor[IO]): ToolFunction[IO] = ToolFunction.text(
    ToolFunction.Info("get_weapon_abilities", "Get Weapon Abilities".some, "Get all weapon abilities (e.g., Rapid Fire, Heavy, Blast).".some, ToolFunction.Effect.ReadOnly, isOpenWorld = false),
    logErrors("get_weapon_abilities") { (_: EmptyInput, _: CallContext[IO]) =>
      ReferenceDataRepository.allWeaponAbilities(xa).map(_.map(WeaponAbilityOut.from).asJson.noSpaces)
    },
  )

  private def requireAuth(token: String, xa: Transactor[IO]): IO[wp40k.domain.auth.AuthenticatedUser] =
    AuthMiddleware.extractUserByToken(token, xa).flatMap {
      case Some(user) => IO.pure(user)
      case None => IO.raiseError(new RuntimeException("Authentication failed: invalid or expired token"))
    }

  private def getInventory(xa: Transactor[IO]): ToolFunction[IO] = ToolFunction.text(
    ToolFunction.Info("get_inventory", "Get Inventory".some, "Get all inventory entries for the authenticated user. Returns a list of datasheetId and quantity pairs.".some, ToolFunction.Effect.ReadOnly, isOpenWorld = false),
    logErrors("get_inventory") { (in: ListInventoryInput, _: CallContext[IO]) =>
      for
        user <- requireAuth(in.token, xa)
        entries <- InventoryRepository.getByUser(user.id)(xa)
      yield entries.map(InventoryEntryOut.from).asJson.noSpaces
    },
  )

  private def listArmies(xa: Transactor[IO], refPrefix: String): ToolFunction[IO] = ToolFunction.text(
    ToolFunction.Info("list_armies", "List Armies".some, "List all saved army lists. Requires authentication token.".some, ToolFunction.Effect.ReadOnly, isOpenWorld = false),
    logErrors("list_armies") { (in: ListArmiesInput, _: CallContext[IO]) =>
      requireAuth(in.token, xa) >>
        ArmyRepository.listSummaries(xa, refPrefix).map(_.map(ArmySummaryOut.from).asJson.noSpaces)
    },
  )

  private def getArmy(xa: Transactor[IO]): ToolFunction[IO] = ToolFunction.structured(
    ToolFunction.Info("get_army", "Get Army".some, "Get full details of a saved army list by ID. Requires authentication token.".some, ToolFunction.Effect.ReadOnly, isOpenWorld = false),
    logErrors("get_army") { (in: ArmyIdInput, _: CallContext[IO]) =>
      requireAuth(in.token, xa) >>
        ArmyRepository.findById(in.armyId)(xa).flatMap {
          case Some(army) => IO.pure(ArmyOut.from(army))
          case None => IO.raiseError(new RuntimeException(s"Army not found: ${in.armyId}"))
        }
    },
  )

  private def createArmy(xa: Transactor[IO]): ToolFunction[IO] = ToolFunction.structured(
    ToolFunction.Info("create_army", "Create Army".some, "Create a new army list. Requires authentication token. Creates an empty army with the given faction, battle size, and detachment.".some, ToolFunction.Effect.Additive(idempotent = false), isOpenWorld = false),
    logErrors("create_army") { (in: CreateArmyInput, _: CallContext[IO]) =>
      for
        user <- requireAuth(in.token, xa)
        battleSize <- IO.fromEither(BattleSize.parse(in.battleSize).leftMap(e => new RuntimeException(e)))
        army = Army(FactionId(in.factionId), battleSize, DetachmentId(in.detachmentId), DatasheetId(in.warlordId), List.empty, in.chapterId)
        id = UUID.randomUUID().toString
        persisted <- ArmyRepository.create(id, in.name, army, Some(user.id))(xa)
      yield ArmyOut.from(persisted)
    },
  )

  private def updateArmy(xa: Transactor[IO]): ToolFunction[IO] = ToolFunction.structured(
    ToolFunction.Info("update_army", "Update Army".some, "Update an existing army's metadata (name, faction, battle size, detachment, warlord). Requires authentication token.".some, ToolFunction.Effect.Additive(idempotent = true), isOpenWorld = false),
    logErrors("update_army") { (in: UpdateArmyInput, _: CallContext[IO]) =>
      for
        _ <- requireAuth(in.token, xa)
        existing <- ArmyRepository.findById(in.armyId)(xa).flatMap {
          case Some(a) => IO.pure(a)
          case None => IO.raiseError(new RuntimeException(s"Army not found: ${in.armyId}"))
        }
        battleSize <- IO.fromEither(BattleSize.parse(in.battleSize).leftMap(e => new RuntimeException(e)))
        army = existing.army.copy(
          factionId = FactionId(in.factionId), battleSize = battleSize,
          detachmentId = DetachmentId(in.detachmentId), warlordId = DatasheetId(in.warlordId), chapterId = in.chapterId)
        result <- ArmyRepository.update(in.armyId, in.name, army)(xa).flatMap {
          case Some(p) => IO.pure(ArmyOut.from(p))
          case None => IO.raiseError(new RuntimeException("Update failed"))
        }
      yield result
    },
  )

  private def deleteArmy(xa: Transactor[IO]): ToolFunction[IO] = ToolFunction.text(
    ToolFunction.Info("delete_army", "Delete Army".some, "Delete a saved army list by ID. Requires authentication token.".some, ToolFunction.Effect.Destructive(idempotent = true), isOpenWorld = false),
    logErrors("delete_army") { (in: DeleteArmyInput, _: CallContext[IO]) =>
      requireAuth(in.token, xa) >>
        ArmyRepository.delete(in.armyId)(xa).map {
          case true => s"Army ${in.armyId} deleted successfully"
          case false => s"Army not found: ${in.armyId}"
        }
    },
  )

  private def validateArmy(refXa: Transactor[IO]): ToolFunction[IO] = ToolFunction.structured(
    ToolFunction.Info("validate_army", "Validate Army".some, "Validate an army configuration against the rules. Accepts optional units list with datasheet IDs, size options, enhancements, and leader attachments. Returns validation errors if any.".some, ToolFunction.Effect.ReadOnly, isOpenWorld = false),
    logErrors("validate_army") { (in: ValidateArmyInput, _: CallContext[IO]) =>
      for
        battleSize <- IO.fromEither(BattleSize.parse(in.battleSize).leftMap(e => new RuntimeException(e)))
        units = in.units.getOrElse(Nil).map(u => ArmyUnit(
          DatasheetId(u.datasheetId), u.sizeOptionLine.getOrElse(1),
          u.enhancementId.map(EnhancementId(_)),
          u.attachedLeaderId.map(DatasheetId(_)),
          isAllied = u.isAllied.getOrElse(false)
        ))
        warlordId = in.warlordId.map(DatasheetId(_))
          .orElse(units.headOption.map(_.datasheetId))
          .getOrElse(DatasheetId("000000000"))
        army = Army(FactionId(in.factionId), battleSize, DetachmentId(in.detachmentId), warlordId, units, in.chapterId)
        ref <- ReferenceDataRepository.loadReferenceData(refXa)
        errors = ArmyValidator.validate(army, ref)
      yield ValidationResultOut(errors.isEmpty, errors.map(_.toString))
    },
  )
