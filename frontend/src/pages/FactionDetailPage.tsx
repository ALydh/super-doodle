import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import type { Datasheet, Stratagem } from "../types";
import { fetchDatasheetsByFaction, fetchStratagemsByFaction } from "../api";
import { ArmyListSection } from "./ArmyListSection";
import { getFactionTheme } from "../factionTheme";

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

  if (error) return <div className="error-message">{error}</div>;

  const factionTheme = getFactionTheme(factionId);

  return (
    <div data-faction={factionTheme}>
      <Link to="/" className="back-link">&larr; Back to Factions</Link>
      <h1 className="faction-title">Datasheets for {factionId}</h1>
      <ul className="datasheet-list">
        {datasheets.map((ds) => (
          <li key={ds.id} className="datasheet-item">
            <Link to={`/datasheets/${ds.id}`}>{ds.name}</Link>
          </li>
        ))}
      </ul>
      {factionId && <ArmyListSection factionId={factionId} />}

      {stratagems.length > 0 && (
        <>
          <h2>Stratagems</h2>
          <ul className="stratagems-list">
            {stratagems.map((s) => (
              <li key={s.id} className="stratagem-item">
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
