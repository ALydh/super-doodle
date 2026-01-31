import { useState, useEffect, useMemo, useRef } from "react";
import type { ArmyUnit, Datasheet, UnitCost, Enhancement, DatasheetLeader, DatasheetOption, WargearSelection, LeaderDisplayMode, DatasheetDetail, WargearWithQuantity } from "../types";
import { fetchDatasheetDetail, filterWargear } from "../api";
import { WeaponAbilityText } from "./WeaponAbilityText";

function parseUnitSize(description: string): number {
  const match = description.match(/(\d+)\s*model/i);
  return match ? parseInt(match[1], 10) : 1;
}

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
  onCopy: (index: number) => void;
  onSetWarlord: (index: number) => void;
  displayMode?: LeaderDisplayMode;
  allUnits?: ArmyUnit[];
  isGroupParent?: boolean;
  isGroupChild?: boolean;
  attachedLeaderInfo?: { name: string; index: number };
  readOnly?: boolean;
}

export function UnitRow({
  unit, index, datasheet, costs, enhancements, leaders, datasheets, options,
  isWarlord, onUpdate, onRemove, onCopy, onSetWarlord,
  displayMode = "table", allUnits = [], isGroupParent = false, isGroupChild = false, attachedLeaderInfo,
  readOnly = false,
}: Props) {
  const [expanded, setExpanded] = useState(false);
  const [detail, setDetail] = useState<DatasheetDetail | null>(null);
  const [filteredWargear, setFilteredWargear] = useState<WargearWithQuantity[]>([]);
  const fetchingRef = useRef(false);

  const unitCosts = costs.filter((c) => c.datasheetId === unit.datasheetId);
  const isCharacter = datasheet?.role === "Characters";
  const unitOptions = options.filter((o) => o.datasheetId === unit.datasheetId);

  const validLeaderTargets = leaders
    .filter((l) => l.leaderId === unit.datasheetId)
    .map((l) => l.attachedId);

  const attachableUnitsInArmy = allUnits
    .map((u, i) => ({ unit: u, index: i, ds: datasheets.find(d => d.id === u.datasheetId) }))
    .filter(({ ds }) => ds && validLeaderTargets.includes(ds.id));

  const getUnitNumber = (unitIndex: number, datasheetId: string): { num: number; total: number } => {
    const sameTypeUnits = allUnits
      .map((u, i) => ({ datasheetId: u.datasheetId, index: i }))
      .filter(u => u.datasheetId === datasheetId);
    const num = sameTypeUnits.findIndex(u => u.index === unitIndex) + 1;
    return { num, total: sameTypeUnits.length };
  };

  const getUnitDisplayName = (ds: { name: string; id: string } | undefined, unitIndex: number): string => {
    if (!ds) return "Unknown";
    const { num, total } = getUnitNumber(unitIndex, ds.id);
    return total > 1 ? `${ds.name} #${num}` : ds.name;
  };

  const thisUnitNumber = getUnitNumber(index, unit.datasheetId);

  const selectedCost = unitCosts.find((c) => c.line === unit.sizeOptionLine);
  const enhancementCost = unit.enhancementId
    ? enhancements.find((e) => e.id === unit.enhancementId)?.cost ?? 0
    : 0;
  const totalCost = (selectedCost?.cost ?? 0) + enhancementCost;

  useEffect(() => {
    if (expanded && !detail && !fetchingRef.current) {
      fetchingRef.current = true;
      fetchDatasheetDetail(unit.datasheetId)
        .then(setDetail)
        .finally(() => { fetchingRef.current = false; });
    }
  }, [expanded, detail, unit.datasheetId]);

  const loadingDetail = expanded && !detail;

  const wargearCount = useMemo(() =>
    unit.wargearSelections.filter(s => s.selected).length
  , [unit.wargearSelections]);

  const unitSize = selectedCost ? parseUnitSize(selectedCost.description) : 1;

  useEffect(() => {
    if (expanded) {
      filterWargear(unit.datasheetId, unit.wargearSelections, unitSize)
        .then(setFilteredWargear)
        .catch(() => setFilteredWargear([]));
    }
  }, [expanded, unit.datasheetId, unit.wargearSelections, unitSize]);

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

    onUpdate(index, { ...unit, wargearSelections: updatedSelections });
  };

  const handleWargearNotesChange = (optionLine: number, notes: string) => {
    const updatedSelections = unit.wargearSelections.map(s =>
      s.optionLine === optionLine ? { ...s, notes: notes || null } : s
    );
    onUpdate(index, { ...unit, wargearSelections: updatedSelections });
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
    expanded ? "expanded" : "",
  ].filter(Boolean).join(" ");

  const renderLeaderSelect = () => {
    if (displayMode === "inline") {
      if (attachedLeaderInfo) {
        return <span className="attached-leader-badge">+ {attachedLeaderInfo.name}</span>;
      }
      return null;
    }

    if (displayMode === "instance" && isCharacter) {
      return (
        <select
          className="unit-select"
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
                {ds?.name} #{unitIdx + 1}
              </option>
            ))}
        </select>
      );
    }

    if (isCharacter && attachableUnitsInArmy.length > 0) {
      return (
        <select
          className="unit-select"
          value={unit.attachedLeaderId ?? ""}
          onChange={(e) => onUpdate(index, { ...unit, attachedLeaderId: e.target.value || null })}
        >
          <option value="">No attachment</option>
          {attachableUnitsInArmy.map(({ index: unitIdx, ds }) => (
            <option key={`${ds!.id}-${unitIdx}`} value={ds!.id}>{getUnitDisplayName(ds, unitIdx)}</option>
          ))}
        </select>
      );
    }

    return null;
  };

  return (
    <tr className={rowClassName}>
      <td colSpan={8}>
        <div className="unit-card-builder">
          <div className="unit-card-header" onClick={() => setExpanded(!expanded)} style={{ cursor: 'pointer' }}>
            <span className="unit-expand-btn">
              {expanded ? "▼" : "▶"}
            </span>

            <div className="unit-card-title">
              {isGroupChild && <span className="group-connector">└─ </span>}
              <span className="unit-name-text">{datasheet?.name ?? unit.datasheetId}</span>
              {thisUnitNumber.total > 1 && <span className="unit-number">#{thisUnitNumber.num}</span>}
              {isGroupParent && <span className="leading-badge">leading</span>}
            </div>

            {isCharacter && !readOnly && (
              <button
                className={`warlord-btn ${isWarlord ? "active" : ""}`}
                onClick={(e) => { e.stopPropagation(); onSetWarlord(index); }}
                title={isWarlord ? "Warlord" : "Set as Warlord"}
              >
                ♛
              </button>
            )}
            {isWarlord && readOnly && <span className="warlord-badge">♛ Warlord</span>}

            {unit.enhancementId && (
              <span className="enhancement-pill">
                {enhancements.find(e => e.id === unit.enhancementId)?.name}
              </span>
            )}

            <span className="unit-cost-badge">{totalCost}pts</span>

            {!readOnly && (
              <div className="unit-card-actions" onClick={(e) => e.stopPropagation()}>
                <button className="btn-copy" onClick={() => onCopy(index)} title="Copy">⧉</button>
                <button className="btn-remove" onClick={() => onRemove(index)} title="Remove">×</button>
              </div>
            )}
          </div>

          <div className="unit-card-controls">
            {unitCosts.length > 1 && (
              <div className="control-group">
                <label>Size</label>
                {readOnly ? (
                  <span className="control-value">{selectedCost?.description}</span>
                ) : (
                  <select
                    className="unit-select"
                    value={unit.sizeOptionLine}
                    onChange={(e) => onUpdate(index, { ...unit, sizeOptionLine: Number(e.target.value) })}
                  >
                    {unitCosts.map((c) => (
                      <option key={c.line} value={c.line}>{c.description} ({c.cost}pts)</option>
                    ))}
                  </select>
                )}
              </div>
            )}

            {isCharacter && attachableUnitsInArmy.length > 0 && !readOnly && (
              <div className="control-group">
                <label>Attach to</label>
                {renderLeaderSelect()}
              </div>
            )}

            {unitOptions.length > 0 && !readOnly && (
              <div className="control-group">
                <label>Wargear</label>
                <span className="wargear-count">{wargearCount}/{unitOptions.length}</span>
              </div>
            )}
          </div>

          {expanded && (
            <div className="unit-card-expanded">
              {loadingDetail && <div className="loading">Loading stats...</div>}

              {detail && (
                <div className="unit-stats-preview">
                  {detail.profiles.length > 0 && (
                    <div className="stats-row">
                      {detail.profiles.map((p, i) => (
                        <div key={i} className="stat-line">
                          <span className="stat"><b>M</b>{p.movement}</span>
                          <span className="stat"><b>T</b>{p.toughness}</span>
                          <span className="stat"><b>SV</b>{p.save}{p.invulnerableSave && `/${p.invulnerableSave}`}</span>
                          <span className="stat"><b>W</b>{p.wounds}</span>
                          <span className="stat"><b>LD</b>{p.leadership}</span>
                          <span className="stat"><b>OC</b>{p.objectiveControl}</span>
                        </div>
                      ))}
                    </div>
                  )}

                  {filteredWargear.length > 0 && (
                    <div className="weapons-preview">
                      <h5>Weapons</h5>
                      <div className="weapons-list">
                        {filteredWargear.map((wq, i) => (
                          <div key={i} className="weapon-line">
                            <span className="weapon-name">{wq.quantity > 1 ? `${wq.quantity}x ` : ""}{wq.wargear.name}</span>
                            <span className="weapon-stat">{wq.wargear.attacks ? `A:${wq.wargear.attacks}` : ''}</span>
                            <span className="weapon-stat">{wq.wargear.ballisticSkill ? `BS:${wq.wargear.ballisticSkill}` : ''}</span>
                            <span className="weapon-stat">{wq.wargear.strength ? `S:${wq.wargear.strength}` : ''}</span>
                            <span className="weapon-stat">{wq.wargear.armorPenetration ? `AP:${wq.wargear.armorPenetration}` : ''}</span>
                            <span className="weapon-stat">{wq.wargear.damage ? `D:${wq.wargear.damage}` : ''}</span>
                            <span className="weapon-abilities">
                              {wq.wargear.description && <WeaponAbilityText text={wq.wargear.description} />}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}

              {isCharacter && enhancements.length > 0 && !readOnly && (
                <div className="enhancement-section">
                  <h5>Enhancement</h5>
                  <select
                    className="unit-select"
                    value={unit.enhancementId ?? ""}
                    onChange={(e) => onUpdate(index, { ...unit, enhancementId: e.target.value || null })}
                  >
                    <option value="">None</option>
                    {enhancements.map((e) => (
                      <option key={e.id} value={e.id}>{e.name} (+{e.cost}pts)</option>
                    ))}
                  </select>
                </div>
              )}

              {unitOptions.length > 0 && !readOnly && (
                <div className="wargear-options">
                  <h5>Wargear Options</h5>
                  {unitOptions.map((option) => {
                    const selection = getWargearSelection(option.line);
                    const isSelected = selection?.selected ?? false;
                    const choices = extractWargearChoices(option.description);
                    const hasChoices = choices && choices.length > 0;

                    return (
                      <div key={option.line} className="wargear-option">
                        <label className="wargear-checkbox">
                          <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={(e) => handleWargearSelectionChange(option.line, e.target.checked)}
                          />
                          <span dangerouslySetInnerHTML={{ __html: option.description }} />
                        </label>
                        {isSelected && hasChoices && (
                          <select
                            className="unit-select wargear-choice-select"
                            value={selection?.notes ?? ''}
                            onChange={(e) => handleWargearNotesChange(option.line, e.target.value)}
                          >
                            <option value="">Select wargear...</option>
                            {choices.map((choice, idx) => (
                              <option key={idx} value={choice}>{choice}</option>
                            ))}
                          </select>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}

              {wargearCount > 0 && readOnly && (
                <div className="wargear-options">
                  <h5>Selected Wargear</h5>
                  {unitOptions
                    .filter(option => getWargearSelection(option.line)?.selected)
                    .map((option) => {
                      const selection = getWargearSelection(option.line);
                      return (
                        <div key={option.line} className="wargear-option readonly">
                          <span dangerouslySetInnerHTML={{ __html: option.description }} />
                          {selection?.notes && <span className="wargear-choice">→ {selection.notes}</span>}
                        </div>
                      );
                    })}
                </div>
              )}

              {detail && detail.abilities.filter(a => a.name).length > 0 && (
                <div className="abilities-preview">
                  <h5>Abilities</h5>
                  <div className="abilities-list">
                    {detail.abilities.filter(a => a.name).map((a, i) => (
                      <div key={i} className="ability-line">
                        <strong>{a.name}</strong>
                        {a.description && <span dangerouslySetInnerHTML={{ __html: `: ${a.description}` }} />}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </td>
    </tr>
  );
}
