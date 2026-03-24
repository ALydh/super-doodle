import { useEffect, useState, useRef } from "react";
import type {
  ArmyBattleData, ArmyUnit, Army, ValidationError, DetachmentAbility,
} from "../../types";
import { BattleSize } from "../../types";
import {
  updateArmy,
  fetchArmyForBattle,
  fetchDetachmentAbilities,
  validateArmy,
} from "../../api";
import { migrateBattleData } from "./useArmyData";

export function useEditMode(
  armyId: string | undefined,
  battleData: ArmyBattleData | null,
  setBattleData: (data: ArmyBattleData | null) => void,
  isEditing: boolean,
  setIsEditing: (v: boolean) => void,
  isEditRoute: boolean,
  navigate: (path: string) => void,
) {
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

  if (isEditRoute && battleData && !editInitRef.current) {
    editInitRef.current = true;
    setEditName(battleData.name);
    setEditBattleSize(battleData.battleSize as BattleSize);
    setEditDetachmentId(battleData.detachmentId);
    setEditWarlordId(battleData.warlordId);
    setEditChapterId(battleData.chapterId);
    setEditUnits(battleData.units.map(bu => bu.unit));
  }

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

  return {
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
  };
}
