import type { ReactElement } from "react";
import type {
  ArmyUnit, Datasheet,
} from "../types";
import { UnitRow } from "./UnitRow";
import { sortByRoleOrder } from "../constants";
import styles from "./UnitRow.module.css";

interface RenderContext {
  units: ArmyUnit[];
  datasheets: Datasheet[];
  warlordId: string;
  onUpdate: (index: number, unit: ArmyUnit) => void;
  onRemove: (index: number) => void;
  onCopy: (index: number) => void;
  onSetWarlord: (index: number) => void;
  readOnly: boolean;
}

export function renderUnitsForMode(
  units: ArmyUnit[],
  datasheets: Datasheet[],
  warlordId: string,
  onUpdate: (index: number, unit: ArmyUnit) => void,
  onRemove: (index: number) => void,
  onCopy: (index: number) => void,
  onSetWarlord: (index: number) => void,
  readOnly = false
): ReactElement[] {
  const ctx: RenderContext = {
    units, datasheets,
    warlordId, onUpdate, onRemove, onCopy, onSetWarlord, readOnly,
  };

  const warlordIndex = ctx.units.findIndex(u => u.datasheetId === ctx.warlordId);

  const indexed = ctx.units.map((unit, index) => ({ unit, index }));

  const unitsByRole: Record<string, { unit: ArmyUnit; index: number }[]> = {};
  for (const entry of indexed) {
    const ds = ctx.datasheets.find(d => d.id === entry.unit.datasheetId);
    const role = ds?.role ?? "Other";
    if (!unitsByRole[role]) unitsByRole[role] = [];
    unitsByRole[role].push(entry);
  }

  const rendered: ReactElement[] = [];
  const sortedRoles = sortByRoleOrder(Object.keys(unitsByRole));

  for (const role of sortedRoles) {
    const roleUnits = unitsByRole[role];

    rendered.push(
      <tr key={`role-header-${role}`} className={styles.roleHeaderRow}>
        <td colSpan={8}>
          <div className={styles.roleHeader}>{role}</div>
        </td>
      </tr>
    );

    const sorted = [...roleUnits].sort((a, b) => {
      const aIsWarlord = a.index === warlordIndex;
      const bIsWarlord = b.index === warlordIndex;
      if (aIsWarlord && !bIsWarlord) return -1;
      if (!aIsWarlord && bIsWarlord) return 1;
      return 0;
    });

    for (const { unit, index } of sorted) {
      const datasheet = ctx.datasheets.find(ds => ds.id === unit.datasheetId);
      const isWarlord = index === warlordIndex;

      rendered.push(
        <UnitRow
          key={index}
          unit={unit}
          index={index}
          datasheet={datasheet}
          isWarlord={isWarlord}
          onUpdate={ctx.onUpdate}
          onRemove={ctx.onRemove}
          onCopy={ctx.onCopy}
          onSetWarlord={ctx.onSetWarlord}
          allUnits={ctx.units}
          readOnly={ctx.readOnly}
        />
      );
    }
  }

  return rendered;
}
