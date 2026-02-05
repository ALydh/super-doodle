import type { ArmyUnit, Datasheet, DatasheetLeader } from "../types";
import { LeaderSlot } from "./LeaderSlot";
import styles from "../pages/UnitRow.module.css";

interface Props {
  unitDatasheetId: string;
  allUnits: ArmyUnit[];
  datasheets: Datasheet[];
  leaders: DatasheetLeader[];
  onUpdate: (leaderIndex: number, updated: ArmyUnit) => void;
}

function canBeAdditionalLeader(datasheet: Datasheet): boolean {
  return datasheet.leaderFooter?.toLowerCase().includes("even if") ?? false;
}

export function LeaderSlotsSection({ unitDatasheetId, allUnits, datasheets, leaders, onUpdate }: Props) {
  const leaderMappings = leaders.filter((l) => l.attachedId === unitDatasheetId);
  const validLeaderDatasheetIds = leaderMappings.map((l) => l.leaderId);

  const characterUnits = allUnits
    .map((u, i) => ({ unit: u, index: i, datasheet: datasheets.find((d) => d.id === u.datasheetId) }))
    .filter(({ datasheet }) => datasheet?.role === "Characters" && validLeaderDatasheetIds.includes(datasheet.id))
    .filter((item): item is { unit: ArmyUnit; index: number; datasheet: Datasheet } => item.datasheet !== undefined);

  const attachedLeaders = characterUnits.filter(({ unit }) => unit.attachedLeaderId === unitDatasheetId);
  const availableLeaders = characterUnits.filter(({ unit }) => !unit.attachedLeaderId);
  const availableAdditionalLeaders = availableLeaders.filter(({ datasheet }) => canBeAdditionalLeader(datasheet));

  if (characterUnits.length === 0) {
    return null;
  }

  const hasPrimaryLeader = attachedLeaders.some(({ datasheet }) => !canBeAdditionalLeader(datasheet));
  const hasAvailablePrimary = availableLeaders.some(({ datasheet }) => !canBeAdditionalLeader(datasheet));
  const hasAvailableAdditional = availableAdditionalLeaders.length > 0;

  let slotCount = attachedLeaders.length;
  if (slotCount === 0 && availableLeaders.length > 0) {
    slotCount = 1;
  } else if (slotCount > 0 && availableLeaders.length > 0) {
    if (hasPrimaryLeader && hasAvailableAdditional) {
      slotCount += 1;
    } else if (!hasPrimaryLeader && hasAvailablePrimary) {
      slotCount += 1;
    } else if (hasAvailableAdditional) {
      slotCount += 1;
    }
  }

  const handleAttach = (leaderIndex: number) => {
    const leaderUnit = allUnits[leaderIndex];
    onUpdate(leaderIndex, { ...leaderUnit, attachedLeaderId: unitDatasheetId });
  };

  const handleDetach = (leaderIndex: number) => {
    const leaderUnit = allUnits[leaderIndex];
    onUpdate(leaderIndex, { ...leaderUnit, attachedLeaderId: null });
  };

  const slots: { leader: typeof attachedLeaders[number] | null; availableForSlot: typeof availableLeaders }[] = [];
  for (let i = 0; i < slotCount; i++) {
    const leader = attachedLeaders[i] ?? null;
    const isAdditionalSlot = i > 0 || (attachedLeaders.length > 0 && !leader);
    const availableForSlot = isAdditionalSlot && hasPrimaryLeader
      ? availableAdditionalLeaders
      : availableLeaders;
    slots.push({ leader, availableForSlot });
  }

  return (
    <div className={styles.leaderSlotsSection}>
      <label>Leaders</label>
      <div className={styles.leaderSlots}>
        {slots.map(({ leader, availableForSlot }, i) => (
          <LeaderSlot
            key={leader?.index ?? `empty-${i}`}
            attachedLeader={leader}
            availableLeaders={availableForSlot}
            onAttach={handleAttach}
            onDetach={handleDetach}
          />
        ))}
      </div>
    </div>
  );
}
