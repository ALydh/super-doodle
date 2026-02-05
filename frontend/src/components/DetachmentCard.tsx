import type { DetachmentAbility, Enhancement } from "../types";
import { sanitizeHtml } from "../sanitize";
import styles from "./DetachmentCard.module.css";

interface Props {
  name: string;
  abilities: DetachmentAbility[];
  enhancements: Enhancement[];
}

export function DetachmentCard({ name, abilities, enhancements }: Props) {
  return (
    <div className={styles.card}>
      <h3 className={styles.name}>{name}</h3>

      {abilities.length > 0 && (
        <div className={styles.abilities}>
          <h4>Detachment Rule</h4>
          {abilities.map((ability) => (
            <div key={ability.id} className={styles.ability}>
              <strong>{ability.name}</strong>
              {ability.legend && (
                <div className={styles.abilityLegend}>{ability.legend}</div>
              )}
              <div
                className={styles.abilityDescription}
                dangerouslySetInnerHTML={{ __html: sanitizeHtml(ability.description) }}
              />
            </div>
          ))}
        </div>
      )}

      {enhancements.length > 0 && (
        <div className={styles.enhancements}>
          <h4>Enhancements</h4>
          <div className={styles.enhancementList}>
            {enhancements.map((enhancement) => (
              <div key={enhancement.id} className={styles.enhancementItem}>
                <div className={styles.enhancementHeader}>
                  <span className={styles.enhancementName}>{enhancement.name}</span>
                  <span className={styles.enhancementCost}>{enhancement.cost}pts</span>
                </div>
                {enhancement.legend && (
                  <div className={styles.enhancementLegend}>{enhancement.legend}</div>
                )}
                <div
                  className={styles.enhancementDescription}
                  dangerouslySetInnerHTML={{ __html: sanitizeHtml(enhancement.description) }}
                />
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
