import type {
  Faction, Datasheet, DatasheetDetail, DetachmentInfo,
  Enhancement, DatasheetLeader, ArmySummary, PersistedArmy,
  Army, ValidationResponse, Stratagem, DetachmentAbility,
  User, AuthResponse, Invite,
} from "./types";

let authToken: string | null = null;

export function setAuthToken(token: string | null): void {
  authToken = token;
}

export function getAuthToken(): string | null {
  return authToken;
}

function authHeaders(): Record<string, string> {
  if (authToken) {
    return { Authorization: `Bearer ${authToken}` };
  }
  return {};
}

export async function register(username: string, password: string, inviteCode?: string): Promise<AuthResponse> {
  const res = await fetch("/api/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password, inviteCode }),
  });
  if (!res.ok) {
    const err = await res.json();
    throw new Error(err.error || `Registration failed: ${res.status}`);
  }
  return res.json();
}

export async function login(username: string, password: string): Promise<AuthResponse> {
  const res = await fetch("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });
  if (!res.ok) {
    const err = await res.json();
    throw new Error(err.error || `Login failed: ${res.status}`);
  }
  return res.json();
}

export async function logout(): Promise<void> {
  await fetch("/api/auth/logout", {
    method: "POST",
    headers: authHeaders(),
  });
}

export async function getCurrentUser(): Promise<User | null> {
  if (!authToken) return null;
  const res = await fetch("/api/auth/me", {
    headers: authHeaders(),
  });
  if (!res.ok) return null;
  return res.json();
}

export async function createInvite(): Promise<Invite> {
  const res = await fetch("/api/invites", {
    method: "POST",
    headers: { ...authHeaders(), "Content-Type": "application/json" },
  });
  if (!res.ok) throw new Error(`Failed to create invite: ${res.status}`);
  return res.json();
}

export async function listInvites(): Promise<Invite[]> {
  const res = await fetch("/api/invites", {
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error(`Failed to list invites: ${res.status}`);
  return res.json();
}

export async function fetchFactions(): Promise<Faction[]> {
  const res = await fetch("/api/factions");
  if (!res.ok) throw new Error(`Failed to fetch factions: ${res.status}`);
  return res.json();
}

export async function fetchDatasheetsByFaction(factionId: string): Promise<Datasheet[]> {
  const res = await fetch(`/api/factions/${factionId}/datasheets`);
  if (!res.ok) throw new Error(`Failed to fetch datasheets: ${res.status}`);
  return res.json();
}

export async function fetchDatasheetDetail(datasheetId: string): Promise<DatasheetDetail> {
  const res = await fetch(`/api/datasheets/${datasheetId}`);
  if (!res.ok) throw new Error(`Failed to fetch datasheet detail: ${res.status}`);
  return res.json();
}

export async function fetchDetachmentsByFaction(factionId: string): Promise<DetachmentInfo[]> {
  const res = await fetch(`/api/factions/${factionId}/detachments`);
  if (!res.ok) throw new Error(`Failed to fetch detachments: ${res.status}`);
  return res.json();
}

export async function fetchEnhancementsByFaction(factionId: string): Promise<Enhancement[]> {
  const res = await fetch(`/api/factions/${factionId}/enhancements`);
  if (!res.ok) throw new Error(`Failed to fetch enhancements: ${res.status}`);
  return res.json();
}

export async function fetchLeadersByFaction(factionId: string): Promise<DatasheetLeader[]> {
  const res = await fetch(`/api/factions/${factionId}/leaders`);
  if (!res.ok) throw new Error(`Failed to fetch leaders: ${res.status}`);
  return res.json();
}

export async function fetchStratagemsByFaction(factionId: string): Promise<Stratagem[]> {
  const res = await fetch(`/api/factions/${factionId}/stratagems`);
  if (!res.ok) throw new Error(`Failed to fetch stratagems: ${res.status}`);
  return res.json();
}

export async function fetchDetachmentAbilities(detachmentId: string): Promise<DetachmentAbility[]> {
  const res = await fetch(`/api/detachments/${detachmentId}/abilities`);
  if (!res.ok) throw new Error(`Failed to fetch detachment abilities: ${res.status}`);
  return res.json();
}

export async function fetchArmiesByFaction(factionId: string): Promise<ArmySummary[]> {
  const res = await fetch(`/api/factions/${factionId}/armies`);
  if (!res.ok) throw new Error(`Failed to fetch armies: ${res.status}`);
  return res.json();
}

export async function fetchArmy(armyId: string): Promise<PersistedArmy> {
  const res = await fetch(`/api/armies/${armyId}`);
  if (!res.ok) throw new Error(`Failed to fetch army: ${res.status}`);
  return res.json();
}

export async function createArmy(name: string, army: Army): Promise<PersistedArmy> {
  const res = await fetch("/api/armies", {
    method: "POST",
    headers: { ...authHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify({ name, army }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.error || `Failed to create army: ${res.status}`);
  }
  return res.json();
}

export async function updateArmy(armyId: string, name: string, army: Army): Promise<PersistedArmy> {
  const res = await fetch(`/api/armies/${armyId}`, {
    method: "PUT",
    headers: { ...authHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify({ name, army }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.error || `Failed to update army: ${res.status}`);
  }
  return res.json();
}

export async function deleteArmy(armyId: string): Promise<void> {
  const res = await fetch(`/api/armies/${armyId}`, {
    method: "DELETE",
    headers: authHeaders(),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.error || `Failed to delete army: ${res.status}`);
  }
}

export async function validateArmy(army: Army): Promise<ValidationResponse> {
  const res = await fetch("/api/armies/validate", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(army),
  });
  if (!res.ok) throw new Error(`Failed to validate army: ${res.status}`);
  return res.json();
}
