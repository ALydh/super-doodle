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
  abilities: DatasheetAbility[];
  stratagems: Stratagem[];
  options: DatasheetOption[];
  parsedWargearOptions: ParsedWargearOption[];
}

export type BattleSize = "Incursion" | "StrikeForce" | "Onslaught";

export const BATTLE_SIZE_POINTS: Record<BattleSize, number> = {
  Incursion: 1000,
  StrikeForce: 2000,
  Onslaught: 3000,
};

export interface WargearSelection {
  optionLine: number;
  selected: boolean;
  notes: string | null;
}

export interface ArmyUnit {
  datasheetId: string;
  sizeOptionLine: number;
  enhancementId: string | null;
  attachedLeaderId: string | null;
  attachedToUnitIndex: number | null;
  wargearSelections: WargearSelection[];
}

export type LeaderDisplayMode = "table" | "grouped" | "inline" | "merged" | "instance";

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
  ownerId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface User {
  id: string;
  username: string;
  isAdmin: boolean;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface Invite {
  code: string;
  createdAt: string;
  used: boolean;
}

export interface ArmySummary {
  id: string;
  name: string;
  factionId: string;
  battleSize: string;
  updatedAt: string;
  warlordName: string | null;
  totalPoints: number;
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

export interface Stratagem {
  factionId: string | null;
  name: string;
  id: string;
  stratagemType: string | null;
  cpCost: number | null;
  legend: string | null;
  turn: string | null;
  phase: string | null;
  detachment: string | null;
  detachmentId: string | null;
  description: string;
}

export interface DetachmentAbility {
  id: string;
  factionId: string;
  name: string;
  legend: string | null;
  description: string;
  detachment: string;
  detachmentId: string;
}

export interface DatasheetAbility {
  datasheetId: string;
  line: number;
  abilityId: string | null;
  model: string | null;
  name: string | null;
  description: string | null;
  abilityType: string | null;
  parameter: string | null;
}

export interface DatasheetOption {
  datasheetId: string;
  line: number;
  button: string | null;
  description: string;
}

export interface WeaponAbility {
  id: string;
  name: string;
  description: string;
}

export type WargearAction = "remove" | "add";

export interface ParsedWargearOption {
  datasheetId: string;
  optionLine: number;
  choiceIndex: number;
  groupId: number;
  action: WargearAction;
  weaponName: string;
  modelTarget: string | null;
  countPerNModels: number;
  maxCount: number;
}

export interface WargearWithQuantity {
  wargear: Wargear;
  quantity: number;
  modelType: string | null;
}

export interface BattleUnitData {
  unit: ArmyUnit;
  datasheet: Datasheet;
  profiles: ModelProfile[];
  wargear: WargearWithQuantity[];
  abilities: DatasheetAbility[];
  keywords: DatasheetKeyword[];
  cost: UnitCost | null;
  enhancement: Enhancement | null;
}

export interface ArmyBattleData {
  id: string;
  name: string;
  factionId: string;
  battleSize: string;
  detachmentId: string;
  warlordId: string;
  units: BattleUnitData[];
}
