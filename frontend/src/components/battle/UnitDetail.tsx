import type { BattleUnitData, Wargear } from "../../types";
import { WeaponAbilityText } from "../../pages/WeaponAbilityText";

interface Props {
  data: BattleUnitData;
  isWarlord: boolean;
}

function matchesWeaponPrefix(weaponName: string, prefix: string): boolean {
  const normalizedWeapon = weaponName.toLowerCase();
  const normalizedPrefix = prefix.toLowerCase();
  return normalizedWeapon === normalizedPrefix || normalizedWeapon.startsWith(normalizedPrefix + " ");
}

function getFilteredWargear(data: BattleUnitData): Wargear[] {
  const { wargear, parsedWargearOptions, unit } = data;

  if (parsedWargearOptions.length === 0) {
    return wargear.filter(w => w.name);
  }

  const addedWeapons = new Set<string>();
  const removedWeapons = new Set<string>();
  for (const p of parsedWargearOptions) {
    const action = p.action.toLowerCase();
    if (action === "add") addedWeapons.add(p.weaponName.toLowerCase());
    else if (action === "remove") removedWeapons.add(p.weaponName.toLowerCase());
  }

  const weaponMap = new Map<string, Wargear>();
  for (const w of wargear) {
    if (!w.name) continue;
    const isOptional = Array.from(addedWeapons).some(added =>
      matchesWeaponPrefix(w.name!, added) && !Array.from(removedWeapons).some(removed => matchesWeaponPrefix(w.name!, removed))
    );
    if (!isOptional) {
      weaponMap.set(w.name.toLowerCase(), w);
    }
  }

  const activeSelections = unit.wargearSelections.filter(s => s.selected);

  for (const sel of activeSelections) {
    const parsed = parsedWargearOptions.filter(p => p.optionLine === sel.optionLine);

    for (const p of parsed) {
      const targetName = p.weaponName.toLowerCase();
      const action = p.action.toLowerCase();
      if (action === "remove") {
        for (const [key, w] of weaponMap) {
          if (matchesWeaponPrefix(w.name ?? "", targetName)) {
            weaponMap.delete(key);
          }
        }
      } else if (action === "add") {
        const weapons = wargear.filter(w => matchesWeaponPrefix(w.name ?? "", targetName));
        for (const w of weapons) {
          weaponMap.set(w.name!.toLowerCase(), w);
        }
      }
    }

    if (sel.notes) {
      const normalizedNotes = sel.notes.toLowerCase();
      const addOptions = parsedWargearOptions.filter(
        p => p.optionLine === sel.optionLine && p.action.toLowerCase() === "add"
      );
      for (const opt of addOptions) {
        if (normalizedNotes.includes(opt.weaponName.toLowerCase())) {
          const weapons = wargear.filter(w => matchesWeaponPrefix(w.name ?? "", opt.weaponName));
          for (const w of weapons) {
            weaponMap.set(w.name!.toLowerCase(), w);
          }
        }
      }
    }
  }

  return Array.from(weaponMap.values());
}

export function UnitDetail({ data, isWarlord }: Props) {
  const { datasheet, profiles, abilities, keywords, cost, enhancement } = data;
  const displayedWargear = getFilteredWargear(data);

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

      {displayedWargear.length > 0 && (
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
              {displayedWargear.map((w, i) => (
                <tr key={i}>
                  <td className="weapon-name">{w.name}</td>
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
            {displayedWargear.map((w, i) => (
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

      {abilities.filter((a) => a.name).length > 0 && (
        <div className="unit-detail-abilities">
          <h4>Abilities</h4>
          <ul className="abilities-list compact">
            {abilities.filter((a) => a.name).map((a, i) => (
              <li key={i}>
                <strong>{a.name}</strong>
                {a.description && <p dangerouslySetInnerHTML={{ __html: a.description }} />}
              </li>
            ))}
          </ul>
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
              <p dangerouslySetInnerHTML={{ __html: enhancement.description }} />
            )}
          </div>
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
