import { useEffect, useState } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import type { PersistedArmy, Datasheet } from "../types";
import { BATTLE_SIZE_POINTS } from "../types";
import { fetchArmy, deleteArmy, fetchDatasheetsByFaction } from "../api";
import { getFactionTheme } from "../factionTheme";
import { useAuth } from "../context/useAuth";

export function ArmyViewPage() {
  const { armyId } = useParams<{ armyId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [army, setArmy] = useState<PersistedArmy | null>(null);
  const [datasheets, setDatasheets] = useState<Datasheet[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!armyId) return;
    fetchArmy(armyId)
      .then((a) => {
        setArmy(a);
        return fetchDatasheetsByFaction(a.army.factionId);
      })
      .then(setDatasheets)
      .catch((e) => setError(e.message));
  }, [armyId]);

  const handleDelete = async () => {
    if (!armyId) return;
    await deleteArmy(armyId);
    if (army) {
      navigate(`/factions/${army.army.factionId}`);
    } else {
      navigate("/");
    }
  };

  if (error) return <div className="error-message">{error}</div>;
  if (!army) return <div>Loading...</div>;

  const dsMap = new Map(datasheets.map((ds) => [ds.id, ds]));
  const maxPoints = BATTLE_SIZE_POINTS[army.army.battleSize];
  const factionTheme = getFactionTheme(army.army.factionId);

  return (
    <div data-faction={factionTheme} className="army-view-page">
      {factionTheme && (
        <img
          src={`/icons/${factionTheme}.svg`}
          alt=""
          className="army-builder-bg-icon"
          aria-hidden="true"
        />
      )}
      <Link to={`/factions/${army.army.factionId}`} className="back-link">
        &larr; Back to Faction
      </Link>
      <h1 className="army-name">{army.name}</h1>
      <p className="army-battle-size">Battle Size: {army.army.battleSize} ({maxPoints}pts)</p>
      <p className="army-detachment">Detachment: {army.army.detachmentId}</p>

      <h2>Units</h2>
      {(() => {
        const unitsByRole = army.army.units.reduce<Record<string, { unit: typeof army.army.units[0]; index: number; ds: Datasheet | undefined }[]>>((acc, unit, i) => {
          const ds = dsMap.get(unit.datasheetId);
          const role = ds?.role ?? "Other";
          if (!acc[role]) acc[role] = [];
          acc[role].push({ unit, index: i, ds });
          return acc;
        }, {});

        const roleOrder = ["Characters", "Battleline", "Dedicated Transport", "Other"];
        const sortedRoles = Object.keys(unitsByRole).sort((a, b) => {
          const aIndex = roleOrder.indexOf(a);
          const bIndex = roleOrder.indexOf(b);
          if (aIndex === -1 && bIndex === -1) return a.localeCompare(b);
          if (aIndex === -1) return 1;
          if (bIndex === -1) return -1;
          return aIndex - bIndex;
        });

        return sortedRoles.map((role) => (
          <div key={role} className="army-view-role-group">
            <h3 className="army-view-role-heading">{role}</h3>
            <ul className="army-units-list">
              {unitsByRole[role]
                .sort((a, b) => (a.ds?.name ?? a.unit.datasheetId).localeCompare(b.ds?.name ?? b.unit.datasheetId))
                .map(({ unit, ds }) => {
                  const isWarlord = unit.datasheetId === army.army.warlordId;
                  return (
                    <li key={unit.datasheetId + Math.random()} className="army-view-unit">
                      <span className="army-view-unit-name">
                        {ds?.name ?? unit.datasheetId}
                        {isWarlord && <span className="warlord-badge">Warlord</span>}
                      </span>
                      {unit.enhancementId && (
                        <span className="army-view-unit-enhancement">+ {unit.enhancementId}</span>
                      )}
                    </li>
                  );
                })}
            </ul>
          </div>
        ));
      })()}

      {(army.ownerId === null || army.ownerId === user?.id) && (
        <div style={{ marginTop: "16px" }}>
          <Link to={`/armies/${armyId}/edit`}>
            <button className="edit-army">Edit</button>
          </Link>{" "}
          <button className="btn-delete delete-army" onClick={handleDelete}>Delete</button>
        </div>
      )}
    </div>
  );
}
