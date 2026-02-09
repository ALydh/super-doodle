import type { DatasheetDetail } from "../types";
import { WeaponAbilityText } from "../pages/WeaponAbilityText";
import { sanitizeHtml } from "../sanitize";
import styles from "./ExpandableUnitCard.module.css";
import sharedStyles from "../shared.module.css";

interface Props {
  detail: DatasheetDetail;
}

export function UnitCardDetail({ detail }: Props) {
  const filteredAbilities = detail.abilities.filter(a => a.name);
  const filteredKeywords = detail.keywords.filter(k => k.keyword);
  const filteredWargear = detail.wargear.filter(w => w.name);
  const hasRightColumn = filteredAbilities.length > 0;

  return (<>
    <div className={styles.wideColumns}>
      <div className={styles.wideLeft}>
        {detail.datasheet.legend && (
          <div className={styles.legend}>{detail.datasheet.legend}</div>
        )}
        {detail.profiles.length > 0 && (
          <div className={styles.statsSection}>
            <h4>Stats</h4>
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
                    {detail.profiles.some(pr => pr.invulnerableSave) && (
                      <td>{p.invulnerableSave ?? "-"}</td>
                    )}
                    <td>{p.wounds}</td>
                    <td>{p.leadership}</td>
                    <td>{p.objectiveControl}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className={styles.statsMobile}>
              {detail.profiles.map((p, i) => (
                <div key={i} className={styles.statsCardValues}>
                  <div className={styles.statItem}><span className={styles.statLabel}>M</span><span className={styles.statValue}>{p.movement}</span></div>
                  <div className={styles.statItem}><span className={styles.statLabel}>T</span><span className={styles.statValue}>{p.toughness}</span></div>
                  <div className={styles.statItem}><span className={styles.statLabel}>SV</span><span className={styles.statValue}>{p.save}</span></div>
                  {p.invulnerableSave && <div className={styles.statItem}><span className={styles.statLabel}>Inv</span><span className={styles.statValue}>{p.invulnerableSave}</span></div>}
                  <div className={styles.statItem}><span className={styles.statLabel}>W</span><span className={styles.statValue}>{p.wounds}</span></div>
                  <div className={styles.statItem}><span className={styles.statLabel}>LD</span><span className={styles.statValue}>{p.leadership}</span></div>
                  <div className={styles.statItem}><span className={styles.statLabel}>OC</span><span className={styles.statValue}>{p.objectiveControl}</span></div>
                </div>
              ))}
            </div>
          </div>
        )}

        {filteredWargear.length > 0 && (
          <div className={styles.weaponsSection}>
            <h4>Weapons</h4>
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
                {filteredWargear.map((w, i) => (
                  <tr key={i}>
                    <td>{w.name}</td>
                    <td>{w.range ?? "-"}</td>
                    <td>{w.attacks ?? "-"}</td>
                    <td>{w.ballisticSkill ?? "-"}</td>
                    <td>{w.strength ?? "-"}</td>
                    <td>{w.armorPenetration ?? "-"}</td>
                    <td>{w.damage ?? "-"}</td>
                    <td><WeaponAbilityText text={w.description} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className={styles.weaponsMobile}>
              {filteredWargear.map((w, i) => (
                <div key={i} className={styles.weaponCard}>
                  <div className={styles.weaponCardHeader}>
                    <span className={styles.weaponCardName}>{w.name}</span>
                    <span className={styles.weaponCardRange}>
                      {w.range?.toLowerCase() === "melee" ? "Melee" : w.range ?? "-"}
                    </span>
                  </div>
                  <div className={styles.weaponCardValues}>
                    <div className={styles.statItem}><span className={styles.statLabel}>A</span><span className={styles.statValue}>{w.attacks ?? "-"}</span></div>
                    <div className={styles.statItem}><span className={styles.statLabel}>BS/WS</span><span className={styles.statValue}>{w.ballisticSkill ?? "-"}</span></div>
                    <div className={styles.statItem}><span className={styles.statLabel}>S</span><span className={styles.statValue}>{w.strength ?? "-"}</span></div>
                    <div className={styles.statItem}><span className={styles.statLabel}>AP</span><span className={styles.statValue}>{w.armorPenetration ?? "-"}</span></div>
                    <div className={styles.statItem}><span className={styles.statLabel}>D</span><span className={styles.statValue}>{w.damage ?? "-"}</span></div>
                  </div>
                  {w.description && <div className={styles.weaponCardAbilities}><WeaponAbilityText text={w.description} /></div>}
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {hasRightColumn && (
        <div className={styles.wideRight}>
          {filteredAbilities.length > 0 && (
            <div className={styles.abilitiesSection}>
              <h4>Abilities</h4>
              <ul className={styles.abilitiesList}>
                {filteredAbilities.map((a, i) => (
                  <li key={i}>
                    <strong>{a.name}</strong>
                    {a.description && <span dangerouslySetInnerHTML={{ __html: sanitizeHtml(`: ${a.description}`) }} />}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>

    {filteredKeywords.length > 0 && (
      <div className={styles.keywordsSection}>
        <h4>Keywords</h4>
        <div className={sharedStyles.keywordsList}>
          {filteredKeywords.map((k, i) => (
            <span key={i} className={sharedStyles.keyword}>{k.keyword}</span>
          ))}
        </div>
      </div>
    )}
  </>);
}
