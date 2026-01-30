import { useState, useEffect, useRef } from "react";
import type { DatasheetDetail, Enhancement } from "../types";
import { fetchDatasheetDetail } from "../api";
import { WeaponAbilityText } from "../pages/WeaponAbilityText";

interface Props {
  datasheetId: string;
  datasheetName: string;
  points?: number;
  isExpanded: boolean;
  onToggle: () => void;
  isWarlord?: boolean;
  enhancement?: Enhancement | null;
}

export function ExpandableUnitCard({
  datasheetId,
  datasheetName,
  points,
  isExpanded,
  onToggle,
  isWarlord,
  enhancement,
}: Props) {
  const [detail, setDetail] = useState<DatasheetDetail | null>(null);
  const fetchingRef = useRef(false);

  useEffect(() => {
    if (isExpanded && !detail && !fetchingRef.current) {
      fetchingRef.current = true;
      fetchDatasheetDetail(datasheetId)
        .then(setDetail)
        .finally(() => { fetchingRef.current = false; });
    }
  }, [isExpanded, datasheetId, detail]);

  const loading = isExpanded && !detail;

  return (
    <div className={`expandable-unit-card ${isExpanded ? "expanded" : ""}`}>
      <button className="expandable-unit-header" onClick={onToggle}>
        <span className="expand-icon">{isExpanded ? "▼" : "▶"}</span>
        <span className="unit-card-name">
          {datasheetName}
          {isWarlord && <span className="warlord-badge">Warlord</span>}
        </span>
        {enhancement && (
          <span className="unit-card-enhancement">+ {enhancement.name}</span>
        )}
        {points !== undefined && points > 0 && (
          <span className="unit-card-points">{points}pts</span>
        )}
      </button>

      {isExpanded && (
        <div className="expandable-unit-content">
          {loading && <div className="loading">Loading...</div>}
          {detail && (
            <>
              {detail.profiles.length > 0 && (
                <div className="unit-stats-section">
                  <table className="stats-table compact">
                    <thead>
                      <tr>
                        <th>M</th>
                        <th>T</th>
                        <th>SV</th>
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
                          <td>{p.save}{p.invulnerableSave && `/${p.invulnerableSave}`}</td>
                          <td>{p.wounds}</td>
                          <td>{p.leadership}</td>
                          <td>{p.objectiveControl}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  <div className="stats-mobile">
                    {detail.profiles.map((p, i) => (
                      <div key={i} className="stats-card-values">
                        <div className="stat-item"><span className="stat-label">M</span><span className="stat-value">{p.movement}</span></div>
                        <div className="stat-item"><span className="stat-label">T</span><span className="stat-value">{p.toughness}</span></div>
                        <div className="stat-item"><span className="stat-label">SV</span><span className="stat-value">{p.save}{p.invulnerableSave && `/${p.invulnerableSave}`}</span></div>
                        <div className="stat-item"><span className="stat-label">W</span><span className="stat-value">{p.wounds}</span></div>
                        <div className="stat-item"><span className="stat-label">LD</span><span className="stat-value">{p.leadership}</span></div>
                        <div className="stat-item"><span className="stat-label">OC</span><span className="stat-value">{p.objectiveControl}</span></div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {detail.wargear.filter(w => w.name).length > 0 && (
                <div className="unit-weapons-section">
                  <h4>Weapons</h4>
                  <table className="weapons-table compact">
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
                      {detail.wargear.filter(w => w.name).map((w, i) => (
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
                  <div className="weapons-mobile">
                    {detail.wargear.filter(w => w.name).map((w, i) => (
                      <div key={i} className="weapon-card">
                        <div className="weapon-card-header">
                          <span className="weapon-card-name">{w.name}</span>
                          <span className="weapon-card-range">
                            {w.range?.toLowerCase() === "melee" ? "Melee" : w.range ?? "-"}
                          </span>
                        </div>
                        <div className="weapon-card-values">
                          <div className="stat-item"><span className="stat-label">A</span><span className="stat-value">{w.attacks ?? "-"}</span></div>
                          <div className="stat-item"><span className="stat-label">BS/WS</span><span className="stat-value">{w.ballisticSkill ?? "-"}</span></div>
                          <div className="stat-item"><span className="stat-label">S</span><span className="stat-value">{w.strength ?? "-"}</span></div>
                          <div className="stat-item"><span className="stat-label">AP</span><span className="stat-value">{w.armorPenetration ?? "-"}</span></div>
                          <div className="stat-item"><span className="stat-label">D</span><span className="stat-value">{w.damage ?? "-"}</span></div>
                        </div>
                        {w.description && <div className="weapon-card-abilities"><WeaponAbilityText text={w.description} /></div>}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {detail.abilities.filter(a => a.name).length > 0 && (
                <div className="unit-abilities-section">
                  <h4>Abilities</h4>
                  <ul className="abilities-list compact">
                    {detail.abilities.filter(a => a.name).map((a, i) => (
                      <li key={i}>
                        <strong>{a.name}</strong>
                        {a.description && <span dangerouslySetInnerHTML={{ __html: `: ${a.description}` }} />}
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {detail.keywords.filter(k => k.keyword).length > 0 && (
                <div className="unit-keywords-section">
                  <h4>Keywords</h4>
                  <div className="keywords-list">
                    {detail.keywords.filter(k => k.keyword).map((k, i) => (
                      <span key={i} className="keyword">{k.keyword}</span>
                    ))}
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      )}
    </div>
  );
}
