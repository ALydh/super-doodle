import type { Faction, Datasheet, Stratagem, Enhancement, WeaponAbility, CoreAbility, ArmySummary } from "./types";
import { glossarySections } from "./data/glossary";
import { fuzzyScore } from "./fuzzyScore";

export type ResultItem =
  | { type: "navigate"; name: string; subtitle?: string; action: () => void }
  | { type: "expand"; name: string; subtitle?: string; description: string };

export interface ResultSection {
  title: string;
  items: ResultItem[];
}

const MAX_RESULTS_PER_SECTION = 10;

interface SpotlightData {
  search: string;
  armies: ArmySummary[];
  factions: Faction[];
  datasheets: Datasheet[];
  stratagems: Stratagem[];
  enhancements: Enhancement[];
  weaponAbilities: WeaponAbility[];
  coreAbilities: CoreAbility[];
  factionNameMap: Map<string, string>;
  user: unknown;
  go: (path: string) => void;
  onClose: () => void;
  toggleCompact: () => void;
}

export function buildSpotlightSections(data: SpotlightData): ResultSection[] {
  const {
    search, armies, factions, datasheets, stratagems, enhancements,
    weaponAbilities, coreAbilities, factionNameMap, user, go, onClose, toggleCompact,
  } = data;

  const lowerSearch = search.toLowerCase();
  const hasSearch = lowerSearch.length > 0;

  const filterSorted = <T,>(items: T[], getName: (item: T) => string) => {
    if (!hasSearch) {
      return items
        .sort((a, b) => getName(a).localeCompare(getName(b)))
        .slice(0, MAX_RESULTS_PER_SECTION);
    }
    return items
      .map((item) => ({ item, score: fuzzyScore(getName(item), lowerSearch) }))
      .filter(({ score }) => score !== null)
      .sort((a, b) =>
        b.score !== a.score
          ? (b.score ?? 0) - (a.score ?? 0)
          : getName(a.item).localeCompare(getName(b.item))
      )
      .map(({ item }) => item)
      .slice(0, MAX_RESULTS_PER_SECTION);
  };

  const result: ResultSection[] = [];

  const filteredArmies = filterSorted(armies, (a) => a.name);
  if (filteredArmies.length > 0) {
    result.push({
      title: "Armies",
      items: filteredArmies.map((a) => ({
        type: "navigate",
        name: a.name,
        subtitle: `${a.totalPoints} pts`,
        action: () => go(`/armies/${a.id}`),
      })),
    });
  }

  const commandDefs: { name: string; action: () => void }[] = [
    { name: "Home", action: () => go("/") },
    { name: "Glossary", action: () => go("/glossary") },
    { name: "Toggle compact mode", action: () => { toggleCompact(); onClose(); } },
  ];
  if (user) {
    commandDefs.push({ name: "Admin", action: () => go("/admin") });
  } else {
    commandDefs.push({ name: "Login", action: () => go("/login") });
    commandDefs.push({ name: "Register", action: () => go("/register") });
  }
  const filteredCommands = filterSorted(commandDefs, (c) => c.name);
  if (filteredCommands.length > 0) {
    result.push({
      title: "Commands",
      items: filteredCommands.map((c) => ({ type: "navigate", name: c.name, action: c.action })),
    });
  }

  const filteredFactions = filterSorted(factions, (f) => f.name);
  if (filteredFactions.length > 0) {
    result.push({
      title: "Factions",
      items: filteredFactions.map((f) => ({ type: "navigate", name: f.name, action: () => go(`/factions/${f.id}`) })),
    });
  }

  const filteredDatasheets = filterSorted(datasheets, (d) => d.name);
  if (filteredDatasheets.length > 0) {
    result.push({
      title: "Units",
      items: filteredDatasheets.map((d) => ({
        type: "navigate",
        name: d.name,
        subtitle: d.factionId ? factionNameMap.get(d.factionId) : undefined,
        action: () => go(d.factionId ? `/factions/${d.factionId}?unit=${d.id}` : "/"),
      })),
    });
  }

  if (!hasSearch) return result;

  const filteredWeapon = filterSorted(weaponAbilities, (a) => a.name);
  if (filteredWeapon.length > 0) {
    result.push({
      title: "Weapon Abilities",
      items: filteredWeapon.map((a) => ({ type: "expand", name: a.name, description: a.description })),
    });
  }

  const filteredCore = filterSorted(coreAbilities, (a) => a.name);
  if (filteredCore.length > 0) {
    result.push({
      title: "Core Abilities",
      items: filteredCore.map((a) => ({ type: "expand", name: a.name, description: a.description })),
    });
  }

  const filteredStratagems = filterSorted(stratagems, (s) => s.name);
  if (filteredStratagems.length > 0) {
    result.push({
      title: "Stratagems",
      items: filteredStratagems.map((s) => {
        const cpSubtitle = s.cpCost != null ? `${s.cpCost} CP` : undefined;
        const subtitle = s.detachment && cpSubtitle
          ? `${cpSubtitle} · ${s.detachment}`
          : cpSubtitle ?? (s.detachment ?? undefined);
        if (s.factionId) {
          return {
            type: "navigate" as const,
            name: s.name,
            subtitle,
            action: () => go(`/factions/${s.factionId}?tab=stratagems`),
          };
        }
        return {
          type: "expand" as const,
          name: s.name,
          subtitle,
          description: s.description,
        };
      }),
    });
  }

  const filteredEnhancements = filterSorted(enhancements, (e) => e.name);
  if (filteredEnhancements.length > 0) {
    result.push({
      title: "Enhancements",
      items: filteredEnhancements.map((e) => {
        const subtitle = e.detachment
          ? `${e.cost} pts · ${e.detachment}`
          : `${e.cost} pts`;
        if (e.factionId && e.detachmentId) {
          return {
            type: "navigate" as const,
            name: e.name,
            subtitle,
            action: () => go(`/factions/${e.factionId}?detachment=${e.detachmentId}`),
          };
        }
        return {
          type: "expand" as const,
          name: e.name,
          subtitle,
          description: e.description,
        };
      }),
    });
  }

  for (const section of glossarySections) {
    const filtered = filterSorted(section.entries, (e) => e.name);
    if (filtered.length > 0) {
      result.push({
        title: section.title,
        items: filtered.map((e) => ({ type: "expand", name: e.name, description: e.description })),
      });
    }
  }

  return result;
}
