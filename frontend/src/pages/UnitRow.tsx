import { useState, useEffect, useMemo, useRef } from "react";
import type { ArmyUnit, Datasheet, WargearSelection, DatasheetDetail, WargearWithQuantity } from "../types";
import { fetchDatasheetDetail, filterWargear } from "../api";
import { WeaponAbilityText } from "./WeaponAbilityText";
import { useReferenceData } from "../context/ReferenceDataContext";
import { sanitizeHtml } from "../sanitize";
import { EnhancementSelector } from "../components/EnhancementSelector";
import { WargearSelector } from "../components/WargearSelector";
import { LeaderSlotsSection } from "../components/LeaderSlotsSection";
import styles from "./UnitRow.module.css";

function parseUnitSize(description: string): number {
  const match = description.match(/(\d+)\s*model/i);
  return match ? parseInt(match[1], 10) : 1;
}

interface Props {
  unit: ArmyUnit;
  index: number;
  datasheet: Datasheet | undefined;
  isWarlord: boolean;
  onUpdate: (index: number, unit: ArmyUnit) => void;
  onRemove: (index: number) => void;
  onCopy: (index: number) => void;
  onSetWarlord: (index: number) => void;
  allUnits?: ArmyUnit[];
  readOnly?: boolean;
}

export function UnitRow({
  unit, index, datasheet,
  isWarlord, onUpdate, onRemove, onCopy, onSetWarlord,
  allUnits = [],
  readOnly = false,
}: Props) {
  const { costs, enhancements, leaders, datasheets, options } = useReferenceData();
  const [expanded, setExpanded] = useState(false);
  const [detail, setDetail] = useState<DatasheetDetail | null>(null);
  const [filteredWargear, setFilteredWargear] = useState<WargearWithQuantity[]>([]);
  const fetchingRef = useRef(false);

  const unitCosts = costs.filter((c) => c.datasheetId === unit.datasheetId);
  const isCharacter = datasheet?.role === "Characters";
  const unitOptions = options.filter((o) => o.datasheetId === unit.datasheetId);

  const getUnitNumber = (unitIndex: number, datasheetId: string): { num: number; total: number } => {
    const sameTypeUnits = allUnits
      .map((u, i) => ({ datasheetId: u.datasheetId, index: i }))
      .filter(u => u.datasheetId === datasheetId);
    const num = sameTypeUnits.findIndex(u => u.index === unitIndex) + 1;
    return { num, total: sameTypeUnits.length };
  };

  const thisUnitNumber = getUnitNumber(index, unit.datasheetId);

  const attachedLeaderName = !isCharacter
    ? allUnits
        .filter(u => u.attachedToUnitIndex === index)
        .map(u => datasheets.find(d => d.id === u.datasheetId)?.name)
        .find(Boolean) ?? null
    : null;

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
      filterWargear(unit.datasheetId, unit.wargearSelections, unitSize, unit.sizeOptionLine)
        .then(setFilteredWargear)
        .catch(() => setFilteredWargear([]));
    }
  }, [expanded, unit.datasheetId, unit.wargearSelections, unitSize, unit.sizeOptionLine]);

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

  const isAllied = unit.isAllied === true;

  const rowClassName = [
    styles.row,
    isAllied ? styles.allied : "",
  ].filter(Boolean).join(" ");

  return (
    <tr className={rowClassName}>
      <td colSpan={8}>
        <div className={styles.card}>
          <div
            className={styles.header}
            role="button"
            tabIndex={0}
            onClick={() => setExpanded(!expanded)}
            onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); setExpanded(!expanded); }}}
            aria-expanded={expanded}
            aria-label={`${datasheet?.name ?? unit.datasheetId}, ${expanded ? "collapse" : "expand"} details`}
          >
            <span className={styles.expandBtn} aria-hidden="true">
              {expanded ? "▼" : "▶"}
            </span>

            <div className={styles.title}>
              <span className={styles.nameText}>{datasheet?.name ?? unit.datasheetId}</span>
              {thisUnitNumber.total > 1 && <span className={styles.unitNumber}>#{thisUnitNumber.num}</span>}
              {isAllied && <span className={styles.alliedBadge}>Allied</span>}
              {attachedLeaderName && <span className={styles.leaderAttachedPill}>{attachedLeaderName}</span>}
            </div>

            {isCharacter && !readOnly && !isAllied && (
              <button
                type="button"
                className={`${styles.warlordBtn} ${isWarlord ? styles.active : ""}`}
                onClick={(e) => { e.stopPropagation(); onSetWarlord(index); }}
                title={isWarlord ? "Warlord" : "Set as Warlord"}
                aria-label={isWarlord ? "Warlord" : "Set as Warlord"}
              >
                ♛
              </button>
            )}
            {isWarlord && readOnly && <span className={styles.warlordBadge}>♛ Warlord</span>}

            {unit.enhancementId && (
              <span className={styles.enhancementPill}>
                {enhancements.find(e => e.id === unit.enhancementId)?.name}
              </span>
            )}

            <span className={styles.costBadge}>{totalCost}pts</span>

            {!readOnly && (
              <div className={styles.actions} onClick={(e) => e.stopPropagation()}>
                <button type="button" className={styles.btnCopy} onClick={() => onCopy(index)} title="Copy unit" aria-label="Copy unit">⧉</button>
                <button type="button" className={styles.btnRemove} onClick={() => onRemove(index)} title="Remove unit" aria-label="Remove unit">×</button>
              </div>
            )}
          </div>

          <div className={styles.controls}>
            {isCharacter && unit.attachedLeaderId && (
              <div className={styles.controlGroup}>
                <label>Leading</label>
                <span className={styles.leadingPill}>
                  {datasheets.find(d => d.id === unit.attachedLeaderId)?.name ?? "Unit"}
                </span>
              </div>
            )}

            {!isCharacter && !readOnly && (
              <LeaderSlotsSection
                unitDatasheetId={unit.datasheetId}
                unitIndex={index}
                allUnits={allUnits}
                datasheets={datasheets}
                leaders={leaders}
                onUpdate={onUpdate}
              />
            )}

            {unitOptions.length > 0 && !readOnly && (
              <div className={styles.controlGroup}>
                <label>Wargear</label>
                <span className={styles.wargearCount}>{wargearCount}/{unitOptions.length}</span>
              </div>
            )}

            {unitCosts.length > 1 && (
              <div className={styles.controlGroup}>
                <label>Size</label>
                {readOnly ? (
                  <span className={styles.controlValue}>{selectedCost?.description}</span>
                ) : (
                  <select
                    className={styles.unitSelect}
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

                  {filteredWargear.length > 0 && (
                    <div>
                      <h5 className={styles.sectionHeading}>Weapons</h5>
                      <div className={styles.weaponsList}>
                        {filteredWargear.map((wq, i) => (
                          <div key={i} className={styles.weaponLine}>
                            <span className={styles.weaponName}>{wq.quantity > 1 ? `${wq.quantity}x ` : ""}{wq.wargear.name}</span>
                            <span className={styles.weaponStat}>{wq.wargear.attacks ? `A:${wq.wargear.attacks}` : ''}</span>
                            <span className={styles.weaponStat}>{wq.wargear.ballisticSkill ? `BS:${wq.wargear.ballisticSkill}` : ''}</span>
                            <span className={styles.weaponStat}>{wq.wargear.strength ? `S:${wq.wargear.strength}` : ''}</span>
                            <span className={styles.weaponStat}>{wq.wargear.armorPenetration ? `AP:${wq.wargear.armorPenetration}` : ''}</span>
                            <span className={styles.weaponStat}>{wq.wargear.damage ? `D:${wq.wargear.damage}` : ''}</span>
                            <span className={styles.weaponAbilities}>
                              {wq.wargear.description && <WeaponAbilityText text={wq.wargear.description} />}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}

              {isCharacter && enhancements.length > 0 && !readOnly && !isAllied && (
                <div className={styles.enhancementSection}>
                  <h5 className={styles.sectionHeading}>Enhancement</h5>
                  <EnhancementSelector
                    enhancements={enhancements}
                    selectedId={unit.enhancementId}
                    onSelect={(id) => onUpdate(index, { ...unit, enhancementId: id })}
                  />
                </div>
              )}

              {unitOptions.length > 0 && !readOnly && (
                <div className={styles.wargearOptions}>
                  <h5 className={styles.sectionHeading}>Wargear Options</h5>
                  <WargearSelector
                    options={unitOptions}
                    selections={unit.wargearSelections}
                    onSelectionChange={handleWargearSelectionChange}
                    onNotesChange={handleWargearNotesChange}
                    extractChoices={extractWargearChoices}
                  />
                </div>
              )}

              {wargearCount > 0 && readOnly && (
                <div className={styles.wargearOptions}>
                  <h5 className={styles.sectionHeading}>Selected Wargear</h5>
                  {unitOptions
                    .filter(option => getWargearSelection(option.line)?.selected)
                    .map((option) => {
                      const selection = getWargearSelection(option.line);
                      return (
                        <div key={option.line} className={styles.wargearOptionReadonly}>
                          <span dangerouslySetInnerHTML={{ __html: sanitizeHtml(option.description) }} />
                          {selection?.notes && <span className={styles.wargearChoice}>→ {selection.notes}</span>}
                        </div>
                      );
                    })}
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
            </div>
          )}
        </div>
      </td>
    </tr>
  );
}
