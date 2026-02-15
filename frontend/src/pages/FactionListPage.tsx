import { useEffect, useState, useRef } from "react";
import { Link, useNavigate } from "react-router-dom";
import type { Faction, ArmySummary, Army } from "../types";
import { fetchFactions, fetchAllArmies, createArmy } from "../api";
import { getFactionTheme } from "../factionTheme";
import { isSpaceMarines, SM_CHAPTERS, getChapterTheme } from "../chapters";
import { useAuth } from "../context/useAuth";
import styles from "./FactionListPage.module.css";

type FactionGroup = "Imperium" | "Chaos" | "Xenos";

const FACTION_GROUPS: Record<string, FactionGroup> = {
  AS: "Imperium",
  AC: "Imperium",
  AdM: "Imperium",
  TL: "Imperium",
  AM: "Imperium",
  GK: "Imperium",
  AoI: "Imperium",
  QI: "Imperium",
  SM: "Imperium",
  CD: "Chaos",
  QT: "Chaos",
  CSM: "Chaos",
  DG: "Chaos",
  EC: "Chaos",
  TS: "Chaos",
  WE: "Chaos",
  AE: "Xenos",
  DRU: "Xenos",
  GC: "Xenos",
  LoV: "Xenos",
  NEC: "Xenos",
  ORK: "Xenos",
  TAU: "Xenos",
  TYR: "Xenos",
};

const GROUP_ORDER: FactionGroup[] = ["Imperium", "Chaos", "Xenos"];

function formatDate(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays === 0) return "Today";
  if (diffDays === 1) return "Yesterday";
  if (diffDays < 7) return `${diffDays} days ago`;
  return date.toLocaleDateString();
}

export function FactionListPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [factions, setFactions] = useState<Faction[]>([]);
  const [armies, setArmies] = useState<ArmySummary[]>([]);
  const [error, setError] = useState<string | null>(null);
  const importInputRef = useRef<HTMLInputElement>(null);

  const handleImport = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const text = await file.text();
      const data = JSON.parse(text) as { name: string; army: Army };
      const persisted = await createArmy(data.name, data.army);
      navigate(`/armies/${persisted.id}`);
    } catch {
      alert("Invalid army file.");
    }
  };

  useEffect(() => {
    Promise.all([fetchFactions(), fetchAllArmies()])
      .then(([f, a]) => {
        setFactions(f);
        setArmies(a);
      })
      .catch((e) => setError(e.message));
  }, []);

  if (error) return <div className="error-message">{error}</div>;

  const factionMap = new Map(factions.map((f) => [f.id, f]));
  const excludedFactions = ["Unbound Adversaries", "Unaligned Forces"];
  const playableFactions = factions.filter((f) => !excludedFactions.includes(f.name));

  const myArmies = armies.filter((a) => user && a.ownerId === user.id);
  const otherArmies = armies.filter((a) => !user || a.ownerId !== user.id);

  const groupedFactions = GROUP_ORDER.map((group) => ({
    group,
    factions: playableFactions
      .filter((f) => FACTION_GROUPS[f.id] === group)
      .sort((a, b) => a.name.localeCompare(b.name)),
  }));

  function getArmyTheme(army: ArmySummary) {
    const baseTheme = getFactionTheme(army.factionId);
    if (army.chapterId && isSpaceMarines(army.factionId)) {
      const chapter = SM_CHAPTERS.find((c) => c.id === army.chapterId);
      return {
        theme: getChapterTheme(army.chapterId),
        icon: baseTheme,
        displayName: chapter?.name ?? factionMap.get(army.factionId)?.name ?? army.factionId,
      };
    }
    return {
      theme: baseTheme,
      icon: baseTheme,
      displayName: factionMap.get(army.factionId)?.name ?? army.factionId,
    };
  }

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <h1>Armies</h1>
        {user && (
          <>
            <button className={styles.importBtn} onClick={() => importInputRef.current?.click()}>Import Army</button>
            <input ref={importInputRef} type="file" accept=".json" hidden onChange={handleImport} />
          </>
        )}
      </div>

      {armies.length === 0 ? (
        <div className={styles.emptyState}>
          <p>No armies yet. Browse a faction to get started.</p>
          {groupedFactions.map(({ group, factions: groupFactions }) => (
            <div key={group} className={styles.factionGroup}>
              <h3 className={styles.factionGroupTitle}>{group}</h3>
              <div className={styles.factionCards}>
                {groupFactions.map((f) => {
                  const factionTheme = getFactionTheme(f.id);
                  return (
                    <Link
                      key={f.id}
                      to={`/factions/${f.id}`}
                      className={styles.factionCard}
                      data-faction={factionTheme}
                    >
                      {factionTheme && (
                        <img
                          src={`/icons/${factionTheme}.svg`}
                          alt=""
                          className={styles.factionCardIcon}
                          aria-hidden="true"
                        />
                      )}
                      <span className={styles.factionCardName}>{f.name}</span>
                    </Link>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <>
          {myArmies.length > 0 && (
            <>
              <h2>My Armies</h2>
              <div className={styles.armyCards}>
                {myArmies.map((army) => {
                  const { theme, icon, displayName } = getArmyTheme(army);

                  return (
                    <Link
                      key={army.id}
                      to={`/armies/${army.id}`}
                      className={styles.armyCard}
                      data-faction={theme}
                    >
                      {icon && (
                        <img
                          src={`/icons/${icon}.svg`}
                          alt=""
                          className={styles.armyCardIcon}
                          aria-hidden="true"
                        />
                      )}
                      <div className={styles.armyCardHeader}>
                        <span className={styles.armyCardFaction}>
                          {displayName}
                        </span>
                        <span className={styles.armyCardSize}>
                          {army.totalPoints} pts
                        </span>
                      </div>
                      <h3 className={styles.armyCardName}>{army.name}</h3>
                      {army.warlordName && (
                        <div className={styles.armyCardWarlord}>{army.warlordName}</div>
                      )}
                      <div className={styles.armyCardFooter}>
                        <span className={styles.armyCardBattleSize}>
                          {army.battleSize}
                        </span>
                        <span className={styles.armyCardUpdated}>
                          {formatDate(army.updatedAt)}
                        </span>
                      </div>
                    </Link>
                  );
                })}
              </div>
            </>
          )}

          {otherArmies.length > 0 && (
            <>
              <h2>Other Armies</h2>
              <div className={styles.armyCards}>
                {otherArmies.map((army) => {
                  const { theme, icon, displayName } = getArmyTheme(army);

                  return (
                    <Link
                      key={army.id}
                      to={`/armies/${army.id}`}
                      className={styles.armyCard}
                      data-faction={theme}
                    >
                      {icon && (
                        <img
                          src={`/icons/${icon}.svg`}
                          alt=""
                          className={styles.armyCardIcon}
                          aria-hidden="true"
                        />
                      )}
                      <div className={styles.armyCardHeader}>
                        <span className={styles.armyCardFaction}>
                          {displayName}
                        </span>
                        <span className={styles.armyCardSize}>
                          {army.totalPoints} pts
                        </span>
                      </div>
                      <h3 className={styles.armyCardName}>{army.name}</h3>
                      {army.warlordName && (
                        <div className={styles.armyCardWarlord}>{army.warlordName}</div>
                      )}
                      <div className={styles.armyCardFooter}>
                        <span className={styles.armyCardBattleSize}>
                          {army.battleSize}
                        </span>
                        <span className={styles.armyCardUpdated}>
                          {formatDate(army.updatedAt)}
                        </span>
                      </div>
                      {army.ownerName && (
                        <div className={styles.armyCardOwner}>{army.ownerName}</div>
                      )}
                    </Link>
                  );
                })}
              </div>
            </>
          )}

          <div className={styles.newArmySection}>
            <h2>Explore Factions</h2>
            {groupedFactions.map(({ group, factions: groupFactions }) => (
              <div key={group} className={styles.factionGroup}>
                <h3 className={styles.factionGroupTitle}>{group}</h3>
                <div className={styles.factionCards}>
                  {groupFactions.map((f) => {
                    const factionTheme = getFactionTheme(f.id);
                    return (
                      <Link
                        key={f.id}
                        to={`/factions/${f.id}`}
                        className={styles.factionCard}
                        data-faction={factionTheme}
                      >
                        {factionTheme && (
                          <img
                            src={`/icons/${factionTheme}.svg`}
                            alt=""
                            className={styles.factionCardIcon}
                            aria-hidden="true"
                          />
                        )}
                        <span className={styles.factionCardName}>{f.name}</span>
                      </Link>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
