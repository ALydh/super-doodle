import type { Stratagem, DetachmentInfo } from "../../types";
import { StratagemCard } from "../../components/StratagemCard";
import styles from "../FactionDetailPage.module.css";

interface Props {
  filteredStratagems: Stratagem[];
  detachments: DetachmentInfo[];
  phases: string[];
  stratagemDetachmentFilter: string;
  onDetachmentFilterChange: (value: string) => void;
  stratagemPhaseFilter: string;
  onPhaseFilterChange: (value: string) => void;
  turns: string[];
  stratagemTurnFilter: string;
  onTurnFilterChange: (value: string) => void;
}

export function StrategemsTab({
  filteredStratagems,
  detachments,
  phases,
  stratagemDetachmentFilter,
  onDetachmentFilterChange,
  stratagemPhaseFilter,
  onPhaseFilterChange,
  turns,
  stratagemTurnFilter,
  onTurnFilterChange,
}: Props) {
  return (
    <div>
      <div className={styles.filters}>
        <label>
          Detachment:
          <select
            value={stratagemDetachmentFilter}
            onChange={(e) => onDetachmentFilterChange(e.target.value)}
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
            onChange={(e) => onPhaseFilterChange(e.target.value)}
          >
            <option value="all">All Phases</option>
            {phases.map((p) => (
              <option key={p} value={p}>{p}</option>
            ))}
          </select>
        </label>
        <label>
          Turn:
          <select
            value={stratagemTurnFilter}
            onChange={(e) => onTurnFilterChange(e.target.value)}
          >
            <option value="all">All Turns</option>
            {turns.map((t) => (
              <option key={t} value={t}>{t}</option>
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
  );
}
