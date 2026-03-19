package wahapedia.engine.spatial

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GeometrySpec extends AnyFlatSpec with Matchers {

  "Vec3" should "compute horizontal distance" in {
    val a = Vec3(0, 0, 0)
    val b = Vec3(3, 4, 10)
    a.horizontalDistanceTo(b) shouldBe 5.0
  }

  it should "compute 3D distance" in {
    val a = Vec3(0, 0, 0)
    val b = Vec3(1, 0, 0)
    a.distanceTo(b) shouldBe 1.0
  }

  it should "add vectors" in {
    Vec3(1, 2, 3) + Vec3(4, 5, 6) shouldBe Vec3(5, 7, 9)
  }

  it should "subtract vectors" in {
    Vec3(4, 5, 6) - Vec3(1, 2, 3) shouldBe Vec3(3, 3, 3)
  }

  it should "scale vectors" in {
    Vec3(1, 2, 3) * 2 shouldBe Vec3(2, 4, 6)
  }

  "distanceBetweenBases" should "return 0 when overlapping" in {
    Geometry.distanceBetweenBases(Vec3(0, 0, 0), Vec3(0.5, 0, 0), 0.5, 0.5) shouldBe 0.0
  }

  it should "compute base-to-base distance" in {
    val dist = Geometry.distanceBetweenBases(Vec3(0, 0, 0), Vec3(3, 0, 0), 0.5, 0.5)
    dist shouldBe 2.0 +- 0.001
  }

  "closestModelDistance" should "find minimum distance between model groups" in {
    val a = Seq(Vec3(0, 0, 0), Vec3(2, 0, 0))
    val b = Seq(Vec3(5, 0, 0), Vec3(10, 0, 0))
    Geometry.closestModelDistance(a, b) shouldBe 3.0 +- 0.001
  }

  it should "return MaxValue for empty groups" in {
    Geometry.closestModelDistance(Nil, Seq(Vec3.Zero)) shouldBe Double.MaxValue
  }

  "isWithinRange" should "be true within range" in {
    Geometry.isWithinRange(Vec3(0, 0, 0), Vec3(12, 0, 0), 12) shouldBe true
  }

  it should "be false out of range" in {
    Geometry.isWithinRange(Vec3(0, 0, 0), Vec3(13, 0, 0), 12) shouldBe false
  }

  "withinCoherency" should "accept single model" in {
    Geometry.withinCoherency(Seq(Vec3(0, 0, 0))) shouldBe true
  }

  it should "accept two models within 2 inches" in {
    Geometry.withinCoherency(Seq(Vec3(0, 0, 0), Vec3(1.5, 0, 0))) shouldBe true
  }

  it should "reject two models more than 2 inches apart" in {
    Geometry.withinCoherency(Seq(Vec3(0, 0, 0), Vec3(3, 0, 0))) shouldBe false
  }

  it should "accept a line of models each within 2 inches" in {
    val positions = (0 to 4).map(i => Vec3(i * 1.5, 0, 0))
    Geometry.withinCoherency(positions) shouldBe true
  }

  "isWithinEngagementRange" should "accept 1 inch horizontal, 5 inch vertical" in {
    Geometry.isWithinEngagementRange(Vec3(0, 0, 0), Vec3(0.9, 0, 4.9)) shouldBe true
  }

  it should "reject more than 1 inch horizontal" in {
    Geometry.isWithinEngagementRange(Vec3(0, 0, 0), Vec3(1.1, 0, 0)) shouldBe false
  }

  it should "reject more than 5 inch vertical" in {
    Geometry.isWithinEngagementRange(Vec3(0, 0, 0), Vec3(0.5, 0, 5.5)) shouldBe false
  }

  "anyModelInEngagementRange" should "detect engagement" in {
    val a = Seq(Vec3(0, 0, 0), Vec3(2, 0, 0))
    val b = Seq(Vec3(0.8, 0, 0))
    Geometry.anyModelInEngagementRange(a, b) shouldBe true
  }

  it should "detect no engagement" in {
    val a = Seq(Vec3(0, 0, 0))
    val b = Seq(Vec3(5, 0, 0))
    Geometry.anyModelInEngagementRange(a, b) shouldBe false
  }
}
