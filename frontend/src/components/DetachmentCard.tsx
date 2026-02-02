import type { DetachmentAbility, Enhancement } from "../types";
import { sanitizeHtml } from "../sanitize";

interface Props {
  name: string;
  abilities: DetachmentAbility[];
  enhancements: Enhancement[];
}

export function DetachmentCard({ name, abilities, enhancements }: Props) {
  return (
    <div className="detachment-card">
      <h3 className="detachment-name">{name}</h3>

      {abilities.length > 0 && (
        <div className="detachment-abilities">
          <h4>Detachment Rule</h4>
          {abilities.map((ability) => (
            <div key={ability.id} className="detachment-ability">
              <strong>{ability.name}</strong>
              {ability.legend && (
                <div className="detachment-ability-legend">{ability.legend}</div>
              )}
              <div
                className="detachment-ability-description"
                dangerouslySetInnerHTML={{ __html: sanitizeHtml(ability.description) }}
              />
            </div>
          ))}
        </div>
      )}

      {enhancements.length > 0 && (
        <div className="detachment-enhancements">
          <h4>Enhancements</h4>
          <div className="enhancement-list">
            {enhancements.map((enhancement) => (
              <div key={enhancement.id} className="enhancement-item">
                <div className="enhancement-header">
                  <span className="enhancement-name">{enhancement.name}</span>
                  <span className="enhancement-cost">{enhancement.cost}pts</span>
                </div>
                {enhancement.legend && (
                  <div className="enhancement-legend">{enhancement.legend}</div>
                )}
                <div
                  className="enhancement-description"
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
