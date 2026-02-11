package wahapedia.engine.state

import wahapedia.engine.spatial.{Vec3, TerrainPiece}

case class Objective(id: String, position: Vec3)

case class BoardState(
  units: Map[UnitId, UnitState],
  terrain: List[TerrainPiece],
  objectives: List[Objective] = Nil,
  boardWidth: Double = 60.0,
  boardHeight: Double = 44.0
):
  def unitsByPlayer(playerId: PlayerId): Iterable[UnitState] =
    units.values.filter(u => u.owner == playerId && u.isAlive)

  def aliveUnits: Iterable[UnitState] =
    units.values.filter(_.isAlive)

  def updateUnit(unit: UnitState): BoardState =
    copy(units = units.updated(unit.id, unit))

  def updateUnits(updated: Iterable[UnitState]): BoardState =
    copy(units = units ++ updated.map(u => u.id -> u))
