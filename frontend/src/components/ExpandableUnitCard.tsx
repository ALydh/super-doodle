import { useState, useEffect, useRef } from "react";
import type { DatasheetDetail, Enhancement, ModelProfile } from "../types";
import { fetchDatasheetDetail } from "../api";
import { UnitCardDetail } from "./UnitCardDetail";
import styles from "./ExpandableUnitCard.module.css";
import sharedStyles from "../shared.module.css";

interface Props {
  datasheetId: string;
  datasheetName: string;
  points?: number;
  isExpanded: boolean;
  onToggle: () => void;
  isWarlord?: boolean;
  enhancement?: Enhancement | null;
  profiles?: ModelProfile[];
}

export function ExpandableUnitCard({
  datasheetId,
  datasheetName,
  points,
  isExpanded,
  onToggle,
  isWarlord,
  enhancement,
  profiles,
}: Props) {
  const [detail, setDetail] = useState<DatasheetDetail | null>(null);
  const fetchingRef = useRef(false);

  useEffect(() => {
    if (isExpanded && !detail && !fetchingRef.current) {
      fetchingRef.current = true;
      fetchDatasheetDetail(datasheetId)
        .then(setDetail)
        .finally(() => { fetchingRef.current = false; });
    }
  }, [isExpanded, datasheetId, detail]);

  const loading = isExpanded && !detail;

  return (
    <div className={`${styles.card} ${isExpanded ? styles.expanded : ""}`}>
      <button className={styles.header} onClick={onToggle}>
        <span className={styles.expandIcon}>{isExpanded ? "▼" : "▶"}</span>
        <span className={styles.name}>
          {datasheetName}
          {isWarlord && <span className={styles.warlordBadge}>Warlord</span>}
        </span>
        {enhancement && (
          <span className={styles.enhancement}>+ {enhancement.name}</span>
        )}
        {profiles && profiles.length > 0 && (
          <span className={styles.stats}>
            <span className={styles.statPill}>M{profiles[0].movement}</span>
            <span className={styles.statPill}>T{profiles[0].toughness}</span>
            <span className={styles.statPill}>W{profiles[0].wounds}</span>
            <span className={styles.statPill}>SV{profiles[0].save}</span>
            {profiles[0].invulnerableSave && (
              <span className={styles.statPill}>Inv{profiles[0].invulnerableSave}</span>
            )}
            <span className={styles.statPill}>OC{profiles[0].objectiveControl}</span>
          </span>
        )}
        {points !== undefined && points > 0 && (
          <span className={styles.points}>{points}pts</span>
        )}
      </button>

      {isExpanded && (
        <div className={styles.content}>
          {loading && <div className={sharedStyles.loading}>Loading...</div>}
          {detail && <UnitCardDetail detail={detail} />}
        </div>
      )}
    </div>
  );
}
