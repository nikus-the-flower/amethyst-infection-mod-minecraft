"""True 3D item models (vanilla JSON "elements") for swords, staffs, hammers and the shield.

Each model is a handful of axis-aligned boxes in "upright" space (blade up, 16 units = 1 block),
turned 45 degrees about Z so it sits in the hand like a normal sword. Colours live in a tiny
palette texture. Run after gen_content.py and gen_textures.py (it overwrites their flat models).
"""
import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/amethystmod"
NS = "amethystmod"

FACES = ("north", "south", "east", "west", "up", "down")

# Hand/GUI transforms (vanilla item/handheld and item/generated values), set directly on each model.
HANDHELD = {
    "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
    "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.82, 0.82, 0.82]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
}
SHIELD = {
    "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 1, 0.5], "scale": [0.85, 0.85, 0.85]},
    "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [0, 1, 0.5], "scale": [0.85, 0.85, 0.85]},
    "firstperson_righthand": {"rotation": [0, -25, 0], "translation": [2, -2, -1], "scale": [0.75, 0.75, 0.75]},
    "firstperson_lefthand": {"rotation": [0, -25, 0], "translation": [2, -2, -1], "scale": [0.75, 0.75, 0.75]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "gui": {"rotation": [15, -25, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}
OBJECT = {
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "gui": {"rotation": [25, -35, 0], "translation": [0, 0, 0], "scale": [0.95, 0.95, 0.95]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}
DISPLAYS = {"minecraft:item/handheld": HANDHELD, "minecraft:item/generated": SHIELD, "object": OBJECT}


class Item3D:
    def __init__(self, name, palette, rotate=-45, parent="minecraft:item/handheld"):
        self.name, self.palette, self.rotate, self.parent = name, palette, rotate, parent
        self.keys = list(palette)
        self.boxes = []

    def box(self, x0, y0, z0, x1, y1, z1, color):
        self.boxes.append((x0, y0, z0, x1, y1, z1, color))

    def write(self, extra=None):
        # every colour gets three tones: light (tops, edges catching light), base (faces), dark (undersides)
        tones = {}
        tex = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        i = 0
        for key in self.keys:
            r, g, b = self.palette[key]
            for tone, f, add in (("light", 1.18, 22), ("base", 1.0, 0), ("dark", 0.68, 0)):
                tex.putpixel((i % 16, i // 16), (min(255, int(r * f) + add), min(255, int(g * f) + add), min(255, int(b * f) + add), 255))
                tones[(key, tone)] = i
                i += 1
        tex.save(ASSETS / "textures/item" / f"p_{self.name}.png")
        face_tone = {"up": "light", "north": "base", "south": "base", "east": "light", "west": "dark", "down": "dark"}
        elements = []
        for x0, y0, z0, x1, y1, z1, color in self.boxes:
            faces = {}
            for f in FACES:
                j = tones[(color, face_tone[f])]
                u, v = j % 16 + 0.25, j // 16 + 0.25
                faces[f] = {"uv": [u, v, u + 0.5, v + 0.5], "texture": "#p"}
            element = {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces}
            if self.rotate:
                element["rotation"] = {"origin": [8, 8, 8], "axis": "z", "angle": self.rotate}
            elements.append(element)
        model = {"gui_light": "side" if self.parent == "object" else "front", "display": DISPLAYS[self.parent],
                 "textures": {"p": f"{NS}:item/p_{self.name}", "particle": f"{NS}:item/p_{self.name}"},
                 "elements": elements}
        if extra:
            model.update(extra)
        (ASSETS / "models/item" / f"{self.name}.json").write_text(json.dumps(model, indent=1), encoding="utf-8", newline="\n")


def sword(name, blade, edge, guard, grip, gem, extras=None, wide_guard=False):
    m = Item3D(name, {"blade": blade, "edge": edge, "guard": guard, "grip": grip, "gem": gem,
                      "dark": (40, 28, 56), "glow": gem})
    m.box(6.8, -1, 6.8, 9.2, 1, 9.2, "guard")
    m.box(7.3, 1, 7.3, 8.7, 5, 8.7, "grip")
    m.box(7.1, 2, 7.1, 8.9, 2.6, 8.9, "dark")
    m.box(7.1, 3.8, 7.1, 8.9, 4.4, 8.9, "dark")
    g = 5 if wide_guard else 4
    m.box(8 - g, 5, 6.4, 8 + g, 6.6, 9.6, "guard")
    m.box(8 - g - 1, 5.6, 6.9, 8 - g, 7.6, 9.1, "guard")
    m.box(8 + g, 5.6, 6.9, 8 + g + 1, 7.6, 9.1, "guard")
    m.box(7.3, 5.1, 6.0, 8.7, 6.5, 6.5, "gem")
    m.box(7.3, 5.1, 9.5, 8.7, 6.5, 10.0, "gem")
    m.box(6.8, 6.6, 7.2, 9.2, 15, 8.8, "blade")
    m.box(7.6, 6.6, 6.9, 8.4, 15, 9.1, "edge")
    m.box(7.1, 15, 7.4, 8.9, 18, 8.6, "blade")
    m.box(7.6, 15, 7.2, 8.4, 18, 8.8, "edge")
    m.box(7.5, 18, 7.6, 8.5, 19.6, 8.4, "edge")
    m.box(7.8, 19.6, 7.8, 8.2, 20.8, 8.2, "edge")
    m.box(7.75, 7.5, 6.8, 8.25, 14.5, 9.2, "dark")       # fuller groove
    m.box(7.4, -1.7, 7.4, 8.6, -0.9, 8.6, "gem")        # pommel gem
    m.box(8 - g - 0.6, 5.3, 6.2, 8 - g + 0.6, 6.3, 6.5, "gem")
    m.box(8 + g - 0.6, 5.3, 6.2, 8 + g + 0.6, 6.3, 6.5, "gem")
    m.box(8 - g - 1.6, 6.0, 7.4, 8 - g - 1.0, 8.4, 8.6, "edge")
    m.box(8 + g + 1.0, 6.0, 7.4, 8 + g + 1.6, 8.4, 8.6, "edge")
    m.box(7.0, -1.9, 7.0, 9.0, -1.3, 9.0, "guard")
    m.box(7.75, 15, 6.75, 8.25, 17.5, 9.25, "edge")
    for e in extras or []:
        m.box(*e[1:], e[0])
    m.write()


def staff(name, shaft, band, head_a, head_b, glow, extras):
    m = Item3D(name, {"shaft": shaft, "band": band, "a": head_a, "b": head_b, "glow": glow, "dark": (36, 26, 50)})
    m.box(7.3, -2, 7.3, 8.7, 17, 8.7, "shaft")
    for y in (0, 4, 8, 12):
        m.box(7.0, y, 7.0, 9.0, y + 0.8, 9.0, "band")
    m.box(6.6, 16.5, 6.6, 9.4, 18, 9.4, "band")
    m.box(5.6, 20.5, 7.7, 10.4, 21.0, 8.3, "band")     # floating ring around the head
    m.box(7.7, 20.5, 5.6, 8.3, 21.0, 10.4, "band")
    m.box(7.5, -2.8, 7.5, 8.5, -2, 8.5, "band")
    m.box(6.2, 19.0, 7.8, 9.8, 19.4, 8.2, "band")
    m.box(7.8, 19.0, 6.2, 8.2, 19.4, 9.8, "band")
    for e in extras:
        m.box(*e[1:], e[0])
    m.write()


def hammer(name, shaft, head, cap, accent, glow, extras):
    m = Item3D(name, {"shaft": shaft, "head": head, "cap": cap, "accent": accent, "glow": glow, "dark": (36, 26, 50)})
    m.box(7.3, -2, 7.3, 8.7, 15, 8.7, "shaft")
    m.box(7.0, 1.5, 7.0, 9.0, 2.3, 9.0, "accent")
    m.box(7.0, 5.5, 7.0, 9.0, 6.3, 9.0, "accent")
    m.box(6.6, -2.8, 6.6, 9.4, -1.6, 9.4, "accent")
    m.box(3, 14, 5, 13, 20, 11, "head")
    m.box(1.8, 14.6, 5.6, 3, 19.4, 10.4, "cap")
    m.box(13, 14.6, 5.6, 14.2, 19.4, 10.4, "cap")
    m.box(3, 14, 4.6, 13, 20, 5, "accent")
    m.box(3, 14, 11, 13, 20, 11.4, "accent")
    for x in (4, 12):
        for y in (15, 19):
            m.box(x - 0.4, y - 0.4, 4.4, x + 0.4, y + 0.4, 4.7, "cap")
            m.box(x - 0.4, y - 0.4, 11.3, x + 0.4, y + 0.4, 11.6, "cap")
    m.box(3, 19.6, 5, 13, 20.4, 11, "cap")
    for x in (5, 8, 11):
        m.box(x - 0.8, 20.4, 7.2, x + 0.8, 21.8 + (0.8 if x == 8 else 0), 8.8, "glow")
        m.box(x - 0.4, 21.8, 7.6, x + 0.4, 22.8 + (0.8 if x == 8 else 0), 8.4, "accent")
    for e in extras:
        m.box(*e[1:], e[0])
    m.write()


def shield():
    pal = {"face": (112, 66, 178), "rim": (46, 26, 80), "light": (196, 142, 250), "gem": (255, 110, 230), "dark": (30, 18, 52)}
    m = Item3D("amethyst_shield", pal, rotate=0, parent="minecraft:item/generated")
    m.box(2.5, 5, 7, 13.5, 15, 9, "face")
    m.box(3.5, 2, 7, 12.5, 5, 9, "face")
    m.box(5, 0.5, 7, 11, 2, 9, "face")
    m.box(2.2, 4.5, 6.6, 3.2, 15.4, 9.4, "rim")
    m.box(12.8, 4.5, 6.6, 13.8, 15.4, 9.4, "rim")
    m.box(2.2, 14.8, 6.6, 13.8, 15.8, 9.4, "rim")
    m.box(3.2, 1.8, 6.6, 4.2, 4.8, 9.4, "rim")
    m.box(11.8, 1.8, 6.6, 12.8, 4.8, 9.4, "rim")
    m.box(4.8, 0.2, 6.6, 11.2, 1.0, 9.4, "rim")
    m.box(6.5, 6.5, 5.6, 9.5, 12.5, 7, "light")
    m.box(7.2, 7.5, 5.2, 8.8, 11.5, 5.8, "gem")
    m.box(5.0, 8.0, 5.9, 6.5, 11.0, 7, "light")
    m.box(9.5, 8.0, 5.9, 11.0, 11.0, 7, "light")
    m.box(4.0, 12.5, 6.1, 5.5, 14.2, 7, "light")
    m.box(10.5, 12.5, 6.1, 12.0, 14.2, 7, "light")
    m.box(7.4, 9.0, 9, 8.6, 11.0, 9.4, "gem")
    # the back: a grip and two straps (what you see in first person)
    m.box(4.0, 7.0, 9.4, 12.0, 8.0, 9.9, "rim")
    m.box(4.0, 11.5, 9.4, 12.0, 12.5, 9.9, "rim")
    m.box(7.2, 7.5, 9.9, 8.8, 12.0, 11.0, "dark")
    # raised in front of you while blocking
    blocking = dict(SHIELD)
    blocking["firstperson_righthand"] = {"rotation": [0, -5, 0], "translation": [-4, 1, -2], "scale": [0.8, 0.8, 0.8]}
    blocking["firstperson_lefthand"] = blocking["firstperson_righthand"]
    m.name = "amethyst_shield_blocking"
    m.write(extra={"display": blocking})
    m.name = "amethyst_shield"
    m.write(extra={"overrides": [{"predicate": {"blocking": 1}, "model": f"{NS}:item/amethyst_shield_blocking"}]})


def purified_spear():
    """Amitariel's spear made pure: a white-silver shaft, gold bands and a pale blue crystal blade."""
    m = Item3D("purified_spear", {"shaft": (226, 230, 240), "gold": (236, 196, 92), "grip": (110, 170, 225),
                                  "blade": (160, 220, 250), "edge": (245, 252, 255), "glow": (120, 220, 255)})
    m.box(7.4, -12, 7.4, 8.6, 20, 8.6, "shaft")
    for y in (-6, 2, 12):
        m.box(7.1, y, 7.1, 8.9, y + 0.8, 8.9, "gold")
    m.box(7.2, -2, 7.2, 8.8, 1.5, 8.8, "grip")
    m.box(7.0, -14, 7.0, 9.0, -12, 9.0, "gold")
    m.box(7.6, -16, 7.6, 8.4, -14, 8.4, "glow")
    m.box(6.9, 19.5, 6.9, 9.1, 21, 9.1, "gold")
    m.box(4.4, 20.5, 7.6, 11.6, 21.4, 8.4, "edge")
    m.box(3.8, 21.4, 7.7, 4.8, 24, 8.3, "edge")
    m.box(11.2, 21.4, 7.7, 12.2, 24, 8.3, "edge")
    m.box(6.4, 21, 7.5, 9.6, 26.5, 8.5, "blade")
    m.box(6.9, 26.5, 7.5, 9.1, 29.5, 8.5, "edge")
    m.box(7.5, 29.5, 7.6, 8.5, 31.5, 8.4, "edge")
    m.box(7.7, 21.5, 7.3, 8.3, 28.5, 8.7, "glow")
    m.write()


def crescent_blade():
    """A curved crystal saber: the blade bends like a crescent moon, its edge glowing."""
    m = Item3D("crescent_blade", {"blade": (190, 205, 250), "edge": (248, 250, 255), "guard": (120, 70, 200),
                                  "grip": (52, 36, 84), "gem": (255, 110, 230), "glow": (170, 120, 255)})
    m.box(6.8, -1, 6.8, 9.2, 1, 9.2, "guard")
    m.box(7.3, 1, 7.3, 8.7, 5, 8.7, "grip")
    m.box(7.1, 2.4, 7.1, 8.9, 3, 8.9, "guard")
    m.box(3.6, 5, 6.6, 12.4, 6.4, 9.4, "guard")
    m.box(2.8, 5.6, 7.0, 3.6, 8.0, 9.0, "glow")
    m.box(12.4, 3.0, 7.0, 13.2, 5.6, 9.0, "glow")
    m.box(7.3, 5.1, 6.1, 8.7, 6.3, 6.6, "gem")
    m.box(7.4, -1.8, 7.4, 8.6, -1, 8.6, "gem")
    # the blade: segments stepping sideways as they rise, wide at the base and pointed at the tip
    for k in range(9):
        y = 6.4 + k * 1.55
        cx = 8 + 0.075 * k * k
        w = 1.35 - 0.1 * k
        m.box(cx - w, y, 7.25, cx + w, y + 1.6, 8.75, "blade")
        m.box(cx + w - 0.1, y, 7.4, cx + w + 0.45, y + 1.6, 8.6, "edge")
        m.box(cx - 0.2, y, 7.1, cx + 0.2, y + 1.6, 8.9, "glow")
    m.box(13.6, 20.4, 7.5, 14.4, 21.6, 8.5, "edge")
    m.write()


def spear():
    """Amitariel's spear: a long dark shaft bound in gold, a crystal crossguard and a leaf-shaped crystal blade."""
    m = Item3D("amitariel_spear", {"shaft": (44, 30, 62), "gold": (236, 196, 92), "grip": (74, 38, 116),
                                   "blade": (150, 90, 215), "edge": (225, 195, 255), "glow": (255, 110, 230)})
    m.box(7.4, -12, 7.4, 8.6, 20, 8.6, "shaft")
    for y in (-6, 2, 12):
        m.box(7.1, y, 7.1, 8.9, y + 0.8, 8.9, "gold")
    m.box(7.2, -2, 7.2, 8.8, 1.5, 8.8, "grip")
    m.box(7.0, -14, 7.0, 9.0, -12, 9.0, "gold")
    m.box(7.6, -16, 7.6, 8.4, -14, 8.4, "edge")
    m.box(6.9, 19.5, 6.9, 9.1, 21, 9.1, "gold")
    m.box(4.8, 20.5, 7.6, 11.2, 21.4, 8.4, "edge")
    m.box(4.2, 21.4, 7.7, 5.2, 23, 8.3, "edge")
    m.box(10.8, 21.4, 7.7, 11.8, 23, 8.3, "edge")
    m.box(6.6, 21, 7.5, 9.4, 26, 8.5, "blade")
    m.box(7.0, 26, 7.5, 9.0, 29, 8.5, "edge")
    m.box(7.5, 29, 7.6, 8.5, 31, 8.4, "edge")
    m.box(7.7, 21.5, 7.3, 8.3, 28, 8.7, "glow")
    m.write()


V = {"edge": (225, 195, 255)}

# ---- swords --------------------------------------------------------------------
sword("amethyst_sword", (150, 90, 215), (225, 195, 255), (58, 34, 100), (62, 40, 96), (255, 110, 230),
      extras=[("edge", 9.2, 7.5, 7.5, 10.6, 11.5, 8.5), ("edge", 5.4, 9.5, 7.5, 6.8, 13.5, 8.5),
              ("edge", 9.2, 12.5, 7.6, 10.2, 15.5, 8.4), ("edge", 5.8, 6.8, 7.6, 6.8, 9.2, 8.4)])
sword("flame_brand", (214, 88, 24), (255, 214, 96), (70, 30, 22), (92, 52, 28), (255, 230, 90),
      extras=[("edge", 9.2, 8, 7.6, 10.2, 12, 8.4), ("edge", 5.8, 10, 7.6, 6.8, 14, 8.4),
              ("edge", 9.2, 13, 7.7, 9.9, 16, 8.3), ("edge", 6.1, 6.8, 7.7, 6.8, 8.8, 8.3),
              ("gem", 9.9, 9, 7.8, 10.5, 11, 8.2)], wide_guard=True)
sword("frost_edge", (120, 200, 240), (232, 250, 255), (70, 104, 140), (96, 120, 150), (255, 255, 255),
      extras=[("edge", 9.2, 7.5, 7.4, 10.4, 11, 8.6), ("edge", 5.6, 8.5, 7.4, 6.8, 12.5, 8.6),
              ("edge", 9.2, 12, 7.5, 10.0, 16, 8.5), ("edge", 5.9, 13.5, 7.5, 6.8, 16.5, 8.5),
              ("blade", 7.2, 5.6, 5.4, 8.8, 8, 6.2)])
sword("storm_blade", (228, 196, 74), (255, 246, 160), (50, 62, 110), (66, 50, 36), (90, 200, 255),
      extras=[("gem", 6.2, 9, 7.6, 6.9, 11, 8.4), ("gem", 9.1, 11.5, 7.6, 9.8, 13.5, 8.4),
              ("gem", 6.2, 13.5, 7.6, 6.9, 15.5, 8.4), ("gem", 9.1, 7.5, 7.6, 9.8, 9.5, 8.4),
              ("gem", 5.6, 11, 7.8, 6.2, 12, 8.2), ("gem", 9.8, 13.5, 7.8, 10.4, 14.5, 8.2)])
sword("void_edge", (36, 24, 56), (214, 90, 230), (22, 14, 36), (30, 20, 44), (255, 90, 235),
      extras=[("gem", 6.4, 8, 6.9, 6.8, 18, 9.1), ("gem", 9.2, 8, 6.9, 9.6, 18, 9.1),
              ("edge", 10.6, 11, 7.6, 11.8, 12.6, 8.4), ("edge", 4.2, 14, 7.6, 5.4, 15.6, 8.4),
              ("edge", 11.2, 16.5, 7.6, 12.2, 17.7, 8.4)])
sword("purifier_blade", (236, 244, 250), (255, 255, 255), (236, 196, 92), (140, 112, 70), (120, 220, 255),
      extras=[("guard", 2.2, 6.6, 7, 4, 10.4, 9), ("guard", 12, 6.6, 7, 13.8, 10.4, 9),
              ("guard", 1.4, 8.4, 7.2, 2.6, 12.4, 8.8), ("guard", 13.4, 8.4, 7.2, 14.6, 12.4, 8.8),
              ("gem", 7.4, 9, 6.6, 8.6, 12, 7.2)], wide_guard=True)

# ---- staffs ----------------------------------------------------------------------
staff("flame_staff", (70, 44, 30), (230, 170, 60), (255, 120, 30), (255, 210, 90), (255, 240, 130),
      extras=[("a", 6.2, 18, 6.2, 9.8, 19.2, 9.8), ("b", 7, 19.2, 7, 9, 23, 9), ("glow", 7.4, 19.6, 7.4, 8.6, 21.6, 8.6),
              ("a", 5.4, 18, 7.2, 6.4, 22, 8.8), ("a", 9.6, 18, 7.2, 10.6, 22, 8.8),
              ("a", 7.2, 18, 5.4, 8.8, 22, 6.4), ("a", 7.2, 18, 9.6, 8.8, 22, 10.6),
              ("b", 7.5, 23, 7.5, 8.5, 25, 8.5)])
staff("frost_staff", (150, 168, 190), (225, 240, 255), (120, 205, 245), (232, 250, 255), (255, 255, 255),
      extras=[("a", 6.6, 18, 6.6, 9.4, 21, 9.4), ("b", 7.2, 21, 7.2, 8.8, 25, 8.8), ("glow", 7.6, 21.5, 7.6, 8.4, 24, 8.4),
              ("a", 5.0, 18.5, 7.2, 6.6, 22.5, 8.8), ("a", 9.4, 18.5, 7.2, 11.0, 22.5, 8.8),
              ("b", 7.3, 18.5, 5.0, 8.7, 22, 6.6), ("b", 7.3, 18.5, 9.4, 8.7, 22, 11.0),
              ("b", 7.6, 25, 7.6, 8.4, 26.5, 8.4)])
staff("prism_scepter", (238, 228, 205), (232, 190, 90), (240, 120, 210), (255, 220, 250), (255, 250, 255),
      extras=[("a", 5.6, 18, 7, 10.4, 20, 9), ("a", 6.4, 20, 7, 9.6, 23, 9), ("b", 7.2, 23, 7.2, 8.8, 25, 8.8),
              ("glow", 7.4, 19, 6.6, 8.6, 24, 9.4), ("b", 4.4, 18.5, 7.4, 5.6, 21.5, 8.6), ("b", 10.4, 18.5, 7.4, 11.6, 21.5, 8.6),
              ("b", 7.6, 25, 7.6, 8.4, 27, 8.4)])

# ---- hammers ---------------------------------------------------------------------
hammer("crystal_hammer", (76, 50, 110), (132, 76, 200), (210, 160, 255), (56, 34, 90), (255, 110, 230),
       extras=[("cap", 6.5, 20, 6.5, 9.5, 23, 9.5), ("cap", 7.2, 23, 7.2, 8.8, 25, 8.8), ("cap", 4.2, 20, 6.8, 5.6, 22.5, 9.2),
               ("cap", 10.4, 20, 6.8, 11.8, 22.5, 9.2), ("glow", 7.2, 15.6, 4.6, 8.8, 18.4, 5.0), ("glow", 7.2, 15.6, 11, 8.8, 18.4, 11.4)])
hammer("talis_hammer", (214, 164, 56), (88, 94, 112), (230, 188, 70), (46, 52, 70), (110, 220, 255),
       extras=[("glow", 6.8, 15.2, 4.4, 9.2, 18.8, 5.0), ("glow", 6.8, 15.2, 11, 9.2, 18.8, 11.6),
               ("accent", 0.4, 15.4, 7, 1.8, 18.6, 9), ("accent", 14.2, 15.4, 7, 15.6, 18.6, 9),
               ("cap", 6.4, 20, 6.4, 9.6, 21, 9.6), ("glow", 7.4, 21, 7.4, 8.6, 25, 8.6), ("cap", 7, 22.4, 7, 9, 23, 9),
               ("glow", 3.4, 14.4, 4.2, 4.6, 19.6, 4.6), ("glow", 11.4, 14.4, 4.2, 12.6, 19.6, 4.6)])

shield()
spear()


def crown():
    """The emperor's crown: a gold band with points, a red gem in front and pearls on the points."""
    m = Item3D("emperor_crown", {"gold": (236, 196, 92), "dark": (168, 120, 40), "ruby": (220, 40, 60),
                                 "pearl": (245, 240, 255), "velvet": (120, 20, 50)}, rotate=0, parent="object")
    for (x0, z0, x1, z1) in ((3, 3, 13, 4), (3, 12, 13, 13), (3, 3, 4, 13), (12, 3, 13, 13)):
        m.box(x0, 2, z0, x1, 6, z1, "gold")
        m.box(x0, 2, z0, x1, 3, z1, "dark")
    m.box(4, 2, 4, 12, 5, 12, "velvet")
    for (x, z) in ((3, 3), (7.5, 3), (12, 3), (3, 12), (7.5, 12), (12, 12), (3, 7.5), (12, 7.5)):
        m.box(x, 6, z, x + 1, 9, z + 1, "gold")
        m.box(x, 9, z, x + 1, 10, z + 1, "pearl")
    m.box(7, 3.5, 2.6, 9, 5.5, 3, "ruby")
    m.box(7, 3.5, 13, 9, 5.5, 13.4, "ruby")
    m.box(7.5, 5, 7.5, 8.5, 8, 8.5, "gold")
    m.box(7.25, 8, 7.25, 8.75, 9.5, 8.75, "ruby")
    m.write()


def flask():
    """The flask of chaos: a round glass bottle with a swirl of violet and green inside, a cork and a seal."""
    m = Item3D("chaos_flask", {"glass": (200, 225, 235), "violet": (150, 60, 220), "green": (90, 220, 120),
                               "cork": (150, 110, 70), "seal": (200, 40, 80)}, rotate=0, parent="object")
    m.box(4, 1, 4, 12, 9, 12, "glass")
    m.box(5, 2, 5, 11, 7, 11, "violet")
    m.box(6, 3, 4.6, 9, 5, 11.4, "green")
    m.box(4.6, 4, 7, 11.4, 6, 9, "green")
    m.box(6.5, 9, 6.5, 9.5, 12, 9.5, "glass")
    m.box(6.2, 12, 6.2, 9.8, 14, 9.8, "cork")
    m.box(7, 14, 7, 9, 14.6, 9, "seal")
    m.write()


def shard():
    """The shard of purity: a long white crystal with a pale blue glow running through it."""
    m = Item3D("purity_shard", {"white": (240, 248, 255), "blue": (150, 210, 245), "glow": (120, 230, 255)}, rotate=-22.5, parent="object")
    m.box(6.5, 1, 6.5, 9.5, 11, 9.5, "white")
    m.box(7, 11, 7, 9, 14, 9, "white")
    m.box(7.5, 14, 7.5, 8.5, 15.5, 8.5, "glow")
    m.box(7.6, 2, 6.2, 8.4, 12, 9.8, "glow")
    m.box(5.5, 1, 7, 6.5, 6, 9, "blue")
    m.box(9.5, 2, 7, 10.5, 8, 9, "blue")
    m.box(9.8, 6, 7.5, 11.8, 7, 8.5, "white")
    m.write()


crown()
flask()
shard()
purified_spear()
crescent_blade()

# ---- bow with draw stages ---------------------------------------------------------
pull = [("pulling", 1, "crystal_bow_pulling_0"), ("pulling", 1, "crystal_bow_pulling_1"), ("pulling", 1, "crystal_bow_pulling_2")]
bow_display = {
    "thirdperson_righthand": {"rotation": [-80, 260, -40], "translation": [-1, -2, 2.5], "scale": [0.9, 0.9, 0.9]},
    "thirdperson_lefthand": {"rotation": [-80, -280, 40], "translation": [-1, -2, 2.5], "scale": [0.9, 0.9, 0.9]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
}
models = ASSETS / "models/item"
(models / "crystal_bow.json").write_text(json.dumps({
    "parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/crystal_bow"}, "display": bow_display,
    "overrides": [
        {"predicate": {"pulling": 1}, "model": f"{NS}:item/crystal_bow_pulling_0"},
        {"predicate": {"pulling": 1, "pull": 0.65}, "model": f"{NS}:item/crystal_bow_pulling_1"},
        {"predicate": {"pulling": 1, "pull": 0.9}, "model": f"{NS}:item/crystal_bow_pulling_2"}]}, indent=1), encoding="utf-8", newline="\n")
for i in range(3):
    (models / f"crystal_bow_pulling_{i}.json").write_text(json.dumps({
        "parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/crystal_bow_pulling_{i}"}, "display": bow_display},
        indent=1), encoding="utf-8", newline="\n")
print("3D item models written")
