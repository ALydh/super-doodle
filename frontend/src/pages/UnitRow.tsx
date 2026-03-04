import { useState, useEffect, useMemo, useRef } from "react";
import type { ArmyUnit, Datasheet, ModelProfile, WargearSelection, DatasheetDetail, WargearWithQuantity } from "../types";
import { fetchDatasheetDetail, filterWargear } from "../api";
import { useReferenceData } from "../context/ReferenceDataContext";
import { LeaderSlotsSection } from "../components/LeaderSlotsSection";
import { UnitRowExpanded } from "./UnitRowExpanded";
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
  profiles?: ModelProfile[];
}

export function UnitRow({
  unit, index, datasheet,
  isWarlord, onUpdate, onRemove, onCopy, onSetWarlord,
  allUnits = [],
  readOnly = false,
  profiles = [],
}: Props) {
  const { costs, enhancements, leaders, datasheets, options } = useReferenceData();
  const [expanded, setExpanded] = useState(false);
  const [detail, setDetail] = useState<DatasheetDetail | null>(null);
  const [filteredWargear, setFilteredWargear] = useState<WargearWithQuantity[]>([]);
  const fetchingRef = useRef(false);

  const unitCosts = costs.filter((c) => c.datasheetId === unit.datasheetId);
  const isCharacter = datasheet?.role === "Characters";
  const unitOptions = options.filter((o) => o.datasheetId === unit.datasheetId);

  const attachedLeaderName = !isCharacter
    ? allUnits
        .filter(u => u.attachedToUnitIndex === index)
        .map(u => datasheets.find(d => d.id === u.datasheetId)?.name)
        .find(Boolean) ?? null
    : null;

  const selectedCost = unitCosts.find((c) => c.line === unit.sizeOptionLine);
  const enhancementObj = unit.enhancementId ? enhancements.find((e) => e.id === unit.enhancementId) : null;
  const enhancementCost = enhancementObj?.cost ?? 0;
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

  const realWargearOptions = useMemo(() =>
    unitOptions.filter(o => o.description !== "None")
  , [unitOptions]);

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
    return liMatches.map(li => li.replace(/<[^>]*>/g, '').trim()).filter(choice => choice.length > 0);
  };

  const isAllied = unit.isAllied === true;
  const displayProfile = (detail?.profiles[0]) ?? profiles[0] ?? null;

  return (
    <div className={`${styles.card} ${isAllied ? styles.allied : ""}`}>
      <button
        className={styles.header}
        onClick={() => setExpanded(!expanded)}
        aria-expanded={expanded}
        aria-label={`${datasheet?.name ?? unit.datasheetId}, ${expanded ? "collapse" : "expand"} details`}
      >
        <span className={styles.expandIcon} aria-hidden="true">
          {expanded ? "▼" : "▶"}
        </span>

        <span className={styles.name}>
          {datasheet?.name ?? unit.datasheetId}
          {isWarlord && !readOnly && (
            <span
              className={styles.warlordBadge}
              onClick={(e) => { e.stopPropagation(); onSetWarlord(index); }}
              role="button"
              title="Warlord"
            >★</span>
          )}
          {isWarlord && readOnly && <span className={styles.warlordBadge}>★</span>}
          {!isWarlord && !readOnly && isCharacter && !isAllied && (
            <span
              className={styles.warlordBtnInline}
              onClick={(e) => { e.stopPropagation(); onSetWarlord(index); }}
              role="button"
              title="Set as Warlord"
            >★</span>
          )}
          {enhancementObj && (
            <>
              <span className={styles.enhancementDot}>●</span>
              <span className={styles.enhancementLabel}>{enhancementObj.name}</span>
            </>
          )}
          {isAllied && <span className={styles.alliedBadge}>Allied</span>}
          {attachedLeaderName && <span className={styles.leaderAttachedPill}>{attachedLeaderName}</span>}
          {isCharacter && unit.attachedLeaderId && (
            <span className={styles.leaderAttachedPill}>
              {datasheets.find(d => d.id === unit.attachedLeaderId)?.name ?? "Unit"}
            </span>
          )}
        </span>

        <span className={styles.stats}>
          {displayProfile && (
            <>
              <span className={styles.statPill}>M{displayProfile.movement}</span>
              <span className={styles.statPill}>T{displayProfile.toughness}</span>
              <span className={styles.statPill}>W{displayProfile.wounds}</span>
              <span className={styles.statPill}>SV{displayProfile.save}</span>
              {displayProfile.invulnerableSave && (
                <span className={styles.statPill}>Inv{displayProfile.invulnerableSave}</span>
              )}
              <span className={styles.statPill}>LD{displayProfile.leadership}</span>
              <span className={styles.statPill}>OC{displayProfile.objectiveControl}</span>
            </>
          )}
        </span>

        <span className={styles.cost}>{totalCost}pts</span>

        {!readOnly && (
          <div className={styles.actions} onClick={(e) => e.stopPropagation()}>
            <button type="button" className={styles.btnCopy} onClick={() => onCopy(index)} title="Copy unit" aria-label="Copy unit">⧉</button>
            <button type="button" className={styles.btnRemove} onClick={() => onRemove(index)} title="Remove unit" aria-label="Remove unit">×</button>
          </div>
        )}
      </button>

      {expanded && (
        <div>
          {!readOnly && (
            <div className={styles.controls}>
              {!isCharacter && (
                <LeaderSlotsSection
                  unitDatasheetId={unit.datasheetId}
                  unitIndex={index}
                  allUnits={allUnits}
                  datasheets={datasheets}
                  leaders={leaders}
                  onUpdate={onUpdate}
                />
              )}
              {unitCosts.length > 1 && (
                <div className={styles.controlGroup}>
                  <label>Size</label>
                  <select
                    className={styles.unitSelect}
                    value={unit.sizeOptionLine}
                    onChange={(e) => onUpdate(index, { ...unit, sizeOptionLine: Number(e.target.value) })}
                  >
                    {unitCosts.map((c) => (
                      <option key={c.line} value={c.line}>{c.description} ({c.cost}pts)</option>
                    ))}
                  </select>
                </div>
              )}
              {realWargearOptions.length > 0 && (
                <div className={styles.controlGroup}>
                  <label>Wargear</label>
                  <span className={styles.wargearCount}>{wargearCount}/{realWargearOptions.length}</span>
                </div>
              )}
            </div>
          )}
          {readOnly && unitCosts.length > 1 && (
            <div className={styles.controls}>
              <div className={styles.controlGroup}>
                <label>Size</label>
                <span className={styles.controlValue}>{selectedCost?.description}</span>
              </div>
            </div>
          )}
          <UnitRowExpanded
            detail={detail}
            loadingDetail={loadingDetail}
            filteredWargear={filteredWargear}
            unit={unit}
            index={index}
            enhancements={enhancements}
            unitOptions={unitOptions}
            isCharacter={isCharacter ?? false}
            isAllied={isAllied}
            readOnly={readOnly}
            wargearCount={wargearCount}
            onUpdate={onUpdate}
            onSelectionChange={handleWargearSelectionChange}
            onNotesChange={handleWargearNotesChange}
            extractWargearChoices={extractWargearChoices}
            getWargearSelection={getWargearSelection}
          />
        </div>
      )}
    </div>
  );
}
