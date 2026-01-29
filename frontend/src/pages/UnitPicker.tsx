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
    <div className="unit-picker">
      <h3>Add Units</h3>
      <input
        className="unit-search"
        type="text"
        placeholder="Search units..."
        value={search}
        onChange={(e) => setSearch(e.target.value)}
      />
      <ul className="unit-picker-list">
        {filtered.map((ds) => {
          const dsCosts = costs.filter((c) => c.datasheetId === ds.id);
          const firstLine = dsCosts[0]?.line ?? 1;
          const minCost = dsCosts.length > 0 ? Math.min(...dsCosts.map((c) => c.cost)) : 0;
          return (
            <li key={ds.id} className="unit-picker-item">
              <button
                className="btn-add add-unit-button"
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
