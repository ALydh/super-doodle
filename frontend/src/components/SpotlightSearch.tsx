import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import type { Faction, Datasheet, Stratagem, Enhancement, WeaponAbility, CoreAbility, ArmySummary } from "../types";
import {
  fetchFactions, fetchAllDatasheets, fetchAllStratagems, fetchAllEnhancements,
  fetchWeaponAbilities, fetchCoreAbilities, fetchAllArmies,
} from "../api";
import { useAuth } from "../context/useAuth";
import { useCompactMode } from "../context/CompactModeContext";
import { sanitizeHtml } from "../sanitize";
import styles from "./SpotlightSearch.module.css";

interface SpotlightSearchProps {
  open: boolean;
  onClose: () => void;
}

const MAX_RESULTS_PER_SECTION = 10;

/**
 * Returns a fuzzy match score for how well `query` matches `target`.
 * Returns null if query characters cannot all be found in order within target.
 *
 * Scoring:
 *  - Exact substring match scores highest (1000 + start-of-string bonus)
 *  - Otherwise, all query chars must appear in order (subsequence match)
 *  - Consecutive matched characters earn increasing bonuses
 *  - Matches at word boundaries (start or after a space) earn extra points
 */
function fuzzyScore(target: string, query: string): number | null {
  if (!query) return 0;
  const t = target.toLowerCase();
  const q = query.toLowerCase();

  // Exact substring match — highest priority
  const exactIdx = t.indexOf(q);
  if (exactIdx !== -1) {
    return 1000 + (exactIdx === 0 ? 50 : 0);
  }

  // Fuzzy: every char in q must appear in t in order
  let score = 0;
  let ti = 0;
  let qi = 0;
  let consecutive = 0;
  let lastTi = -1;

  while (ti < t.length && qi < q.length) {
    if (t[ti] === q[qi]) {
      score += 1;
      if (lastTi === ti - 1) {
        consecutive++;
        score += consecutive; // growing bonus for runs of consecutive chars
      } else {
        consecutive = 0;
      }
      // word-boundary bonus
      if (ti === 0 || t[ti - 1] === " ") {
        score += 5;
      }
      lastTi = ti;
      qi++;
    }
    ti++;
  }

  return qi < q.length ? null : score; // null = not all chars matched
}

type ResultItem =
  | { type: "navigate"; name: string; subtitle?: string; action: () => void }
  | { type: "expand"; name: string; subtitle?: string; description: string };

interface ResultSection {
  title: string;
  items: ResultItem[];
}

export function SpotlightSearch({ open, onClose }: SpotlightSearchProps) {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { toggleCompact } = useCompactMode();

  const [factions, setFactions] = useState<Faction[]>([]);
  const [datasheets, setDatasheets] = useState<Datasheet[]>([]);
  const [stratagems, setStratagems] = useState<Stratagem[]>([]);
  const [enhancements, setEnhancements] = useState<Enhancement[]>([]);
  const [weaponAbilities, setWeaponAbilities] = useState<WeaponAbility[]>([]);
  const [coreAbilities, setCoreAbilities] = useState<CoreAbility[]>([]);
  const [armies, setArmies] = useState<ArmySummary[]>([]);

  const [search, setSearch] = useState("");
  const [loaded, setLoaded] = useState(false);
  const [activeIndex, setActiveIndex] = useState(0);
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());
  const inputRef = useRef<HTMLInputElement>(null);
  const resultsRef = useRef<HTMLDivElement>(null);

  const factionNameMap = useMemo(() => {
    const map = new Map<string, string>();
    for (const f of factions) map.set(f.id, f.name);
    return map;
  }, [factions]);

  useEffect(() => {
    if (loaded) return;
    Promise.allSettled([
      fetchFactions(),
      fetchAllDatasheets(),
      fetchAllStratagems(),
      fetchAllEnhancements(),
      fetchWeaponAbilities(),
      fetchCoreAbilities(),
      fetchAllArmies(),
    ]).then(([f, d, s, e, wa, ca, a]) => {
      if (f.status === "fulfilled") setFactions(f.value);
      if (d.status === "fulfilled") setDatasheets(d.value);
      if (s.status === "fulfilled") setStratagems(s.value);
      if (e.status === "fulfilled") setEnhancements(e.value);
      if (wa.status === "fulfilled") setWeaponAbilities(wa.value);
      if (ca.status === "fulfilled") setCoreAbilities(ca.value);
      if (a.status === "fulfilled") setArmies(a.value);
      setLoaded(true);
    });
  }, [loaded]);

  useEffect(() => {
    if (open) {
      setSearch("");
      setActiveIndex(0);
      setExpandedIds(new Set());
      requestAnimationFrame(() => inputRef.current?.focus());
    }
  }, [open]);

  const go = useCallback((path: string) => {
    navigate(path);
    onClose();
  }, [navigate, onClose]);

  const sections = useMemo<ResultSection[]>(() => {
    const lowerSearch = search.toLowerCase();
    const hasSearch = lowerSearch.length > 0;

    const filterSorted = <T,>(items: T[], getName: (item: T) => string) => {
      if (!hasSearch) {
        return items
          .sort((a, b) => getName(a).localeCompare(getName(b)))
          .slice(0, MAX_RESULTS_PER_SECTION);
      }
      return items
        .map((item) => ({ item, score: fuzzyScore(getName(item), lowerSearch) }))
        .filter(({ score }) => score !== null)
        .sort((a, b) =>
          b.score !== a.score
            ? (b.score ?? 0) - (a.score ?? 0)
            : getName(a.item).localeCompare(getName(b.item))
        )
        .map(({ item }) => item)
        .slice(0, MAX_RESULTS_PER_SECTION);
    };

    const result: ResultSection[] = [];

    // Armies
    const filteredArmies = filterSorted(armies, (a) => a.name);
    if (filteredArmies.length > 0) {
      result.push({
        title: "Armies",
        items: filteredArmies.map((a) => ({
          type: "navigate",
          name: a.name,
          subtitle: `${a.totalPoints} pts`,
          action: () => go(`/armies/${a.id}`),
        })),
      });
    }

    // Commands
    const commandDefs: { name: string; action: () => void }[] = [
      { name: "Home", action: () => go("/") },
      { name: "Toggle compact mode", action: () => { toggleCompact(); onClose(); } },
    ];
    if (user) {
      commandDefs.push({ name: "Admin", action: () => go("/admin") });
    } else {
      commandDefs.push({ name: "Login", action: () => go("/login") });
      commandDefs.push({ name: "Register", action: () => go("/register") });
    }
    const filteredCommands = filterSorted(commandDefs, (c) => c.name);
    if (filteredCommands.length > 0) {
      result.push({
        title: "Commands",
        items: filteredCommands.map((c) => ({ type: "navigate", name: c.name, action: c.action })),
      });
    }

    if (!hasSearch) return result;

    // Factions
    const filteredFactions = filterSorted(factions, (f) => f.name);
    if (filteredFactions.length > 0) {
      result.push({
        title: "Factions",
        items: filteredFactions.map((f) => ({ type: "navigate", name: f.name, action: () => go(`/factions/${f.id}`) })),
      });
    }

    // Datasheets
    const filteredDatasheets = filterSorted(datasheets, (d) => d.name);
    if (filteredDatasheets.length > 0) {
      result.push({
        title: "Datasheets",
        items: filteredDatasheets.map((d) => ({
          type: "navigate",
          name: d.name,
          subtitle: d.factionId ? factionNameMap.get(d.factionId) : undefined,
          action: () => go(d.factionId ? `/factions/${d.factionId}?unit=${d.id}` : "/"),
        })),
      });
    }

    // Stratagems
    const filteredStratagems = filterSorted(stratagems, (s) => s.name);
    if (filteredStratagems.length > 0) {
      result.push({
        title: "Stratagems",
        items: filteredStratagems.map((s) => ({
          type: "expand",
          name: s.name,
          subtitle: s.cpCost != null ? `${s.cpCost} CP` : undefined,
          description: s.description,
        })),
      });
    }

    // Enhancements
    const filteredEnhancements = filterSorted(enhancements, (e) => e.name);
    if (filteredEnhancements.length > 0) {
      result.push({
        title: "Enhancements",
        items: filteredEnhancements.map((e) => ({
          type: "expand",
          name: e.name,
          subtitle: `${e.cost} pts`,
          description: e.description,
        })),
      });
    }

    // Weapon Abilities
    const filteredWeapon = filterSorted(weaponAbilities, (a) => a.name);
    if (filteredWeapon.length > 0) {
      result.push({
        title: "Weapon Abilities",
        items: filteredWeapon.map((a) => ({ type: "expand", name: a.name, description: a.description })),
      });
    }

    // Core Abilities
    const filteredCore = filterSorted(coreAbilities, (a) => a.name);
    if (filteredCore.length > 0) {
      result.push({
        title: "Core Abilities",
        items: filteredCore.map((a) => ({ type: "expand", name: a.name, description: a.description })),
      });
    }

    return result;
  }, [search, factions, datasheets, stratagems, enhancements, weaponAbilities, coreAbilities, armies, factionNameMap, user, go, onClose, toggleCompact]);

  const flatItems = useMemo(() => sections.flatMap((s) => s.items), [sections]);

  useEffect(() => {
    setActiveIndex(0);
    setExpandedIds(new Set());
  }, [search]);

  const activateItem = useCallback((index: number) => {
    const item = flatItems[index];
    if (!item) return;
    if (item.type === "navigate") {
      item.action();
    } else {
      const id = `${item.name}-${index}`;
      setExpandedIds((prev) => {
        const next = new Set(prev);
        if (next.has(id)) next.delete(id); else next.add(id);
        return next;
      });
    }
  }, [flatItems]);

  useEffect(() => {
    if (!open) return;
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        e.preventDefault();
        onClose();
        return;
      }
      if (e.key === "ArrowDown") {
        e.preventDefault();
        setActiveIndex((prev) => Math.min(prev + 1, flatItems.length - 1));
        return;
      }
      if (e.key === "ArrowUp") {
        e.preventDefault();
        setActiveIndex((prev) => Math.max(prev - 1, 0));
        return;
      }
      if (e.key === "Enter") {
        e.preventDefault();
        activateItem(activeIndex);
      }
    };
    document.addEventListener("keydown", handleKey);
    return () => document.removeEventListener("keydown", handleKey);
  }, [open, onClose, flatItems, activeIndex, activateItem]);

  useEffect(() => {
    const el = resultsRef.current?.querySelector(`[data-index="${activeIndex}"]`);
    el?.scrollIntoView({ block: "nearest" });
  }, [activeIndex]);

  if (!open) return null;

  const noResults = search.length > 0 && flatItems.length === 0;
  let globalIndex = 0;

  return (
    <div className={styles.backdrop} onClick={onClose}>
      <div className={styles.panel} onClick={(e) => e.stopPropagation()}>
        <input
          ref={inputRef}
          className={styles.search}
          type="text"
          placeholder="Search..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <div className={styles.results} ref={resultsRef}>
          {noResults && <p className={styles.empty}>No results found.</p>}

          {sections.map((section) => (
            <div className={styles.section} key={section.title}>
              <h2>{section.title}</h2>
              {section.items.map((item) => {
                const idx = globalIndex++;
                const isActive = idx === activeIndex;
                const expandId = `${item.name}-${idx}`;
                const isExpanded = expandedIds.has(expandId);

                return (
                  <div
                    key={idx}
                    data-index={idx}
                    className={`${styles.entry} ${isActive ? styles.entryActive : ""}`}
                    onClick={() => activateItem(idx)}
                  >
                    <div className={styles.entryHeader}>
                      <div>
                        <span className={styles.entryName}>{item.name}</span>
                        {item.subtitle && <span className={styles.subtitle}>{item.subtitle}</span>}
                      </div>
                      {item.type === "navigate" ? (
                        <span className={styles.entryArrow}>&#8594;</span>
                      ) : (
                        <span className={styles.entryToggle} data-open={isExpanded || undefined}>&#9660;</span>
                      )}
                    </div>
                    {item.type === "expand" && isExpanded && (
                      <div
                        className={styles.entryDescription}
                        dangerouslySetInnerHTML={{ __html: sanitizeHtml(item.description) }}
                      />
                    )}
                  </div>
                );
              })}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
