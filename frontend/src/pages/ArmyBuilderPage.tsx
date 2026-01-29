import { useEffect, useState, useRef, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import type {
  ArmyUnit, BattleSize, Datasheet, UnitCost, Enhancement,
  DetachmentInfo, DatasheetLeader, ValidationError, Army, DetachmentAbility, Stratagem, DatasheetOption,
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
import { UnitRow } from "./UnitRow";
import { ValidationErrors } from "./ValidationErrors";
import { getFactionTheme } from "../factionTheme";

export function ArmyBuilderPage() {
  const { factionId, armyId } = useParams<{ factionId?: string; armyId?: string }>();
  const navigate = useNavigate();
  const isEdit = !!armyId;

  const [name, setName] = useState("");
  const [battleSize, setBattleSize] = useState<BattleSize>("StrikeForce");
  const [detachmentId, setDetachmentId] = useState("");
  const [units, setUnits] = useState<ArmyUnit[]>([]);
  const [warlordId, setWarlordId] = useState("");
  const [validationErrors, setValidationErrors] = useState<ValidationError[]>([]);

  const [datasheets, setDatasheets] = useState<Datasheet[]>([]);
  const [allCosts, setAllCosts] = useState<UnitCost[]>([]);
  const [detachments, setDetachments] = useState<DetachmentInfo[]>([]);
  const [enhancements, setEnhancements] = useState<Enhancement[]>([]);
  const [leaders, setLeaders] = useState<DatasheetLeader[]>([]);
  const [allOptions, setAllOptions] = useState<DatasheetOption[]>([]);
  const [detachmentAbilities, setDetachmentAbilities] = useState<DetachmentAbility[]>([]);
  const [abilitiesExpanded, setAbilitiesExpanded] = useState(false);
  const [allStratagems, setAllStratagems] = useState<Stratagem[]>([]);
  const [strategemsExpanded, setStrategemsExpanded] = useState(false);
  const [loading, setLoading] = useState(true);
  const [resolvedFactionId, setResolvedFactionId] = useState<string>("");

  const validateTimerRef = useRef<ReturnType<typeof setTimeout>>(undefined);

  useEffect(() => {
    if (isEdit && armyId) {
      fetchArmy(armyId).then((persisted) => {
        setName(persisted.name);
        setBattleSize(persisted.army.battleSize);
        setDetachmentId(persisted.army.detachmentId);
        // Ensure backward compatibility for units without wargearSelections
        const unitsWithWargear = persisted.army.units.map(unit => ({
          ...unit,
          wargearSelections: unit.wargearSelections ?? []
        }));
        setUnits(unitsWithWargear);
        setWarlordId(persisted.army.warlordId);
        setResolvedFactionId(persisted.army.factionId);
      });
    } else if (factionId) {
      setResolvedFactionId(factionId);
    }
  }, [isEdit, armyId, factionId]);

  useEffect(() => {
    if (!resolvedFactionId) return;
    setLoading(true);
    Promise.all([
      fetchDatasheetsByFaction(resolvedFactionId),
      fetchDetachmentsByFaction(resolvedFactionId),
      fetchEnhancementsByFaction(resolvedFactionId),
      fetchLeadersByFaction(resolvedFactionId),
      fetchStratagemsByFaction(resolvedFactionId),
    ]).then(([ds, det, enh, ldr, strat]) => {
      setDatasheets(ds);
      setDetachments(det);
      setEnhancements(enh);
      setLeaders(ldr);
      setAllStratagems(strat);
      if (!detachmentId && det.length > 0) {
        setDetachmentId(det[0].detachmentId);
      }
      const dsIds = [...new Set(ds.map((d) => d.id))];
      return Promise.all(dsIds.map((id) => fetchDatasheetDetail(id)));
    }).then((details) => {
      const costs = details.flatMap((d) => d.costs);
      const options = details.flatMap((d) => d.options);
      setAllCosts(costs);
      setAllOptions(options);
      setLoading(false);
    });
  }, [resolvedFactionId]);

  useEffect(() => {
    if (!detachmentId) return;
    fetchDetachmentAbilities(detachmentId).then(setDetachmentAbilities);
  }, [detachmentId]);

  const buildArmy = useCallback((): Army | null => {
    if (!resolvedFactionId || !detachmentId) return null;
    return {
      factionId: resolvedFactionId,
      battleSize,
      detachmentId,
      warlordId: warlordId || (units[0]?.datasheetId ?? ""),
      units,
    };
  }, [resolvedFactionId, battleSize, detachmentId, warlordId, units]);

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
    setUnits([...units, { datasheetId, sizeOptionLine, enhancementId: null, attachedLeaderId: null, wargearSelections: [] }]);
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

  if (loading) return <div>Loading...</div>;

  const factionTheme = getFactionTheme(resolvedFactionId);

  return (
    <div data-faction={factionTheme}>
      <h1 data-testid="builder-title">{isEdit ? "Edit Army" : "Create Army"}</h1>

      <div>
        <label>
          Army Name:{" "}
          <input
            data-testid="army-name-input"
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
            data-testid="battle-size-select"
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
            data-testid="detachment-select"
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
        <div data-testid="detachment-abilities-section">
          <button
            data-testid="detachment-abilities-toggle"
            onClick={() => setAbilitiesExpanded(!abilitiesExpanded)}
          >
            Detachment Abilities ({detachmentAbilities.length}) {abilitiesExpanded ? "▼" : "▶"}
          </button>
          {abilitiesExpanded && (
            <ul data-testid="detachment-abilities-list">
              {detachmentAbilities.map((a) => (
                <li key={a.id} data-testid="detachment-ability-item">
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
          <div data-testid="detachment-stratagems-section">
            <button
              data-testid="detachment-stratagems-toggle"
              onClick={() => setStrategemsExpanded(!strategemsExpanded)}
            >
              Detachment Stratagems ({filteredStratagems.length}) {strategemsExpanded ? "▼" : "▶"}
            </button>
            {strategemsExpanded && (
              <ul data-testid="detachment-stratagems-list">
                {filteredStratagems.map((s) => (
                  <li key={s.id} data-testid="detachment-stratagem-item">
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

      <div data-testid="points-total">
        Points: {pointsTotal} / {BATTLE_SIZE_POINTS[battleSize]}
      </div>

      <ValidationErrors errors={validationErrors} />

      <h2>Units</h2>
      <table data-testid="army-units-table">
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
          {units.map((unit, i) => (
            <UnitRow
              key={i}
              unit={unit}
              index={i}
              datasheet={datasheets.find((ds) => ds.id === unit.datasheetId)}
              costs={allCosts}
              enhancements={enhancements.filter(
                (e) => !e.detachmentId || e.detachmentId === detachmentId
              )}
              leaders={leaders}
              datasheets={datasheets}
              options={allOptions}
              isWarlord={warlordId === unit.datasheetId}
              onUpdate={handleUpdateUnit}
              onRemove={handleRemoveUnit}
              onSetWarlord={handleSetWarlord}
            />
          ))}
        </tbody>
      </table>

      <UnitPicker datasheets={datasheets} costs={allCosts} onAdd={handleAddUnit} />

      <div style={{ marginTop: "16px" }}>
        <button data-testid="save-army" onClick={handleSave} disabled={!name.trim()}>
          {isEdit ? "Update Army" : "Save Army"}
        </button>
      </div>
    </div>
  );
}
