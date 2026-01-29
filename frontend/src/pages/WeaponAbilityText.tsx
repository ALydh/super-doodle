import { useEffect, useState } from "react";
import type { WeaponAbility } from "../types";
import { fetchWeaponAbilities } from "../api";

let cachedAbilities: WeaponAbility[] | null = null;
let fetchPromise: Promise<WeaponAbility[]> | null = null;

function useWeaponAbilities(): WeaponAbility[] {
  const [abilities, setAbilities] = useState<WeaponAbility[]>(cachedAbilities ?? []);

  useEffect(() => {
    if (cachedAbilities) {
      setAbilities(cachedAbilities);
      return;
    }
    if (!fetchPromise) {
      fetchPromise = fetchWeaponAbilities().then((data) => {
        cachedAbilities = data;
        return data;
      });
    }
    fetchPromise.then(setAbilities);
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

interface Props {
  text: string | null;
}

export function WeaponAbilityText({ text }: Props) {
  const abilities = useWeaponAbilities();

  if (!text || text === "-") {
    return <span>-</span>;
  }

  if (abilities.length === 0) {
    return <span dangerouslySetInnerHTML={{ __html: text }} />;
  }

  const matches = findAbilityMatches(text, abilities);

  if (matches.length === 0) {
    return <span dangerouslySetInnerHTML={{ __html: text }} />;
  }

  const parts: React.ReactNode[] = [];
  let lastEnd = 0;

  for (const match of matches) {
    if (match.start > lastEnd) {
      parts.push(
        <span key={`text-${lastEnd}`} dangerouslySetInnerHTML={{ __html: text.slice(lastEnd, match.start) }} />
      );
    }
    parts.push(
      <span key={`ability-${match.start}`} className="weapon-ability-tooltip" data-tooltip={match.ability.description}>
        {match.text}
      </span>
    );
    lastEnd = match.end;
  }

  if (lastEnd < text.length) {
    parts.push(<span key={`text-${lastEnd}`} dangerouslySetInnerHTML={{ __html: text.slice(lastEnd) }} />);
  }

  return <>{parts}</>;
}
