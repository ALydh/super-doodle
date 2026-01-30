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

  const datasheetsByRole = datasheets.reduce<Record<string, Datasheet[]>>(
    (acc, ds) => {
      const role = ds.role ?? "Other";
      if (!acc[role]) acc[role] = [];
      acc[role].push(ds);
      return acc;
    },
    {},
  );

  const roleOrder = [
    "Characters",
    "Battleline",
    "Dedicated Transport",
    "Other",
  ];
  const sortedRoles = Object.keys(datasheetsByRole).sort((a, b) => {
    const aIndex = roleOrder.indexOf(a);
    const bIndex = roleOrder.indexOf(b);
    if (aIndex === -1 && bIndex === -1) return a.localeCompare(b);
    if (aIndex === -1) return 1;
    if (bIndex === -1) return -1;
    return aIndex - bIndex;
  });

  return (
    <div data-faction={factionTheme}>
      <Link to="/" className="back-link">
        &larr; Back to Factions
      </Link>
      {factionId && <ArmyListSection factionId={factionId} />}
      <h1 className="faction-title">Datasheets for {factionId}</h1>
      {sortedRoles.map((role) => (
        <section key={role} className="role-section">
          <h2 className="role-heading">{role}</h2>
          <ul className="datasheet-list">
            {datasheetsByRole[role]
              .sort((a, b) => a.name.localeCompare(b.name))
              .map((ds) => (
                <li key={ds.id} className="datasheet-item">
                  <Link to={`/datasheets/${ds.id}`}>{ds.name}</Link>
                </li>
              ))}
          </ul>
        </section>
      ))}

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
