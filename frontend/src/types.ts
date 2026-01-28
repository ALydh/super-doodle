export interface Faction {
  id: string;
  name: string;
  link: string;
}

export interface Datasheet {
  id: string;
  name: string;
  factionId: string | null;
  sourceId: string | null;
  legend: string | null;
  role: string | null;
  loadout: string | null;
  transport: string | null;
  virtual: boolean;
  leaderHead: string | null;
  leaderFooter: string | null;
  damagedW: string | null;
  damagedDescription: string | null;
  link: string;
}

export interface ModelProfile {
  datasheetId: string;
  line: number;
  name: string | null;
  movement: string;
  toughness: string;
  save: string;
  invulnerableSave: string | null;
  invulnerableSaveDescription: string | null;
  wounds: number;
  leadership: string;
  objectiveControl: number;
  baseSize: string | null;
  baseSizeDescription: string | null;
}

export interface Wargear {
  datasheetId: string;
  line: number | null;
  lineInWargear: number | null;
  dice: string | null;
  name: string | null;
  description: string | null;
  range: string | null;
  weaponType: string | null;
  attacks: string | null;
  ballisticSkill: string | null;
  strength: string | null;
  armorPenetration: string | null;
  damage: string | null;
}

export interface UnitCost {
  datasheetId: string;
  line: number;
  description: string;
  cost: number;
}

export interface DatasheetKeyword {
  datasheetId: string;
  keyword: string | null;
  model: string | null;
  isFactionKeyword: boolean;
}

export interface DatasheetDetail {
  datasheet: Datasheet;
  profiles: ModelProfile[];
  wargear: Wargear[];
  costs: UnitCost[];
  keywords: DatasheetKeyword[];
}

export type BattleSize = "Incursion" | "StrikeForce" | "Onslaught";

export const BATTLE_SIZE_POINTS: Record<BattleSize, number> = {
  Incursion: 1000,
  StrikeForce: 2000,
  Onslaught: 3000,
};

export interface ArmyUnit {
  datasheetId: string;
  sizeOptionLine: number;
  enhancementId: string | null;
  attachedLeaderId: string | null;
}

export interface Army {
  factionId: string;
  battleSize: BattleSize;
  detachmentId: string;
  warlordId: string;
  units: ArmyUnit[];
}

export interface PersistedArmy {
  id: string;
  name: string;
  army: Army;
  createdAt: string;
  updatedAt: string;
}

export interface ArmySummary {
  id: string;
  name: string;
  factionId: string;
  battleSize: string;
  updatedAt: string;
}

export interface Enhancement {
  factionId: string;
  id: string;
  name: string;
  cost: number;
  detachment: string | null;
  detachmentId: string | null;
  legend: string | null;
  description: string;
}

export interface DetachmentInfo {
  name: string;
  detachmentId: string;
}

export interface DatasheetLeader {
  leaderId: string;
  attachedId: string;
}

export interface ValidationError {
  errorType: string;
  [key: string]: unknown;
}

export interface ValidationResponse {
  valid: boolean;
  errors: ValidationError[];
}
