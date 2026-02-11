import { useEffect, useState, useMemo } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import type { Stratagem, DetachmentAbility, Enhancement, DetachmentInfo, ArmyBattleData, BattleUnitData, Army } from "../types";
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
} from "../api";
import { getFactionTheme } from "../factionTheme";
import { getChapterTheme, isSpaceMarines, SM_CHAPTERS } from "../chapters";
import { useAuth } from "../context/useAuth";
import { TabNavigation } from "../components/TabNavigation";
import { StratagemCard } from "../components/StratagemCard";
import { DetachmentCard } from "../components/DetachmentCard";
import { UnitsTab } from "./army-view/UnitsTab";
import { ShoppingTab } from "./army-view/ShoppingTab";
import styles from "./ArmyViewPage.module.css";

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

export function ArmyViewPage() {
  const { armyId } = useParams<{ armyId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [battleData, setBattleData] = useState<ArmyBattleData | null>(null);
  const [stratagems, setStratagems] = useState<Stratagem[]>([]);
  const [detachmentAbilities, setDetachmentAbilities] = useState<DetachmentAbility[]>([]);
  const [enhancements, setEnhancements] = useState<Enhancement[]>([]);
  const [detachments, setDetachments] = useState<DetachmentInfo[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<TabId>("units");
  const [searchQuery, setSearchQuery] = useState("");
  const [inventory, setInventory] = useState<Map<string, number> | null>(null);

  useEffect(() => {
    if (!armyId) return;
    let cancelled = false;

    fetchArmyForBattle(armyId)
      .then((data) => {
        if (cancelled) return;
        const claimedIndices = new Set<number>();
        data.units = data.units.map(bu => {
          if (!bu.unit.attachedLeaderId || bu.unit.attachedToUnitIndex != null) return bu;
          const bodyguardIndex = data.units.findIndex((other, i) =>
            other.unit.datasheetId === bu.unit.attachedLeaderId && !claimedIndices.has(i)
          );
          if (bodyguardIndex >= 0) claimedIndices.add(bodyguardIndex);
          return { ...bu, unit: { ...bu.unit, attachedToUnitIndex: bodyguardIndex >= 0 ? bodyguardIndex : null } };
        });
        setBattleData(data);
        fetchDatasheetDetailsByFaction(data.factionId);
        fetchLeadersByFaction(data.factionId);
        fetchAvailableAllies(data.factionId);
        return Promise.all([
          fetchStratagemsByFaction(data.factionId),
          fetchEnhancementsByFaction(data.factionId),
          fetchDetachmentsByFaction(data.factionId),
          data.detachmentId ? fetchDetachmentAbilities(data.detachmentId) : Promise.resolve([]),
        ]);
      })
      .then((results) => {
        if (cancelled || !results) return;
        const [strat, enh, det, abilities] = results;
        setStratagems(strat);
        setEnhancements(enh);
        setDetachments(det);
        setDetachmentAbilities(abilities);
      })
      .catch((e) => {
        if (!cancelled) setError(e.message);
      });

    return () => { cancelled = true; };
  }, [armyId]);

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

  const handleCopy = async () => {
    if (!battleData) return;
    const army: Army = {
      factionId: battleData.factionId,
      battleSize: battleData.battleSize as BattleSize,
      detachmentId: battleData.detachmentId,
      warlordId: battleData.warlordId,
      units: battleData.units.map((bu) => bu.unit),
      chapterId: battleData.chapterId,
    };
    const persisted = await createArmy(`${battleData.name} (Copy)`, army);
    navigate(`/armies/${persisted.id}`);
  };

  const handleDelete = async () => {
    if (!armyId) return;
    if (!window.confirm("Are you sure you want to delete this army? This cannot be undone.")) {
      return;
    }
    await deleteArmy(armyId);
    navigate("/");
  };

  if (error) return <div className="error-message">{error}</div>;
  if (!battleData) return <div>Loading...</div>;

  const maxPoints = BATTLE_SIZE_POINTS[battleData.battleSize as BattleSize] ?? 0;
  const baseFactionTheme = getFactionTheme(battleData.factionId);
  const isSM = isSpaceMarines(battleData.factionId);
  const chapterTheme = isSM && battleData.chapterId ? getChapterTheme(battleData.chapterId) : null;
  const factionTheme = chapterTheme ?? baseFactionTheme;
  const chapterName = isSM && battleData.chapterId
    ? SM_CHAPTERS.find((c) => c.id === battleData.chapterId)?.name ?? null
    : null;

  const detachmentInfo = detachments.find((d) => d.detachmentId === battleData.detachmentId);
  const detachmentName = detachmentInfo?.name ?? battleData.detachmentId;

  const detachmentStratagems = stratagems.filter(
    (s) => s.detachmentId === battleData.detachmentId || !s.detachmentId
  );

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

  return (
    <div data-faction={factionTheme} className={styles.page}>
      {factionTheme && (
        <img
          src={`/icons/${baseFactionTheme}.svg`}
          alt=""
          className={styles.bgIcon}
          aria-hidden="true"
        />
      )}
      <div className={styles.header}>
        {factionTheme && (
          <img
            src={`/icons/${baseFactionTheme}.svg`}
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
          {user && (
            <button className={styles.copyBtn} onClick={handleCopy}>Copy</button>
          )}
          <Link to={`/armies/${armyId}/edit`} state={{ factionId: battleData.factionId }}>
            <button className={styles.editBtn}>Edit</button>
          </Link>
          {user && (
            <button className={styles.deleteBtn} onClick={handleDelete}>Delete</button>
          )}
        </div>
      </div>

      <TabNavigation tabs={inventory ? TABS_WITH_SHOPPING : TABS} activeTab={activeTab} onTabChange={(t) => setActiveTab(t as TabId)} />

      {activeTab === "units" && (
        <UnitsTab
          filteredRoleGroups={filteredRoleGroups}
          battleData={battleData}
          searchQuery={searchQuery}
          onSearchChange={setSearchQuery}
        />
      )}

      {activeTab === "stratagems" && (
        <div>
          <div className={styles.stratagemsList}>
            {factionAbilityCards.map((s) => (
              <StratagemCard key={s.id} stratagem={s} accent />
            ))}
            {detachmentStratagems.map((s) => (
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

      {activeTab === "shopping" && inventory && (
        <ShoppingTab shoppingList={shoppingList} />
      )}
    </div>
  );
}
