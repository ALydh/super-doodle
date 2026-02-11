import type { ArmyUnit, DatasheetDetail, WargearWithQuantity, Enhancement, DatasheetOption, WargearSelection } from "../types";
import { WeaponAbilityText } from "./WeaponAbilityText";
import { sanitizeHtml } from "../sanitize";
import { EnhancementSelector } from "../components/EnhancementSelector";
import { WargearSelector } from "../components/WargearSelector";
import styles from "./UnitRow.module.css";

interface Props {
  detail: DatasheetDetail | null;
  loadingDetail: boolean;
  filteredWargear: WargearWithQuantity[];
  unit: ArmyUnit;
  index: number;
  enhancements: Enhancement[];
  unitOptions: DatasheetOption[];
  isCharacter: boolean;
  isAllied: boolean;
  readOnly: boolean;
  wargearCount: number;
  onUpdate: (index: number, unit: ArmyUnit) => void;
  onSelectionChange: (optionLine: number, selected: boolean) => void;
  onNotesChange: (optionLine: number, notes: string) => void;
  extractWargearChoices: (description: string) => string[] | null;
  getWargearSelection: (optionLine: number) => WargearSelection | undefined;
}

export function UnitRowExpanded({
  detail,
  loadingDetail,
  filteredWargear,
  unit,
  index,
  enhancements,
  unitOptions,
  isCharacter,
  isAllied,
  readOnly,
  wargearCount,
  onUpdate,
  onSelectionChange,
  onNotesChange,
  extractWargearChoices,
  getWargearSelection,
}: Props) {
  return (
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
            onSelectionChange={onSelectionChange}
            onNotesChange={onNotesChange}
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

      {detail && detail.abilities.filter(a => a.name).length > 0 && (() => {
        const named = detail.abilities.filter(a => a.name);
        const core = named.filter(a => a.abilityType === "Core");
        const other = named.filter(a => a.abilityType !== "Core");
        return (
          <div className={styles.abilitiesPreview}>
            <h5 className={styles.sectionHeading}>Abilities</h5>
            {core.length > 0 && (
              <div className={styles.coreAbilitiesPills}>
                {core.map((a, i) => (
                  <span key={i} className={styles.coreAbilityPill}>{a.name}</span>
                ))}
              </div>
            )}
            {other.length > 0 && (
              <div className={styles.abilitiesList}>
                {other.map((a, i) => (
                  <div key={i} className={styles.abilityLine}>
                    <strong>{a.name}</strong>
                    {a.description && <span dangerouslySetInnerHTML={{ __html: sanitizeHtml(`: ${a.description}`) }} />}
                  </div>
                ))}
              </div>
            )}
          </div>
        );
      })()}
    </div>
  );
}
