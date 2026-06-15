import type {
  ArmyBattleData, Army, BattleUnitData,
  DetachmentAbility, Stratagem,
} from "../../types";
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

export interface DenseExportContext {
  totalPoints: number;
  detachmentName: string;
  chapterName: string | null;
  detachmentAbilities: DetachmentAbility[];
  detachmentStratagems: Stratagem[];
}

function toNum(v: string | null): number | string | null {
  if (v == null) return null;
  const cleaned = v.trim().replace(/"$/, "");
  if (cleaned === "") return v;
  const n = Number(cleaned);
  if (Number.isFinite(n) && String(n) === cleaned) return n;
  return v;
}

export function handleExportJsonDense(battleData: ArmyBattleData, ctx: DenseExportContext) {
  const warlordUnit = battleData.units.find((bu) => bu.unit.datasheetId === battleData.warlordId);

  const units = battleData.units.map((bu) => {
    const modelsMatch = bu.cost?.description.match(/(\d+)\s*model/i)?.[1];
    const attachedToName = bu.unit.attachedToUnitIndex != null
      ? battleData.units[bu.unit.attachedToUnitIndex]?.datasheet.name ?? null
      : null;
    return {
      name: bu.datasheet.name,
      datasheetId: bu.unit.datasheetId,
      role: bu.datasheet.role,
      points: (bu.cost?.cost ?? 0) + (bu.enhancement?.cost ?? 0),
      models: modelsMatch ? parseInt(modelsMatch, 10) : 1,
      modelsDescription: bu.cost?.description ?? null,
      isWarlord: bu.unit.datasheetId === battleData.warlordId,
      isAllied: bu.unit.isAllied ?? false,
      attachedToUnitIndex: bu.unit.attachedToUnitIndex,
      attachedToName,
      legend: bu.datasheet.legend,
      loadout: bu.datasheet.loadout,
      transport: bu.datasheet.transport,
      damagedW: bu.datasheet.damagedW,
      damagedDescription: bu.datasheet.damagedDescription,
      enhancement: bu.enhancement
        ? {
            id: bu.enhancement.id,
            name: bu.enhancement.name,
            cost: bu.enhancement.cost,
            description: bu.enhancement.description,
            legend: bu.enhancement.legend,
            detachment: bu.enhancement.detachment,
          }
        : null,
      profiles: bu.profiles.map((p) => ({
        name: p.name,
        movement: toNum(p.movement),
        toughness: toNum(p.toughness),
        save: toNum(p.save),
        invulnerableSave: toNum(p.invulnerableSave),
        invulnerableSaveDescription: p.invulnerableSaveDescription,
        wounds: p.wounds,
        leadership: toNum(p.leadership),
        objectiveControl: p.objectiveControl,
        baseSize: toNum(p.baseSize),
        baseSizeDescription: p.baseSizeDescription,
      })),
      wargear: bu.wargear.map((w) => ({
        name: w.wargear.name,
        modelType: w.modelType,
        quantity: w.quantity,
        range: toNum(w.wargear.range),
        weaponType: w.wargear.weaponType,
        attacks: toNum(w.wargear.attacks),
        ballisticSkill: toNum(w.wargear.ballisticSkill),
        strength: toNum(w.wargear.strength),
        armorPenetration: toNum(w.wargear.armorPenetration),
        damage: toNum(w.wargear.damage),
        description: w.wargear.description,
      })),
      abilities: bu.abilities.map((a) => ({
        name: a.name,
        description: a.description,
        model: a.model,
        abilityType: a.abilityType,
        parameter: a.parameter,
      })),
      keywords: bu.keywords.map((k) => ({
        keyword: k.keyword,
        model: k.model,
        isFactionKeyword: k.isFactionKeyword,
      })),
    };
  });

  const payload = {
    name: battleData.name,
    factionId: battleData.factionId,
    battleSize: battleData.battleSize,
    totalPoints: ctx.totalPoints,
    detachment: { id: battleData.detachmentId, name: ctx.detachmentName },
    chapter: battleData.chapterId
      ? { id: battleData.chapterId, name: ctx.chapterName }
      : null,
    warlord: {
      datasheetId: battleData.warlordId,
      name: warlordUnit?.datasheet.name ?? null,
    },
    detachmentAbilities: ctx.detachmentAbilities.map((a) => ({
      name: a.name,
      description: a.description,
      legend: a.legend,
      detachment: a.detachment,
    })),
    stratagems: ctx.detachmentStratagems.map((s) => ({
      name: s.name,
      cpCost: s.cpCost,
      phase: s.phase,
      turn: s.turn,
      stratagemType: s.stratagemType,
      description: s.description,
      legend: s.legend,
      detachment: s.detachment,
    })),
    checklistNotes: battleData.checklistNotes,
    units,
  };

  downloadBlob(JSON.stringify(payload, null, 2), "application/json", `${battleData.name}.dense.json`);
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
