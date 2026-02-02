import { useState, useEffect, useRef } from "react";
import type { ArmyUnit, Datasheet, DatasheetDetail } from "../types";
import { fetchDatasheetDetail } from "../api";
import { WeaponAbilityText } from "./WeaponAbilityText";
import { useReferenceData } from "../context/ReferenceDataContext";
import { sanitizeHtml } from "../sanitize";

interface StackedUnit {
  unit: ArmyUnit;
  index: number;
}

interface Props {
  stackedUnits: StackedUnit[];
  datasheet: Datasheet | undefined;
  onUpdate: (index: number, unit: ArmyUnit) => void;
  onRemove: (index: number) => void;
  onCopy: (index: number) => void;
  readOnly?: boolean;
}

export function StackedUnitRow({
  stackedUnits,
  datasheet,
  onUpdate,
  onRemove,
  onCopy,
  readOnly = false,
}: Props) {
  const { costs } = useReferenceData();
  const [expanded, setExpanded] = useState(false);
  const [detail, setDetail] = useState<DatasheetDetail | null>(null);
  const fetchingRef = useRef(false);

  const firstUnit = stackedUnits[0].unit;
  const count = stackedUnits.length;
  const unitCosts = costs.filter((c) => c.datasheetId === firstUnit.datasheetId);
  const selectedCost = unitCosts.find((c) => c.line === firstUnit.sizeOptionLine);
  const unitPoints = selectedCost?.cost ?? 0;
  const totalPoints = unitPoints * count;

  useEffect(() => {
    if (expanded && !detail && !fetchingRef.current) {
      fetchingRef.current = true;
      fetchDatasheetDetail(firstUnit.datasheetId)
        .then(setDetail)
        .finally(() => { fetchingRef.current = false; });
    }
  }, [expanded, detail, firstUnit.datasheetId]);

  const loadingDetail = expanded && !detail;

  const handleRemoveOne = () => {
    const lastUnit = stackedUnits[stackedUnits.length - 1];
    onRemove(lastUnit.index);
  };

  const handleCopyOne = () => {
    const firstIdx = stackedUnits[0].index;
    onCopy(firstIdx);
  };

  return (
    <tr className="unit-row stacked-row">
      <td colSpan={8}>
        <div className="unit-card-builder stacked-card">
          <div className="stacked-card-shadow" />
          <div className="stacked-card-shadow" />
          <div className="unit-card-header" onClick={() => setExpanded(!expanded)} style={{ cursor: 'pointer' }}>
            <span className="unit-expand-btn">
              {expanded ? "▼" : "▶"}
            </span>

            <div className="unit-card-title">
              <span className="unit-name-text">{datasheet?.name ?? firstUnit.datasheetId}</span>
              <span className="stacked-count">×{count}</span>
            </div>

            <span className="unit-cost-badge stacked-total">{totalPoints}pts</span>

            {!readOnly && (
              <div className="unit-card-actions" onClick={(e) => e.stopPropagation()}>
                <button className="btn-copy" onClick={handleCopyOne} title="Add another">+</button>
                <button className="btn-remove" onClick={handleRemoveOne} title="Remove one">−</button>
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
                    value={firstUnit.sizeOptionLine}
                    onChange={(e) => {
                      const newLine = Number(e.target.value);
                      stackedUnits.forEach(({ unit, index }) => {
                        onUpdate(index, { ...unit, sizeOptionLine: newLine });
                      });
                    }}
                  >
                    {unitCosts.map((c) => (
                      <option key={c.line} value={c.line}>{c.description} ({c.cost}pts each)</option>
                    ))}
                  </select>
                )}
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

                  {detail.wargear.filter(w => w.name).length > 0 && (
                    <div className="weapons-preview">
                      <h5>Weapons</h5>
                      <div className="weapons-list">
                        {detail.wargear.filter(w => w.name).map((w, i) => (
                          <div key={i} className="weapon-line">
                            <span className="weapon-name">{w.name}</span>
                            <span className="weapon-stat">{w.attacks ? `A:${w.attacks}` : ''}</span>
                            <span className="weapon-stat">{w.ballisticSkill ? `BS:${w.ballisticSkill}` : ''}</span>
                            <span className="weapon-stat">{w.strength ? `S:${w.strength}` : ''}</span>
                            <span className="weapon-stat">{w.armorPenetration ? `AP:${w.armorPenetration}` : ''}</span>
                            <span className="weapon-stat">{w.damage ? `D:${w.damage}` : ''}</span>
                            <span className="weapon-abilities">
                              {w.description && <WeaponAbilityText text={w.description} />}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}

              {detail && detail.abilities.filter(a => a.name).length > 0 && (
                <div className="abilities-preview">
                  <h5>Abilities</h5>
                  <div className="abilities-list">
                    {detail.abilities.filter(a => a.name).map((a, i) => (
                      <div key={i} className="ability-line">
                        <strong>{a.name}</strong>
                        {a.description && <span dangerouslySetInnerHTML={{ __html: sanitizeHtml(`: ${a.description}`) }} />}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {!readOnly && (
                <div className="stacked-units-list">
                  <h5>Individual Units</h5>
                  {stackedUnits.map(({ index }, i) => (
                    <div key={index} className="stacked-unit-item">
                      <span>Unit #{i + 1}</span>
                      <span>{unitPoints}pts</span>
                      <button className="btn-remove-small" onClick={() => onRemove(index)} title="Remove">×</button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </td>
    </tr>
  );
}
