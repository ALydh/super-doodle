import { useState } from "react";
import type { DatasheetOption, WargearSelection } from "../types";
import { sanitizeHtml } from "../sanitize";
import styles from "./WargearSelector.module.css";

export type WargearOptionType =
  | { kind: 'single'; choices: string[] }
  | { kind: 'two'; choices: string[] }
  | { kind: 'either-or-two'; singleton: string; choices: string[] };

interface Props {
  options: DatasheetOption[];
  selections: WargearSelection[];
  onSelectionChange: (optionLine: number, selected: boolean) => void;
  onNotesChange: (optionLine: number, notes: string) => void;
  extractOption: (description: string) => WargearOptionType | null;
}

export function WargearSelector({
  options,
  selections,
  onSelectionChange,
  onNotesChange,
  extractOption,
}: Props) {
  const [isExpanded, setIsExpanded] = useState(false);

  const getSelection = (line: number) => selections.find(s => s.optionLine === line);
  const selectedOptions = selections
    .filter(s => s.selected)
    .map(s => {
      const option = options.find(o => o.line === s.optionLine);
      if (s.notes) return s.notes.includes('|') ? s.notes.split('|').filter(Boolean).join(' + ') : s.notes;
      if (!option) return null;
      const desc = option.description.replace(/<[^>]*>/g, '');
      const match = desc.match(/replaced with (?:\d+ )?(.+?)(?:\.|$)/i);
      if (match) return match[1].trim();
      return desc.length > 40 ? desc.slice(0, 40) + '…' : desc;
    })
    .filter(Boolean) as string[];

  if (!isExpanded) {
    return (
      <div className={styles.collapsed}>
        <div className={styles.summary}>
          {selectedOptions.length === 0 ? (
            <span className={styles.noneText}>No wargear options selected</span>
          ) : (
            <span className={styles.selectedList}>{selectedOptions.join(', ')}</span>
          )}
        </div>
        <button
          type="button"
          className={styles.changeBtn}
          onClick={() => setIsExpanded(true)}
        >
          {selectedOptions.length === 0 ? "Configure Wargear" : "Change Wargear"}
        </button>
      </div>
    );
  }

  return (
    <div className={styles.cards}>
      <button
        type="button"
        className={styles.collapseBtn}
        onClick={() => setIsExpanded(false)}
      >
        Done
      </button>
      {options.map((option) => {
        const selection = getSelection(option.line);
        const isSelected = selection?.selected ?? false;
        const opt = extractOption(option.description);
        const notes = selection?.notes ?? '';
        const hasPipe = notes.includes('|');
        const [notes1, notes2] = hasPipe ? notes.split('|', 2) : ['', ''];
        const isEither = !hasPipe;

        return (
          <div
            key={option.line}
            className={`${styles.cardOption} ${isSelected ? styles.selected : ""}`}
            onClick={() => {
              const newSelected = !isSelected;
              onSelectionChange(option.line, newSelected);
              if (newSelected && !notes && opt?.kind === 'either-or-two') {
                onNotesChange(option.line, opt.singleton);
              }
            }}
          >
            <div className={styles.indicator}>
              {isSelected ? "✓" : ""}
            </div>
            <div className={styles.content}>
              <p
                className={styles.description}
                dangerouslySetInnerHTML={{ __html: sanitizeHtml(option.description) }}
              />
              {isSelected && opt?.kind === 'single' && (
                <select
                  className={`${styles.unitSelect} ${styles.choiceDropdown}`}
                  value={notes}
                  onChange={(e) => onNotesChange(option.line, e.target.value)}
                  onClick={(e) => e.stopPropagation()}
                >
                  <option value="">Select wargear...</option>
                  {opt.choices.map((choice, idx) => (
                    <option key={idx} value={choice}>{choice}</option>
                  ))}
                </select>
              )}
              {isSelected && opt?.kind === 'two' && (
                <div onClick={(e) => e.stopPropagation()}>
                  <select
                    className={`${styles.unitSelect} ${styles.choiceDropdown}`}
                    value={notes1}
                    onChange={(e) => onNotesChange(option.line, `${e.target.value}|${notes2}`)}
                  >
                    <option value="">First weapon...</option>
                    {opt.choices.map((choice, idx) => (
                      <option key={idx} value={choice}>{choice}</option>
                    ))}
                  </select>
                  <select
                    className={`${styles.unitSelect} ${styles.choiceDropdown}`}
                    value={notes2}
                    onChange={(e) => onNotesChange(option.line, `${notes1}|${e.target.value}`)}
                  >
                    <option value="">Second weapon...</option>
                    {opt.choices.map((choice, idx) => (
                      <option key={idx} value={choice}>{choice}</option>
                    ))}
                  </select>
                </div>
              )}
              {isSelected && opt?.kind === 'either-or-two' && (
                <div onClick={(e) => e.stopPropagation()}>
                  <label>
                    <input
                      type="radio"
                      checked={isEither}
                      onChange={() => onNotesChange(option.line, opt.singleton)}
                    />
                    {' '}{opt.singleton}
                  </label>
                  <label>
                    <input
                      type="radio"
                      checked={!isEither}
                      onChange={() => onNotesChange(option.line, '|')}
                    />
                    {' '}Two weapons from list
                  </label>
                  {!isEither && (
                    <>
                      <select
                        className={`${styles.unitSelect} ${styles.choiceDropdown}`}
                        value={notes1}
                        onChange={(e) => onNotesChange(option.line, `${e.target.value}|${notes2}`)}
                      >
                        <option value="">First weapon...</option>
                        {opt.choices.map((choice, idx) => (
                          <option key={idx} value={choice}>{choice}</option>
                        ))}
                      </select>
                      <select
                        className={`${styles.unitSelect} ${styles.choiceDropdown}`}
                        value={notes2}
                        onChange={(e) => onNotesChange(option.line, `${notes1}|${e.target.value}`)}
                      >
                        <option value="">Second weapon...</option>
                        {opt.choices.map((choice, idx) => (
                          <option key={idx} value={choice}>{choice}</option>
                        ))}
                      </select>
                    </>
                  )}
                </div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
