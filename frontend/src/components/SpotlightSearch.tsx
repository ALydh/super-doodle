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
import { useFocusTrap } from "../hooks/useFocusTrap";
import { buildSpotlightSections } from "../spotlightSections";
import type { ResultSection } from "../spotlightSections";
import styles from "./SpotlightSearch.module.css";

interface SpotlightSearchProps {
  open: boolean;
  onClose: () => void;
}

export function SpotlightSearch({ open, onClose }: SpotlightSearchProps) {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { toggleCompact } = useCompactMode();
  const trapRef = useFocusTrap(open);

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

  const sections = useMemo<ResultSection[]>(() =>
    buildSpotlightSections({
      search, armies, factions, datasheets, stratagems, enhancements,
      weaponAbilities, coreAbilities, factionNameMap, user, go, onClose, toggleCompact,
    }),
    [search, factions, datasheets, stratagems, enhancements, weaponAbilities, coreAbilities, armies, factionNameMap, user, go, onClose, toggleCompact],
  );

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
    <div className={styles.backdrop} onClick={onClose} ref={trapRef}>
      <div className={styles.panel} role="dialog" aria-modal="true" aria-label="Search" onClick={(e) => e.stopPropagation()}>
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
