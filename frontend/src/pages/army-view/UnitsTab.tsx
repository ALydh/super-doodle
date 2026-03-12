import type { ArmyBattleData, BattleUnitData } from "../../types";
import { BattleUnitCard } from "../../components/battle/BattleUnitCard";
import styles from "../ArmyViewPage.module.css";

interface RoleGroup {
  role: string;
  units: BattleUnitData[];
}

interface Props {
  filteredRoleGroups: RoleGroup[];
  battleData: ArmyBattleData;
  searchQuery: string;
  onSearchChange: (query: string) => void;
  expandedIds: Set<string>;
  onToggleExpanded: (id: string) => void;
}

export function UnitsTab({ filteredRoleGroups, battleData, searchQuery, onSearchChange, expandedIds, onToggleExpanded }: Props) {
  return (
    <div>
      <div className={styles.search}>
        <input
          type="text"
          placeholder="Search units..."
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
        />
      </div>
      <div className={styles.grid}>
        {filteredRoleGroups.map((rg) => (
          <div key={rg.role}>
            <div className={styles.roleHeader}>{rg.role}</div>
            {rg.units.map((unit, index) => {
              const leadingName = unit.unit.attachedLeaderId
                ? battleData.units.find(u => u.unit.datasheetId === unit.unit.attachedLeaderId)?.datasheet.name
                : undefined;
              const unitIndex = battleData.units.indexOf(unit);
              const attachedLeader = battleData.units
                .find(u => u.unit.attachedToUnitIndex === unitIndex)?.datasheet.name;
              const cardId = `${unit.unit.datasheetId}_${unitIndex}`;
              return (
                <BattleUnitCard
                  key={`${unit.unit.datasheetId}-${index}`}
                  data={unit}
                  isWarlord={battleData.warlordId === unit.unit.datasheetId}
                  isExpanded={expandedIds.has(cardId)}
                  onToggle={() => onToggleExpanded(cardId)}
                  leadingUnit={leadingName}
                  attachedLeader={attachedLeader}
                />
              );
            })}
          </div>
        ))}
      </div>
    </div>
  );
}
