"""Sanity-checks that every model, texture, lang key and loot table the mod refers to exists."""
import json
import re
import sys
from pathlib import Path

root = Path(__file__).resolve().parent.parent
res = root / "src/main/resources"
assets = res / "assets/amethystmod"
data = res / "data/amethystmod"
problems = []


def tex_exists(ref):
    ns, _, path = ref.partition(":")
    if ns != "amethystmod":
        return True
    return (assets / "textures" / f"{path}.png").exists()


def model_exists(ref):
    ns, _, path = ref.partition(":")
    if ns != "amethystmod":
        return True
    return (assets / "models" / f"{path}.json").exists()


for f in list((assets / "models").rglob("*.json")):
    m = json.loads(f.read_text(encoding="utf-8"))
    for ref in m.get("textures", {}).values():
        if not ref.startswith("#") and not tex_exists(ref):
            problems.append(f"{f.name}: missing texture {ref}")
    if "parent" in m and not model_exists(m["parent"]):
        problems.append(f"{f.name}: missing parent {m['parent']}")
for f in (assets / "blockstates").glob("*.json"):
    text = f.read_text(encoding="utf-8")
    for ref in set(re.findall(r'"model":\s*"([^"]+)"', text)):
        if not model_exists(ref):
            problems.append(f"blockstate {f.name}: missing model {ref}")

main_src = "\n".join(p.read_text(encoding="utf-8") for p in (root / "src").rglob("*.java"))
registered_items = set(re.findall(r'register\("([a-z_]+)"', main_src))
lang = json.loads((assets / "lang/en_us.json").read_text(encoding="utf-8"))
lang_ru = json.loads((assets / "lang/ru_ru.json").read_text(encoding="utf-8"))
for name in sorted(registered_items):
    keys = [k for p in ("item", "block", "entity", "effect") if (k := f"{p}.amethystmod.{name}") in lang]
    if not keys and name not in ("amethyst", "purified", "blight_spore", "crystal_spark", "prism_flare", "ward_mote", "crystal_rain", "shield_charge", "shield_health", "lore_id", "core_phase"):
        problems.append(f"no english name for {name}")
for k in lang:
    if k not in lang_ru:
        problems.append(f"missing russian: {k}")
for f in (assets / "geo").glob("*.json"):
    for t in (f"textures/entity/{f.stem}.png", f"textures/entity/{f.stem}_glow.png"):
        if not (assets / t).exists():
            problems.append(f"missing {t}")
for f in (assets / "textures/item").glob("*.png"):
    if f.stem.startswith("p_"):
        continue
    if not (assets / "models/item" / f"{f.stem}.json").exists():
        problems.append(f"item texture without model: {f.stem}")
print("\n".join(problems) or "assets OK")
sys.exit(1 if problems else 0)
