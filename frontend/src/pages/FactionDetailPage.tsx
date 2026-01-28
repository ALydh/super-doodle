import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import type { Datasheet } from "../types";
import { fetchDatasheetsByFaction } from "../api";
import { ArmyListSection } from "./ArmyListSection";

export function FactionDetailPage() {
  const { factionId } = useParams<{ factionId: string }>();
  const [datasheets, setDatasheets] = useState<Datasheet[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!factionId) return;
    fetchDatasheetsByFaction(factionId)
      .then(setDatasheets)
      .catch((e) => setError(e.message));
  }, [factionId]);

  if (error) return <div data-testid="error">{error}</div>;

  return (
    <div>
      <Link to="/" data-testid="back-to-factions">&larr; Back to Factions</Link>
      <h1 data-testid="faction-title">Datasheets for {factionId}</h1>
      <ul data-testid="datasheet-list">
        {datasheets.map((ds) => (
          <li key={ds.id} data-testid="datasheet-item">
            <Link to={`/datasheets/${ds.id}`}>{ds.name}</Link>
          </li>
        ))}
      </ul>
      {factionId && <ArmyListSection factionId={factionId} />}
    </div>
  );
}
