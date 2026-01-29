import type { ReactElement } from "react";
import type {
  ArmyUnit, Datasheet, UnitCost, Enhancement,
  DatasheetLeader, DatasheetOption, LeaderDisplayMode,
} from "../types";
import { UnitRow } from "./UnitRow";

interface RenderContext {
  units: ArmyUnit[];
  datasheets: Datasheet[];
  costs: UnitCost[];
  enhancements: Enhancement[];
  leaders: DatasheetLeader[];
  options: DatasheetOption[];
  warlordId: string;
  onUpdate: (index: number, unit: ArmyUnit) => void;
  onRemove: (index: number) => void;
  onSetWarlord: (index: number) => void;
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
  return ctx.units.map((unit, i) => (
    <UnitRow
      key={i}
      unit={unit}
      index={i}
      datasheet={ctx.datasheets.find((ds) => ds.id === unit.datasheetId)}
      costs={ctx.costs}
      enhancements={ctx.enhancements}
      leaders={ctx.leaders}
      datasheets={ctx.datasheets}
      options={ctx.options}
      isWarlord={ctx.warlordId === unit.datasheetId}
      onUpdate={ctx.onUpdate}
      onRemove={ctx.onRemove}
      onSetWarlord={ctx.onSetWarlord}
      displayMode="table"
      allUnits={ctx.units}
    />
  ));
}

function renderGroupedMode(ctx: RenderContext): ReactElement[] {
  const rendered: ReactElement[] = [];
  const renderedIndices = new Set<number>();

  ctx.units.forEach((unit, i) => {
    if (renderedIndices.has(i)) return;

    const datasheet = ctx.datasheets.find(ds => ds.id === unit.datasheetId);
    const isCharacter = datasheet?.role === "Characters";

    if (isCharacter && unit.attachedLeaderId) {
      const bodyguardIndex = ctx.units.findIndex(u => u.datasheetId === unit.attachedLeaderId);
      const bodyguardUnit = bodyguardIndex >= 0 ? ctx.units[bodyguardIndex] : null;
      const bodyguardDatasheet = bodyguardUnit ? ctx.datasheets.find(ds => ds.id === bodyguardUnit.datasheetId) : undefined;

      rendered.push(
        <UnitRow
          key={i}
          unit={unit}
          index={i}
          datasheet={datasheet}
          costs={ctx.costs}
          enhancements={ctx.enhancements}
          leaders={ctx.leaders}
          datasheets={ctx.datasheets}
          options={ctx.options}
          isWarlord={ctx.warlordId === unit.datasheetId}
          onUpdate={ctx.onUpdate}
          onRemove={ctx.onRemove}
          onSetWarlord={ctx.onSetWarlord}
          displayMode="grouped"
          allUnits={ctx.units}
          isGroupParent={!!bodyguardUnit}
        />
      );
      renderedIndices.add(i);

      if (bodyguardUnit && bodyguardIndex >= 0) {
        rendered.push(
          <UnitRow
            key={bodyguardIndex}
            unit={bodyguardUnit}
            index={bodyguardIndex}
            datasheet={bodyguardDatasheet}
            costs={ctx.costs}
            enhancements={ctx.enhancements}
            leaders={ctx.leaders}
            datasheets={ctx.datasheets}
            options={ctx.options}
            isWarlord={ctx.warlordId === bodyguardUnit.datasheetId}
            onUpdate={ctx.onUpdate}
            onRemove={ctx.onRemove}
            onSetWarlord={ctx.onSetWarlord}
            displayMode="grouped"
            allUnits={ctx.units}
            isGroupChild={true}
          />
        );
        renderedIndices.add(bodyguardIndex);
      }
    }
  });

  ctx.units.forEach((unit, i) => {
    if (renderedIndices.has(i)) return;
    const datasheet = ctx.datasheets.find(ds => ds.id === unit.datasheetId);
    rendered.push(
      <UnitRow
        key={i}
        unit={unit}
        index={i}
        datasheet={datasheet}
        costs={ctx.costs}
        enhancements={ctx.enhancements}
        leaders={ctx.leaders}
        datasheets={ctx.datasheets}
        options={ctx.options}
        isWarlord={ctx.warlordId === unit.datasheetId}
        onUpdate={ctx.onUpdate}
        onRemove={ctx.onRemove}
        onSetWarlord={ctx.onSetWarlord}
        displayMode="grouped"
        allUnits={ctx.units}
      />
    );
    renderedIndices.add(i);
  });

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
        costs={ctx.costs}
        enhancements={ctx.enhancements}
        leaders={ctx.leaders}
        datasheets={ctx.datasheets}
        options={ctx.options}
        isWarlord={ctx.warlordId === unit.datasheetId}
        onUpdate={ctx.onUpdate}
        onRemove={ctx.onRemove}
        onSetWarlord={ctx.onSetWarlord}
        displayMode="inline"
        allUnits={ctx.units}
        attachedLeaderInfo={attachedLeader ? {
          name: attachedLeader.leaderDatasheet?.name ?? "Leader",
          index: attachedLeader.leaderIndex,
        } : undefined}
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
      costs={ctx.costs}
      enhancements={ctx.enhancements}
      leaders={ctx.leaders}
      datasheets={ctx.datasheets}
      options={ctx.options}
      isWarlord={ctx.warlordId === unit.datasheetId}
      onUpdate={ctx.onUpdate}
      onRemove={ctx.onRemove}
      onSetWarlord={ctx.onSetWarlord}
      displayMode="instance"
      allUnits={ctx.units}
    />
  ));
}

export function renderUnitsForMode(
  mode: LeaderDisplayMode,
  units: ArmyUnit[],
  datasheets: Datasheet[],
  costs: UnitCost[],
  enhancements: Enhancement[],
  leaders: DatasheetLeader[],
  options: DatasheetOption[],
  warlordId: string,
  onUpdate: (index: number, unit: ArmyUnit) => void,
  onRemove: (index: number) => void,
  onSetWarlord: (index: number) => void
): ReactElement[] {
  const ctx: RenderContext = {
    units, datasheets, costs, enhancements, leaders, options,
    warlordId, onUpdate, onRemove, onSetWarlord,
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
