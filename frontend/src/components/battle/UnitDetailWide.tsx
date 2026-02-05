import type { BattleUnitData } from "../../types";
import { WeaponAbilityText } from "../../pages/WeaponAbilityText";
import { sanitizeHtml } from "../../sanitize";
import styles from "./UnitDetail.module.css";

interface Props {
  data: BattleUnitData;
  isWarlord: boolean;
}

export function UnitDetailWide({ data, isWarlord }: Props) {
  const { datasheet, profiles, wargear, abilities, keywords, cost, enhancement } = data;

  const hasEnhancement = !!enhancement;
  const filteredAbilities = abilities.filter((a) => a.name);
  const hasAbilities = filteredAbilities.length > 0;
  const filteredKeywords = keywords.filter((k) => k.keyword);
  const hasKeywords = filteredKeywords.length > 0;
  const hasWeapons = wargear.length > 0;
  const hasRightColumn = hasEnhancement || hasAbilities;

  return (
    <div className={styles.wide}>
      <div className={styles.header}>
        <h3 className={styles.name}>
          {datasheet.name}
          {isWarlord && <span className={styles.warlordBadge}>Warlord</span>}
        </h3>
        {enhancement && (
          <span className={styles.enhancementPill}>{enhancement.name}</span>
        )}
        {cost && <span className={styles.cost}>{cost.cost}pts</span>}
      </div>

      {datasheet.role && (
        <div className={styles.role}>{datasheet.role}</div>
      )}

      {(profiles.length > 0 || hasWeapons || hasRightColumn || datasheet.legend) && (
        <div className={styles.wideColumns}>
          <div className={styles.wideLeft}>
            {datasheet.legend && (
              <div className={styles.legend}>{datasheet.legend}</div>
            )}
            {profiles.length > 0 && (
              <div className={styles.stats}>
                <h4>Stats</h4>
                <table className={styles.statsTable}>
                  <thead>
                    <tr>
                      <th>M</th>
                      <th>T</th>
                      <th>SV</th>
                      {profiles.some((p) => p.invulnerableSave) && <th>Inv</th>}
                      <th>W</th>
                      <th>LD</th>
                      <th>OC</th>
                    </tr>
                  </thead>
                  <tbody>
                    {profiles.map((p, i) => (
                      <tr key={i}>
                        <td>{p.movement}</td>
                        <td>{p.toughness}</td>
                        <td>{p.save}</td>
                        {profiles.some((pr) => pr.invulnerableSave) && (
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
                  {profiles.map((p, i) => (
                    <div key={i} className={styles.statsCard}>
                      {p.name && <div className={styles.statsCardName}>{p.name}</div>}
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
              </div>
            )}

            {hasWeapons && (
              <div className={styles.weapons}>
                <h4>Weapons</h4>
                <table className={styles.weaponsTable}>
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
                    {wargear.map((wq, i) => (
                      <tr key={i}>
                        <td className={styles.weaponName}>{wq.quantity > 1 ? `${wq.quantity}x ` : ""}{wq.wargear.name}</td>
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
                  {wargear.map((wq, i) => (
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

          {hasRightColumn && (
            <div className={styles.wideRight}>
              {hasEnhancement && (
                <div className={styles.enhancement}>
                  <h4>Enhancement</h4>
                  <div className={styles.enhancementDetail}>
                    <div className={styles.enhancementHeader}>
                      <strong>{enhancement.name}</strong>
                      <span className={styles.enhancementCost}>+{enhancement.cost}pts</span>
                    </div>
                    {enhancement.description && (
                      <p dangerouslySetInnerHTML={{ __html: sanitizeHtml(enhancement.description) }} />
                    )}
                  </div>
                </div>
              )}

              {hasAbilities && (
                <div className={styles.abilities}>
                  <h4>Abilities</h4>
                  <ul className={styles.abilitiesList}>
                    {filteredAbilities.map((a, i) => (
                      <li key={i}>
                        <strong>{a.name}</strong>
                        {a.description && <p dangerouslySetInnerHTML={{ __html: sanitizeHtml(a.description) }} />}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {hasKeywords && (
        <div className={styles.keywords}>
          <h4>Keywords</h4>
          <div className={styles.keywordsList}>
            {filteredKeywords.map((k, i) => (
              <span key={i} className={`${styles.keyword} ${k.isFactionKeyword ? styles.factionKeyword : ""}`}>
                {k.keyword}
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
