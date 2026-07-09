#!/usr/bin/env python3
"""Parse a saved MFM (Munitorum Field Manual) faction page (.mht) into JSON."""
import email, re, json, html as htmllib, sys
from email import policy

def load_html(path):
    msg = email.message_from_bytes(open(path, "rb").read(), policy=policy.default)
    for p in msg.walk():
        if p.get_content_type() == "text/html":
            return p.get_payload(decode=True).decode("utf-8", "replace")
    raise SystemExit("no html part")

def clean(s):
    return htmllib.unescape(re.sub(r"<[^>]+>", "", s)).strip()

BLOCK_RE = re.compile(
    r'<div class="flex flex-col space-y-1 m-1 print:break-inside-avoid-page">(.*?)</div></div>',
    re.S,
)

def parse(path):
    html = load_html(path)
    fm = re.search(r'font-header text-4xl[^>]*>([^<]+)</div>', html)
    faction = clean(fm.group(1)) if fm else None
    ver = re.search(r'font-semibold">(v[\d.]+)</h2>', html)

    units, detachments = [], []
    for bm in BLOCK_RE.finditer(html):
        block = bm.group(0)
        # detachment?
        det = re.search(r'<span class="text-xl break-all">([^<]+)</span>\s*<span[^>]*>([^<]*DP)</span>', block)
        if det:
            name = clean(det.group(1)); dp = clean(det.group(2))
            mission_m = re.search(r'style="background-color:#[0-9A-Fa-f]+">([^<]+)</div>', block)
            mission = clean(mission_m.group(1)) if mission_m else None
            enh = []
            for e in re.finditer(r'<span>([^<]+?)</span><span>(\d+)\s*pts</span>', block):
                label = clean(e.group(1)); pts = int(e.group(2))
                tag = None
                tm = re.search(r'\((Upgrade|Aura)\)$', label)
                if tm:
                    tag = tm.group(1); label = label[:tm.start()].strip()
                enh.append({"name": label, "points": pts, **({"type": tag} if tag else {})})
            detachments.append({"name": name, "cp": dp, "mission": mission, "enhancements": enh})
            continue
        # unit block: header is a bare div with text-xl (no break-all span)
        hm = re.search(r'bg-slate-500 dark:bg-slate-800 font-bold text-xl text-white">([^<]+)</div>', block)
        if not hm:
            continue
        uname = clean(hm.group(1))
        tiers = []
        for tm in re.finditer(
            r'bg-slate-200 dark:bg-slate-600[^>]*>([^<]+)</div>\s*<ul[^>]*>(.*?)</ul>', block, re.S):
            label = clean(tm.group(1))
            rows = []
            for li in re.finditer(r'<span>([^<]*?)</span><span>(\d+)\s*pts</span>', tm.group(2)):
                rows.append({"models": clean(li.group(1)), "points": int(li.group(2))})
            tiers.append({"label": label, "costs": rows})
        if tiers:
            units.append({"name": uname, "tiers": tiers})
    return {
        "faction": faction,
        "version": clean(ver.group(1)) if ver else None,
        "source": "https://mfm.warhammer-community.com/en/dark-angels",
        "units": units,
        "detachments": detachments,
    }

if __name__ == "__main__":
    data = parse(sys.argv[1])
    out = sys.argv[2] if len(sys.argv) > 2 else None
    js = json.dumps(data, indent=2, ensure_ascii=False)
    if out:
        open(out, "w").write(js)
    print(f"faction={data['faction']} version={data['version']}")
    print(f"units={len(data['units'])} detachments={len(data['detachments'])}")
