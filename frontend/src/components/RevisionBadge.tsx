import { useEffect, useState } from "react";
import { fetchActiveRevision } from "../api";
import type { Revision } from "../types";
import styles from "./RevisionBadge.module.css";

interface RevisionBadgeProps {
  onClick: () => void;
}

export function RevisionBadge({ onClick }: RevisionBadgeProps) {
  const [revision, setRevision] = useState<Revision | null>(null);

  useEffect(() => {
    fetchActiveRevision().then(setRevision).catch(() => {});
  }, []);

  if (!revision) return null;

  const date = new Date(revision.wahapediaTimestamp).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });

  return (
    <button className={styles.badge} onClick={onClick} title="View data revisions">
      Data: {date}
    </button>
  );
}
