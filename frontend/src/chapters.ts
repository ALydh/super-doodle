export interface Chapter {
  id: string;
  name: string;
  keyword: string;
  themeKey: string;
}

export const SM_CHAPTERS: Chapter[] = [
  { id: "ultramarines", name: "Ultramarines", keyword: "Ultramarines", themeKey: "sm-ultramarines" },
  { id: "blood-angels", name: "Blood Angels", keyword: "Blood Angels", themeKey: "sm-blood-angels" },
  { id: "dark-angels", name: "Dark Angels", keyword: "Dark Angels", themeKey: "sm-dark-angels" },
  { id: "space-wolves", name: "Space Wolves", keyword: "Space Wolves", themeKey: "sm-space-wolves" },
  { id: "imperial-fists", name: "Imperial Fists", keyword: "Imperial Fists", themeKey: "sm-imperial-fists" },
  { id: "raven-guard", name: "Raven Guard", keyword: "Raven Guard", themeKey: "sm-raven-guard" },
  { id: "iron-hands", name: "Iron Hands", keyword: "Iron Hands", themeKey: "sm-iron-hands" },
  { id: "salamanders", name: "Salamanders", keyword: "Salamanders", themeKey: "sm-salamanders" },
  { id: "white-scars", name: "White Scars", keyword: "White Scars", themeKey: "sm-white-scars" },
];

export const CHAPTER_DETACHMENTS: Record<string, string[]> = {
  "ultramarines": ["000001120", "000001132"],
  "blood-angels": ["000000758", "000000900", "000000901", "000001004", "000001123"],
  "dark-angels": ["000000834", "000000835", "000000981", "000001059"],
  "space-wolves": ["000000836", "000001008", "000001068", "000001069", "000001070", "000001126"],
  "imperial-fists": ["000001103"],
  "iron-hands": ["000001086", "000001118"],
  "salamanders": ["000001006"],
  "raven-guard": ["000001104"],
  "white-scars": ["000001119"],
};

export const ALL_CHAPTER_DETACHMENT_IDS = new Set(
  Object.values(CHAPTER_DETACHMENTS).flat(),
);

export const CHAPTER_KEYWORDS = new Set(SM_CHAPTERS.map((c) => c.keyword));

export function getChapterTheme(chapterId: string | null): string {
  if (!chapterId) return "space-marines";
  const chapter = SM_CHAPTERS.find((c) => c.id === chapterId);
  return chapter?.themeKey ?? "space-marines";
}

export function isSpaceMarines(factionId: string): boolean {
  return factionId === "SM";
}
