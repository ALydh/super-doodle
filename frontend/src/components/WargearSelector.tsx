import { useState } from "react";
import type { DatasheetOption, WargearSelection } from "../types";
import { sanitizeHtml } from "../sanitize";

interface Props {
  options: DatasheetOption[];
  selections: WargearSelection[];
  onSelectionChange: (optionLine: number, selected: boolean) => void;
  onNotesChange: (optionLine: number, notes: string) => void;
  extractChoices: (description: string) => string[] | null;
}

export function WargearSelector({
  options,
  selections,
  onSelectionChange,
  onNotesChange,
  extractChoices,
}: Props) {
  const [isExpanded, setIsExpanded] = useState(false);

  const getSelection = (line: number) => selections.find(s => s.optionLine === line);
  const selectedCount = selections.filter(s => s.selected).length;

  if (!isExpanded) {
    return (
      <div className="wargear-selector-collapsed">
        <div className="wargear-collapsed-summary">
          {selectedCount === 0 ? (
            <span className="wargear-none-text">No wargear options selected</span>
          ) : (
            <span className="wargear-count-text">{selectedCount} option{selectedCount !== 1 ? 's' : ''} selected</span>
          )}
        </div>
        <button
          type="button"
          className="wargear-change-btn"
          onClick={() => setIsExpanded(true)}
        >
          {selectedCount === 0 ? "Configure Wargear" : "Change Wargear"}
        </button>
      </div>
    );
  }

  return (
    <div className="wargear-selector wargear-selector-cards">
      <button
        type="button"
        className="wargear-collapse-btn"
        onClick={() => setIsExpanded(false)}
      >
        Done
      </button>
      {options.map((option) => {
        const selection = getSelection(option.line);
        const isSelected = selection?.selected ?? false;
        const choices = extractChoices(option.description);
        const hasChoices = choices && choices.length > 0;

        return (
          <div
            key={option.line}
            className={`wargear-card-option ${isSelected ? "selected" : ""}`}
            onClick={() => onSelectionChange(option.line, !isSelected)}
          >
            <div className="wargear-card-indicator">
              {isSelected ? "✓" : ""}
            </div>
            <div className="wargear-card-content">
              <p
                className="wargear-card-description"
                dangerouslySetInnerHTML={{ __html: sanitizeHtml(option.description) }}
              />
              {isSelected && hasChoices && (
                <select
                  className="unit-select wargear-choice-dropdown"
                  value={selection?.notes ?? ''}
                  onChange={(e) => onNotesChange(option.line, e.target.value)}
                  onClick={(e) => e.stopPropagation()}
                >
                  <option value="">Select wargear...</option>
                  {choices.map((choice, idx) => (
                    <option key={idx} value={choice}>{choice}</option>
                  ))}
                </select>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
