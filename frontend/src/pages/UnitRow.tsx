import { useState, useEffect, useMemo } from "react";
import type { ArmyUnit, Datasheet, UnitCost, Enhancement, DatasheetLeader, DatasheetOption, WargearSelection, LeaderDisplayMode } from "../types";

interface Props {
  unit: ArmyUnit;
  index: number;
  datasheet: Datasheet | undefined;
  costs: UnitCost[];
  enhancements: Enhancement[];
  leaders: DatasheetLeader[];
  datasheets: Datasheet[];
  options: DatasheetOption[];
  isWarlord: boolean;
  onUpdate: (index: number, unit: ArmyUnit) => void;
  onRemove: (index: number) => void;
  onSetWarlord: (index: number) => void;
  displayMode?: LeaderDisplayMode;
  allUnits?: ArmyUnit[];
  isGroupParent?: boolean;
  isGroupChild?: boolean;
  attachedLeaderInfo?: { name: string; index: number };
}

export function UnitRow({
  unit, index, datasheet, costs, enhancements, leaders, datasheets, options,
  isWarlord, onUpdate, onRemove, onSetWarlord,
  displayMode = "table", allUnits = [], isGroupParent = false, isGroupChild = false, attachedLeaderInfo,
}: Props) {
  const [showWargear, setShowWargear] = useState(false);

  const unitCosts = costs.filter((c) => c.datasheetId === unit.datasheetId);
  const isCharacter = datasheet?.role === "Characters";

  const unitOptions = options.filter((o) => o.datasheetId === unit.datasheetId);

  const validLeaderTargets = leaders
    .filter((l) => l.leaderId === unit.datasheetId)
    .map((l) => l.attachedId);

  const attachableUnitsInArmy = allUnits
    .map((u, i) => ({ unit: u, index: i, ds: datasheets.find(d => d.id === u.datasheetId) }))
    .filter(({ ds }) => ds && validLeaderTargets.includes(ds.id));

  const selectedCost = unitCosts.find((c) => c.line === unit.sizeOptionLine);
  const enhancementCost = unit.enhancementId
    ? enhancements.find((e) => e.id === unit.enhancementId)?.cost ?? 0
    : 0;
  const totalCost = (selectedCost?.cost ?? 0) + enhancementCost;

  // Update local unit when prop changes
  useEffect(() => {
    // Log unit prop changes
  }, [unit]);

  // Memoize wargear count
  const wargearCount = useMemo(() =>
    unit.wargearSelections.filter(s => s.selected).length
  , [unit.wargearSelections]);

  const handleWargearSelectionChange = (optionLine: number, selected: boolean) => {
    const existingSelection = unit.wargearSelections.find(s => s.optionLine === optionLine);
    let updatedSelections: WargearSelection[];

    if (existingSelection) {
      updatedSelections = unit.wargearSelections.map(s =>
        s.optionLine === optionLine ? { ...s, selected } : s
      );
    } else {
      updatedSelections = [...unit.wargearSelections, { optionLine, selected, notes: null }];
    }

    const updatedUnit = { ...unit, wargearSelections: updatedSelections };
    console.log('=== UnitRow calling onUpdate with', { index, updatedUnit });
    onUpdate(index, updatedUnit);
  };

  const handleWargearNotesChange = (optionLine: number, notes: string) => {
    const updatedSelections = unit.wargearSelections.map(s =>
      s.optionLine === optionLine ? { ...s, notes: notes || null } : s
    );
    const updatedUnit = { ...unit, wargearSelections: updatedSelections };
    onUpdate(index, updatedUnit);
  };

  const getWargearSelection = (optionLine: number): WargearSelection | undefined => {
    return unit.wargearSelections.find(s => s.optionLine === optionLine);
  };

  const extractWargearChoices = (description: string): string[] | null => {
    const lowerDesc = description.toLowerCase();
    if (!lowerDesc.includes('one of the following') && !lowerDesc.includes('can be replaced with one of the following')) {
      return null;
    }

    const ulMatch = description.match(/<ul[^>]*>([\s\S]*?)<\/ul>/);
    if (!ulMatch) return null;

    const liMatches = ulMatch[1].match(/<li[^>]*>([\s\S]*?)<\/li>/g);
    if (!liMatches) return [];

    return liMatches.map(li => {
      const content = li.replace(/<[^>]*>/g, '').trim();
      return content;
    }).filter(choice => choice.length > 0);
  };

  const nonCharacterUnitsWithIndices = allUnits
    .map((u, i) => ({ unit: u, index: i, ds: datasheets.find(d => d.id === u.datasheetId) }))
    .filter(({ ds }) => ds?.role !== "Characters");

  const rowClassName = [
    "unit-row",
    isGroupParent ? "group-parent" : "",
    isGroupChild ? "group-child" : "",
  ].filter(Boolean).join(" ");

  const renderLeaderCell = () => {
    if (displayMode === "inline") {
      if (attachedLeaderInfo) {
        return <span className="attached-leader-badge">👤 {attachedLeaderInfo.name}</span>;
      }
      return null;
    }

    if (displayMode === "instance" && isCharacter) {
      return (
        <select
          className="unit-leader-select"
          value={unit.attachedToUnitIndex ?? ""}
          onChange={(e) => onUpdate(index, {
            ...unit,
            attachedToUnitIndex: e.target.value ? Number(e.target.value) : null,
            attachedLeaderId: null,
          })}
        >
          <option value="">No attachment</option>
          {nonCharacterUnitsWithIndices
            .filter(({ index: unitIdx }) => {
              const targetDatasheet = datasheets.find(d => d.id === allUnits[unitIdx].datasheetId);
              return validLeaderTargets.includes(targetDatasheet?.id ?? "");
            })
            .map(({ index: unitIdx, ds }) => (
              <option key={unitIdx} value={unitIdx}>
                {ds?.name} (Unit #{unitIdx + 1})
              </option>
            ))}
        </select>
      );
    }

    if (isCharacter && attachableUnitsInArmy.length > 0) {
      return (
        <select
          className="unit-leader-select"
          value={unit.attachedLeaderId ?? ""}
          onChange={(e) => onUpdate(index, { ...unit, attachedLeaderId: e.target.value || null })}
        >
          <option value="">No attachment</option>
          {attachableUnitsInArmy.map(({ ds }) => (
            <option key={ds!.id} value={ds!.id}>{ds!.name}</option>
          ))}
        </select>
      );
    }

    return null;
  };

  return (
    <>
      <tr className={rowClassName}>
        <td className="unit-row-name">
          {isGroupChild && <span className="group-connector">└─ </span>}
          {datasheet?.name ?? unit.datasheetId}
          {isGroupParent && <span className="group-indicator"> (leading)</span>}
        </td>
        <td>
          <select
            className="unit-size-select"
            value={unit.sizeOptionLine}
            onChange={(e) => onUpdate(index, { ...unit, sizeOptionLine: Number(e.target.value) })}
          >
            {unitCosts.map((c) => (
              <option key={c.line} value={c.line}>{c.description} ({c.cost}pts)</option>
            ))}
          </select>
        </td>
        <td>
          {isCharacter && (
            <select
              className="unit-enhancement-select"
              value={unit.enhancementId ?? ""}
              onChange={(e) => onUpdate(index, { ...unit, enhancementId: e.target.value || null })}
            >
              <option value="">None</option>
              {enhancements.map((e) => (
                <option key={e.id} value={e.id}>{e.name} ({e.cost}pts)</option>
              ))}
            </select>
          )}
        </td>
        <td>
          {renderLeaderCell()}
        </td>
        <td>
          {unitOptions.length > 0 && (
            <button
              className="btn-toggle wargear-toggle"
              onClick={() => setShowWargear(!showWargear)}
            >
              {wargearCount}/{unitOptions.length}
            </button>
          )}
        </td>
        <td className="unit-cost">{totalCost}pts</td>
        <td>
          {isCharacter && (
            <label>
              <input
                type="radio"
                name="warlord"
                className="warlord-radio"
                checked={isWarlord}
                onChange={() => onSetWarlord(index)}
              />
              Warlord
            </label>
          )}
        </td>
        <td>
          <button className="btn-remove remove-unit" onClick={() => onRemove(index)}>Remove</button>
        </td>
      </tr>
      {showWargear && unitOptions.length > 0 && (
        <tr className="wargear-section">
          <td colSpan={8}>
            <div style={{ padding: '12px' }}>
              <h4>Wargear Options:</h4>
              {unitOptions.map((option) => {
                const selection = getWargearSelection(option.line);
                const isSelected = selection?.selected ?? false;
                const choices = extractWargearChoices(option.description);
                const hasChoices = choices && choices.length > 0;

                return (
                  <div key={option.line} style={{ marginBottom: '8px' }}>
                    <label style={{ display: 'flex', alignItems: 'flex-start', gap: '8px' }}>
                      <input
                        type="checkbox"
                        className={`wargear-option wargear-option-${option.line}`}
                        checked={isSelected}
                        onChange={(e) => {
                          console.log('Checkbox onChange triggered:', option.line, e.target.checked);
                          handleWargearSelectionChange(option.line, e.target.checked);
                        }}
                      />
                      <span
                        dangerouslySetInnerHTML={{ __html: option.description }}
                        style={{ flex: 1 }}
                      />
                    </label>
                    {isSelected && hasChoices && (
                      <div style={{ marginLeft: '24px', marginTop: '4px' }}>
                        <select
                          className={`wargear-choice wargear-choice-${option.line}`}
                          value={selection?.notes ?? ''}
                          onChange={(e) => handleWargearNotesChange(option.line, e.target.value)}
                          style={{ minWidth: '200px' }}
                        >
                          <option value="">Select wargear...</option>
                          {choices.map((choice, idx) => (
                            <option key={idx} value={choice}>
                              {choice}
                            </option>
                          ))}
                        </select>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </td>
        </tr>
      )}
    </>
  );
}
