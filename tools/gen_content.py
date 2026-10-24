"""Generates blockstates, models, lang, loot, tags, recipes and worldgen JSON.

Run before gen_textures.py. Wipes and rewrites blockstates/, models/, lang/ and data/.
Keep the block lists in sync with ModBlocks.java.
"""
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets/amethystmod"
DATA = RES / "data"
NS = "amethystmod"

for d in (ASSETS / "blockstates", ASSETS / "models", ASSETS / "lang", DATA):
    shutil.rmtree(d, ignore_errors=True)


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


COLORS = {  # id -> (English, Russian)
    "violet": ("Violet", "Фиолетовый"),
    "rose": ("Rose", "Розовый"),
    "azure": ("Azure", "Лазурный"),
}

lang_en = {
    "itemGroup.amethystmod.amethyst": "Amethyst Infection",
    "item.amethystmod.amethyst_essence": "Amethyst Essence",
    "gamerule.amethystInfectionSpeed": "Amethyst infection speed (%)",
    "gamerule.amethystInfectionSpeed.description": "How fast the amethyst infection spreads and crystals grow. 0 stops it, 100 is normal, up to 1000 (cores also age that much faster)",
}
lang_ru = {
    "itemGroup.amethystmod.amethyst": "Аметистовое заражение",
    "item.amethystmod.amethyst_essence": "Аметистовая эссенция",
    "gamerule.amethystInfectionSpeed": "Скорость аметистового заражения (%)",
    "gamerule.amethystInfectionSpeed.description": "Как быстро распространяется заражение и растут кристаллы. 0 останавливает, 100 обычная, до 1000 (ядра и стареют во столько же раз быстрее)",
}


def name(block_id, en, ru):
    lang_en[f"block.{NS}.{block_id}"] = en
    lang_ru[f"block.{NS}.{block_id}"] = ru


def loot_self(block_id):
    write(DATA / NS / "loot_table/blocks" / f"{block_id}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NS}:{block_id}"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def loot_essence(block_id, lo, hi):
    write(DATA / NS / "loot_table/blocks" / f"{block_id}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{
            "type": "minecraft:item", "name": f"{NS}:amethyst_essence",
            "functions": [{"function": "minecraft:set_count",
                           "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def simple_state(block_id):
    write(ASSETS / "blockstates" / f"{block_id}.json", {"variants": {"": {"model": f"{NS}:block/{block_id}"}}})


def item_block_model(block_id):
    write(ASSETS / "models/item" / f"{block_id}.json", {"parent": f"{NS}:block/{block_id}"})


def item_flat_model(block_id, texture):
    write(ASSETS / "models/item" / f"{block_id}.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": texture}})


pickaxe, shovel = [], []

# ---- plain items -----------------------------------------------------------
item_flat_model("amethyst_essence", f"{NS}:item/amethyst_essence")

# ---- cube blocks -----------------------------------------------------------
cubes = [
    ("infected_stone", "Infected Stone", "Заражённый камень", pickaxe),
    ("infected_deepslate", "Infected Deepslate", "Заражённый глубинный сланец", pickaxe),
    ("infected_dirt", "Infected Dirt", "Заражённая земля", shovel),
    ("infected_sand", "Infected Sand", "Заражённый песок", shovel),
    ("crystallized_stone", "Crystallized Stone", "Кристаллизованный камень", pickaxe),
    ("crystallized_deepslate", "Crystallized Deepslate", "Кристаллизованный глубинный сланец", pickaxe),
    ("amethyst_crystal_block", "Amethyst Crystal Block", "Блок аметистового кристалла", pickaxe),
    ("brilliant_amethyst", "Brilliant Amethyst", "Сияющий аметист", pickaxe),
    ("amethyst_bricks", "Amethyst Bricks", "Аметистовые кирпичи", pickaxe),
]
for bid, en, ru, tag in cubes:
    name(bid, en, ru)
    simple_state(bid)
    write(ASSETS / "models/block" / f"{bid}.json",
          {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{bid}"}})
    item_block_model(bid)
    loot_self(bid)
    tag.append(f"{NS}:{bid}")

# ---- amethyst brick slab -------------------------------------------------
name("amethyst_brick_slab", "Amethyst Brick Slab", "Аметистовая плита из кирпичей")
for variant, parent in (("bottom", "slab"), ("top", "slab_top")):
    pass
bricks_tex = f"{NS}:block/amethyst_bricks"
write(ASSETS / "blockstates/amethyst_brick_slab.json", {"variants": {
    "type=bottom": {"model": f"{NS}:block/amethyst_brick_slab"},
    "type=double": {"model": f"{NS}:block/amethyst_bricks"},
    "type=top": {"model": f"{NS}:block/amethyst_brick_slab_top"}}})
for suffix, parent in (("", "slab"), ("_top", "slab_top")):
    write(ASSETS / "models/block" / f"amethyst_brick_slab{suffix}.json", {
        "parent": f"minecraft:block/{parent}",
        "textures": {"bottom": bricks_tex, "top": bricks_tex, "side": bricks_tex}})
item_block_model("amethyst_brick_slab")
write(DATA / NS / "loot_table/blocks/amethyst_brick_slab.json", {
    "type": "minecraft:block",
    "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:item", "name": f"{NS}:amethyst_brick_slab",
        "functions": [{"function": "minecraft:set_count", "count": 2, "add": False, "conditions": [
            {"condition": "minecraft:block_state_property", "block": f"{NS}:amethyst_brick_slab",
             "properties": {"type": "double"}}]},
            {"function": "minecraft:explosion_decay"}]}]}]})
pickaxe.append(f"{NS}:amethyst_brick_slab")
write(DATA / NS / "recipe/amethyst_brick_slab.json", {
    "type": "minecraft:crafting_shaped", "pattern": ["###"], "key": {"#": {"item": f"{NS}:amethyst_bricks"}},
    "result": {"id": f"{NS}:amethyst_brick_slab", "count": 6}})

# ---- infected log ----------------------------------------------------------
name("infected_log", "Infected Log", "Заражённое бревно")
write(ASSETS / "blockstates/infected_log.json", {"variants": {
    "axis=y": {"model": f"{NS}:block/infected_log"},
    "axis=z": {"model": f"{NS}:block/infected_log", "x": 90},
    "axis=x": {"model": f"{NS}:block/infected_log", "x": 90, "y": 90}}})
write(ASSETS / "models/block/infected_log.json", {
    "parent": "minecraft:block/cube_column",
    "textures": {"end": f"{NS}:block/infected_log_top", "side": f"{NS}:block/infected_log"}})
item_block_model("infected_log")
loot_self("infected_log")
pickaxe.append(f"{NS}:infected_log")
write(DATA / "minecraft/tags/block/mineable/axe.json", {"values": [f"{NS}:infected_log"]})

# ---- infected (crystal) leaves: never decay, see-through like leaves ----------
name("infected_leaves", "Crystal Leaves", "Кристальная листва")
simple_state("infected_leaves")
write(ASSETS / "models/block/infected_leaves.json", {
    "parent": "minecraft:block/cube_all", "render_type": "minecraft:cutout_mipped",
    "textures": {"all": f"{NS}:block/infected_leaves"}})
item_block_model("infected_leaves")
loot_self("infected_leaves")
write(DATA / "minecraft/tags/block/mineable/hoe.json", {"values": [f"{NS}:infected_leaves"]})

# ---- infected grass block --------------------------------------------------
name("infected_grass_block", "Infected Grass Block", "Заражённый дёрн")
simple_state("infected_grass_block")
write(ASSETS / "models/block/infected_grass_block.json", {
    "parent": "minecraft:block/cube_bottom_top",
    "textures": {"bottom": f"{NS}:block/infected_dirt", "top": f"{NS}:block/infected_grass_block_top",
                 "side": f"{NS}:block/infected_grass_block_side"}})
item_block_model("infected_grass_block")
loot_self("infected_grass_block")
shovel.append(f"{NS}:infected_grass_block")

# ---- clear amethyst (glass-like) -------------------------------------------
name("clear_amethyst", "Clear Amethyst", "Прозрачный аметист")
simple_state("clear_amethyst")
write(ASSETS / "models/block/clear_amethyst.json", {
    "parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent",
    "textures": {"all": f"{NS}:block/clear_amethyst"}})
item_block_model("clear_amethyst")
loot_self("clear_amethyst")

name("clear_amethyst_pane", "Clear Amethyst Pane", "Панель из прозрачного аметиста")
pane = f"{NS}:block/clear_amethyst_pane"
write(ASSETS / "blockstates/clear_amethyst_pane.json", {"multipart": [
    {"apply": {"model": f"{pane}_post"}},
    {"when": {"north": "true"}, "apply": {"model": f"{pane}_side"}},
    {"when": {"east": "true"}, "apply": {"model": f"{pane}_side", "y": 90}},
    {"when": {"south": "true"}, "apply": {"model": f"{pane}_side_alt"}},
    {"when": {"west": "true"}, "apply": {"model": f"{pane}_side_alt", "y": 90}},
    {"when": {"north": "false"}, "apply": {"model": f"{pane}_noside"}},
    {"when": {"east": "false"}, "apply": {"model": f"{pane}_noside_alt"}},
    {"when": {"south": "false"}, "apply": {"model": f"{pane}_noside_alt", "y": 90}},
    {"when": {"west": "false"}, "apply": {"model": f"{pane}_noside", "y": 270}}]})
for part in ("post", "side", "side_alt", "noside", "noside_alt"):
    write(ASSETS / "models/block" / f"clear_amethyst_pane_{part}.json", {
        "parent": f"minecraft:block/template_glass_pane_{part}", "render_type": "minecraft:translucent",
        "textures": {"pane": f"{NS}:block/clear_amethyst", "edge": f"{NS}:block/clear_amethyst_pane_top"}})
item_flat_model("clear_amethyst_pane", f"{NS}:block/clear_amethyst")
loot_self("clear_amethyst_pane")

# ---- crystals --------------------------------------------------------------
facing = {
    "down": {"x": 180}, "east": {"x": 90, "y": 90}, "north": {"x": 90},
    "south": {"x": 90, "y": 180}, "up": {}, "west": {"x": 90, "y": 270},
}
stages = [  # (id pattern, English pattern, Russian pattern, drop range)
    ("crystal_bud_{c}", "{c} Crystal Bud", "{c} бутон кристалла", (1, 1)),
    ("crystal_{c}", "{c} Crystal", "{c} кристалл", (1, 2)),
    ("crystal_large_{c}", "Large {c} Crystal", "Крупный {c_ru} кристалл", (2, 4)),
]
for cid, (c_en, c_ru) in COLORS.items():
    for pattern, en, ru, (lo, hi) in stages:
        bid = pattern.format(c=cid)
        name(bid, en.format(c=c_en), ru.format(c=c_ru, c_ru=c_ru.lower().replace("ый", "ый")))
        model = f"{NS}:block/{bid}"
        write(ASSETS / "blockstates" / f"{bid}.json", {"variants": {
            f"facing={f}": {"model": model, **rot} for f, rot in facing.items()}})
        write(ASSETS / "models/block" / f"{bid}.json", {
            "parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
            "textures": {"cross": model}})
        item_flat_model(bid, model)
        loot_essence(bid, lo, hi)
        pickaxe.append(f"{NS}:{bid}")

# ---- items: gear, staffs, artifacts ----------------------------------------
# id: (English, Russian, tooltip English, tooltip Russian, model parent)
G, H = "minecraft:item/generated", "minecraft:item/handheld"
ITEMS = {
    "frost_crystal": ("Frost Crystal", "Кристалл холода", None, None, G),
    "flame_crystal": ("Flame Crystal", "Кристалл огня", None, None, G),
    "purified_amethyst": ("Purified Amethyst", "Очищенный аметист", None, None, G),
    "resonant_shard": ("Resonant Shard", "Резонирующий осколок",
                       "Dropped now and then by creatures of the Amethyst. Needed for good gear.",
                       "Иногда выпадает из тварей Аметиста. Нужен для хорошего снаряжения.", G),
    "amethyst_sword": ("Amethyst Blade", "Аметистовый клинок",
                       "Strikes may shatter into a crystal burst. Right-click: crystal dash through enemies.",
                       "Удары могут взрываться кристальными осколками. ПКМ: кристальный рывок сквозь врагов.", H),
    "amethyst_pickaxe": ("Amethyst Pickaxe", "Аметистовая кирка", None, None, H),
    "amethyst_axe": ("Amethyst Axe", "Аметистовый топор", None, None, H),
    "amethyst_shovel": ("Amethyst Shovel", "Аметистовая лопата", None, None, H),
    "amethyst_hoe": ("Amethyst Hoe", "Аметистовая мотыга", None, None, H),
    "amethyst_helmet": ("Amethyst Helmet", "Аметистовый шлем", None, None, G),
    "amethyst_chestplate": ("Amethyst Chestplate", "Аметистовый нагрудник", None, None, G),
    "amethyst_leggings": ("Amethyst Leggings", "Аметистовые поножи", None, None, G),
    "amethyst_boots": ("Amethyst Boots", "Аметистовые ботинки", None, None, G),
    "purified_helmet": ("Purifier's Helmet", "Шлем очищения", None, None, G),
    "purified_chestplate": ("Purifier's Chestplate", "Нагрудник очищения", None, None, G),
    "purified_leggings": ("Purifier's Leggings", "Поножи очищения", None, None, G),
    "purified_boots": ("Purifier's Boots", "Ботинки очищения", None, None, G),
    "flame_staff": ("Flame Staff", "Посох пламени",
                    "Hold right-click: a flamethrower that sets everything ahead ablaze.",
                    "Зажмите ПКМ: огнемёт, поджигающий всё впереди.", H),
    "frost_staff": ("Frost Staff", "Ледяной посох",
                    "Right-click: a wave of cold that freezes and slows enemies and turns water to ice.",
                    "ПКМ: волна холода замораживает и замедляет врагов, превращает воду в лёд.", H),
    "prism_scepter": ("Prism Scepter", "Призматический скипетр",
                      "Hold right-click: a piercing beam of crystal light.",
                      "Зажмите ПКМ: пронзающий луч кристального света.", H),
    "amitariel_spear": ("Spear of Amitariel", "Копьё Амитариэля",
                        "Tap right-click: a whirling spin. Hold and let go: a spinning dash that ends throwing enemies back.",
                        "Нажмите ПКМ: вихрь с копьём. Зажмите и отпустите: рывок с вращением, в конце отбрасывающий врагов.", H),
    "crystal_hammer": ("Crystal Hammer", "Кристальный молот",
                       "Hold right-click: leap up, charge for 3 seconds in the air, then crash down, hurling enemies away and healing yourself.",
                       "Зажмите ПКМ: прыжок, 3 секунды заряда в воздухе, затем удар о землю, отбрасывающий врагов и лечащий вас.", H),
    "aegis_heart": ("Aegis Heart", "Сердце эгиды",
                    "Carry it: Resistance. Right-click: a thick crystal shell of absorption and resistance.",
                    "В инвентаре: сопротивление урону. ПКМ: плотный кристальный щит с поглощением и защитой.", G),
    "amethyst_wings": ("Amethyst Wings", "Аметистовые крылья",
                       "Right-click: 25 seconds of free flight.",
                       "ПКМ: 25 секунд свободного полёта.", G),
    "crescent_blade": ('Crescent Blade', 'Клинок полумесяцев',
        'Hold right-click and let go: crescents of light fly out. Hold longer for more and stronger ones.',
        'Зажмите ПКМ и отпустите: вылетают полумесяцы света. Чем дольше держать, тем их больше и они сильнее.', H),
    "purified_spear": ('Purified Crystal Spear', 'Очищенное кристальное копьё',
        'Hold right-click: spears of light fly where you look. Hold 10 seconds for a volley.',
        'Зажмите ПКМ: копья света летят туда, куда вы смотрите. Держите 10 секунд для шквала.', H),
    "purity_aegis": ('Aegis of Purity', 'Эгида чистоты',
        'Charge it with purified amethyst (right-click it with the essence). While charged: white hearts and a dome of light.',
        'Заряжается очищенным аметистом (ПКМ эссенцией по эгиде). Пока заряжена: белые сердца и купол света.', G),
    "purified_crystal_apple": ('Purified Crystal Apple', 'Очищенное кристальное яблоко',
        'Strong regeneration and extra hearts. Cures the blight.',
        'Сильная регенерация и дополнительные сердца. Лечит порчу.', G),
    "purified_crystal_bread": ('Purified Crystal Bread', 'Очищенный кристальный хлеб',
        'Very filling, a little regeneration. Cures the blight.',
        'Очень сытный, немного регенерации. Лечит порчу.', G),
    "cereal_with_milk": ('Cereal with Milk', 'Хлопья с молоком',
        'The best breakfast in any world: speed, regeneration, haste and high jumps.',
        'Лучший завтрак в любом мире: скорость, регенерация, спешка и высокие прыжки.', G),
    "country_potatoes": ('Country Potatoes with Sauce', 'Картошка по-деревенски с соусом',
        'Mmm, what tasty potatoes! A rush of strength and toughness.',
        'Ммм, какая вкусная картошка! Прилив силы и прочности.', G),
    "purity_amulet": ("Purity Amulet", "Амулет чистоты",
                      "Carry it: immune to the blight; creatures of the Amethyst deal 25% less damage.",
                      "В инвентаре: иммунитет к порче; твари Аметиста наносят на 25% меньше урона.", G),
    "blink_ring": ("Blink Ring", "Кольцо мерцания",
                   "Carry it: Speed. Right-click: teleport up to 14 blocks ahead.",
                   "В инвентаре: скорость. ПКМ: телепорт на 14 блоков вперёд.", G),
    "vampiric_shard": ("Vampiric Shard", "Вампирический осколок",
                       "Carry it: every kill restores your health.",
                       "В инвентаре: каждое убийство восстанавливает здоровье.", G),
    "berserker_core": ("Berserker Core", "Ядро берсерка",
                       "Right-click: 20 seconds of great strength, speed and haste. Then a crash.",
                       "ПКМ: 20 секунд огромной силы, скорости и спешки. Потом упадок сил.", G),
    "stasis_crystal": ("Stasis Crystal", "Кристалл стазиса",
                       "Right-click: freezes every enemy within 12 blocks in time.",
                       "ПКМ: замораживает во времени всех врагов в радиусе 12 блоков.", G),
    "gravity_orb": ("Gravity Orb", "Сфера гравитации",
                    "Right-click: pulls enemies within 16 blocks to you and batters them.",
                    "ПКМ: стягивает врагов в радиусе 16 блоков к вам и бьёт их.", G),
    "crystal_totem": ("Crystal Totem", "Кристальный тотем",
                      "Carry it: cheats death once and unleashes a shockwave.",
                      "В инвентаре: один раз спасает от смерти и выпускает ударную волну.", G),
    "thunder_shard": ("Thunder Shard", "Осколок грома",
                      "Right-click: calls a triple lightning strike on what you look at.",
                      "ПКМ: тройной удар молнии туда, куда вы смотрите.", G),
    "talis_hammer": ("Jaybee Talis's Hammer", "Молот Джейби Талиса",
                     "Hold right-click to charge for up to 5 seconds, release to smash the floor with lightning and hurl everything away.",
                     "Зажмите ПКМ, чтобы зарядить до 5 секунд, отпустите: удар о пол с молниями отбросит всё вокруг.", H),
    "flame_brand": ("Flame Brand", "Пламенный клинок",
                    "Blows ignite. Right-click: a wide burst of fire.", "Удары поджигают. ПКМ: широкая вспышка огня.", H),
    "frost_edge": ("Frost Edge", "Ледяной клинок",
                   "Blows slow and chill. Right-click: a nova of cold around you.", "Удары замедляют и охлаждают. ПКМ: волна холода вокруг вас.", H),
    "storm_blade": ("Storm Blade", "Громовой клинок",
                    "Blows may call lightning. Right-click: chain lightning.", "Удары могут вызывать молнию. ПКМ: цепная молния.", H),
    "void_edge": ("Void Edge", "Клинок пустоты",
                  "Blows weaken and heal you. Right-click: step through space up to 10 blocks.", "Удары ослабляют цель и лечат вас. ПКМ: шаг сквозь пространство на 10 блоков.", H),
    "purifier_blade": ("Purifier's Blade", "Клинок очищения",
                       "Extra damage to creatures of the Amethyst. Right-click: a purging flash.", "Бонусный урон тварям Аметиста. ПКМ: очищающая вспышка.", H),
    "crystal_bow": ("Crystal Bow", "Кристальный лук",
                    "Made to fire crystal arrows.", "Создан для кристальных стрел.", G),
    "crystal_arrow": ("Crystal Arrow", "Кристальная стрела",
                      "Shatters on impact, hurting everything around the target.", "Разбивается при попадании и ранит всё вокруг цели.", G),
    "amethyst_shield": ("Amethyst Shield", "Аметистовый щит",
                        "Whatever it blocks gets a crystal burst back.", "Тот, чей удар он блокирует, получает кристальный взрыв в ответ.", G),
    "crystal_geode": ("Crystal Geode", "Кристальная жеода",
                      "The seed of an infection core. Use it on the ground to plant a core of its level.",
                      "Семя ядра заражения. Используйте на земле, чтобы посадить ядро его уровня.", G),
    "guide_book": ("Amethyst Compendium", "Аметистовый компендиум",
                   "Right-click to read: creatures, weapons, recipes and more.", "ПКМ, чтобы читать: твари, оружие, рецепты и многое другое.", G),
}
for iid, (en, ru, tip_en, tip_ru, parent) in ITEMS.items():
    lang_en[f"item.{NS}.{iid}"] = en
    lang_ru[f"item.{NS}.{iid}"] = ru
    if tip_en:
        lang_en[f"item.{NS}.{iid}.tooltip"] = tip_en
        lang_ru[f"item.{NS}.{iid}.tooltip"] = tip_ru
    write(ASSETS / "models/item" / f"{iid}.json", {"parent": parent, "textures": {"layer0": f"{NS}:item/{iid}"}})

# item tags so vanilla enchanting and tool logic recognise the gear
tool_items = {
    "swords": ["amethyst_sword", "flame_brand", "frost_edge", "storm_blade", "void_edge", "purifier_blade"], "pickaxes": ["amethyst_pickaxe"], "axes": ["amethyst_axe"],
    "shovels": ["amethyst_shovel"], "hoes": ["amethyst_hoe"],
    "head_armor": ["amethyst_helmet", "purified_helmet"],
    "chest_armor": ["amethyst_chestplate", "purified_chestplate"],
    "leg_armor": ["amethyst_leggings", "purified_leggings"],
    "foot_armor": ["amethyst_boots", "purified_boots"],
}
for tag, ids in tool_items.items():
    write(DATA / "minecraft/tags/item" / f"{tag}.json", {"values": [f"{NS}:{i}" for i in ids]})
armor_ids = [f"{NS}:{i}" for t in ("head_armor", "chest_armor", "leg_armor", "foot_armor") for i in tool_items[t]]
sword_ids = [f"{NS}:{i}" for i in ("amethyst_sword", "flame_brand", "frost_edge", "storm_blade", "void_edge", "purifier_blade", "crystal_hammer", "talis_hammer")]
mining_ids = [f"{NS}:amethyst_{t}" for t in ("pickaxe", "axe", "shovel", "hoe")]
for tag, ids in {
    "enchantable/armor": armor_ids, "enchantable/head_armor": [f"{NS}:{i}" for i in tool_items["head_armor"]],
    "enchantable/chest_armor": [f"{NS}:{i}" for i in tool_items["chest_armor"]],
    "enchantable/leg_armor": [f"{NS}:{i}" for i in tool_items["leg_armor"]],
    "enchantable/foot_armor": [f"{NS}:{i}" for i in tool_items["foot_armor"]],
    "enchantable/equippable": armor_ids, "enchantable/sword": sword_ids,
    "enchantable/weapon": sword_ids, "enchantable/sharp_weapon": sword_ids[:6],
    "enchantable/fire_aspect": sword_ids, "enchantable/mining": mining_ids,
    "enchantable/mining_loot": mining_ids,
    "enchantable/durability": armor_ids + sword_ids + mining_ids + [
        f"{NS}:{i}" for i in ("flame_staff", "frost_staff", "prism_scepter", "crystal_bow", "amethyst_shield")],
    "enchantable/bow": [f"{NS}:crystal_bow"],
    "enchantable/vanishing": armor_ids + sword_ids + mining_ids,
}.items():
    write(DATA / "minecraft/tags/item" / f"{tag}.json", {"values": ids})

# ---- creatures: names, drops, spawn eggs ------------------------------------
ENTITIES = {
    "amethyst_stalker": ("Amethyst Stalker", "Аметистовый сталкер"),
    "crystal_weaver": ("Crystal Weaver", "Кристальный ткач"),
    "shard_walker": ("Shard-Walker", "Осколочный ходок"),
    "void_lurker": ("Void-Crystal Lurker", "Кристальный лурк пустоты"),
    "geode_colossus": ("Geode Colossus", "Колосс-геода"),
    "celestial_drifter": ("Celestial Drifter", "Небесный дрейфер"),
    "amitariel": ("Amitariel, Apostle of the Amethyst", "Амитариэль, апостол Аметиста"),
    "amethyst_heart": ("The Amethyst Heart", "Сердце Аметиста"),
    "crystal_shard": ("Crystal Shard", "Кристальный осколок"),
    "crystal_arrow": ("Crystal Arrow", "Кристальная стрела"),
}
for eid, (en, ru) in ENTITIES.items():
    lang_en[f"entity.{NS}.{eid}"] = en
    lang_ru[f"entity.{NS}.{eid}"] = ru
    if eid not in ("crystal_shard", "crystal_arrow"):
        lang_en[f"item.{NS}.{eid}_spawn_egg"] = f"{en} Spawn Egg"
        lang_ru[f"item.{NS}.{eid}_spawn_egg"] = f"Яйцо призыва: {ru}"
        write(ASSETS / "models/item" / f"{eid}_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
for iid, en, ru in (("crystal_shard", "Crystal Shard", "Кристальный осколок"),
                    ("portal_core", "Portal Core", "Ядро портала"),
                    ("heart_core", "Heart of the Amethyst", "Сердце Аметиста (ядро)")):
    lang_en[f"item.{NS}.{iid}"] = en
    lang_ru[f"item.{NS}.{iid}"] = ru
    write(ASSETS / "models/item" / f"{iid}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{iid}"}})
lang_en[f"item.{NS}.portal_core.tooltip"] = "Placed on the portal frame in the tower, it opens the way to the world of the Amethyst."
lang_ru[f"item.{NS}.portal_core.tooltip"] = "Положите на рамку портала в башне, и откроется путь в мир Аметиста."
lang_en[f"item.{NS}.heart_core.tooltip"] = "The pulsing core of the Amethyst itself."
lang_ru[f"item.{NS}.heart_core.tooltip"] = "Пульсирующее ядро самого Аметиста."


def entity_loot(eid, drops):
    pools = []
    for item_id, lo, hi, chance in drops:
        entry = {"type": "minecraft:item", "name": item_id}
        if hi > 1 or lo != hi:
            entry["functions"] = [{"function": "minecraft:set_count",
                                   "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
        pool = {"rolls": 1, "entries": [entry]}
        if chance < 1:
            pool["conditions"] = [{"condition": "minecraft:random_chance", "chance": chance}]
        pools.append(pool)
    write(DATA / NS / "loot_table/entities" / f"{eid}.json", {"type": "minecraft:entity", "pools": pools})


A_ESS = f"{NS}:amethyst_essence"
RS = f"{NS}:resonant_shard"
entity_loot("amethyst_stalker", [(A_ESS, 0, 2, 1), (RS, 1, 1, 0.08)])
entity_loot("crystal_weaver", [(A_ESS, 0, 1, 1), ("minecraft:string", 0, 2, 1), (RS, 1, 1, 0.08)])
entity_loot("shard_walker", [(A_ESS, 2, 4, 1), (f"{NS}:purified_amethyst", 1, 1, 0.15), (RS, 1, 1, 0.3)])
entity_loot("void_lurker", [(A_ESS, 1, 2, 1), (RS, 1, 1, 0.2)])
entity_loot("geode_colossus", [(A_ESS, 6, 12, 1), (RS, 2, 3, 1), (f"{NS}:flame_crystal", 1, 1, 0.25), (f"{NS}:frost_crystal", 1, 1, 0.25)])
entity_loot("celestial_drifter", [(A_ESS, 3, 6, 1), (RS, 1, 1, 0.35), (f"{NS}:frost_crystal", 1, 1, 0.25)])
entity_loot("amitariel", [(f"{NS}:amitariel_spear", 1, 1, 1), (RS, 3, 5, 1), (A_ESS, 12, 20, 1), (f"{NS}:brilliant_amethyst", 1, 3, 1)])
lang_en[f"entity.{NS}.infection_core"] = "Infection Core"
lang_ru[f"entity.{NS}.infection_core"] = "Ядро заражения"
# the base drop; cores in phase 2 and 3 drop more on top of it (see InfectionCoreEntity)
entity_loot("infection_core", [(A_ESS, 2, 4, 1), (RS, 1, 1, 0.1)])
entity_loot("amethyst_heart", [(f"{NS}:heart_core", 1, 1, 1), (A_ESS, 30, 50, 1), (f"{NS}:brilliant_amethyst", 4, 8, 1)])

# ---- story: the apostle speaks, the heart changes ---------------------------
STORY = {
    "entity.amethystmod.amitariel.short": ("Amitariel", "Амитариэль"),
    "message.amethystmod.amitariel.line1": ("I have been waiting for you.", "Я вас ждал."),
    "message.amethystmod.amitariel.line2": ("You are strong... stronger than I thought.", "Вы сильны... Сильнее, чем я думал."),
    "message.amethystmod.amitariel.line3": ("Become a follower of the Amethyst.", "Станьте последователем Аметиста."),
    "message.amethystmod.amitariel.line4": ("Accept its gift, and you will receive power you never dreamed of.",
                                            "Примите его дар, и вы получите больше силы, чем могли мечтать."),
    "message.amethystmod.amitariel.question": ("Will you join the Amethyst? Say \"yes\" in chat... or say \"no\" and prove your folly.",
                                               "Вы присоединитесь к Аметисту? Напишите в чат «да»... или «нет» и докажите свою глупость."),
    "message.amethystmod.amitariel.yes": ("A wise choice. The Amethyst welcomes you.", "Мудрый выбор. Аметист принимает вас."),
    "message.amethystmod.amitariel.no": ("Fools. Then die!", "Глупцы. Тогда умрите!"),
    "message.amethystmod.amitariel.timeout": ("Silence is an answer too. Die!", "Молчание тоже ответ. Умрите!"),
    "message.amethystmod.amitariel.attacked": ("So that is your answer!", "Вот ваш ответ!"),
    "message.amethystmod.amitariel.phase2": ("You insects think far too much of yourselves!",
                                             "А вы, букашки, слишком много о себе возомнили!"),
    "message.amethystmod.amitariel.death": ("The Amethyst... will forgive me...", "Аметист... простит меня..."),
    "message.amethystmod.heart.phase2": ("The Heart beats faster. Crystal servants awaken.", "Сердце бьётся быстрее. Кристальные слуги пробуждаются."),
    "message.amethystmod.heart.phase3": ("The crystals sing. The Heart rains shards upon you.", "Кристаллы поют. Сердце обрушивает на вас град осколков."),
    "message.amethystmod.heart.phase4": ("The Heart of the Amethyst goes mad!", "Сердце Аметиста обезумело!"),
    "commands.amethystmod.tower.building": ("The tower awakens...", "Башня пробуждается..."),
    "commands.amethystmod.tower.done": ("The tower stands at 0, 0.", "Башня стоит на 0, 0."),
    "commands.amethystmod.tower.missing": ("The tower has not been built yet.", "Башня ещё не построена."),
    "commands.amethystmod.tower.busy": ("The tower is being built right now.", "Башня сейчас строится."),
}
for key, (en, ru) in STORY.items():
    lang_en[key] = en
    lang_ru[key] = ru

# ---- the cores of the infection ---------------------------------------------
CORES = {
    "item.amethystmod.crystal_geode.level": ("%s (level %s)", "%s (уровень %s)"),
    "message.amethystmod.geode.overworld": ("The geode only takes root in the normal world.", "Жеода приживается только в обычном мире."),
    "message.amethystmod.geode.dead": ("The Heart has fallen: the geode is dead.", "Сердце повержено: жеода мертва."),
    "message.amethystmod.core.barrier": ("Only purified amethyst can pierce the core's barrier.",
                                         "Барьер ядра пробивает только очищенный аметист."),
    "bossbar.amethystmod.core": ("Infection Core, phase %s", "Ядро заражения, фаза %s"),
    "bossbar.amethystmod.core.barrier": ("Infection Core, phase %s: barrier %s%%", "Ядро заражения, фаза %s: барьер %s%%"),
    "commands.amethystmod.core.overworld": ("Cores live only in the normal world.", "Ядра живут только в обычном мире."),
    "commands.amethystmod.core.none": ("No core nearby.", "Рядом нет ядра."),
    "commands.amethystmod.core.stopped": ("The Heart has fallen: no new cores can grow.", "Сердце повержено: новые ядра не растут."),
    "commands.amethystmod.core.spawned": ("Core #%s (phase %s) at %s %s.", "Ядро №%s (фаза %s) на %s %s."),
    "commands.amethystmod.core.phase": ("Core #%s is now in phase %s.", "Ядро №%s теперь в фазе %s."),
    "commands.amethystmod.core.upgraded": ("Raised the phase of %s cores.", "Фаза повышена у ядер: %s."),
    "commands.amethystmod.core.killed": ("Removed %s cores (their land stays infected).", "Убрано ядер: %s (их земля остаётся заражённой)."),
    "commands.amethystmod.core.list": ("%s cores in the world, %s within %s blocks:", "Ядер в мире: %s, в радиусе %3$s блоков: %2$s:"),
    "commands.amethystmod.core.line": ("#%s phase %s at %s %s, %s blocks away, %s days old, land radius %s",
                                       "№%s фаза %s на %s %s, до него %s блоков, возраст %s дн., радиус земли %s"),
    "commands.amethystmod.core.geode": ("Gave %s crystal geodes of level %s.", "Выдано кристальных жеод: %s, уровень %s."),
    "commands.amethystmod.core.thrown": ("Core #%s threw a geode: new core #%s at %s %s.", "Ядро №%s бросило жеоду: новое ядро №%s на %s %s."),
    "commands.amethystmod.core.nothrow": ("The geode found no free place to land.", "Жеоде негде приземлиться."),
    "gamerule.amethystCorePhase2Days": ("Infection cores: days to phase 2", "Ядра заражения: дней до фазы 2"),
    "gamerule.amethystCorePhase2Days.description": ("In-game days after which a core reaches phase 2 (6 by default).",
                                                    "Через сколько игровых дней ядро доходит до фазы 2 (по умолчанию 6)."),
    "gamerule.amethystCorePhase3Days": ("Infection cores: days to phase 3", "Ядра заражения: дней до фазы 3"),
    "gamerule.amethystCorePhase3Days.description": ("In-game days from its birth after which a core reaches phase 3 (15 by default).",
                                                    "Через сколько игровых дней от рождения ядро доходит до фазы 3 (по умолчанию 15)."),
    "biome.amethystmod.crystal_wastes": ("Crystal Wastes", "Кристальные пустоши"),
}
for key, (en, ru) in CORES.items():
    lang_en[key] = en
    lang_ru[key] = ru

# what pierces the barrier of a core: everything made of purified amethyst
write(DATA / NS / "tags/item/purified_amethyst.json", {"values": [f"{NS}:{i}" for i in (
    "purified_amethyst", "purified_helmet", "purified_chestplate", "purified_leggings", "purified_boots",
    "purified_spear", "purifier_blade", "crescent_blade", "purity_aegis", "purity_shard", "purity_amulet")]})

# the land of old cores: the crystal wastes (the biome is set by the infection engine, never generated)
write(DATA / NS / "worldgen/biome/crystal_wastes.json", {
    "has_precipitation": False, "temperature": 0.7, "downfall": 0.2, "creature_spawn_probability": 0.0,
    "effects": {
        "sky_color": 0xA27BE6, "fog_color": 0xC6A3F0, "water_color": 0x9A62E8, "water_fog_color": 0x3A1F66,
        "grass_color": 0xA070D8, "foliage_color": 0x9A62D6,
        "particle": {"options": {"type": f"{NS}:crystal_spark"}, "probability": 0.003},
        "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0},
        "additions_sound": {"sound": "minecraft:block.amethyst_block.chime", "tick_chance": 0.008},
    },
    "spawners": {"monster": [
        {"type": f"{NS}:amethyst_stalker", "weight": 8, "minCount": 1, "maxCount": 1},
        {"type": f"{NS}:crystal_weaver", "weight": 4, "minCount": 1, "maxCount": 1}],
        "creature": [], "ambient": [], "axolotls": [], "underground_water_creature": [], "water_creature": [],
        "water_ambient": [], "misc": []},
    "spawn_costs": {}, "carvers": {"air": []}, "features": [],
})

def item_entry(item_id, weight, lo=None, hi=None):
    e = {"type": "minecraft:item", "name": item_id, "weight": weight}
    if lo is not None:
        e["functions"] = [{"function": "minecraft:set_count", "count": {"min": lo, "max": hi}}]
    return e


def chest(name, pools):
    write(DATA / NS / "loot_table/chests" / f"{name}.json", {"type": "minecraft:chest", "pools": pools})


def n(i):
    return f"{NS}:{i}"


materials = [item_entry(n("amethyst_essence"), 30, 3, 9), item_entry(n("resonant_shard"), 7, 1, 2), item_entry(n("purified_amethyst"), 8, 1, 3),
             item_entry(n("flame_crystal"), 4), item_entry(n("frost_crystal"), 4),
             item_entry(n("brilliant_amethyst"), 6, 1, 4), item_entry("minecraft:diamond", 5, 1, 3),
             item_entry("minecraft:golden_apple", 4), item_entry("minecraft:ender_pearl", 6, 1, 3),
             item_entry(n("crystal_arrow"), 8, 6, 16)]
artifacts_all = [item_entry(n(i), 1) for i in ("aegis_heart", "amethyst_wings", "blink_ring", "vampiric_shard",
                                                 "berserker_core", "stasis_crystal", "gravity_orb", "crystal_totem", "thunder_shard")]
artifacts_low = [item_entry(n(i), 1) for i in ("blink_ring", "vampiric_shard", "gravity_orb", "stasis_crystal")]
swords = [item_entry(n(i), 3) for i in ("amethyst_sword", "flame_brand", "frost_edge", "storm_blade", "void_edge", "purifier_blade")]
bows = [item_entry(n("crystal_bow"), 3), item_entry(n("amethyst_shield"), 3)]
staffs = [item_entry(n(i), 2) for i in ("flame_staff", "frost_staff", "prism_scepter")]
hammers = [item_entry(n("crystal_hammer"), 2), item_entry(n("talis_hammer"), 1)]
armor = [item_entry(n(i), 2) for i in ("amethyst_helmet", "amethyst_chestplate", "amethyst_leggings", "amethyst_boots",
                                       "purified_helmet", "purified_chestplate", "purified_leggings", "purified_boots")]

chest("tower_low", [
    {"rolls": {"min": 2, "max": 4}, "entries": materials},
    {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": 0.35}], "entries": swords + bows + armor},
    {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": 0.12}], "entries": artifacts_low},
    {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": 0.6}], "entries": [item_entry(n("guide_book"), 1)]},
    {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": 0.15}], "entries": [item_entry(n("purity_amulet"), 1)]}])
chest("tower_mid", [
    {"rolls": {"min": 3, "max": 5}, "entries": materials},
    {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": 0.6}], "entries": swords + bows + staffs + armor},
    {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": 0.35}], "entries": artifacts_all},
    {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": 0.3}], "entries": [item_entry(n("purity_amulet"), 1)]},
    {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": 0.4}],
     "entries": [item_entry(n("purified_crystal_bread"), 3, 1, 3), item_entry(n("country_potatoes"), 2, 1, 2),
                 item_entry(n("purified_crystal_apple"), 1), item_entry(n("cereal_with_milk"), 1)]}])
chest("tower_high", [
    {"rolls": {"min": 4, "max": 6}, "entries": materials},
    {"rolls": 1, "entries": swords + staffs + hammers + bows + armor},
    {"rolls": 1, "entries": artifacts_all},
    {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": 0.4}], "entries": artifacts_all + hammers},
    {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": 0.45}], "entries": [item_entry(n("purity_amulet"), 1)]},
    {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": 0.6}],
     "entries": [item_entry(n("purified_crystal_apple"), 2, 1, 2), item_entry(n("country_potatoes"), 2, 1, 2),
                 item_entry(n("cereal_with_milk"), 1)]}])

# ---- the guide book --------------------------------------------------------
import guide_text
for cat, (en, ru) in guide_text.CATEGORIES.items():
    lang_en[f"guide.{NS}.cat.{cat}"] = en
    lang_ru[f"guide.{NS}.cat.{cat}"] = ru
for key, (en, ru) in guide_text.UI.items():
    lang_en[f"guide.{NS}.{key}"] = en
    lang_ru[f"guide.{NS}.{key}"] = ru
for gid, (t_en, t_ru, x_en, x_ru) in guide_text.GUIDE.items():
    lang_en[f"guide.{NS}.{gid}.title"] = t_en
    lang_ru[f"guide.{NS}.{gid}.title"] = t_ru
    lang_en[f"guide.{NS}.{gid}.text"] = x_en
    lang_ru[f"guide.{NS}.{gid}.text"] = x_ru

# ---- effects, particles, set bonuses ---------------------------------------
for key, en, ru in (
    ("effect.amethystmod.crystal_resonance", "Crystal Resonance", "Кристальный резонанс"),
    ("effect.amethystmod.anti_amethyst_ward", "Anti-Amethyst Ward", "Антиаметистовая защита"),
    ("effect.amethystmod.amethyst_blight", "Amethyst Blight", "Аметистовая порча"),
    ("tooltip.amethystmod.set.amethyst", "Full set: Crystal Resonance. More damage, faster strikes, extra speed.",
     "Полный комплект: Кристальный резонанс. Больше урона, быстрее удары, выше скорость."),
    ("tooltip.amethystmod.set.purified",
     "Full set: Anti-Amethyst Ward. -80% damage from amethyst creatures, immune to blight, heals on infected ground and burns the infection away.",
     "Полный комплект: Антиаметистовая защита. -80% урона от аметистовых тварей, иммунитет к порче, лечит на заражённой земле и выжигает заражение."),
):
    lang_en[key] = en
    lang_ru[key] = ru
for pname in ("crystal_spark", "ward_mote", "blight_spore", "prism_flare"):
    write(ASSETS / "particles" / f"{pname}.json", {"textures": [f"{NS}:{pname}_{i}" for i in range(8)]})

write(DATA / "minecraft/tags/item/arrows.json", {"values": [f"{NS}:crystal_arrow"]})

# mobs of the infection (filled in with the creatures stage); purified armor resists them
write(DATA / NS / "tags/entity_type/amethyst_creatures.json", {"values": []})

# ---- lang, tags, recipes ---------------------------------------------------
write(ASSETS / "lang/en_us.json", lang_en)
write(ASSETS / "lang/ru_ru.json", lang_ru)
write(DATA / "minecraft/tags/block/mineable/pickaxe.json", {"values": pickaxe})
write(DATA / "minecraft/tags/block/mineable/shovel.json", {"values": shovel})

ess = {"item": f"{NS}:amethyst_essence"}


def item(i):
    return {"item": f"{NS}:{i}"}


recipes = {
    "amethyst_crystal_block": {"type": "minecraft:crafting_shaped", "pattern": ["##", "##"],
                               "key": {"#": ess}, "result": {"id": f"{NS}:amethyst_crystal_block", "count": 1}},
    "amethyst_bricks": {"type": "minecraft:crafting_shaped", "pattern": ["##", "##"],
                        "key": {"#": item("amethyst_crystal_block")},
                        "result": {"id": f"{NS}:amethyst_bricks", "count": 4}},
    "brilliant_amethyst": {"type": "minecraft:crafting_shapeless",
                           "ingredients": [item("amethyst_crystal_block"), {"item": "minecraft:glowstone_dust"}],
                           "result": {"id": f"{NS}:brilliant_amethyst", "count": 1}},
    "clear_amethyst_pane": {"type": "minecraft:crafting_shaped", "pattern": ["###", "###"],
                            "key": {"#": item("clear_amethyst")},
                            "result": {"id": f"{NS}:clear_amethyst_pane", "count": 16}},
    "clear_amethyst": {"type": "minecraft:smelting", "ingredient": item("infected_sand"),
                       "result": {"id": f"{NS}:clear_amethyst"}, "experience": 0.1, "cookingtime": 200},
}
def mc(i):
    return {"item": f"minecraft:{i}"}


def shaped(pattern, key, result, count=1):
    return {"type": "minecraft:crafting_shaped", "pattern": pattern, "key": key,
            "result": {"id": f"{NS}:{result}", "count": count}}


E, B = item("amethyst_essence"), item("amethyst_crystal_block")
R = item("resonant_shard")
for mat, src in (("amethyst", B), ("purified", item("purified_amethyst"))):
    recipes[f"{mat}_helmet"] = shaped(["#R#", "# #"], {"#": src, "R": R}, f"{mat}_helmet")
    recipes[f"{mat}_chestplate"] = shaped(["#R#", "###", "###"], {"#": src, "R": R}, f"{mat}_chestplate")
    recipes[f"{mat}_leggings"] = shaped(["#R#", "# #", "# #"], {"#": src, "R": R}, f"{mat}_leggings")
    recipes[f"{mat}_boots"] = shaped(["R R", "# #"], {"#": src, "R": R}, f"{mat}_boots")
stick = mc("stick")
recipes["amethyst_sword"] = shaped(["B", "B", "S"], {"B": B, "S": stick}, "amethyst_sword")
recipes["amethyst_pickaxe"] = shaped(["###", " S ", " S "], {"#": E, "S": stick}, "amethyst_pickaxe")
recipes["amethyst_axe"] = shaped(["##", "#S", " S"], {"#": E, "S": stick}, "amethyst_axe")
recipes["amethyst_shovel"] = shaped(["#", "S", "S"], {"#": E, "S": stick}, "amethyst_shovel")
recipes["amethyst_hoe"] = shaped(["##", " S", " S"], {"#": E, "S": stick}, "amethyst_hoe")

recipes["frost_crystal"] = shaped(["III", "IEI", "III"], {"I": mc("ice"), "E": E}, "frost_crystal")
recipes["flame_crystal"] = shaped([" R ", "RER", " R "], {"R": mc("blaze_rod"), "E": E}, "flame_crystal")
recipes["purified_amethyst"] = {"type": "minecraft:crafting_shapeless",
                                "ingredients": [E, E, E, E, mc("diamond"), mc("quartz")],
                                "result": {"id": f"{NS}:purified_amethyst", "count": 2}}

recipes["flame_staff"] = shaped(["  F", " S ", "E  "], {"F": item("flame_crystal"), "S": mc("blaze_rod"), "E": E}, "flame_staff")
recipes["frost_staff"] = shaped(["  F", " S ", "E  "], {"F": item("frost_crystal"), "S": stick, "E": E}, "frost_staff")
recipes["prism_scepter"] = shaped(["  B", " R ", "R  "], {"B": item("brilliant_amethyst"), "R": mc("end_rod")}, "prism_scepter")
recipes["crystal_hammer"] = shaped(["BRB", "BSB", " S "], {"B": B, "R": item("resonant_shard"), "S": stick}, "crystal_hammer")

recipes["aegis_heart"] = shaped(["BEB", "EDE", "BEB"], {"B": B, "E": E, "D": mc("diamond")}, "aegis_heart")
recipes["amethyst_wings"] = shaped(["PBP", "FEF", "P P"], {"P": mc("phantom_membrane"), "B": item("brilliant_amethyst"), "F": mc("feather"), "E": E}, "amethyst_wings")
recipes["blink_ring"] = shaped(["GEG", "P P", "GEG"], {"G": mc("gold_ingot"), "E": E, "P": mc("ender_pearl")}, "blink_ring")
recipes["vampiric_shard"] = {"type": "minecraft:crafting_shapeless",
                             "ingredients": [B, mc("ghast_tear"), mc("redstone_block")],
                             "result": {"id": f"{NS}:vampiric_shard", "count": 1}}
recipes["berserker_core"] = shaped(["RER", "EFE", "RER"], {"R": mc("redstone_block"), "E": E, "F": item("flame_crystal")}, "berserker_core")
recipes["stasis_crystal"] = shaped(["QEQ", "EFE", "QEQ"], {"Q": mc("quartz"), "E": E, "F": item("frost_crystal")}, "stasis_crystal")
recipes["gravity_orb"] = shaped(["OEO", "EPE", "OEO"], {"O": mc("obsidian"), "E": E, "P": mc("ender_pearl")}, "gravity_orb")
recipes["crystal_totem"] = shaped(["BEB", "ETE", "BEB"], {"B": B, "E": E, "T": mc("totem_of_undying")}, "crystal_totem")
recipes["thunder_shard"] = shaped(["LEL", "EBE", "LEL"], {"L": mc("lightning_rod"), "E": E, "B": item("brilliant_amethyst")}, "thunder_shard")

recipes["talis_hammer"] = shaped(["LBL", "BSB", " S "], {"L": mc("lightning_rod"), "B": item("brilliant_amethyst"), "S": mc("blaze_rod")}, "talis_hammer")
recipes["flame_brand"] = shaped(["F", "B", "S"], {"F": item("flame_crystal"), "B": B, "S": mc("blaze_rod")}, "flame_brand")
recipes["frost_edge"] = shaped(["F", "B", "S"], {"F": item("frost_crystal"), "B": B, "S": stick}, "frost_edge")
recipes["storm_blade"] = shaped(["L", "B", "S"], {"L": mc("lightning_rod"), "B": B, "S": stick}, "storm_blade")
recipes["void_edge"] = shaped(["P", "B", "S"], {"P": mc("ender_eye"), "B": B, "S": stick}, "void_edge")
recipes["purifier_blade"] = shaped(["P", "B", "S"], {"P": item("purified_amethyst"), "B": B, "S": mc("iron_ingot")}, "purifier_blade")
recipes["crystal_bow"] = shaped([" BS", "B S", " BS"], {"B": B, "S": mc("string")}, "crystal_bow")
recipes["crystal_arrow"] = shaped(["E", "S", "F"], {"E": E, "S": stick, "F": mc("feather")}, "crystal_arrow", 4)
recipes["amethyst_shield"] = shaped(["EBE", "ERE", " E "], {"E": E, "B": B, "R": item("resonant_shard")}, "amethyst_shield")
P_, R_ = item("purified_amethyst"), item("resonant_shard")
recipes["crescent_blade"] = shaped([" PR", "PSP", "RP "], {"P": P_, "R": R_, "S": item("amethyst_sword")}, "crescent_blade")
recipes["purified_spear"] = shaped([" P ", "PSP", " R "], {"P": P_, "R": R_, "S": item("amitariel_spear")}, "purified_spear")
recipes["purity_aegis"] = shaped(["PRP", "PDP", "PPP"], {"P": P_, "R": R_, "D": mc("diamond")}, "purity_aegis")
recipes["purified_crystal_apple"] = shaped([" P ", "PAP", " P "], {"P": P_, "A": mc("apple")}, "purified_crystal_apple")
recipes["purified_crystal_bread"] = {"type": "minecraft:crafting_shapeless",
                                     "ingredients": [mc("wheat"), mc("wheat"), mc("wheat"), P_],
                                     "result": {"id": f"{NS}:purified_crystal_bread", "count": 2}}
recipes["country_potatoes"] = {"type": "minecraft:crafting_shapeless",
                               "ingredients": [mc("baked_potato"), mc("baked_potato"), mc("baked_potato"), mc("beetroot"), mc("bowl")],
                               "result": {"id": f"{NS}:country_potatoes", "count": 1}}
recipes["guide_book"] = {"type": "minecraft:crafting_shapeless", "ingredients": [mc("book"), E],
                         "result": {"id": f"{NS}:guide_book", "count": 1}}

for rid, body in recipes.items():
    write(DATA / NS / "recipe" / f"{rid}.json", body)

# ---- worldgen --------------------------------------------------------------
write(DATA / NS / "worldgen/configured_feature/infection_patch.json",
      {"type": f"{NS}:infection_patch", "config": {}})
write(DATA / NS / "worldgen/placed_feature/infection_surface.json", {
    "feature": f"{NS}:infection_patch",
    "placement": [
        {"type": "minecraft:rarity_filter", "chance": 12},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
        {"type": "minecraft:biome"}]})
write(DATA / NS / "worldgen/placed_feature/infection_underground.json", {
    "feature": f"{NS}:infection_patch",
    "placement": [
        {"type": "minecraft:count", "count": 1},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range",
         "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": -56},
                    "max_inclusive": {"absolute": 40}}},
        {"type": "minecraft:biome"}]})
print("content written")
