import type { ArmyUnit, UnitCost, Enhancement } from "./types";

export function unitOrdinals(units: ArmyUnit[]): number[] {
  const counts = new Map<string, number>();
  return units.map((u) => {
    const next = (counts.get(u.datasheetId) ?? 0) + 1;
    counts.set(u.datasheetId, next);
    return next;
  });
}

export function costForOrdinal(
  costs: UnitCost[],
  datasheetId: string,
  line: number,
  ordinal: number,
): UnitCost | undefined {
  const forLine = costs.filter((c) => c.datasheetId === datasheetId && c.line === line);
  return (
    forLine.find((c) => ordinal >= c.minCount && (c.maxCount === null || ordinal <= c.maxCount)) ??
    forLine[0]
  );
}

export function armyPointsTotal(
  units: ArmyUnit[],
  costs: UnitCost[],
  enhancements: Enhancement[],
): number {
  const ordinals = unitOrdinals(units);
  return units.reduce((sum, u, i) => {
    const cost = costForOrdinal(costs, u.datasheetId, u.sizeOptionLine, ordinals[i]);
    const enhCost = u.enhancementId
      ? enhancements.find((e) => e.id === u.enhancementId)?.cost ?? 0
      : 0;
    return sum + (cost?.cost ?? 0) + enhCost;
  }, 0);
}
