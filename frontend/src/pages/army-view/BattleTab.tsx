import type { ArmyBattleData, BattleUnitData } from "../../types";
import type { BattleViewMode } from "./useSessionStorage";
import { BattleUnitCard } from "../../components/battle/BattleUnitCard";
import styles from "../ArmyViewPage.module.css";

interface RoleGroup {
  role: string;
  units: BattleUnitData[];
}

interface Props {
  roleGroups: RoleGroup[];
  battleData: ArmyBattleData;
  searchQuery: string;
  onSearchChange: (query: string) => void;
  viewMode: BattleViewMode;
  onViewModeChange: (mode: BattleViewMode) => void;
  expandedIds: Set<string>;
  onToggleExpanded: (id: string) => void;
}

const MODES: { id: BattleViewMode; label: string }[] = [
  { id: "compact", label: "Compact" },
  { id: "expanded", label: "Expanded" },
  { id: "ranged", label: "Ranged" },
  { id: "melee", label: "Melee" },
];

export function BattleTab({ roleGroups, battleData, searchQuery, onSearchChange, viewMode, onViewModeChange, expandedIds, onToggleExpanded }: Props) {
  return (
    <div>
      <div className={styles.battleControls}>
        <div className={styles.battleModeToggle} role="tablist" aria-label="Battle view mode">
          {MODES.map((m) => (
            <button
              key={m.id}
              type="button"
              role="tab"
              aria-selected={viewMode === m.id}
              className={`${styles.battleModeBtn} ${viewMode === m.id ? styles.battleModeBtnActive : ""}`}
              onClick={() => onViewModeChange(m.id)}
            >
              {m.label}
            </button>
          ))}
        </div>
        <input
          className={styles.battleSearch}
          type="text"
          placeholder="Search units..."
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
        />
      </div>
      <div className={styles.grid}>
        {roleGroups.map((rg) => (
          <div key={rg.role}>
            <div className={styles.roleHeader}>{rg.role}</div>
            {rg.units.map((unit, index) => {
              const leadingName = unit.unit.attachedLeaderId
                ? battleData.units.find(u => u.unit.datasheetId === unit.unit.attachedLeaderId)?.datasheet.name
                : undefined;
              const unitIndex = battleData.units.indexOf(unit);
              const attachedLeader = battleData.units
                .find(u => u.unit.attachedToUnitIndex === unitIndex)?.datasheet.name;
              const cardId = `battle:${unit.unit.datasheetId}_${unitIndex}`;
              return (
                <BattleUnitCard
                  key={`${unit.unit.datasheetId}-${index}`}
                  data={unit}
                  isWarlord={battleData.warlordId === unit.unit.datasheetId}
                  isExpanded={expandedIds.has(cardId)}
                  onToggle={() => onToggleExpanded(cardId)}
                  leadingUnit={leadingName}
                  attachedLeader={attachedLeader}
                  viewMode={viewMode}
                />
              );
            })}
          </div>
        ))}
      </div>
    </div>
  );
}
