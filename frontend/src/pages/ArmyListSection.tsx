import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import type { ArmySummary } from "../types";
import { fetchArmiesByFaction } from "../api";

interface Props {
  factionId: string;
}

export function ArmyListSection({ factionId }: Props) {
  const [armies, setArmies] = useState<ArmySummary[]>([]);

  useEffect(() => {
    fetchArmiesByFaction(factionId).then(setArmies).catch(() => {});
  }, [factionId]);

  return (
    <div data-testid="army-list-section">
      <h2>Armies</h2>
      <Link to={`/factions/${factionId}/armies/new`} data-testid="create-army-link">
        Create Army
      </Link>
      {armies.length > 0 && (
        <ul data-testid="army-list">
          {armies.map((a) => (
            <li key={a.id} data-testid="army-list-item">
              <Link to={`/armies/${a.id}`}>{a.name}</Link>
              {" "}— {a.battleSize}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
