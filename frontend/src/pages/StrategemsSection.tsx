import { useState } from "react";
import type { Stratagem } from "../types";

interface Props {
  stratagems: Stratagem[];
}

export function StrategemsSection({ stratagems }: Props) {
  const [expanded, setExpanded] = useState(false);

  if (stratagems.length === 0) return null;

  return (
    <div className="detachment-stratagems-section">
      <button
        className="btn-toggle detachment-stratagems-toggle"
        onClick={() => setExpanded(!expanded)}
      >
        Stratagems ({stratagems.length}) {expanded ? "▼" : "▶"}
      </button>
      {expanded && (
        <ul className="detachment-stratagems-list">
          {stratagems.map((s) => (
            <li key={s.id} className="detachment-stratagem-item">
              <strong>{s.name}</strong>
              {s.cpCost !== null && <span> ({s.cpCost} CP)</span>}
              {s.phase && <span> - {s.phase}</span>}
              <p dangerouslySetInnerHTML={{ __html: s.description }} />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
