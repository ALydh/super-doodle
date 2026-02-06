import { useState, useMemo } from "react";
import type {
  Datasheet,
  UnitCost,
  AlliedFactionInfo,
  DatasheetKeyword,
} from "../types";
import { sortByRoleOrder } from "../constants";
import { CHAPTER_KEYWORDS } from "../chapters";
import styles from "./UnitPicker.module.css";

type ChapterFilter = "all" | "chapter";
type InventoryFilter = "all" | "owned" | "missing";

interface Props {
  datasheets: Datasheet[];
  costs: UnitCost[];
  onAdd: (
    datasheetId: string,
    sizeOptionLine: number,
    isAllied?: boolean,
  ) => void;
  alliedFactions?: AlliedFactionInfo[];
  alliedCosts?: UnitCost[];
  chapterKeyword?: string | null;
  keywordsByDatasheet?: Map<string, DatasheetKeyword[]>;
  inventory?: Map<string, number> | null;
}

export function UnitPicker({
  datasheets,
  costs,
  onAdd,
  alliedFactions = [],
  alliedCosts = [],
  chapterKeyword = null,
  keywordsByDatasheet = new Map(),
  inventory = null,
}: Props) {
  const [search, setSearch] = useState("");
  const [alliesExpanded, setAlliesExpanded] = useState(false);
  const [chapterFilter, setChapterFilter] = useState<ChapterFilter>("all");
  const [inventoryFilter, setInventoryFilter] = useState<InventoryFilter>("all");

  const classifyUnit = useMemo(() => {
    if (!chapterKeyword) return () => "generic" as const;
    return (datasheetId: string): "chapter" | "generic" | "other-chapter" => {
      const keywords = keywordsByDatasheet.get(datasheetId) ?? [];
      const factionKeywords = keywords
        .filter((k) => k.isFactionKeyword)
        .map((k) => k.keyword)
        .filter(Boolean) as string[];
      if (factionKeywords.includes(chapterKeyword)) return "chapter";
      if (factionKeywords.some((k) => CHAPTER_KEYWORDS.has(k)))
        return "other-chapter";
      return "generic";
    };
  }, [chapterKeyword, keywordsByDatasheet]);

  const filtered = datasheets.filter((ds) => {
    if (ds.virtual) return false;
    if (!ds.name.toLowerCase().includes(search.toLowerCase())) return false;
    if (chapterKeyword && chapterFilter === "chapter") {
      const cls = classifyUnit(ds.id);
      if (cls !== "chapter" && cls !== "generic") return false;
    }
    if (inventory && inventoryFilter !== "all") {
      const qty = inventory.get(ds.id) ?? 0;
      if (inventoryFilter === "owned" && qty === 0) return false;
      if (inventoryFilter === "missing" && qty > 0) return false;
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

  const filteredByRole = filtered.reduce<Record<string, typeof filtered>>(
    (acc, ds) => {
      const role = ds.role ?? "Other";
      if (!acc[role]) acc[role] = [];
      acc[role].push(ds);
      return acc;
    },
    {},
  );

  const sortedRoles = sortByRoleOrder(Object.keys(filteredByRole));

  const filteredAlliedFactions = alliedFactions
    .map((ally) => ({
      ...ally,
      datasheets: ally.datasheets.filter((ds) =>
        ds.name.toLowerCase().includes(search.toLowerCase()),
      ),
    }))
    .filter((ally) => ally.datasheets.length > 0);

  const getCostForDatasheet = (datasheetId: string, costsArray: UnitCost[]) => {
    const dsCosts = costsArray.filter((c) => c.datasheetId === datasheetId);
    const firstLine = dsCosts[0]?.line ?? 1;
    const minCost =
      dsCosts.length > 0 ? Math.min(...dsCosts.map((c) => c.cost)) : 0;
    return { firstLine, minCost };
  };

  return (
    <div className={styles.picker}>
      <h3>Add Units</h3>
      <input
        type="text"
        placeholder="Search units..."
        value={search}
        onChange={(e) => setSearch(e.target.value)}
      />
      <div className={styles.filters}>
        {chapterKeyword && (
          <>
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
          </>
        )}
        {inventory && (
          <>
            {chapterKeyword && <span className={styles.filterSeparator}>|</span>}
            <button
              type="button"
              className={`${styles.filterPill} ${inventoryFilter === "all" ? styles.filterPillActive : ""}`}
              onClick={() => setInventoryFilter("all")}
            >
              All Units
            </button>
            <button
              type="button"
              className={`${styles.filterPill} ${inventoryFilter === "owned" ? styles.filterPillActive : ""}`}
              onClick={() => setInventoryFilter("owned")}
            >
              Owned
            </button>
            <button
              type="button"
              className={`${styles.filterPill} ${inventoryFilter === "missing" ? styles.filterPillActive : ""}`}
              onClick={() => setInventoryFilter("missing")}
            >
              Missing
            </button>
          </>
        )}
      </div>
      {sortedRoles.map((role) => (
        <div key={role} className={styles.roleGroup}>
          <h4 className={styles.roleHeading}>{role}</h4>
          <ul className={styles.list}>
            {filteredByRole[role].sort(sortUnit).map((ds) => {
              const { firstLine, minCost } = getCostForDatasheet(ds.id, costs);
              const unitClass = chapterKeyword ? classifyUnit(ds.id) : null;
              const ownedQty = inventory?.get(ds.id) ?? 0;
              return (
                <li
                  key={ds.id}
                  className={`${styles.item} ${unitClass === "other-chapter" ? styles.deprioritized : ""} ${inventory && ownedQty > 0 ? styles.itemOwned : ""}`}
                >
                  <span className={styles.name}>
                    {ds.name}
                    {unitClass === "chapter" && (
                      <span className={styles.chapterBadge} />
                    )}
                  </span>
                  {inventory && ownedQty > 0 && (
                    <span className={styles.ownedBadge}>{ownedQty} owned</span>
                  )}
                  <span className={styles.costPill}>{minCost}</span>
                  <button className={styles.addBtn} onClick={() => onAdd(ds.id, firstLine)}>+</button>
                </li>
              );
            })}
          </ul>
        </div>
      ))}

      {filteredAlliedFactions.length > 0 && (
        <div className="unit-picker-allied-section">
          <button
            type="button"
            className="unit-picker-allied-toggle"
            onClick={() => setAlliesExpanded(!alliesExpanded)}
          >
            <span className="expand-icon">{alliesExpanded ? "▼" : "▶ "}</span>
            Allied Units
          </button>
          {alliesExpanded &&
            filteredAlliedFactions.map((ally) => (
              <div key={ally.factionId} className="unit-picker-allied-faction">
                <h4 className="unit-picker-ally-heading">
                  {ally.factionName}
                  <span className="ally-type-badge"> - {ally.allyType}</span>
                </h4>
                <ul className="unit-picker-list">
                  {ally.datasheets
                    .sort((a, b) => a.name.localeCompare(b.name))
                    .map((ds) => {
                      const { firstLine, minCost } = getCostForDatasheet(
                        ds.id,
                        alliedCosts,
                      );
                      return (
                        <li
                          key={ds.id}
                          className="unit-picker-item unit-picker-item-allied"
                        >
                          <span className="unit-picker-name">{ds.name}</span>
                          <span className="unit-picker-cost-pill">
                            {minCost}
                          </span>
                          <button
                            className="btn-add-icon"
                            onClick={() => onAdd(ds.id, firstLine, true)}
                          >
                            +
                          </button>
                        </li>
                      );
                    })}
                </ul>
              </div>
            ))}
        </div>
      )}
    </div>
  );
}
