import { useState } from "react";
import type { DetachmentAbility } from "../types";
import { sanitizeHtml } from "../sanitize";
import styles from "./DetachmentAbilitiesSection.module.css";

interface Props {
  abilities: DetachmentAbility[];
}

export function DetachmentAbilitiesSection({ abilities }: Props) {
  const [expanded, setExpanded] = useState(false);

  if (abilities.length === 0) return null;

  return (
    <div className={styles.section}>
      <button
        className={styles.toggle}
        onClick={() => setExpanded(!expanded)}
      >
        Abilities ({abilities.length}) {expanded ? "▼" : "▶"}
      </button>
      {expanded && (
        <ul className={styles.list}>
          {abilities.map((a) => (
            <li key={a.id}>
              <strong>{a.name}</strong>
              <p dangerouslySetInnerHTML={{ __html: sanitizeHtml(a.description) }} />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
