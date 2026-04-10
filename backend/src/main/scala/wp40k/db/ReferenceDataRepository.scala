package wp40k.db

import doobie.*
import doobie.implicits.*
import cats.effect.{IO, Ref}
import cats.implicits.*
import cats.data.NonEmptyList
import wp40k.domain.models.*
import wp40k.domain.types.*
import wp40k.domain.army.ReferenceData
import DoobieMeta.given
import java.time.Instant
import scala.concurrent.duration.*

private case class CachedReferenceData(data: ReferenceData, cachedAt: Instant)

object ReferenceDataRepository {

  private val cacheTtl: FiniteDuration = 5.minutes
  private val cache: Ref[IO, Option[CachedReferenceData]] = Ref.unsafe(None)

  private def queryAll[A: Read](base: Fragment)(xa: Transactor[IO]): IO[List[A]] =
    base.query[A].to[List].transact(xa)

  private def queryByDatasheet[A: Read](base: Fragment)(id: DatasheetId)(xa: Transactor[IO]): IO[List[A]] =
    (base ++ fr"WHERE datasheet_id = $id").query[A].to[List].transact(xa)

  private def queryByDatasheets[A: Read](base: Fragment)(ids: NonEmptyList[DatasheetId])(xa: Transactor[IO]): IO[List[A]] =
    (base ++ fr"WHERE" ++ Fragments.in(fr"datasheet_id", ids)).query[A].to[List].transact(xa)

  private val datasheetBase = fr"""SELECT id, name, faction_id, source_id, legend, role, loadout, transport,
    virtual, leader_head, leader_footer, damaged_w, damaged_description, link FROM datasheets"""

  private val modelProfileBase = fr"""SELECT datasheet_id, line, name, movement, toughness, save,
    invulnerable_save, invulnerable_save_description, wounds, leadership,
    objective_control, base_size, base_size_description FROM model_profiles"""

  private val wargearBase = fr"""SELECT datasheet_id, line, line_in_wargear, dice, name, description,
    range, weapon_type, attacks, ballistic_skill, strength, armor_penetration, damage FROM wargear"""

  private val unitCostBase = fr"SELECT datasheet_id, line, description, cost FROM unit_cost"

  private val keywordBase = fr"SELECT datasheet_id, keyword, model, is_faction_keyword FROM datasheet_keywords"

  private val datasheetAbilityBase = fr"""SELECT da.datasheet_id, da.line, da.ability_id, da.model,
    COALESCE(NULLIF(da.name, ''), a.name) as name,
    COALESCE(NULLIF(da.description, ''), a.description) as description,
    da.ability_type, da.parameter
    FROM datasheet_abilities da
    LEFT JOIN abilities a ON da.ability_id = a.id"""

  private val datasheetOptionBase = fr"SELECT datasheet_id, line, button, description FROM datasheet_options"

  private val stratagemBase = fr"""SELECT faction_id, name, id, stratagem_type, cp_cost, legend, turn, phase,
    detachment, detachment_id, description FROM stratagems"""

  private val enhancementBase = fr"SELECT faction_id, id, name, cost, detachment, detachment_id, legend, description FROM enhancements"

  private val detachmentAbilityBase = fr"SELECT id, faction_id, name, legend, description, detachment, detachment_id FROM detachment_abilities"

  private val parsedWargearOptionBase = fr"""SELECT datasheet_id, option_line, choice_index, group_id, action,
    weapon_name, model_target, count_per_n_models, max_count FROM parsed_wargear_options"""

  def loadReferenceDataCached(xa: Transactor[IO]): IO[ReferenceData] =
    for {
      now <- IO(Instant.now())
      cached <- cache.get
      result <- cached match {
        case Some(c) if now.toEpochMilli - c.cachedAt.toEpochMilli < cacheTtl.toMillis =>
          IO.pure(c.data)
        case _ =>
          loadReferenceData(xa).flatTap { data =>
            cache.set(Some(CachedReferenceData(data, now)))
          }
      }
    } yield result

  def invalidateCache: IO[Unit] =
    cache.set(None)

  def allFactions(xa: Transactor[IO]): IO[List[Faction]] =
    sql"SELECT id, name, link, faction_group FROM factions"
      .query[Faction].to[List].transact(xa)

  def allSources(xa: Transactor[IO]): IO[List[Source]] =
    sql"SELECT id, name, source_type, edition, version, errata_date, errata_link FROM sources"
      .query[Source].to[List].transact(xa)

  def allAbilities(xa: Transactor[IO]): IO[List[Ability]] =
    sql"SELECT id, name, legend, faction_id, description FROM abilities"
      .query[Ability].to[List].transact(xa)

  def allDatasheets(xa: Transactor[IO]): IO[List[Datasheet]] =
    queryAll[Datasheet](datasheetBase)(xa)

  def datasheetById(datasheetId: DatasheetId)(xa: Transactor[IO]): IO[Option[Datasheet]] =
    (datasheetBase ++ fr"WHERE id = $datasheetId").query[Datasheet].option.transact(xa)

  def datasheetsForIds(datasheetIds: NonEmptyList[DatasheetId])(xa: Transactor[IO]): IO[List[Datasheet]] =
    (datasheetBase ++ fr"WHERE" ++ Fragments.in(fr"id", datasheetIds)).query[Datasheet].to[List].transact(xa)

  def datasheetsByFaction(factionId: FactionId)(xa: Transactor[IO]): IO[List[Datasheet]] =
    sql"""SELECT d.id, d.name, d.faction_id, d.source_id, d.legend, d.role, d.loadout, d.transport,
           d.virtual, d.leader_head, d.leader_footer, d.damaged_w, d.damaged_description, d.link
           FROM datasheets d
           WHERE d.faction_id = $factionId
           AND d.id = (
             SELECT d2.id
             FROM datasheets d2
             LEFT JOIN sources s2 ON d2.source_id = s2.id
             WHERE d2.faction_id = $factionId AND d2.name = d.name
             ORDER BY COALESCE(s2.edition, 0) DESC, d2.id ASC
             LIMIT 1
           )"""
      .query[Datasheet].to[List].transact(xa)

  def allModelProfiles(xa: Transactor[IO]): IO[List[ModelProfile]] =
    queryAll[ModelProfile](modelProfileBase)(xa)

  def modelProfilesForDatasheet(datasheetId: DatasheetId)(xa: Transactor[IO]): IO[List[ModelProfile]] =
    queryByDatasheet[ModelProfile](modelProfileBase)(datasheetId)(xa)

  def modelProfilesForDatasheets(datasheetIds: NonEmptyList[DatasheetId])(xa: Transactor[IO]): IO[List[ModelProfile]] =
    queryByDatasheets[ModelProfile](modelProfileBase)(datasheetIds)(xa)

  def allWargear(xa: Transactor[IO]): IO[List[Wargear]] =
    queryAll[Wargear](wargearBase)(xa)

  def wargearForDatasheet(datasheetId: DatasheetId)(xa: Transactor[IO]): IO[List[Wargear]] =
    queryByDatasheet[Wargear](wargearBase)(datasheetId)(xa)

  def wargearForDatasheets(datasheetIds: NonEmptyList[DatasheetId])(xa: Transactor[IO]): IO[List[Wargear]] =
    queryByDatasheets[Wargear](wargearBase)(datasheetIds)(xa)

  def allUnitCompositions(xa: Transactor[IO]): IO[List[UnitComposition]] =
    sql"SELECT datasheet_id, line, description FROM unit_composition"
      .query[UnitComposition].to[List].transact(xa)

  def allUnitCosts(xa: Transactor[IO]): IO[List[UnitCost]] =
    queryAll[UnitCost](unitCostBase)(xa)

  def unitCostsForDatasheet(datasheetId: DatasheetId)(xa: Transactor[IO]): IO[List[UnitCost]] =
    queryByDatasheet[UnitCost](unitCostBase)(datasheetId)(xa)

  def unitCostsForDatasheets(datasheetIds: NonEmptyList[DatasheetId])(xa: Transactor[IO]): IO[List[UnitCost]] =
    queryByDatasheets[UnitCost](unitCostBase)(datasheetIds)(xa)

  def allKeywords(xa: Transactor[IO]): IO[List[DatasheetKeyword]] =
    queryAll[DatasheetKeyword](keywordBase)(xa)

  def keywordsForDatasheet(datasheetId: DatasheetId)(xa: Transactor[IO]): IO[List[DatasheetKeyword]] =
    queryByDatasheet[DatasheetKeyword](keywordBase)(datasheetId)(xa)

  def keywordsForDatasheets(datasheetIds: NonEmptyList[DatasheetId])(xa: Transactor[IO]): IO[List[DatasheetKeyword]] =
    queryByDatasheets[DatasheetKeyword](keywordBase)(datasheetIds)(xa)

  def factionKeywordsForFaction(factionId: FactionId)(xa: Transactor[IO]): IO[List[DatasheetKeyword]] =
    sql"""SELECT DISTINCT dk.datasheet_id, dk.keyword, dk.model, dk.is_faction_keyword
          FROM datasheet_keywords dk
          JOIN datasheets d ON dk.datasheet_id = d.id
          WHERE d.faction_id = $factionId"""
      .query[DatasheetKeyword].to[List].transact(xa)

  def allDatasheetAbilities(xa: Transactor[IO]): IO[List[DatasheetAbility]] =
    queryAll[DatasheetAbility](datasheetAbilityBase)(xa)

  def abilitiesForDatasheet(datasheetId: DatasheetId)(xa: Transactor[IO]): IO[List[DatasheetAbility]] =
    (datasheetAbilityBase ++ fr"WHERE da.datasheet_id = $datasheetId").query[DatasheetAbility].to[List].transact(xa)

  def abilitiesForDatasheets(datasheetIds: NonEmptyList[DatasheetId])(xa: Transactor[IO]): IO[List[DatasheetAbility]] =
    (datasheetAbilityBase ++ fr"WHERE" ++ Fragments.in(fr"da.datasheet_id", datasheetIds)).query[DatasheetAbility].to[List].transact(xa)

  def allDatasheetOptions(xa: Transactor[IO]): IO[List[DatasheetOption]] =
    queryAll[DatasheetOption](datasheetOptionBase)(xa)

  def optionsForDatasheet(datasheetId: DatasheetId)(xa: Transactor[IO]): IO[List[DatasheetOption]] =
    queryByDatasheet[DatasheetOption](datasheetOptionBase)(datasheetId)(xa)

  def optionsForDatasheets(datasheetIds: NonEmptyList[DatasheetId])(xa: Transactor[IO]): IO[List[DatasheetOption]] =
    queryByDatasheets[DatasheetOption](datasheetOptionBase)(datasheetIds)(xa)

  def allDatasheetLeaders(xa: Transactor[IO]): IO[List[DatasheetLeader]] =
    sql"SELECT leader_id, attached_id FROM datasheet_leaders"
      .query[DatasheetLeader].to[List].transact(xa)

  def allStratagems(xa: Transactor[IO]): IO[List[Stratagem]] =
    queryAll[Stratagem](stratagemBase)(xa)

  def stratagemsByFaction(factionId: FactionId)(xa: Transactor[IO]): IO[List[Stratagem]] =
    (stratagemBase ++ fr"WHERE faction_id = $factionId OR faction_id IS NULL").query[Stratagem].to[List].transact(xa)

  def allDatasheetStratagems(xa: Transactor[IO]): IO[List[DatasheetStratagem]] =
    sql"SELECT datasheet_id, stratagem_id FROM datasheet_stratagems"
      .query[DatasheetStratagem].to[List].transact(xa)

  def stratagemsByDatasheet(datasheetId: DatasheetId)(xa: Transactor[IO]): IO[List[Stratagem]] =
    sql"""SELECT s.faction_id, s.name, s.id, s.stratagem_type, s.cp_cost, s.legend, s.turn, s.phase,
           s.detachment, s.detachment_id, s.description
           FROM stratagems s
           JOIN datasheet_stratagems ds ON ds.stratagem_id = s.id
           WHERE ds.datasheet_id = $datasheetId"""
      .query[Stratagem].to[List].transact(xa)

  def stratagemsByDatasheets(datasheetIds: NonEmptyList[DatasheetId])(xa: Transactor[IO]): IO[List[(DatasheetId, Stratagem)]] =
    (fr"""SELECT ds.datasheet_id, s.faction_id, s.name, s.id, s.stratagem_type, s.cp_cost, s.legend, s.turn, s.phase,
           s.detachment, s.detachment_id, s.description
           FROM stratagems s
           JOIN datasheet_stratagems ds ON ds.stratagem_id = s.id
           WHERE """ ++ Fragments.in(fr"ds.datasheet_id", datasheetIds))
      .query[(DatasheetId, Stratagem)].to[List].transact(xa)

  def allEnhancements(xa: Transactor[IO]): IO[List[Enhancement]] =
    queryAll[Enhancement](enhancementBase)(xa)

  def enhancementsByFaction(factionId: FactionId)(xa: Transactor[IO]): IO[List[Enhancement]] =
    (enhancementBase ++ fr"WHERE faction_id = $factionId").query[Enhancement].to[List].transact(xa)

  def allDatasheetEnhancements(xa: Transactor[IO]): IO[List[DatasheetEnhancement]] =
    sql"SELECT datasheet_id, enhancement_id FROM datasheet_enhancements"
      .query[DatasheetEnhancement].to[List].transact(xa)

  def enhancementEligibleDatasheets(factionId: FactionId)(xa: Transactor[IO]): IO[Map[EnhancementId, List[String]]] =
    sql"""SELECT de.enhancement_id, d.name
          FROM datasheet_enhancements de
          JOIN datasheets d ON de.datasheet_id = d.id
          JOIN enhancements e ON de.enhancement_id = e.id
          WHERE e.faction_id = $factionId"""
      .query[(String, String)].to[List].transact(xa)
      .map(_.groupBy(_._1).map((k, v) => EnhancementId(k) -> v.map(_._2)))

  def allDetachmentAbilities(xa: Transactor[IO]): IO[List[DetachmentAbility]] =
    queryAll[DetachmentAbility](detachmentAbilityBase)(xa)

  def detachmentAbilitiesByDetachmentId(detachmentId: String)(xa: Transactor[IO]): IO[List[DetachmentAbility]] =
    (detachmentAbilityBase ++ fr"WHERE detachment_id = $detachmentId").query[DetachmentAbility].to[List].transact(xa)

  def detachmentAbilitiesByFaction(factionId: FactionId)(xa: Transactor[IO]): IO[List[DetachmentAbility]] =
    (detachmentAbilityBase ++ fr"WHERE faction_id = $factionId").query[DetachmentAbility].to[List].transact(xa)

  def allDatasheetDetachmentAbilities(xa: Transactor[IO]): IO[List[DatasheetDetachmentAbility]] =
    sql"SELECT datasheet_id, detachment_ability_id FROM datasheet_detachment_abilities"
      .query[DatasheetDetachmentAbility].to[List].transact(xa)

  def lastUpdate(xa: Transactor[IO]): IO[List[LastUpdate]] =
    sql"SELECT timestamp FROM last_update"
      .query[LastUpdate].to[List].transact(xa)

  def allWeaponAbilities(xa: Transactor[IO]): IO[List[WeaponAbility]] =
    sql"SELECT id, name, description FROM weapon_abilities"
      .query[WeaponAbility].to[List].transact(xa)

  def parsedWargearOptionsForDatasheet(datasheetId: DatasheetId)(xa: Transactor[IO]): IO[List[ParsedWargearOption]] =
    queryByDatasheet[ParsedWargearOption](parsedWargearOptionBase)(datasheetId)(xa)

  def parsedWargearOptionsForDatasheets(datasheetIds: NonEmptyList[DatasheetId])(xa: Transactor[IO]): IO[List[ParsedWargearOption]] =
    queryByDatasheets[ParsedWargearOption](parsedWargearOptionBase)(datasheetIds)(xa)

  def parsedLoadoutsForDatasheet(datasheetId: DatasheetId)(xa: Transactor[IO]): IO[List[ModelLoadout]] =
    sql"SELECT model_pattern, weapon FROM parsed_loadouts WHERE datasheet_id = $datasheetId"
      .query[(String, String)].to[List].transact(xa)
      .map(rows => rows.groupBy(_._1).map { case (pattern, weapons) =>
        ModelLoadout(pattern, weapons.map(_._2))
      }.toList)

  def parsedLoadoutsForDatasheets(datasheetIds: NonEmptyList[DatasheetId])(xa: Transactor[IO]): IO[Map[String, List[ModelLoadout]]] =
    (fr"SELECT datasheet_id, model_pattern, weapon FROM parsed_loadouts WHERE " ++ Fragments.in(fr"datasheet_id", datasheetIds))
      .query[(String, String, String)].to[List].transact(xa)
      .map { rows =>
        rows.groupBy(_._1).map { case (dsId, dsRows) =>
          dsId -> dsRows.groupBy(_._2).map { case (pattern, patternRows) =>
            ModelLoadout(pattern, patternRows.map(_._3))
          }.toList
        }
      }

  case class WargearDefault(weapon: String, count: Int, modelType: Option[String])

  def wargearDefaultsForDatasheet(datasheetId: DatasheetId, sizeLine: Int)(xa: Transactor[IO]): IO[List[WargearDefault]] =
    sql"SELECT weapon, count, model_type FROM unit_wargear_defaults WHERE datasheet_id = $datasheetId AND size_line = $sizeLine"
      .query[WargearDefault].to[List].transact(xa)

  def wargearDefaultsForDatasheets(datasheetIds: NonEmptyList[DatasheetId])(xa: Transactor[IO]): IO[Map[(String, Int), List[WargearDefault]]] =
    (fr"SELECT datasheet_id, size_line, weapon, count, model_type FROM unit_wargear_defaults WHERE " ++ Fragments.in(fr"datasheet_id", datasheetIds))
      .query[(String, Int, String, Int, Option[String])].to[List].transact(xa)
      .map { rows =>
        rows.groupBy(r => (r._1, r._2)).map { case (key, groupRows) =>
          key -> groupRows.map(r => WargearDefault(r._3, r._4, r._5))
        }
      }

  case class DetachmentInfo(name: String, detachmentId: String)

  def detachmentsByFaction(factionId: FactionId)(xa: Transactor[IO]): IO[List[DetachmentInfo]] =
    sql"SELECT DISTINCT detachment, detachment_id FROM detachment_abilities WHERE faction_id = $factionId"
      .query[DetachmentInfo].to[List].transact(xa)

  def leadersByFaction(factionId: FactionId)(xa: Transactor[IO]): IO[List[DatasheetLeader]] =
    sql"""SELECT dl.leader_id, dl.attached_id
          FROM datasheet_leaders dl
          JOIN datasheets d ON dl.leader_id = d.id
          WHERE d.faction_id = $factionId"""
      .query[DatasheetLeader].to[List].transact(xa)

  def loadReferenceData(xa: Transactor[IO]): IO[ReferenceData] =
    for {
      datasheets <- allDatasheets(xa)
      keywords <- allKeywords(xa)
      unitCosts <- allUnitCosts(xa)
      enhancements <- allEnhancements(xa)
      leaders <- allDatasheetLeaders(xa)
      detachmentAbilities <- allDetachmentAbilities(xa)
    } yield ReferenceData(datasheets, keywords, unitCosts, enhancements, leaders, detachmentAbilities)

  def parsedCompositionForDatasheet(datasheetId: DatasheetId)(xa: Transactor[IO]): IO[List[ParsedCompositionLine]] =
    sql"SELECT model_name, min_count, max_count, group_index FROM parsed_unit_composition WHERE datasheet_id = $datasheetId"
      .query[(String, Int, Int, Int)].to[List].transact(xa)
      .map(_.map { case (name, min, max, groupIdx) => ParsedCompositionLine(name, min, max, groupIdx) })

  def parsedCompositionForDatasheets(datasheetIds: NonEmptyList[DatasheetId])(xa: Transactor[IO]): IO[Map[String, List[ParsedCompositionLine]]] =
    (fr"SELECT datasheet_id, model_name, min_count, max_count, group_index FROM parsed_unit_composition WHERE " ++ Fragments.in(fr"datasheet_id", datasheetIds))
      .query[(String, String, Int, Int, Int)].to[List].transact(xa)
      .map { rows =>
        rows.groupBy(_._1).map { case (dsId, dsRows) =>
          dsId -> dsRows.map { case (_, name, min, max, groupIdx) => ParsedCompositionLine(name, min, max, groupIdx) }
        }
      }

  def counts(xa: Transactor[IO]): IO[Map[String, Int]] = {
    val tables = List(
      "factions", "sources", "abilities", "datasheets", "model_profiles",
      "wargear", "unit_composition", "unit_cost", "datasheet_keywords",
      "datasheet_abilities", "datasheet_options", "datasheet_leaders",
      "stratagems", "datasheet_stratagems", "enhancements",
      "datasheet_enhancements", "detachment_abilities",
      "datasheet_detachment_abilities", "last_update", "weapon_abilities", "parsed_wargear_options",
      "parsed_loadouts", "parsed_unit_composition",
      "unit_wargear_defaults"
    )
    tables.traverse { table =>
      Fragment.const(s"SELECT COUNT(*) FROM $table").query[Int].unique.transact(xa).map(table -> _)
    }.map(_.toMap)
  }
}
