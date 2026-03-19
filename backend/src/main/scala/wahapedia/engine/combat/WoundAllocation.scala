package wahapedia.engine.combat

import wahapedia.engine.state.{UnitState, ModelState, ModelId}
import wahapedia.engine.event.{GameEvent, WoundAllocated, ModelDestroyed, UnitDestroyed}

object WoundAllocation:

  case class AllocationResult(
    updatedUnit: UnitState,
    events: List[GameEvent]
  )

  def allocateDamage(
    unit: UnitState,
    totalDamage: Int,
    mortalWounds: Int
  ): AllocationResult =
    val combined = totalDamage + mortalWounds
    if combined <= 0 then return AllocationResult(unit, Nil)

    var currentModels = unit.models
    var events = List.empty[GameEvent]
    var remaining = combined

    while remaining > 0 && currentModels.exists(_.woundsRemaining > 0) do
      val targetModel = selectModel(currentModels)
      targetModel match
        case None => remaining = 0
        case Some(model) =>
          val damageToApply = remaining.min(model.woundsRemaining)
          val newWounds = model.woundsRemaining - damageToApply
          remaining -= damageToApply

          val idx = currentModels.indexOf(model)
          val updatedModel = model.copy(woundsRemaining = newWounds)
          currentModels = currentModels.updated(idx, updatedModel)

          events = events :+ WoundAllocated(unit.id, model.id, damageToApply, newWounds)

          if newWounds <= 0 then
            events = events :+ ModelDestroyed(unit.id, model.id)

    val updatedUnit = unit.copy(models = currentModels)
    val isDestroyed = currentModels.forall(_.woundsRemaining <= 0)

    if isDestroyed then
      events = events :+ UnitDestroyed(unit.id)
      AllocationResult(updatedUnit.copy(isDestroyed = true), events)
    else
      AllocationResult(updatedUnit, events)

  private def selectModel(models: Vector[ModelState]): Option[ModelState] =
    val alive = models.filter(_.woundsRemaining > 0)
    if alive.isEmpty then None
    else
      val wounded = alive.filter(m => m.woundsRemaining < maxWoundsForModel(m))
      if wounded.nonEmpty then Some(wounded.head)
      else
        val nonLeaders = alive.filterNot(_.isLeader)
        if nonLeaders.nonEmpty then Some(nonLeaders.head)
        else Some(alive.head)

  private def maxWoundsForModel(model: ModelState): Int =
    model.woundsRemaining
