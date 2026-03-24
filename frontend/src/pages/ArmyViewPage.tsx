import { useMemo } from "react";
import { useParams, useNavigate, useMatch } from "react-router-dom";
import type { Stratagem } from "../types";
import { BATTLE_SIZE_POINTS, BattleSize } from "../types";
import { deleteArmy } from "../api";
import { getFactionTheme } from "../factionTheme";
import { getChapterTheme, isSpaceMarines, SM_CHAPTERS, CHAPTER_DETACHMENTS, ALL_CHAPTER_DETACHMENT_IDS } from "../chapters";
import { useAuth } from "../context/useAuth";
import { TabNavigation } from "../components/TabNavigation";
import { StratagemCard } from "../components/StratagemCard";
import { DetachmentCard } from "../components/DetachmentCard";
import { UnitsTab } from "./army-view/UnitsTab";
import { ShoppingTab } from "./army-view/ShoppingTab";
import { ChecklistTab } from "./army-view/ChecklistTab";
import { Spinner } from "../components/Spinner";
import { ErrorMessage } from "../components/ErrorMessage";
import { UnitPicker } from "./UnitPicker";
import { ValidationErrors } from "./ValidationErrors";
import { renderUnitsForMode } from "./renderUnitsForMode";
import { ReferenceDataProvider } from "../context/ReferenceDataContext";
import { PointsDisplay } from "./PointsDisplay";
import { DetachmentAbilitiesSection } from "./DetachmentAbilitiesSection";
import { StrategemsSection } from "./StrategemsSection";
import { useArmyData } from "./army-view/useArmyData";
import { useSessionStorage } from "./army-view/useSessionStorage";
import { useEditMode } from "./army-view/useEditMode";
import { useChecklistNotes } from "./army-view/useChecklistNotes";
import { handleCopy, handleExportJson, handleExportTxt } from "./army-view/exportHandlers";
import styles from "./ArmyViewPage.module.css";
import builderStyles from "./ArmyBuilderPage.module.css";

type TabId = "units" | "stratagems" | "detachment" | "shopping" | "checklist";

const TABS = [
  { id: "units" as const, label: "Units" },
  { id: "stratagems" as const, label: "Stratagems" },
  { id: "detachment" as const, label: "Detachment" },
  { id: "checklist" as const, label: "Checklist" },
];

const TABS_WITH_SHOPPING = [
  ...TABS,
  { id: "shopping" as const, label: "Shopping List" },
];

export function ArmyViewPage() {
  const { armyId } = useParams<{ armyId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const isEditRoute = !!useMatch("/armies/:armyId/edit");

  const armyData = useArmyData(armyId, isEditRoute);
  const {
    battleData, setBattleData,
    stratagems, detachmentAbilities, enhancements, detachments,
    error, inventory,
    datasheets, allCosts, allOptions, keywordsByDatasheet,
    alliedCosts, alliedFactions, leaders, profilesByDatasheet,
    isEditing, setIsEditing,
    roleGroups, totalPoints, maxPoints,
  } = armyData;

  const session = useSessionStorage(armyId);
  const {
    activeTab, setActiveTab,
    searchQuery, setSearchQuery,
    stratagemPhaseFilter, setStratagemPhaseFilter,
    stratagemTurnFilter, setStratagemTurnFilter,
    expandedViewIds, handleToggleViewExpanded,
    expandedEditIndices, handleToggleEditExpanded,
  } = session;

  const edit = useEditMode(armyId, battleData, setBattleData, isEditing, setIsEditing, isEditRoute, navigate);
  const {
    editName, setEditName,
    editBattleSize, setEditBattleSize,
    editDetachmentId, setEditDetachmentId,
    editWarlordId, editChapterId, setEditChapterId,
    editUnits,
    validationErrors,
    pickerOpen, setPickerOpen,
    settingsOpen, setSettingsOpen,
    editDetachmentAbilities,
    enterEdit, handleCancel, handleSave,
    handleAddUnit, handleUpdateUnit, handleRemoveUnit, handleCopyUnit, handleSetWarlord,
  } = edit;

  const { checklistNotes, setChecklistNotes } = useChecklistNotes(armyId, battleData);

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
      list.push({ datasheetId: dsId, name, needed: models, owned, missing: Math.max(0, models - owned) });
    }

    return list.sort((a, b) => {
      if (a.missing > 0 && b.missing === 0) return -1;
      if (a.missing === 0 && b.missing > 0) return 1;
      return a.name.localeCompare(b.name);
    });
  }, [battleData, inventory]);

  const handleDelete = async () => {
    if (!armyId) return;
    if (!window.confirm("Are you sure you want to delete this army? This cannot be undone.")) return;
    await deleteArmy(armyId);
    navigate("/");
  };

  if (error) return <ErrorMessage message={error} />;
  if (!battleData) return <Spinner />;

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
    (s) =>
      (s.detachmentId === battleData.detachmentId || !s.detachmentId) &&
      !s.stratagemType?.startsWith("Challenger") &&
      !s.stratagemType?.startsWith("Boarding Actions")
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
            <button className={styles.exportBtn} onClick={() => handleExportJson(battleData)} aria-label="Export as JSON">Export</button>
            <button className={styles.exportTxtBtn} onClick={() => handleExportTxt(battleData, totalPoints)} aria-label="Export as text">Text</button>
            {user && (
              <button className={styles.copyBtn} onClick={() => handleCopy(battleData, navigate)} aria-label="Copy army">Copy</button>
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
                {renderUnitsForMode(editUnits, editLoadedDatasheets, editWarlordId, handleUpdateUnit, handleRemoveUnit, handleCopyUnit, handleSetWarlord, false, profilesByDatasheet, expandedEditIndices, handleToggleEditExpanded)}
              </div>
            </ReferenceDataProvider>
          </div>
        ) : (
          <UnitsTab
            filteredRoleGroups={filteredRoleGroups}
            battleData={battleData}
            searchQuery={searchQuery}
            onSearchChange={setSearchQuery}
            expandedIds={expandedViewIds}
            onToggleExpanded={handleToggleViewExpanded}
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

      {activeTab === "checklist" && (
        <ChecklistTab
          stratagems={detachmentStratagems}
          detachmentAbilities={detachmentAbilities}
          notes={checklistNotes}
          onNoteChange={(phase, note) => setChecklistNotes((prev) => ({ ...prev, [phase]: note }))}
        />
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
