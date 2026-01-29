/**
 * Maps faction IDs from the API to CSS theme attribute values.
 * Returns undefined if no matching theme exists.
 */
export function getFactionTheme(factionId: string | null | undefined): string | undefined {
  if (!factionId) return undefined;

  const id = factionId.toLowerCase();

  // Direct matches and common variations
  const themeMap: Record<string, string> = {
    // Space Marines
    "sm": "space-marines",
    "space-marines": "space-marines",
    "spacemarines": "space-marines",
    "adeptus-astartes": "space-marines",
    "astartes": "space-marines",

    // Chaos Space Marines
    "csm": "chaos",
    "chaos": "chaos",
    "chaos-space-marines": "chaos",
    "heretic-astartes": "chaos",

    // Astra Militarum
    "am": "guard",
    "guard": "guard",
    "astra-militarum": "guard",
    "imperial-guard": "guard",

    // Adeptus Mechanicus
    "adm": "mechanicus",
    "admech": "mechanicus",
    "mechanicus": "mechanicus",
    "adeptus-mechanicus": "mechanicus",

    // Adepta Sororitas
    "as": "sororitas",
    "sob": "sororitas",
    "sororitas": "sororitas",
    "adepta-sororitas": "sororitas",
    "sisters": "sororitas",
    "sisters-of-battle": "sororitas",

    // Orks
    "ork": "orks",
    "orks": "orks",

    // Aeldari / Craftworlds
    "ae": "aeldari",
    "aeldari": "aeldari",
    "eldar": "aeldari",
    "craftworlds": "aeldari",
    "craftworld": "aeldari",

    // Drukhari
    "dru": "drukhari",
    "drukhari": "drukhari",
    "dark-eldar": "drukhari",
    "darkeldar": "drukhari",

    // Necrons
    "nec": "necrons",
    "necrons": "necrons",
    "necron": "necrons",

    // Tyranids
    "tyr": "tyranids",
    "tyranids": "tyranids",
    "tyranid": "tyranids",
    "nids": "tyranids",

    // T'au Empire
    "tau": "tau",
    "t'au": "tau",
    "tau-empire": "tau",

    // Genestealer Cults
    "gc": "genestealer-cults",
    "gsc": "genestealer-cults",
    "genestealer-cults": "genestealer-cults",
    "genestealers": "genestealer-cults",

    // Leagues of Votann
    "lov": "votann",
    "votann": "votann",
    "leagues-of-votann": "votann",
    "squats": "votann",
  };

  // Check for direct match
  if (themeMap[id]) {
    return themeMap[id];
  }

  // Check if any key is contained in the faction ID
  for (const [key, theme] of Object.entries(themeMap)) {
    if (id.includes(key)) {
      return theme;
    }
  }

  return undefined;
}
