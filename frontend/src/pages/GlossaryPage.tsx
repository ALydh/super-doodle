import { useEffect, useState } from "react";
import type { WeaponAbility, CoreAbility } from "../types";
import { fetchWeaponAbilities, fetchCoreAbilities } from "../api";
import { sanitizeHtml } from "../sanitize";
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
  const [search, setSearch] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([fetchWeaponAbilities(), fetchCoreAbilities()])
      .then(([w, c]) => {
        setWeaponAbilities(w);
        setCoreAbilities(c);
      })
      .catch((e) => setError(e.message));
  }, []);

  if (error) return <div className="error-message">{error}</div>;

  const lowerSearch = search.toLowerCase();
  const filteredWeapon = weaponAbilities
    .filter((a) => a.name.toLowerCase().includes(lowerSearch))
    .sort((a, b) => a.name.localeCompare(b.name));
  const filteredCore = coreAbilities
    .filter((a) => a.name.toLowerCase().includes(lowerSearch))
    .sort((a, b) => a.name.localeCompare(b.name));

  const noResults = filteredWeapon.length === 0 && filteredCore.length === 0;

  return (
    <div className={styles.page}>
      <h1>Glossary</h1>
      <input
        autoFocus
        className={styles.search}
        type="text"
        placeholder="Search abilities... (Ctrl+K)"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
      />

      {noResults && <p className={styles.empty}>No matching abilities found.</p>}

      {filteredWeapon.length > 0 && (
        <div className={styles.section}>
          <h2>Weapon Abilities</h2>
          {filteredWeapon.map((a) => (
            <GlossaryEntry key={a.id} name={a.name} description={a.description} />
          ))}
        </div>
      )}

      {filteredCore.length > 0 && (
        <div className={styles.section}>
          <h2>Core Abilities</h2>
          {filteredCore.map((a) => (
            <GlossaryEntry key={a.id} name={a.name} description={a.description} />
          ))}
        </div>
      )}
    </div>
  );
}
