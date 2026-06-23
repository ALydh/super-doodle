import type { BattleUnitData } from "../../types";
import { UnitDetailWide } from "./UnitDetailWide";
import styles from "./BattleUnitCard.module.css";

type BattleViewMode = "compact" | "expanded" | "ranged" | "melee";

interface Props {
  data: BattleUnitData;
  isWarlord: boolean;
  isExpanded: boolean;
  onToggle: () => void;
  leadingUnit?: string;
  attachedLeader?: string;
  viewMode?: BattleViewMode;
}

export function BattleUnitCard({ data, isWarlord, isExpanded: expanded, onToggle, leadingUnit, attachedLeader, viewMode = "compact" }: Props) {
  const { datasheet, profiles, cost, enhancement } = data;

  const mainProfile = profiles[0];
  const totalCost = (cost?.cost ?? 0) + (enhancement?.cost ?? 0);
  const modelCount = cost?.description.match(/(\d+)\s*model/i)?.[1];
  const bodyVisible = viewMode === "compact" ? expanded : true;
  const headerClickable = viewMode === "compact";

  return (
    <div className={`${styles.card} ${bodyVisible ? styles.expanded : ""}`}>
      <button
        className={styles.header}
        onClick={headerClickable ? onToggle : undefined}
        tabIndex={headerClickable ? 0 : -1}
        style={headerClickable ? undefined : { cursor: "default" }}
      >
        <span className={styles.name}>
          {datasheet.name}
          {isWarlord && <span className={styles.warlordBadge}>★</span>}
          {enhancement && (
            <>
              <span className={styles.enhancementDot}>●</span>
              <span className={styles.enhancementLabel}>{enhancement.name}</span>
            </>
          )}
          {leadingUnit && (
            <span className={styles.leadingPill}>{leadingUnit}</span>
          )}
          {attachedLeader && (
            <span className={styles.leadingPill}>{attachedLeader}</span>
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
        {modelCount && Number(modelCount) > 1 && <span className={styles.modelCount}>{modelCount} models</span>}
        <span className={styles.cost}>{totalCost}pts</span>
      </button>
      {bodyVisible && (
        <div className={styles.content}>
          {viewMode === "ranged" || viewMode === "melee" ? (
            <UnitDetailWide data={data} isWarlord={isWarlord} hideHeader weaponsOnly={viewMode} />
          ) : (
            <UnitDetailWide data={data} isWarlord={isWarlord} hideHeader />
          )}
        </div>
      )}
    </div>
  );
}
