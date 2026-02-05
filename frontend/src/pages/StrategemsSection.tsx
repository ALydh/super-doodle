import { useState } from "react";
import type { Stratagem } from "../types";
import { sanitizeHtml } from "../sanitize";
import styles from "./StrategemsSection.module.css";

interface Props {
  stratagems: Stratagem[];
}

export function StrategemsSection({ stratagems }: Props) {
  const [expanded, setExpanded] = useState(false);

  if (stratagems.length === 0) return null;

  return (
    <div className={styles.section}>
      <button
        className={styles.toggle}
        onClick={() => setExpanded(!expanded)}
      >
        Stratagems ({stratagems.length}) {expanded ? "▼" : "▶"}
      </button>
      {expanded && (
        <ul className={styles.list}>
          {stratagems.map((s) => (
            <li key={s.id}>
              <strong>{s.name}</strong>
              {s.cpCost !== null && <span> ({s.cpCost} CP)</span>}
              {s.phase && <span> - {s.phase}</span>}
              <p dangerouslySetInnerHTML={{ __html: sanitizeHtml(s.description) }} />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
