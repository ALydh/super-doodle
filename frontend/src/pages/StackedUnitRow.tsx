import { useState, useEffect, useRef } from "react";
import type { ArmyUnit, Datasheet, DatasheetDetail } from "../types";
import { fetchDatasheetDetail } from "../api";
import { WeaponAbilityText } from "./WeaponAbilityText";
import { useReferenceData } from "../context/ReferenceDataContext";
import { sanitizeHtml } from "../sanitize";
import styles from "./UnitRow.module.css";

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
  const isAllied = firstUnit.isAllied === true;
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
    <tr className={`${styles.row} ${styles.stackedRow} ${isAllied ? styles.allied : ""}`}>
      <td colSpan={8}>
        <div className={`${styles.card} ${styles.stackedCard}`}>
          <div className={styles.stackedShadow} />
          <div className={styles.stackedShadow} />
          <div className={styles.header} onClick={() => setExpanded(!expanded)} style={{ cursor: 'pointer' }}>
            <span className={styles.expandBtn}>
              {expanded ? "▼" : "▶"}
            </span>

            <div className={styles.title}>
              <span className={styles.nameText}>{datasheet?.name ?? firstUnit.datasheetId}</span>
              <span className={styles.stackedCount}>×{count}</span>
              {isAllied && <span className={styles.alliedBadge}>Allied</span>}
            </div>

            <span className={`${styles.costBadge} ${styles.stackedTotal}`}>{totalPoints}pts</span>

            {!readOnly && (
              <div className={styles.actions} onClick={(e) => e.stopPropagation()}>
                <button className={styles.btnCopy} onClick={handleCopyOne} title="Add another">+</button>
                <button className={styles.btnRemove} onClick={handleRemoveOne} title="Remove one">−</button>
              </div>
            )}
          </div>

          <div className={styles.controls}>
            {unitCosts.length > 1 && (
              <div className={styles.controlGroup}>
                <label>Size</label>
                {readOnly ? (
                  <span className={styles.controlValue}>{selectedCost?.description}</span>
                ) : (
                  <select
                    className={styles.unitSelect}
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
            <div className={styles.expanded}>
              {loadingDetail && <div className="loading">Loading stats...</div>}

              {detail && (
                <div className={styles.statsPreview}>
                  {detail.profiles.length > 0 && (
                    <div className={styles.statsRow}>
                      {detail.profiles.map((p, i) => (
                        <div key={i} className={styles.statLine}>
                          <span className={styles.stat}><b>M</b>{p.movement}</span>
                          <span className={styles.stat}><b>T</b>{p.toughness}</span>
                          <span className={styles.stat}><b>SV</b>{p.save}{p.invulnerableSave && `/${p.invulnerableSave}`}</span>
                          <span className={styles.stat}><b>W</b>{p.wounds}</span>
                          <span className={styles.stat}><b>LD</b>{p.leadership}</span>
                          <span className={styles.stat}><b>OC</b>{p.objectiveControl}</span>
                        </div>
                      ))}
                    </div>
                  )}

                  {detail.wargear.filter(w => w.name).length > 0 && (
                    <div>
                      <h5 className={styles.sectionHeading}>Weapons</h5>
                      <div className={styles.weaponsList}>
                        {detail.wargear.filter(w => w.name).map((w, i) => (
                          <div key={i} className={styles.weaponLine}>
                            <span className={styles.weaponName}>{w.name}</span>
                            <span className={styles.weaponStat}>{w.attacks ? `A:${w.attacks}` : ''}</span>
                            <span className={styles.weaponStat}>{w.ballisticSkill ? `BS:${w.ballisticSkill}` : ''}</span>
                            <span className={styles.weaponStat}>{w.strength ? `S:${w.strength}` : ''}</span>
                            <span className={styles.weaponStat}>{w.armorPenetration ? `AP:${w.armorPenetration}` : ''}</span>
                            <span className={styles.weaponStat}>{w.damage ? `D:${w.damage}` : ''}</span>
                            <span className={styles.weaponAbilities}>
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
                <div className={styles.abilitiesPreview}>
                  <h5 className={styles.sectionHeading}>Abilities</h5>
                  <div className={styles.abilitiesList}>
                    {detail.abilities.filter(a => a.name).map((a, i) => (
                      <div key={i} className={styles.abilityLine}>
                        <strong>{a.name}</strong>
                        {a.description && <span dangerouslySetInnerHTML={{ __html: sanitizeHtml(`: ${a.description}`) }} />}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {!readOnly && (
                <div className={styles.stackedUnitsList}>
                  <h5>Individual Units</h5>
                  {stackedUnits.map(({ index }, i) => (
                    <div key={index} className={styles.stackedUnitItem}>
                      <span>Unit #{i + 1}</span>
                      <span>{unitPoints}pts</span>
                      <button className={styles.btnRemoveSmall} onClick={() => onRemove(index)} title="Remove">×</button>
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
