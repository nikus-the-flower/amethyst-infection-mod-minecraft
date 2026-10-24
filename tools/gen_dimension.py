"""The world of the Amethyst: dimension, terrain, biomes, decoration, portal blocks and their texts.

Run after gen_content.py and gen_textures.py (it adds to their output). Part of gen_all.py.
"""
import io
import json
import random
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets/amethystmod"
DATA = RES / "data/amethystmod"
NS = "amethystmod"
COMMON_JAR = Path.home() / ".gradle/caches/fabric-loom/1.21.1/minecraft-common.jar"


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


def n(i):
    return f"{NS}:{i}"


# ---- dimension type --------------------------------------------------------------
write(DATA / "dimension_type/amethyst.json", {
    "ultrawarm": False, "natural": False, "coordinate_scale": 1.0, "has_skylight": True, "has_ceiling": False,
    "ambient_light": 0.12, "fixed_time": 13600, "piglin_safe": False, "bed_works": True,
    "respawn_anchor_works": False, "has_raids": False, "logical_height": 256, "min_y": 0, "height": 256,
    "infiniburn": "#minecraft:infiniburn_end", "effects": "minecraft:overworld",
    "monster_spawn_light_level": 15, "monster_spawn_block_light_limit": 15,
})

# ---- terrain: big floating islands close together ----------------------------------


def shifted(noise, scale):
    """Climate noise; a bigger scale makes smaller biomes (the Amethyst world is small, ~500 blocks across)."""
    return {"type": "minecraft:shifted_noise", "noise": noise, "shift_x": "minecraft:shift_x", "shift_y": 0.0,
            "shift_z": "minecraft:shift_z", "xz_scale": scale, "y_scale": 0.0}


def add(a, b):
    return {"type": "minecraft:add", "argument1": a, "argument2": b}


def mul(a, b):
    return {"type": "minecraft:mul", "argument1": a, "argument2": b}


def ygrad(y0, v0, y1, v1):
    return {"type": "minecraft:y_clamped_gradient", "from_y": y0, "to_y": y1, "from_value": v0, "to_value": v1}


def clamp(x, lo, hi):
    return {"type": "minecraft:clamp", "input": x, "min": lo, "max": hi}


CONT = shifted("minecraft:continentalness", 0.6)
EROSION = shifted("minecraft:erosion", 1.0)
RIDGE = shifted("minecraft:ridge", 0.6)

# Islands as lenses: a 2D mask (continentalness) decides where land is; around y=88 the land is
# thickest; the top is flattened (drops fast above) and the underside tapers slowly (End-like).
vshape = {"type": "minecraft:min", "argument1": ygrad(30, -2.4, 88, 0.0), "argument2": ygrad(88, 0.0, 112, -2.6)}
bias = mul(1.8, add(0.1, CONT))
# the shard wastes (high erosion) break up into small scattered islands
wastes = mul(-1.3, clamp(mul(2.2, add(-0.3, EROSION)), 0.0, 1.0))
# canyons: gorges along the ridge lines, cut from the top down to y~68 (they keep a floor)
canyon = mul(mul(-2.0, clamp(add(1.0, mul(-14.0, {"type": "minecraft:abs", "argument": RIDGE})), 0.0, 1.0)), ygrad(66, 0.0, 80, 1.0))
total = add(mul(0.45, "minecraft:end/base_3d_noise"), add(vshape, add(bias, add(wastes, canyon))))
final_density = {"type": "minecraft:squeeze", "argument": mul(0.64, {"type": "minecraft:interpolated",
                                                                      "argument": {"type": "minecraft:blend_density", "argument": total}})}

with zipfile.ZipFile(COMMON_JAR) as z:
    settings = json.loads(z.read("data/minecraft/worldgen/noise_settings/floating_islands.json"))

BIOMES = ["crystal_grove", "amethyst_canyons", "amethyst_meadows", "clear_amethyst_lakes", "shard_wastes"]


def block(name):
    return {"Name": name}


def rule_block(name):
    return {"type": "minecraft:block", "result_state": block(name)}


def if_biome(biomes, then):
    return {"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": [n(b) for b in biomes]}, "then_run": then}


def depth(offset, add_surface):
    return {"type": "minecraft:stone_depth", "offset": offset, "surface_type": "floor", "add_surface_depth": add_surface,
            "secondary_depth_range": 0}


surface_rule = {"type": "minecraft:sequence", "sequence": [
    {"type": "minecraft:condition", "if_true": depth(0, False), "then_run": {"type": "minecraft:sequence", "sequence": [
        if_biome(["amethyst_canyons"], {"type": "minecraft:sequence", "sequence": [
            {"type": "minecraft:condition", "if_true": {"type": "minecraft:noise_threshold", "noise": "minecraft:surface",
                                                         "min_threshold": 0.0, "max_threshold": 10.0},
             "then_run": rule_block(n("crystallized_stone"))},
            rule_block(n("infected_stone"))]}),
        if_biome(["shard_wastes"], rule_block(n("crystallized_deepslate"))),
        if_biome(["clear_amethyst_lakes"], {"type": "minecraft:sequence", "sequence": [
            {"type": "minecraft:condition", "if_true": {"type": "minecraft:noise_threshold", "noise": "minecraft:surface",
                                                         "min_threshold": -0.2, "max_threshold": 10.0},
             "then_run": rule_block(n("infected_sand"))},
            rule_block(n("infected_grass_block"))]}),
        rule_block(n("infected_grass_block"))]}},
    {"type": "minecraft:condition", "if_true": depth(0, True), "then_run": {"type": "minecraft:sequence", "sequence": [
        if_biome(["amethyst_canyons", "shard_wastes"], rule_block(n("infected_stone"))),
        if_biome(["clear_amethyst_lakes"], rule_block(n("infected_sand"))),
        rule_block(n("infected_dirt"))]}},
    {"type": "minecraft:condition", "if_true": {"type": "minecraft:vertical_gradient", "random_name": n("deep"),
                                                 "true_at_and_below": {"absolute": 40}, "false_at_and_above": {"absolute": 60}},
     "then_run": rule_block(n("infected_deepslate"))},
]}

settings.update({
    "sea_level": 0,
    "aquifers_enabled": False,
    "ore_veins_enabled": False,
    "legacy_random_source": False,
    "default_block": block(n("infected_stone")),
    "default_fluid": {"Name": "minecraft:water", "Properties": {"level": "0"}},
    "noise": {"min_y": 0, "height": 256, "size_horizontal": 2, "size_vertical": 1},
    "surface_rule": surface_rule,
    "spawn_target": [],
})
router = settings["noise_router"]
router.update({
    "final_density": final_density,
    "initial_density_without_jaggedness": 0.0,
    "temperature": shifted("minecraft:temperature", 1.2),
    "vegetation": shifted("minecraft:vegetation", 1.2),
    "continents": CONT,
    "erosion": EROSION,
    "ridges": RIDGE,
    "depth": 0.0,
})
write(DATA / "worldgen/noise_settings/amethyst.json", settings)

# ---- biome placement ---------------------------------------------------------------
FULL = [-1.0, 1.0]
CANYON_BAND = 0.07
ERODED = 0.3


def point(biome, temperature=FULL, humidity=FULL, erosion=FULL, weirdness=FULL):
    return {"biome": n(biome), "parameters": {"temperature": temperature, "humidity": humidity, "continentalness": FULL,
                                              "erosion": erosion, "weirdness": weirdness, "depth": 0.0, "offset": 0.0}}


not_canyon = [[-1.0, -CANYON_BAND], [CANYON_BAND, 1.0]]
points = [point("shard_wastes", erosion=[ERODED, 1.0]),
          point("amethyst_canyons", erosion=[-1.0, ERODED], weirdness=[-CANYON_BAND, CANYON_BAND])]
for w in not_canyon:
    points.append(point("clear_amethyst_lakes", humidity=[0.42, 1.0], erosion=[-1.0, ERODED], weirdness=w))
    points.append(point("crystal_grove", temperature=[0.05, 1.0], humidity=[-1.0, 0.42], erosion=[-1.0, ERODED], weirdness=w))
    points.append(point("amethyst_meadows", temperature=[-1.0, 0.05], humidity=[-1.0, 0.42], erosion=[-1.0, ERODED], weirdness=w))

write(DATA / "dimension/amethyst.json", {
    "type": n("amethyst"),
    "generator": {"type": "minecraft:noise", "settings": n("amethyst"),
                  "biome_source": {"type": "minecraft:multi_noise", "biomes": points}},
})

# ---- decoration -------------------------------------------------------------------


def placed(name, feature, placement):
    write(DATA / "worldgen/placed_feature" / f"{name}.json", {"feature": feature, "placement": placement})


def configured(name, ftype, config):
    write(DATA / "worldgen/configured_feature" / f"{name}.json", {"type": ftype, "config": config})


SURFACE = [{"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"}, {"type": "minecraft:biome"}]


def uniform(lo, hi):
    return {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}


def height(lo, hi):
    return {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": lo},
                                                          "max_inclusive": {"absolute": hi}}}


for f in ("crystal_spire", "crystal_scatter", "floating_shard"):
    configured(f, n(f), {})
configured("brilliant_vein", "minecraft:ore", {"size": 9, "discard_chance_on_air_exposure": 0.0, "targets": [
    {"target": {"predicate_type": "minecraft:block_match", "block": n("infected_stone")}, "state": block(n("brilliant_amethyst"))}]})
configured("crystallized_vein", "minecraft:ore", {"size": 28, "discard_chance_on_air_exposure": 0.0, "targets": [
    {"target": {"predicate_type": "minecraft:block_match", "block": n("infected_stone")}, "state": block(n("crystallized_stone"))},
    {"target": {"predicate_type": "minecraft:block_match", "block": n("infected_deepslate")}, "state": block(n("crystallized_deepslate"))}]})
# our own pool, not minecraft:lake: the vanilla lake looks up biomes around it and could hang world generation
configured("crystal_lake", n("crystal_pool"), {})

placed("grove_spires", n("crystal_spire"), [{"type": "minecraft:count", "count": uniform(1, 3)}] + SURFACE)
placed("sparse_spires", n("crystal_spire"), [{"type": "minecraft:rarity_filter", "chance": 5}] + SURFACE)
placed("crystals_dense", n("crystal_scatter"), [{"type": "minecraft:count", "count": 4}] + SURFACE)
placed("crystals", n("crystal_scatter"), [{"type": "minecraft:count", "count": 2}] + SURFACE)
placed("canyon_wall_crystals", n("crystal_scatter"),
       [{"type": "minecraft:count", "count": 10}, {"type": "minecraft:in_square"}, height(36, 140), {"type": "minecraft:biome"}])
placed("brilliant_veins", n("brilliant_vein"),
       [{"type": "minecraft:count", "count": 6}, {"type": "minecraft:in_square"}, height(20, 150), {"type": "minecraft:biome"}])
placed("crystallized_veins", n("crystallized_vein"),
       [{"type": "minecraft:count", "count": 5}, {"type": "minecraft:in_square"}, height(20, 150), {"type": "minecraft:biome"}])
placed("crystal_lakes", n("crystal_lake"),
       [{"type": "minecraft:rarity_filter", "chance": 2}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}])
placed("floating_shards", n("floating_shard"),
       [{"type": "minecraft:count", "count": 3}, {"type": "minecraft:in_square"}, height(50, 170), {"type": "minecraft:biome"}])
placed("rare_floating_shards", n("floating_shard"),
       [{"type": "minecraft:rarity_filter", "chance": 6}, {"type": "minecraft:in_square"}, height(120, 180), {"type": "minecraft:biome"}])

# ---- biomes -----------------------------------------------------------------------
SPAWN_KEYS = ["monster", "creature", "ambient", "axolotls", "underground_water_creature", "water_creature", "water_ambient", "misc"]


def spawn(mob, weight, lo, hi):
    # a quarter of the weight and smaller groups: the Amethyst world should feel empty and watchful
    return {"type": n(mob), "weight": max(1, weight // 4), "minCount": 1, "maxCount": max(1, min(hi, 2))}


def biome(name, sky, fog, water, grass, monsters, ores, lakes, vegetal, top=()):
    features = [[] for _ in range(11)]
    features[1] = [n(f) for f in lakes]
    features[6] = [n(f) for f in ores]
    features[9] = [n(f) for f in vegetal]
    features[10] = [n(f) for f in top]
    spawners = {k: [] for k in SPAWN_KEYS}
    spawners["monster"] = monsters
    write(DATA / "worldgen/biome" / f"{name}.json", {
        "has_precipitation": False, "temperature": 0.6, "downfall": 0.4, "creature_spawn_probability": 0.02,
        "effects": {
            "sky_color": sky, "fog_color": fog, "water_color": water, "water_fog_color": 0x2A0F4A,
            "grass_color": grass, "foliage_color": grass,
            "particle": {"options": {"type": n("crystal_spark")}, "probability": 0.0045},
            "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0},
            "additions_sound": {"sound": "minecraft:block.amethyst_block.chime", "tick_chance": 0.0111},
        },
        "spawners": spawners, "spawn_costs": {}, "carvers": {"air": []}, "features": features,
    })


common = [spawn("amethyst_stalker", 20, 1, 3)]
biome("crystal_grove", 0x8A4FD8, 0x5B2E8F, 0xB57BFF, 0xA070E0,
      common + [spawn("crystal_weaver", 12, 1, 2), spawn("celestial_drifter", 5, 1, 1)],
      ["brilliant_veins", "crystallized_veins"], [], ["grove_spires", "crystals"], ["rare_floating_shards"])
biome("amethyst_canyons", 0x7A3FC0, 0x4A2270, 0xA060F0, 0x8858C8,
      common + [spawn("void_lurker", 12, 1, 3), spawn("geode_colossus", 4, 1, 1), spawn("shard_walker", 6, 1, 2),
                spawn("crystal_weaver", 6, 1, 2)],
      ["brilliant_veins", "crystallized_veins"], [], ["canyon_wall_crystals", "crystals", "sparse_spires"])
biome("amethyst_meadows", 0x9A60E0, 0x6A3AA0, 0xB57BFF, 0xB080F0,
      common + [spawn("shard_walker", 10, 1, 2), spawn("crystal_weaver", 6, 1, 2)],
      ["brilliant_veins"], [], ["crystals_dense", "sparse_spires"], ["rare_floating_shards"])
biome("clear_amethyst_lakes", 0x8F6AE8, 0x6E4AB8, 0xC9A0FF, 0xA888F0,
      common + [spawn("celestial_drifter", 10, 1, 1), spawn("crystal_weaver", 6, 1, 2)],
      ["brilliant_veins"], ["crystal_lakes"], ["crystals", "sparse_spires"])
biome("shard_wastes", 0x5A2A9A, 0x2E1450, 0x9050E0, 0x7040B0,
      common + [spawn("celestial_drifter", 10, 1, 2), spawn("void_lurker", 10, 1, 2)],
      ["crystallized_veins"], [], ["crystals"], ["floating_shards"])

# ---- portal blocks -----------------------------------------------------------------
for b in ("portal_frame",):
    write(ASSETS / "blockstates" / f"{b}.json", {"variants": {"": {"model": n(f"block/{b}")}}})
    write(ASSETS / "models/block" / f"{b}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": n(f"block/{b}")}})
    write(ASSETS / "models/item" / f"{b}.json", {"parent": n(f"block/{b}")})
write(ASSETS / "blockstates/portal_keystone.json", {"variants": {
    "lit=false": {"model": n("block/portal_keystone")}, "lit=true": {"model": n("block/portal_keystone_lit")}}})
for suffix in ("", "_lit"):
    write(ASSETS / "models/block" / f"portal_keystone{suffix}.json", {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"top": n("block/portal_frame"), "bottom": n("block/portal_frame"), "side": n(f"block/portal_keystone{suffix}")}})
write(ASSETS / "models/item/portal_keystone.json", {"parent": n("block/portal_keystone")})

face = {"uv": [0, 0, 16, 16], "texture": "#portal"}
write(ASSETS / "models/block/amethyst_portal_ns.json", {
    "render_type": "minecraft:translucent", "ambientocclusion": False,
    "textures": {"particle": n("block/amethyst_portal"), "portal": n("block/amethyst_portal")},
    "elements": [{"from": [0, 0, 6], "to": [16, 16, 10], "shade": False,
                  "faces": {"north": face, "south": face}}]})
write(ASSETS / "models/block/amethyst_portal_ew.json", {
    "render_type": "minecraft:translucent", "ambientocclusion": False,
    "textures": {"particle": n("block/amethyst_portal"), "portal": n("block/amethyst_portal")},
    "elements": [{"from": [6, 0, 0], "to": [10, 16, 16], "shade": False,
                  "faces": {"east": face, "west": face}}]})
write(ASSETS / "blockstates/amethyst_portal.json", {"variants": {
    "axis=x": {"model": n("block/amethyst_portal_ns")}, "axis=z": {"model": n("block/amethyst_portal_ew")}}})
for tag in ("dragon_immune", "wither_immune"):
    write(RES / "data/minecraft/tags/block" / f"{tag}.json", {"replace": False, "values": [n("portal_frame"), n("portal_keystone"), n("amethyst_portal")]})
write(ASSETS / "particles/crystal_rain.json", {"textures": [n(f"crystal_rain_{i}") for i in range(4)]})

# ---- textures ---------------------------------------------------------------------
TEX = ASSETS / "textures"
rng = random.Random(42)


def frame_tile(lit=None):
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            v = rng.randint(-8, 8)
            c = (52 + v, 30 + v, 84 + v)
            if x in (0, 15) or y in (0, 15):
                c = (30, 16, 52)
            if (x in (3, 12) and 3 <= y <= 12) or (y in (3, 12) and 3 <= x <= 12):
                c = (190, 120, 255)
            img.putpixel((x, y), c + (255,))
    if lit is not None:
        d = ImageDraw.Draw(img)
        d.rectangle([5, 5, 10, 10], fill=(24, 12, 40, 255))
        if lit:
            d.polygon([(8, 5), (10, 8), (8, 10), (5, 8)], fill=(255, 140, 240, 255))
            img.putpixel((7, 7), (255, 255, 255, 255))
    return img


frame_tile().save(TEX / "block/portal_frame.png")
frame_tile(False).save(TEX / "block/portal_keystone.png")
frame_tile(True).save(TEX / "block/portal_keystone_lit.png")

frames = 16
strip = Image.new("RGBA", (16, 16 * frames))
for f in range(frames):
    for x in range(16):
        for y in range(16):
            import math
            a = math.sin((x * 0.7 + f * 0.4)) + math.cos((y * 0.6 - f * 0.5)) + math.sin((x + y) * 0.35 + f * 0.3)
            t = (a + 3) / 6
            r = int(120 + 120 * t)
            g = int(40 + 60 * t * t)
            b = int(200 + 55 * t)
            strip.putpixel((x, y + f * 16), (min(r, 255), g, min(b, 255), int(150 + 80 * t)))
strip.save(TEX / "block/amethyst_portal.png")
write(TEX / "block/amethyst_portal.png.mcmeta", {"animation": {"frametime": 2, "interpolate": True}})

for i in range(4):
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.polygon([(4, 0), (6, 3), (4, 7 - i), (2, 3)], fill=(200, 140, 255, 230))
    d.line([(4, 0), (4, 6 - i)], fill=(245, 225, 255, 255))
    img.save(TEX / "particle" / f"crystal_rain_{i}.png")

# ---- texts ------------------------------------------------------------------------
TEXTS = {
    "block.amethystmod.portal_frame": ("Amethyst Portal Frame", "Рамка аметистового портала"),
    "block.amethystmod.portal_keystone": ("Portal Keystone", "Замковый камень портала"),
    "block.amethystmod.amethyst_portal": ("Amethyst Portal", "Аметистовый портал"),
    "entity.amethystmod.heart_pylon": ("Heart Crystal", "Кристалл Сердца"),
    "entity.amethystmod.heart_sentinel": ("Heart Sentinel", "Страж Сердца"),
    "entity.amethystmod.heart_node": ("Heart Node", "Узел Сердца"),
    "entity.amethystmod.crystal_spike": ("Crystal Spike", "Кристальный шип"),
    "entity.amethystmod.rune_circle": ("Rune Circle", "Рунный круг"),
    "biome.amethystmod.crystal_grove": ("Crystal Grove", "Кристальная роща"),
    "biome.amethystmod.amethyst_canyons": ("Amethyst Canyons", "Аметистовые каньоны"),
    "biome.amethystmod.amethyst_meadows": ("Amethyst Meadows", "Аметистовые луга"),
    "biome.amethystmod.clear_amethyst_lakes": ("Clear Amethyst Lakes", "Озёра прозрачного аметиста"),
    "biome.amethystmod.shard_wastes": ("Shard Wastes", "Осколочные пустоши"),
    "message.amethystmod.portal.sealed": ("This portal is sealed until the Heart of the Amethyst falls.",
                                          "Этот портал запечатан, пока не падёт Сердце Аметиста."),
    "message.amethystmod.world.arrival": ("You are in the world of the Amethyst. The Heart beats in the temple at its centre (0, 0).",
                                          "Вы в мире Аметиста. Сердце бьётся в храме в его центре (0, 0)."),
    "message.amethystmod.heart.awake": ("The Heart awakens! Five crystals shield it: break them first.",
                                        "Сердце пробуждается! Его защищают пять кристаллов: сначала разбейте их."),
    "message.amethystmod.heart.shielded": ("The crystals around the arena shield the Heart.",
                                           "Кристаллы вокруг арены защищают Сердце."),
    "message.amethystmod.heart.transforming": ("The Heart is changing...", "Сердце меняется..."),
    "message.amethystmod.heart.nodes": ("Its body is too hard: strike the glowing nodes at its roots!",
                                        "Его тело слишком прочное: бейте по светящимся узлам у корней!"),
    "message.amethystmod.heart.phase1": ('The shield shatters! Strike the nodes at its roots while it is open.', 'Щит разбит! Бейте узлы у корней, пока Сердце раскрыто.'),
    "message.amethystmod.heart.phase2": ('Its armour cracks: strike the wounds on its chest and back.', 'Броня треснула: бейте раны на груди и спине.'),
    "message.amethystmod.heart.phase3": ('The armour falls away: strike the veins under its belly.', 'Броня спала: бейте жилы под брюхом.'),
    "message.amethystmod.heart.phase4": ('No defence left: strike it anywhere!', 'Защиты больше нет: бейте Сердце куда угодно!'),
    "message.amethystmod.heart.hint.jump": ("Jump over the beams!", "Перепрыгивайте лучи!"),
    "entity.amethystmod.falling_crystal": ('Falling Crystal', 'Падающий кристалл'),
    "entity.amethystmod.crystal_beam": ('Crystal Beam', 'Кристальный луч'),
    "bar.amethystmod.heart.stage": ('%s: stage %s/5', '%s: этап %s/5'),
    "message.amethystmod.heart.wounds": ('Its body is too hard: strike the glowing wounds on its chest and back!', 'Тело слишком прочное: бейте светящиеся раны на груди и спине!'),
    "message.amethystmod.heart.veins": ('Its body is too hard: strike the crystal veins under its belly!', 'Тело слишком прочное: бейте кристальные жилы под брюхом!'),
    "entity.amethystmod.dimensional_rift": ('Dimensional Rift', 'Пространственный разлом'),
    "message.amethystmod.rift.tower": ('Space tears open in the hall: a rift into the world of the Amethyst!', 'Пространство разрывается: в зале открылся разлом в мир Аметиста!'),
    "message.amethystmod.rift.temple": ('The Heart is gone: a rift home opens in the middle of the arena!', 'Сердца больше нет: посреди арены открылся разлом домой!'),
    "entity.amethystmod.phantom_spear": ("Spear of Light", "Копьё света"),
    "item.amethystmod.purity_aegis.charge": ("Charge: %s%%, white hearts: %s", "Заряд: %s%%, белые сердца: %s"),
    "entity.amethystmod.crescent_slash": ("Crescent of Light", "Полумесяц света"),
    "message.amethystmod.heart.hint.run": ("Crystals chase you: keep running!", "Кристаллы преследуют вас: не останавливайтесь!"),
    "message.amethystmod.heart.hint.sectors": ("Stand in a pale sector!", "Встаньте в бледный сектор!"),
    "message.amethystmod.heart.hint.pull": ("It pulls you in: run away from it!", "Оно тянет к себе: бегите прочь!"),
    "message.amethystmod.heart.hint.flash": ("Turn away from the Heart!", "Отвернитесь от Сердца!"),
    "message.amethystmod.heart.final": ("The Heart is about to burst! Destroy it within 30 seconds!",
                                        "Сердце вот-вот взорвётся! Уничтожьте его за 30 секунд!"),
    "message.amethystmod.heart.defeated": ("The Heart of the Amethyst is destroyed. The infection of the world has stopped, and the way home is open.",
                                           "Сердце Аметиста уничтожено. Заражение в мире остановлено, путь домой открыт."),
    "bar.amethystmod.heart.countdown": ("The Heart bursts in %s s!", "Сердце взорвётся через %s с!"),
    "commands.amethystmod.temple.building": ("The temple rises...", "Храм возводится..."),
    "title.amethystmod.heart": ('The Amethyst Heart', 'Сердце Аметиста'),
    "title.amethystmod.heart.sub": ('Sovereign of the crystal world', 'Владыка кристального мира'),
    "title.amethystmod.heart.phase1": ('Awakening', 'Пробуждение'),
    "title.amethystmod.heart.phase2": ('Resonance', 'Резонанс'),
    "title.amethystmod.heart.phase3": ('Crystallization', 'Кристаллизация'),
    "title.amethystmod.heart.phase4": ('Agony', 'Агония'),
    "title.amethystmod.heart.death": ('The Heart Shatters', 'Сердце раскалывается'),
    "title.amethystmod.heart.death.sub": ('The Amethyst falls silent', 'Аметист умолкает'),
    "title.amethystmod.amitariel": ('Amitariel', 'Амитариэль'),
    "title.amethystmod.amitariel.sub": ('Apostle of the Amethyst', 'Апостол Аметиста'),
    "title.amethystmod.amitariel.phase2": ('True Form', 'Истинный облик'),
    "title.amethystmod.amitariel.death": ('The Apostle Falls', 'Апостол пал'),
    "message.amethystmod.heart.closed": ('The nodes are sealed. Wait until the Heart opens after an attack!', 'Узлы закрыты. Дождитесь, пока Сердце раскроется после атаки!'),
    "message.amethystmod.heart.hint.wave": ('A wave is coming: jump!', 'Идёт волна: прыгайте!'),
}
for lang_file, idx in (("en_us.json", 0), ("ru_ru.json", 1)):
    path = ASSETS / "lang" / lang_file
    lang = json.loads(path.read_text(encoding="utf-8"))
    for key, pair in TEXTS.items():
        lang[key] = pair[idx]
    write(path, lang)
print("dimension written")
