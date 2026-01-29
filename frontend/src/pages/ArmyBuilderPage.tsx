import { useEffect, useState, useRef, useCallback, useMemo } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import type {
  ArmyUnit, BattleSize, Datasheet, UnitCost, Enhancement,
  DetachmentInfo, DatasheetLeader, ValidationError, Army, DetachmentAbility, Stratagem, DatasheetOption,
  LeaderDisplayMode,
} from "../types";
import { BATTLE_SIZE_POINTS } from "../types";
import {
  fetchDatasheetsByFaction, fetchDetachmentsByFaction,
  fetchEnhancementsByFaction, fetchLeadersByFaction,
  fetchArmy, createArmy, updateArmy, validateArmy, fetchDetachmentAbilities,
  fetchStratagemsByFaction,
} from "../api";
import { fetchDatasheetDetail } from "../api";
import { UnitPicker } from "./UnitPicker";
import { ValidationErrors } from "./ValidationErrors";
import { getFactionTheme } from "../factionTheme";
import { MergedUnitsDisplay } from "./MergedUnitsDisplay";
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
  const [leaderDisplayMode, setLeaderDisplayMode] = useState<LeaderDisplayMode>("table");

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
    ]).then(([ds, det, enh, ldr, strat]) => {
      if (cancelled) return;
      setDetachments(det);
      setEnhancements(enh);
      setLeaders(ldr);
      setAllStratagems(strat);
      // Set default detachment only if not already initialized
      if (!detachmentInitializedRef.current && det.length > 0) {
        setDetachmentId(det[0].detachmentId);
        detachmentInitializedRef.current = true;
      }
      const dsIds = [...new Set(ds.map((d) => d.id))];
      return Promise.all(dsIds.map((id) => fetchDatasheetDetail(id))).then((details) => {
        if (cancelled) return;
        const costs = details.flatMap((d) => d.costs);
        const options = details.flatMap((d) => d.options);
        setAllCosts(costs);
        setAllOptions(options);
        setDatasheets(ds);
      });
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

  return (
    <div data-faction={factionTheme} className="army-builder-page">
      {factionTheme && (
        <img
          src={`/icons/${factionTheme}.svg`}
          alt=""
          className="army-builder-bg-icon"
          aria-hidden="true"
        />
      )}
      <h1 className="builder-title">{isEdit ? "Edit Army" : "Create Army"}</h1>

      <div>
        <label>
          Army Name:{" "}
          <input
            className="army-name-input"
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        </label>
      </div>

      <div>
        <label>
          Battle Size:{" "}
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
      </div>

      <div>
        <label>
          Detachment:{" "}
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

      {detachmentAbilities.length > 0 && (
        <div className="detachment-abilities-section">
          <button
            className="btn-toggle detachment-abilities-toggle"
            onClick={() => setAbilitiesExpanded(!abilitiesExpanded)}
          >
            Detachment Abilities ({detachmentAbilities.length}) {abilitiesExpanded ? "▼" : "▶"}
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

      {(() => {
        const filteredStratagems = allStratagems.filter((s) => s.detachmentId === detachmentId);
        return filteredStratagems.length > 0 && (
          <div className="detachment-stratagems-section">
            <button
              className="btn-toggle detachment-stratagems-toggle"
              onClick={() => setStrategemsExpanded(!strategemsExpanded)}
            >
              Detachment Stratagems ({filteredStratagems.length}) {strategemsExpanded ? "▼" : "▶"}
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
        );
      })()}

      <div className="points-total">
        Points: {pointsTotal} / {BATTLE_SIZE_POINTS[battleSize]}
      </div>

      <ValidationErrors errors={validationErrors} datasheets={loadedDatasheets} />

      <h2>Units</h2>
      <div className="leader-display-mode-selector">
        <label>Leader Display Mode: </label>
        <select
          value={leaderDisplayMode}
          onChange={(e) => setLeaderDisplayMode(e.target.value as LeaderDisplayMode)}
        >
          <option value="table">Default (Table)</option>
          <option value="grouped">Visual Grouping</option>
          <option value="inline">Inline Display</option>
          <option value="merged">Merged Groups</option>
          <option value="instance">Instance-based</option>
        </select>
      </div>
      <div className="army-units-wrapper">
        {leaderDisplayMode === "merged" ? (
          <MergedUnitsDisplay
            units={units}
            datasheets={loadedDatasheets}
            costs={allCosts}
            enhancements={enhancements.filter((e) => !e.detachmentId || e.detachmentId === detachmentId)}
            leaders={leaders}
            options={allOptions}
            warlordId={warlordId}
            onUpdate={handleUpdateUnit}
            onRemove={handleRemoveUnit}
            onCopy={handleCopyUnit}
            onSetWarlord={handleSetWarlord}
          />
        ) : (
          <table className="army-units-table">
            <thead>
              <tr>
                <th>Unit</th>
                <th>Size</th>
                <th>Enhancement</th>
                <th>{leaderDisplayMode === "inline" ? "Attached Leader" : "Leader"}</th>
                <th>Wargear</th>
                <th>Cost</th>
                <th>Warlord</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {renderUnitsForMode(
                leaderDisplayMode,
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
        )}
      </div>

      <UnitPicker datasheets={loadedDatasheets} costs={allCosts} onAdd={handleAddUnit} />

      <div style={{ marginTop: "16px" }}>
        <button className="btn-save save-army" onClick={handleSave} disabled={!name.trim()}>
          {isEdit ? "Update Army" : "Save Army"}
        </button>
      </div>
    </div>
  );
}
