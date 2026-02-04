import type { BattleUnitData } from "../../types";
import { WeaponAbilityText } from "../../pages/WeaponAbilityText";
import { sanitizeHtml } from "../../sanitize";

interface Props {
  data: BattleUnitData;
  isWarlord: boolean;
}

export function UnitDetail({ data, isWarlord }: Props) {
  const { datasheet, profiles, wargear, abilities, keywords, cost, enhancement } = data;

  return (
    <div className="unit-detail">
      <div className="unit-detail-header">
        <h3 className="unit-detail-name">
          {datasheet.name}
          {isWarlord && <span className="warlord-badge">Warlord</span>}
        </h3>
        {enhancement && (
          <span className="enhancement-pill">{enhancement.name}</span>
        )}
        {cost && <span className="unit-detail-cost">{cost.cost}pts</span>}
      </div>

      {datasheet.role && (
        <div className="unit-detail-role">{datasheet.role}</div>
      )}

      {profiles.length > 0 && (
        <div className="unit-detail-stats">
          <table className="stats-table compact">
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
          <div className="stats-mobile">
            {profiles.map((p, i) => (
              <div key={i} className="stats-card">
                {p.name && <div className="stats-card-name">{p.name}</div>}
                <div className="stats-card-values">
                  <div className="stat-item"><span className="stat-label">M</span><span className="stat-value">{p.movement}</span></div>
                  <div className="stat-item"><span className="stat-label">T</span><span className="stat-value">{p.toughness}</span></div>
                  <div className="stat-item"><span className="stat-label">SV</span><span className="stat-value">{p.save}</span></div>
                  {p.invulnerableSave && <div className="stat-item"><span className="stat-label">Inv</span><span className="stat-value">{p.invulnerableSave}</span></div>}
                  <div className="stat-item"><span className="stat-label">W</span><span className="stat-value">{p.wounds}</span></div>
                  <div className="stat-item"><span className="stat-label">LD</span><span className="stat-value">{p.leadership}</span></div>
                  <div className="stat-item"><span className="stat-label">OC</span><span className="stat-value">{p.objectiveControl}</span></div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {wargear.length > 0 && (
        <div className="unit-detail-weapons">
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
              {wargear.map((wq, i) => (
                <tr key={i}>
                  <td className="weapon-name">{wq.quantity > 1 ? `${wq.quantity}x ` : ""}{wq.wargear.name}</td>
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
          <div className="weapons-mobile">
            {wargear.map((wq, i) => (
              <div key={i} className="weapon-card">
                <div className="weapon-card-header">
                  <span className="weapon-card-name">{wq.quantity > 1 ? `${wq.quantity}x ` : ""}{wq.wargear.name}</span>
                  <span className="weapon-card-range">
                    {wq.wargear.range?.toLowerCase() === "melee" ? "Melee" : wq.wargear.range ?? "-"}
                  </span>
                </div>
                <div className="weapon-card-values">
                  <div className="stat-item"><span className="stat-label">A</span><span className="stat-value">{wq.wargear.attacks ?? "-"}</span></div>
                  <div className="stat-item"><span className="stat-label">BS/WS</span><span className="stat-value">{wq.wargear.ballisticSkill ?? "-"}</span></div>
                  <div className="stat-item"><span className="stat-label">S</span><span className="stat-value">{wq.wargear.strength ?? "-"}</span></div>
                  <div className="stat-item"><span className="stat-label">AP</span><span className="stat-value">{wq.wargear.armorPenetration ?? "-"}</span></div>
                  <div className="stat-item"><span className="stat-label">D</span><span className="stat-value">{wq.wargear.damage ?? "-"}</span></div>
                </div>
                {wq.wargear.description && <div className="weapon-card-abilities"><WeaponAbilityText text={wq.wargear.description} /></div>}
              </div>
            ))}
          </div>
        </div>
      )}

      {enhancement && (
        <div className="unit-detail-enhancement">
          <h4>Enhancement</h4>
          <div className="enhancement-detail">
            <div className="enhancement-header">
              <strong>{enhancement.name}</strong>
              <span className="enhancement-cost">+{enhancement.cost}pts</span>
            </div>
            {enhancement.description && (
              <p dangerouslySetInnerHTML={{ __html: sanitizeHtml(enhancement.description) }} />
            )}
          </div>
        </div>
      )}

      {abilities.filter((a) => a.name).length > 0 && (
        <div className="unit-detail-abilities">
          <h4>Abilities</h4>
          <ul className="abilities-list compact">
            {abilities.filter((a) => a.name).map((a, i) => (
              <li key={i}>
                <strong>{a.name}</strong>
                {a.description && <p dangerouslySetInnerHTML={{ __html: sanitizeHtml(a.description) }} />}
              </li>
            ))}
          </ul>
        </div>
      )}

      {keywords.filter((k) => k.keyword).length > 0 && (
        <div className="unit-detail-keywords">
          <h4>Keywords</h4>
          <div className="keywords-list">
            {keywords.filter((k) => k.keyword).map((k, i) => (
              <span key={i} className={`keyword ${k.isFactionKeyword ? "faction-keyword" : ""}`}>
                {k.keyword}
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
