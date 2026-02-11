package wahapedia.engine.state

import wahapedia.domain.types.{FactionId, DetachmentId}

opaque type PlayerId = String
object PlayerId:
  def apply(id: String): PlayerId = id
  def value(id: PlayerId): String = id

case class PlayerState(
  id: PlayerId,
  factionId: FactionId,
  detachmentId: DetachmentId,
  commandPoints: Int = 0,
  victoryPoints: Int = 0,
  isPrimaryPlayer: Boolean = false
)
