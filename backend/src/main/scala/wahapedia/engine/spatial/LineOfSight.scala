package wahapedia.engine.spatial

object LineOfSight:

  def hasLineOfSight(from: Vec3, to: Vec3, terrain: Seq[TerrainPiece]): Boolean =
    val dir = to - from
    !terrain.exists: piece =>
      piece.traits.contains(TerrainTrait.BlocksLoS) &&
        piece.bounds.intersectsRay(from, dir) &&
        !piece.bounds.contains(from) &&
        !piece.bounds.contains(to)

  def canSeeAnyModel(
    shooterModels: Seq[Vec3],
    targetModels: Seq[Vec3],
    terrain: Seq[TerrainPiece]
  ): Boolean =
    shooterModels.exists: from =>
      targetModels.exists: to =>
        hasLineOfSight(from, to, terrain)

  def isInCover(
    shooterPos: Vec3,
    targetPos: Vec3,
    terrain: Seq[TerrainPiece]
  ): Boolean =
    val dir = targetPos - shooterPos
    terrain.exists: piece =>
      piece.traits.contains(TerrainTrait.GiveCover) &&
        piece.bounds.intersectsRay(shooterPos, dir)
