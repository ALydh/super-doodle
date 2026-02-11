package wahapedia.engine.command

import wahapedia.engine.state.*
import wahapedia.engine.combat.*
import wahapedia.engine.event.*
import wahapedia.engine.phase.*

object CommandExecutor:

  def execute(
    state: GameState,
    command: Command,
    dice: DiceRoller,
    weaponResolver: WeaponResolver = DefaultWeaponResolver
  ): (GameState, List[GameEvent]) =
    command match
      case cmd: MoveUnit =>
        MovementPhase.executeMove(state, cmd)
      case cmd: AdvanceUnit =>
        MovementPhase.validateAdvance(state, cmd, dice) match
          case Right(roll) => MovementPhase.executeAdvance(state, cmd, roll)
          case Left(_) => (state, Nil)
      case cmd: FallBack =>
        MovementPhase.executeFallBack(state, cmd)
      case cmd: DeepStrike =>
        MovementPhase.executeDeepStrike(state, cmd)
      case cmd: SelectShootingUnit =>
        ShootingPhase.executeSelectUnit(state, cmd)
      case cmd: SelectTarget =>
        val weapons = weaponResolver.resolveWeapons(state, cmd)
        ShootingPhase.executeAttack(state, cmd, weapons, dice)
      case cmd: DeclareCharge =>
        ChargePhase.executeDeclareCharge(state, cmd)
      case cmd: ChargeMove =>
        ChargePhase.executeChargeMove(state, cmd)
      case cmd: PileIn =>
        FightPhase.executePileIn(state, cmd)
      case cmd: FightTarget =>
        val weapons = weaponResolver.resolveMeleeWeapons(state, cmd)
        FightPhase.executeFight(state, cmd, weapons, dice)
      case cmd: Consolidate =>
        FightPhase.executeConsolidate(state, cmd)
      case cmd: EndPhase =>
        PhaseRunner.advancePhase(state)
      case cmd: UseStratagem =>
        (state, Nil)
      case cmd: AllocateWound =>
        (state, Nil)

trait WeaponResolver:
  def resolveWeapons(state: GameState, cmd: SelectTarget): List[WeaponProfile]
  def resolveMeleeWeapons(state: GameState, cmd: FightTarget): List[WeaponProfile]

object DefaultWeaponResolver extends WeaponResolver:
  def resolveWeapons(state: GameState, cmd: SelectTarget): List[WeaponProfile] =
    List(WeaponProfile("Default Ranged", "1", Some(4), 4, 0, "1"))

  def resolveMeleeWeapons(state: GameState, cmd: FightTarget): List[WeaponProfile] =
    List(WeaponProfile("Close Combat", "1", Some(4), 4, 0, "1", isMelee = true))
