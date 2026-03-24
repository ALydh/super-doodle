import { useEffect, useState, useRef, useMemo } from "react";
import type { ArmyBattleData, Army } from "../../types";
import { BattleSize } from "../../types";
import { updateArmy } from "../../api";

export function useChecklistNotes(
  armyId: string | undefined,
  battleData: ArmyBattleData | null,
) {
  const initialNotes = useMemo(() => battleData?.checklistNotes ?? {}, [battleData]);
  const [checklistNotes, setChecklistNotes] = useState<Record<string, string>>(initialNotes);
  const notesTimerRef = useRef<ReturnType<typeof setTimeout>>(undefined);

  useEffect(() => {
    if (battleData) {
      setChecklistNotes(battleData.checklistNotes ?? {});
    }
  }, [battleData]);

  useEffect(() => {
    if (!armyId || !battleData) return;
    clearTimeout(notesTimerRef.current);
    notesTimerRef.current = setTimeout(() => {
      const army: Army = {
        factionId: battleData.factionId,
        battleSize: battleData.battleSize as BattleSize,
        detachmentId: battleData.detachmentId,
        warlordId: battleData.warlordId,
        units: battleData.units.map((bu) => bu.unit),
        chapterId: battleData.chapterId,
        checklistNotes,
      };
      updateArmy(armyId, battleData.name, army).catch(() => {});
    }, 1000);
    return () => clearTimeout(notesTimerRef.current);
  }, [checklistNotes]); // eslint-disable-line react-hooks/exhaustive-deps

  return { checklistNotes, setChecklistNotes };
}
