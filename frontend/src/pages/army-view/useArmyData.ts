import { useEffect, useState, useMemo } from "react";
import type {
  Stratagem, DetachmentAbility, Enhancement, DetachmentInfo, ArmyBattleData, BattleUnitData,
  DatasheetKeyword, AlliedFactionInfo, DatasheetLeader, ModelProfile, Datasheet, UnitCost, DatasheetOption,
} from "../../types";
import { BATTLE_SIZE_POINTS, BattleSize } from "../../types";
import {
  fetchArmyForBattle,
  fetchStratagemsByFaction,
  fetchDetachmentAbilities,
  fetchEnhancementsByFaction,
  fetchDetachmentsByFaction,
  fetchInventory,
  fetchDatasheetDetailsByFaction,
  fetchLeadersByFaction,
  fetchAvailableAllies,
} from "../../api";
import { useAuth } from "../../context/useAuth";
import { sortByRoleOrder } from "../../constants";

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

export { migrateBattleData };
export type { RoleGroup };

export function useArmyData(armyId: string | undefined, isEditRoute: boolean) {
  const { user } = useAuth();

  const [battleData, setBattleData] = useState<ArmyBattleData | null>(null);
  const [stratagems, setStratagems] = useState<Stratagem[]>([]);
  const [detachmentAbilities, setDetachmentAbilities] = useState<DetachmentAbility[]>([]);
  const [enhancements, setEnhancements] = useState<Enhancement[]>([]);
  const [detachments, setDetachments] = useState<DetachmentInfo[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [inventory, setInventory] = useState<Map<string, number> | null>(null);

  // Edit data state (lazy-loaded when entering edit mode)
  const [datasheets, setDatasheets] = useState<Datasheet[]>([]);
  const [allCosts, setAllCosts] = useState<UnitCost[]>([]);
  const [allOptions, setAllOptions] = useState<DatasheetOption[]>([]);
  const [keywordsByDatasheet, setKeywordsByDatasheet] = useState<Map<string, DatasheetKeyword[]>>(new Map());
  const [alliedCosts, setAlliedCosts] = useState<UnitCost[]>([]);
  const [alliedFactions, setAlliedFactions] = useState<AlliedFactionInfo[]>([]);
  const [leaders, setLeaders] = useState<DatasheetLeader[]>([]);
  const [profilesByDatasheet, setProfilesByDatasheet] = useState<Map<string, ModelProfile[]>>(new Map());

  const [isEditing, setIsEditing] = useState(!!isEditRoute);

  useEffect(() => {
    if (!armyId) return;
    let cancelled = false;

    fetchArmyForBattle(armyId)
      .then((data) => {
        if (cancelled) return;
        const migratedData = migrateBattleData(data);
        setBattleData(migratedData);
        return Promise.allSettled([
          fetchStratagemsByFaction(data.factionId),
          fetchEnhancementsByFaction(data.factionId),
          fetchDetachmentsByFaction(data.factionId),
          data.detachmentId ? fetchDetachmentAbilities(data.detachmentId) : Promise.resolve([]),
        ]);
      })
      .then((results) => {
        if (cancelled || !results) return;
        const extract = <T,>(r: PromiseSettledResult<T>, fallback: T): T => {
          if (r.status === "fulfilled") return r.value;
          console.error("Failed to load data:", r.reason);
          return fallback;
        };
        setStratagems(extract(results[0], []));
        setEnhancements(extract(results[1], []));
        setDetachments(extract(results[2], []));
        setDetachmentAbilities(extract(results[3], []) as DetachmentAbility[]);
      })
      .catch((e) => {
        if (!cancelled) setError(e.message);
      });

    return () => { cancelled = true; };
  }, [armyId, isEditRoute]);

  useEffect(() => {
    if (!isEditing || !battleData) return;
    let cancelled = false;

    Promise.all([
      fetchDatasheetDetailsByFaction(battleData.factionId),
      fetchLeadersByFaction(battleData.factionId),
      fetchAvailableAllies(battleData.factionId),
    ]).then(([details, ldr, allies]) => {
      if (cancelled) return;
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
    });

    return () => { cancelled = true; };
  }, [isEditing, battleData?.factionId]);

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

  const roleGroups = useMemo(() => {
    if (!battleData) return [];
    return unitsByRole(battleData.units, battleData.warlordId);
  }, [battleData]);

  const totalPoints = useMemo(() => {
    if (!battleData) return 0;
    return battleData.units.reduce((sum, u) => {
      const unitCost = u.cost?.cost ?? 0;
      const enhancementCost = u.enhancement?.cost ?? 0;
      return sum + unitCost + enhancementCost;
    }, 0);
  }, [battleData]);

  const maxPoints = battleData ? (BATTLE_SIZE_POINTS[battleData.battleSize as BattleSize] ?? 0) : 0;

  return {
    battleData, setBattleData,
    stratagems, detachmentAbilities, enhancements, detachments,
    error, inventory,
    datasheets, allCosts, allOptions, keywordsByDatasheet,
    alliedCosts, alliedFactions, leaders, profilesByDatasheet,
    isEditing, setIsEditing,
    roleGroups, totalPoints, maxPoints,
  };
}
