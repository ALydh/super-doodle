import { useEffect, useState, useMemo } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import type { PersistedArmy, Datasheet, Stratagem, DetachmentAbility, Enhancement, DetachmentInfo, DatasheetDetail, UnitCost, DatasheetLeader, DatasheetOption } from "../types";
import { BATTLE_SIZE_POINTS } from "../types";
import {
  fetchArmy,
  deleteArmy,
  fetchDatasheetsByFaction,
  fetchStratagemsByFaction,
  fetchDetachmentAbilities,
  fetchEnhancementsByFaction,
  fetchDetachmentsByFaction,
  fetchDatasheetDetail,
  fetchLeadersByFaction,
} from "../api";
import { getFactionTheme } from "../factionTheme";
import { useAuth } from "../context/useAuth";
import { TabNavigation } from "../components/TabNavigation";
import { StratagemCard } from "../components/StratagemCard";
import { DetachmentCard } from "../components/DetachmentCard";
import { renderUnitsForMode } from "./renderUnitsForMode";

type TabId = "units" | "stratagems" | "detachment";

const TABS = [
  { id: "units" as const, label: "Units" },
  { id: "stratagems" as const, label: "Stratagems" },
  { id: "detachment" as const, label: "Detachment" },
];

export function ArmyViewPage() {
  const { armyId } = useParams<{ armyId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [army, setArmy] = useState<PersistedArmy | null>(null);
  const [datasheets, setDatasheets] = useState<Datasheet[]>([]);
  const [datasheetDetails, setDatasheetDetails] = useState<Map<string, DatasheetDetail>>(new Map());
  const [stratagems, setStratagems] = useState<Stratagem[]>([]);
  const [detachmentAbilities, setDetachmentAbilities] = useState<DetachmentAbility[]>([]);
  const [enhancements, setEnhancements] = useState<Enhancement[]>([]);
  const [detachments, setDetachments] = useState<DetachmentInfo[]>([]);
  const [leaders, setLeaders] = useState<DatasheetLeader[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<TabId>("units");

  useEffect(() => {
    if (!armyId) return;
    fetchArmy(armyId)
      .then((a) => {
        setArmy(a);
        return Promise.all([
          fetchDatasheetsByFaction(a.army.factionId),
          fetchStratagemsByFaction(a.army.factionId),
          fetchEnhancementsByFaction(a.army.factionId),
          fetchDetachmentsByFaction(a.army.factionId),
          fetchLeadersByFaction(a.army.factionId),
          a.army.detachmentId ? fetchDetachmentAbilities(a.army.detachmentId) : Promise.resolve([]),
        ]);
      })
      .then(([ds, strat, enh, det, ldr, abilities]) => {
        setDatasheets(ds);
        setStratagems(strat);
        setEnhancements(enh);
        setDetachments(det);
        setLeaders(ldr);
        setDetachmentAbilities(abilities);
      })
      .catch((e) => setError(e.message));
  }, [armyId]);

  useEffect(() => {
    if (!army) return;
    const uniqueIds = [...new Set(army.army.units.map((u) => u.datasheetId))];
    const missing = uniqueIds.filter((id) => !datasheetDetails.has(id));
    if (missing.length === 0) return;
    Promise.all(missing.map((id) => fetchDatasheetDetail(id)))
      .then((details) => {
        setDatasheetDetails((prev) => {
          const next = new Map(prev);
          details.forEach((d) => next.set(d.datasheet.id, d));
          return next;
        });
      })
      .catch(() => {});
  }, [army, datasheetDetails]);

  const allCosts: UnitCost[] = useMemo(() =>
    Array.from(datasheetDetails.values()).flatMap(d => d.costs),
    [datasheetDetails]
  );

  const allOptions: DatasheetOption[] = useMemo(() =>
    Array.from(datasheetDetails.values()).flatMap(d => d.options),
    [datasheetDetails]
  );

  const handleDelete = async () => {
    if (!armyId) return;
    await deleteArmy(armyId);
    navigate("/");
  };

  if (error) return <div className="error-message">{error}</div>;
  if (!army) return <div>Loading...</div>;

  const maxPoints = BATTLE_SIZE_POINTS[army.army.battleSize];
  const factionTheme = getFactionTheme(army.army.factionId);

  const detachmentInfo = detachments.find((d) => d.detachmentId === army.army.detachmentId);
  const detachmentName = detachmentInfo?.name ?? army.army.detachmentId;

  const detachmentStratagems = stratagems.filter(
    (s) => s.detachmentId === army.army.detachmentId || !s.detachmentId
  );

  const detachmentEnhancements = enhancements.filter(
    (e) => e.detachmentId === army.army.detachmentId
  );

  const noop = () => {};

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
      <div className="army-view-header">
        {factionTheme && (
          <img
            src={`/icons/${factionTheme}.svg`}
            alt=""
            className="army-view-header-icon"
          />
        )}
        <div className="army-view-header-text">
          <h1 className="army-name">{army.name}</h1>
          <p className="army-meta">
            {army.army.battleSize} - {maxPoints}pts | {detachmentName}
          </p>
        </div>
        {(army.ownerId === null || army.ownerId === user?.id) && (
          <div className="army-view-actions">
            <Link to={`/armies/${armyId}/edit`}>
              <button className="edit-army">Edit</button>
            </Link>
            <button className="btn-delete delete-army" onClick={handleDelete}>Delete</button>
          </div>
        )}
      </div>

      <TabNavigation tabs={TABS} activeTab={activeTab} onTabChange={(t) => setActiveTab(t as TabId)} />

      {activeTab === "units" && (
        <div className="units-tab">
          <table className="units-table">
            <tbody>
              {renderUnitsForMode(
                "grouped",
                army.army.units,
                datasheets,
                allCosts,
                detachmentEnhancements,
                leaders,
                allOptions,
                army.army.warlordId,
                noop,
                noop,
                noop,
                noop,
                true
              )}
            </tbody>
          </table>
        </div>
      )}

      {activeTab === "stratagems" && (
        <div className="stratagems-tab">
          <div className="stratagems-list">
            {detachmentStratagems.map((s) => (
              <StratagemCard key={s.id} stratagem={s} />
            ))}
          </div>
        </div>
      )}

      {activeTab === "detachment" && (
        <div className="detachment-tab">
          <DetachmentCard
            name={detachmentName}
            abilities={detachmentAbilities}
            enhancements={detachmentEnhancements}
          />
        </div>
      )}
    </div>
  );
}
