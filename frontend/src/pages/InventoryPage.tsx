import { useEffect, useState, useMemo, useCallback } from "react";
import { useParams, Link } from "react-router-dom";
import type { Datasheet, DatasheetDetail, UnitCost } from "../types";
import {
  fetchDatasheetDetailsByFaction,
  fetchFactions,
  fetchInventory,
  upsertInventoryEntry,
} from "../api";
import { useAuth } from "../context/useAuth";
import { getFactionTheme } from "../factionTheme";
import { sortByRoleOrder } from "../constants";
import styles from "./InventoryPage.module.css";

type InventoryFilter = "all" | "owned" | "missing";

const parseModels = (desc: string): number => {
  const match = desc.match(/(\d+)\s*model/i);
  return match ? parseInt(match[1], 10) : 1;
};

export function InventoryPage() {
  const { factionId } = useParams<{ factionId: string }>();
  const { user, loading: authLoading } = useAuth();

  const [factionName, setFactionName] = useState<string>("");
  const [datasheets, setDatasheets] = useState<Datasheet[]>([]);
  const [costsByDatasheet, setCostsByDatasheet] = useState<Map<string, UnitCost[]>>(new Map());
  const [inventory, setInventory] = useState<Map<string, number>>(new Map());
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState<InventoryFilter>("all");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!factionId || !user) return;
    let cancelled = false;

    Promise.all([
      fetchDatasheetDetailsByFaction(factionId),
      fetchInventory(),
      fetchFactions(),
    ])
      .then(([details, inv, factions]) => {
        if (cancelled) return;
        const nonVirtual = details.filter((d: DatasheetDetail) => !d.datasheet.virtual);
        setDatasheets(nonVirtual.map((d) => d.datasheet));
        const costs = new Map<string, UnitCost[]>();
        for (const d of nonVirtual) {
          costs.set(d.datasheet.id, d.costs);
        }
        setCostsByDatasheet(costs);
        const faction = factions.find((f) => f.id === factionId);
        setFactionName(faction?.name ?? factionId);

        const invMap = new Map<string, number>();
        for (const entry of inv) {
          invMap.set(entry.datasheetId, entry.quantity);
        }
        setInventory(invMap);
      })
      .catch((e) => {
        if (!cancelled) setError(e.message);
      });

    return () => { cancelled = true; };
  }, [factionId, user]);

  const handleQuantityChange = useCallback(
    (datasheetId: string, delta: number) => {
      const current = inventory.get(datasheetId) ?? 0;
      const next = Math.max(0, current + delta);
      setInventory((prev) => {
        const updated = new Map(prev);
        if (next === 0) {
          updated.delete(datasheetId);
        } else {
          updated.set(datasheetId, next);
        }
        return updated;
      });
      upsertInventoryEntry(datasheetId, next).catch(() => {
        // revert on failure
        setInventory((prev) => {
          const reverted = new Map(prev);
          if (current === 0) {
            reverted.delete(datasheetId);
          } else {
            reverted.set(datasheetId, current);
          }
          return reverted;
        });
      });
    },
    [inventory],
  );

  const filtered = useMemo(() => {
    return datasheets.filter((ds) => {
      if (!ds.name.toLowerCase().includes(search.toLowerCase())) return false;
      const qty = inventory.get(ds.id) ?? 0;
      if (filter === "owned" && qty === 0) return false;
      if (filter === "missing" && qty > 0) return false;
      return true;
    });
  }, [datasheets, search, filter, inventory]);

  const byRole = useMemo(() => {
    return filtered.reduce<Record<string, Datasheet[]>>((acc, ds) => {
      const role = ds.role ?? "Other";
      if (!acc[role]) acc[role] = [];
      acc[role].push(ds);
      return acc;
    }, {});
  }, [filtered]);

  const sortedRoles = sortByRoleOrder(Object.keys(byRole));

  const ownedCount = useMemo(() => {
    let count = 0;
    for (const ds of datasheets) {
      if ((inventory.get(ds.id) ?? 0) > 0) count++;
    }
    return count;
  }, [datasheets, inventory]);

  const totalModels = useMemo(() => {
    let total = 0;
    for (const [, qty] of inventory) {
      total += qty;
    }
    return total;
  }, [inventory]);

  const minModelsByDatasheet = useMemo(() => {
    const map = new Map<string, number>();
    for (const [id, costs] of costsByDatasheet) {
      if (costs.length > 0) {
        map.set(id, Math.min(...costs.map((c) => parseModels(c.description))));
      }
    }
    return map;
  }, [costsByDatasheet]);

  const pointsPerUnit = useMemo(() => {
    const map = new Map<string, number>();
    for (const [datasheetId, qty] of inventory) {
      const costs = costsByDatasheet.get(datasheetId);
      if (!costs || costs.length === 0 || qty === 0) continue;
      const options = costs
        .map((c) => ({ models: parseModels(c.description), cost: c.cost }))
        .sort((a, b) => b.models - a.models);
      let points = 0;
      let remaining = qty;
      for (const opt of options) {
        if (opt.models <= 0) continue;
        const fits = Math.floor(remaining / opt.models);
        points += fits * opt.cost;
        remaining -= fits * opt.models;
      }
      map.set(datasheetId, points);
    }
    return map;
  }, [inventory, costsByDatasheet]);

  const handleExportCsv = useCallback(() => {
    const rows: string[] = ["name,role,quantity,points"];
    for (const ds of datasheets) {
      const qty = inventory.get(ds.id) ?? 0;
      if (qty === 0) continue;
      const pts = pointsPerUnit.get(ds.id) ?? "";
      const name = `"${ds.name.replace(/"/g, '""')}"`;
      const role = `"${(ds.role ?? "Other").replace(/"/g, '""')}"`;
      rows.push(`${name},${role},${qty},${pts}`);
    }
    const csv = rows.join("\n");
    const blob = new Blob([csv], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${factionName.replace(/\s+/g, "_")}_inventory.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }, [datasheets, inventory, pointsPerUnit, factionName]);

  const totalPoints = useMemo(() => {
    let total = 0;
    for (const [datasheetId, qty] of inventory) {
      const costs = costsByDatasheet.get(datasheetId);
      if (!costs || costs.length === 0 || qty === 0) continue;

      const options = costs
        .map((c) => ({ models: parseModels(c.description), cost: c.cost }))
        .sort((a, b) => b.models - a.models);

      let remaining = qty;
      for (const opt of options) {
        if (opt.models <= 0) continue;
        const fits = Math.floor(remaining / opt.models);
        total += fits * opt.cost;
        remaining -= fits * opt.models;
      }
    }
    return total;
  }, [inventory, costsByDatasheet]);

  if (authLoading) return <div>Loading...</div>;

  if (!user) {
    return (
      <div>
        <p>You must be logged in to manage your inventory.</p>
        <Link to="/login">Login</Link> or <Link to="/register">Register</Link>
      </div>
    );
  }

  if (error) return <div className="error-message">{error}</div>;

  const factionTheme = getFactionTheme(factionId);

  return (
    <div data-faction={factionTheme} className={styles.page}>
      {factionTheme && (
        <img
          src={`/icons/${factionTheme}.svg`}
          alt=""
          className={styles.bgIcon}
          aria-hidden="true"
        />
      )}
      <div className={styles.header}>
        <div className={styles.headerInfo}>
          {factionTheme && (
            <img
              src={`/icons/${factionTheme}.svg`}
              alt=""
              className={styles.headerIcon}
            />
          )}
          <h1 className={styles.title}>{factionName} - Inventory</h1>
        </div>
        <button
          type="button"
          className={styles.exportBtn}
          onClick={handleExportCsv}
          disabled={inventory.size === 0}
        >
          Export CSV
        </button>
      </div>

      <div className={styles.summary}>
        <div className={styles.summaryItem}>
          <span className={styles.summaryLabel}>Unique units owned</span>
          <span className={styles.summaryValue}>{ownedCount} / {datasheets.length}</span>
        </div>
        <div className={styles.summaryItem}>
          <span className={styles.summaryLabel}>Total models</span>
          <span className={styles.summaryValue}>{totalModels}</span>
        </div>
        <div className={styles.summaryItem}>
          <span className={styles.summaryLabel}>Total points</span>
          <span className={styles.summaryValue}>{totalPoints}</span>
        </div>
      </div>

      <div className={styles.controls}>
        <input
          type="text"
          className={styles.searchInput}
          placeholder="Search units..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <div className={styles.filterPills}>
          <button
            type="button"
            className={`${styles.filterPill} ${filter === "all" ? styles.filterPillActive : ""}`}
            onClick={() => setFilter("all")}
          >
            All
          </button>
          <button
            type="button"
            className={`${styles.filterPill} ${filter === "owned" ? styles.filterPillActive : ""}`}
            onClick={() => setFilter("owned")}
          >
            Owned
          </button>
          <button
            type="button"
            className={`${styles.filterPill} ${filter === "missing" ? styles.filterPillActive : ""}`}
            onClick={() => setFilter("missing")}
          >
            Missing
          </button>
        </div>
      </div>

      {filtered.length === 0 && datasheets.length > 0 && (
        <p className={styles.noResults}>No units found</p>
      )}

      {sortedRoles.map((role) => (
        <section key={role} className={styles.roleSection}>
          <h2 className={styles.roleHeading}>{role}</h2>
          <div className={styles.unitList}>
            {byRole[role]
              .sort((a, b) => a.name.localeCompare(b.name))
              .map((ds) => {
                const qty = inventory.get(ds.id) ?? 0;
                const step = minModelsByDatasheet.get(ds.id) ?? 1;
                return (
                  <div
                    key={ds.id}
                    className={`${styles.unitItem} ${qty > 0 ? styles.unitItemOwned : ""}`}
                  >
                    <span className={styles.unitName}>{ds.name}</span>
                    {qty > 0 && pointsPerUnit.has(ds.id) && (
                      <span className={styles.unitPoints}>{pointsPerUnit.get(ds.id)}pts</span>
                    )}
                    <div className={styles.quantityControl}>
                      <button
                        className={styles.quantityBtn}
                        onClick={() => handleQuantityChange(ds.id, -step)}
                        disabled={qty === 0}
                      >
                        -{step > 1 ? step : ""}
                      </button>
                      <span
                        className={`${styles.quantityValue} ${qty === 0 ? styles.quantityValueZero : ""}`}
                      >
                        {qty}
                      </span>
                      <button
                        className={`${styles.quantityBtn} ${qty === 0 ? "" : styles.quantityBtnActive}`}
                        onClick={() => handleQuantityChange(ds.id, step)}
                      >
                        +{step > 1 ? step : ""}
                      </button>
                    </div>
                  </div>
                );
              })}
          </div>
        </section>
      ))}
    </div>
  );
}
