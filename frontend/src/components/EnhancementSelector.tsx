import { useState } from "react";
import type { Enhancement } from "../types";
import { sanitizeHtml } from "../sanitize";
import styles from "./EnhancementSelector.module.css";

const stripFactionRestriction = (desc: string) =>
  desc.replace(/^[A-Z][A-Z\s]+ model only\.\s*/i, '');

type SelectorMode = "cards" | "accordion" | "radio";

interface Props {
  enhancements: Enhancement[];
  selectedId: string | null;
  onSelect: (id: string | null) => void;
  mode?: SelectorMode;
}

export function EnhancementSelector({ enhancements, selectedId, onSelect, mode = "cards" }: Props) {
  const [isExpanded, setIsExpanded] = useState(false);
  const [expandedAccordion, setExpandedAccordion] = useState<string | null>(null);
  const selectedEnhancement = selectedId ? enhancements.find(e => e.id === selectedId) : null;

  const handleSelect = (id: string | null) => {
    onSelect(id);
    setIsExpanded(false);
  };

  if (!isExpanded) {
    if (selectedEnhancement) {
      return (
        <div className={styles.collapsed}>
          <div className={styles.detail}>
            <div className={styles.header}>
              <strong>{selectedEnhancement.name}</strong>
              <span className={styles.cost}>+{selectedEnhancement.cost}pts</span>
            </div>
            {selectedEnhancement.description && (
              <p dangerouslySetInnerHTML={{ __html: sanitizeHtml(stripFactionRestriction(selectedEnhancement.description)) }} />
            )}
          </div>
          <button
            type="button"
            className={styles.changeBtn}
            onClick={() => setIsExpanded(true)}
          >
            Change Enhancement
          </button>
        </div>
      );
    }

    return (
      <div className={styles.collapsed}>
        <div className={`${styles.detail} ${styles.none}`}>
          <span className={styles.noneText}>No enhancement</span>
        </div>
        <button
          type="button"
          className={styles.changeBtn}
          onClick={() => setIsExpanded(true)}
        >
          Add Enhancement
        </button>
      </div>
    );
  }

  if (mode === "cards") {
    return (
      <div className={styles.cards}>
        <div className={`${styles.cardOption} ${styles.cardNone}`} onClick={() => handleSelect(null)}>
          <div className={styles.cardHeader}>
            <span className={styles.cardName}>None</span>
            <span className={styles.cardCost}>+0pts</span>
          </div>
          <p className={styles.cardDescription}>No enhancement selected</p>
        </div>
        {enhancements.map((e) => (
          <div
            key={e.id}
            className={styles.cardOption}
            onClick={() => handleSelect(e.id)}
          >
            <div className={styles.cardHeader}>
              <span className={styles.cardName}>{e.name}</span>
              <span className={styles.cardCost}>+{e.cost}pts</span>
            </div>
            {e.description && (
              <p
                className={styles.cardDescription}
                dangerouslySetInnerHTML={{ __html: sanitizeHtml(stripFactionRestriction(e.description)) }}
              />
            )}
          </div>
        ))}
      </div>
    );
  }

  if (mode === "accordion") {
    return (
      <div className={styles.accordion}>
        <div
          className={`${styles.accordionItem} ${expandedAccordion === "none" ? styles.expanded : ""}`}
        >
          <div
            className={styles.accordionHeader}
            onClick={() => setExpandedAccordion(expandedAccordion === "none" ? null : "none")}
          >
            <span className={styles.accordionExpand}>{expandedAccordion === "none" ? "▼" : "▶"}</span>
            <span className={styles.accordionName}>None</span>
            <span className={styles.accordionCost}>+0pts</span>
            <button
              type="button"
              className={styles.selectBtn}
              onClick={(ev) => { ev.stopPropagation(); handleSelect(null); }}
            >
              Select
            </button>
          </div>
          {expandedAccordion === "none" && (
            <div className={styles.accordionContent}>
              <p>No enhancement selected</p>
            </div>
          )}
        </div>
        {enhancements.map((e) => (
          <div
            key={e.id}
            className={`${styles.accordionItem} ${expandedAccordion === e.id ? styles.expanded : ""}`}
          >
            <div
              className={styles.accordionHeader}
              onClick={() => setExpandedAccordion(expandedAccordion === e.id ? null : e.id)}
            >
              <span className={styles.accordionExpand}>{expandedAccordion === e.id ? "▼" : "▶"}</span>
              <span className={styles.accordionName}>{e.name}</span>
              <span className={styles.accordionCost}>+{e.cost}pts</span>
              <button
                type="button"
                className={styles.selectBtn}
                onClick={(ev) => { ev.stopPropagation(); handleSelect(e.id); }}
              >
                Select
              </button>
            </div>
            {expandedAccordion === e.id && e.description && (
              <div className={styles.accordionContent}>
                <p dangerouslySetInnerHTML={{ __html: sanitizeHtml(stripFactionRestriction(e.description)) }} />
              </div>
            )}
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className={styles.radio}>
      <label className={styles.radioItem}>
        <input
          type="radio"
          name="enhancement"
          checked={selectedId === null}
          onChange={() => handleSelect(null)}
        />
        <div className={styles.radioContent}>
          <div className={styles.radioHeader}>
            <span className={styles.radioName}>None</span>
            <span className={styles.radioCost}>+0pts</span>
          </div>
          <p className={styles.radioDescription}>No enhancement selected</p>
        </div>
      </label>
      {enhancements.map((e) => (
        <label key={e.id} className={styles.radioItem}>
          <input
            type="radio"
            name="enhancement"
            checked={selectedId === e.id}
            onChange={() => handleSelect(e.id)}
          />
          <div className={styles.radioContent}>
            <div className={styles.radioHeader}>
              <span className={styles.radioName}>{e.name}</span>
              <span className={styles.radioCost}>+{e.cost}pts</span>
            </div>
            {e.description && (
              <p
                className={styles.radioDescription}
                dangerouslySetInnerHTML={{ __html: sanitizeHtml(stripFactionRestriction(e.description)) }}
              />
            )}
          </div>
        </label>
      ))}
    </div>
  );
}
