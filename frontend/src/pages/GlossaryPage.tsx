import { useEffect, useState } from "react";
import type { WeaponAbility, CoreAbility, Faction } from "../types";
import { fetchWeaponAbilities, fetchCoreAbilities, fetchFactions } from "../api";
import { glossarySections } from "../data/glossary";
import { sanitizeHtml } from "../sanitize";
import { ErrorMessage } from "../components/ErrorMessage";
import styles from "./GlossaryPage.module.css";

interface EntryProps {
  name: string;
  description: string;
}

function GlossaryEntry({ name, description }: EntryProps) {
  const [open, setOpen] = useState(false);

  return (
    <div className={styles.entry} onClick={() => setOpen(!open)}>
      <div className={styles.entryHeader}>
        <span className={styles.entryName}>{name}</span>
        <span className={styles.entryToggle} data-open={open || undefined}>&#9660;</span>
      </div>
      {open && (
        <div
          className={styles.entryDescription}
          dangerouslySetInnerHTML={{ __html: sanitizeHtml(description) }}
        />
      )}
    </div>
  );
}

export function GlossaryPage() {
  const [weaponAbilities, setWeaponAbilities] = useState<WeaponAbility[]>([]);
  const [coreAbilities, setCoreAbilities] = useState<CoreAbility[]>([]);
  const [factions, setFactions] = useState<Faction[]>([]);
  const [search, setSearch] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([fetchWeaponAbilities(), fetchCoreAbilities(), fetchFactions()])
      .then(([w, c, f]) => {
        setWeaponAbilities(w);
        setCoreAbilities(c);
        setFactions(f);
      })
      .catch((e) => setError(e.message));
  }, []);

  const lowerSearch = search.toLowerCase();

  const filteredWeapon = weaponAbilities
    .filter((a) => a.name.toLowerCase().includes(lowerSearch))
    .sort((a, b) => a.name.localeCompare(b.name));
  const filteredCore = coreAbilities
    .filter((a) => a.name.toLowerCase().includes(lowerSearch))
    .sort((a, b) => a.name.localeCompare(b.name));

  const generalCore = filteredCore.filter((a) => !a.factionId);
  const factionCore = filteredCore.filter((a) => a.factionId);

  const factionNameById = new Map(factions.map((f) => [f.id, f.name]));
  const factionGroups = new Map<string, CoreAbility[]>();
  for (const a of factionCore) {
    const list = factionGroups.get(a.factionId!);
    if (list) list.push(a);
    else factionGroups.set(a.factionId!, [a]);
  }
  const sortedFactionIds = [...factionGroups.keys()].sort((a, b) =>
    (factionNameById.get(a) ?? a).localeCompare(factionNameById.get(b) ?? b)
  );

  const filteredSections = glossarySections
    .map((section) => ({
      ...section,
      entries: section.entries
        .filter((e) => e.name.toLowerCase().includes(lowerSearch))
        .sort((a, b) => a.name.localeCompare(b.name)),
    }))
    .filter((section) => section.entries.length > 0);

  const noResults =
    filteredWeapon.length === 0 &&
    filteredCore.length === 0 &&
    filteredSections.length === 0;

  if (error) return <ErrorMessage message={error} />;

  return (
    <div className={styles.page}>
      <h1>Glossary</h1>
      <input
        autoFocus
        className={styles.search}
        type="text"
        placeholder="Search glossary... (Ctrl+K)"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
      />

      {noResults && <p className={styles.empty}>No matching entries found.</p>}

      {filteredWeapon.length > 0 && (
        <div className={styles.section}>
          <h2>Weapon Abilities</h2>
          {filteredWeapon.map((a) => (
            <GlossaryEntry key={a.id} name={a.name} description={a.description} />
          ))}
        </div>
      )}

      {generalCore.length > 0 && (
        <div className={styles.section}>
          <h2>Core Abilities</h2>
          {generalCore.map((a) => (
            <GlossaryEntry key={a.id} name={a.name} description={a.description} />
          ))}
        </div>
      )}

      {sortedFactionIds.map((fid) => {
        const abilities = factionGroups.get(fid)!;
        const factionName = factionNameById.get(fid) ?? fid;
        return (
          <div key={fid} className={styles.section}>
            <h2>{factionName} Abilities</h2>
            {abilities.map((a) => (
              <GlossaryEntry key={a.id} name={a.name} description={a.description} />
            ))}
          </div>
        );
      })}

      {filteredSections.map((section) => (
        <div className={styles.section} key={section.title}>
          <h2>{section.title}</h2>
          {section.entries.map((e) => (
            <GlossaryEntry key={e.name} name={e.name} description={e.description} />
          ))}
        </div>
      ))}
    </div>
  );
}
