import type { BattleSize } from "../types";
import { BATTLE_SIZE_POINTS } from "../types";
import styles from "./PointsDisplay.module.css";

interface Props {
  total: number;
  battleSize: BattleSize;
}

export function PointsDisplay({ total, battleSize }: Props) {
  const max = BATTLE_SIZE_POINTS[battleSize];
  const rawPercent = Math.min((total / max) * 100, 100);
  const percent = total > 0 ? Math.max(rawPercent, 3) : 0;

  return (
    <div
      className={`${styles.total} ${total > max ? styles.overBudget : ""}`}
      style={{ "--points-percent": `${percent}%` } as React.CSSProperties}
    >
      <div className={styles.bar} />
      <span className={styles.text}>{total} / {max} pts</span>
    </div>
  );
}
