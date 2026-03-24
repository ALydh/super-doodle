import type { ArmyBattleData, Army, BattleUnitData } from "../../types";
import { BattleSize } from "../../types";
import { createArmy } from "../../api";

function buildViewArmy(battleData: ArmyBattleData): Army {
  return {
    factionId: battleData.factionId,
    battleSize: battleData.battleSize as BattleSize,
    detachmentId: battleData.detachmentId,
    warlordId: battleData.warlordId,
    units: battleData.units.map((bu) => bu.unit),
    chapterId: battleData.chapterId,
  };
}

export async function handleCopy(
  battleData: ArmyBattleData,
  navigate: (path: string) => void,
) {
  const persisted = await createArmy(`${battleData.name} (Copy)`, buildViewArmy(battleData));
  navigate(`/armies/${persisted.id}`);
}

export function handleExportJson(battleData: ArmyBattleData) {
  const army = buildViewArmy(battleData);
  const readableUnits = army.units.map((u, i) => {
    const bu = battleData.units[i];
    const models = bu.cost?.description.match(/(\d+)\s*model/i)?.[1];
    return {
      _name: bu.datasheet.name,
      ...(models ? { _models: parseInt(models, 10) } : {}),
      ...u,
    };
  });
  const payload = JSON.stringify({ name: battleData.name, army: { ...army, units: readableUnits } }, null, 2);
  downloadBlob(payload, "application/json", `${battleData.name}.json`);
}

export function handleExportTxt(battleData: ArmyBattleData, totalPoints: number) {
  const lines: string[] = [];
  lines.push(battleData.name);
  lines.push(`${battleData.battleSize} — ${totalPoints}pts`);
  lines.push("");

  const attached = new Map<number, BattleUnitData[]>();
  battleData.units.forEach((bu) => {
    if (bu.unit.attachedToUnitIndex != null) {
      const list = attached.get(bu.unit.attachedToUnitIndex) ?? [];
      list.push(bu);
      attached.set(bu.unit.attachedToUnitIndex, list);
    }
  });

  const printed = new Set<number>();
  battleData.units.forEach((bu, i) => {
    if (printed.has(i)) return;
    printed.add(i);
    const models = bu.cost?.description.match(/(\d+)\s*model/i)?.[1];
    const pts = (bu.cost?.cost ?? 0) + (bu.enhancement?.cost ?? 0);
    let line = `${bu.datasheet.name}`;
    if (models) line += ` (${models})`;
    line += ` — ${pts}pts`;
    if (bu.enhancement) line += ` [${bu.enhancement.name}]`;
    lines.push(line);

    const leaders = attached.get(i);
    if (leaders) {
      for (const leader of leaders) {
        printed.add(battleData.units.indexOf(leader));
        const lPts = (leader.cost?.cost ?? 0) + (leader.enhancement?.cost ?? 0);
        let lLine = `  ↳ ${leader.datasheet.name} — ${lPts}pts`;
        if (leader.enhancement) lLine += ` [${leader.enhancement.name}]`;
        lines.push(lLine);
      }
    }
  });

  downloadBlob(lines.join("\n"), "text/plain", `${battleData.name}.txt`);
}

function downloadBlob(content: string, type: string, filename: string) {
  const blob = new Blob([content], { type });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}
