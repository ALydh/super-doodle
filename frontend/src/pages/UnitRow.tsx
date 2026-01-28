import type { ArmyUnit, Datasheet, UnitCost, Enhancement, DatasheetLeader } from "../types";

interface Props {
  unit: ArmyUnit;
  index: number;
  datasheet: Datasheet | undefined;
  costs: UnitCost[];
  enhancements: Enhancement[];
  leaders: DatasheetLeader[];
  datasheets: Datasheet[];
  isWarlord: boolean;
  onUpdate: (index: number, unit: ArmyUnit) => void;
  onRemove: (index: number) => void;
  onSetWarlord: (index: number) => void;
}

export function UnitRow({
  unit, index, datasheet, costs, enhancements, leaders, datasheets,
  isWarlord, onUpdate, onRemove, onSetWarlord,
}: Props) {
  const unitCosts = costs.filter((c) => c.datasheetId === unit.datasheetId);
  const isCharacter = datasheet?.role === "Characters";

  const validLeaderTargets = leaders
    .filter((l) => l.leaderId === unit.datasheetId)
    .map((l) => l.attachedId);

  const attachableUnits = datasheets.filter((ds) => validLeaderTargets.includes(ds.id));

  const selectedCost = unitCosts.find((c) => c.line === unit.sizeOptionLine);
  const enhancementCost = unit.enhancementId
    ? enhancements.find((e) => e.id === unit.enhancementId)?.cost ?? 0
    : 0;
  const totalCost = (selectedCost?.cost ?? 0) + enhancementCost;

  return (
    <tr data-testid="unit-row">
      <td data-testid="unit-row-name">{datasheet?.name ?? unit.datasheetId}</td>
      <td>
        <select
          data-testid="unit-size-select"
          value={unit.sizeOptionLine}
          onChange={(e) => onUpdate(index, { ...unit, sizeOptionLine: Number(e.target.value) })}
        >
          {unitCosts.map((c) => (
            <option key={c.line} value={c.line}>{c.description} ({c.cost}pts)</option>
          ))}
        </select>
      </td>
      <td>
        {isCharacter && (
          <select
            data-testid="unit-enhancement-select"
            value={unit.enhancementId ?? ""}
            onChange={(e) => onUpdate(index, { ...unit, enhancementId: e.target.value || null })}
          >
            <option value="">None</option>
            {enhancements.map((e) => (
              <option key={e.id} value={e.id}>{e.name} ({e.cost}pts)</option>
            ))}
          </select>
        )}
      </td>
      <td>
        {isCharacter && attachableUnits.length > 0 && (
          <select
            data-testid="unit-leader-select"
            value={unit.attachedLeaderId ?? ""}
            onChange={(e) => onUpdate(index, { ...unit, attachedLeaderId: e.target.value || null })}
          >
            <option value="">No attachment</option>
            {attachableUnits.map((ds) => (
              <option key={ds.id} value={ds.id}>{ds.name}</option>
            ))}
          </select>
        )}
      </td>
      <td data-testid="unit-cost">{totalCost}pts</td>
      <td>
        {isCharacter && (
          <label>
            <input
              type="radio"
              name="warlord"
              data-testid="warlord-radio"
              checked={isWarlord}
              onChange={() => onSetWarlord(index)}
            />
            Warlord
          </label>
        )}
      </td>
      <td>
        <button data-testid="remove-unit" onClick={() => onRemove(index)}>Remove</button>
      </td>
    </tr>
  );
}
