import { useEffect, useState } from "react";
import { fetchRevisionDiff } from "../api";
import type { RevisionDiff } from "../types";
import styles from "./RevisionDiffView.module.css";

interface RevisionDiffViewProps {
  oldId: string;
  newId: string;
}

type Tab = "points" | "units" | "stats" | "enhancements" | "stratagems" | "abilities";

function TextDiff({ oldText, newText }: { oldText: string | null; newText: string | null }) {
  if (!oldText && !newText) return null;
  if (!oldText) return <div className={styles.diffNew}>{newText}</div>;
  if (!newText) return <div className={styles.diffOld}>{oldText}</div>;
  if (oldText === newText) return null;
  return (
    <div className={styles.diffBlock}>
      <div className={styles.diffOld}>{oldText}</div>
      <div className={styles.diffNew}>{newText}</div>
    </div>
  );
}

function ExpandableRow({ children, detail }: { children: React.ReactNode; detail: React.ReactNode }) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <tr onClick={() => setOpen(!open)} className={styles.expandableRow}>
        {children}
      </tr>
      {open && (
        <tr>
          <td colSpan={99} className={styles.detailCell}>{detail}</td>
        </tr>
      )}
    </>
  );
}

export function RevisionDiffView({ oldId, newId }: RevisionDiffViewProps) {
  const [diff, setDiff] = useState<RevisionDiff | null>(null);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState<Tab>("points");

  useEffect(() => {
    setLoading(true);
    fetchRevisionDiff(oldId, newId)
      .then(setDiff)
      .finally(() => setLoading(false));
  }, [oldId, newId]);

  if (loading) return <div className={styles.loading}>Loading diff...</div>;
  if (!diff) return <div className={styles.loading}>Failed to load diff</div>;

  const tabs: { key: Tab; label: string; count: number }[] = [
    { key: "points", label: "Points", count: diff.pointChanges.length },
    { key: "units", label: "Units", count: diff.unitChanges.length },
    { key: "stats", label: "Stats", count: diff.statChanges.length },
    { key: "enhancements", label: "Enhancements", count: diff.enhancementChanges.length },
    { key: "stratagems", label: "Stratagems", count: diff.stratagemChanges.length },
    { key: "abilities", label: "Abilities", count: diff.abilityChanges.length },
  ];

  return (
    <div>
      <div className={styles.tabs}>
        {tabs.map((t) => (
          <button
            key={t.key}
            className={`${styles.tab} ${tab === t.key ? styles.tabActive : ""}`}
            onClick={() => setTab(t.key)}
          >
            {t.label} ({t.count})
          </button>
        ))}
      </div>
      <div className={styles.content}>
        {tab === "points" && (
          <table className={styles.table}>
            <thead>
              <tr><th>Unit</th><th>Size</th><th>Old</th><th>New</th><th>Change</th></tr>
            </thead>
            <tbody>
              {diff.pointChanges.map((p, i) => {
                const delta = (p.newCost ?? 0) - (p.oldCost ?? 0);
                return (
                  <tr key={i}>
                    <td>{p.datasheetName}</td>
                    <td className={styles.mono}>{p.description}</td>
                    <td className={styles.mono}>{p.oldCost ?? "—"}</td>
                    <td className={styles.mono}>{p.newCost ?? "—"}</td>
                    <td className={delta > 0 ? styles.increase : delta < 0 ? styles.decrease : ""}>
                      {delta > 0 ? `+${delta}` : delta}
                    </td>
                  </tr>
                );
              })}
              {diff.pointChanges.length === 0 && <tr><td colSpan={5} className={styles.empty}>No point changes</td></tr>}
            </tbody>
          </table>
        )}
        {tab === "units" && (
          <table className={styles.table}>
            <thead><tr><th>Unit</th><th>Faction</th><th>Change</th></tr></thead>
            <tbody>
              {diff.unitChanges.map((u, i) => (
                <tr key={i}>
                  <td>{u.name}</td>
                  <td>{u.factionId}</td>
                  <td className={u.changeType === "added" ? styles.added : styles.removed}>{u.changeType}</td>
                </tr>
              ))}
              {diff.unitChanges.length === 0 && <tr><td colSpan={3} className={styles.empty}>No unit changes</td></tr>}
            </tbody>
          </table>
        )}
        {tab === "stats" && (
          <table className={styles.table}>
            <thead><tr><th>Unit</th><th>Stat</th><th>Old</th><th>New</th></tr></thead>
            <tbody>
              {diff.statChanges.map((s, i) => (
                <tr key={i}>
                  <td>{s.datasheetName}</td>
                  <td>{s.field}</td>
                  <td className={styles.mono}>{s.oldValue}</td>
                  <td className={styles.mono}>{s.newValue}</td>
                </tr>
              ))}
              {diff.statChanges.length === 0 && <tr><td colSpan={4} className={styles.empty}>No stat changes</td></tr>}
            </tbody>
          </table>
        )}
        {tab === "enhancements" && (
          <table className={styles.table}>
            <thead><tr><th>Enhancement</th><th>Old Cost</th><th>New Cost</th><th>Change</th></tr></thead>
            <tbody>
              {diff.enhancementChanges.map((e, i) => {
                const hasText = e.oldDescription !== e.newDescription;
                if (hasText) {
                  return (
                    <ExpandableRow key={i} detail={<TextDiff oldText={e.oldDescription} newText={e.newDescription} />}>
                      <td>{e.name} <span className={styles.expandHint}>▸</span></td>
                      <td className={styles.mono}>{e.oldCost ?? "—"}</td>
                      <td className={styles.mono}>{e.newCost ?? "—"}</td>
                      <td className={e.changeType === "added" ? styles.added : e.changeType === "removed" ? styles.removed : ""}>{e.changeType}</td>
                    </ExpandableRow>
                  );
                }
                return (
                  <tr key={i}>
                    <td>{e.name}</td>
                    <td className={styles.mono}>{e.oldCost ?? "—"}</td>
                    <td className={styles.mono}>{e.newCost ?? "—"}</td>
                    <td className={e.changeType === "added" ? styles.added : e.changeType === "removed" ? styles.removed : ""}>{e.changeType}</td>
                  </tr>
                );
              })}
              {diff.enhancementChanges.length === 0 && <tr><td colSpan={4} className={styles.empty}>No enhancement changes</td></tr>}
            </tbody>
          </table>
        )}
        {tab === "stratagems" && (
          <table className={styles.table}>
            <thead><tr><th>Stratagem</th><th>Old CP</th><th>New CP</th><th>Change</th></tr></thead>
            <tbody>
              {diff.stratagemChanges.map((s, i) => {
                const hasText = s.oldDescription !== s.newDescription;
                if (hasText) {
                  return (
                    <ExpandableRow key={i} detail={<TextDiff oldText={s.oldDescription} newText={s.newDescription} />}>
                      <td>{s.name} <span className={styles.expandHint}>▸</span></td>
                      <td className={styles.mono}>{s.oldCpCost ?? "—"}</td>
                      <td className={styles.mono}>{s.newCpCost ?? "—"}</td>
                      <td className={s.changeType === "added" ? styles.added : s.changeType === "removed" ? styles.removed : ""}>{s.changeType}</td>
                    </ExpandableRow>
                  );
                }
                return (
                  <tr key={i}>
                    <td>{s.name}</td>
                    <td className={styles.mono}>{s.oldCpCost ?? "—"}</td>
                    <td className={styles.mono}>{s.newCpCost ?? "—"}</td>
                    <td className={s.changeType === "added" ? styles.added : s.changeType === "removed" ? styles.removed : ""}>{s.changeType}</td>
                  </tr>
                );
              })}
              {diff.stratagemChanges.length === 0 && <tr><td colSpan={4} className={styles.empty}>No stratagem changes</td></tr>}
            </tbody>
          </table>
        )}
        {tab === "abilities" && (
          <table className={styles.table}>
            <thead><tr><th>Ability</th><th>Faction</th><th>Change</th></tr></thead>
            <tbody>
              {diff.abilityChanges.map((a, i) => {
                const hasText = a.changeType === "modified" || a.changeType === "added" || a.changeType === "removed";
                if (hasText && (a.oldDescription || a.newDescription)) {
                  return (
                    <ExpandableRow key={i} detail={<TextDiff oldText={a.oldDescription} newText={a.newDescription} />}>
                      <td>{a.name} <span className={styles.expandHint}>▸</span></td>
                      <td>{a.factionId}</td>
                      <td className={a.changeType === "added" ? styles.added : a.changeType === "removed" ? styles.removed : ""}>{a.changeType}</td>
                    </ExpandableRow>
                  );
                }
                return (
                  <tr key={i}>
                    <td>{a.name}</td>
                    <td>{a.factionId}</td>
                    <td className={a.changeType === "added" ? styles.added : a.changeType === "removed" ? styles.removed : ""}>{a.changeType}</td>
                  </tr>
                );
              })}
              {diff.abilityChanges.length === 0 && <tr><td colSpan={3} className={styles.empty}>No ability changes</td></tr>}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
