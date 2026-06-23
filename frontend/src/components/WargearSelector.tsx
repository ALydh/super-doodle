import { useState } from "react";
import type { DatasheetOption, WargearSelection, ParsedWargearOption } from "../types";
import { sanitizeHtml } from "../sanitize";
import styles from "./WargearSelector.module.css";

export type WargearOptionType =
  | { kind: 'single'; choices: string[]; values?: string[] }
  | { kind: 'two'; choices: string[] }
  | { kind: 'either-or-two'; singleton: string; choices: string[] }
  | { kind: 'multi'; choices: string[]; max?: number };

function defaultNotesFor(opt: WargearOptionType | null): string | undefined {
  if (!opt) return undefined;
  switch (opt.kind) {
    case 'single':
      return opt.values?.[0] ?? opt.choices[0];
    case 'two':
      return opt.choices.length >= 2 ? `${opt.choices[0]}|${opt.choices[1]}` : undefined;
    case 'either-or-two':
      return opt.singleton;
    case 'multi':
      return undefined;
  }
}

interface Props {
  options: DatasheetOption[];
  selections: WargearSelection[];
  onSelectionChange: (optionLine: number, selected: boolean, initialNotes?: string) => void;
  onNotesChange: (optionLine: number, notes: string) => void;
  extractOption: (description: string) => WargearOptionType | null;
  parsedWargearOptions?: ParsedWargearOption[];
}

export function WargearSelector({
  options,
  selections,
  onSelectionChange,
  onNotesChange,
  extractOption,
  parsedWargearOptions = [],
}: Props) {
  const [isExpanded, setIsExpanded] = useState(false);
  const [manualByLine, setManualByLine] = useState<Record<number, boolean>>({});

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
        const isManual = manualByLine[option.line] ?? false;
        const manualWeapons = (() => {
          const seen = new Set<string>();
          return parsedWargearOptions
            .filter(p => p.optionLine === option.line && p.action === 'add' && p.choiceIndex > 0)
            .map(p => p.weaponName)
            .filter(name => {
              const key = name.toLowerCase();
              if (seen.has(key)) return false;
              seen.add(key);
              return true;
            });
        })();
        const selectedManualSet = new Set(notes.split('|').map(s => s.trim().toLowerCase()).filter(Boolean));
        const toggleManualWeapon = (name: string) => {
          const next = new Set(selectedManualSet);
          const key = name.toLowerCase();
          if (next.has(key)) next.delete(key);
          else next.add(key);
          onNotesChange(option.line, [...next].join('|'));
        };

        return (
          <div
            key={option.line}
            className={`${styles.cardOption} ${isSelected ? styles.selected : ""}`}
            onClick={() => {
              const newSelected = !isSelected;
              const initialNotes = newSelected && !notes ? defaultNotesFor(opt) : undefined;
              onSelectionChange(option.line, newSelected, initialNotes);
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
              {isSelected && !isManual && opt?.kind === 'single' && (
                <select
                  className={`${styles.unitSelect} ${styles.choiceDropdown}`}
                  value={notes}
                  onChange={(e) => onNotesChange(option.line, e.target.value)}
                  onClick={(e) => e.stopPropagation()}
                >
                  <option value="">Select wargear...</option>
                  {opt.choices.map((choice, idx) => (
                    <option key={idx} value={opt.values?.[idx] ?? choice}>{choice}</option>
                  ))}
                </select>
              )}
              {isSelected && !isManual && opt?.kind === 'two' && (
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
              {isSelected && !isManual && opt?.kind === 'multi' && (
                <div className={styles.manualGrid} onClick={(e) => e.stopPropagation()}>
                  {opt.choices.map((choice) => {
                    const key = choice.toLowerCase();
                    const checkedSet = new Set(notes.split('|').map(s => s.trim().toLowerCase()).filter(Boolean));
                    const isChecked = checkedSet.has(key);
                    const atMax = opt.max != null && checkedSet.size >= opt.max && !isChecked;
                    return (
                      <label key={choice} className={styles.manualItem}>
                        <input
                          type="checkbox"
                          checked={isChecked}
                          disabled={atMax}
                          onChange={() => {
                            const next = new Set(checkedSet);
                            if (next.has(key)) next.delete(key);
                            else next.add(key);
                            onNotesChange(option.line, [...next].join('|'));
                          }}
                        />
                        {' '}{choice}
                      </label>
                    );
                  })}
                  {opt.max != null && (
                    <span className={styles.manualHint}>Up to {opt.max}</span>
                  )}
                </div>
              )}
              {isSelected && !isManual && opt?.kind === 'either-or-two' && (
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
              {isSelected && isManual && manualWeapons.length > 0 && (
                <div className={styles.manualGrid} onClick={(e) => e.stopPropagation()}>
                  {manualWeapons.map((name) => (
                    <label key={name} className={styles.manualItem}>
                      <input
                        type="checkbox"
                        checked={selectedManualSet.has(name.toLowerCase())}
                        onChange={() => toggleManualWeapon(name)}
                      />
                      {' '}{name}
                    </label>
                  ))}
                </div>
              )}
              {isSelected && manualWeapons.length > 0 && (
                <button
                  type="button"
                  className={styles.manualToggle}
                  onClick={(e) => {
                    e.stopPropagation();
                    setManualByLine((prev) => ({ ...prev, [option.line]: !isManual }));
                  }}
                >
                  {isManual ? "Use guided selection" : "Pick weapons manually"}
                </button>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
