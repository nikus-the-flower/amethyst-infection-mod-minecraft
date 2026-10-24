"""Generates placeholder 16x16 pixel textures (replace with Blender/hand-made art later).

Run after gen_content.py. Wipes and rewrites textures/.
"""
import colorsys
import io
import random
import shutil
import zipfile
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/amethystmod/textures"
BLOCK = TEX / "block"
ITEM = TEX / "item"
shutil.rmtree(TEX, ignore_errors=True)
BLOCK.mkdir(parents=True)
ITEM.mkdir(parents=True)
ARMOR = TEX / "models/armor"
ARMOR.mkdir(parents=True)
CLIENT_JAR = Path.home() / ".gradle/caches/fabric-loom/1.21.1/minecraft-client-only.jar"

random.seed(7)


def shade(c, d):
    return tuple(max(0, min(255, v + d)) for v in c)


def noise_tile(base, spread, veins=None, vein_chance=0.0):
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            c = shade(base, random.randint(-spread, spread))
            if veins and random.random() < vein_chance:
                c = shade(random.choice(veins), random.randint(-12, 12))
            img.putpixel((x, y), c + (255,))
    return img


def add_facets(img, light, dark, count):
    """Scatter small diamond-shaped crystal facets over a tile."""
    for _ in range(count):
        cx, cy = random.randint(1, 14), random.randint(1, 14)
        r = random.choice((1, 2))
        for dx in range(-r, r + 1):
            for dy in range(-r, r + 1):
                if abs(dx) + abs(dy) <= r:
                    x, y = cx + dx, cy + dy
                    if 0 <= x < 16 and 0 <= y < 16:
                        edge = abs(dx) + abs(dy) == r and r > 1
                        img.putpixel((x, y), (dark if edge or dx > 0 else light) + (255,))
    return img


def crystal_block(base, light, border):
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            c = shade(base, 22 if ((x + y) // 4) % 2 else 0)
            c = shade(c, random.randint(-6, 6))
            if x in (0, 15) or y in (0, 15):
                c = border
            img.putpixel((x, y), c + (255,))
    for _ in range(5):
        img.putpixel((random.randint(1, 14), random.randint(1, 14)), light + (255,))
    return img


def bricks():
    img = Image.new("RGBA", (16, 16))
    mortar = (80, 45, 125)
    for x in range(16):
        for y in range(16):
            row = y // 4
            joint = 7 if row % 2 == 0 else 3
            is_mortar = y % 4 == 3 or x % 8 == joint
            c = mortar if is_mortar else shade((128, 78, 190), random.randint(-9, 9))
            img.putpixel((x, y), c + (255,))
    return img


def clear_amethyst():
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            if x in (0, 15) or y in (0, 15):
                px = (215, 175, 255, 235)
            else:
                px = (185, 125, 255, random.randint(95, 125))
            img.putpixel((x, y), px)
    for i in range(2, 6):
        img.putpixel((i, i), (245, 225, 255, 210))
    for i in range(9, 12):
        img.putpixel((i, i), (245, 225, 255, 190))
    return img


def pane_top():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in (7, 8):
            img.putpixel((x, y), (205, 160, 255, 255))
    return img


def grass_top():
    img = noise_tile((125, 82, 175), 10, [(165, 115, 225), (95, 55, 140)], 0.12)
    return img


def grass_side(dirt):
    img = dirt.copy()
    for x in range(16):
        depth = 3 + random.randint(0, 1)
        for y in range(depth):
            img.putpixel((x, y), shade((125, 82, 175), random.randint(-10, 10)) + (255,))
    return img


PALETTES = {
    "violet": ((150, 90, 210), (200, 150, 255), (95, 50, 150)),
    "rose": ((225, 115, 185), (255, 185, 225), (160, 60, 120)),
    "azure": ((90, 140, 230), (160, 205, 255), (50, 80, 160)),
}
SPIKES = {  # (x center, height, half width at base)
    "bud": [(5, 3, 1), (8, 5, 1)],
    "mid": [(6, 7, 1), (10, 6, 1), (4, 9, 2), (12, 10, 2), (8, 14, 3)],
    "large": [(2, 7, 1), (14, 8, 1), (6, 8, 1), (10, 9, 1), (4, 11, 2), (12, 12, 2), (8, 15, 3)],
}


def crystal(palette, spikes):
    body, light, dark = palette
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for cx, h, w in sorted(spikes, key=lambda s: s[1]):
        for y in range(16 - h, 16):
            t = (y - (16 - h)) / max(1, h)
            half = max(0, round(w * t))
            for x in range(cx - half, cx + half + 1):
                if 0 <= x < 16:
                    c = body
                    if x < cx:
                        c = light
                    elif x > cx:
                        c = dark
                    if x == cx - half and half:
                        c = light
                    img.putpixel((x, y), c + (255,))
    return img


def essence():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pts = [(7, 2), (8, 2), (6, 3), (7, 3), (8, 3), (9, 3)]
    for y in range(4, 11):
        half = 3 - abs(y - 7) // 2
        for x in range(8 - half - 1, 8 + half + 1):
            pts.append((x, y))
    for y in range(11, 14):
        half = 13 - y
        for x in range(8 - half, 8 + half):
            pts.append((x, y))
    for x, y in pts:
        if 0 <= x < 16 and 0 <= y < 16:
            c = (190, 130, 245) if x < 8 else (140, 80, 200)
            if (x + y) % 5 == 0:
                c = (225, 190, 255)
            img.putpixel((x, y), c + (255,))
    return img


stone = noise_tile((115, 105, 130), 12, [(170, 110, 230), (130, 70, 190)], 0.14)
deepslate = noise_tile((70, 62, 85), 9, [(160, 100, 220), (115, 60, 175)], 0.12)
dirt = noise_tile((105, 75, 110), 14, [(180, 120, 235), (140, 90, 200)], 0.10)

def log_side():
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            v = (x % 4 == 1) * -14 + (x % 4 == 3) * 8 + random.randint(-6, 6)
            c = (78 + v, 52 + v, 98 + v)
            if random.random() < 0.05:
                c = (200, 130, 255)
            img.putpixel((x, y), c + (255,))
    return img


def log_top():
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            c = (130, 82, 190) if int(d) % 2 else (100, 62, 150)
            if d > 6.5:
                c = (74, 48, 94)
            if d < 1.6:
                c = (255, 120, 235)
            img.putpixel((x, y), tuple(max(0, min(255, v + random.randint(-6, 6))) for v in c) + (255,))
    return img


log_side().save(BLOCK / "infected_log.png")
log_top().save(BLOCK / "infected_log_top.png")
stone.save(BLOCK / "infected_stone.png")
deepslate.save(BLOCK / "infected_deepslate.png")
dirt.save(BLOCK / "infected_dirt.png")
noise_tile((205, 185, 225), 8, [(175, 140, 215), (225, 205, 245)], 0.08).save(BLOCK / "infected_sand.png")
grass_top().save(BLOCK / "infected_grass_block_top.png")
grass_side(dirt).save(BLOCK / "infected_grass_block_side.png")
add_facets(stone.copy(), (210, 160, 255), (120, 70, 185), 7).save(BLOCK / "crystallized_stone.png")
add_facets(deepslate.copy(), (190, 140, 245), (110, 60, 175), 7).save(BLOCK / "crystallized_deepslate.png")
crystal_block((140, 85, 205), (235, 205, 255), (105, 60, 165)).save(BLOCK / "amethyst_crystal_block.png")
crystal_block((205, 150, 250), (255, 245, 255), (235, 200, 255)).save(BLOCK / "brilliant_amethyst.png")
bricks().save(BLOCK / "amethyst_bricks.png")
clear_amethyst().save(BLOCK / "clear_amethyst.png")
pane_top().save(BLOCK / "clear_amethyst_pane_top.png")

for cid, palette in PALETTES.items():
    crystal(palette, SPIKES["bud"]).save(BLOCK / f"crystal_bud_{cid}.png")
    crystal(palette, SPIKES["mid"]).save(BLOCK / f"crystal_{cid}.png")
    crystal(palette, SPIKES["large"]).save(BLOCK / f"crystal_large_{cid}.png")

# ---- items: recolored vanilla bases, drawn icons ---------------------------
def vanilla(path):
    with zipfile.ZipFile(CLIENT_JAR) as z:
        return Image.open(io.BytesIO(z.read(f"assets/minecraft/textures/{path}"))).convert("RGBA")


def retint(img, tint, gain=1.2, keep_colored=False):
    """Recolor grayscale-ish pixels by luminance; optionally keep saturated ones (e.g. wooden handles)."""
    out = img.copy()
    for x in range(img.width):
        for y in range(img.height):
            r, g, b, a = img.getpixel((x, y))
            if a == 0:
                continue
            if keep_colored and colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)[1] > 0.25:
                continue
            lum = (r + g + b) / 3 / 255
            out.putpixel((x, y), tuple(min(255, int(t * lum * gain)) for t in tint) + (a,))
    return out


def outline(img, color=(35, 15, 55)):
    out = img.copy()
    for x in range(16):
        for y in range(16):
            if img.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and img.getpixel((nx, ny))[3] > 0:
                        out.putpixel((x, y), color + (255,))
                        break
    return out


def canvas():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def rgba(c):
    return tuple(c) + (255,)


PURPLE_TINT, PURIFIED_TINT = (175, 115, 240), (205, 235, 250)
for tool in ("sword", "pickaxe", "axe", "shovel", "hoe"):
    retint(vanilla(f"item/iron_{tool}.png"), PURPLE_TINT, keep_colored=True).save(ITEM / f"amethyst_{tool}.png")
AMETHYST_RAMP = [(28, 12, 48), (62, 30, 108), (104, 56, 170), (146, 92, 218), (190, 140, 250), (228, 200, 255)]
PURIFIED_RAMP = [(40, 48, 70), (98, 112, 140), (156, 172, 200), (204, 216, 236), (236, 242, 252), (255, 255, 255)]
PINK, PINK_LIGHT = (255, 110, 230), (255, 196, 246)
CYAN, CYAN_LIGHT = (90, 205, 250), (200, 245, 255)
GOLD, GOLD_DARK = (236, 196, 92), (168, 120, 40)


def ramp_paint(img, ramp):
    """Repaints an icon by brightness onto a colour ramp (darkest to lightest), keeping its shape and shading."""
    out = img.copy()
    lums = [sum(img.getpixel((x, y))[:3]) / 3 for x in range(16) for y in range(16) if img.getpixel((x, y))[3]]
    lo, hi = min(lums), max(lums)
    for x in range(16):
        for y in range(16):
            r, g, b, a = img.getpixel((x, y))
            if a:
                t = ((r + g + b) / 3 - lo) / max(1, hi - lo)
                out.putpixel((x, y), ramp[min(len(ramp) - 1, round(t * (len(ramp) - 1)))] + (a,))
    return out


def put(img, points, color, inside_only=True):
    for x, y in points:
        if not inside_only or img.getpixel((x, y))[3]:
            img.putpixel((x, y), color + (255,))


ARMOR_ACCENTS = {
    # piece: (amethyst accents, purified accents) as lists of (points, colour, inside only)
    "helmet": ([([(6, 2), (7, 2), (8, 2), (9, 2)], (146, 92, 218), False), ([(7, 1), (8, 1)], (228, 200, 255), False),
                ([(7, 5), (8, 5)], PINK, True), ([(7, 4)], PINK_LIGHT, True)],
               [([(4, 6), (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6), (11, 6)], GOLD, True),
                ([(7, 5), (8, 5)], CYAN, True), ([(7, 4), (8, 4)], CYAN_LIGHT, True), ([(7, 2), (8, 2)], GOLD, False)]),
    "chestplate": ([([(2, 1), (3, 1), (13, 1), (12, 1)], (146, 92, 218), False), ([(2, 0), (13, 0)], (228, 200, 255), False),
                    ([(7, 7), (8, 7), (7, 8), (8, 8)], PINK, True), ([(7, 7)], PINK_LIGHT, True),
                    ([(7, 10), (8, 11), (7, 12)], (190, 140, 250), True)],
                   [([(4, 12), (5, 12), (6, 12), (7, 12), (8, 12), (9, 12), (10, 12), (11, 12)], GOLD, True),
                    ([(2, 3), (3, 3), (12, 3), (13, 3)], GOLD, True),
                    ([(7, 7), (8, 7), (7, 8), (8, 8)], CYAN, True), ([(7, 7)], CYAN_LIGHT, True)]),
    "leggings": ([([(4, 3), (5, 3), (6, 3), (9, 3), (10, 3), (11, 3)], (62, 30, 108), True), ([(7, 3), (8, 3)], PINK, True),
                  ([(4, 7), (11, 7)], PINK_LIGHT, True)],
                 [([(4, 3), (5, 3), (6, 3), (9, 3), (10, 3), (11, 3)], GOLD, True), ([(7, 3), (8, 3)], CYAN, True),
                  ([(4, 12), (5, 12), (10, 12), (11, 12)], GOLD_DARK, True)]),
    "boots": ([([(2, 10), (12, 10)], PINK, True), ([(4, 4), (10, 4)], PINK_LIGHT, True), ([(1, 9), (14, 9)], (146, 92, 218), False)],
              [([(2, 11), (3, 11), (4, 11), (11, 11), (12, 11), (13, 11)], GOLD, True), ([(2, 10), (12, 10)], CYAN, True),
               ([(4, 4), (10, 4)], CYAN_LIGHT, True)]),
}
for piece in ("helmet", "chestplate", "leggings", "boots"):
    base = vanilla(f"item/iron_{piece}.png")
    for name, ramp, accents in (("amethyst", AMETHYST_RAMP, ARMOR_ACCENTS[piece][0]),
                                ("purified", PURIFIED_RAMP, ARMOR_ACCENTS[piece][1])):
        icon = ramp_paint(base, ramp)
        for points, color, inside in accents:
            put(icon, points, color, inside)
        icon.save(ITEM / f"{name}_{piece}.png")


# ---- food ----------------------------------------------------------------------------------------
def crystal_apple():
    img = ramp_paint(vanilla("item/golden_apple.png"),
                     [(40, 60, 110), (90, 130, 190), (150, 200, 235), (205, 235, 250), (245, 252, 255)])
    for x, y in ((8, 2), (9, 1), (8, 3), (9, 2)):
        if img.getpixel((x, y))[3]:
            img.putpixel((x, y), (110, 70, 40, 255))
    put(img, [(6, 8), (9, 10), (7, 11), (10, 7)], (180, 140, 240))
    put(img, [(5, 6), (6, 5)], (255, 255, 255))
    put(img, [(10, 3), (11, 3), (11, 4)], (120, 230, 220), inside_only=False)
    return img


def crystal_bread():
    img = vanilla("item/bread.png")
    put(img, [(8, 4), (11, 3), (6, 6), (10, 6), (5, 8), (12, 5), (9, 2)], CYAN)
    put(img, [(8, 3), (11, 2), (6, 5)], CYAN_LIGHT)
    return img


def bowl_with(content):
    """Fills the stew of a vanilla mushroom stew with something else: content(x, y) gives the colour."""
    img = vanilla("item/mushroom_stew.png")
    for x in range(16):
        for y in range(16):
            r, g, b, a = img.getpixel((x, y))
            if a and 5 <= y <= 8 and (r + g + b) / 3 >= 100:
                img.putpixel((x, y), content(x, y) + (255,))
    return img


def cereal():
    flakes = {(4, 7): (232, 170, 60), (6, 6): (250, 210, 90), (8, 7): (214, 110, 60), (10, 6): (255, 140, 200),
              (5, 7): (250, 210, 90), (9, 6): (232, 170, 60), (11, 7): (250, 210, 90), (7, 6): (255, 140, 200)}
    img = bowl_with(lambda x, y: flakes.get((x, y), (240, 238, 230) if y < 8 else (220, 216, 206)))
    # a few flakes heaped over the rim
    for (x, y), c in {(6, 5): (250, 210, 90), (7, 4): (232, 170, 60), (8, 5): (255, 140, 200), (9, 5): (214, 110, 60)}.items():
        img.putpixel((x, y), c + (255,))
    return outline(img, (60, 40, 30))


def potatoes():
    wedge = {(4, 7): (226, 170, 72), (5, 6): (240, 196, 96), (6, 7): (196, 132, 44), (7, 6): (226, 170, 72),
             (8, 7): (240, 196, 96), (9, 6): (196, 132, 44), (10, 7): (226, 170, 72), (11, 6): (240, 196, 96),
             (6, 6): (190, 40, 30), (9, 7): (190, 40, 30), (8, 6): (90, 160, 60)}
    img = bowl_with(lambda x, y: wedge.get((x, y), (210, 150, 60)))
    for (x, y), c in {(5, 5): (240, 196, 96), (6, 4): (226, 170, 72), (7, 5): (196, 132, 44), (8, 4): (240, 196, 96),
                      (9, 5): (226, 170, 72), (10, 5): (190, 40, 30), (7, 4): (90, 160, 60)}.items():
        img.putpixel((x, y), c + (255,))
    return outline(img, (60, 40, 30))


def aegis():
    img, d = canvas()
    d.ellipse([2, 1, 13, 14], fill=rgba((150, 190, 230)))
    d.ellipse([3, 2, 12, 13], fill=rgba((205, 230, 250)))
    d.ellipse([2, 1, 13, 14], outline=rgba(GOLD))
    d.polygon([(8, 4), (11, 8), (8, 12), (5, 8)], fill=rgba(CYAN))
    d.polygon([(8, 4), (5, 8), (8, 8)], fill=rgba(CYAN_LIGHT))
    img.putpixel((7, 6), (255, 255, 255, 255))
    for x, y in ((8, 1), (8, 14), (2, 8), (13, 8)):
        img.putpixel((x, y), GOLD_DARK + (255,))
    return outline(img)


PAPER, PAPER_DARK, INK = (232, 216, 176), (184, 160, 112), (90, 64, 40)


def scroll():
    img, d = canvas()
    d.polygon([(3, 5), (12, 2), (13, 11), (4, 14)], fill=rgba(PAPER))
    d.line([(3, 5), (4, 14)], fill=rgba(PAPER_DARK))
    d.ellipse([1, 3, 5, 7], fill=rgba(PAPER_DARK))
    d.ellipse([11, 9, 15, 13], fill=rgba(PAPER_DARK))
    for i in range(3):
        d.line([(6, 6 + i * 2), (11, 4 + i * 2)], fill=rgba(INK))
    img.putpixel((8, 12), (180, 40, 40, 255))
    return outline(img, (70, 50, 30))


def parchment():
    img, d = canvas()
    d.rectangle([3, 1, 12, 14], fill=rgba(PAPER))
    for y in (1, 14):
        for x in range(3, 13, 3):
            img.putpixel((x, y), (0, 0, 0, 0))
    d.rectangle([3, 1, 4, 14], fill=rgba(PAPER_DARK))
    for i in range(5):
        d.line([(5, 3 + i * 2), (11 - (i % 2) * 2, 3 + i * 2)], fill=rgba(INK))
    img.putpixel((10, 12), (160, 100, 200, 255))
    return outline(img, (70, 50, 30))


def old_book(cover, band):
    img, d = canvas()
    d.rectangle([3, 2, 12, 14], fill=rgba(cover))
    d.rectangle([3, 2, 4, 14], fill=rgba(tuple(int(c * 0.7) for c in cover)))
    d.rectangle([12, 3, 13, 13], fill=rgba(PAPER))
    d.line([(5, 4), (11, 4)], fill=rgba(band))
    d.line([(5, 12), (11, 12)], fill=rgba(band))
    d.rectangle([7, 7, 9, 9], fill=rgba(band))
    img.putpixel((8, 8), (190, 120, 255, 255))
    return outline(img, (40, 25, 15))


def torn_page():
    img, d = canvas()
    d.polygon([(3, 2), (12, 1), (13, 7), (11, 9), (13, 12), (10, 15), (3, 14)], fill=rgba(PAPER))
    for i in range(4):
        d.line([(5, 4 + i * 3), (10, 4 + i * 3)], fill=rgba(INK))
    d.line([(3, 2), (3, 14)], fill=rgba(PAPER_DARK))
    return outline(img, (70, 50, 30))


def letter():
    img, d = canvas()
    d.rectangle([1, 4, 14, 12], fill=rgba(PAPER))
    d.line([(1, 4), (8, 9), (14, 4)], fill=rgba(PAPER_DARK))
    d.line([(1, 12), (6, 8)], fill=rgba(PAPER_DARK))
    d.line([(14, 12), (10, 8)], fill=rgba(PAPER_DARK))
    d.ellipse([6, 7, 10, 11], fill=rgba((170, 30, 50)))
    img.putpixel((8, 9), (230, 90, 110, 255))
    return outline(img, (70, 50, 30))


scroll().save(ITEM / "scroll.png")
parchment().save(ITEM / "ancient_parchment.png")
old_book((90, 50, 30), (210, 170, 70)).save(ITEM / "ancient_book.png")
old_book((70, 90, 60), (190, 150, 90)).save(ITEM / "diary.png")
torn_page().save(ITEM / "torn_page.png")
letter().save(ITEM / "letter.png")
crystal_apple().save(ITEM / "purified_crystal_apple.png")
crystal_bread().save(ITEM / "purified_crystal_bread.png")
cereal().save(ITEM / "cereal_with_milk.png")
potatoes().save(ITEM / "country_potatoes.png")
aegis().save(ITEM / "purity_aegis.png")

# ---- the white hearts of the aegis (9x9, from the vanilla heart) ----------------------------------
GUI = TEX / "gui"
GUI.mkdir(parents=True, exist_ok=True)
for src, name, ramp in (("full", "purity_heart_full", [(195, 215, 235), (238, 245, 255), (255, 255, 255)]),
                        ("half", "purity_heart_half", [(195, 215, 235), (238, 245, 255), (255, 255, 255)]),
                        ("container", "purity_heart_container", [(20, 30, 50), (60, 80, 110), (110, 140, 170)])):
    heart = vanilla(f"gui/sprites/hud/heart/{src}.png")
    out = heart.copy()
    lums = [sum(heart.getpixel((x, y))[:3]) / 3 for x in range(heart.width) for y in range(heart.height) if heart.getpixel((x, y))[3]]
    lo, hi = min(lums), max(lums)
    for x in range(heart.width):
        for y in range(heart.height):
            r, g, b, a = heart.getpixel((x, y))
            if a:
                t = ((r + g + b) / 3 - lo) / max(1, hi - lo)
                out.putpixel((x, y), ramp[min(2, round(t * 2))] + (a,))
    out.save(GUI / f"{name}.png")
for layer in (1, 2):
    base = vanilla(f"models/armor/iron_layer_{layer}.png")
    retint(base, PURPLE_TINT).save(ARMOR / f"amethyst_layer_{layer}.png")
    retint(base, PURIFIED_TINT, gain=1.3).save(ARMOR / f"purified_layer_{layer}.png")

ess = essence()
retint(ess, (255, 150, 45), gain=1.35).save(ITEM / "flame_crystal.png")
retint(ess, (120, 215, 255), gain=1.35).save(ITEM / "frost_crystal.png")
retint(ess, (235, 245, 235), gain=1.3).save(ITEM / "purified_amethyst.png")


def resonant_shard():
    # a slim crystal shard ringed by two bands of light
    img, d = canvas()
    d.polygon([(9, 1), (12, 5), (10, 14), (6, 13), (5, 6)], fill=rgba((150, 70, 225)))
    d.polygon([(9, 1), (5, 6), (6, 13), (8, 8)], fill=rgba((215, 160, 255)))
    d.line([(9, 1), (10, 14)], fill=rgba((90, 30, 150)))
    d.line([(3, 6), (13, 9)], fill=rgba((255, 225, 255)))
    d.line([(3, 10), (13, 7)], fill=rgba((255, 150, 235)))
    d.point([(8, 4), (7, 5)], fill=(255, 255, 255, 255))
    return outline(img)


resonant_shard().save(ITEM / "resonant_shard.png")


def staff(gem_light, gem_dark, handle=(110, 75, 45)):
    img, d = canvas()
    d.line([(2, 14), (10, 6)], fill=rgba(handle))
    d.line([(3, 14), (10, 7)], fill=rgba(shade(handle, -30)))
    d.polygon([(12, 1), (14, 3), (12, 6), (10, 3)], fill=rgba(gem_dark))
    d.polygon([(12, 1), (10, 3), (12, 4)], fill=rgba(gem_light))
    img.putpixel((12, 3), (255, 255, 255, 255))
    d.point([(9, 6), (10, 7)], fill=rgba((200, 170, 90)))
    return outline(img)


def hammer():
    img, d = canvas()
    d.line([(2, 14), (9, 7)], fill=rgba((110, 75, 45)))
    d.line([(3, 14), (9, 8)], fill=rgba((80, 55, 30)))
    d.polygon([(7, 2), (14, 4), (13, 9), (6, 7)], fill=rgba((140, 85, 205)))
    d.polygon([(7, 2), (14, 4), (13, 5), (7, 4)], fill=rgba((205, 155, 255)))
    d.line([(6, 7), (13, 9)], fill=rgba((95, 50, 150)))
    return outline(img)


def heart():
    img, d = canvas()
    d.polygon([(8, 14), (2, 8), (2, 5), (4, 3), (6, 3), (8, 5), (10, 3), (12, 3), (14, 5), (14, 8)], fill=rgba((170, 90, 235)))
    d.polygon([(8, 12), (4, 8), (4, 5), (5, 4), (6, 4), (8, 6)], fill=rgba((225, 170, 255)))
    d.point([(5, 5), (4, 6)], fill=(255, 255, 255, 255))
    d.line([(8, 13), (13, 8)], fill=rgba((120, 60, 180)))
    return outline(img)


def wings():
    img, d = canvas()
    for sign in (-1, 1):
        cx = 8 + sign
        pts = [(cx, 9), (cx + sign * 6, 2), (cx + sign * 7, 6), (cx + sign * 5, 8), (cx + sign * 6, 11), (cx + sign * 3, 11), (cx, 12)]
        d.polygon(pts, fill=rgba((160, 100, 230)))
        d.line([(cx, 9), (cx + sign * 6, 2)], fill=rgba((235, 210, 255)))
        d.line([(cx, 10), (cx + sign * 6, 6)], fill=rgba((215, 175, 255)))
    d.rectangle([7, 8, 8, 12], fill=rgba((235, 210, 255)))
    return outline(img)


def ring():
    img, d = canvas()
    d.ellipse([3, 5, 12, 14], outline=rgba((235, 195, 70)))
    d.ellipse([4, 6, 11, 13], outline=rgba((190, 145, 40)))
    d.polygon([(7, 1), (10, 3), (7, 6), (5, 3)], fill=rgba((185, 120, 255)))
    d.polygon([(7, 1), (5, 3), (7, 4)], fill=rgba((235, 205, 255)))
    return outline(img)


def vampire_shard():
    img, d = canvas()
    d.polygon([(8, 1), (11, 6), (9, 14), (6, 13), (5, 6)], fill=rgba((165, 40, 95)))
    d.polygon([(8, 1), (5, 6), (6, 13), (8, 9)], fill=rgba((225, 90, 150)))
    d.line([(8, 1), (9, 14)], fill=rgba((95, 20, 85)))
    d.point([(7, 4), (7, 5)], fill=(255, 220, 235, 255))
    return outline(img)


def orb(light, base, dark, rim=None):
    img, d = canvas()
    d.ellipse([2, 2, 13, 13], fill=rgba(base))
    d.ellipse([2, 5, 13, 13], fill=rgba(dark))
    d.ellipse([2, 2, 12, 11], fill=rgba(base))
    d.ellipse([4, 3, 7, 6], fill=rgba(light))
    img.putpixel((5, 4), (255, 255, 255, 255))
    if rim:
        d.ellipse([1, 6, 14, 10], outline=rgba(rim))
    return outline(img)


def berserker():
    img = orb((255, 200, 90), (230, 90, 35), (140, 30, 20))
    d = ImageDraw.Draw(img)
    d.line([(5, 8), (7, 9)], fill=rgba((60, 10, 10)))
    d.line([(10, 8), (8, 9)], fill=rgba((60, 10, 10)))
    d.point([(8, 1), (6, 0), (10, 1)], fill=rgba((255, 170, 40)))
    return img


def stasis():
    img, d = canvas()
    d.polygon([(8, 1), (13, 5), (13, 11), (8, 15), (3, 11), (3, 5)], fill=rgba((90, 190, 245)))
    d.polygon([(8, 1), (3, 5), (3, 11), (8, 8)], fill=rgba((175, 235, 255)))
    d.polygon([(8, 8), (13, 5), (13, 11), (8, 15)], fill=rgba((50, 120, 200)))
    d.line([(8, 3), (8, 13)], fill=rgba((235, 250, 255)))
    d.line([(5, 8), (11, 8)], fill=rgba((235, 250, 255)))
    return outline(img)


def totem():
    img, d = canvas()
    d.ellipse([5, 1, 10, 6], fill=rgba((235, 195, 70)))
    d.rectangle([4, 7, 11, 12], fill=rgba((235, 195, 70)))
    d.rectangle([6, 13, 9, 14], fill=rgba((190, 145, 40)))
    d.polygon([(8, 2), (9, 4), (8, 6), (7, 4)], fill=rgba((185, 120, 255)))
    d.polygon([(8, 8), (10, 10), (8, 12), (6, 10)], fill=rgba((185, 120, 255)))
    d.point([(6, 3), (5, 8)], fill=(255, 245, 190, 255))
    return outline(img)


def thunder():
    img, d = canvas()
    d.polygon([(8, 1), (13, 6), (11, 14), (5, 14), (3, 6)], fill=rgba((150, 90, 215)))
    d.polygon([(8, 1), (3, 6), (5, 14), (8, 8)], fill=rgba((205, 155, 255)))
    d.polygon([(9, 2), (5, 9), (8, 9), (6, 14), (11, 7), (8, 7)], fill=rgba((255, 230, 70)))
    d.line([(9, 2), (8, 7)], fill=rgba((255, 250, 190)))
    return outline(img)


staff((255, 215, 120), (240, 90, 20)).save(ITEM / "flame_staff.png")
staff((215, 245, 255), (70, 170, 235), handle=(150, 170, 190)).save(ITEM / "frost_staff.png")
hammer().save(ITEM / "crystal_hammer.png")
sc, sd = canvas()
sd.line([(2, 14), (9, 7)], fill=rgba((235, 225, 200)))
sd.line([(3, 14), (9, 8)], fill=rgba((190, 175, 150)))
sd.polygon([(11, 0), (15, 3), (11, 7), (8, 4)], fill=rgba((235, 130, 215)))
sd.polygon([(11, 0), (8, 4), (11, 4)], fill=rgba((255, 225, 250)))
sd.polygon([(11, 4), (15, 3), (11, 7)], fill=rgba((165, 85, 215)))
outline(sc).save(ITEM / "prism_scepter.png")

heart().save(ITEM / "aegis_heart.png")
wings().save(ITEM / "amethyst_wings.png")
ring().save(ITEM / "blink_ring.png")
vampire_shard().save(ITEM / "vampiric_shard.png")
berserker().save(ITEM / "berserker_core.png")
stasis().save(ITEM / "stasis_crystal.png")
orb((200, 170, 255), (75, 35, 130), (35, 15, 70), rim=(200, 150, 255)).save(ITEM / "gravity_orb.png")
totem().save(ITEM / "crystal_totem.png")
thunder().save(ITEM / "thunder_shard.png")


def amulet():
    # a silver chain holding a pale, clean crystal
    img, d = canvas()
    d.arc([2, 0, 13, 9], 20, 160, fill=rgba((200, 205, 215)))
    d.arc([3, 0, 12, 8], 20, 160, fill=rgba((150, 155, 170)))
    d.polygon([(8, 7), (12, 10), (8, 15), (4, 10)], fill=rgba((150, 225, 245)))
    d.polygon([(8, 7), (4, 10), (8, 12)], fill=rgba((230, 250, 255)))
    d.polygon([(8, 12), (12, 10), (8, 15)], fill=rgba((90, 160, 205)))
    d.point([(7, 9)], fill=(255, 255, 255, 255))
    return outline(img)


amulet().save(ITEM / "purity_amulet.png")

ess.save(ITEM / "amethyst_essence.png")


def shard_icon():
    img, d = canvas()
    d.polygon([(3, 13), (6, 6), (11, 1), (13, 3), (9, 9), (5, 14)], fill=rgba((170, 105, 240)))
    d.polygon([(11, 1), (13, 3), (9, 9), (8, 6)], fill=rgba((235, 200, 255)))
    d.line([(4, 13), (11, 2)], fill=rgba((110, 55, 185)))
    return outline(img)


shard_icon().save(ITEM / "crystal_shard.png")

for bow_name in ("bow", "bow_pulling_0", "bow_pulling_1", "bow_pulling_2"):
    retint(vanilla(f"item/{bow_name}.png"), PURPLE_TINT, keep_colored=True).save(ITEM / f"crystal_{bow_name}.png")
retint(vanilla("item/arrow.png"), (190, 120, 255), gain=1.3, keep_colored=True).save(ITEM / "crystal_arrow.png")
PROJ = TEX / "entity/projectiles"
PROJ.mkdir(parents=True, exist_ok=True)
retint(vanilla("entity/projectiles/arrow.png"), (190, 120, 255), gain=1.3, keep_colored=True).save(PROJ / "crystal_arrow.png")


def guide_book_icon():
    img, d = canvas()
    d.rectangle([3, 1, 12, 14], fill=rgba((112, 56, 182)))
    d.rectangle([3, 1, 5, 14], fill=rgba((68, 30, 118)))
    d.rectangle([12, 2, 13, 13], fill=rgba((238, 228, 246)))
    d.line([(6, 2), (11, 2)], fill=rgba((160, 100, 230)))
    d.polygon([(8, 5), (10, 7), (8, 10), (6, 7)], fill=rgba((255, 110, 230)))
    d.point([(7, 6)], fill=(255, 255, 255, 255))
    d.rectangle([3, 13, 12, 14], fill=rgba((236, 196, 92)))
    return outline(img)


guide_book_icon().save(ITEM / "guide_book.png")
orb((255, 215, 255), (175, 65, 235), (80, 25, 130), rim=(255, 150, 240)).save(ITEM / "portal_core.png")
retint(heart(), (255, 110, 215), gain=1.25).save(ITEM / "heart_core.png")

# ---- particles and effect icons --------------------------------------------
PART = TEX / "particle"
EFFECT = TEX / "mob_effect"
PART.mkdir(parents=True)
EFFECT.mkdir(parents=True)


def particle_frames(name, color, core=(255, 255, 255)):
    for i in range(8):
        img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
        fade = 1.0 - i / 9
        for x in range(8):
            for y in range(8):
                d = abs(x - 3.5) + abs(y - 3.5)
                if (abs(x - 3.5) < 0.6 or abs(y - 3.5) < 0.6) and d < 4.2 - i * 0.35:
                    c = core if d < 1.6 else color
                    img.putpixel((x, y), c + (int(255 * fade),))
                elif d < 3.0 - i * 0.3:
                    img.putpixel((x, y), color + (int(150 * fade),))
        img.save(PART / f"{name}_{i}.png")


particle_frames("crystal_spark", (190, 120, 255))
particle_frames("ward_mote", (150, 225, 255), core=(240, 255, 255))
particle_frames("blight_spore", (170, 40, 190), core=(255, 160, 255))
particle_frames("prism_flare", (255, 90, 200), core=(255, 235, 250))


def effect_icon(name, draw_fn):
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    draw_fn(d)
    img = outline18(img)
    img.save(EFFECT / f"{name}.png")


def outline18(img):
    out = img.copy()
    for x in range(18):
        for y in range(18):
            if img.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 18 and 0 <= ny < 18 and img.getpixel((nx, ny))[3] > 0:
                        out.putpixel((x, y), (30, 10, 50, 255))
                        break
    return out


def resonance(d):
    d.polygon([(9, 2), (14, 8), (9, 16), (4, 8)], fill=(170, 105, 240, 255))
    d.polygon([(9, 2), (4, 8), (9, 9)], fill=(225, 185, 255, 255))
    d.line([(9, 2), (9, 16)], fill=(110, 55, 180, 255))
    d.point([(7, 6)], fill=(255, 255, 255, 255))


def ward(d):
    d.polygon([(9, 2), (15, 4), (15, 9), (9, 16), (3, 9), (3, 4)], fill=(190, 235, 250, 255))
    d.polygon([(9, 2), (3, 4), (3, 9), (9, 9)], fill=(240, 252, 255, 255))
    d.polygon([(9, 9), (15, 9), (9, 16)], fill=(120, 190, 225, 255))
    d.line([(9, 5), (9, 12)], fill=(150, 90, 215, 255))
    d.line([(6, 8), (12, 8)], fill=(150, 90, 215, 255))


def blight(d):
    d.polygon([(9, 2), (14, 7), (12, 15), (6, 15), (4, 7)], fill=(120, 40, 175, 255))
    d.polygon([(9, 2), (4, 7), (6, 15), (9, 9)], fill=(190, 90, 235, 255))
    d.point([(8, 8), (11, 10), (7, 12)], fill=(35, 5, 55, 255))
    d.line([(10, 4), (11, 13)], fill=(70, 15, 110, 255))


def infected_leaves():
    """Oak leaves turned to violet crystal, with a few glints."""
    img = retint(vanilla("block/oak_leaves.png"), (160, 100, 225), gain=1.35)
    rng = random.Random(77)
    for _ in range(7):
        x, y = rng.randrange(16), rng.randrange(16)
        if img.getpixel((x, y))[3]:
            img.putpixel((x, y), (235, 205, 255, 255))
    return img


def crystal_geode():
    """A geode: a rough grey-brown stone shell, cracked open on one side to show violet amethyst crystals."""
    img, d = canvas()
    d.ellipse([1, 2, 14, 14], fill=(110, 96, 100, 255))
    d.ellipse([2, 3, 13, 13], fill=(138, 122, 124, 255))
    d.point([(3, 4), (4, 3), (3, 5)], fill=(168, 152, 150, 255))
    d.point([(12, 11), (11, 12), (13, 10)], fill=(84, 72, 78, 255))
    d.ellipse([4, 5, 12, 12], fill=(226, 205, 245, 255))
    d.ellipse([5, 6, 11, 11], fill=(120, 62, 190, 255))
    d.polygon([(8, 5), (6, 8), (8, 9)], fill=(190, 140, 250, 255))
    d.polygon([(8, 5), (10, 8), (8, 9)], fill=(150, 90, 225, 255))
    d.polygon([(6, 10), (5, 7), (7, 8)], fill=(205, 160, 255, 255))
    d.polygon([(10, 10), (11, 7), (9, 8)], fill=(100, 50, 165, 255))
    d.line([(8, 9), (8, 11)], fill=(85, 40, 140, 255))
    d.point([(7, 7), (6, 8)], fill=(252, 240, 255, 255))
    d.point([(8, 13)], fill=(70, 60, 66, 255))
    return outline(img)


infected_leaves().save(BLOCK / "infected_leaves.png")
crystal_geode().save(ITEM / "crystal_geode.png")

effect_icon("crystal_resonance", resonance)
effect_icon("anti_amethyst_ward", ward)
effect_icon("amethyst_blight", blight)
print("textures written")