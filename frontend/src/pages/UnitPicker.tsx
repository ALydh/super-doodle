import { useState } from "react";
import type { Datasheet, UnitCost, AlliedFactionInfo } from "../types";
import { sortByRoleOrder } from "../constants";
import styles from "./UnitPicker.module.css";

interface Props {
  datasheets: Datasheet[];
  costs: UnitCost[];
  onAdd: (datasheetId: string, sizeOptionLine: number, isAllied?: boolean) => void;
  alliedFactions?: AlliedFactionInfo[];
  alliedCosts?: UnitCost[];
}

export function UnitPicker({ datasheets, costs, onAdd, alliedFactions = [], alliedCosts = [] }: Props) {
  const [search, setSearch] = useState("");
  const [alliesExpanded, setAlliesExpanded] = useState(false);

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

  const filteredAlliedFactions = alliedFactions.map((ally) => ({
    ...ally,
    datasheets: ally.datasheets.filter(
      (ds) => ds.name.toLowerCase().includes(search.toLowerCase())
    ),
  })).filter((ally) => ally.datasheets.length > 0);

  const getCostForDatasheet = (datasheetId: string, costsArray: UnitCost[]) => {
    const dsCosts = costsArray.filter((c) => c.datasheetId === datasheetId);
    const firstLine = dsCosts[0]?.line ?? 1;
    const minCost = dsCosts.length > 0 ? Math.min(...dsCosts.map((c) => c.cost)) : 0;
    return { firstLine, minCost };
  };

  return (
    <div className={styles.picker}>
      <h3>Add Units</h3>
      <input
        type="text"
        placeholder="Search units..."
        value={search}
        onChange={(e) => setSearch(e.target.value)}
      />
      {sortedRoles.map((role) => (
        <div key={role} className={styles.roleGroup}>
          <h4 className={styles.roleHeading}>{role}</h4>
          <ul className={styles.list}>
            {filteredByRole[role].sort((a, b) => a.name.localeCompare(b.name)).map((ds) => {
              const { firstLine, minCost } = getCostForDatasheet(ds.id, costs);
              return (
                <li key={ds.id} className={styles.item}>
                  <span className={styles.name}>{ds.name}</span>
                  <span className={styles.costPill}>{minCost}</span>
                  <button className={styles.addBtn} onClick={() => onAdd(ds.id, firstLine)}>+</button>
                </li>
              );
            })}
          </ul>
        </div>
      ))}

      {filteredAlliedFactions.length > 0 && (
        <div className="unit-picker-allied-section">
          <button
            type="button"
            className="unit-picker-allied-toggle"
            onClick={() => setAlliesExpanded(!alliesExpanded)}
          >
            <span className="expand-icon">{alliesExpanded ? "▼" : "▶"}</span>
            Allied Units
          </button>
          {alliesExpanded && filteredAlliedFactions.map((ally) => (
            <div key={ally.factionId} className="unit-picker-allied-faction">
              <h4 className="unit-picker-ally-heading">
                {ally.factionName}
                <span className="ally-type-badge">{ally.allyType}</span>
              </h4>
              <ul className="unit-picker-list">
                {ally.datasheets.sort((a, b) => a.name.localeCompare(b.name)).map((ds) => {
                  const { firstLine, minCost } = getCostForDatasheet(ds.id, alliedCosts);
                  return (
                    <li key={ds.id} className="unit-picker-item unit-picker-item-allied">
                      <span className="unit-picker-name">{ds.name}</span>
                      <span className="unit-picker-cost-pill">{minCost}</span>
                      <button className="btn-add-icon" onClick={() => onAdd(ds.id, firstLine, true)}>+</button>
                    </li>
                  );
                })}
              </ul>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
