import { useEffect, useState, useRef, useCallback, useMemo } from "react";
import { useParams, useNavigate, useLocation, Link } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import type {
  ArmyUnit, BattleSize, Datasheet, UnitCost, Enhancement,
  DetachmentInfo, DatasheetLeader, ValidationError, Army, DetachmentAbility, Stratagem, DatasheetOption,
  AlliedFactionInfo, DatasheetKeyword,
} from "../types";
import {
  fetchDetachmentsByFaction,
  fetchEnhancementsByFaction, fetchLeadersByFaction,
  fetchArmy, createArmy, updateArmy, validateArmy, fetchDetachmentAbilities,
  fetchStratagemsByFaction, fetchDatasheetDetailsByFaction, fetchAvailableAllies,
  fetchInventory,
} from "../api";
import { UnitPicker } from "./UnitPicker";
import { ValidationErrors } from "./ValidationErrors";
import { getFactionTheme } from "../factionTheme";
import { renderUnitsForMode } from "./renderUnitsForMode";
import { ReferenceDataProvider } from "../context/ReferenceDataContext";
import { DetachmentAbilitiesSection } from "./DetachmentAbilitiesSection";
import { StrategemsSection } from "./StrategemsSection";
import { PointsDisplay } from "./PointsDisplay";
import { SM_CHAPTERS, CHAPTER_DETACHMENTS, ALL_CHAPTER_DETACHMENT_IDS, getChapterTheme, isSpaceMarines } from "../chapters";
import styles from "./ArmyBuilderPage.module.css";

export function ArmyBuilderPage() {
  const { factionId, armyId } = useParams<{ factionId?: string; armyId?: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const routerFactionId: string = (location.state as { factionId?: string } | null)?.factionId ?? "";
  const { user, loading: authLoading } = useAuth();
  const isEdit = !!armyId;

  const [name, setName] = useState("");
  const [battleSize, setBattleSize] = useState<BattleSize>("StrikeForce");
  const [detachmentId, setDetachmentId] = useState("");
  const [units, setUnits] = useState<ArmyUnit[]>([]);
  const [warlordId, setWarlordId] = useState("");
  const [chapterId, setChapterId] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<ValidationError[]>([]);

  const [datasheets, setDatasheets] = useState<Datasheet[] | null>(null);
  const [allCosts, setAllCosts] = useState<UnitCost[]>([]);
  const [detachments, setDetachments] = useState<DetachmentInfo[]>([]);
  const [enhancements, setEnhancements] = useState<Enhancement[]>([]);
  const [leaders, setLeaders] = useState<DatasheetLeader[]>([]);
  const [allOptions, setAllOptions] = useState<DatasheetOption[]>([]);
  const [detachmentAbilities, setDetachmentAbilities] = useState<DetachmentAbility[]>([]);
  const [allStratagems, setAllStratagems] = useState<Stratagem[]>([]);
  const [pickerExpanded, setPickerExpanded] = useState(false);
  const [loadedArmyFactionId, setLoadedArmyFactionId] = useState<string>("");
  const [alliedFactions, setAlliedFactions] = useState<AlliedFactionInfo[]>([]);
  const [alliedCosts, setAlliedCosts] = useState<UnitCost[]>([]);
  const [keywordsByDatasheet, setKeywordsByDatasheet] = useState<Map<string, DatasheetKeyword[]>>(new Map());
  const [inventory, setInventory] = useState<Map<string, number> | null>(null);

  const validateTimerRef = useRef<ReturnType<typeof setTimeout>>(undefined);
  const detachmentInitializedRef = useRef(false);

  // Derive effective faction ID: for edit mode use loaded army's faction, for create use URL param
  const effectiveFactionId = useMemo(() => {
    if (isEdit) {
      return loadedArmyFactionId || routerFactionId;
    }
    return factionId ?? "";
  }, [isEdit, loadedArmyFactionId, routerFactionId, factionId]);

  // Derive loading state from data presence
  const loading = effectiveFactionId !== "" && datasheets === null;

  useEffect(() => {
    if (isEdit && armyId) {
      fetchArmy(armyId).then((persisted) => {
        setName(persisted.name);
        setBattleSize(persisted.army.battleSize);
        setDetachmentId(persisted.army.detachmentId);
        detachmentInitializedRef.current = true;
        const rawUnits = persisted.army.units.map(unit => ({
          ...unit,
          wargearSelections: unit.wargearSelections ?? [],
          attachedToUnitIndex: unit.attachedToUnitIndex ?? null,
        }));
        const claimedIndices = new Set<number>();
        const migratedUnits = rawUnits.map(unit => {
          if (!unit.attachedLeaderId || unit.attachedToUnitIndex != null) return unit;
          const bodyguardIndex = rawUnits.findIndex((u, i) =>
            u.datasheetId === unit.attachedLeaderId && !claimedIndices.has(i)
          );
          if (bodyguardIndex >= 0) claimedIndices.add(bodyguardIndex);
          return { ...unit, attachedToUnitIndex: bodyguardIndex >= 0 ? bodyguardIndex : null };
        });
        setUnits(migratedUnits);
        setWarlordId(persisted.army.warlordId);
        setChapterId(persisted.army.chapterId ?? null);
        setLoadedArmyFactionId(persisted.army.factionId);
      });
    }
  }, [isEdit, armyId]);

  useEffect(() => {
    if (!effectiveFactionId) return;
    let cancelled = false;

    Promise.all([
      fetchDetachmentsByFaction(effectiveFactionId),
      fetchEnhancementsByFaction(effectiveFactionId),
      fetchLeadersByFaction(effectiveFactionId),
      fetchStratagemsByFaction(effectiveFactionId),
      fetchDatasheetDetailsByFaction(effectiveFactionId),
      fetchAvailableAllies(effectiveFactionId),
    ]).then(([det, enh, ldr, strat, details, allies]) => {
      if (cancelled) return;
      setDetachments(det);
      setEnhancements(enh);
      setLeaders(ldr);
      setAllStratagems(strat);
      setAlliedFactions(allies);
      if (!detachmentInitializedRef.current && det.length > 0) {
        setDetachmentId(det[0].detachmentId);
        detachmentInitializedRef.current = true;
      }
      const costs = details.flatMap((d) => d.costs);
      const options = details.flatMap((d) => d.options);
      setAllCosts(costs);
      setAllOptions(options);
      setDatasheets(details.map((d) => d.datasheet));

      const kwMap = new Map<string, DatasheetKeyword[]>();
      for (const d of details) {
        kwMap.set(d.datasheet.id, d.keywords);
      }
      setKeywordsByDatasheet(kwMap);

      const alliedDatasheetIds = allies.flatMap((a) => a.datasheets.map((d) => d.id));
      if (alliedDatasheetIds.length > 0) {
        Promise.all(allies.map((a) => fetchDatasheetDetailsByFaction(a.factionId)))
          .then((alliedDetails) => {
            const alliedCostsData = alliedDetails.flatMap((d) => d.flatMap((dd) => dd.costs));
            setAlliedCosts(alliedCostsData);
          });
      }
    });

    return () => { cancelled = true; };
  }, [effectiveFactionId]);

  useEffect(() => {
    if (!detachmentId) return;
    fetchDetachmentAbilities(detachmentId).then(setDetachmentAbilities);
  }, [detachmentId]);

  useEffect(() => {
    if (!user) return;
    fetchInventory().then((entries) => {
      const map = new Map<string, number>();
      for (const e of entries) {
        map.set(e.datasheetId, e.quantity);
      }
      setInventory(map);
    }).catch(() => {
      // Inventory is optional, don't block the page
    });
  }, [user]);

  const buildArmy = useCallback((): Army | null => {
    if (!effectiveFactionId || !detachmentId) return null;
    return {
      factionId: effectiveFactionId,
      battleSize,
      detachmentId,
      warlordId: warlordId || (units[0]?.datasheetId ?? ""),
      units,
      chapterId,
    };
  }, [effectiveFactionId, battleSize, detachmentId, warlordId, units, chapterId]);

  useEffect(() => {
    if (loading) return;
    clearTimeout(validateTimerRef.current);
    validateTimerRef.current = setTimeout(() => {
      const army = buildArmy();
      if (army && army.units.length > 0) {
        validateArmy(army).then((res) => setValidationErrors(res.errors));
      } else {
        setValidationErrors([]);
      }
    }, 500);
    return () => clearTimeout(validateTimerRef.current);
  }, [units, battleSize, detachmentId, warlordId, loading, buildArmy]);

  const combinedCosts = [...allCosts, ...alliedCosts];
  const pointsTotal = units.reduce((sum, u) => {
    const cost = combinedCosts.find((c) => c.datasheetId === u.datasheetId && c.line === u.sizeOptionLine);
    const enhCost = u.enhancementId ? enhancements.find((e) => e.id === u.enhancementId)?.cost ?? 0 : 0;
    return sum + (cost?.cost ?? 0) + enhCost;
  }, 0);

  const handleAddUnit = (datasheetId: string, sizeOptionLine: number, isAllied?: boolean) => {
    setUnits([...units, { datasheetId, sizeOptionLine, enhancementId: null, attachedLeaderId: null, attachedToUnitIndex: null, wargearSelections: [], isAllied }]);
  };

  const handleUpdateUnit = (index: number, unit: ArmyUnit) => {
    const next = [...units];
    next[index] = unit;
    setUnits(next);
  };

  const handleRemoveUnit = (index: number) => {
    setUnits(units.filter((_, i) => i !== index));
  };

  const handleCopyUnit = (index: number) => {
    const unitToCopy = units[index];
    const copiedUnit: ArmyUnit = {
      ...unitToCopy,
      enhancementId: null,
      attachedLeaderId: null,
      attachedToUnitIndex: null,
    };
    setUnits([...units, copiedUnit]);
  };

  const handleSetWarlord = (index: number) => {
    setWarlordId(units[index].datasheetId);
  };

  const handleSave = async () => {
    const army = buildArmy();
    if (!army) return;
    if (isEdit && armyId) {
      await updateArmy(armyId, name, army);
      navigate(`/armies/${armyId}`);
    } else {
      const persisted = await createArmy(name, army);
      navigate(`/armies/${persisted.id}`);
    }
  };

  if (authLoading) return <div>Loading...</div>;

  if (!user) {
    return (
      <div>
        <p>You must be logged in to create or edit armies.</p>
        <Link to="/login">Login</Link> or <Link to="/register">Register</Link>
      </div>
    );
  }

  if (loading) return <div>Loading...</div>;

  const isSM = isSpaceMarines(effectiveFactionId);
  const chapterTheme = isSM && chapterId ? getChapterTheme(chapterId) : null;
  const factionTheme = chapterTheme ?? getFactionTheme(effectiveFactionId);
  const selectedChapter = isSM ? SM_CHAPTERS.find((c) => c.id === chapterId) ?? null : null;
  const alliedDatasheets = alliedFactions.flatMap((a) => a.datasheets);
  const loadedDatasheets = [...(datasheets ?? []), ...alliedDatasheets];

  const chapterDetachmentIds = chapterId ? new Set(CHAPTER_DETACHMENTS[chapterId] ?? []) : null;
  const sortedDetachments = chapterDetachmentIds
    ? detachments
        .filter((d) => chapterDetachmentIds.has(d.detachmentId) || !ALL_CHAPTER_DETACHMENT_IDS.has(d.detachmentId))
        .sort((a, b) => {
          const aIsChapter = chapterDetachmentIds.has(a.detachmentId);
          const bIsChapter = chapterDetachmentIds.has(b.detachmentId);
          if (aIsChapter && !bIsChapter) return -1;
          if (!aIsChapter && bIsChapter) return 1;
          return 0;
        })
    : detachments;

  const filteredStratagems = allStratagems.filter((s) => s.detachmentId === detachmentId);

  return (
    <div data-faction={factionTheme} data-page="army-builder" className={`${styles.page} ${styles.layoutA}`}>
      {factionTheme && (
        <img
          src={`/icons/${chapterTheme ? "space-marines" : factionTheme}.svg`}
          alt=""
          className={styles.bgIcon}
          aria-hidden="true"
        />
      )}

      <div className={styles.header}>
        <h1 className={styles.title}>{isEdit ? "Edit Army" : "Create Army"}</h1>
      </div>

      <div className={styles.content}>
        <div className={`${styles.col} ${styles.colInfo}`}>
          <div className={styles.colHeader}>Army Info</div>
          <div className={styles.settings}>
            <label>
              Army Name
              <input
                className={styles.nameInput}
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
            </label>
            <label>
              Battle Size
              <select
                className={styles.sizeSelect}
                value={battleSize}
                onChange={(e) => setBattleSize(e.target.value as BattleSize)}
              >
                <option value="Incursion">Incursion (1000pts)</option>
                <option value="StrikeForce">Strike Force (2000pts)</option>
                <option value="Onslaught">Onslaught (3000pts)</option>
              </select>
            </label>
            {isSM && (
              <label>
                Chapter
                <select
                  className="chapter-select"
                  value={chapterId ?? ""}
                  onChange={(e) => setChapterId(e.target.value || null)}
                >
                  <option value="">No Chapter</option>
                  {SM_CHAPTERS.map((c) => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </label>
            )}
            <label>
              Detachment
              <select
                className={styles.detachmentSelect}
                value={detachmentId}
                onChange={(e) => setDetachmentId(e.target.value)}
              >
                {sortedDetachments.map((d) => (
                  <option key={d.detachmentId} value={d.detachmentId}>{d.name}</option>
                ))}
              </select>
            </label>
          </div>
          <PointsDisplay total={pointsTotal} battleSize={battleSize} />
          <DetachmentAbilitiesSection abilities={detachmentAbilities} />
          <StrategemsSection stratagems={filteredStratagems} />
          <button className={styles.btnSave} onClick={handleSave} disabled={!name.trim()}>
            {isEdit ? "Update Army" : "Save Army"}
          </button>
        </div>

        <div className={`${styles.col} ${styles.colUnits}`}>
          <div className={styles.colHeader}>Selected Units</div>
          <ValidationErrors errors={validationErrors} datasheets={loadedDatasheets} />
          <ReferenceDataProvider
            costs={combinedCosts}
            enhancements={enhancements.filter((e) => !e.detachmentId || e.detachmentId === detachmentId)}
            leaders={leaders}
            datasheets={loadedDatasheets}
            options={allOptions}
          >
            <div className={styles.unitsWrapper}>
              <table className={styles.unitsTable}>
                <thead>
                  <tr>
                    <th>Unit</th>
                    <th>Size</th>
                    <th>Enhancement</th>
                    <th>Leader</th>
                    <th>Wargear</th>
                    <th>Cost</th>
                    <th>Warlord</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {renderUnitsForMode(
                    units,
                    loadedDatasheets,
                    warlordId,
                    handleUpdateUnit,
                    handleRemoveUnit,
                    handleCopyUnit,
                    handleSetWarlord
                  )}
                </tbody>
              </table>
            </div>
          </ReferenceDataProvider>
        </div>

        <div className={`${styles.col} ${styles.colPicker} ${pickerExpanded ? "" : styles.collapsed}`}>
          <button className={`${styles.colHeader} ${styles.colHeaderToggle}`} onClick={() => setPickerExpanded(!pickerExpanded)}>
            Add Units {pickerExpanded ? "▼" : "▶"}
          </button>
          {pickerExpanded && (
            <UnitPicker
              datasheets={datasheets ?? []}
              costs={allCosts}
              onAdd={handleAddUnit}
              alliedFactions={alliedFactions}
              alliedCosts={alliedCosts}
              chapterKeyword={selectedChapter?.keyword ?? null}
              keywordsByDatasheet={keywordsByDatasheet}
              inventory={inventory}
            />
          )}
        </div>
      </div>
    </div>
  );
}
