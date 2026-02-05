import type { ValidationError, Datasheet } from "../types";
import styles from "./ValidationErrors.module.css";

function errorMessage(err: ValidationError, datasheets: Datasheet[]): string {
  const getUnitName = (id: string) => datasheets.find(d => d.id === id)?.name ?? id;

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
    case "InvalidLeaderAttachment": {
      const leaderName = getUnitName(err.leaderId as string);
      const targetName = getUnitName(err.attachedToId as string);
      return `${leaderName} cannot attach to ${targetName}`;
    }
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
  datasheets?: Datasheet[];
}

export function ValidationErrors({ errors, datasheets = [] }: Props) {
  if (errors.length === 0) return null;

  return (
    <div className={styles.errors}>
      <strong>Validation Errors:</strong>
      <ul>
        {errors.map((err, i) => (
          <li key={i} className={styles.error}>{errorMessage(err, datasheets)}</li>
        ))}
      </ul>
    </div>
  );
}
