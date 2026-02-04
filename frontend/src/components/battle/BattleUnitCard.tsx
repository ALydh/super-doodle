import { useState } from "react";
import type { BattleUnitData } from "../../types";
import { UnitDetailWide } from "./UnitDetailWide";

interface Props {
  data: BattleUnitData;
  isWarlord: boolean;
  defaultExpanded?: boolean;
  count?: number;
}

export function BattleUnitCard({ data, isWarlord, defaultExpanded = false, count = 1 }: Props) {
  const [expanded, setExpanded] = useState(defaultExpanded);
  const { datasheet, profiles, cost, enhancement } = data;

  const mainProfile = profiles[0];
  const totalCost = cost ? cost.cost * count + (enhancement?.cost ?? 0) : 0;

  return (
    <div className={`battle-unit-card ${expanded ? "expanded" : ""} ${count > 1 ? "stacked-card" : ""}`}>
      {count > 1 && (
        <>
          <div className="stacked-card-shadow" />
          <div className="stacked-card-shadow" />
        </>
      )}
      <button
        className="battle-unit-card-header"
        onClick={() => setExpanded(!expanded)}
      >
        <span className="expand-icon">{expanded ? "▼" : "▶"}</span>
        <span className="battle-unit-card-name">
          {datasheet.name}
          {count > 1 && <span className="stacked-count">×{count}</span>}
          {isWarlord && <span className="warlord-badge">★</span>}
        </span>
        {enhancement && (
          <span className="enhancement-pill">{enhancement.name}</span>
        )}
        <span className="battle-unit-card-stats">
          {mainProfile && (
            <>
              <span className="stat-pill">M{mainProfile.movement}</span>
              <span className="stat-pill">T{mainProfile.toughness}</span>
              <span className="stat-pill">W{mainProfile.wounds}</span>
              <span className="stat-pill">SV{mainProfile.save}</span>
              {mainProfile.invulnerableSave && (
                <span className="stat-pill">Inv{mainProfile.invulnerableSave}</span>
              )}
              <span className="stat-pill">OC{mainProfile.objectiveControl}</span>
            </>
          )}
        </span>
        <span className="battle-unit-card-cost">{totalCost}pts</span>
      </button>
      {expanded && (
        <div className="battle-unit-card-content">
          <UnitDetailWide data={data} isWarlord={isWarlord} />
        </div>
      )}
    </div>
  );
}
