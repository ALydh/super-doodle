import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import type { DatasheetDetail } from "../types";
import { fetchDatasheetDetail } from "../api";
import { getFactionTheme } from "../factionTheme";
import { WeaponAbilityText } from "./WeaponAbilityText";
import { sanitizeHtml } from "../sanitize";
import styles from "./DatasheetDetailPage.module.css";

export function DatasheetDetailPage() {
  const { datasheetId } = useParams<{ datasheetId: string }>();
  const [detail, setDetail] = useState<DatasheetDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!datasheetId) return;
    fetchDatasheetDetail(datasheetId)
      .then(setDetail)
      .catch((e) => setError(e.message));
  }, [datasheetId]);

  if (error) return <div className="error-message">{error}</div>;
  if (!detail) return <div>Loading...</div>;

  const { datasheet, profiles, wargear, costs, keywords, abilities, stratagems, options } = detail;
  const factionTheme = getFactionTheme(datasheet.factionId);

  return (
    <div data-faction={factionTheme}>
      <h1 className={styles.name}>{datasheet.name}</h1>
      {datasheet.role && <p className={styles.role}>Role: {datasheet.role}</p>}

      {profiles.length > 0 && (
        <>
          <h2>Stats</h2>
          <table className={styles.statsTable}>
            <thead>
              <tr>
                <th>Name</th>
                <th>M</th>
                <th>T</th>
                <th>SV</th>
                <th>W</th>
                <th>LD</th>
                <th>OC</th>
              </tr>
            </thead>
            <tbody>
              {profiles.map((p, i) => (
                <tr key={i}>
                  <td>{p.name ?? datasheet.name}</td>
                  <td>{p.movement}</td>
                  <td>{p.toughness}</td>
                  <td>{p.save}</td>
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
                <div className={styles.statsCardName}>{p.name ?? datasheet.name}</div>
                <div className={styles.statsCardValues}>
                  <div className={styles.statItem}><span className={styles.statLabel}>M</span><span className={styles.statValue}>{p.movement}</span></div>
                  <div className={styles.statItem}><span className={styles.statLabel}>T</span><span className={styles.statValue}>{p.toughness}</span></div>
                  <div className={styles.statItem}><span className={styles.statLabel}>SV</span><span className={styles.statValue}>{p.save}</span></div>
                  <div className={styles.statItem}><span className={styles.statLabel}>W</span><span className={styles.statValue}>{p.wounds}</span></div>
                  <div className={styles.statItem}><span className={styles.statLabel}>LD</span><span className={styles.statValue}>{p.leadership}</span></div>
                  <div className={styles.statItem}><span className={styles.statLabel}>OC</span><span className={styles.statValue}>{p.objectiveControl}</span></div>
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      {wargear.length > 0 && (
        <>
          <h2>Weapons</h2>
          <table className={styles.weaponsTable}>
            <thead>
              <tr>
                <th>Name</th>
                <th>Range</th>
                <th>Type</th>
                <th>A</th>
                <th>BS/WS</th>
                <th>S</th>
                <th>AP</th>
                <th>D</th>
                <th>Abilities</th>
              </tr>
            </thead>
            <tbody>
              {wargear
                .filter((w) => w.name)
                .map((w, i) => (
                  <tr key={i}>
                    <td>{w.name}</td>
                    <td>{w.range ?? "-"}</td>
                    <td>{w.weaponType ?? "-"}</td>
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
            {wargear.filter((w) => w.name).map((w, i) => (
              <div key={i} className={styles.weaponCard}>
                <div className={styles.weaponCardHeader}>
                  <span className={styles.weaponCardName}>{w.name}</span>
                  <span className={styles.weaponCardRange}>
                    {w.range?.toLowerCase() === "melee" ? "Melee" : `Ranged: ${w.range ?? "-"}`}
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
        </>
      )}

      {options.length > 0 && (
        <>
          <h2>Wargear Options</h2>
          <ul className={styles.wargearOptionsList}>
            {options.map((o, i) => (
              <li key={i} dangerouslySetInnerHTML={{ __html: sanitizeHtml(o.description) }} />
            ))}
          </ul>
        </>
      )}

      {costs.length > 0 && (
        <>
          <h2>Point Costs</h2>
          <table className={styles.costsTable}>
            <thead>
              <tr>
                <th>Description</th>
                <th>Cost</th>
              </tr>
            </thead>
            <tbody>
              {costs.map((c, i) => (
                <tr key={i}>
                  <td>{c.description}</td>
                  <td className={styles.costValue}>{c.cost}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}

      {abilities.length > 0 && (
        <>
          <h2>Abilities</h2>
          <ul className={styles.abilitiesList}>
            {abilities
              .filter((a) => a.name)
              .map((a, i) => (
                <li key={i}>
                  <strong>{a.name}</strong>
                  {a.description && <p dangerouslySetInnerHTML={{ __html: sanitizeHtml(a.description) }} />}
                </li>
              ))}
          </ul>
        </>
      )}

      {keywords.length > 0 && (
        <>
          <h2>Keywords</h2>
          <div className={styles.keywordsList}>
            {keywords
              .filter((k) => k.keyword)
              .map((k, i) => (
                <span key={i} className={styles.keyword}>
                  {k.keyword}
                </span>
              ))}
          </div>
        </>
      )}

      {stratagems.length > 0 && (
        <>
          <h2>Unit Stratagems</h2>
          <ul className={styles.stratagemsList}>
            {stratagems.map((s) => (
              <li key={s.id}>
                <strong>{s.name}</strong>
                {s.cpCost !== null && <span> ({s.cpCost} CP)</span>}
                {s.phase && <span> - {s.phase}</span>}
                <p dangerouslySetInnerHTML={{ __html: sanitizeHtml(s.description) }} />
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}
