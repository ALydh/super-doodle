import type { ValidationError } from "../types";

function errorMessage(err: ValidationError): string {
  switch (err.errorType) {
    case "PointsExceeded":
      return `Points exceeded: ${err.total}/${err.limit}`;
    case "NoCharacter":
      return "Army must include at least one Character unit";
    case "InvalidWarlord":
      return "Warlord must be a Character unit";
    case "WarlordNotInArmy":
      return "Warlord unit is not in the army";
    case "FactionMismatch":
      return `${err.unitName} does not belong to this faction`;
    case "DuplicateExceeded":
      return `${err.unitName}: ${err.count} copies exceeds max of ${err.maxAllowed}`;
    case "DuplicateEpicHero":
      return `${err.unitName} is an Epic Hero and can only appear once`;
    case "InvalidLeaderAttachment":
      return "Invalid leader attachment";
    case "TooManyEnhancements":
      return `Too many enhancements: ${err.count} (max 3)`;
    case "DuplicateEnhancement":
      return "Same enhancement used more than once";
    case "EnhancementOnNonCharacter":
      return "Enhancement assigned to non-Character unit";
    case "EnhancementDetachmentMismatch":
      return "Enhancement does not match army detachment";
    case "UnitCostNotFound":
      return "Could not determine unit cost";
    case "DatasheetNotFound":
      return "Datasheet not found";
    default:
      return `Validation error: ${err.errorType}`;
  }
}

interface Props {
  errors: ValidationError[];
}

export function ValidationErrors({ errors }: Props) {
  if (errors.length === 0) return null;

  return (
    <div data-testid="validation-errors">
      <strong>Validation Errors:</strong>
      <ul>
        {errors.map((err, i) => (
          <li key={i} data-testid="validation-error">{errorMessage(err)}</li>
        ))}
      </ul>
    </div>
  );
}
