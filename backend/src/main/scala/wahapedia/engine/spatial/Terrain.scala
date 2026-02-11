package wahapedia.engine.spatial

opaque type TerrainId = String

object TerrainId:
  def apply(id: String): TerrainId = id
  def value(id: TerrainId): String = id

enum TerrainTrait:
  case GiveCover
  case BlocksLoS
  case Scaleable
  case DenseArea
  case Breachable

case class AABB(min: Vec3, max: Vec3):
  def contains(p: Vec3): Boolean =
    p.x >= min.x && p.x <= max.x &&
    p.y >= min.y && p.y <= max.y &&
    p.z >= min.z && p.z <= max.z

  def intersectsRay(origin: Vec3, dir: Vec3): Boolean =
    var tmin = (min.x - origin.x) / dir.x
    var tmax = (max.x - origin.x) / dir.x
    if tmin > tmax then { val tmp = tmin; tmin = tmax; tmax = tmp }

    var tymin = (min.y - origin.y) / dir.y
    var tymax = (max.y - origin.y) / dir.y
    if tymin > tymax then { val tmp = tymin; tymin = tymax; tymax = tmp }

    if tmin > tymax || tymin > tmax then return false
    if tymin > tmin then tmin = tymin
    if tymax < tmax then tmax = tymax

    var tzmin = (min.z - origin.z) / dir.z
    var tzmax = (max.z - origin.z) / dir.z
    if tzmin > tzmax then { val tmp = tzmin; tzmin = tzmax; tzmax = tmp }

    !(tmin > tzmax || tzmin > tmax)

case class Surface(height: Double, bounds: AABB)

case class TerrainPiece(
  id: TerrainId,
  bounds: AABB,
  traits: Set[TerrainTrait],
  surfaces: List[Surface] = Nil
)
