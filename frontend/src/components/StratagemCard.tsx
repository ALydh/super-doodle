import type { Stratagem } from "../types";
import { sanitizeHtml } from "../sanitize";
import styles from "./StratagemCard.module.css";

interface Props {
  stratagem: Stratagem;
}

export function StratagemCard({ stratagem }: Props) {
  return (
    <div className={styles.card}>
      <div className={styles.header}>
        <span className={styles.name}>{stratagem.name}</span>
        {stratagem.cpCost !== null && (
          <span className={styles.cp}>{stratagem.cpCost} CP</span>
        )}
      </div>
      <div className={styles.meta}>
        {stratagem.stratagemType && (
          <span className={styles.type}>{stratagem.stratagemType}</span>
        )}
        {stratagem.phase && (
          <span className={styles.phase}>{stratagem.phase}</span>
        )}
        {stratagem.turn && (
          <span className={styles.turn}>{stratagem.turn}</span>
        )}
      </div>
      {stratagem.legend && (
        <div className={styles.legend}>{stratagem.legend}</div>
      )}
      <div
        className={styles.description}
        dangerouslySetInnerHTML={{ __html: sanitizeHtml(stratagem.description) }}
      />
    </div>
  );
}
