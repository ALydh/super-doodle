import { useEffect, useState, useRef, useCallback, useMemo } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import type {
  ArmyUnit, BattleSize, Datasheet, UnitCost, Enhancement,
  DetachmentInfo, DatasheetLeader, ValidationError, Army, DetachmentAbility, Stratagem, DatasheetOption,
} from "../types";
import { BATTLE_SIZE_POINTS } from "../types";
import {
  fetchDatasheetsByFaction, fetchDetachmentsByFaction,
  fetchEnhancementsByFaction, fetchLeadersByFaction,
  fetchArmy, createArmy, updateArmy, validateArmy, fetchDetachmentAbilities,
  fetchStratagemsByFaction, fetchDatasheetDetailsByFaction,
} from "../api";
import { UnitPicker } from "./UnitPicker";
import { ValidationErrors } from "./ValidationErrors";
import { getFactionTheme } from "../factionTheme";
import { renderUnitsForMode } from "./renderUnitsForMode";

export function ArmyBuilderPage() {
  const { factionId, armyId } = useParams<{ factionId?: string; armyId?: string }>();
  const navigate = useNavigate();
  const { user, loading: authLoading } = useAuth();
  const isEdit = !!armyId;

  const [name, setName] = useState("");
  const [battleSize, setBattleSize] = useState<BattleSize>("StrikeForce");
  const [detachmentId, setDetachmentId] = useState("");
  const [units, setUnits] = useState<ArmyUnit[]>([]);
  const [warlordId, setWarlordId] = useState("");
  const [validationErrors, setValidationErrors] = useState<ValidationError[]>([]);

  const [datasheets, setDatasheets] = useState<Datasheet[] | null>(null);
  const [allCosts, setAllCosts] = useState<UnitCost[]>([]);
  const [detachments, setDetachments] = useState<DetachmentInfo[]>([]);
  const [enhancements, setEnhancements] = useState<Enhancement[]>([]);
  const [leaders, setLeaders] = useState<DatasheetLeader[]>([]);
  const [allOptions, setAllOptions] = useState<DatasheetOption[]>([]);
  const [detachmentAbilities, setDetachmentAbilities] = useState<DetachmentAbility[]>([]);
  const [abilitiesExpanded, setAbilitiesExpanded] = useState(false);
  const [allStratagems, setAllStratagems] = useState<Stratagem[]>([]);
  const [strategemsExpanded, setStrategemsExpanded] = useState(false);
  const [pickerExpanded, setPickerExpanded] = useState(false);
  const [loadedArmyFactionId, setLoadedArmyFactionId] = useState<string>("");

  const validateTimerRef = useRef<ReturnType<typeof setTimeout>>(undefined);
  const detachmentInitializedRef = useRef(false);

  // Derive effective faction ID: for edit mode use loaded army's faction, for create use URL param
  const effectiveFactionId = useMemo(() => {
    if (isEdit) {
      return loadedArmyFactionId;
    }
    return factionId ?? "";
  }, [isEdit, loadedArmyFactionId, factionId]);

  // Derive loading state from data presence
  const loading = effectiveFactionId !== "" && datasheets === null;

  useEffect(() => {
    if (isEdit && armyId) {
      fetchArmy(armyId).then((persisted) => {
        setName(persisted.name);
        setBattleSize(persisted.army.battleSize);
        setDetachmentId(persisted.army.detachmentId);
        detachmentInitializedRef.current = true;
        // Ensure backward compatibility for units without wargearSelections or attachedToUnitIndex
        const unitsWithWargear = persisted.army.units.map(unit => ({
          ...unit,
          wargearSelections: unit.wargearSelections ?? [],
          attachedToUnitIndex: unit.attachedToUnitIndex ?? null,
        }));
        setUnits(unitsWithWargear);
        setWarlordId(persisted.army.warlordId);
        setLoadedArmyFactionId(persisted.army.factionId);
      });
    }
  }, [isEdit, armyId]);

  useEffect(() => {
    if (!effectiveFactionId) return;
    let cancelled = false;

    Promise.all([
      fetchDatasheetsByFaction(effectiveFactionId),
      fetchDetachmentsByFaction(effectiveFactionId),
      fetchEnhancementsByFaction(effectiveFactionId),
      fetchLeadersByFaction(effectiveFactionId),
      fetchStratagemsByFaction(effectiveFactionId),
      fetchDatasheetDetailsByFaction(effectiveFactionId),
    ]).then(([ds, det, enh, ldr, strat, details]) => {
      if (cancelled) return;
      setDetachments(det);
      setEnhancements(enh);
      setLeaders(ldr);
      setAllStratagems(strat);
      if (!detachmentInitializedRef.current && det.length > 0) {
        setDetachmentId(det[0].detachmentId);
        detachmentInitializedRef.current = true;
      }
      const costs = details.flatMap((d) => d.costs);
      const options = details.flatMap((d) => d.options);
      setAllCosts(costs);
      setAllOptions(options);
      setDatasheets(ds);
    });

    return () => { cancelled = true; };
  }, [effectiveFactionId]);

  useEffect(() => {
    if (!detachmentId) return;
    fetchDetachmentAbilities(detachmentId).then(setDetachmentAbilities);
  }, [detachmentId]);

  const buildArmy = useCallback((): Army | null => {
    if (!effectiveFactionId || !detachmentId) return null;
    return {
      factionId: effectiveFactionId,
      battleSize,
      detachmentId,
      warlordId: warlordId || (units[0]?.datasheetId ?? ""),
      units,
    };
  }, [effectiveFactionId, battleSize, detachmentId, warlordId, units]);

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

  const pointsTotal = units.reduce((sum, u) => {
    const cost = allCosts.find((c) => c.datasheetId === u.datasheetId && c.line === u.sizeOptionLine);
    const enhCost = u.enhancementId ? enhancements.find((e) => e.id === u.enhancementId)?.cost ?? 0 : 0;
    return sum + (cost?.cost ?? 0) + enhCost;
  }, 0);

  const handleAddUnit = (datasheetId: string, sizeOptionLine: number) => {
    setUnits([...units, { datasheetId, sizeOptionLine, enhancementId: null, attachedLeaderId: null, attachedToUnitIndex: null, wargearSelections: [] }]);
  };

  const handleUpdateUnit = (index: number, unit: ArmyUnit) => {
    console.log('=== ArmyBuilder handleUpdateUnit called ===', index, 'with:', JSON.stringify(unit, null, 2));
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
    console.log('Saving army with units:', JSON.stringify(army.units, null, 2));
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

  const factionTheme = getFactionTheme(effectiveFactionId);
  const loadedDatasheets = datasheets ?? [];

  const filteredStratagems = allStratagems.filter((s) => s.detachmentId === detachmentId);

  return (
    <div data-faction={factionTheme} className="army-builder-page layout-a">
      {factionTheme && (
        <img
          src={`/icons/${factionTheme}.svg`}
          alt=""
          className="army-builder-bg-icon"
          aria-hidden="true"
        />
      )}

      <div className="builder-header">
        <h1 className="builder-title">{isEdit ? "Edit Army" : "Create Army"}</h1>
      </div>

      <div className="builder-content">
        <div className="builder-col builder-col-info">
          <div className="col-header">Army Info</div>
          <div className="army-settings-section">
            <label>
              Army Name
              <input
                className="army-name-input"
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
            </label>
            <label>
              Battle Size
              <select
                className="battle-size-select"
                value={battleSize}
                onChange={(e) => setBattleSize(e.target.value as BattleSize)}
              >
                <option value="Incursion">Incursion (1000pts)</option>
                <option value="StrikeForce">Strike Force (2000pts)</option>
                <option value="Onslaught">Onslaught (3000pts)</option>
              </select>
            </label>
            <label>
              Detachment
              <select
                className="detachment-select"
                value={detachmentId}
                onChange={(e) => setDetachmentId(e.target.value)}
              >
                {detachments.map((d) => (
                  <option key={d.detachmentId} value={d.detachmentId}>{d.name}</option>
                ))}
              </select>
            </label>
          </div>
          <div
            className={`points-total ${pointsTotal > BATTLE_SIZE_POINTS[battleSize] ? "over-budget" : ""}`}
            style={{ "--points-percent": `${Math.min((pointsTotal / BATTLE_SIZE_POINTS[battleSize]) * 100, 100)}%` } as React.CSSProperties}
          >
            <div className="points-bar" />
            <span className="points-text">{pointsTotal} / {BATTLE_SIZE_POINTS[battleSize]} pts</span>
          </div>
          {detachmentAbilities.length > 0 && (
            <div className="detachment-abilities-section">
              <button
                className="btn-toggle detachment-abilities-toggle"
                onClick={() => setAbilitiesExpanded(!abilitiesExpanded)}
              >
                Abilities ({detachmentAbilities.length}) {abilitiesExpanded ? "▼" : "▶"}
              </button>
              {abilitiesExpanded && (
                <ul className="detachment-abilities-list">
                  {detachmentAbilities.map((a) => (
                    <li key={a.id} className="detachment-ability-item">
                      <strong>{a.name}</strong>
                      <p dangerouslySetInnerHTML={{ __html: a.description }} />
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}
          {filteredStratagems.length > 0 && (
            <div className="detachment-stratagems-section">
              <button
                className="btn-toggle detachment-stratagems-toggle"
                onClick={() => setStrategemsExpanded(!strategemsExpanded)}
              >
                Stratagems ({filteredStratagems.length}) {strategemsExpanded ? "▼" : "▶"}
              </button>
              {strategemsExpanded && (
                <ul className="detachment-stratagems-list">
                  {filteredStratagems.map((s) => (
                    <li key={s.id} className="detachment-stratagem-item">
                      <strong>{s.name}</strong>
                      {s.cpCost !== null && <span> ({s.cpCost} CP)</span>}
                      {s.phase && <span> - {s.phase}</span>}
                      <p dangerouslySetInnerHTML={{ __html: s.description }} />
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}
          <button className="btn-save save-army" onClick={handleSave} disabled={!name.trim()}>
            {isEdit ? "Update Army" : "Save Army"}
          </button>
        </div>

        <div className="builder-col builder-col-units">
          <div className="col-header">Selected Units</div>
          <ValidationErrors errors={validationErrors} datasheets={loadedDatasheets} />
          <div className="army-units-wrapper">
            <table className="army-units-table">
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
                  "grouped",
                  units,
                  loadedDatasheets,
                  allCosts,
                  enhancements.filter((e) => !e.detachmentId || e.detachmentId === detachmentId),
                  leaders,
                  allOptions,
                  warlordId,
                  handleUpdateUnit,
                  handleRemoveUnit,
                  handleCopyUnit,
                  handleSetWarlord
                )}
              </tbody>
            </table>
          </div>
        </div>

        <div className={`builder-col builder-col-picker ${pickerExpanded ? "" : "collapsed"}`}>
          <button className="col-header col-header-toggle" onClick={() => setPickerExpanded(!pickerExpanded)}>
            Add Units {pickerExpanded ? "▼" : "▶"}
          </button>
          {pickerExpanded && (
            <UnitPicker datasheets={loadedDatasheets} costs={allCosts} onAdd={handleAddUnit} />
          )}
        </div>
      </div>
    </div>
  );
}
