import { useEffect, useState } from "react";
import { fetchRevisions, triggerRevisionCheck, activateRevision } from "../api";
import { useAuth } from "../context/useAuth";
import { RevisionDiffView } from "./RevisionDiffView";
import type { Revision } from "../types";
import styles from "./RevisionPanel.module.css";

interface RevisionPanelProps {
  open: boolean;
  onClose: () => void;
}

export function RevisionPanel({ open, onClose }: RevisionPanelProps) {
  const { user } = useAuth();
  const [revisions, setRevisions] = useState<Revision[]>([]);
  const [checking, setChecking] = useState(false);
  const [diffPair, setDiffPair] = useState<[string, string] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      fetchRevisions().then(setRevisions).catch((e) => setError(e.message));
    }
  }, [open]);

  if (!open) return null;

  const handleCheck = async () => {
    setChecking(true);
    setError(null);
    try {
      const result = await triggerRevisionCheck();
      if (result.error) setError(result.error);
      const updated = await fetchRevisions();
      setRevisions(updated);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Check failed");
    } finally {
      setChecking(false);
    }
  };

  const handleActivate = async (id: string) => {
    try {
      await activateRevision(id);
      const updated = await fetchRevisions();
      setRevisions(updated);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Activation failed");
    }
  };

  const handleDiff = (oldId: string, newId: string) => {
    setDiffPair([oldId, newId]);
  };

  if (diffPair) {
    return (
      <div className={styles.backdrop} onClick={onClose}>
        <div className={styles.panel} onClick={(e) => e.stopPropagation()}>
          <div className={styles.header}>
            <button className={styles.backBtn} onClick={() => setDiffPair(null)}>&larr; Back</button>
            <h2>Changes: {diffPair[0]} &rarr; {diffPair[1]}</h2>
            <button className={styles.closeBtn} onClick={onClose}>&times;</button>
          </div>
          <RevisionDiffView oldId={diffPair[0]} newId={diffPair[1]} />
        </div>
      </div>
    );
  }

  return (
    <div className={styles.backdrop} onClick={onClose}>
      <div className={styles.panel} onClick={(e) => e.stopPropagation()} role="dialog" aria-modal="true">
        <div className={styles.header}>
          <h2>Data Revisions</h2>
          <button className={styles.closeBtn} onClick={onClose}>&times;</button>
        </div>
        {error && <div className={styles.error}>{error}</div>}
        {user?.isAdmin && (
          <div className={styles.actions}>
            <button onClick={handleCheck} disabled={checking} className={styles.checkBtn}>
              {checking ? "Checking..." : "Check for updates"}
            </button>
          </div>
        )}
        <div className={styles.list}>
          {revisions.map((rev, i) => {
            const next = revisions[i + 1];
            return (
              <div key={rev.id} className={`${styles.item} ${rev.isActive ? styles.active : ""}`}>
                <div className={styles.itemHeader}>
                  <span className={styles.revId}>{rev.id}</span>
                  {rev.isActive && <span className={styles.activeBadge}>active</span>}
                </div>
                <div className={styles.itemMeta}>
                  Wahapedia: {rev.wahapediaTimestamp}<br />
                  Fetched: {new Date(rev.fetchedAt).toLocaleString()}
                </div>
                <div className={styles.itemActions}>
                  {next && (
                    <button className={styles.diffBtn} onClick={() => handleDiff(next.id, rev.id)}>
                      Diff vs previous
                    </button>
                  )}
                  {!rev.isActive && user?.isAdmin && (
                    <button className={styles.activateBtn} onClick={() => handleActivate(rev.id)}>
                      Activate
                    </button>
                  )}
                </div>
              </div>
            );
          })}
          {revisions.length === 0 && <p className={styles.empty}>No revisions found</p>}
        </div>
      </div>
    </div>
  );
}
