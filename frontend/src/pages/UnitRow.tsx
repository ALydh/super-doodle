import { useState, Fragment, useEffect, useMemo } from "react";
import type { ArmyUnit, Datasheet, UnitCost, Enhancement, DatasheetLeader, DatasheetOption, WargearSelection } from "../types";

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
}

export function UnitRow({
  unit, index, datasheet, costs, enhancements, leaders, datasheets, options,
  isWarlord, onUpdate, onRemove, onSetWarlord,
}: Props) {
  const [showWargear, setShowWargear] = useState(false);
  
  const unitCosts = costs.filter((c) => c.datasheetId === unit.datasheetId);
  const isCharacter = datasheet?.role === "Characters";

  const unitOptions = options.filter((o) => o.datasheetId === unit.datasheetId);

  const validLeaderTargets = leaders
    .filter((l) => l.leaderId === unit.datasheetId)
    .map((l) => l.attachedId);

  const attachableUnits = datasheets.filter((ds) => validLeaderTargets.includes(ds.id));

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

  return (
    <>
      <tr data-testid="unit-row">
        <td data-testid="unit-row-name">{datasheet?.name ?? unit.datasheetId}</td>
        <td>
          <select
            data-testid="unit-size-select"
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
              data-testid="unit-enhancement-select"
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
          {isCharacter && attachableUnits.length > 0 && (
            <select
              data-testid="unit-leader-select"
              value={unit.attachedLeaderId ?? ""}
              onChange={(e) => onUpdate(index, { ...unit, attachedLeaderId: e.target.value || null })}
            >
              <option value="">No attachment</option>
              {attachableUnits.map((ds) => (
                <option key={ds.id} value={ds.id}>{ds.name}</option>
              ))}
            </select>
          )}
        </td>
        <td>
          {unitOptions.length > 0 && (
            <button 
              data-testid="wargear-toggle"
              onClick={() => setShowWargear(!showWargear)}
            >
              {wargearCount}/{unitOptions.length}
            </button>
          )}
        </td>
        <td data-testid="unit-cost">{totalCost}pts</td>
        <td>
          {isCharacter && (
            <label>
              <input
                type="radio"
                name="warlord"
                data-testid="warlord-radio"
                checked={isWarlord}
                onChange={() => onSetWarlord(index)}
              />
              Warlord
            </label>
          )}
        </td>
        <td>
          <button data-testid="remove-unit" onClick={() => onRemove(index)}>Remove</button>
        </td>
      </tr>
      {showWargear && unitOptions.length > 0 && (
        <tr data-testid="wargear-section">
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
                        data-testid={`wargear-option-${option.line}`}
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
                          data-testid={`wargear-choice-${option.line}`}
                          value={selection?.notes ?? ''}
                          onChange={(e) => handleWargearNotesChange(option.line, e.target.value)}
                          style={{ minWidth: '200px' }}
                        >
                          <option value="">Select wargear...</option>
                          {choices.map((choice, index) => (
                            <option key={index} value={choice}>
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