package wahapedia.engine.spatial

case class Vec3(x: Double, y: Double, z: Double):
  def +(other: Vec3): Vec3 = Vec3(x + other.x, y + other.y, z + other.z)
  def -(other: Vec3): Vec3 = Vec3(x - other.x, y - other.y, z - other.z)
  def *(s: Double): Vec3 = Vec3(x * s, y * s, z * s)
  def dot(other: Vec3): Double = x * other.x + y * other.y + z * other.z
  def lengthSquared: Double = x * x + y * y + z * z
  def length: Double = math.sqrt(lengthSquared)
  def normalized: Vec3 = if length == 0 then this else this * (1.0 / length)
  def horizontalDistanceTo(other: Vec3): Double =
    math.sqrt((x - other.x) * (x - other.x) + (y - other.y) * (y - other.y))
  def distanceTo(other: Vec3): Double = (this - other).length

object Vec3:
  val Zero: Vec3 = Vec3(0, 0, 0)
