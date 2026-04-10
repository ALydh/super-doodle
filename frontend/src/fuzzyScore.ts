/**
 * Returns a fuzzy match score for how well `query` matches `target`.
 * Returns null if query characters cannot all be found in order within target.
 *
 * Scoring:
 *  - Exact substring match scores highest (1000 + start-of-string bonus)
 *  - Otherwise, all query chars must appear in order (subsequence match)
 *  - Consecutive matched characters earn increasing bonuses
 *  - Matches at word boundaries (start or after a space) earn extra points
 */
export function fuzzyScore(target: string, query: string): number | null {
  if (!query) return 0;
  const t = target.toLowerCase();
  const q = query.toLowerCase();

  const exactIdx = t.indexOf(q);
  if (exactIdx !== -1) {
    return 1000 + (exactIdx === 0 ? 50 : 0);
  }

  let score = 0;
  let ti = 0;
  let qi = 0;
  let consecutive = 0;
  let lastTi = -1;

  while (ti < t.length && qi < q.length) {
    if (t[ti] === q[qi]) {
      score += 1;
      if (lastTi === ti - 1) {
        consecutive++;
        score += consecutive;
      } else {
        consecutive = 0;
      }
      if (ti === 0 || t[ti - 1] === " ") {
        score += 5;
      }
      lastTi = ti;
      qi++;
    }
    ti++;
  }

  return qi < q.length ? null : score;
}
