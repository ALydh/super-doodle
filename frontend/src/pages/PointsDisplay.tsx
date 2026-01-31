import type { BattleSize } from "../types";
import { BATTLE_SIZE_POINTS } from "../types";

interface Props {
  total: number;
  battleSize: BattleSize;
}

export function PointsDisplay({ total, battleSize }: Props) {
  const max = BATTLE_SIZE_POINTS[battleSize];
  const percent = Math.min((total / max) * 100, 100);

  return (
    <div
      className={`points-total ${total > max ? "over-budget" : ""}`}
      style={{ "--points-percent": `${percent}%` } as React.CSSProperties}
    >
      <div className="points-bar" />
      <span className="points-text">{total} / {max} pts</span>
    </div>
  );
}
