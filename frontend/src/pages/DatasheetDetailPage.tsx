import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import type { DatasheetDetail } from "../types";
import { fetchDatasheetDetail } from "../api";
import { getFactionTheme } from "../factionTheme";

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
      {datasheet.factionId && (
        <Link to={`/factions/${datasheet.factionId}`} className="back-link">
          &larr; Back to Datasheets
        </Link>
      )}
      <h1 className="unit-name">{datasheet.name}</h1>
      {datasheet.role && <p className="unit-role">Role: {datasheet.role}</p>}

      {profiles.length > 0 && (
        <>
          <h2>Stats</h2>
          <table className="stats-table">
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
                <tr key={i} className="stats-row">
                  <td>{p.name ?? datasheet.name}</td>
                  <td className="stat-m">{p.movement}</td>
                  <td className="stat-t">{p.toughness}</td>
                  <td className="stat-sv">{p.save}</td>
                  <td className="stat-w">{p.wounds}</td>
                  <td className="stat-ld">{p.leadership}</td>
                  <td className="stat-oc">{p.objectiveControl}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}

      {wargear.length > 0 && (
        <>
          <h2>Weapons</h2>
          <table className="weapons-table">
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
                  <tr key={i} className="weapon-row">
                    <td>{w.name}</td>
                    <td>{w.range ?? "-"}</td>
                    <td>{w.weaponType ?? "-"}</td>
                    <td>{w.attacks ?? "-"}</td>
                    <td>{w.ballisticSkill ?? "-"}</td>
                    <td>{w.strength ?? "-"}</td>
                    <td>{w.armorPenetration ?? "-"}</td>
                    <td>{w.damage ?? "-"}</td>
                    <td className="weapon-abilities" dangerouslySetInnerHTML={{ __html: w.description ?? "-" }} />
                  </tr>
                ))}
            </tbody>
          </table>
        </>
      )}

      {options.length > 0 && (
        <>
          <h2>Wargear Options</h2>
          <ul className="wargear-options-list">
            {options.map((o, i) => (
              <li key={i} className="wargear-option-item" dangerouslySetInnerHTML={{ __html: o.description }} />
            ))}
          </ul>
        </>
      )}

      {costs.length > 0 && (
        <>
          <h2>Point Costs</h2>
          <table className="costs-table">
            <thead>
              <tr>
                <th>Description</th>
                <th>Cost</th>
              </tr>
            </thead>
            <tbody>
              {costs.map((c, i) => (
                <tr key={i} className="cost-row">
                  <td>{c.description}</td>
                  <td className="cost-value">{c.cost}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}

      {abilities.length > 0 && (
        <>
          <h2>Abilities</h2>
          <ul className="abilities-list">
            {abilities
              .filter((a) => a.name)
              .map((a, i) => (
                <li key={i} className="ability-item">
                  <strong>{a.name}</strong>
                  {a.abilityType && <span> ({a.abilityType})</span>}
                  {a.description && <p dangerouslySetInnerHTML={{ __html: a.description }} />}
                </li>
              ))}
          </ul>
        </>
      )}

      {stratagems.length > 0 && (
        <>
          <h2>Unit Stratagems</h2>
          <ul className="unit-stratagems-list">
            {stratagems.map((s) => (
              <li key={s.id} className="unit-stratagem-item">
                <strong>{s.name}</strong>
                {s.cpCost !== null && <span> ({s.cpCost} CP)</span>}
                {s.phase && <span> - {s.phase}</span>}
                <p dangerouslySetInnerHTML={{ __html: s.description }} />
              </li>
            ))}
          </ul>
        </>
      )}

      {keywords.length > 0 && (
        <>
          <h2>Keywords</h2>
          <div className="keywords-list">
            {keywords
              .filter((k) => k.keyword)
              .map((k, i) => (
                <span key={i} className="keyword">
                  {k.keyword}
                </span>
              ))}
          </div>
        </>
      )}
    </div>
  );
}
