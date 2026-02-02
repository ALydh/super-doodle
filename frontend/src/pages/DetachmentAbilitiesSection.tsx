import { useState } from "react";
import type { DetachmentAbility } from "../types";
import { sanitizeHtml } from "../sanitize";

interface Props {
  abilities: DetachmentAbility[];
}

export function DetachmentAbilitiesSection({ abilities }: Props) {
  const [expanded, setExpanded] = useState(false);

  if (abilities.length === 0) return null;

  return (
    <div className="detachment-abilities-section">
      <button
        className="btn-toggle detachment-abilities-toggle"
        onClick={() => setExpanded(!expanded)}
      >
        Abilities ({abilities.length}) {expanded ? "▼" : "▶"}
      </button>
      {expanded && (
        <ul className="detachment-abilities-list">
          {abilities.map((a) => (
            <li key={a.id} className="detachment-ability-item">
              <strong>{a.name}</strong>
              <p dangerouslySetInnerHTML={{ __html: sanitizeHtml(a.description) }} />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
