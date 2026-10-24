"""Generates the Amethyst Infection logo: vector SVG, smooth PNG and pixel-art PNG.

Run: python branding/make_logo.py  (outputs go next to this script)
"""
import math
import os

from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.dirname(os.path.abspath(__file__))
S = 512  # design canvas size

# Crystal palettes: left face, center face, right face, left tip, center tip, right tip
VIOLET = ["#4a1d8a", "#7c3fd1", "#b48af2", "#6a35b8", "#a374ea", "#e2ccff"]
PINK = ["#7a2373", "#c24fb5", "#ee9be0", "#a03c98", "#e07fd4", "#ffd1f5"]
AZURE = ["#2a3f8f", "#4f74d6", "#8fb4f7", "#3f5cbf", "#78a0f0", "#d4e5ff"]
OUTLINE = "#14051f"

# (base x, base y, shaft height, width, lean degrees, palette) - back to front
CRYSTALS = [
    (140, 425, 55, 40, -48, VIOLET),
    (374, 425, 70, 44, 46, VIOLET),
    (188, 415, 125, 66, -24, PINK),
    (326, 415, 140, 70, 22, AZURE),
    (256, 410, 190, 92, 0, VIOLET),
    (212, 446, 20, 20, -16, PINK),
    (302, 446, 26, 22, 14, AZURE),
]

MOUND = [(112, 452), (128, 420), (170, 400), (220, 392), (256, 390), (296, 392),
         (344, 400), (386, 420), (400, 452)]
MOUND_TOP = "#3a2650"
MOUND_BOTTOM = "#1e1230"
VEINS = [[(170, 420), (195, 432), (205, 448)], [(330, 418), (318, 434), (322, 450)],
         [(250, 404), (262, 422), (256, 446)]]

SPARKLES = [(118, 150, 20), (398, 118, 26), (420, 300, 13), (92, 318, 11), (300, 70, 9)]


def crystal_faces(bx, by, h, w, lean, pal):
    """Returns [(points, color)] for the faces plus the silhouette polygon."""
    a = math.radians(lean)
    ca, sa = math.cos(a), math.sin(a)

    def p(x, y):
        return (bx + x * ca - y * sa, by + x * sa + y * ca)

    l, cl, cr, r = -w / 2, -w / 6, w / 6, w / 2
    tip = p(0, -h - w * 0.85)
    faces = [
        ([p(l, 0), p(cl, 0), p(cl, -h), p(l, -h)], pal[0]),
        ([p(cl, 0), p(cr, 0), p(cr, -h), p(cl, -h)], pal[1]),
        ([p(cr, 0), p(r, 0), p(r, -h), p(cr, -h)], pal[2]),
        ([p(l, -h), p(cl, -h), tip], pal[3]),
        ([p(cl, -h), p(cr, -h), tip], pal[4]),
        ([p(cr, -h), p(r, -h), tip], pal[5]),
    ]
    silhouette = [p(l, 0), p(l, -h), tip, p(r, -h), p(r, 0)]
    # thin glint along the center face
    glint = [p(cl + w * 0.07, -h * 0.15), p(cl + w * 0.07, -h * 0.8)]
    return faces, silhouette, glint


def star(cx, cy, r):
    k = r * 0.22
    return [(cx, cy - r), (cx + k, cy - k), (cx + r, cy), (cx + k, cy + k),
            (cx, cy + r), (cx - k, cy + k), (cx - r, cy), (cx - k, cy - k)]


def hex_rgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))


def lerp(c1, c2, t):
    return tuple(round(a + (b - a) * t) for a, b in zip(c1, c2))


# ---------------------------------------------------------------- SVG

def write_svg():
    pts = lambda ps: " ".join(f"{x:.1f},{y:.1f}" for x, y in ps)
    o = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {S} {S}" width="{S}" height="{S}">',
         "<defs>",
         '<radialGradient id="bg" cx="50%" cy="52%" r="62%">'
         '<stop offset="0" stop-color="#6a2fa8"/><stop offset="0.6" stop-color="#2e1150"/>'
         '<stop offset="1" stop-color="#14071f"/></radialGradient>',
         '<radialGradient id="glow" cx="50%" cy="50%" r="50%">'
         '<stop offset="0" stop-color="#e6c8ff" stop-opacity="0.55"/>'
         '<stop offset="1" stop-color="#b070ff" stop-opacity="0"/></radialGradient>',
         '<linearGradient id="mound" x1="0" y1="0" x2="0" y2="1">'
         f'<stop offset="0" stop-color="{MOUND_TOP}"/><stop offset="1" stop-color="{MOUND_BOTTOM}"/>'
         "</linearGradient>",
         "</defs>",
         f'<rect x="8" y="8" width="{S-16}" height="{S-16}" rx="92" fill="url(#bg)" '
         'stroke="#c9a6ff" stroke-opacity="0.45" stroke-width="6"/>',
         '<ellipse cx="256" cy="290" rx="190" ry="190" fill="url(#glow)"/>']
    for x, y, r in SPARKLES:
        o.append(f'<polygon points="{pts(star(x, y, r))}" fill="#f3e6ff"/>')
    for c in CRYSTALS:
        faces, sil, glint = crystal_faces(*c)
        o.append(f'<polygon points="{pts(sil)}" fill="none" stroke="{OUTLINE}" '
                 'stroke-width="9" stroke-linejoin="round"/>')
        for ps, col in faces:
            o.append(f'<polygon points="{pts(ps)}" fill="{col}" stroke="{col}" stroke-width="0.8"/>')
        if c[2] > 60:
            o.append(f'<polyline points="{pts(glint)}" stroke="#ffffff" stroke-opacity="0.55" '
                     'stroke-width="4" stroke-linecap="round" fill="none"/>')
    o.append(f'<polygon points="{pts(MOUND)}" fill="url(#mound)" stroke="{OUTLINE}" '
             'stroke-width="8" stroke-linejoin="round"/>')
    for v in VEINS:
        o.append(f'<polyline points="{pts(v)}" stroke="#b77cf5" stroke-width="5" '
                 'stroke-linecap="round" stroke-linejoin="round" fill="none"/>')
    # small crystals sit in front of the mound
    for c in CRYSTALS[5:]:
        faces, sil, _ = crystal_faces(*c)
        o.append(f'<polygon points="{pts(sil)}" fill="none" stroke="{OUTLINE}" '
                 'stroke-width="7" stroke-linejoin="round"/>')
        for ps, col in faces:
            o.append(f'<polygon points="{pts(ps)}" fill="{col}" stroke="{col}" stroke-width="0.8"/>')
    o.append("</svg>")
    with open(os.path.join(OUT, "logo.svg"), "w", encoding="utf-8") as f:
        f.write("\n".join(o))


# ---------------------------------------------------------------- smooth PNG

def render_smooth(size=512):
    k = 4 * size / S  # supersample
    W = int(S * k)
    sc = lambda ps: [(x * k, y * k) for x, y in ps]

    bg = Image.new("RGB", (W, W))
    c0, c1, c2 = hex_rgb("#6a2fa8"), hex_rgb("#2e1150"), hex_rgb("#14071f")
    bd = ImageDraw.Draw(bg)
    cx, cy, rmax = W / 2, W * 0.52, W * 0.62 * 1.15
    steps = 200
    for i in range(steps, 0, -1):
        t = i / steps
        col = lerp(c0, c1, t / 0.6) if t < 0.6 else lerp(c1, c2, (t - 0.6) / 0.4)
        r = rmax * t
        bd.ellipse([cx - r, cy - r, cx + r, cy + r], fill=col)

    img = bg.convert("RGBA")
    glow = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([(256 - 150) * k, (290 - 150) * k, (256 + 150) * k, (290 + 150) * k],
               fill=(220, 180, 255, 120))
    glow = glow.filter(ImageFilter.GaussianBlur(60 * k))
    img.alpha_composite(glow)

    d = ImageDraw.Draw(img)
    for x, y, r in SPARKLES:
        d.polygon(sc(star(x, y, r)), fill="#f3e6ff")

    def draw_crystal(c, ow):
        faces, sil, glint = crystal_faces(*c)
        d.line(sc(sil + sil[:1]), fill=OUTLINE, width=int(ow * k), joint="curve")
        d.polygon(sc(sil), fill=OUTLINE)
        for ps, col in faces:
            d.polygon(sc(ps), fill=col)
        if c[2] > 60:
            d.line(sc(glint), fill=(255, 255, 255, 150), width=int(4 * k))

    for c in CRYSTALS[:5]:
        draw_crystal(c, 9)
    d.line(sc(MOUND + MOUND[:1]), fill=OUTLINE, width=int(8 * k), joint="curve")
    mound = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    md = ImageDraw.Draw(mound)
    top, bot = hex_rgb(MOUND_TOP), hex_rgb(MOUND_BOTTOM)
    for yy in range(int(390 * k), int(453 * k)):
        md.line([(0, yy), (W, yy)], fill=lerp(top, bot, (yy / k - 390) / 62))
    mask = Image.new("L", (W, W), 0)
    ImageDraw.Draw(mask).polygon(sc(MOUND), fill=255)
    img.paste(mound, (0, 0), mask)
    for v in VEINS:
        d.line(sc(v), fill="#b77cf5", width=int(5 * k), joint="curve")
    for c in CRYSTALS[5:]:
        draw_crystal(c, 7)

    # rounded square frame
    shape = Image.new("L", (W, W), 0)
    ImageDraw.Draw(shape).rounded_rectangle([8 * k, 8 * k, (S - 8) * k, (S - 8) * k], 92 * k, fill=255)
    out = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    out.paste(img, (0, 0), shape)
    ImageDraw.Draw(out).rounded_rectangle([8 * k, 8 * k, (S - 8) * k, (S - 8) * k], 92 * k,
                                          outline=(201, 166, 255, 115), width=int(6 * k))
    return out.resize((size, size), Image.LANCZOS)


# ---------------------------------------------------------------- pixel art

def render_pixel(px=64, scale=8):
    k = px / S
    sc = lambda ps: [(round(x * k), round(y * k)) for x, y in ps]

    bg = Image.new("RGBA", (px, px))
    bands = ["#5e2896", "#4a1f7a", "#36165c", "#251040", "#170a2a"]
    for y in range(px):
        for x in range(px):
            dist = math.hypot(x + 0.5 - px / 2, y + 0.5 - px * 0.55) / (px / 2)
            bg.putpixel((x, y), hex_rgb(bands[min(int(dist * 3.2), 4)]) + (255,))

    def crystal_layer(cs):
        layer = Image.new("RGBA", (px, px), (0, 0, 0, 0))
        ld = ImageDraw.Draw(layer)
        for c in cs:
            faces, _, _ = crystal_faces(*c)
            for ps, col in faces:
                ld.polygon(sc(ps), fill=col)
        return layer

    def outlined(layer):
        a = layer.getchannel("A")
        grown = a.filter(ImageFilter.MaxFilter(3))
        ring = Image.new("RGBA", (px, px), hex_rgb(OUTLINE) + (255,))
        out = Image.new("RGBA", (px, px), (0, 0, 0, 0))
        out.paste(ring, (0, 0), grown)
        out.alpha_composite(layer)
        return out

    img = bg.copy()
    d = ImageDraw.Draw(img)
    for x, y, r in SPARKLES:
        cx, cy = round(x * k), round(y * k)
        arm = max(1, round(r * k))
        d.point([(cx, cy)], fill="#ffffff")
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for i in range(1, arm + 1):
                col = "#e2ccff" if i < arm or arm == 1 else "#9a6fd6"
                d.point([(cx + i * dx, cy + i * dy)], fill=col)

    img.alpha_composite(outlined(crystal_layer(CRYSTALS[:5])))
    mound = Image.new("RGBA", (px, px), (0, 0, 0, 0))
    mdraw = ImageDraw.Draw(mound)
    mdraw.polygon(sc(MOUND), fill=MOUND_TOP)
    # darker lower half of the mound
    for y in range(round(430 * k), px):
        for x in range(px):
            if mound.getpixel((x, y))[3]:
                mound.putpixel((x, y), hex_rgb(MOUND_BOTTOM) + (255,))
    for v in VEINS:
        mdraw.line(sc(v), fill="#b77cf5")
    img.alpha_composite(outlined(mound))
    img.alpha_composite(outlined(crystal_layer(CRYSTALS[5:])))

    # glints on the big crystal: a short column of light pixels
    cx = round((256 - 92 / 6 + 92 * 0.07) * k)
    for y in range(round(260 * k), round(380 * k)):
        if img.getpixel((cx, y))[:3] == hex_rgb(VIOLET[1]):
            img.putpixel((cx, y), hex_rgb("#c9a6ff") + (255,))

    # rounded corners with a light frame
    frame = hex_rgb("#8f5fd0") + (255,)
    clear = (0, 0, 0, 0)
    rad = max(2, px // 10)
    for y in range(px):
        for x in range(px):
            ex = min(x, px - 1 - x)
            ey = min(y, px - 1 - y)
            if ex < rad and ey < rad:
                dist = math.hypot(rad - ex - 0.5, rad - ey - 0.5)
                if dist > rad:
                    img.putpixel((x, y), clear)
                elif dist > rad - 1:
                    img.putpixel((x, y), frame)
            elif ex == 0 or ey == 0:
                img.putpixel((x, y), frame)
    return img.resize((px * scale, px * scale), Image.NEAREST)


if __name__ == "__main__":
    write_svg()
    render_smooth(512).save(os.path.join(OUT, "logo_512.png"))
    render_smooth(128).save(os.path.join(OUT, "icon_128.png"))
    render_pixel().save(os.path.join(OUT, "logo_pixel_512.png"))
    print("done")
