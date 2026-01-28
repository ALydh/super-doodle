import { useEffect, useState } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import type { PersistedArmy, Datasheet } from "../types";
import { BATTLE_SIZE_POINTS } from "../types";
import { fetchArmy, deleteArmy, fetchDatasheetsByFaction } from "../api";

export function ArmyViewPage() {
  const { armyId } = useParams<{ armyId: string }>();
  const navigate = useNavigate();
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

  if (error) return <div data-testid="error">{error}</div>;
  if (!army) return <div>Loading...</div>;

  const dsMap = new Map(datasheets.map((ds) => [ds.id, ds]));
  const maxPoints = BATTLE_SIZE_POINTS[army.army.battleSize];

  return (
    <div>
      <Link to={`/factions/${army.army.factionId}`} data-testid="back-to-faction">
        &larr; Back to Faction
      </Link>
      <h1 data-testid="army-name">{army.name}</h1>
      <p data-testid="army-battle-size">Battle Size: {army.army.battleSize} ({maxPoints}pts)</p>
      <p data-testid="army-detachment">Detachment: {army.army.detachmentId}</p>

      <h2>Units</h2>
      <ul data-testid="army-units-list">
        {army.army.units.map((unit, i) => {
          const ds = dsMap.get(unit.datasheetId);
          const isWarlord = unit.datasheetId === army.army.warlordId;
          return (
            <li key={i} data-testid="army-view-unit">
              {ds?.name ?? unit.datasheetId}
              {isWarlord && <strong> (Warlord)</strong>}
              {unit.enhancementId && ` + Enhancement: ${unit.enhancementId}`}
            </li>
          );
        })}
      </ul>

      <div style={{ marginTop: "16px" }}>
        <Link to={`/armies/${armyId}/edit`}>
          <button data-testid="edit-army">Edit</button>
        </Link>{" "}
        <button data-testid="delete-army" onClick={handleDelete}>Delete</button>
      </div>
    </div>
  );
}
