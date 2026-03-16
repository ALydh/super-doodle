import { useState } from "react";
import type { ArmyUnit, DatasheetDetail, WargearWithQuantity, Enhancement, DatasheetOption, WargearSelection } from "../types";
import type { WargearOptionType } from "../components/WargearSelector";
import { WeaponAbilityText } from "./WeaponAbilityText";
import { sanitizeHtml } from "../sanitize";
import { EnhancementSelector } from "../components/EnhancementSelector";
import { WargearSelector } from "../components/WargearSelector";
import styles from "./UnitRow.module.css";
import sharedStyles from "../shared.module.css";

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
  onSelectionChange: (optionLine: number, selected: boolean, initialNotes?: string) => void;
  onNotesChange: (optionLine: number, notes: string) => void;
  extractWargearOption: (description: string) => WargearOptionType | null;
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
  extractWargearOption,
  getWargearSelection,
}: Props) {
  const [expandedCore, setExpandedCore] = useState<number | null>(null);
  return (
    <div className={styles.expanded}>
      {loadingDetail && <div className="loading">Loading stats...</div>}

      {detail && (
        <div className={styles.statsPreview}>
          {detail.profiles.length > 0 && (
            <>
              <h5 className={styles.sectionHeading}>Stats</h5>
              <table className={`${sharedStyles.statsTable} ${styles.statsTable}`}>
                <thead>
                  <tr>
                    <th>M</th>
                    <th>T</th>
                    <th>SV</th>
                    {detail.profiles.some(p => p.invulnerableSave) && <th>Inv</th>}
                    <th>W</th>
                    <th>LD</th>
                    <th>OC</th>
                  </tr>
                </thead>
                <tbody>
                  {detail.profiles.map((p, i) => (
                    <tr key={i}>
                      <td>{p.movement}</td>
                      <td>{p.toughness}</td>
                      <td>{p.save}</td>
                      {detail.profiles.some(pr => pr.invulnerableSave) && <td>{p.invulnerableSave ?? "-"}</td>}
                      <td>{p.wounds}</td>
                      <td>{p.leadership}</td>
                      <td>{p.objectiveControl}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className={styles.statsMobile}>
                {detail.profiles.map((p, i) => (
                  <div key={i} className={styles.statsCard}>
                    {p.name && detail.profiles.length > 1 && <div className={styles.statsCardName}>{p.name}</div>}
                    <div className={styles.statsCardValues}>
                      <div className={styles.statItem}><span className={styles.statLabel}>M</span><span className={styles.statValue}>{p.movement}</span></div>
                      <div className={styles.statItem}><span className={styles.statLabel}>T</span><span className={styles.statValue}>{p.toughness}</span></div>
                      <div className={styles.statItem}><span className={styles.statLabel}>SV</span><span className={styles.statValue}>{p.save}</span></div>
                      {p.invulnerableSave && <div className={styles.statItem}><span className={styles.statLabel}>Inv</span><span className={styles.statValue}>{p.invulnerableSave}</span></div>}
                      <div className={styles.statItem}><span className={styles.statLabel}>W</span><span className={styles.statValue}>{p.wounds}</span></div>
                      <div className={styles.statItem}><span className={styles.statLabel}>LD</span><span className={styles.statValue}>{p.leadership}</span></div>
                      <div className={styles.statItem}><span className={styles.statLabel}>OC</span><span className={styles.statValue}>{p.objectiveControl}</span></div>
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}

          {filteredWargear.length > 0 && (
            <div>
              <h5 className={styles.sectionHeading}>Weapons</h5>
              <table className={`${sharedStyles.weaponsTable} ${styles.weaponsTable}`}>
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Range</th>
                    <th>A</th>
                    <th>BS/WS</th>
                    <th>S</th>
                    <th>AP</th>
                    <th>D</th>
                    <th>Abilities</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredWargear.map((wq, i) => (
                    <tr key={i}>
                      <td>{wq.quantity > 1 ? `${wq.quantity}x ` : ""}{wq.wargear.name}</td>
                      <td>{wq.wargear.range ?? "-"}</td>
                      <td>{wq.wargear.attacks ?? "-"}</td>
                      <td>{wq.wargear.ballisticSkill ?? "-"}</td>
                      <td>{wq.wargear.strength ?? "-"}</td>
                      <td>{wq.wargear.armorPenetration ?? "-"}</td>
                      <td>{wq.wargear.damage ?? "-"}</td>
                      <td><WeaponAbilityText text={wq.wargear.description} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className={styles.weaponsMobile}>
                {filteredWargear.map((wq, i) => (
                  <div key={i} className={styles.weaponCard}>
                    <div className={styles.weaponCardHeader}>
                      <span className={styles.weaponCardName}>{wq.quantity > 1 ? `${wq.quantity}x ` : ""}{wq.wargear.name}</span>
                      <span className={styles.weaponCardRange}>
                        {wq.wargear.range?.toLowerCase() === "melee" ? "Melee" : wq.wargear.range ?? "-"}
                      </span>
                    </div>
                    <div className={styles.weaponCardValues}>
                      <div className={styles.statItem}><span className={styles.statLabel}>A</span><span className={styles.statValue}>{wq.wargear.attacks ?? "-"}</span></div>
                      <div className={styles.statItem}><span className={styles.statLabel}>BS/WS</span><span className={styles.statValue}>{wq.wargear.ballisticSkill ?? "-"}</span></div>
                      <div className={styles.statItem}><span className={styles.statLabel}>S</span><span className={styles.statValue}>{wq.wargear.strength ?? "-"}</span></div>
                      <div className={styles.statItem}><span className={styles.statLabel}>AP</span><span className={styles.statValue}>{wq.wargear.armorPenetration ?? "-"}</span></div>
                      <div className={styles.statItem}><span className={styles.statLabel}>D</span><span className={styles.statValue}>{wq.wargear.damage ?? "-"}</span></div>
                    </div>
                    {wq.wargear.description && <div className={styles.weaponCardAbilities}><WeaponAbilityText text={wq.wargear.description} /></div>}
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

      {unitOptions.some(o => o.description !== "None") && !readOnly && (
        <div className={styles.wargearOptions}>
          <h5 className={styles.sectionHeading}>Wargear Options</h5>
          <WargearSelector
            options={unitOptions}
            selections={unit.wargearSelections}
            onSelectionChange={onSelectionChange}
            onNotesChange={onNotesChange}
            extractOption={extractWargearOption}
          />
        </div>
      )}

      {wargearCount > 0 && readOnly && (
        <div className={styles.wargearOptions}>
          <h5 className={styles.sectionHeading}>Selected Wargear</h5>
          {unitOptions
            .filter(option => option.description !== "None" && getWargearSelection(option.line)?.selected)
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
        const other = named.filter(a => a.abilityType !== "Core" && a.abilityType !== "Faction" && a.abilityType !== "Wargear profile");
        return (
          <div className={styles.abilitiesPreview}>
            <h5 className={styles.sectionHeading}>Abilities</h5>
            {core.length > 0 && (
              <>
                <div className={styles.coreAbilitiesPills}>
                  {core.map((a, i) => (
                    <span
                      key={i}
                      className={`${styles.coreAbilityPill} ${styles.coreAbilityPillClickable} ${expandedCore === i ? styles.coreAbilityPillActive : ""}`}
                      onClick={() => setExpandedCore(expandedCore === i ? null : i)}
                    >{a.name}</span>
                  ))}
                </div>
                {expandedCore !== null && core[expandedCore]?.description && (
                  <div className={styles.coreAbilityExpanded} dangerouslySetInnerHTML={{ __html: sanitizeHtml(core[expandedCore].description!) }} />
                )}
              </>
            )}
            {other.length > 0 && (
              <div className={styles.abilitiesList}>
                {other.map((a, i) => (
                  <div key={i} className={styles.abilityLine}>
                    <strong>{a.name}</strong>
                    {a.description && <p dangerouslySetInnerHTML={{ __html: sanitizeHtml(a.description) }} />}
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
