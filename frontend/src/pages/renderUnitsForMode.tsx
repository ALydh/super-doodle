import type { ReactElement } from "react";
import type {
  ArmyUnit, Datasheet,
  LeaderDisplayMode,
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

function getAttachedLeaderForUnit(
  unitIndex: number,
  units: ArmyUnit[],
  datasheets: Datasheet[]
): { leaderUnit: ArmyUnit; leaderIndex: number; leaderDatasheet: Datasheet | undefined } | null {
  const unit = units[unitIndex];
  const unitDatasheet = datasheets.find(ds => ds.id === unit.datasheetId);
  if (!unitDatasheet) return null;

  for (let i = 0; i < units.length; i++) {
    if (i === unitIndex) continue;
    const potentialLeader = units[i];
    const leaderDs = datasheets.find(ds => ds.id === potentialLeader.datasheetId);
    if (leaderDs?.role !== "Characters") continue;

    if (potentialLeader.attachedLeaderId === unit.datasheetId) {
      return { leaderUnit: potentialLeader, leaderIndex: i, leaderDatasheet: leaderDs };
    }
    if (potentialLeader.attachedToUnitIndex === unitIndex) {
      return { leaderUnit: potentialLeader, leaderIndex: i, leaderDatasheet: leaderDs };
    }
  }
  return null;
}

function renderTableMode(ctx: RenderContext): ReactElement[] {
  const { stacks, singles } = groupIdenticalUnits(ctx.units, ctx.warlordId);
  const result: ReactElement[] = [];

  for (const stack of stacks) {
    const firstUnit = stack[0].unit;
    result.push(
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
  }

  for (const { unit, index } of singles) {
    result.push(
      <UnitRow
        key={index}
        unit={unit}
        index={index}
        datasheet={ctx.datasheets.find((ds) => ds.id === unit.datasheetId)}
        isWarlord={ctx.warlordId === unit.datasheetId}
        onUpdate={ctx.onUpdate}
        onRemove={ctx.onRemove}
        onCopy={ctx.onCopy}
        onSetWarlord={ctx.onSetWarlord}
        displayMode="table"
        allUnits={ctx.units}
        readOnly={ctx.readOnly}
      />
    );
  }

  return result;
}

function renderGroupedMode(ctx: RenderContext): ReactElement[] {
  const { stacks, singles } = groupIdenticalUnits(ctx.units, ctx.warlordId);
  const rendered: ReactElement[] = [];
  const renderedIndices = new Set<number>();

  const findWarlordIndex = (): number => {
    return ctx.units.findIndex(u => u.datasheetId === ctx.warlordId);
  };

  const renderUnitWithAttachment = (unit: ArmyUnit, index: number) => {
    const datasheet = ctx.datasheets.find(ds => ds.id === unit.datasheetId);
    const isCharacter = datasheet?.role === "Characters";
    const isWarlord = ctx.warlordId === unit.datasheetId && findWarlordIndex() === index;

    if (isCharacter && unit.attachedLeaderId) {
      const bodyguardIndex = ctx.units.findIndex(u => u.datasheetId === unit.attachedLeaderId);
      const bodyguardUnit = bodyguardIndex >= 0 ? ctx.units[bodyguardIndex] : null;
      const bodyguardDatasheet = bodyguardUnit ? ctx.datasheets.find(ds => ds.id === bodyguardUnit.datasheetId) : undefined;

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
          displayMode="grouped"
          allUnits={ctx.units}
          isGroupParent={!!bodyguardUnit}
          readOnly={ctx.readOnly}
        />
      );
      renderedIndices.add(index);

      if (bodyguardUnit && bodyguardIndex >= 0 && !renderedIndices.has(bodyguardIndex)) {
        rendered.push(
          <UnitRow
            key={bodyguardIndex}
            unit={bodyguardUnit}
            index={bodyguardIndex}
            datasheet={bodyguardDatasheet}
            isWarlord={false}
            onUpdate={ctx.onUpdate}
            onRemove={ctx.onRemove}
            onCopy={ctx.onCopy}
            onSetWarlord={ctx.onSetWarlord}
            displayMode="grouped"
            allUnits={ctx.units}
            isGroupChild={true}
            readOnly={ctx.readOnly}
          />
        );
        renderedIndices.add(bodyguardIndex);
      }
    } else {
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
          displayMode="grouped"
          allUnits={ctx.units}
          readOnly={ctx.readOnly}
        />
      );
      renderedIndices.add(index);
    }
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

function renderInlineMode(ctx: RenderContext): ReactElement[] {
  return ctx.units.map((unit, i) => {
    const datasheet = ctx.datasheets.find(ds => ds.id === unit.datasheetId);
    const attachedLeader = getAttachedLeaderForUnit(i, ctx.units, ctx.datasheets);

    return (
      <UnitRow
        key={i}
        unit={unit}
        index={i}
        datasheet={datasheet}
        isWarlord={ctx.warlordId === unit.datasheetId}
        onUpdate={ctx.onUpdate}
        onRemove={ctx.onRemove}
        onCopy={ctx.onCopy}
        onSetWarlord={ctx.onSetWarlord}
        displayMode="inline"
        allUnits={ctx.units}
        attachedLeaderInfo={attachedLeader ? {
          name: attachedLeader.leaderDatasheet?.name ?? "Leader",
          index: attachedLeader.leaderIndex,
        } : undefined}
        readOnly={ctx.readOnly}
      />
    );
  });
}

function renderInstanceMode(ctx: RenderContext): ReactElement[] {
  return ctx.units.map((unit, i) => (
    <UnitRow
      key={i}
      unit={unit}
      index={i}
      datasheet={ctx.datasheets.find((ds) => ds.id === unit.datasheetId)}
      isWarlord={ctx.warlordId === unit.datasheetId}
      onUpdate={ctx.onUpdate}
      onRemove={ctx.onRemove}
      onCopy={ctx.onCopy}
      onSetWarlord={ctx.onSetWarlord}
      displayMode="instance"
      allUnits={ctx.units}
      readOnly={ctx.readOnly}
    />
  ));
}

export function renderUnitsForMode(
  mode: LeaderDisplayMode,
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

  switch (mode) {
    case "grouped":
      return renderGroupedMode(ctx);
    case "inline":
      return renderInlineMode(ctx);
    case "instance":
      return renderInstanceMode(ctx);
    case "table":
    default:
      return renderTableMode(ctx);
  }
}
