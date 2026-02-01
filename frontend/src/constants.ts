export const ROLE_ORDER = ["Characters", "Battleline", "Dedicated Transport", "Other"] as const;

export function sortByRoleOrder(roles: string[]): string[] {
  return [...roles].sort((a, b) => {
    const aIndex = ROLE_ORDER.indexOf(a as typeof ROLE_ORDER[number]);
    const bIndex = ROLE_ORDER.indexOf(b as typeof ROLE_ORDER[number]);
    if (aIndex === -1 && bIndex === -1) return a.localeCompare(b);
    if (aIndex === -1) return 1;
    if (bIndex === -1) return -1;
    return aIndex - bIndex;
  });
}
