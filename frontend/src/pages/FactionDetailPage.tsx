import { useEffect, useState, useMemo } from "react";
import { Link, useParams } from "react-router-dom";
import type { Datasheet, DatasheetDetail, Stratagem, DetachmentInfo, DetachmentAbility, Enhancement, ModelProfile, DatasheetKeyword } from "../types";
import {
  fetchDatasheetDetailsByFaction,
  fetchStratagemsByFaction,
  fetchDetachmentsByFaction,
  fetchDetachmentAbilities,
  fetchEnhancementsByFaction,
  fetchFactions,
} from "../api";
import { getFactionTheme } from "../factionTheme";
import { isSpaceMarines, SM_CHAPTERS, CHAPTER_KEYWORDS, getChapterTheme } from "../chapters";
import { TabNavigation } from "../components/TabNavigation";
import { ExpandableUnitCard } from "../components/ExpandableUnitCard";
import { StratagemCard } from "../components/StratagemCard";
import { DetachmentCard } from "../components/DetachmentCard";
import { sortByRoleOrder } from "../constants";
import styles from "./FactionDetailPage.module.css";

type TabId = "units" | "stratagems" | "detachments";

const TABS = [
  { id: "units" as const, label: "Units" },
  { id: "stratagems" as const, label: "Stratagems" },
  { id: "detachments" as const, label: "Detachments" },
];

export function FactionDetailPage() {
  const { factionId } = useParams<{ factionId: string }>();
  const [factionName, setFactionName] = useState<string>("");
  const [datasheetDetails, setDatasheetDetails] = useState<DatasheetDetail[]>([]);
  const [stratagems, setStratagems] = useState<Stratagem[]>([]);
  const [detachments, setDetachments] = useState<DetachmentInfo[]>([]);
  const [detachmentAbilities, setDetachmentAbilities] = useState<Map<string, DetachmentAbility[]>>(new Map());
  const [enhancements, setEnhancements] = useState<Enhancement[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<TabId>("units");
  const [expandedUnit, setExpandedUnit] = useState<string | null>(null);
  const [stratagemDetachmentFilter, setStratagemDetachmentFilter] = useState<string>("all");
  const [stratagemPhaseFilter, setStratagemPhaseFilter] = useState<string>("all");
  const [search, setSearch] = useState("");
  const [chapterId, setChapterId] = useState<string | null>(null);
  const [chapterFilter, setChapterFilter] = useState<"all" | "chapter">("all");

  useEffect(() => {
    if (!factionId) return;
    Promise.all([
      fetchDatasheetDetailsByFaction(factionId),
      fetchStratagemsByFaction(factionId),
      fetchDetachmentsByFaction(factionId),
      fetchEnhancementsByFaction(factionId),
      fetchFactions(),
    ])
      .then(([ds, strat, det, enh, factions]) => {
        setDatasheetDetails(ds);
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

  const datasheets = useMemo(
    () => datasheetDetails.map((d) => d.datasheet).filter((ds) => !ds.virtual),
    [datasheetDetails],
  );

  const profilesByDatasheet = useMemo(() => {
    const map = new Map<string, ModelProfile[]>();
    for (const d of datasheetDetails) {
      if (d.profiles.length > 0) map.set(d.datasheet.id, d.profiles);
    }
    return map;
  }, [datasheetDetails]);

  const keywordsByDatasheet = useMemo(() => {
    const map = new Map<string, DatasheetKeyword[]>();
    for (const d of datasheetDetails) {
      map.set(d.datasheet.id, d.keywords);
    }
    return map;
  }, [datasheetDetails]);

  const isSM = factionId ? isSpaceMarines(factionId) : false;
  const chapterObj = SM_CHAPTERS.find((c) => c.id === chapterId);
  const chapterKeyword = chapterObj?.keyword ?? null;
  const chapterTheme = chapterId ? getChapterTheme(chapterId) : null;
  const baseFactionTheme = getFactionTheme(factionId);
  const factionTheme = chapterTheme ?? baseFactionTheme;

  const classifyUnit = useMemo(() => {
    if (!chapterKeyword) return () => "generic" as const;
    return (datasheetId: string): "chapter" | "generic" | "other-chapter" => {
      const keywords = keywordsByDatasheet.get(datasheetId) ?? [];
      const factionKeywords = keywords
        .filter((k) => k.isFactionKeyword)
        .map((k) => k.keyword)
        .filter(Boolean) as string[];
      if (factionKeywords.includes(chapterKeyword)) return "chapter";
      if (factionKeywords.some((k) => CHAPTER_KEYWORDS.has(k))) return "other-chapter";
      return "generic";
    };
  }, [chapterKeyword, keywordsByDatasheet]);

  const filtered = datasheets.filter((ds) => {
    if (!ds.name.toLowerCase().includes(search.toLowerCase())) return false;
    if (chapterKeyword && chapterFilter === "chapter") {
      const cls = classifyUnit(ds.id);
      return cls === "chapter" || cls === "generic";
    }
    return true;
  });

  const sortUnit = (a: Datasheet, b: Datasheet) => {
    if (chapterKeyword) {
      const aClass = classifyUnit(a.id);
      const bClass = classifyUnit(b.id);
      const order = { chapter: 0, generic: 1, "other-chapter": 2 };
      const diff = order[aClass] - order[bClass];
      if (diff !== 0) return diff;
    }
    return a.name.localeCompare(b.name);
  };

  const datasheetsByRole = filtered.reduce<Record<string, Datasheet[]>>(
    (acc, ds) => {
      const role = ds.role ?? "Other";
      if (!acc[role]) acc[role] = [];
      acc[role].push(ds);
      return acc;
    },
    {},
  );

  const sortedRoles = sortByRoleOrder(Object.keys(datasheetsByRole));
  const noResults = filtered.length === 0 && datasheets.length > 0;

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
    <div data-faction={factionTheme} className={styles.page}>
      {factionTheme && (
        <img
          src={`/icons/${baseFactionTheme}.svg`}
          alt=""
          className={styles.bgIcon}
          aria-hidden="true"
        />
      )}
      <div className={styles.header}>
        <div className={styles.headerInfo}>
          {factionTheme && (
            <img
              src={`/icons/${baseFactionTheme}.svg`}
              alt=""
              className={styles.headerIcon}
            />
          )}
          <h1 className={styles.title}>{factionName}</h1>
        </div>
        <Link to={`/factions/${factionId}/armies/new`} className={styles.btnCreateArmy}>
          + Create Army
        </Link>
      </div>

      <TabNavigation tabs={TABS} activeTab={activeTab} onTabChange={(t) => setActiveTab(t as TabId)} />

      {activeTab === "units" && (
        <div className={styles.unitsTab}>
          <input
            type="text"
            className={styles.searchInput}
            placeholder="Search units..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          {isSM && (
            <div className={styles.chapterControls}>
              <select
                className={styles.chapterSelect}
                value={chapterId ?? ""}
                onChange={(e) => {
                  const val = e.target.value || null;
                  setChapterId(val);
                  if (!val) setChapterFilter("all");
                }}
              >
                <option value="">No Chapter</option>
                {SM_CHAPTERS.map((c) => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
              {chapterKeyword && (
                <div className={styles.chapterFilters}>
                  <button
                    type="button"
                    className={`${styles.filterPill} ${chapterFilter === "all" ? styles.filterPillActive : ""}`}
                    onClick={() => setChapterFilter("all")}
                  >
                    All
                  </button>
                  <button
                    type="button"
                    className={`${styles.filterPill} ${chapterFilter === "chapter" ? styles.filterPillActive : ""}`}
                    onClick={() => setChapterFilter("chapter")}
                  >
                    Chapter Only
                  </button>
                </div>
              )}
            </div>
          )}
          {noResults && <p className={styles.noResults}>No units found</p>}
          {sortedRoles.map((role) => (
            <section key={role} className={styles.roleSection}>
              <h2 className={styles.roleHeading}>{role}</h2>
              <div className={styles.cardsList}>
                {datasheetsByRole[role].sort(sortUnit).map((ds) => {
                  const unitClass = chapterKeyword ? classifyUnit(ds.id) : null;
                  return (
                    <div
                      key={ds.id}
                      className={unitClass === "other-chapter" ? styles.deprioritized : undefined}
                    >
                      <ExpandableUnitCard
                        datasheetId={ds.id}
                        datasheetName={ds.name}
                        isExpanded={expandedUnit === ds.id}
                        onToggle={() => handleUnitToggle(ds.id)}
                        profiles={profilesByDatasheet.get(ds.id)}
                      />
                    </div>
                  );
                })}
              </div>
            </section>
          ))}
        </div>
      )}

      {activeTab === "stratagems" && (
        <div>
          <div className={styles.filters}>
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
          <div className={styles.stratagemsList}>
            {filteredStratagems.map((s) => (
              <StratagemCard key={s.id} stratagem={s} />
            ))}
          </div>
        </div>
      )}

      {activeTab === "detachments" && (
        <div>
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
