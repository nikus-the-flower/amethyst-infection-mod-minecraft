"""Cuboid entity model toolkit.

Models are described in "author space": pixels (1/16 block), Y up, the entity stands on y=0,
and it faces -Z (X+ is its left). Part pivots of root parts are absolute, pivots of child parts
are relative to the parent's pivot. Rotations use Minecraft's convention (degrees, pitch/yaw/roll).

`Model.export()` writes the JSON the game loads plus the colour and glow textures.
"""
import json
import math
import random
import zlib
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources/assets/amethystmod"

STYLES = {
    "rock": (84, 60, 128),
    "crystal": (146, 86, 212),
    "bright": (200, 146, 252),
    "dark": (46, 32, 72),
    "glow": (255, 95, 235),
    "eye": (255, 235, 255),
    "bone": (205, 188, 232),
    "pale": (205, 232, 248),
    "paleglow": (160, 235, 255),
    "gold": (236, 196, 92),
    "skin": (222, 196, 214),
    "cloth": (74, 38, 116),
    "armor": (156, 146, 186),
    "hair": (238, 232, 250),
    "leather": (112, 74, 42),
    "moss": (70, 92, 56),
    "wood": (132, 98, 58),
    "beard": (214, 210, 204),
    "paper": (232, 220, 186),
    "tan": (214, 164, 124),
}
FACE_SHADE = {"top": 1.18, "front": 1.0, "right": 0.9, "left": 0.84, "back": 0.78, "bottom": 0.6}


class Cube:
    def __init__(self, lo, hi, style):
        self.lo, self.hi, self.style = lo, hi, style
        self.size = tuple(max(1, round(h - l)) for l, h in zip(lo, hi))
        self.hi = tuple(l + s for l, s in zip(lo, self.size))
        self.uv = (0, 0)


class Part:
    def __init__(self, name, parent, pivot, rot):
        self.name, self.parent, self.pivot, self.rot = name, parent, pivot, rot
        self.cubes = []


class Model:
    def __init__(self, name, anim, seed=1):
        self.name, self.anim = name, anim
        self.parts = {}
        self.order = []
        self.rng = random.Random(seed)
        self.counter = 0

    def part(self, name, parent=None, pivot=(0, 0, 0), rot=(0, 0, 0)):
        assert name not in self.parts, name
        p = Part(name, parent, pivot, rot)
        self.parts[name] = p
        self.order.append(name)
        return p

    def cube(self, part, lo, hi, style="rock"):
        self.parts[part].cubes.append(Cube(lo, hi, style))

    def box(self, part, cx, y0, cz, w, h, d, style="rock"):
        """A cube centred on x/z with its bottom at y0."""
        self.cube(part, (cx - w / 2, y0, cz - d / 2), (cx + w / 2, y0 + h, cz + d / 2), style)

    def spike(self, parent, x, y, z, h, w=4, fwd=0, left=0, yaw=0, style="crystal", tip="bright"):
        """A tapering crystal growing from (x,y,z) relative to the parent's pivot."""
        self.counter += 1
        name = f"{parent}_sp{self.counter}"
        self.part(name, parent, (x, y, z), (fwd, yaw, left))
        h1 = max(1, round(h * 0.62))
        h2 = max(1, round(h) - h1)
        self.cube(name, (-w / 2, 0, -w / 2), (w / 2, h1, w / 2), style)
        self.cube(name, (-w / 4, h1, -w / 4), (w / 4, h1 + h2, w / 4), tip)

    def cluster(self, parent, cx, cy, cz, n, hmin, hmax, spread, lean=(0, 0), w=(2, 4, 6)):
        """Several crystals growing around a point; lean = (forward, left) bias in degrees."""
        r = self.rng
        for _ in range(n):
            self.spike(parent,
                       cx + r.uniform(-spread, spread), cy, cz + r.uniform(-spread, spread),
                       r.uniform(hmin, hmax), w=r.choice(w),
                       fwd=lean[0] + r.uniform(-28, 28), left=lean[1] + r.uniform(-28, 28),
                       yaw=r.uniform(0, 360))

    # ---- conversion ---------------------------------------------------------
    def _mc_pivot(self, p):
        x, y, z = p.pivot
        return (x, 24 - y, z) if p.parent is None else (x, -y, z)

    def pack(self, width=256):
        cubes = [c for n in self.order for c in self.parts[n].cubes]
        cubes.sort(key=lambda c: -(c.size[2] + c.size[1]))
        x = y = row_h = 0
        for c in cubes:
            w, h, d = c.size
            cw, ch = 2 * (d + w), d + h
            if x + cw > width:
                x, y, row_h = 0, y + row_h, 0
            c.uv = (x, y)
            x += cw
            row_h = max(row_h, ch)
        self.tex_w, self.tex_h = width, ((y + row_h + 15) // 16) * 16

    def to_json(self):
        parts = []
        for n in self.order:
            p = self.parts[n]
            parts.append({
                "name": p.name, "parent": p.parent,
                "pivot": [round(v, 3) for v in self._mc_pivot(p)],
                "rot": list(p.rot),
                "cubes": [{"o": [c.lo[0], -c.hi[1], c.lo[2]], "s": list(c.size), "uv": list(c.uv)} for c in p.cubes],
            })
        return {"texture": [self.tex_w, self.tex_h], "anim": self.anim, "parts": parts}

    # ---- textures -----------------------------------------------------------
    def paint(self):
        rng = random.Random(zlib.crc32(self.name.encode()))
        main = Image.new("RGBA", (self.tex_w, self.tex_h), (0, 0, 0, 0))
        glow = Image.new("RGBA", (self.tex_w, self.tex_h), (0, 0, 0, 0))
        for n in self.order:
            for c in self.parts[n].cubes:
                self._paint_cube(main, glow, c, rng)
        return main, glow

    @staticmethod
    def _faces(c):
        w, h, d = c.size
        u, v = c.uv
        return {
            "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
            "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
        }

    def _paint_cube(self, main, glow, c, rng):
        """Pixel-art shading per face: mottled base, bevelled edges, cracks and glowing veins on rock,
        a bright-top gradient with facets and sparkles on crystals, radial glow on cores and eyes."""
        base = STYLES[c.style]
        crystalline = c.style in ("crystal", "bright", "pale", "gold", "armor")
        glowing = c.style in ("glow", "paleglow", "eye")
        for face, (fx, fy, fw, fh) in self._faces(c).items():
            if fw <= 0 or fh <= 0:
                continue
            shade = FACE_SHADE[face]
            mottle = {}
            pixels = {}
            for x in range(fw):
                for y in range(fh):
                    cell = (x // 3, y // 3)
                    if cell not in mottle:
                        mottle[cell] = rng.randint(-12, 12)
                    r, g, b = base
                    n = mottle[cell] // 2 + rng.randint(-5, 5)
                    edge = x == 0 or y == 0 or x == fw - 1 or y == fh - 1
                    if crystalline:
                        t = y / max(1, fh - 1)
                        n += int((1 - t) * 26) if face not in ("top", "bottom") else 18
                        if ((x + y) // 3) % 2:
                            n += 10
                        if edge:
                            n += 30 if (x == 0 or y == 0) else -30
                    elif glowing:
                        cx, cy = (fw - 1) / 2, (fh - 1) / 2
                        d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5 / max(1.0, max(cx, cy, 1.0))
                        n = int(28 * (1 - d)) - 10 + rng.randint(-4, 4)
                        if c.style == "eye":
                            n += 20
                    elif edge:
                        n -= 22
                    col = [max(0, min(255, int((v + n) * shade))) for v in (r, g, b)]
                    pixels[(x, y)] = col
            if c.style in ("rock", "dark") and fw >= 4 and fh >= 4:
                # a crack: a short dark random walk
                x, y = rng.randrange(fw), rng.randrange(fh)
                for _ in range(rng.randint(3, 3 + (fw + fh) // 3)):
                    if (x, y) in pixels:
                        pixels[(x, y)] = [int(v * 0.55) for v in pixels[(x, y)]]
                    x += rng.choice((-1, 0, 1))
                    y += rng.choice((0, 1))
                # sometimes a glowing crystal vein
                if rng.random() < 0.35:
                    x, y = rng.randrange(fw), rng.randrange(fh)
                    for _ in range(rng.randint(3, 3 + (fw + fh) // 2)):
                        if (x, y) in pixels:
                            pixels[(x, y)] = [200, 120, 255]
                            glow.putpixel((fx + x, fy + y), (220, 140, 255, 170))
                        x += rng.choice((-1, 0, 1))
                        y += rng.choice((-1, 0, 1))
            for (x, y), col in pixels.items():
                px, py = fx + x, fy + y
                main.putpixel((px, py), tuple(col) + (255,))
                if glowing:
                    glow.putpixel((px, py), (min(255, col[0] + 35), min(255, col[1] + 35), min(255, col[2] + 35), 255))
                elif crystalline and rng.random() < 0.07:
                    main.putpixel((px, py), (245, 230, 255, 255))
                    glow.putpixel((px, py), (240, 210, 255, 150))

    def export(self):
        self.pack()
        (RES / "geo").mkdir(parents=True, exist_ok=True)
        (RES / "textures/entity").mkdir(parents=True, exist_ok=True)
        (RES / "geo" / f"{self.name}.json").write_text(json.dumps(self.to_json(), indent=1), encoding="utf-8", newline="\n")
        main, glow = self.paint()
        main.save(RES / "textures/entity" / f"{self.name}.png")
        glow.save(RES / "textures/entity" / f"{self.name}_glow.png")
        cubes = sum(len(p.cubes) for p in self.parts.values())
        print(f"{self.name}: {len(self.parts)} parts, {cubes} cubes, atlas {self.tex_w}x{self.tex_h}")
