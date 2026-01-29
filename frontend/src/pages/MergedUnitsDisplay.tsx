import { useState } from "react";
import type {
  ArmyUnit, Datasheet, UnitCost, Enhancement,
  DatasheetLeader, DatasheetOption,
} from "../types";

interface Props {
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

interface UnitGroup {
  type: "standalone" | "merged";
  squadUnit?: ArmyUnit;
  squadIndex?: number;
  squadDatasheet?: Datasheet;
  leaderUnit?: ArmyUnit;
  leaderIndex?: number;
  leaderDatasheet?: Datasheet;
  unit?: ArmyUnit;
  index?: number;
  datasheet?: Datasheet;
}

export function MergedUnitsDisplay({
  units, datasheets, costs, enhancements, leaders,
  warlordId, onUpdate, onRemove, onSetWarlord,
}: Props) {
  void leaders;
  const [expandedGroups, setExpandedGroups] = useState<Set<number>>(new Set());

  const groups: UnitGroup[] = [];
  const usedIndices = new Set<number>();

  units.forEach((unit, i) => {
    if (usedIndices.has(i)) return;

    const datasheet = datasheets.find(ds => ds.id === unit.datasheetId);
    const isCharacter = datasheet?.role === "Characters";

    if (isCharacter && unit.attachedLeaderId) {
      const squadIndex = units.findIndex((u, idx) =>
        idx !== i && u.datasheetId === unit.attachedLeaderId
      );
      if (squadIndex !== -1 && !usedIndices.has(squadIndex)) {
        const squadUnit = units[squadIndex];
        const squadDatasheet = datasheets.find(ds => ds.id === squadUnit.datasheetId);
        groups.push({
          type: "merged",
          squadUnit,
          squadIndex,
          squadDatasheet,
          leaderUnit: unit,
          leaderIndex: i,
          leaderDatasheet: datasheet,
        });
        usedIndices.add(i);
        usedIndices.add(squadIndex);
        return;
      }
    }

    if (isCharacter && unit.attachedToUnitIndex !== null) {
      const squadIndex = unit.attachedToUnitIndex;
      if (squadIndex >= 0 && squadIndex < units.length && !usedIndices.has(squadIndex)) {
        const squadUnit = units[squadIndex];
        const squadDatasheet = datasheets.find(ds => ds.id === squadUnit.datasheetId);
        groups.push({
          type: "merged",
          squadUnit,
          squadIndex,
          squadDatasheet,
          leaderUnit: unit,
          leaderIndex: i,
          leaderDatasheet: datasheet,
        });
        usedIndices.add(i);
        usedIndices.add(squadIndex);
        return;
      }
    }
  });

  units.forEach((unit, i) => {
    if (usedIndices.has(i)) return;
    const datasheet = datasheets.find(ds => ds.id === unit.datasheetId);
    groups.push({
      type: "standalone",
      unit,
      index: i,
      datasheet,
    });
    usedIndices.add(i);
  });

  const toggleGroup = (groupKey: number) => {
    const newExpanded = new Set(expandedGroups);
    if (newExpanded.has(groupKey)) {
      newExpanded.delete(groupKey);
    } else {
      newExpanded.add(groupKey);
    }
    setExpandedGroups(newExpanded);
  };

  const getCost = (unit: ArmyUnit) => {
    const baseCost = costs.find(c => c.datasheetId === unit.datasheetId && c.line === unit.sizeOptionLine)?.cost ?? 0;
    const enhCost = unit.enhancementId ? enhancements.find(e => e.id === unit.enhancementId)?.cost ?? 0 : 0;
    return baseCost + enhCost;
  };

  return (
    <div className="merged-units-display">
      {groups.map((group, groupIdx) => {
        if (group.type === "merged") {
          const isExpanded = expandedGroups.has(groupIdx);
          const totalCost = getCost(group.squadUnit!) + getCost(group.leaderUnit!);
          const isWarlord = warlordId === group.leaderUnit!.datasheetId;

          return (
            <div key={groupIdx} className="merged-group">
              <div
                className={`merged-group-header ${isExpanded ? "expanded" : ""}`}
                onClick={() => toggleGroup(groupIdx)}
              >
                <span className="merge-indicator">⚔</span>
                <span className="merged-name">
                  {group.leaderDatasheet?.name} + {group.squadDatasheet?.name}
                </span>
                <span className="merged-cost">{totalCost}pts</span>
                {isWarlord && <span className="warlord-badge">★ Warlord</span>}
                <span className="expand-icon">{isExpanded ? "▼" : "▶"}</span>
              </div>
              {isExpanded && (
                <div className="merged-group-details">
                  <div className="merged-unit-detail">
                    <strong>Leader: {group.leaderDatasheet?.name}</strong>
                    <span> ({getCost(group.leaderUnit!)}pts)</span>
                    <div className="merged-unit-controls">
                      <select
                        value={group.leaderUnit!.sizeOptionLine}
                        onChange={(e) => onUpdate(group.leaderIndex!, {
                          ...group.leaderUnit!,
                          sizeOptionLine: Number(e.target.value)
                        })}
                      >
                        {costs.filter(c => c.datasheetId === group.leaderUnit!.datasheetId).map(c => (
                          <option key={c.line} value={c.line}>{c.description} ({c.cost}pts)</option>
                        ))}
                      </select>
                      <select
                        value={group.leaderUnit!.enhancementId ?? ""}
                        onChange={(e) => onUpdate(group.leaderIndex!, {
                          ...group.leaderUnit!,
                          enhancementId: e.target.value || null
                        })}
                      >
                        <option value="">No Enhancement</option>
                        {enhancements.map(e => (
                          <option key={e.id} value={e.id}>{e.name} ({e.cost}pts)</option>
                        ))}
                      </select>
                      <label>
                        <input
                          type="radio"
                          name="warlord"
                          checked={isWarlord}
                          onChange={() => onSetWarlord(group.leaderIndex!)}
                        />
                        Warlord
                      </label>
                    </div>
                  </div>
                  <div className="merged-unit-detail">
                    <strong>Bodyguard: {group.squadDatasheet?.name}</strong>
                    <span> ({getCost(group.squadUnit!)}pts)</span>
                    <div className="merged-unit-controls">
                      <select
                        value={group.squadUnit!.sizeOptionLine}
                        onChange={(e) => onUpdate(group.squadIndex!, {
                          ...group.squadUnit!,
                          sizeOptionLine: Number(e.target.value)
                        })}
                      >
                        {costs.filter(c => c.datasheetId === group.squadUnit!.datasheetId).map(c => (
                          <option key={c.line} value={c.line}>{c.description} ({c.cost}pts)</option>
                        ))}
                      </select>
                    </div>
                  </div>
                  <div className="merged-group-actions">
                    <button
                      className="btn-detach"
                      onClick={() => onUpdate(group.leaderIndex!, {
                        ...group.leaderUnit!,
                        attachedLeaderId: null,
                        attachedToUnitIndex: null,
                      })}
                    >
                      Detach Leader
                    </button>
                    <button
                      className="btn-remove"
                      onClick={() => {
                        onRemove(Math.max(group.leaderIndex!, group.squadIndex!));
                        onRemove(Math.min(group.leaderIndex!, group.squadIndex!));
                      }}
                    >
                      Remove Group
                    </button>
                  </div>
                </div>
              )}
            </div>
          );
        } else {
          const unit = group.unit!;
          const datasheet = group.datasheet;
          const index = group.index!;
          const isCharacter = datasheet?.role === "Characters";
          const unitCost = getCost(unit);

          const validLeaderTargets = leaders
            .filter(l => l.leaderId === unit.datasheetId)
            .map(l => l.attachedId);
          const attachableUnits = datasheets.filter(ds => validLeaderTargets.includes(ds.id));

          return (
            <div key={groupIdx} className="standalone-unit">
              <div className="standalone-unit-row">
                <span className="unit-name">{datasheet?.name}</span>
                <select
                  value={unit.sizeOptionLine}
                  onChange={(e) => onUpdate(index, { ...unit, sizeOptionLine: Number(e.target.value) })}
                >
                  {costs.filter(c => c.datasheetId === unit.datasheetId).map(c => (
                    <option key={c.line} value={c.line}>{c.description} ({c.cost}pts)</option>
                  ))}
                </select>
                {isCharacter && (
                  <>
                    <select
                      value={unit.enhancementId ?? ""}
                      onChange={(e) => onUpdate(index, { ...unit, enhancementId: e.target.value || null })}
                    >
                      <option value="">No Enhancement</option>
                      {enhancements.map(e => (
                        <option key={e.id} value={e.id}>{e.name} ({e.cost}pts)</option>
                      ))}
                    </select>
                    {attachableUnits.length > 0 && (
                      <select
                        value={unit.attachedLeaderId ?? ""}
                        onChange={(e) => onUpdate(index, { ...unit, attachedLeaderId: e.target.value || null })}
                      >
                        <option value="">No Attachment</option>
                        {attachableUnits.map(ds => (
                          <option key={ds.id} value={ds.id}>{ds.name}</option>
                        ))}
                      </select>
                    )}
                    <label>
                      <input
                        type="radio"
                        name="warlord"
                        checked={warlordId === unit.datasheetId}
                        onChange={() => onSetWarlord(index)}
                      />
                      Warlord
                    </label>
                  </>
                )}
                <span className="unit-cost">{unitCost}pts</span>
                <button className="btn-remove" onClick={() => onRemove(index)}>Remove</button>
              </div>
            </div>
          );
        }
      })}
    </div>
  );
}
