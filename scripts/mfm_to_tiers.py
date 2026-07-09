#!/usr/bin/env python3
"""Convert a parsed MFM faction JSON into a unit_cost tier patch CSV.

Reads the JSON produced by scripts/parse_mfm.py plus the wahapedia
Datasheets.csv / Datasheets_models_cost.csv, and emits
Datasheets_models_cost_tiers.csv rows for every unit on the page, so the
patch carries the MFM point values (single open-ended tier for normal units,
multiple ordinal ranges for units with 11th-edition escalating costs).

Each output row is: datasheet_id|line|description|cost|min_count|max_count
- line matches the wahapedia size-option line for that model count
- min_count/max_count encode the ordinal range the tier applies to
"""
import csv, json, re, sys, unicodedata

TIER_RANGES = {
    "YOUR UNIT COSTS": (1, ""),
    "YOUR 1ST UNIT COSTS": (1, 1),
    "YOUR 2ND + UNIT COSTS": (2, ""),
    "YOUR 1ST TO 2ND UNITS COST": (1, 2),
    "YOUR 3RD + UNIT COSTS": (3, ""),
}


def norm(s):
    s = unicodedata.normalize("NFKD", s)
    s = s.replace("’", "'").replace("‘", "'")
    return re.sub(r"[^a-z0-9]", "", s.lower())


def norm_models(desc):
    m = re.search(r"(\d+)\s*model", desc, re.I)
    return m.group(1) if m else norm(desc)


def load_datasheet_ids(datasheets_path):
    ids = {}
    with open(datasheets_path, encoding="utf-8-sig") as f:
        reader = csv.reader((l for l in f), delimiter="|")
        header = next(reader)
        for row in reader:
            if len(row) < 3:
                continue
            ids.setdefault(norm(row[1]), row[0])
    return ids


def load_cost_lines(cost_path):
    """datasheet_id -> {normalized model count -> line}"""
    lines = {}
    with open(cost_path, encoding="utf-8-sig") as f:
        reader = csv.reader((l for l in f), delimiter="|")
        next(reader)
        for row in reader:
            if len(row) < 3:
                continue
            ds, line, desc = row[0], row[1], row[2]
            lines.setdefault(ds, {})[norm_models(desc)] = (int(line), desc)
    return lines


def main():
    if len(sys.argv) < 3:
        print("usage: mfm_to_tiers.py <parsed.json> <data_dir> [out.csv]", file=sys.stderr)
        sys.exit(2)
    parsed_path, data_dir = sys.argv[1], sys.argv[2]
    out_path = sys.argv[3] if len(sys.argv) > 3 else None

    data = json.load(open(parsed_path, encoding="utf-8"))
    ids = load_datasheet_ids(f"{data_dir}/Datasheets.csv")
    cost_lines = load_cost_lines(f"{data_dir}/Datasheets_models_cost.csv")

    rows, unmatched, unmatched_size = [], [], []
    for unit in data["units"]:
        ds_id = ids.get(norm(unit["name"]))
        if ds_id is None:
            unmatched.append(unit["name"])
            continue
        size_map = cost_lines.get(ds_id, {})
        for tier in unit["tiers"]:
            min_c, max_c = TIER_RANGES[tier["label"]]
            for cost in tier["costs"]:
                key = norm_models(cost["models"])
                match = size_map.get(key)
                if match is None:
                    unmatched_size.append((unit["name"], cost["models"]))
                    continue
                line, desc = match
                rows.append((ds_id, line, desc, cost["points"], min_c, max_c))

    rows.sort(key=lambda r: (r[0], r[1], r[4]))

    out = open(out_path, "w", newline="", encoding="utf-8") if out_path else sys.stdout
    w = csv.writer(out, delimiter="|", lineterminator="\n")
    w.writerow(["datasheet_id", "line", "description", "cost", "min_count", "max_count"])
    for r in rows:
        w.writerow(list(r) + [""])
    if out_path:
        out.close()

    print(f"wrote {len(rows)} tier rows for "
          f"{len({r[0] for r in rows})} datasheets", file=sys.stderr)
    if unmatched:
        print(f"unmatched units ({len(unmatched)}): {unmatched}", file=sys.stderr)
    if unmatched_size:
        print(f"unmatched sizes ({len(unmatched_size)}): {unmatched_size}", file=sys.stderr)


if __name__ == "__main__":
    main()
