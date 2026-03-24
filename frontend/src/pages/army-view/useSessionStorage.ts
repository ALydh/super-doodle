import { useEffect, useState } from "react";

type TabId = "units" | "stratagems" | "detachment" | "shopping" | "checklist";

export type { TabId };

export function useSessionStorage(armyId: string | undefined) {
  const [activeTab, setActiveTab] = useState<TabId>(() =>
    (sessionStorage.getItem(`avp:${armyId}:activeTab`) as TabId | null) ?? "units"
  );
  const [searchQuery, setSearchQuery] = useState(() =>
    sessionStorage.getItem(`avp:${armyId}:searchQuery`) ?? ""
  );
  const [stratagemPhaseFilter, setStratagemPhaseFilter] = useState(() =>
    sessionStorage.getItem(`avp:${armyId}:stratPhaseFilter`) ?? "all"
  );
  const [stratagemTurnFilter, setStratagemTurnFilter] = useState(() =>
    sessionStorage.getItem(`avp:${armyId}:stratTurnFilter`) ?? "all"
  );
  const [expandedViewIds, setExpandedViewIds] = useState<Set<string>>(() => {
    try { return new Set(JSON.parse(sessionStorage.getItem(`avp:${armyId}:view`) ?? "[]")); } catch (e) { console.error("Failed to parse session storage (view):", e); return new Set(); }
  });
  const [expandedEditIndices, setExpandedEditIndices] = useState<Set<number>>(() => {
    try { return new Set(JSON.parse(sessionStorage.getItem(`avp:${armyId}:edit`) ?? "[]")); } catch (e) { console.error("Failed to parse session storage (edit):", e); return new Set(); }
  });

  useEffect(() => { sessionStorage.setItem(`avp:${armyId}:activeTab`, activeTab); }, [armyId, activeTab]);
  useEffect(() => { sessionStorage.setItem(`avp:${armyId}:searchQuery`, searchQuery); }, [armyId, searchQuery]);
  useEffect(() => { sessionStorage.setItem(`avp:${armyId}:stratPhaseFilter`, stratagemPhaseFilter); }, [armyId, stratagemPhaseFilter]);
  useEffect(() => { sessionStorage.setItem(`avp:${armyId}:stratTurnFilter`, stratagemTurnFilter); }, [armyId, stratagemTurnFilter]);

  const handleToggleViewExpanded = (id: string) => {
    setExpandedViewIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      sessionStorage.setItem(`avp:${armyId}:view`, JSON.stringify([...next]));
      return next;
    });
  };

  const handleToggleEditExpanded = (index: number) => {
    setExpandedEditIndices((prev) => {
      const next = new Set(prev);
      if (next.has(index)) next.delete(index); else next.add(index);
      sessionStorage.setItem(`avp:${armyId}:edit`, JSON.stringify([...next]));
      return next;
    });
  };

  return {
    activeTab, setActiveTab,
    searchQuery, setSearchQuery,
    stratagemPhaseFilter, setStratagemPhaseFilter,
    stratagemTurnFilter, setStratagemTurnFilter,
    expandedViewIds, handleToggleViewExpanded,
    expandedEditIndices, handleToggleEditExpanded,
  };
}
