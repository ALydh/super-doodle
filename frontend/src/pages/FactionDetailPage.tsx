import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import type { Datasheet, Stratagem } from "../types";
import { fetchDatasheetsByFaction, fetchStratagemsByFaction } from "../api";
import { ArmyListSection } from "./ArmyListSection";

export function FactionDetailPage() {
  const { factionId } = useParams<{ factionId: string }>();
  const [datasheets, setDatasheets] = useState<Datasheet[]>([]);
  const [stratagems, setStratagems] = useState<Stratagem[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!factionId) return;
    Promise.all([
      fetchDatasheetsByFaction(factionId),
      fetchStratagemsByFaction(factionId),
    ])
      .then(([ds, strat]) => {
        setDatasheets(ds);
        setStratagems(strat);
      })
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

      {stratagems.length > 0 && (
        <>
          <h2>Stratagems</h2>
          <ul data-testid="stratagems-list">
            {stratagems.map((s) => (
              <li key={s.id} data-testid="stratagem-item">
                <strong>{s.name}</strong>
                {s.cpCost !== null && <span> ({s.cpCost} CP)</span>}
                {s.phase && <span> - {s.phase}</span>}
                <p dangerouslySetInnerHTML={{ __html: s.description }} />
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}
