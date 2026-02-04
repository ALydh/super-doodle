import { useState } from "react";
import type { Enhancement } from "../types";
import { sanitizeHtml } from "../sanitize";

type SelectorMode = "cards" | "accordion" | "radio";

interface Props {
  enhancements: Enhancement[];
  selectedId: string | null;
  onSelect: (id: string | null) => void;
  mode?: SelectorMode;
}

export function EnhancementSelector({ enhancements, selectedId, onSelect, mode = "cards" }: Props) {
  const [expandedAccordion, setExpandedAccordion] = useState<string | null>(null);
  const selectedEnhancement = selectedId ? enhancements.find(e => e.id === selectedId) : null;

  if (selectedEnhancement) {
    return (
      <div className="enhancement-selector-collapsed">
        <div className="enhancement-detail">
          <div className="enhancement-header">
            <strong>{selectedEnhancement.name}</strong>
            <span className="enhancement-cost">+{selectedEnhancement.cost}pts</span>
          </div>
          {selectedEnhancement.description && (
            <p dangerouslySetInnerHTML={{ __html: sanitizeHtml(selectedEnhancement.description) }} />
          )}
        </div>
        <button
          type="button"
          className="enhancement-change-btn"
          onClick={() => onSelect(null)}
        >
          Change Enhancement
        </button>
      </div>
    );
  }

  if (mode === "cards") {
    return (
      <div className="enhancement-selector enhancement-selector-cards">
        <div className="enhancement-card-option enhancement-card-none" onClick={() => onSelect(null)}>
          <div className="enhancement-card-header">
            <span className="enhancement-card-name">None</span>
            <span className="enhancement-card-cost">+0pts</span>
          </div>
          <p className="enhancement-card-description">No enhancement selected</p>
        </div>
        {enhancements.map((e) => (
          <div
            key={e.id}
            className="enhancement-card-option"
            onClick={() => onSelect(e.id)}
          >
            <div className="enhancement-card-header">
              <span className="enhancement-card-name">{e.name}</span>
              <span className="enhancement-card-cost">+{e.cost}pts</span>
            </div>
            {e.description && (
              <p
                className="enhancement-card-description"
                dangerouslySetInnerHTML={{ __html: sanitizeHtml(e.description) }}
              />
            )}
          </div>
        ))}
      </div>
    );
  }

  if (mode === "accordion") {
    return (
      <div className="enhancement-selector enhancement-selector-accordion">
        <div
          className={`enhancement-accordion-item ${expandedAccordion === "none" ? "expanded" : ""}`}
        >
          <div
            className="enhancement-accordion-header"
            onClick={() => setExpandedAccordion(expandedAccordion === "none" ? null : "none")}
          >
            <span className="enhancement-accordion-expand">{expandedAccordion === "none" ? "▼" : "▶"}</span>
            <span className="enhancement-accordion-name">None</span>
            <span className="enhancement-accordion-cost">+0pts</span>
            <button
              type="button"
              className="enhancement-select-btn"
              onClick={(ev) => { ev.stopPropagation(); onSelect(null); }}
            >
              Select
            </button>
          </div>
          {expandedAccordion === "none" && (
            <div className="enhancement-accordion-content">
              <p>No enhancement selected</p>
            </div>
          )}
        </div>
        {enhancements.map((e) => (
          <div
            key={e.id}
            className={`enhancement-accordion-item ${expandedAccordion === e.id ? "expanded" : ""}`}
          >
            <div
              className="enhancement-accordion-header"
              onClick={() => setExpandedAccordion(expandedAccordion === e.id ? null : e.id)}
            >
              <span className="enhancement-accordion-expand">{expandedAccordion === e.id ? "▼" : "▶"}</span>
              <span className="enhancement-accordion-name">{e.name}</span>
              <span className="enhancement-accordion-cost">+{e.cost}pts</span>
              <button
                type="button"
                className="enhancement-select-btn"
                onClick={(ev) => { ev.stopPropagation(); onSelect(e.id); }}
              >
                Select
              </button>
            </div>
            {expandedAccordion === e.id && e.description && (
              <div className="enhancement-accordion-content">
                <p dangerouslySetInnerHTML={{ __html: sanitizeHtml(e.description) }} />
              </div>
            )}
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="enhancement-selector enhancement-selector-radio">
      <label className="enhancement-radio-item">
        <input
          type="radio"
          name="enhancement"
          checked={selectedId === null}
          onChange={() => onSelect(null)}
        />
        <div className="enhancement-radio-content">
          <div className="enhancement-radio-header">
            <span className="enhancement-radio-name">None</span>
            <span className="enhancement-radio-cost">+0pts</span>
          </div>
          <p className="enhancement-radio-description">No enhancement selected</p>
        </div>
      </label>
      {enhancements.map((e) => (
        <label key={e.id} className="enhancement-radio-item">
          <input
            type="radio"
            name="enhancement"
            checked={selectedId === e.id}
            onChange={() => onSelect(e.id)}
          />
          <div className="enhancement-radio-content">
            <div className="enhancement-radio-header">
              <span className="enhancement-radio-name">{e.name}</span>
              <span className="enhancement-radio-cost">+{e.cost}pts</span>
            </div>
            {e.description && (
              <p
                className="enhancement-radio-description"
                dangerouslySetInnerHTML={{ __html: sanitizeHtml(e.description) }}
              />
            )}
          </div>
        </label>
      ))}
    </div>
  );
}
