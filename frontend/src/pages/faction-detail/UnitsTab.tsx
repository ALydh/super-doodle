import type { Datasheet, ModelProfile } from "../../types";
import { ExpandableUnitCard } from "../../components/ExpandableUnitCard";
import { SM_CHAPTERS } from "../../chapters";
import styles from "../FactionDetailPage.module.css";

interface Props {
  search: string;
  onSearchChange: (value: string) => void;
  isSM: boolean;
  chapterId: string | null;
  onChapterIdChange: (value: string | null) => void;
  chapterKeyword: string | null;
  chapterFilter: "all" | "chapter";
  onChapterFilterChange: (value: "all" | "chapter") => void;
  noResults: boolean;
  sortedRoles: string[];
  datasheetsByRole: Record<string, Datasheet[]>;
  sortUnit: (a: Datasheet, b: Datasheet) => number;
  classifyUnit: (datasheetId: string) => "chapter" | "generic" | "other-chapter";
  expandedUnit: string | null;
  onUnitToggle: (datasheetId: string) => void;
  profilesByDatasheet: Map<string, ModelProfile[]>;
}

export function UnitsTab({
  search,
  onSearchChange,
  isSM,
  chapterId,
  onChapterIdChange,
  chapterKeyword,
  chapterFilter,
  onChapterFilterChange,
  noResults,
  sortedRoles,
  datasheetsByRole,
  sortUnit,
  classifyUnit,
  expandedUnit,
  onUnitToggle,
  profilesByDatasheet,
}: Props) {
  return (
    <div className={styles.unitsTab}>
      <input
        type="text"
        className={styles.searchInput}
        placeholder="Search units..."
        value={search}
        onChange={(e) => onSearchChange(e.target.value)}
      />
      {isSM && (
        <div className={styles.chapterControls}>
          <select
            className={styles.chapterSelect}
            value={chapterId ?? ""}
            onChange={(e) => {
              const val = e.target.value || null;
              onChapterIdChange(val);
              if (!val) onChapterFilterChange("all");
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
                onClick={() => onChapterFilterChange("all")}
              >
                All
              </button>
              <button
                type="button"
                className={`${styles.filterPill} ${chapterFilter === "chapter" ? styles.filterPillActive : ""}`}
                onClick={() => onChapterFilterChange("chapter")}
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
                    onToggle={() => onUnitToggle(ds.id)}
                    profiles={profilesByDatasheet.get(ds.id)}
                  />
                </div>
              );
            })}
          </div>
        </section>
      ))}
    </div>
  );
}
