import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import type { Faction, ArmySummary } from "../types";
import { fetchFactions, fetchAllArmies } from "../api";
import { getFactionTheme } from "../factionTheme";
import { BATTLE_SIZE_POINTS } from "../types";

function formatDate(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays === 0) return "Today";
  if (diffDays === 1) return "Yesterday";
  if (diffDays < 7) return `${diffDays} days ago`;
  return date.toLocaleDateString();
}

export function FactionListPage() {
  const [factions, setFactions] = useState<Faction[]>([]);
  const [armies, setArmies] = useState<ArmySummary[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([fetchFactions(), fetchAllArmies()])
      .then(([f, a]) => {
        setFactions(f);
        setArmies(a);
      })
      .catch((e) => setError(e.message));
  }, []);

  if (error) return <div className="error-message">{error}</div>;

  const factionMap = new Map(factions.map((f) => [f.id, f]));

  return (
    <div className="landing-page">
      <h1>Your Armies</h1>

      {armies.length === 0 ? (
        <div className="empty-state">
          <p>No armies yet. Pick a faction to get started.</p>
          <ul className="faction-list">
            {factions.map((f) => (
              <li key={f.id} className="faction-item">
                <Link to={`/factions/${f.id}`}>{f.name}</Link>
              </li>
            ))}
          </ul>
        </div>
      ) : (
        <>
          <div className="army-cards">
            {armies.map((army) => {
              const faction = factionMap.get(army.factionId);
              const factionTheme = getFactionTheme(army.factionId);

              return (
                <Link
                  key={army.id}
                  to={`/armies/${army.id}`}
                  className="army-card"
                  data-faction={factionTheme}
                >
                  {factionTheme && (
                    <img
                      src={`/icons/${factionTheme}.svg`}
                      alt=""
                      className="army-card-icon"
                      aria-hidden="true"
                    />
                  )}
                  <div className="army-card-header">
                    <span className="army-card-faction">
                      {faction?.name || army.factionId}
                    </span>
                    <span className="army-card-size">
                      {army.totalPoints} pts
                    </span>
                  </div>
                  <h3 className="army-card-name">{army.name}</h3>
                  {army.warlordName && (
                    <div className="army-card-warlord">{army.warlordName}</div>
                  )}
                  <div className="army-card-footer">
                    <span className="army-card-battle-size">
                      {army.battleSize}
                    </span>
                    <span className="army-card-updated">
                      {formatDate(army.updatedAt)}
                    </span>
                  </div>
                </Link>
              );
            })}
          </div>

          <div className="new-army-section">
            <h2>Create New Army</h2>
            <ul className="faction-list">
              {factions.map((f) => (
                <li key={f.id} className="faction-item">
                  <Link to={`/factions/${f.id}`}>{f.name}</Link>
                </li>
              ))}
            </ul>
          </div>
        </>
      )}
    </div>
  );
}
