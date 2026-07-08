import { describe, it, expect } from "vitest";
import { unitOrdinals, costForOrdinal, armyPointsTotal } from "./points";
import type { ArmyUnit, UnitCost, Enhancement } from "./types";

function unit(datasheetId: string, sizeOptionLine = 1, enhancementId: string | null = null): ArmyUnit {
  return {
    datasheetId,
    sizeOptionLine,
    enhancementId,
    attachedLeaderId: null,
    attachedToUnitIndex: null,
    wargearSelections: [],
  };
}

function cost(datasheetId: string, line: number, cost: number, minCount: number, maxCount: number | null): UnitCost {
  return { datasheetId, line, description: `${line} models`, cost, minCount, maxCount };
}

describe("unitOrdinals", () => {
  it("numbers repeated datasheets per occurrence", () => {
    expect(unitOrdinals([unit("A"), unit("B"), unit("A"), unit("A")])).toEqual([1, 1, 2, 3]);
  });
});

describe("costForOrdinal", () => {
  const costs = [
    cost("A", 1, 240, 1, 1),
    cost("A", 1, 260, 2, null),
  ];

  it("picks the first tier for the 1st unit", () => {
    expect(costForOrdinal(costs, "A", 1, 1)?.cost).toBe(240);
  });

  it("picks the escalated tier for the 2nd+ unit", () => {
    expect(costForOrdinal(costs, "A", 1, 2)?.cost).toBe(260);
    expect(costForOrdinal(costs, "A", 1, 5)?.cost).toBe(260);
  });

  it("falls back to the base row when no tier matches", () => {
    const single = [cost("B", 1, 100, 1, null)];
    expect(costForOrdinal(single, "B", 1, 3)?.cost).toBe(100);
  });
});

describe("armyPointsTotal", () => {
  const enhancements: Enhancement[] = [
    { id: "e1", factionId: "SM", name: "Relic", cost: 15, detachment: null, detachmentId: null, legend: null, description: "" } as Enhancement,
  ];

  it("charges escalating cost for the second identical unit", () => {
    const costs = [cost("A", 1, 240, 1, 1), cost("A", 1, 260, 2, null)];
    const total = armyPointsTotal([unit("A"), unit("A")], costs, enhancements);
    expect(total).toBe(500);
  });

  it("charges the 1st-to-2nd vs 3rd+ tiers correctly", () => {
    const costs = [cost("A", 1, 80, 1, 2), cost("A", 1, 90, 3, null)];
    const total = armyPointsTotal([unit("A"), unit("A"), unit("A")], costs, enhancements);
    expect(total).toBe(80 + 80 + 90);
  });

  it("adds enhancement cost on top of the tiered unit cost", () => {
    const costs = [cost("A", 1, 240, 1, 1), cost("A", 1, 260, 2, null)];
    const total = armyPointsTotal([unit("A", 1, "e1")], costs, enhancements);
    expect(total).toBe(255);
  });

  it("keeps single-tier units unchanged", () => {
    const costs = [cost("A", 1, 100, 1, null)];
    expect(armyPointsTotal([unit("A"), unit("A")], costs, enhancements)).toBe(200);
  });
});
