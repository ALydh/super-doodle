import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import type { Faction } from "../types";
import { fetchFactions } from "../api";

export function FactionListPage() {
  const [factions, setFactions] = useState<Faction[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchFactions().then(setFactions).catch((e) => setError(e.message));
  }, []);

  if (error) return <div className="error-message">{error}</div>;

  return (
    <div>
      <h1>Factions</h1>
      <ul className="faction-list">
        {factions.map((f) => (
          <li key={f.id} className="faction-item">
            <Link to={`/factions/${f.id}`}>{f.name}</Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
