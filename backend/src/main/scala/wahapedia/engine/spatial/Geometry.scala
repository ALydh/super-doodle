package wahapedia.engine.spatial

object Geometry:

  def distanceBetweenBases(a: Vec3, b: Vec3, baseRadiusA: Double, baseRadiusB: Double): Double =
    val centerDist = a.horizontalDistanceTo(b)
    math.max(0.0, centerDist - baseRadiusA - baseRadiusB)

  def closestModelDistance(
    modelsA: Seq[Vec3],
    modelsB: Seq[Vec3],
    baseRadiusA: Double = 0.0,
    baseRadiusB: Double = 0.0
  ): Double =
    if modelsA.isEmpty || modelsB.isEmpty then Double.MaxValue
    else
      (for
        a <- modelsA
        b <- modelsB
      yield distanceBetweenBases(a, b, baseRadiusA, baseRadiusB)).min

  def isWithinRange(from: Vec3, to: Vec3, rangeInches: Double): Boolean =
    from.horizontalDistanceTo(to) <= rangeInches

  def withinCoherency(positions: Seq[Vec3], coherencyRange: Double = 2.0): Boolean =
    if positions.size <= 1 then true
    else positions.forall: pos =>
      positions.exists: other =>
        (other ne pos) && pos.horizontalDistanceTo(other) <= coherencyRange

  def withinCoherencyStrict(positions: Seq[Vec3]): Boolean =
    if positions.size <= 6 then withinCoherency(positions, 2.0)
    else positions.forall: pos =>
      val nearby = positions.count: other =>
        (other ne pos) && pos.horizontalDistanceTo(other) <= 2.0
      nearby >= 2

  def isWithinEngagementRange(a: Vec3, b: Vec3): Boolean =
    a.horizontalDistanceTo(b) <= 1.0 && math.abs(a.z - b.z) <= 5.0

  def anyModelInEngagementRange(modelsA: Seq[Vec3], modelsB: Seq[Vec3]): Boolean =
    modelsA.exists: a =>
      modelsB.exists: b =>
        isWithinEngagementRange(a, b)
