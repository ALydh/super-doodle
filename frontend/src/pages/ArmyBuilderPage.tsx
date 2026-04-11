import { useEffect, useState, useRef, useCallback } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import type {
  ArmyUnit, BattleSize, Datasheet, UnitCost, Enhancement,
  DetachmentInfo, DatasheetLeader, ValidationError, Army, DetachmentAbility, Stratagem, DatasheetOption,
  AlliedFactionInfo, DatasheetKeyword, ModelProfile,
} from "../types";
import {
  fetchDetachmentsByFaction,
  fetchEnhancementsByFaction, fetchLeadersByFaction,
  createArmy, validateArmy, fetchDetachmentAbilities,
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
import { Spinner } from "../components/Spinner";
import { BATTLE_SIZE_POINTS } from "../types";
import styles from "./ArmyBuilderPage.module.css";

export function ArmyBuilderPage() {
  const { factionId } = useParams<{ factionId?: string }>();
  const navigate = useNavigate();
  const { user, loading: authLoading } = useAuth();

  const [name, setName] = useState("");
  const [battleSize, setBattleSize] = useState<BattleSize>("StrikeForce");
  const [detachmentId, setDetachmentId] = useState("");
  const [units, setUnits] = useState<ArmyUnit[]>([]);
  const [warlordId, setWarlordId] = useState("");
  const [chapterId, setChapterId] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<ValidationError[]>([]);
  const [expandedIndices, setExpandedIndices] = useState<Set<number>>(new Set());

  const [datasheets, setDatasheets] = useState<Datasheet[] | null>(null);
  const [allCosts, setAllCosts] = useState<UnitCost[]>([]);
  const [detachments, setDetachments] = useState<DetachmentInfo[]>([]);
  const [enhancements, setEnhancements] = useState<Enhancement[]>([]);
  const [leaders, setLeaders] = useState<DatasheetLeader[]>([]);
  const [allOptions, setAllOptions] = useState<DatasheetOption[]>([]);
  const [detachmentAbilities, setDetachmentAbilities] = useState<DetachmentAbility[]>([]);
  const [allStratagems, setAllStratagems] = useState<Stratagem[]>([]);
  const [alliedFactions, setAlliedFactions] = useState<AlliedFactionInfo[]>([]);
  const [alliedCosts, setAlliedCosts] = useState<UnitCost[]>([]);
  const [keywordsByDatasheet, setKeywordsByDatasheet] = useState<Map<string, DatasheetKeyword[]>>(new Map());
  const [profilesByDatasheet, setProfilesByDatasheet] = useState<Map<string, ModelProfile[]>>(new Map());
  const [inventory, setInventory] = useState<Map<string, number> | null>(null);

  const [pickerOpen, setPickerOpen] = useState(false);

  const validateTimerRef = useRef<ReturnType<typeof setTimeout>>(undefined);
  const detachmentInitializedRef = useRef(false);

  const effectiveFactionId = factionId ?? "";

  const loading = effectiveFactionId !== "" && datasheets === null;

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
      for (const d of details) kwMap.set(d.datasheet.id, d.keywords);
      setKeywordsByDatasheet(kwMap);
      setProfilesByDatasheet(new Map<string, ModelProfile[]>(details.map((d) => [d.datasheet.id, d.profiles])));
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
      for (const e of entries) map.set(e.datasheetId, e.quantity);
      setInventory(map);
    }).catch(() => {});
  }, [user]);

  const buildArmy = useCallback((): Army | null => {
    if (!effectiveFactionId || !detachmentId) return null;
    return { factionId: effectiveFactionId, battleSize, detachmentId, warlordId: warlordId || (units[0]?.datasheetId ?? ""), units, chapterId };
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
  const handleUpdateUnit = (index: number, unit: ArmyUnit) => { const next = [...units]; next[index] = unit; setUnits(next); };
  const handleRemoveUnit = (index: number) => { setUnits(units.filter((_, i) => i !== index)); };
  const handleCopyUnit = (index: number) => { const u = units[index]; setUnits([...units, { ...u, enhancementId: null, attachedLeaderId: null, attachedToUnitIndex: null }]); };
  const handleSetWarlord = (index: number) => { setWarlordId(units[index].datasheetId); };
  const handleToggleExpanded = (index: number) => {
    setExpandedIndices((prev) => {
      const next = new Set(prev);
      if (next.has(index)) next.delete(index);
      else next.add(index);
      return next;
    });
  };

  const handleSave = async () => {
    const army = buildArmy();
    if (!army) return;
    const persisted = await createArmy(name, army);
    navigate(`/armies/${persisted.id}`);
  };

  if (authLoading) return <Spinner />;
  if (!user) return (
    <div><p>You must be logged in to create or edit armies.</p>
      <Link to="/login">Login</Link> or <Link to="/register">Register</Link>
    </div>
  );
  if (loading) return <Spinner />;

  const isSM = isSpaceMarines(effectiveFactionId);
  const chapterTheme = isSM && chapterId ? getChapterTheme(chapterId) : null;
  const factionTheme = chapterTheme ?? getFactionTheme(effectiveFactionId);
  const selectedChapter = isSM ? SM_CHAPTERS.find((c) => c.id === chapterId) ?? null : null;
  const alliedDatasheets = alliedFactions.flatMap((a) => a.datasheets);
  const loadedDatasheets = [...(datasheets ?? []), ...alliedDatasheets];
  const chapterDetachmentIds = chapterId ? new Set(CHAPTER_DETACHMENTS[chapterId] ?? []) : null;
  const sortedDetachments = chapterDetachmentIds
    ? detachments.filter((d) => chapterDetachmentIds.has(d.detachmentId) || !ALL_CHAPTER_DETACHMENT_IDS.has(d.detachmentId))
        .sort((a, b) => (chapterDetachmentIds.has(a.detachmentId) ? 0 : 1) - (chapterDetachmentIds.has(b.detachmentId) ? 0 : 1))
    : detachments;
  const filteredStratagems = allStratagems.filter((s) => s.detachmentId === detachmentId);
  const maxPoints = BATTLE_SIZE_POINTS[battleSize] ?? 0;
  const detachmentName = detachments.find((d) => d.detachmentId === detachmentId)?.name ?? "";

  // ── Shared content blocks ────────────────────────────────────────────────

  const unitsContent = (
    <div className={styles.unitsPanel}>
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
            <thead><tr><th>Unit</th><th>Size</th><th>Enhancement</th><th>Leader</th><th>Wargear</th><th>Cost</th><th>Warlord</th><th></th></tr></thead>
            <tbody>
              {renderUnitsForMode(units, loadedDatasheets, warlordId, handleUpdateUnit, handleRemoveUnit, handleCopyUnit, handleSetWarlord, false, profilesByDatasheet, expandedIndices, handleToggleExpanded)}
            </tbody>
          </table>
        </div>
      </ReferenceDataProvider>
    </div>
  );

  const pickerContent = (
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
  );

  const bgIcon = factionTheme && (
    <img src={`/icons/${factionTheme}.svg`} alt="" className={styles.bgIcon} aria-hidden="true" />
  );

  return (
    <div data-faction={factionTheme} className={styles.page}>
      {bgIcon}
      <div className={styles.viewHeader}>
        <div className={styles.headerTop}>
          {factionTheme && <img src={`/icons/${factionTheme}.svg`} alt="" className={styles.headerIcon} />}
          <div className={styles.headerText}>
            <input
              className={styles.nameInputInline}
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Army name..."
            />
            <p className={styles.meta}>
              {detachmentName && <>{detachmentName} · </>}{battleSize} · <span className={pointsTotal > maxPoints ? styles.overBudget : styles.pointsOk}>{pointsTotal}/{maxPoints}pts</span>
            </p>
          </div>
        </div>
        <div className={styles.headerActions}>
          <button className={styles.btnSave} onClick={handleSave} disabled={!name.trim()}>
            Save
          </button>
        </div>
      </div>
      <details className={styles.settingsDetails}>
        <summary className={styles.settingsSummary}>Settings</summary>
        <div className={styles.settings}>
          <label>
            Battle Size
            <select className={styles.sizeSelect} value={battleSize} onChange={(e) => setBattleSize(e.target.value as BattleSize)}>
              <option value="Incursion">Incursion (1000pts)</option>
              <option value="StrikeForce">Strike Force (2000pts)</option>
              <option value="Onslaught">Onslaught (3000pts)</option>
            </select>
          </label>
          {isSM && (
            <label>
              Chapter
              <select value={chapterId ?? ""} onChange={(e) => setChapterId(e.target.value || null)}>
                <option value="">No Chapter</option>
                {SM_CHAPTERS.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </label>
          )}
          <label>
            Detachment
            <select className={styles.detachmentSelect} value={detachmentId} onChange={(e) => setDetachmentId(e.target.value)}>
              {sortedDetachments.map((d) => <option key={d.detachmentId} value={d.detachmentId}>{d.name}</option>)}
            </select>
          </label>
        </div>
        <PointsDisplay total={pointsTotal} battleSize={battleSize} />
        <DetachmentAbilitiesSection abilities={detachmentAbilities} />
        <StrategemsSection stratagems={filteredStratagems} />
      </details>
      <div className={styles.modalAddBar}>
        <button type="button" className={styles.addUnitBtn} onClick={() => setPickerOpen(true)}>
          + Add Units
        </button>
      </div>
      {unitsContent}
      {pickerOpen && (
        <div className={styles.modalOverlay} onClick={() => setPickerOpen(false)}>
          <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
            <div className={styles.modalHeader}>
              <span className={styles.modalTitle}>Add Units</span>
              <button type="button" className={styles.modalClose} onClick={() => setPickerOpen(false)}>✕</button>
            </div>
            <div className={styles.modalBody}>{pickerContent}</div>
          </div>
        </div>
      )}
    </div>
  );
}
