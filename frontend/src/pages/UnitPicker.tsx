import { useState } from "react";
import type { Datasheet, UnitCost } from "../types";
import { sortByRoleOrder } from "../constants";

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

  const filteredByRole = filtered.reduce<Record<string, typeof filtered>>((acc, ds) => {
    const role = ds.role ?? "Other";
    if (!acc[role]) acc[role] = [];
    acc[role].push(ds);
    return acc;
  }, {});

  const sortedRoles = sortByRoleOrder(Object.keys(filteredByRole));

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
      {sortedRoles.map((role) => (
        <div key={role} className="unit-picker-role-group">
          <h4 className="unit-picker-role-heading">{role}</h4>
          <ul className="unit-picker-list">
            {filteredByRole[role].sort((a, b) => a.name.localeCompare(b.name)).map((ds) => {
              const dsCosts = costs.filter((c) => c.datasheetId === ds.id);
              const firstLine = dsCosts[0]?.line ?? 1;
              const minCost = dsCosts.length > 0 ? Math.min(...dsCosts.map((c) => c.cost)) : 0;
              return (
                <li key={ds.id} className="unit-picker-item">
                  <span className="unit-picker-name">{ds.name}</span>
                  <span className="unit-picker-cost-pill">{minCost}</span>
                  <button className="btn-add-icon" onClick={() => onAdd(ds.id, firstLine)}>+</button>
                </li>
              );
            })}
          </ul>
        </div>
      ))}
    </div>
  );
}
