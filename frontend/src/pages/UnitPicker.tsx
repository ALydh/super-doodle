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
type RoleFilter = "all" | string;

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
  const [chapterFilter, setChapterFilter] = useState<ChapterFilter>("all");
  const [inventoryFilter, setInventoryFilter] = useState<InventoryFilter>("all");
  const [roleFilter, setRoleFilter] = useState<RoleFilter>("all");
  const [keywordFilter, setKeywordFilter] = useState("all");
  const [unitsExpanded, setUnitsExpanded] = useState(true);
  const [alliedExpanded, setAlliedExpanded] = useState<Record<string, boolean>>({});

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

  const matchesSearch = (name: string) =>
    name.toLowerCase().includes(search.toLowerCase());

  const availableRoles = sortByRoleOrder(
    [...new Set(datasheets.filter((ds) => !ds.virtual).map((ds) => ds.role ?? "Other"))]
  );

  const availableKeywords = useMemo(() => {
    const set = new Set<string>();
    for (const ds of datasheets) {
      if (ds.virtual) continue;
      for (const kw of keywordsByDatasheet.get(ds.id) ?? []) {
        if (!kw.isFactionKeyword && kw.keyword && !kw.model) set.add(kw.keyword);
      }
    }
    return [...set].sort();
  }, [datasheets, keywordsByDatasheet]);

  const hasKeyword = (datasheetId: string) =>
    keywordsByDatasheet.get(datasheetId)?.some((k) => k.keyword === keywordFilter) ?? false;

  const filtered = datasheets.filter((ds) => {
    if (ds.virtual) return false;
    if (!matchesSearch(ds.name)) return false;
    if (roleFilter !== "all" && (ds.role ?? "Other") !== roleFilter) return false;
    if (keywordFilter !== "all" && !hasKeyword(ds.id)) return false;
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
      datasheets: ally.datasheets
        .filter((ds) => {
          if (!matchesSearch(ds.name)) return false;
          if (keywordFilter !== "all" && !hasKeyword(ds.id)) return false;
          if (inventory && inventoryFilter !== "all") {
            const qty = inventory.get(ds.id) ?? 0;
            if (inventoryFilter === "owned" && qty === 0) return false;
            if (inventoryFilter === "missing" && qty > 0) return false;
          }
          return true;
        })
        .sort((a, b) => a.name.localeCompare(b.name)),
    }))
    .filter((ally) => ally.datasheets.length > 0);

  const getCost = (datasheetId: string, costsArray: UnitCost[]) => {
    const dsCosts = costsArray.filter((c) => c.datasheetId === datasheetId);
    const firstLine = dsCosts[0]?.line ?? 1;
    const minCost =
      dsCosts.length > 0 ? Math.min(...dsCosts.map((c) => c.cost)) : 0;
    return { firstLine, minCost };
  };

  return (
    <div className={styles.picker}>
      <input
        type="text"
        placeholder="Search units..."
        value={search}
        onChange={(e) => setSearch(e.target.value)}
      />
      <div className={styles.filters}>
        <select
          className={styles.filterSelect}
          value={roleFilter}
          onChange={(e) => setRoleFilter(e.target.value)}
        >
          <option value="all">All roles</option>
          {availableRoles.map((role) => (
            <option key={role} value={role}>{role}</option>
          ))}
        </select>
        <select
          className={styles.filterSelect}
          value={keywordFilter}
          onChange={(e) => setKeywordFilter(e.target.value)}
        >
          <option value="all">All keywords</option>
          {availableKeywords.map((kw) => (
            <option key={kw} value={kw}>{kw}</option>
          ))}
        </select>
        {chapterKeyword && (
          <select
            className={styles.filterSelect}
            value={chapterFilter}
            onChange={(e) => setChapterFilter(e.target.value as ChapterFilter)}
          >
            <option value="all">All chapters</option>
            <option value="chapter">Chapter only</option>
          </select>
        )}
        {inventory && (
          <select
            className={styles.filterSelect}
            value={inventoryFilter}
            onChange={(e) => setInventoryFilter(e.target.value as InventoryFilter)}
          >
            <option value="all">All inventory</option>
            <option value="owned">Owned</option>
            <option value="missing">Missing</option>
          </select>
        )}
      </div>

      <button type="button" className={styles.toggle} onClick={() => setUnitsExpanded(!unitsExpanded)}>
        Units {unitsExpanded ? "▼" : "▶"}
      </button>
      {unitsExpanded && sortedRoles.map((role) => (
        <div key={role} className={styles.roleGroup}>
          <h4 className={styles.roleHeading}>{role}</h4>
          <ul className={styles.list}>
            {filteredByRole[role].sort(sortUnit).map((ds) => {
              const { firstLine, minCost } = getCost(ds.id, costs);
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

      {filteredAlliedFactions.map((ally) => (
        <div key={ally.factionId}>
          <button
            type="button"
            className={styles.toggle}
            onClick={() => setAlliedExpanded((prev) => ({ ...prev, [ally.factionId]: !prev[ally.factionId] }))}
          >
            {ally.factionName} <span className={styles.allyTypeBadge}>({ally.allyType})</span>{" "}
            {alliedExpanded[ally.factionId] ? "▼" : "▶"}
          </button>
          {alliedExpanded[ally.factionId] && (
            <ul className={styles.list}>
              {ally.datasheets.map((ds) => {
                const { firstLine, minCost } = getCost(ds.id, alliedCosts);
                return (
                  <li key={ds.id} className={styles.item}>
                    <span className={styles.name}>{ds.name}</span>
                    <span className={styles.costPill}>{minCost}</span>
                    <button className={styles.addBtn} onClick={() => onAdd(ds.id, firstLine, true)}>+</button>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      ))}
    </div>
  );
}
