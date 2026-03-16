import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import type { WeaponAbility } from "../types";
import { fetchWeaponAbilities } from "../api";
import { sanitizeHtml } from "../sanitize";
import styles from "./WeaponAbilityText.module.css";

let cachedAbilities: WeaponAbility[] | null = null;
let fetchPromise: Promise<WeaponAbility[]> | null = null;

export function clearWeaponAbilitiesCache(): void {
  cachedAbilities = null;
  fetchPromise = null;
}

function useWeaponAbilities(): WeaponAbility[] {
  const [abilities, setAbilities] = useState<WeaponAbility[]>(cachedAbilities ?? []);

  useEffect(() => {
    if (cachedAbilities) return;
    if (!fetchPromise) {
      fetchPromise = fetchWeaponAbilities().then((data) => {
        cachedAbilities = data;
        return data;
      });
    }
    let cancelled = false;
    fetchPromise.then((data) => {
      if (!cancelled) setAbilities(data);
    });
    return () => { cancelled = true; };
  }, []);

  return abilities;
}

interface AbilityMatch {
  ability: WeaponAbility;
  start: number;
  end: number;
  text: string;
}

function findAbilityMatches(text: string, abilities: WeaponAbility[]): AbilityMatch[] {
  const matches: AbilityMatch[] = [];
  const lowerText = text.toLowerCase();

  for (const ability of abilities) {
    const searchTerms = [
      ability.name.toLowerCase(),
      ability.id.replace(/-/g, " "),
      ability.id.replace(/-/g, ""),
    ];

    for (const term of searchTerms) {
      let searchPos = 0;
      while (true) {
        const idx = lowerText.indexOf(term, searchPos);
        if (idx === -1) break;

        const beforeChar = idx > 0 ? lowerText[idx - 1] : " ";
        const afterChar = idx + term.length < lowerText.length ? lowerText[idx + term.length] : " ";
        const isWordBoundary = /[\s,<>]/.test(beforeChar) && /[\s,<>0-9+]/.test(afterChar);

        if (isWordBoundary) {
          const alreadyMatched = matches.some(
            (m) => (idx >= m.start && idx < m.end) || (idx + term.length > m.start && idx + term.length <= m.end)
          );
          if (!alreadyMatched) {
            let endPos = idx + term.length;
            const afterMatch = text.slice(endPos);
            const paramMatch = afterMatch.match(/^[\s]*(\d+\+?)/);
            if (paramMatch) {
              endPos += paramMatch[0].length;
            }
            matches.push({
              ability,
              start: idx,
              end: endPos,
              text: text.slice(idx, endPos),
            });
          }
        }
        searchPos = idx + 1;
      }
    }
  }

  return matches.sort((a, b) => a.start - b.start);
}

interface TooltipPos { x: number; y: number }

function AbilitySpan({ text, description }: { text: string; description: string }) {
  const [pos, setPos] = useState<TooltipPos | null>(null);

  return (
    <>
      <span
        className={styles.tooltip}
        onMouseMove={(e) => setPos({ x: e.clientX, y: e.clientY - 12 })}
        onMouseLeave={() => setPos(null)}
      >
        {text}
      </span>
      {pos && createPortal(
        <div
          className={styles.tooltipBox}
          style={{ left: pos.x, top: pos.y, transform: 'translate(-50%, -100%)' }}
        >
          {description}
        </div>,
        document.body
      )}
    </>
  );
}

interface Props {
  text: string | null;
}

function renderAbilitySegment(segment: string, abilities: WeaponAbility[], keyPrefix: string): React.ReactNode {
  const matches = findAbilityMatches(segment, abilities);

  if (matches.length === 0) {
    return <span dangerouslySetInnerHTML={{ __html: sanitizeHtml(segment) }} />;
  }

  const parts: React.ReactNode[] = [];
  let lastEnd = 0;

  for (const match of matches) {
    if (match.start > lastEnd) {
      parts.push(
        <span key={`${keyPrefix}-text-${lastEnd}`} dangerouslySetInnerHTML={{ __html: sanitizeHtml(segment.slice(lastEnd, match.start)) }} />
      );
    }
    parts.push(
      <AbilitySpan key={`${keyPrefix}-ability-${match.start}`} text={match.text} description={match.ability.description} />
    );
    lastEnd = match.end;
  }

  if (lastEnd < segment.length) {
    parts.push(<span key={`${keyPrefix}-text-${lastEnd}`} dangerouslySetInnerHTML={{ __html: sanitizeHtml(segment.slice(lastEnd)) }} />);
  }

  return <>{parts}</>;
}

export function WeaponAbilityText({ text }: Props) {
  const abilities = useWeaponAbilities();

  if (!text || text === "-") {
    return <span>-</span>;
  }

  const segments = text.split(/,\s*/).filter(s => s.trim());

  if (segments.length <= 1) {
    if (abilities.length === 0) {
      return <span dangerouslySetInnerHTML={{ __html: sanitizeHtml(text) }} />;
    }
    return <>{renderAbilitySegment(text, abilities, "single")}</>;
  }

  return (
    <span className={styles.list}>
      {segments.map((segment, i) => (
        <span key={i} className={styles.item}>
          {abilities.length === 0
            ? <span dangerouslySetInnerHTML={{ __html: sanitizeHtml(segment.trim()) }} />
            : renderAbilitySegment(segment.trim(), abilities, `seg-${i}`)}
        </span>
      ))}
    </span>
  );
}
