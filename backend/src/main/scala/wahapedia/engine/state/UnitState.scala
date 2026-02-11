package wahapedia.engine.state

import wahapedia.domain.types.DatasheetId
import wahapedia.domain.army.WargearSelection
import wahapedia.engine.effect.ActiveEffect

opaque type UnitId = String
object UnitId:
  def apply(id: String): UnitId = id
  def value(id: UnitId): String = id

opaque type ModelId = String
object ModelId:
  def apply(id: String): ModelId = id
  def value(id: ModelId): String = id

case class ModelState(
  id: ModelId,
  profileLine: Int,
  position: wahapedia.engine.spatial.Vec3,
  woundsRemaining: Int,
  wargearSelections: List[WargearSelection],
  isLeader: Boolean
)

case class UnitState(
  id: UnitId,
  datasheetId: DatasheetId,
  owner: PlayerId,
  models: Vector[ModelState],
  attachedLeader: Option[UnitId] = None,
  hasMoved: Boolean = false,
  hasAdvanced: Boolean = false,
  hasShot: Boolean = false,
  hasCharged: Boolean = false,
  hasFought: Boolean = false,
  hasFallenBack: Boolean = false,
  battleShocked: Boolean = false,
  isDestroyed: Boolean = false,
  isInReserve: Boolean = false,
  activeEffects: List[ActiveEffect] = Nil
):
  def aliveModels: Vector[ModelState] = models.filter(_.woundsRemaining > 0)
  def isAlive: Boolean = aliveModels.nonEmpty && !isDestroyed
  def modelPositions: Seq[wahapedia.engine.spatial.Vec3] = aliveModels.map(_.position)
