import type { Datasheet, ModelProfile } from "../../types";
import { ExpandableUnitCard } from "../../components/ExpandableUnitCard";
import styles from "../FactionDetailPage.module.css";

interface Props {
  search: string;
  onSearchChange: (value: string) => void;
  chapterKeyword: string | null;
  noResults: boolean;
  sortedRoles: string[];
  datasheetsByRole: Record<string, Datasheet[]>;
  sortUnit: (a: Datasheet, b: Datasheet) => number;
  classifyUnit: (datasheetId: string) => "chapter" | "generic" | "other-chapter";
  expandedUnit: string | null;
  onUnitToggle: (datasheetId: string) => void;
  profilesByDatasheet: Map<string, ModelProfile[]>;
  costsByDatasheet: Map<string, number>;
}

export function UnitsTab({
  search,
  onSearchChange,
  chapterKeyword,
  noResults,
  sortedRoles,
  datasheetsByRole,
  sortUnit,
  classifyUnit,
  expandedUnit,
  onUnitToggle,
  profilesByDatasheet,
  costsByDatasheet,
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
                  id={`unit-${ds.id}`}
                  className={unitClass === "other-chapter" ? styles.deprioritized : undefined}
                >
                  <ExpandableUnitCard
                    datasheetId={ds.id}
                    datasheetName={ds.name}
                    isExpanded={expandedUnit === ds.id}
                    onToggle={() => onUnitToggle(ds.id)}
                    profiles={profilesByDatasheet.get(ds.id)}
                    points={costsByDatasheet.get(ds.id)}
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
