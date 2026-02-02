import type { Stratagem } from "../types";
import { sanitizeHtml } from "../sanitize";

interface Props {
  stratagem: Stratagem;
}

export function StratagemCard({ stratagem }: Props) {
  return (
    <div className="stratagem-card">
      <div className="stratagem-header">
        <span className="stratagem-name">{stratagem.name}</span>
        {stratagem.cpCost !== null && (
          <span className="stratagem-cp">{stratagem.cpCost} CP</span>
        )}
      </div>
      <div className="stratagem-meta">
        {stratagem.stratagemType && (
          <span className="stratagem-type">{stratagem.stratagemType}</span>
        )}
        {stratagem.phase && (
          <span className="stratagem-phase">{stratagem.phase}</span>
        )}
        {stratagem.turn && (
          <span className="stratagem-turn">{stratagem.turn}</span>
        )}
      </div>
      {stratagem.legend && (
        <div className="stratagem-legend">{stratagem.legend}</div>
      )}
      <div
        className="stratagem-description"
        dangerouslySetInnerHTML={{ __html: sanitizeHtml(stratagem.description) }}
      />
    </div>
  );
}
