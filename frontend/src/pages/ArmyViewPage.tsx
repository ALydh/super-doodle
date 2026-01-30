import { useEffect, useState } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import type { PersistedArmy, Datasheet, Stratagem, DetachmentAbility, Enhancement, DetachmentInfo, DatasheetDetail } from "../types";
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
} from "../api";
import { getFactionTheme } from "../factionTheme";
import { useAuth } from "../context/useAuth";
import { TabNavigation } from "../components/TabNavigation";
import { ExpandableUnitCard } from "../components/ExpandableUnitCard";
import { StratagemCard } from "../components/StratagemCard";
import { DetachmentCard } from "../components/DetachmentCard";

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
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<TabId>("units");
  const [expandedUnit, setExpandedUnit] = useState<string | null>(null);

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
          a.army.detachmentId ? fetchDetachmentAbilities(a.army.detachmentId) : Promise.resolve([]),
        ]);
      })
      .then(([ds, strat, enh, det, abilities]) => {
        setDatasheets(ds);
        setStratagems(strat);
        setEnhancements(enh);
        setDetachments(det);
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

  const handleDelete = async () => {
    if (!armyId) return;
    await deleteArmy(armyId);
    navigate("/");
  };

  if (error) return <div className="error-message">{error}</div>;
  if (!army) return <div>Loading...</div>;

  const dsMap = new Map(datasheets.map((ds) => [ds.id, ds]));
  const enhMap = new Map(enhancements.map((e) => [e.id, e]));
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

  const handleUnitToggle = (unitKey: string) => {
    setExpandedUnit(expandedUnit === unitKey ? null : unitKey);
  };

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
          {sortedRoles.map((role) => (
            <div key={role} className="army-view-role-group">
              <h3 className="army-view-role-heading">{role}</h3>
              <div className="unit-cards-list">
                {unitsByRole[role]
                  .sort((a, b) => (a.ds?.name ?? a.unit.datasheetId).localeCompare(b.ds?.name ?? b.unit.datasheetId))
                  .map(({ unit, index, ds }) => {
                    const isWarlord = unit.datasheetId === army.army.warlordId;
                    const enhancement = unit.enhancementId ? enhMap.get(unit.enhancementId) : null;
                    const unitKey = `${unit.datasheetId}-${index}`;
                    const detail = datasheetDetails.get(unit.datasheetId);
                    const baseCost = detail?.costs.find((c) => c.line === unit.sizeOptionLine)?.cost ?? 0;
                    const enhCost = enhancement?.cost ?? 0;
                    const totalCost = baseCost + enhCost;
                    return (
                      <ExpandableUnitCard
                        key={unitKey}
                        datasheetId={unit.datasheetId}
                        datasheetName={ds?.name ?? unit.datasheetId}
                        points={totalCost}
                        isExpanded={expandedUnit === unitKey}
                        onToggle={() => handleUnitToggle(unitKey)}
                        isWarlord={isWarlord}
                        enhancement={enhancement}
                      />
                    );
                  })}
              </div>
            </div>
          ))}
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
