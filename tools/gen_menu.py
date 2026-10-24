"""Main menu assets, from tools/menu_src/ (the author's own pictures):
  logo_*.png  -> assets/amethystmod/textures/gui/menu/   (the logo; LogoDrawerMixin draws it)
  bg_0..5.png -> assets/minecraft/textures/gui/title/background/panorama_0..5.png  (the rotating menu panorama)
  buttons     -> assets/minecraft/textures/gui/sprites/widget/button*.png  (drawn here: crystal buttons for every menu)

Panorama faces are the six views of a cube, taken from ONE spot with field of view 90 (vertical):
0 front, 1 right, 2 back, 3 left, 4 up, 5 down. Pictures that are not square are cut to the central
square, which is exactly 90 degrees both ways at FOV 90.
"""
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "tools/menu_src"
LOGO_OUT = ROOT / "src/main/resources/assets/amethystmod/textures/gui/menu"
PANO_OUT = ROOT / "src/main/resources/assets/minecraft/textures/gui/title/background"
LOGO_OUT.mkdir(parents=True, exist_ok=True)

for f in sorted(SRC.glob("logo_*.png")):
    im = Image.open(f).convert("RGBA")
    im = im.resize((1000, round(1000 * im.height / im.width)), Image.LANCZOS)
    im.save(LOGO_OUT / f.name, optimize=True)

# The item in the author's hand showed in two of the pictures: these polygons (picture index -> points in the
# original 1920x1200 picture) are painted over with the surrounding background.
BG_FIX = {
    3: [[(1148, 1048), (1160, 1048), (1176, 1110), (1200, 1170), (1200, 1200), (1146, 1200), (1138, 1130)]],
    5: [[(452, 392), (477, 394), (505, 396), (565, 408), (672, 422), (752, 452), (750, 478), (672, 466), (640, 470),
         (565, 440), (505, 423), (452, 405)]],
}


def inpaint(im, polygons, grow=7, iterations=900):
    """Fills the polygons (grown a little) by letting the colours around them flow in."""
    mask_img = Image.new("L", im.size, 0)
    draw = ImageDraw.Draw(mask_img)
    for poly in polygons:
        draw.polygon(poly, fill=255)
    mask_img = mask_img.filter(ImageFilter.MaxFilter(grow * 2 + 1))
    box = mask_img.getbbox()
    x0, y0, x1, y1 = box[0] - 12, box[1] - 12, box[2] + 12, box[3] + 12
    x0, y0, x1, y1 = max(0, x0), max(0, y0), min(im.width, x1), min(im.height, y1)
    region = np.asarray(im.crop((x0, y0, x1, y1)), dtype=np.float32).copy()
    mask = np.asarray(mask_img.crop((x0, y0, x1, y1))) > 0
    known = ~mask
    region[mask] = region[known].mean(axis=0)
    for _ in range(iterations):
        pad = np.pad(region, ((1, 1), (1, 1), (0, 0)), mode="edge")
        avg = (pad[:-2, 1:-1] + pad[2:, 1:-1] + pad[1:-1, :-2] + pad[1:-1, 2:]) / 4.0
        region[mask] = avg[mask]
    out = im.copy()
    out.paste(Image.fromarray(region.clip(0, 255).astype(np.uint8)), (x0, y0))
    return out


faces = [SRC / f"bg_{i}.png" for i in range(6)]
if all(f.exists() for f in faces):
    PANO_OUT.mkdir(parents=True, exist_ok=True)
    for i, f in enumerate(faces):
        im = Image.open(f).convert("RGB")
        if i in BG_FIX:
            im = inpaint(im, BG_FIX[i])
        side = min(im.size)
        left, top = (im.width - side) // 2, (im.height - side) // 2
        im = im.crop((left, top, left + side, top + side)).resize((1024, 1024), Image.LANCZOS)
        im.save(PANO_OUT / f"panorama_{i}.png", optimize=True)
    print("menu panorama written")
else:
    print("no panorama pictures (tools/menu_src/bg_0..5.png): the vanilla panorama stays")


# ---- crystal buttons: 200x20, a 3 pixel border that stays and a middle that stretches ----------------------
WIDGET_OUT = ROOT / "src/main/resources/assets/minecraft/textures/gui/sprites/widget"
WIDGET_OUT.mkdir(parents=True, exist_ok=True)
NINE_SLICE = json.dumps({"gui": {"scaling": {"type": "nine_slice", "width": 200, "height": 20, "border": 3}}}, indent=4) + chr(10)


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def crystal_button(kind):
    """kind: normal, highlighted or disabled. A faceted violet plate: dark outline, a light bevel on the top and
    left, a shadowed bevel on the bottom and right, a soft top-to-bottom gradient and a few pale facet streaks."""
    palette = {
        "normal": dict(top=(118, 62, 196), bottom=(62, 28, 118), outline=(24, 10, 44), light=(206, 150, 255),
                       dark=(44, 18, 84), facet=(150, 92, 226), glint=(236, 200, 255)),
        "highlighted": dict(top=(168, 96, 240), bottom=(96, 44, 170), outline=(255, 142, 238), light=(255, 214, 255),
                            dark=(70, 28, 124), facet=(204, 140, 255), glint=(255, 240, 255)),
        "disabled": dict(top=(66, 58, 82), bottom=(40, 34, 54), outline=(20, 16, 30), light=(104, 94, 124),
                         dark=(30, 26, 42), facet=(78, 70, 98), glint=(120, 110, 140)),
    }[kind]
    w, h = 200, 20
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = img.load()
    for y in range(h):
        for x in range(w):
            t = y / (h - 1)
            col = mix(palette["top"], palette["bottom"], t)
            # a faint, repeatable grain keeps the plate from looking flat
            col = tuple(max(0, min(255, c + (((x * 7 + y * 13) % 5) - 2))) for c in col)
            # facet streaks: slanted bands that stay inside the stretchable middle
            band = (x + y * 2) % 46
            if 3 < x < w - 3 and 2 < y < h - 3:
                if band < 3:
                    col = mix(col, palette["facet"], 0.55)
                elif band == 3:
                    col = mix(col, palette["glint"], 0.35)
            edge_top, edge_left = y == 1, x == 1
            edge_bottom, edge_right = y == h - 2, x == w - 2
            if edge_top or edge_left:
                col = mix(col, palette["light"], 0.85)
            if edge_bottom or edge_right:
                col = mix(col, palette["dark"], 0.8)
            if y == 2 and 2 < x < w - 3:
                col = mix(col, palette["light"], 0.3)
            if y == h - 3 and 2 < x < w - 3:
                col = mix(col, palette["dark"], 0.35)
            if x in (0, w - 1) or y in (0, h - 1):
                col = palette["outline"]
            px[x, y] = col + (255,)
    # rounded outline corners and a bright gem in the top corners
    for (cx, cy) in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)):
        px[cx, cy] = (0, 0, 0, 0)
    for (cx, cy) in ((2, 2), (w - 3, 2)):
        px[cx, cy] = palette["glint"] + (255,)
    return img


for kind, name in (("normal", "button"), ("highlighted", "button_highlighted"), ("disabled", "button_disabled")):
    crystal_button(kind).save(WIDGET_OUT / f"{name}.png", optimize=True)
    (WIDGET_OUT / f"{name}.png.mcmeta").write_text(NINE_SLICE, encoding="utf-8", newline=chr(10))
print("menu assets written")
