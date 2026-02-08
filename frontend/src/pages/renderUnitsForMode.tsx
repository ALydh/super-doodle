import type { ReactElement } from "react";
import type {
  ArmyUnit, Datasheet,
} from "../types";
import { UnitRow } from "./UnitRow";
import { StackedUnitRow } from "./StackedUnitRow";
import { sortByRoleOrder } from "../constants";
import styles from "./UnitRow.module.css";

interface StackedUnit {
  unit: ArmyUnit;
  index: number;
}

function areUnitsIdentical(a: ArmyUnit, b: ArmyUnit): boolean {
  if (a.datasheetId !== b.datasheetId) return false;
  if (a.sizeOptionLine !== b.sizeOptionLine) return false;
  if (a.enhancementId !== b.enhancementId) return false;
  if (a.attachedLeaderId || b.attachedLeaderId) return false;
  if (a.attachedToUnitIndex != null || b.attachedToUnitIndex != null) return false;

  const aSelections = a.wargearSelections.filter(s => s.selected).sort((x, y) => x.optionLine - y.optionLine);
  const bSelections = b.wargearSelections.filter(s => s.selected).sort((x, y) => x.optionLine - y.optionLine);

  if (aSelections.length !== bSelections.length) return false;
  for (let i = 0; i < aSelections.length; i++) {
    if (aSelections[i].optionLine !== bSelections[i].optionLine) return false;
    if (aSelections[i].notes !== bSelections[i].notes) return false;
  }

  return true;
}

function groupIdenticalUnits(units: ArmyUnit[], warlordId: string): { stacks: StackedUnit[][]; singles: StackedUnit[] } {
  const stacks: StackedUnit[][] = [];
  const singles: StackedUnit[] = [];
  const processed = new Set<number>();

  const claimedBodyguardIndices = new Set<number>();
  units.forEach((u) => {
    if (u.attachedLeaderId) {
      const bodyguardIdx = units.findIndex(bg => bg.datasheetId === u.attachedLeaderId);
      if (bodyguardIdx >= 0) claimedBodyguardIndices.add(bodyguardIdx);
    }
  });

  for (let i = 0; i < units.length; i++) {
    if (processed.has(i)) continue;

    const unit = units[i];
    const isWarlord = warlordId === unit.datasheetId && units.findIndex(u => u.datasheetId === warlordId) === i;
    const isClaimedBodyguard = claimedBodyguardIndices.has(i);

    if (isWarlord || unit.attachedLeaderId || unit.attachedToUnitIndex != null || isClaimedBodyguard) {
      singles.push({ unit, index: i });
      processed.add(i);
      continue;
    }

    const stack: StackedUnit[] = [{ unit, index: i }];
    processed.add(i);

    for (let j = i + 1; j < units.length; j++) {
      if (processed.has(j)) continue;
      if (claimedBodyguardIndices.has(j)) continue;
      const otherUnit = units[j];

      if (areUnitsIdentical(unit, otherUnit)) {
        stack.push({ unit: otherUnit, index: j });
        processed.add(j);
      }
    }

    if (stack.length > 1) {
      stacks.push(stack);
    } else {
      singles.push(stack[0]);
    }
  }

  return { stacks, singles };
}

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

  const { stacks, singles } = groupIdenticalUnits(ctx.units, ctx.warlordId);
  const rendered: ReactElement[] = [];
  const renderedIndices = new Set<number>();

  const findWarlordIndex = (): number => {
    return ctx.units.findIndex(u => u.datasheetId === ctx.warlordId);
  };

  const renderUnitWithAttachment = (unit: ArmyUnit, index: number) => {
    const datasheet = ctx.datasheets.find(ds => ds.id === unit.datasheetId);
    const isWarlord = ctx.warlordId === unit.datasheetId && findWarlordIndex() === index;

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
    renderedIndices.add(index);
  };

  const getRole = (datasheetId: string): string => {
    const ds = ctx.datasheets.find(d => d.id === datasheetId);
    return ds?.role ?? "Other";
  };

  const unitsByRole: Record<string, { stacks: StackedUnit[][]; singles: StackedUnit[] }> = {};

  for (const stack of stacks) {
    const role = getRole(stack[0].unit.datasheetId);
    if (!unitsByRole[role]) unitsByRole[role] = { stacks: [], singles: [] };
    unitsByRole[role].stacks.push(stack);
  }

  for (const single of singles) {
    if (renderedIndices.has(single.index)) continue;
    const role = getRole(single.unit.datasheetId);
    if (!unitsByRole[role]) unitsByRole[role] = { stacks: [], singles: [] };
    unitsByRole[role].singles.push(single);
  }

  const sortedRoles = sortByRoleOrder(Object.keys(unitsByRole));

  for (const role of sortedRoles) {
    const { stacks: roleStacks, singles: roleSingles } = unitsByRole[role];

    const hasUnrenderedUnits = roleStacks.length > 0 ||
      roleSingles.some(s => !renderedIndices.has(s.index));

    if (!hasUnrenderedUnits) continue;

    rendered.push(
      <tr key={`role-header-${role}`} className={styles.roleHeaderRow}>
        <td colSpan={8}>
          <div className={styles.roleHeader}>{role}</div>
        </td>
      </tr>
    );

    for (const stack of roleStacks) {
      const firstUnit = stack[0].unit;
      rendered.push(
        <StackedUnitRow
          key={`stack-${stack[0].index}`}
          stackedUnits={stack}
          datasheet={ctx.datasheets.find((ds) => ds.id === firstUnit.datasheetId)}
          onUpdate={ctx.onUpdate}
          onRemove={ctx.onRemove}
          onCopy={ctx.onCopy}
          readOnly={ctx.readOnly}
        />
      );
      stack.forEach(s => renderedIndices.add(s.index));
    }

    const sortedSingles = [...roleSingles].sort((a, b) => {
      const aIsWarlord = a.index === findWarlordIndex();
      const bIsWarlord = b.index === findWarlordIndex();
      if (aIsWarlord && !bIsWarlord) return -1;
      if (!aIsWarlord && bIsWarlord) return 1;
      return 0;
    });

    for (const { unit, index } of sortedSingles) {
      if (renderedIndices.has(index)) continue;
      renderUnitWithAttachment(unit, index);
    }
  }

  return rendered;
}
