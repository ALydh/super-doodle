import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import type { Datasheet, Stratagem, DetachmentInfo, DetachmentAbility, Enhancement } from "../types";
import {
  fetchDatasheetsByFaction,
  fetchStratagemsByFaction,
  fetchDetachmentsByFaction,
  fetchDetachmentAbilities,
  fetchEnhancementsByFaction,
  fetchFactions,
} from "../api";
import { getFactionTheme } from "../factionTheme";
import { TabNavigation } from "../components/TabNavigation";
import { ExpandableUnitCard } from "../components/ExpandableUnitCard";
import { StratagemCard } from "../components/StratagemCard";
import { DetachmentCard } from "../components/DetachmentCard";

type TabId = "units" | "stratagems" | "detachments";

const TABS = [
  { id: "units" as const, label: "Units" },
  { id: "stratagems" as const, label: "Stratagems" },
  { id: "detachments" as const, label: "Detachments" },
];

export function FactionDetailPage() {
  const { factionId } = useParams<{ factionId: string }>();
  const [factionName, setFactionName] = useState<string>("");
  const [datasheets, setDatasheets] = useState<Datasheet[]>([]);
  const [stratagems, setStratagems] = useState<Stratagem[]>([]);
  const [detachments, setDetachments] = useState<DetachmentInfo[]>([]);
  const [detachmentAbilities, setDetachmentAbilities] = useState<Map<string, DetachmentAbility[]>>(new Map());
  const [enhancements, setEnhancements] = useState<Enhancement[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<TabId>("units");
  const [expandedUnit, setExpandedUnit] = useState<string | null>(null);
  const [stratagemDetachmentFilter, setStratagemDetachmentFilter] = useState<string>("all");
  const [stratagemPhaseFilter, setStratagemPhaseFilter] = useState<string>("all");

  useEffect(() => {
    if (!factionId) return;
    Promise.all([
      fetchDatasheetsByFaction(factionId),
      fetchStratagemsByFaction(factionId),
      fetchDetachmentsByFaction(factionId),
      fetchEnhancementsByFaction(factionId),
      fetchFactions(),
    ])
      .then(([ds, strat, det, enh, factions]) => {
        setDatasheets(ds);
        setStratagems(strat);
        setDetachments(det);
        setEnhancements(enh);
        const faction = factions.find((f) => f.id === factionId);
        setFactionName(faction?.name ?? factionId);
      })
      .catch((e) => setError(e.message));
  }, [factionId]);

  useEffect(() => {
    if (activeTab !== "detachments" || detachments.length === 0) return;
    detachments.forEach((det) => {
      if (!detachmentAbilities.has(det.detachmentId)) {
        fetchDetachmentAbilities(det.detachmentId).then((abilities) => {
          setDetachmentAbilities((prev) => new Map(prev).set(det.detachmentId, abilities));
        });
      }
    });
  }, [activeTab, detachments, detachmentAbilities]);

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

  const roleOrder = ["Characters", "Battleline", "Dedicated Transport", "Other"];
  const sortedRoles = Object.keys(datasheetsByRole).sort((a, b) => {
    const aIndex = roleOrder.indexOf(a);
    const bIndex = roleOrder.indexOf(b);
    if (aIndex === -1 && bIndex === -1) return a.localeCompare(b);
    if (aIndex === -1) return 1;
    if (bIndex === -1) return -1;
    return aIndex - bIndex;
  });

  const phases = [...new Set(stratagems.filter((s) => s.phase).map((s) => s.phase!))].sort();

  const filteredStratagems = stratagems.filter((s) => {
    if (stratagemDetachmentFilter !== "all" && s.detachmentId !== stratagemDetachmentFilter) {
      return false;
    }
    if (stratagemPhaseFilter !== "all" && s.phase !== stratagemPhaseFilter) {
      return false;
    }
    return true;
  });

  const handleUnitToggle = (datasheetId: string) => {
    setExpandedUnit(expandedUnit === datasheetId ? null : datasheetId);
  };

  return (
    <div data-faction={factionTheme} className="faction-detail-page">
      {factionTheme && (
        <img
          src={`/icons/${factionTheme}.svg`}
          alt=""
          className="army-builder-bg-icon"
          aria-hidden="true"
        />
      )}
      <div className="faction-header">
        <div className="faction-header-info">
          {factionTheme && (
            <img
              src={`/icons/${factionTheme}.svg`}
              alt=""
              className="faction-header-icon"
            />
          )}
          <h1 className="faction-title">{factionName}</h1>
        </div>
        <Link to={`/factions/${factionId}/armies/new`} className="btn-create-army">
          + Create Army
        </Link>
      </div>

      <TabNavigation tabs={TABS} activeTab={activeTab} onTabChange={(t) => setActiveTab(t as TabId)} />

      {activeTab === "units" && (
        <div className="units-tab">
          {sortedRoles.map((role) => (
            <section key={role} className="role-section">
              <h2 className="role-heading">{role}</h2>
              <div className="unit-cards-list">
                {datasheetsByRole[role]
                  .sort((a, b) => a.name.localeCompare(b.name))
                  .map((ds) => (
                    <ExpandableUnitCard
                      key={ds.id}
                      datasheetId={ds.id}
                      datasheetName={ds.name}
                      isExpanded={expandedUnit === ds.id}
                      onToggle={() => handleUnitToggle(ds.id)}
                    />
                  ))}
              </div>
            </section>
          ))}
        </div>
      )}

      {activeTab === "stratagems" && (
        <div className="stratagems-tab">
          <div className="stratagem-filters">
            <label>
              Detachment:
              <select
                value={stratagemDetachmentFilter}
                onChange={(e) => setStratagemDetachmentFilter(e.target.value)}
              >
                <option value="all">All Detachments</option>
                {detachments.map((d) => (
                  <option key={d.detachmentId} value={d.detachmentId}>
                    {d.name}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Phase:
              <select
                value={stratagemPhaseFilter}
                onChange={(e) => setStratagemPhaseFilter(e.target.value)}
              >
                <option value="all">All Phases</option>
                {phases.map((p) => (
                  <option key={p} value={p}>{p}</option>
                ))}
              </select>
            </label>
          </div>
          <div className="stratagems-list">
            {filteredStratagems.map((s) => (
              <StratagemCard key={s.id} stratagem={s} />
            ))}
          </div>
        </div>
      )}

      {activeTab === "detachments" && (
        <div className="detachments-tab">
          {detachments.map((det) => (
            <DetachmentCard
              key={det.detachmentId}
              name={det.name}
              abilities={detachmentAbilities.get(det.detachmentId) ?? []}
              enhancements={enhancements.filter((e) => e.detachmentId === det.detachmentId)}
            />
          ))}
        </div>
      )}
    </div>
  );
}
