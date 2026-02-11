package wahapedia.engine.spatial

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LineOfSightSpec extends AnyFlatSpec with Matchers {

  val wall = TerrainPiece(
    id = TerrainId("wall1"),
    bounds = AABB(Vec3(5, -5, 0), Vec3(5.5, 5, 6)),
    traits = Set(TerrainTrait.BlocksLoS)
  )

  "hasLineOfSight" should "be true with no terrain" in {
    LineOfSight.hasLineOfSight(Vec3(0, 0, 1), Vec3(10, 0, 1), Nil) shouldBe true
  }

  it should "be blocked by LoS-blocking terrain" in {
    LineOfSight.hasLineOfSight(Vec3(0, 0, 1), Vec3(10, 0, 1), Seq(wall)) shouldBe false
  }

  it should "not be blocked if shooter is inside terrain" in {
    LineOfSight.hasLineOfSight(Vec3(5.2, 0, 1), Vec3(10, 0, 1), Seq(wall)) shouldBe true
  }

  it should "not be blocked if target is inside terrain" in {
    LineOfSight.hasLineOfSight(Vec3(0, 0, 1), Vec3(5.2, 0, 1), Seq(wall)) shouldBe true
  }

  it should "not be blocked by non-blocking terrain" in {
    val woods = wall.copy(traits = Set(TerrainTrait.GiveCover))
    LineOfSight.hasLineOfSight(Vec3(0, 0, 1), Vec3(10, 0, 1), Seq(woods)) shouldBe true
  }

  "canSeeAnyModel" should "find at least one visible model" in {
    val shooters = Seq(Vec3(0, 0, 1), Vec3(0, 3, 1))
    val targets = Seq(Vec3(10, 0, 1), Vec3(10, 3, 1))
    LineOfSight.canSeeAnyModel(shooters, targets, Seq(wall)) shouldBe false
  }

  it should "detect visibility around terrain" in {
    val shooters = Seq(Vec3(0, 6, 1))
    val targets = Seq(Vec3(10, 6, 1))
    LineOfSight.canSeeAnyModel(shooters, targets, Seq(wall)) shouldBe true
  }

  "isInCover" should "detect cover from terrain" in {
    val coverTerrain = TerrainPiece(
      id = TerrainId("ruins1"),
      bounds = AABB(Vec3(4, -2, 0), Vec3(5, 2, 4)),
      traits = Set(TerrainTrait.GiveCover, TerrainTrait.BlocksLoS)
    )
    LineOfSight.isInCover(Vec3(0, 0, 1), Vec3(10, 0, 1), Seq(coverTerrain)) shouldBe true
  }

  it should "not give cover when no terrain between" in {
    LineOfSight.isInCover(Vec3(0, 0, 1), Vec3(3, 0, 1), Seq(wall)) shouldBe false
  }

  "AABB.intersectsRay" should "detect ray intersection" in {
    val box = AABB(Vec3(2, -1, 0), Vec3(3, 1, 2))
    val origin = Vec3(0, 0, 1)
    val dir = Vec3(5, 0, 0)
    box.intersectsRay(origin, dir) shouldBe true
  }

  it should "miss when ray goes above" in {
    val box = AABB(Vec3(2, -1, 0), Vec3(3, 1, 2))
    val origin = Vec3(0, 0, 3)
    val dir = Vec3(5, 0, 0)
    box.intersectsRay(origin, dir) shouldBe false
  }
}
