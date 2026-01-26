#!/usr/bin/env bash
set -euo pipefail

BASE_URL="https://wahapedia.ru/wh40k10ed"
OUT_DIR="data/wahapedia"

FILES=(
  Factions.csv
  Source.csv
  Last_update.csv
  Datasheets.csv
  Datasheets_models.csv
  Datasheets_models_cost.csv
  Datasheets_wargear.csv
  Datasheets_options.csv
  Datasheets_unit_composition.csv
  Datasheets_keywords.csv
  Datasheets_abilities.csv
  Datasheets_leader.csv
  Datasheets_stratagems.csv
  Datasheets_enhancements.csv
  Datasheets_detachment_abilities.csv
  Stratagems.csv
  Abilities.csv
  Enhancements.csv
  Detachment_abilities.csv
)

mkdir -p "$OUT_DIR"

failed=0
for file in "${FILES[@]}"; do
  printf "Fetching %s ... " "$file"
  if curl -sSf -o "$OUT_DIR/$file" "$BASE_URL/$file"; then
    echo "ok"
  else
    echo "FAILED"
    failed=$((failed + 1))
  fi
done

echo ""
echo "Downloaded $((${#FILES[@]} - failed))/${#FILES[@]} files to $OUT_DIR/"
if [ "$failed" -gt 0 ]; then
  echo "$failed file(s) failed to download."
  exit 1
fi
