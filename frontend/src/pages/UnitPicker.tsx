import { useState } from "react";
import type { Datasheet, UnitCost } from "../types";

interface Props {
  datasheets: Datasheet[];
  costs: UnitCost[];
  onAdd: (datasheetId: string, sizeOptionLine: number) => void;
}

export function UnitPicker({ datasheets, costs, onAdd }: Props) {
  const [search, setSearch] = useState("");

  const filtered = datasheets.filter(
    (ds) => !ds.virtual && ds.name.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div data-testid="unit-picker">
      <h3>Add Units</h3>
      <input
        data-testid="unit-search"
        type="text"
        placeholder="Search units..."
        value={search}
        onChange={(e) => setSearch(e.target.value)}
      />
      <ul data-testid="unit-picker-list">
        {filtered.map((ds) => {
          const dsCosts = costs.filter((c) => c.datasheetId === ds.id);
          const firstLine = dsCosts[0]?.line ?? 1;
          const minCost = dsCosts.length > 0 ? Math.min(...dsCosts.map((c) => c.cost)) : 0;
          return (
            <li key={ds.id} data-testid="unit-picker-item">
              <button
                data-testid="add-unit-button"
                onClick={() => onAdd(ds.id, firstLine)}
              >
                Add
              </button>
              {" "}{ds.name} {ds.role && `(${ds.role})`} — {minCost}pts
            </li>
          );
        })}
      </ul>
    </div>
  );
}
