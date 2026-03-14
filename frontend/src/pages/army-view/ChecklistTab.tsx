import type { Stratagem, DetachmentAbility } from "../../types";
import { StratagemCard } from "../../components/StratagemCard";
import { sanitizeHtml } from "../../sanitize";
import styles from "./ChecklistTab.module.css";

interface Props {
  stratagems: Stratagem[];
  detachmentAbilities: DetachmentAbility[];
  notes: Record<string, string>;
  onNoteChange: (phase: string, note: string) => void;
}

const PHASES = [
  "Command phase",
  "Movement phase",
  "Shooting phase",
  "Charge phase",
  "Fight phase",
] as const;

function getAbilityPhases(ability: DetachmentAbility): string[] {
  const desc = ability.description.toLowerCase();
  return PHASES.filter((p) => desc.includes(p.toLowerCase()));
}

function stratagemMatchesPhase(stratagemPhase: string | null, phase: string): boolean {
  if (!stratagemPhase) return false;
  if (stratagemPhase === "Any phase") return true;
  const baseName = phase.replace(" phase", "");
  return stratagemPhase.includes(baseName);
}

export function ChecklistTab({ stratagems, detachmentAbilities, notes, onNoteChange }: Props) {
  const alwaysActive = detachmentAbilities.filter(
    (a) => getAbilityPhases(a).length === 0
  );

  return (
    <div className={styles.checklist}>
      {alwaysActive.length > 0 && (
        <section className={styles.section}>
          <h2 className={styles.phaseHeader}>Detachment Rules — Always Active</h2>
          <div className={styles.abilities}>
            {alwaysActive.map((a) => (
              <div key={a.id} className={styles.abilityItem}>
                <strong className={styles.abilityName}>{a.name}</strong>
                <div
                  className={styles.abilityDesc}
                  dangerouslySetInnerHTML={{ __html: sanitizeHtml(a.description) }}
                />
              </div>
            ))}
          </div>
          <div className={styles.noteArea}>
            <textarea
              className={styles.noteInput}
              placeholder="Add notes..."
              value={notes["always-active"] ?? ""}
              onChange={(e) => onNoteChange("always-active", e.target.value)}
            />
          </div>
        </section>
      )}

      {PHASES.map((phase) => {
        const phaseStratagems = stratagems.filter((s) => stratagemMatchesPhase(s.phase, phase));
        const phaseAbilities = detachmentAbilities.filter((a) =>
          getAbilityPhases(a).includes(phase)
        );

        if (phaseStratagems.length === 0 && phaseAbilities.length === 0) return null;

        const yourTurn = phaseStratagems.filter(
          (s) => !s.turn || s.turn === "Your turn" || s.turn === "Either player's turn"
        );
        const opponentTurn = phaseStratagems.filter(
          (s) => s.turn === "Opponent's turn" || s.turn === "Either player's turn"
        );

        return (
          <section key={phase} className={styles.section}>
            <h2 className={styles.phaseHeader}>{phase}</h2>

            {phaseAbilities.length > 0 && (
              <div className={styles.abilities}>
                {phaseAbilities.map((a) => (
                  <div key={a.id} className={styles.abilityItem}>
                    <strong className={styles.abilityName}>{a.name}</strong>
                    <div
                      className={styles.abilityDesc}
                      dangerouslySetInnerHTML={{ __html: sanitizeHtml(a.description) }}
                    />
                  </div>
                ))}
              </div>
            )}

            {yourTurn.length > 0 && (
              <div className={styles.turnGroup}>
                {opponentTurn.length > 0 && (
                  <h3 className={styles.turnHeader}>Your Turn</h3>
                )}
                <div className={styles.stratagemList}>
                  {yourTurn.map((s) => (
                    <StratagemCard key={s.id} stratagem={s} />
                  ))}
                </div>
              </div>
            )}

            {opponentTurn.length > 0 && (
              <div className={styles.turnGroup}>
                <h3 className={styles.turnHeader}>Opponent's Turn</h3>
                <div className={styles.stratagemList}>
                  {opponentTurn.map((s) => (
                    <StratagemCard key={s.id} stratagem={s} />
                  ))}
                </div>
              </div>
            )}

            <div className={styles.noteArea}>
              <textarea
                className={styles.noteInput}
                placeholder="Add notes..."
                value={notes[phase] ?? ""}
                onChange={(e) => onNoteChange(phase, e.target.value)}
              />
            </div>
          </section>
        );
      })}

      {(() => {
        const general = stratagems.filter((s) => !s.phase);
        if (general.length === 0) return null;
        return (
          <section className={styles.section}>
            <h2 className={styles.phaseHeader}>General Stratagems</h2>
            <div className={styles.stratagemList}>
              {general.map((s) => (
                <StratagemCard key={s.id} stratagem={s} />
              ))}
            </div>
            <div className={styles.noteArea}>
              <textarea
                className={styles.noteInput}
                placeholder="Add notes..."
                value={notes["general"] ?? ""}
                onChange={(e) => onNoteChange("general", e.target.value)}
              />
            </div>
          </section>
        );
      })()}
    </div>
  );
}
