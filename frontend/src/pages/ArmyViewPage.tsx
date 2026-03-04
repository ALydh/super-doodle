import { useEffect, useState, useMemo, useRef } from "react";
import { useParams, useNavigate, useMatch } from "react-router-dom";
import type {
  Stratagem, DetachmentAbility, Enhancement, DetachmentInfo, ArmyBattleData, BattleUnitData,
  Army, ArmyUnit, Datasheet, UnitCost, DatasheetOption, ValidationError, DatasheetKeyword,
  AlliedFactionInfo, DatasheetLeader, ModelProfile,
} from "../types";
import { sortByRoleOrder } from "../constants";
import { BATTLE_SIZE_POINTS, BattleSize } from "../types";
import {
  fetchArmyForBattle,
  deleteArmy,
  createArmy,
  fetchStratagemsByFaction,
  fetchDetachmentAbilities,
  fetchEnhancementsByFaction,
  fetchDetachmentsByFaction,
  fetchInventory,
  fetchDatasheetDetailsByFaction,
  fetchLeadersByFaction,
  fetchAvailableAllies,
  updateArmy,
  validateArmy,
} from "../api";
import { getFactionTheme } from "../factionTheme";
import { getChapterTheme, isSpaceMarines, SM_CHAPTERS, CHAPTER_DETACHMENTS, ALL_CHAPTER_DETACHMENT_IDS } from "../chapters";
import { useAuth } from "../context/useAuth";
import { TabNavigation } from "../components/TabNavigation";
import { StratagemCard } from "../components/StratagemCard";
import { DetachmentCard } from "../components/DetachmentCard";
import { UnitsTab } from "./army-view/UnitsTab";
import { ShoppingTab } from "./army-view/ShoppingTab";
import { Spinner } from "../components/Spinner";
import { ErrorMessage } from "../components/ErrorMessage";
import { UnitPicker } from "./UnitPicker";
import { ValidationErrors } from "./ValidationErrors";
import { renderUnitsForMode } from "./renderUnitsForMode";
import { ReferenceDataProvider } from "../context/ReferenceDataContext";
import { PointsDisplay } from "./PointsDisplay";
import { DetachmentAbilitiesSection } from "./DetachmentAbilitiesSection";
import { StrategemsSection } from "./StrategemsSection";
import styles from "./ArmyViewPage.module.css";
import builderStyles from "./ArmyBuilderPage.module.css";

interface RoleGroup {
  role: string;
  units: BattleUnitData[];
}

function unitsByRole(units: BattleUnitData[], warlordId: string): RoleGroup[] {
  const byRole: Record<string, BattleUnitData[]> = {};
  for (const u of units) {
    const role = u.datasheet.role ?? "Other";
    if (!byRole[role]) byRole[role] = [];
    byRole[role].push(u);
  }

  return sortByRoleOrder(Object.keys(byRole)).map((role) => ({
    role,
    units: byRole[role].sort((a, b) => {
      const aIsWarlord = a.unit.datasheetId === warlordId;
      const bIsWarlord = b.unit.datasheetId === warlordId;
      if (aIsWarlord && !bIsWarlord) return -1;
      if (!aIsWarlord && bIsWarlord) return 1;
      return 0;
    }),
  }));
}

type TabId = "units" | "stratagems" | "detachment" | "shopping";

const TABS = [
  { id: "units" as const, label: "Units" },
  { id: "stratagems" as const, label: "Stratagems" },
  { id: "detachment" as const, label: "Detachment" },
];

const TABS_WITH_SHOPPING = [
  ...TABS,
  { id: "shopping" as const, label: "Shopping List" },
];

function migrateBattleData(data: ArmyBattleData): ArmyBattleData {
  const claimedIndices = new Set<number>();
  data.units = data.units.map(bu => {
    if (!bu.unit.attachedLeaderId || bu.unit.attachedToUnitIndex != null) return bu;
    const bodyguardIndex = data.units.findIndex((other, i) =>
      other.unit.datasheetId === bu.unit.attachedLeaderId && !claimedIndices.has(i)
    );
    if (bodyguardIndex >= 0) claimedIndices.add(bodyguardIndex);
    return { ...bu, unit: { ...bu.unit, attachedToUnitIndex: bodyguardIndex >= 0 ? bodyguardIndex : null } };
  });
  return data;
}

export function ArmyViewPage() {
  const { armyId } = useParams<{ armyId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const isEditRoute = useMatch("/armies/:armyId/edit");

  // View state
  const [battleData, setBattleData] = useState<ArmyBattleData | null>(null);
  const [stratagems, setStratagems] = useState<Stratagem[]>([]);
  const [detachmentAbilities, setDetachmentAbilities] = useState<DetachmentAbility[]>([]);
  const [enhancements, setEnhancements] = useState<Enhancement[]>([]);
  const [detachments, setDetachments] = useState<DetachmentInfo[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<TabId>("units");
  const [searchQuery, setSearchQuery] = useState("");
  const [stratagemPhaseFilter, setStratagemPhaseFilter] = useState("all");
  const [stratagemTurnFilter, setStratagemTurnFilter] = useState("all");
  const [inventory, setInventory] = useState<Map<string, number> | null>(null);

  // Edit data state (loaded alongside view data)
  const [datasheets, setDatasheets] = useState<Datasheet[]>([]);
  const [allCosts, setAllCosts] = useState<UnitCost[]>([]);
  const [allOptions, setAllOptions] = useState<DatasheetOption[]>([]);
  const [keywordsByDatasheet, setKeywordsByDatasheet] = useState<Map<string, DatasheetKeyword[]>>(new Map());
  const [alliedCosts, setAlliedCosts] = useState<UnitCost[]>([]);
  const [alliedFactions, setAlliedFactions] = useState<AlliedFactionInfo[]>([]);
  const [leaders, setLeaders] = useState<DatasheetLeader[]>([]);
  const [profilesByDatasheet, setProfilesByDatasheet] = useState<Map<string, ModelProfile[]>>(new Map());

  // Edit mode state
  const [isEditing, setIsEditing] = useState(!!isEditRoute);
  const [editName, setEditName] = useState("");
  const [editBattleSize, setEditBattleSize] = useState<BattleSize>("StrikeForce");
  const [editDetachmentId, setEditDetachmentId] = useState("");
  const [editWarlordId, setEditWarlordId] = useState("");
  const [editChapterId, setEditChapterId] = useState<string | null>(null);
  const [editUnits, setEditUnits] = useState<ArmyUnit[]>([]);
  const [validationErrors, setValidationErrors] = useState<ValidationError[]>([]);
  const [pickerOpen, setPickerOpen] = useState(false);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [editDetachmentAbilities, setEditDetachmentAbilities] = useState<DetachmentAbility[]>([]);

  const validateTimerRef = useRef<ReturnType<typeof setTimeout>>(undefined);
  const editInitRef = useRef(false);

  useEffect(() => {
    if (!armyId) return;
    let cancelled = false;

    fetchArmyForBattle(armyId)
      .then((data) => {
        if (cancelled) return;
        setBattleData(migrateBattleData(data));
        return Promise.all([
          fetchStratagemsByFaction(data.factionId),
          fetchEnhancementsByFaction(data.factionId),
          fetchDetachmentsByFaction(data.factionId),
          data.detachmentId ? fetchDetachmentAbilities(data.detachmentId) : Promise.resolve([]),
          fetchDatasheetDetailsByFaction(data.factionId),
          fetchLeadersByFaction(data.factionId),
          fetchAvailableAllies(data.factionId),
        ]);
      })
      .then((results) => {
        if (cancelled || !results) return;
        const [strat, enh, det, abilities, details, ldr, allies] = results;
        setStratagems(strat);
        setEnhancements(enh);
        setDetachments(det);
        setDetachmentAbilities(abilities as DetachmentAbility[]);
        setAllCosts(details.flatMap((d) => d.costs));
        setAllOptions(details.flatMap((d) => d.options));
        setDatasheets(details.map((d) => d.datasheet));
        const kwMap = new Map<string, DatasheetKeyword[]>();
        const profMap = new Map<string, ModelProfile[]>();
        for (const d of details) {
          kwMap.set(d.datasheet.id, d.keywords);
          profMap.set(d.datasheet.id, d.profiles);
        }
        setKeywordsByDatasheet(kwMap);
        setProfilesByDatasheet(profMap);
        setLeaders(ldr);
        setAlliedFactions(allies);
        if (allies.length > 0) {
          Promise.all(allies.map((a) => fetchDatasheetDetailsByFaction(a.factionId)))
            .then((alliedDetails) => {
              if (!cancelled) setAlliedCosts(alliedDetails.flatMap((d) => d.flatMap((dd) => dd.costs)));
            });
        }
      })
      .catch((e) => {
        if (!cancelled) setError(e.message);
      });

    return () => { cancelled = true; };
  }, [armyId]);

  useEffect(() => {
    if (isEditing && battleData && !editInitRef.current) {
      editInitRef.current = true;
      setEditName(battleData.name);
      setEditBattleSize(battleData.battleSize as BattleSize);
      setEditDetachmentId(battleData.detachmentId);
      setEditWarlordId(battleData.warlordId);
      setEditChapterId(battleData.chapterId);
      setEditUnits(battleData.units.map(bu => bu.unit));
    }
  }, [isEditing, battleData]);

  useEffect(() => {
    if (!isEditing || !editDetachmentId) return;
    fetchDetachmentAbilities(editDetachmentId).then(setEditDetachmentAbilities);
  }, [isEditing, editDetachmentId]);

  useEffect(() => {
    if (!isEditing || !editDetachmentId || !battleData) return;
    clearTimeout(validateTimerRef.current);
    validateTimerRef.current = setTimeout(() => {
      if (editUnits.length > 0) {
        const army: Army = {
          factionId: battleData.factionId,
          battleSize: editBattleSize,
          detachmentId: editDetachmentId,
          warlordId: editWarlordId || (editUnits[0]?.datasheetId ?? ""),
          units: editUnits,
          chapterId: editChapterId,
        };
        validateArmy(army).then((res) => setValidationErrors(res.errors));
      } else {
        setValidationErrors([]);
      }
    }, 500);
    return () => clearTimeout(validateTimerRef.current);
  }, [isEditing, editUnits, editBattleSize, editDetachmentId, editWarlordId, editChapterId, battleData]);

  useEffect(() => {
    if (!user) return;
    fetchInventory().then((entries) => {
      const map = new Map<string, number>();
      for (const e of entries) {
        map.set(e.datasheetId, e.quantity);
      }
      setInventory(map);
    }).catch(() => {});
  }, [user]);

  const shoppingList = useMemo(() => {
    if (!battleData || !inventory) return [];

    const parseModels = (desc: string): number => {
      const match = desc.match(/(\d+)\s*model/i);
      return match ? parseInt(match[1], 10) : 1;
    };

    const needed = new Map<string, { name: string; models: number }>();
    for (const unit of battleData.units) {
      const dsId = unit.unit.datasheetId;
      const models = unit.cost ? parseModels(unit.cost.description) : 1;
      const existing = needed.get(dsId);
      if (existing) {
        existing.models += models;
      } else {
        needed.set(dsId, { name: unit.datasheet.name, models });
      }
    }

    const list: { datasheetId: string; name: string; needed: number; owned: number; missing: number }[] = [];
    for (const [dsId, { name, models }] of needed) {
      const owned = inventory.get(dsId) ?? 0;
      list.push({
        datasheetId: dsId,
        name,
        needed: models,
        owned,
        missing: Math.max(0, models - owned),
      });
    }

    return list.sort((a, b) => {
      if (a.missing > 0 && b.missing === 0) return -1;
      if (a.missing === 0 && b.missing > 0) return 1;
      return a.name.localeCompare(b.name);
    });
  }, [battleData, inventory]);

  const roleGroups = useMemo(() => {
    if (!battleData) return [];
    return unitsByRole(battleData.units, battleData.warlordId);
  }, [battleData]);

  const filteredRoleGroups = useMemo(() => {
    if (!searchQuery.trim()) return roleGroups;
    const query = searchQuery.toLowerCase();
    return roleGroups
      .map((rg) => ({
        role: rg.role,
        units: rg.units.filter((u) =>
          u.datasheet.name.toLowerCase().includes(query)
        ),
      }))
      .filter((rg) => rg.units.length > 0);
  }, [roleGroups, searchQuery]);

  const totalPoints = useMemo(() => {
    if (!battleData) return 0;
    return battleData.units.reduce((sum, u) => {
      const unitCost = u.cost?.cost ?? 0;
      const enhancementCost = u.enhancement?.cost ?? 0;
      return sum + unitCost + enhancementCost;
    }, 0);
  }, [battleData]);

  const enterEdit = () => {
    if (!battleData) return;
    editInitRef.current = true;
    setEditName(battleData.name);
    setEditBattleSize(battleData.battleSize as BattleSize);
    setEditDetachmentId(battleData.detachmentId);
    setEditWarlordId(battleData.warlordId);
    setEditChapterId(battleData.chapterId);
    setEditUnits(battleData.units.map(bu => bu.unit));
    setIsEditing(true);
  };

  const handleCancel = () => {
    setIsEditing(false);
    editInitRef.current = false;
    if (isEditRoute) navigate(`/armies/${armyId}`);
  };

  const handleSave = async () => {
    if (!armyId || !battleData) return;
    const army: Army = {
      factionId: battleData.factionId,
      battleSize: editBattleSize,
      detachmentId: editDetachmentId,
      warlordId: editWarlordId || (editUnits[0]?.datasheetId ?? ""),
      units: editUnits,
      chapterId: editChapterId,
    };
    await updateArmy(armyId, editName, army);
    const data = await fetchArmyForBattle(armyId);
    setBattleData(migrateBattleData(data));
    setIsEditing(false);
    editInitRef.current = false;
    navigate(`/armies/${armyId}`);
  };

  const handleAddUnit = (datasheetId: string, sizeOptionLine: number, isAllied?: boolean) => {
    setEditUnits(prev => [...prev, { datasheetId, sizeOptionLine, enhancementId: null, attachedLeaderId: null, attachedToUnitIndex: null, wargearSelections: [], isAllied }]);
  };
  const handleUpdateUnit = (index: number, unit: ArmyUnit) => {
    setEditUnits(prev => { const next = [...prev]; next[index] = unit; return next; });
  };
  const handleRemoveUnit = (index: number) => {
    setEditUnits(prev => prev.filter((_, i) => i !== index));
  };
  const handleCopyUnit = (index: number) => {
    setEditUnits(prev => { const u = prev[index]; return [...prev, { ...u, enhancementId: null, attachedLeaderId: null, attachedToUnitIndex: null }]; });
  };
  const handleSetWarlord = (index: number) => {
    setEditWarlordId(editUnits[index].datasheetId);
  };

  const buildViewArmy = (): Army => ({
    factionId: battleData!.factionId,
    battleSize: battleData!.battleSize as BattleSize,
    detachmentId: battleData!.detachmentId,
    warlordId: battleData!.warlordId,
    units: battleData!.units.map((bu) => bu.unit),
    chapterId: battleData!.chapterId,
  });

  const handleCopy = async () => {
    if (!battleData) return;
    const persisted = await createArmy(`${battleData.name} (Copy)`, buildViewArmy());
    navigate(`/armies/${persisted.id}`);
  };

  const handleExport = () => {
    if (!battleData) return;
    const army = buildViewArmy();
    const readableUnits = army.units.map((u, i) => {
      const bu = battleData.units[i];
      const models = bu.cost?.description.match(/(\d+)\s*model/i)?.[1];
      return {
        _name: bu.datasheet.name,
        ...(models ? { _models: parseInt(models, 10) } : {}),
        ...u,
      };
    });
    const payload = JSON.stringify({ name: battleData.name, army: { ...army, units: readableUnits } }, null, 2);
    const blob = new Blob([payload], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${battleData.name}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleExportTxt = () => {
    if (!battleData) return;
    const lines: string[] = [];
    lines.push(battleData.name);
    lines.push(`${battleData.battleSize} — ${totalPoints}pts`);
    lines.push("");

    const attached = new Map<number, BattleUnitData[]>();
    battleData.units.forEach((bu) => {
      if (bu.unit.attachedToUnitIndex != null) {
        const list = attached.get(bu.unit.attachedToUnitIndex) ?? [];
        list.push(bu);
        attached.set(bu.unit.attachedToUnitIndex, list);
      }
    });

    const printed = new Set<number>();
    battleData.units.forEach((bu, i) => {
      if (printed.has(i)) return;
      printed.add(i);
      const models = bu.cost?.description.match(/(\d+)\s*model/i)?.[1];
      const pts = (bu.cost?.cost ?? 0) + (bu.enhancement?.cost ?? 0);
      let line = `${bu.datasheet.name}`;
      if (models) line += ` (${models})`;
      line += ` — ${pts}pts`;
      if (bu.enhancement) line += ` [${bu.enhancement.name}]`;
      lines.push(line);

      const leaders = attached.get(i);
      if (leaders) {
        for (const leader of leaders) {
          printed.add(battleData.units.indexOf(leader));
          const lPts = (leader.cost?.cost ?? 0) + (leader.enhancement?.cost ?? 0);
          let lLine = `  ↳ ${leader.datasheet.name} — ${lPts}pts`;
          if (leader.enhancement) lLine += ` [${leader.enhancement.name}]`;
          lines.push(lLine);
        }
      }
    });

    const blob = new Blob([lines.join("\n")], { type: "text/plain" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${battleData.name}.txt`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleDelete = async () => {
    if (!armyId) return;
    if (!window.confirm("Are you sure you want to delete this army? This cannot be undone.")) {
      return;
    }
    await deleteArmy(armyId);
    navigate("/");
  };

  if (error) return <ErrorMessage message={error} />;
  if (!battleData) return <Spinner />;

  const maxPoints = BATTLE_SIZE_POINTS[battleData.battleSize as BattleSize] ?? 0;
  const baseFactionTheme = getFactionTheme(battleData.factionId);
  const isSM = isSpaceMarines(battleData.factionId);
  const viewChapterTheme = isSM && battleData.chapterId ? getChapterTheme(battleData.chapterId) : null;
  const editChapterTheme = isSM && editChapterId ? getChapterTheme(editChapterId) : null;
  const factionTheme = (isEditing ? editChapterTheme : viewChapterTheme) ?? baseFactionTheme;
  const chapterName = isSM && battleData.chapterId
    ? SM_CHAPTERS.find((c) => c.id === battleData.chapterId)?.name ?? null
    : null;

  const detachmentInfo = detachments.find((d) => d.detachmentId === battleData.detachmentId);
  const detachmentName = detachmentInfo?.name ?? battleData.detachmentId;

  const detachmentStratagems = stratagems.filter(
    (s) => s.detachmentId === battleData.detachmentId || !s.detachmentId
  );

  const stratagemPhases = [...new Set(detachmentStratagems.filter((s) => s.phase).map((s) => s.phase!))].sort();
  const stratagemTurns = [...new Set(detachmentStratagems.filter((s) => s.turn).map((s) => s.turn!))].sort();

  const filteredStratagems = detachmentStratagems
    .filter((s) => {
      if (stratagemPhaseFilter !== "all" && s.phase !== stratagemPhaseFilter) return false;
      if (stratagemTurnFilter !== "all" && s.turn !== stratagemTurnFilter) return false;
      return true;
    })
    .sort((a, b) => (a.detachmentId ? 0 : 1) - (b.detachmentId ? 0 : 1));

  const factionAbilityCards: Stratagem[] = (() => {
    const seen = new Set<string>();
    const result: Stratagem[] = [];
    for (const unit of battleData.units) {
      for (const a of unit.abilities) {
        if (a.abilityType === "Faction" && a.name && !seen.has(a.name)) {
          seen.add(a.name);
          result.push({
            factionId: battleData.factionId,
            name: a.name,
            id: `faction-ability-${a.name}`,
            stratagemType: "Faction Ability",
            cpCost: null,
            legend: null,
            turn: null,
            phase: null,
            detachment: null,
            detachmentId: null,
            description: a.description ?? "",
          });
        }
      }
    }
    return result;
  })();

  const detachmentEnhancements = enhancements.filter(
    (e) => e.detachmentId === battleData.detachmentId
  );

  // Edit mode computed values
  const editAlliedDatasheets = alliedFactions.flatMap((a) => a.datasheets);
  const editLoadedDatasheets = [...datasheets, ...editAlliedDatasheets];
  const editCombinedCosts = [...allCosts, ...alliedCosts];
  const editPointsTotal = editUnits.reduce((sum, u) => {
    const cost = editCombinedCosts.find((c) => c.datasheetId === u.datasheetId && c.line === u.sizeOptionLine);
    const enhCost = u.enhancementId ? enhancements.find((e) => e.id === u.enhancementId)?.cost ?? 0 : 0;
    return sum + (cost?.cost ?? 0) + enhCost;
  }, 0);
  const editMaxPoints = BATTLE_SIZE_POINTS[editBattleSize] ?? 0;
  const editDetachmentName = detachments.find((d) => d.detachmentId === editDetachmentId)?.name ?? "";
  const editChapterDetachmentIds = editChapterId ? new Set(CHAPTER_DETACHMENTS[editChapterId] ?? []) : null;
  const editSortedDetachments = editChapterDetachmentIds
    ? detachments
        .filter((d) => editChapterDetachmentIds.has(d.detachmentId) || !ALL_CHAPTER_DETACHMENT_IDS.has(d.detachmentId))
        .sort((a, b) => (editChapterDetachmentIds.has(a.detachmentId) ? 0 : 1) - (editChapterDetachmentIds.has(b.detachmentId) ? 0 : 1))
    : detachments;
  const editSelectedChapter = isSM ? SM_CHAPTERS.find((c) => c.id === editChapterId) ?? null : null;
  const editFilteredStratagems = stratagems.filter((s) => s.detachmentId === editDetachmentId);

  return (
    <>
    <div data-faction={factionTheme} className={styles.page}>
      {factionTheme && (
        <img
          src={`/icons/${factionTheme}.svg`}
          alt=""
          className={styles.bgIcon}
          aria-hidden="true"
        />
      )}

      {isEditing ? (
        <div className={styles.header}>
          {factionTheme && (
            <img src={`/icons/${factionTheme}.svg`} alt="" className={styles.headerIcon} />
          )}
          <div className={styles.headerText}>
            <input
              className={styles.nameInput}
              type="text"
              value={editName}
              onChange={(e) => setEditName(e.target.value)}
              placeholder="Army name..."
            />
            <p className={styles.meta}>
              {isSM && editChapterId && <>{SM_CHAPTERS.find(c => c.id === editChapterId)?.name} | </>}{editBattleSize} - <span className={editPointsTotal > editMaxPoints ? styles.overBudget : ""}>{editPointsTotal}/{editMaxPoints}pts</span>{editDetachmentName && <> | {editDetachmentName}</>}
            </p>
          </div>
          <div className={styles.actions}>
            <button className={styles.btnSave} onClick={handleSave} disabled={!editName.trim()} aria-label="Save" title="Save" />
            <button className={styles.btnCancel} onClick={handleCancel} aria-label="Cancel" title="Cancel" />
          </div>
        </div>
      ) : (
        <div className={styles.header}>
          {factionTheme && (
            <img
              src={`/icons/${factionTheme}.svg`}
              alt=""
              className={styles.headerIcon}
            />
          )}
          <div className={styles.headerText}>
            <h1 className={styles.armyName}>{battleData.name}</h1>
            <p className={styles.meta}>
              {chapterName && <>{chapterName} | </>}{battleData.battleSize} - {totalPoints}/{maxPoints}pts | {detachmentName}
            </p>
          </div>
          <div className={styles.actions}>
            <button className={styles.exportBtn} onClick={handleExport} aria-label="Export as JSON">Export</button>
            <button className={styles.exportTxtBtn} onClick={handleExportTxt} aria-label="Export as text">Text</button>
            {user && (
              <button className={styles.copyBtn} onClick={handleCopy} aria-label="Copy army">Copy</button>
            )}
            {user && (
              <button className={styles.editBtn} onClick={enterEdit} aria-label="Edit army">Edit</button>
            )}
            {user && (
              <button className={styles.deleteBtn} onClick={handleDelete} aria-label="Delete army">Delete</button>
            )}
          </div>
        </div>
      )}

      <TabNavigation
        tabs={!isEditing && inventory ? TABS_WITH_SHOPPING : TABS}
        activeTab={activeTab}
        onTabChange={(t) => setActiveTab(t as TabId)}
      />

      {isEditing && activeTab === "units" && (
        <div className={styles.modalAddBar}>
          <button type="button" className={styles.addUnitBtn} onClick={() => setSettingsOpen(true)}>
            ⚙ Settings
          </button>
          <button type="button" className={styles.addUnitBtn} onClick={() => setPickerOpen(true)}>
            + Add Units
          </button>
        </div>
      )}

      {activeTab === "units" && (
        isEditing ? (
          <div>
            <ValidationErrors errors={validationErrors} datasheets={editLoadedDatasheets} />
            <ReferenceDataProvider
              costs={editCombinedCosts}
              enhancements={enhancements.filter((e) => !e.detachmentId || e.detachmentId === editDetachmentId)}
              leaders={leaders}
              datasheets={editLoadedDatasheets}
              options={allOptions}
            >
              <div className={styles.grid}>
                {renderUnitsForMode(editUnits, editLoadedDatasheets, editWarlordId, handleUpdateUnit, handleRemoveUnit, handleCopyUnit, handleSetWarlord, false, profilesByDatasheet)}
              </div>
            </ReferenceDataProvider>
          </div>
        ) : (
          <UnitsTab
            filteredRoleGroups={filteredRoleGroups}
            battleData={battleData}
            searchQuery={searchQuery}
            onSearchChange={setSearchQuery}
          />
        )
      )}

      {activeTab === "stratagems" && (
        <div>
          <div className={styles.filters}>
            <label>
              Phase:
              <select
                value={stratagemPhaseFilter}
                onChange={(e) => setStratagemPhaseFilter(e.target.value)}
              >
                <option value="all">All Phases</option>
                {stratagemPhases.map((p) => (
                  <option key={p} value={p}>{p}</option>
                ))}
              </select>
            </label>
            <label>
              Turn:
              <select
                value={stratagemTurnFilter}
                onChange={(e) => setStratagemTurnFilter(e.target.value)}
              >
                <option value="all">All Turns</option>
                {stratagemTurns.map((t) => (
                  <option key={t} value={t}>{t}</option>
                ))}
              </select>
            </label>
          </div>
          <div className={styles.stratagemsList}>
            {factionAbilityCards.map((s) => (
              <StratagemCard key={s.id} stratagem={s} accent />
            ))}
            {filteredStratagems.map((s) => (
              <StratagemCard key={s.id} stratagem={s} />
            ))}
          </div>
        </div>
      )}

      {activeTab === "detachment" && (
        <div>
          <DetachmentCard
            name={detachmentName}
            abilities={detachmentAbilities}
            enhancements={detachmentEnhancements}
          />
        </div>
      )}

      {!isEditing && activeTab === "shopping" && inventory && (
        <ShoppingTab shoppingList={shoppingList} />
      )}

    </div>

      {isEditing && settingsOpen && (
        <div className={builderStyles.modalOverlay} onClick={() => setSettingsOpen(false)}>
          <div className={builderStyles.modal} onClick={(e) => e.stopPropagation()}>
            <div className={builderStyles.modalHeader}>
              <span className={builderStyles.modalTitle}>Settings</span>
              <button type="button" className={builderStyles.modalClose} onClick={() => setSettingsOpen(false)}>✕</button>
            </div>
            <div className={builderStyles.modalBody}>
              <div className={styles.settingsInner}>
                <label>
                  Battle Size
                  <select className={styles.sizeSelect} value={editBattleSize} onChange={(e) => setEditBattleSize(e.target.value as BattleSize)}>
                    <option value="Incursion">Incursion (1000pts)</option>
                    <option value="StrikeForce">Strike Force (2000pts)</option>
                    <option value="Onslaught">Onslaught (3000pts)</option>
                  </select>
                </label>
                {isSM && (
                  <label>
                    Chapter
                    <select value={editChapterId ?? ""} onChange={(e) => setEditChapterId(e.target.value || null)}>
                      <option value="">No Chapter</option>
                      {SM_CHAPTERS.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                    </select>
                  </label>
                )}
                <label>
                  Detachment
                  <select className={styles.detachmentSelect} value={editDetachmentId} onChange={(e) => setEditDetachmentId(e.target.value)}>
                    {editSortedDetachments.map((d) => <option key={d.detachmentId} value={d.detachmentId}>{d.name}</option>)}
                  </select>
                </label>
              </div>
              <PointsDisplay total={editPointsTotal} battleSize={editBattleSize} />
              <DetachmentAbilitiesSection abilities={editDetachmentAbilities} />
              <StrategemsSection stratagems={editFilteredStratagems} />
            </div>
          </div>
        </div>
      )}

      {isEditing && pickerOpen && (
        <div className={builderStyles.modalOverlay} onClick={() => setPickerOpen(false)}>
          <div className={builderStyles.modal} onClick={(e) => e.stopPropagation()}>
            <div className={builderStyles.modalHeader}>
              <span className={builderStyles.modalTitle}>Add Units</span>
              <button type="button" className={builderStyles.modalClose} onClick={() => setPickerOpen(false)}>✕</button>
            </div>
            <div className={builderStyles.modalBody}>
              <UnitPicker
                datasheets={datasheets}
                costs={allCosts}
                onAdd={handleAddUnit}
                alliedFactions={alliedFactions}
                alliedCosts={alliedCosts}
                chapterKeyword={editSelectedChapter?.keyword ?? null}
                keywordsByDatasheet={keywordsByDatasheet}
                inventory={inventory}
              />
            </div>
          </div>
        </div>
      )}
    </>
  );
}
