import { useState } from "react";
import type { BattleUnitData } from "../../types";
import { UnitDetailWide } from "./UnitDetailWide";
import styles from "./BattleUnitCard.module.css";

interface Props {
  data: BattleUnitData;
  isWarlord: boolean;
  defaultExpanded?: boolean;
  count?: number;
}

export function BattleUnitCard({ data, isWarlord, defaultExpanded = false, count = 1 }: Props) {
  const [expanded, setExpanded] = useState(defaultExpanded);
  const { datasheet, profiles, cost, enhancement } = data;

  const mainProfile = profiles[0];
  const totalCost = cost ? cost.cost * count + (enhancement?.cost ?? 0) : 0;

  return (
    <div className={`${styles.card} ${expanded ? styles.expanded : ""} ${count > 1 ? styles.stacked : ""}`}>
      {count > 1 && (
        <>
          <div className={styles.stackedShadow} />
          <div className={styles.stackedShadow} />
        </>
      )}
      <button
        className={styles.header}
        onClick={() => setExpanded(!expanded)}
      >
        <span className={styles.name}>
          {datasheet.name}
          {count > 1 && <span className={styles.stackedCount}>×{count}</span>}
          {isWarlord && <span className={styles.warlordBadge}>★</span>}
          {enhancement && (
            <>
              <span className={styles.enhancementDot}>●</span>
              <span className={styles.enhancementLabel}>{enhancement.name}</span>
            </>
          )}
        </span>
        <span className={styles.stats}>
          {mainProfile && (
            <>
              <span className={styles.statPill}>M{mainProfile.movement}</span>
              <span className={styles.statPill}>T{mainProfile.toughness}</span>
              <span className={styles.statPill}>W{mainProfile.wounds}</span>
              <span className={styles.statPill}>SV{mainProfile.save}</span>
              {mainProfile.invulnerableSave && (
                <span className={styles.statPill}>Inv{mainProfile.invulnerableSave}</span>
              )}
              <span className={styles.statPill}>LD{mainProfile.leadership}</span>
              <span className={styles.statPill}>OC{mainProfile.objectiveControl}</span>
            </>
          )}
        </span>
        <span className={styles.cost}>{totalCost}pts</span>
      </button>
      {expanded && (
        <div className={styles.content}>
          <UnitDetailWide data={data} isWarlord={isWarlord} hideHeader />
        </div>
      )}
    </div>
  );
}
