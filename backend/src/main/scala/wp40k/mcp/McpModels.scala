package wp40k.mcp

import org.jsoup.Jsoup
import wp40k.domain.types.*
import wp40k.domain.models.*
import wp40k.db.{PersistedArmy, ArmySummary}

def stripHtml(s: String): String = Jsoup.parse(s).text()
def stripHtmlOpt(s: Option[String]): Option[String] = s.map(stripHtml)

case class FactionInput(factionId: String)
case class StratagemInput(factionId: String, detachmentId: Option[String] = None, excludeCore: Option[Boolean] = None)
case class FactionDatasheetsInput(factionId: String, role: Option[String] = None, keyword: Option[String] = None, search: Option[String] = None)
case class DatasheetInput(datasheetId: String)
case class SearchInput(query: String, factionId: Option[String] = None)
case class ArmyIdInput(token: String, armyId: String)
case class ListArmiesInput(token: String)
case class CreateArmyInput(token: String, name: String, factionId: String, battleSize: String, detachmentId: String, warlordId: String, chapterId: Option[String] = None)
case class UpdateArmyInput(token: String, armyId: String, name: String, factionId: String, battleSize: String, detachmentId: String, warlordId: String, chapterId: Option[String] = None)
case class DeleteArmyInput(token: String, armyId: String)
case class ArmyUnitInput(datasheetId: String, sizeOptionLine: Option[Int] = None, enhancementId: Option[String] = None, attachedLeaderId: Option[String] = None, isAllied: Option[Boolean] = None)
case class ValidateArmyInput(factionId: String, battleSize: String, detachmentId: String, warlordId: Option[String] = None, chapterId: Option[String] = None, units: Option[List[ArmyUnitInput]] = None)

case class FactionOut(id: String, name: String, group: Option[String])
object FactionOut:
  def from(f: Faction): FactionOut = FactionOut(FactionId.value(f.id), f.name, f.group)

case class DatasheetSummaryOut(
  id: String, name: String, factionId: Option[String], role: Option[String], legend: Option[String],
  costs: List[UnitCostOut],
  keywords: List[String],
  factionKeywords: List[String],
  transport: Option[String],
  leadsUnits: List[String],
  ledBy: List[String]
)
object DatasheetSummaryOut:
  def from(d: Datasheet): DatasheetSummaryOut =
    DatasheetSummaryOut(DatasheetId.value(d.id), d.name, d.factionId.map(FactionId.value), d.role.map(_.toString), stripHtmlOpt(d.legend),
      Nil, Nil, Nil, stripHtmlOpt(d.transport), Nil, Nil)

  def enriched(
    d: Datasheet,
    costs: List[UnitCost],
    kws: List[DatasheetKeyword],
    leadsUnits: List[String],
    ledBy: List[String]
  ): DatasheetSummaryOut =
    DatasheetSummaryOut(
      DatasheetId.value(d.id), d.name, d.factionId.map(FactionId.value), d.role.map(_.toString), stripHtmlOpt(d.legend),
      costs.map(UnitCostOut.from),
      kws.filterNot(_.isFactionKeyword).flatMap(_.keyword).distinct,
      kws.filter(_.isFactionKeyword).flatMap(_.keyword).distinct,
      stripHtmlOpt(d.transport),
      leadsUnits,
      ledBy
    )

case class ModelProfileOut(name: Option[String], movement: String, toughness: String, save: String, invulnerableSave: Option[String], wounds: Int, leadership: String, objectiveControl: Int)
object ModelProfileOut:
  def from(p: ModelProfile): ModelProfileOut =
    ModelProfileOut(p.name, p.movement, p.toughness, p.save.toString, p.invulnerableSave, p.wounds, p.leadership, p.objectiveControl)

case class WargearOut(name: Option[String], weaponType: Option[String], range: Option[String], attacks: Option[String], ballisticSkill: Option[String], strength: Option[String], armorPenetration: Option[String], damage: Option[String], description: Option[String])
object WargearOut:
  def from(w: Wargear): WargearOut =
    WargearOut(w.name, w.weaponType, w.range, w.attacks, w.ballisticSkill, w.strength, w.armorPenetration, w.damage, stripHtmlOpt(w.description))

case class AbilityOut(name: Option[String], description: Option[String], abilityType: Option[String])
object AbilityOut:
  def from(a: DatasheetAbility): AbilityOut = AbilityOut(a.name, stripHtmlOpt(a.description), a.abilityType)

case class UnitCostOut(line: Int, description: String, cost: Int)
object UnitCostOut:
  def from(c: UnitCost): UnitCostOut = UnitCostOut(c.line, c.description, c.cost)

case class KeywordOut(keyword: Option[String], isFactionKeyword: Boolean)
object KeywordOut:
  def from(k: DatasheetKeyword): KeywordOut = KeywordOut(k.keyword, k.isFactionKeyword)

case class DatasheetDetailOut(
  id: String, name: String, factionId: Option[String], role: Option[String], legend: Option[String],
  loadout: Option[String], transport: Option[String],
  profiles: List[ModelProfileOut], wargear: List[WargearOut],
  abilities: List[AbilityOut], costs: List[UnitCostOut], keywords: List[KeywordOut]
)

case class StratagemOut(id: String, name: String, stratagemType: Option[String], cpCost: Option[Int], legend: Option[String], turn: Option[String], phase: Option[String], detachment: Option[String], description: String)
object StratagemOut:
  def from(s: Stratagem): StratagemOut =
    StratagemOut(StratagemId.value(s.id), s.name, s.stratagemType, s.cpCost, s.legend, s.turn, s.phase, s.detachment, stripHtml(s.description))

case class EnhancementOut(id: String, name: String, cost: Int, detachment: Option[String], legend: Option[String], description: String, eligibleDatasheets: List[String])
object EnhancementOut:
  def from(e: Enhancement, eligible: List[String] = Nil): EnhancementOut =
    EnhancementOut(EnhancementId.value(e.id), e.name, e.cost, e.detachment, e.legend, stripHtml(e.description), eligible)

case class DetachmentAbilityOut(name: String, legend: Option[String], description: String)
case class DetachmentDetailOut(name: String, detachmentId: String, abilities: List[DetachmentAbilityOut])
object DetachmentDetailOut:
  def fromAbilities(abilities: List[DetachmentAbility]): List[DetachmentDetailOut] =
    abilities.groupBy(a => (a.detachment, a.detachmentId)).toList.map { case ((name, detId), abs) =>
      DetachmentDetailOut(name, detId, abs.map(a => DetachmentAbilityOut(a.name, a.legend, stripHtml(a.description))))
    }

case class CoreAbilityOut(id: String, name: String, description: String)
object CoreAbilityOut:
  def from(a: Ability): CoreAbilityOut = CoreAbilityOut(AbilityId.value(a.id), a.name, stripHtml(a.description))

case class WeaponAbilityOut(id: String, name: String, description: String)
object WeaponAbilityOut:
  def from(wa: WeaponAbility): WeaponAbilityOut = WeaponAbilityOut(wa.id, wa.name, stripHtml(wa.description))

case class ArmySummaryOut(id: String, name: String, factionId: String, battleSize: String, updatedAt: String, warlordName: Option[String], totalPoints: Int)
object ArmySummaryOut:
  def from(s: ArmySummary): ArmySummaryOut =
    ArmySummaryOut(s.id, s.name, s.factionId, s.battleSize, s.updatedAt, s.warlordName, s.totalPoints)

case class ArmyOut(id: String, name: String, factionId: String, battleSize: String, detachmentId: String, warlordId: String, chapterId: Option[String], unitCount: Int, createdAt: String, updatedAt: String)
object ArmyOut:
  def from(p: PersistedArmy): ArmyOut =
    ArmyOut(p.id, p.name, FactionId.value(p.army.factionId), p.army.battleSize.toString,
      DetachmentId.value(p.army.detachmentId), DatasheetId.value(p.army.warlordId),
      p.army.chapterId, p.army.units.size, p.createdAt, p.updatedAt)

case class ValidationResultOut(valid: Boolean, errors: List[String])
