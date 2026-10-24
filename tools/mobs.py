"""Model definitions for the amethyst creatures and bosses. Run: python tools/mobs.py [name ...]"""
import sys

from geo import Model


def stalker():
    m = Model("amethyst_stalker", "quadruped", seed=11)
    m.part("body", None, (0, 17, 0))
    m.cube("body", (-5, -4, -10), (5, 5, 8), "rock")
    m.cube("body", (-4, -6, -7), (4, -4, 5), "crystal")
    for i, z in enumerate(range(-8, 8, 3)):
        m.spike("body", ((i % 3) - 1) * 1.5, 5, z, 5 + (i % 3) * 2, w=4, fwd=-22)
    m.cluster("body", 0, 5, 0, 3, 3, 5, 3, lean=(-15, 0))

    m.part("head", "body", (0, 2, -10))
    m.cube("head", (-4, -3, -7), (4, 4, 0), "rock")
    m.cube("head", (-2.5, -3, -12), (2.5, 1, -7), "rock")
    m.cube("head", (-2, -4, -11), (2, -3, -7), "crystal")
    m.cube("head", (2, 1, -7.5), (4, 2, -6.5), "eye")
    m.cube("head", (-4, 1, -7.5), (-2, 2, -6.5), "eye")
    m.spike("head", 2.5, -2, -11, 6, w=2, fwd=150)
    m.spike("head", -2.5, -2, -11, 6, w=2, fwd=150)
    m.cluster("head", 0, 4, -3, 5, 4, 8, 3, lean=(-30, 0))

    m.part("tail", "body", (0, 2, 8), (25, 0, 0))
    m.cube("tail", (-2, -2, 0), (2, 2, 9), "rock")
    m.cube("tail", (-1.5, 2, 1), (1.5, 3, 8), "crystal")
    m.cluster("tail", 0, 0, 9, 7, 4, 10, 2, lean=(60, 0))

    for name, x, z in (("leg_fl", 4, -6), ("leg_fr", -4, -6), ("leg_bl", 4, 6), ("leg_br", -4, 6)):
        m.part(name, "body", (x, -3, z))
        m.cube(name, (-2, -14, -2), (2, 0, 2), "rock")
        m.cube(name, (-2.5, -14, -4), (2.5, -12, 2), "crystal")
        m.spike(name, x / abs(x) * 2, 0, 0, 5, w=2, left=x / abs(x) * 60)
    return m


def weaver():
    m = Model("crystal_weaver", "spider", seed=12)
    m.part("thorax", None, (0, 10, 0))
    m.cube("thorax", (-4, -3, -4), (4, 3, 4), "rock")
    m.cube("thorax", (-3, 3, -3), (3, 4, 3), "crystal")
    m.part("head", "thorax", (0, 0, -4))
    m.cube("head", (-3, -3, -6), (3, 3, 0), "rock")
    for x, y in ((-2, 1), (0, 1), (2, 1), (-1, -1), (1, -1)):
        m.cube("head", (x - 0.5, y, -6.5), (x + 0.5, y + 1, -5.5), "eye")
    m.spike("head", 2, -3, -5, 5, w=2, fwd=140, left=-10)
    m.spike("head", -2, -3, -5, 5, w=2, fwd=140, left=10)
    m.cluster("head", 0, 3, -2, 3, 3, 6, 2, lean=(-20, 0))

    m.part("abdomen", "thorax", (0, 0, 4))
    m.cube("abdomen", (-5, -4, 0), (5, 5, 12), "rock")
    m.cube("abdomen", (-3, -5, 3), (3, -4, 10), "glow")
    m.cube("abdomen", (-2, 5, 2), (2, 6, 10), "crystal")
    m.cluster("abdomen", 0, 5, 6, 9, 5, 12, 4, lean=(-12, 0))
    m.cluster("abdomen", 0, 0, 12, 4, 4, 8, 2, lean=(70, 0))

    yaws = [40, 15, -15, -40]
    zs = [-2.5, -0.8, 0.9, 2.6]
    for i in range(4):
        for side, sx in (("l", 1), ("r", -1)):
            n = f"leg_{side}{i}"
            m.part(n, "thorax", (sx * 3, 0, zs[i]), (0, sx * yaws[i], sx * 35))
            lo, hi = (0, 10) if sx > 0 else (-10, 0)
            m.cube(n, (lo, -1, -1), (hi, 1, 1), "crystal")
            b = n + "_b"
            m.part(b, n, (sx * 10, 0, 0), (0, 0, sx * 55))
            lo, hi = (0, 12) if sx > 0 else (-12, 0)
            m.cube(b, (lo, -1, -1), (hi, 1, 1), "bright")
    return m


def walker():
    m = Model("shard_walker", "biped", seed=13)
    m.part("torso", None, (0, 26, 0))
    m.cube("torso", (-8, 0, -5), (8, 18, 5), "rock")
    m.cube("torso", (-3, 7, -5.6), (3, 13, -5), "glow")
    m.cube("torso", (-5, 5, -5.4), (5, 7, -5), "crystal")
    m.cube("torso", (-5, 13, -5.4), (5, 15, -5), "crystal")
    m.cube("torso", (-6, -3, -4), (6, 0, 4), "dark")
    m.cluster("torso", 0, 18, 4, 10, 8, 18, 6, lean=(-20, 0), w=(4, 6))
    m.spike("torso", 9, 17, 0, 16, w=6, left=35)
    m.spike("torso", -9, 17, 0, 16, w=6, left=-35)

    m.part("head", "torso", (0, 18, -1))
    m.cube("head", (-5, 0, -5), (5, 9, 4), "rock")
    m.cube("head", (-3, 4, -5.5), (-1, 5, -4.5), "eye")
    m.cube("head", (1, 4, -5.5), (3, 5, -4.5), "eye")
    m.cube("head", (-3, 1, -5.5), (3, 2, -4.5), "dark")
    m.spike("head", 0, 9, 0, 9, w=4)
    m.spike("head", 3, 9, 1, 7, w=2, left=25, fwd=-15)
    m.spike("head", -3, 9, 1, 7, w=2, left=-25, fwd=-15)

    for side, sx in (("l", 1), ("r", -1)):
        a = f"arm_{side}"
        m.part(a, "torso", (sx * 11, 16, 0))
        m.cube(a, (-3, -14, -3), (3, 0, 3), "rock")
        m.cube(a, (-4, 0, -4), (4, 3, 4), "crystal")
        m.spike(a, sx * 3, 3, 0, 8, w=4, left=sx * 40)
        low = a + "_low"
        m.part(low, a, (0, -14, 0))
        m.cube(low, (-3.5, -14, -3.5), (3.5, 0, 3.5), "rock") if False else m.cube(low, (-3, -14, -3), (3, 0, 3), "crystal")
        for dx in (-2, 0, 2):
            m.spike(low, dx, -14, -1, 8, w=2, fwd=170)
        l = f"leg_{side}"
        m.part(l, "torso", (sx * 4.5, 0, 0))
        m.cube(l, (-3.5, -26, -3.5), (3.5, 0, 3.5), "rock")
        m.cube(l, (-4, -28, -6), (4, -26, 3), "dark")
        m.spike(l, sx * 4, -12, 0, 8, w=4, left=sx * 55)
    return m


def lurker():
    m = Model("void_lurker", "raptor", seed=14)
    m.part("torso", None, (0, 19, 0), (8, 0, 0))
    m.cube("torso", (-3, -3, -9), (3, 4, 6), "rock")
    m.cube("torso", (-2, -4, -6), (2, -3, 3), "crystal")
    for i, z in enumerate(range(-7, 6, 3)):
        m.spike("torso", 0, 4, z, 5 + (i % 2) * 2, w=2, fwd=-35)
    m.part("head", "torso", (0, 3, -9), (-8, 0, 0))
    m.cube("head", (-3, -3, -7), (3, 3, 0), "rock")
    m.cube("head", (-2, -2, -12), (2, 1, -7), "rock")
    m.cube("head", (-2, -3, -11), (2, -2, -7), "crystal")
    m.cube("head", (-3.5, 1, -6), (-2.5, 3, -4), "eye")
    m.cube("head", (2.5, 1, -6), (3.5, 3, -4), "eye")
    m.spike("head", 0, 3, -1, 8, w=2, fwd=-55)
    m.spike("head", 2, 3, -2, 6, w=2, fwd=-50, left=15)
    m.spike("head", -2, 3, -2, 6, w=2, fwd=-50, left=-15)
    for side, sx in (("l", 1), ("r", -1)):
        a = f"arm_{side}"
        m.part(a, "torso", (sx * 3.5, 1, -6))
        m.cube(a, (-1.5, -7, -1.5), (1.5, 0, 1.5), "rock")
        m.spike(a, 0, -7, -1, 12, w=2, fwd=150)
        l = f"leg_{side}"
        m.part(l, "torso", (sx * 3.5, -2, 3))
        m.cube(l, (-2, -17, -2.5), (2, 0, 2.5), "rock")
        m.cube(l, (-2.5, -17, -6), (2.5, -15, 2), "crystal")
        m.spike(l, sx * 2, -8, 0, 6, w=2, left=sx * 60)
    m.part("tail", "torso", (0, 0, 6), (-8, 0, 0))
    m.cube("tail", (-2, -2, 0), (2, 2, 10), "rock")
    m.part("tail_b", "tail", (0, 0, 10))
    m.cube("tail_b", (-1.5, -1.5, 0), (1.5, 1.5, 9), "rock")
    m.cube("tail_b", (-1, -3, 6), (1, 3, 15), "bright")
    return m


def colossus():
    m = Model("geode_colossus", "biped", seed=15)
    m.part("torso", None, (0, 42, 0))
    m.cube("torso", (-13, 0, -8), (13, 28, 8), "rock")
    m.cube("torso", (-5, 8, -8.7), (5, 19, -8), "glow")
    m.cube("torso", (-8, 6, -8.4), (8, 8, -8), "crystal")
    m.cube("torso", (-8, 19, -8.4), (8, 21, -8), "crystal")
    m.cube("torso", (-8, 8, -8.4), (-5, 19, -8), "crystal")
    m.cube("torso", (5, 8, -8.4), (8, 19, -8), "crystal")
    m.cube("torso", (-9, -4, -6), (9, 0, 6), "dark")
    m.cluster("torso", 0, 28, 4, 14, 10, 26, 9, lean=(-18, 0), w=(4, 6, 6))
    m.cluster("torso", 8, 28, -2, 4, 6, 14, 4, lean=(0, 20))
    m.cluster("torso", -8, 28, -2, 4, 6, 14, 4, lean=(0, -20))
    m.part("head", "torso", (0, 28, -2))
    m.cube("head", (-6, 0, -6), (6, 10, 6), "rock")
    m.cube("head", (-4, 6, -6.5), (-1, 7.5, -5.5), "eye") if False else m.cube("head", (-4, 6, -6.5), (-1, 7, -5.5), "eye")
    m.cube("head", (1, 6, -6.5), (4, 7, -5.5), "eye")
    m.cube("head", (-4, 2, -6.5), (4, 3, -5.5), "glow")
    m.cube("head", (-4, 3, -6.5), (-3, 4, -5.5), "dark")
    m.spike("head", 5, 9, 0, 12, w=4, left=35)
    m.spike("head", -5, 9, 0, 12, w=4, left=-35)
    m.spike("head", 0, 10, 0, 10, w=4)
    for side, sx in (("l", 1), ("r", -1)):
        a = f"arm_{side}"
        m.part(a, "torso", (sx * 18, 26, 0))
        m.cube(a, (-5, -18, -5), (5, 0, 5), "rock")
        m.cube(a, (-6, 0, -6), (6, 5, 6), "crystal")
        m.spike(a, sx * 4, 5, 0, 12, w=6, left=sx * 30)
        low = a + "_low"
        m.part(low, a, (0, -18, 0))
        m.cube(low, (-6, -20, -6), (6, 0, 6), "rock")
        m.cube(low, (-7, -22, -7), (7, -18, 7), "crystal")
        for dx in (-4, 0, 4):
            m.spike(low, dx, -22, -3, 7, w=2, fwd=170)
        l = f"leg_{side}"
        m.part(l, "torso", (sx * 8, 0, 0))
        m.cube(l, (-6, -42, -6), (6, 0, 6), "rock")
        m.cube(l, (-7, -44, -10), (7, -40, 7), "dark")
        m.spike(l, sx * 6, -18, 0, 10, w=4, left=sx * 55)
    return m


def drifter():
    m = Model("celestial_drifter", "flyer", seed=16)
    m.part("body", None, (0, 44, 0))
    m.cube("body", (-6, -10, -6), (6, 10, 6), "rock")
    m.cube("body", (-3, -6, -6.7), (3, 4, -6), "glow")
    m.cube("body", (-4, -11, -4), (4, -10, 4), "crystal")
    m.part("head", "body", (0, 10, 0))
    m.cube("head", (-5, 0, -5), (5, 8, 5), "rock")
    for x, y in ((-3, 3), (-1, 5), (1, 3), (3, 5), (-1, 1)):
        m.cube("head", (x - 0.5, y, -5.5), (x + 0.5, y + 1, -4.5), "eye")
    m.cluster("head", 0, 8, 0, 6, 4, 9, 3, lean=(-10, 0))
    for side, sx in (("l", 1), ("r", -1)):
        for i in range(6):
            n = f"wing_{side}_{i}"
            m.part(n, "body", (sx * 6, 7 - i * 2.2, 3), (0, sx * (-10 + i * 6), sx * (35 - i * 14)))
            ln = 24 - i * 2
            lo, hi = (0, ln) if sx > 0 else (-ln, 0)
            m.cube(n, (lo, -1, -2.5), (hi, 1, 2.5), "crystal")
            tl = (ln, ln + 5) if sx > 0 else (-ln - 5, -ln)
            m.cube(n, (tl[0], -0.5, -1.5), (tl[1], 0.5, 1.5), "bright")
    for i, (x, z) in enumerate(((-3, -3), (3, -3), (-4, 1), (4, 1), (-2, 4), (2, 4), (0, -1))):
        n = f"tentacle_{i}"
        m.part(n, "body", (x, -10, z))
        m.cube(n, (-1, -14, -1), (1, 0, 1), "crystal")
        m.part(n + "_b", n, (0, -14, 0))
        m.cube(n + "_b", (-1, -14, -1), (1, 0, 1), "bright")
        m.cube(n + "_b", (-0.5, -16, -0.5), (0.5, -14, 0.5), "glow")
    return m


def amitariel():
    """The apostle: a tall Minecraft-proportioned man (about 2.8 blocks) in violet robes and silver-violet
    plate, the Amethyst growing out of his shoulders, back and cheek, a crystal crown, and his spear."""
    m = Model("amitariel", "biped", seed=17)
    # torso: robe, breastplate and backplate, belt, a crystal heart in the chest
    m.part("torso", None, (0, 17, 0))
    m.cube("torso", (-5.5, 0, -3), (5.5, 17, 3), "cloth")
    m.cube("torso", (-5.8, 7, -3.5), (5.8, 16.5, -2.9), "armor")
    m.cube("torso", (-5.8, 7, 2.9), (5.8, 16.5, 3.5), "armor")
    m.cube("torso", (-4, 16.3, -3.6), (4, 17.2, 3.6), "gold")
    m.cube("torso", (-2.2, 9.5, -4.2), (2.2, 14, -3.5), "bright")
    m.cube("torso", (-1.2, 10.5, -4.6), (1.2, 13, -4.1), "glow")
    m.cube("torso", (-5.9, 0, -3.4), (5.9, 2, 3.4), "gold")
    m.cube("torso", (-1, 0.3, -3.8), (1, 1.7, -3.4), "glow")
    # tabard and hip plates hanging from the belt
    m.cube("torso", (-3, -8, -3.5), (3, 0, -3.1), "cloth")
    m.cube("torso", (-3, -8.6, -3.6), (3, -8, -3.2), "glow")
    for sx in (-1, 1):
        lo, hi = (2.4, 6.1) if sx > 0 else (-6.1, -2.4)
        m.cube("torso", (lo, -5, -3.4), (hi, 0, 3.4), "armor")
    # the Amethyst bursting out of his back and left breast
    m.cluster("torso", 0, 13, 3, 5, 5, 11, 3, lean=(-30, 0), w=(2, 3))
    m.spike("torso", -4, 12, -3, 5, w=2, fwd=35, left=-25)
    m.spike("torso", -3, 14, -3, 4, w=1.5, fwd=40, left=-10)
    # head: face, white hair, eyes, a golden circlet with a crown of crystals, crystal on the cheek
    m.part("head", "torso", (0, 17, 0))
    m.cube("head", (-5, 0, -5), (5, 10, 5), "skin")
    m.cube("head", (-5.3, 7.5, -5.3), (5.3, 10.6, 5.3), "hair")
    m.cube("head", (-5.3, -5, 3.6), (5.3, 10.6, 5.6), "hair")
    m.cube("head", (-5.3, 3, -2), (-4.8, 8, 3.6), "hair")
    m.cube("head", (4.8, 3, -2), (5.3, 8, 3.6), "hair")
    m.cube("head", (-5.4, 7, -5.4), (5.4, 7.9, 5.4), "gold")
    m.cube("head", (-0.8, 7.1, -5.8), (0.8, 8.4, -5.3), "glow")
    m.cube("head", (-3.6, 4.4, -5.4), (-1.4, 5.4, -5.0), "eye")
    m.cube("head", (1.4, 4.4, -5.4), (3.6, 5.4, -5.0), "eye")
    m.cube("head", (-3.8, 5.8, -5.3), (-1.2, 6.3, -5.0), "dark")
    m.cube("head", (1.2, 5.8, -5.3), (3.8, 6.3, -5.0), "dark")
    m.cube("head", (-1.2, 2.2, -5.2), (1.2, 2.6, -5.0), "rock")
    m.cube("head", (-0.6, 3, -5.6), (0.6, 4.4, -5.0), "skin")
    m.cube("head", (3.2, 1.5, -5.4), (4.8, 3.6, -4.2), "bright")
    for x, fwd, left, h in ((0, -12, 0, 7), (2.6, -5, 22, 5), (-2.6, -5, -22, 5), (4.6, 10, 40, 4), (-4.6, 10, -40, 4)):
        m.spike("head", x, 7.9, -3.5 if abs(x) < 3 else -1, h, w=1.5, fwd=fwd, left=left)
    # arms: robe sleeves, great pauldrons crusted with crystals, armoured forearms, hands
    for side, sx in (("l", 1), ("r", -1)):
        a = f"arm_{side}"
        m.part(a, "torso", (sx * 8, 15, 0))
        m.cube(a, (-2.6, -9, -2.6), (2.6, 1, 2.6), "cloth")
        m.cube(a, (-3.8, -2.5, -3.8), (3.8, 3, 3.8), "armor")
        m.cube(a, (-4, -3, -4), (4, -2.4, 4), "gold")
        m.spike(a, sx * 1.5, 3, 0, 6, w=2.5, left=sx * 25)
        m.spike(a, sx * 2.5, 2.5, 1.5, 4, w=1.5, left=sx * 45, fwd=-20)
        m.spike(a, sx * 0.5, 3, -1.5, 3, w=1.5, left=sx * 10, fwd=25)
        low = a + "_low"
        m.part(low, a, (0, -9, 0))
        m.cube(low, (-2.9, -6.5, -2.9), (2.9, 0, 2.9), "armor")
        m.cube(low, (-3, -4, -3), (3, -3.3, 3), "glow")
        m.cube(low, (-2.4, -9, -2.4), (2.4, -6.5, 2.4), "skin")
        m.spike(low, sx * 3, -2, 0, 4, w=1.5, left=sx * 60, fwd=180)
        # legs: robe, greaves with crystal knee guards, boots trimmed in gold
        l = f"leg_{side}"
        m.part(l, "torso", (sx * 2.8, 0, 0))
        m.cube(l, (-2.7, -8, -2.7), (2.7, 0, 2.7), "cloth")
        m.cube(l, (-2.9, -15, -2.9), (2.9, -8, 2.9), "armor")
        m.cube(l, (-1.8, -10, -3.6), (1.8, -7, -2.8), "crystal")
        m.cube(l, (-3.1, -17, -3.8), (3.1, -14.5, 3.1), "dark")
        m.cube(l, (-3.2, -14.8, -3.9), (3.2, -14.3, 3.2), "gold")
        m.spike(l, sx * 3, -12, 0, 4, w=1.5, left=sx * 60)
    # the cape, with a glowing hem
    m.part("cape", "torso", (0, 16.5, 3.6), (8, 0, 0))
    m.cube("cape", (-5.6, -26, 0), (5.6, 0, 0.8), "cloth")
    m.cube("cape", (-5.6, -27, 0), (5.6, -26, 0.9), "glow")
    m.cube("cape", (-5.6, -1, 0), (5.6, 0, 1.2), "gold")
    # the spear: a long dark shaft bound in gold, a crystal crossguard and a leaf-shaped crystal blade
    m.part("spear", "arm_r_low", (0, -8, -1), (-70, 0, 0))
    m.cube("spear", (-0.6, -24, -0.6), (0.6, 31, 0.6), "dark")
    for y in (-14, -2, 10, 22):
        m.cube("spear", (-0.9, y, -0.9), (0.9, y + 1, 0.9), "gold")
    m.cube("spear", (-0.8, -4, -0.8), (0.8, 4, 0.8), "cloth")
    m.cube("spear", (-1.2, 30, -1.2), (1.2, 32.5, 1.2), "gold")
    m.cube("spear", (-4.5, 31.5, -0.6), (4.5, 32.8, 0.6), "bright")
    m.cube("spear", (-5.5, 32.8, -0.5), (-3.8, 36, 0.5), "bright")
    m.cube("spear", (3.8, 32.8, -0.5), (5.5, 36, 0.5), "bright")
    m.cube("spear", (-3, 32.5, -0.7), (3, 41, 0.7), "crystal")
    m.cube("spear", (-2, 41, -0.7), (2, 46, 0.7), "bright")
    m.cube("spear", (-1, 46, -0.6), (1, 50, 0.6), "bright")
    m.cube("spear", (-0.6, 33.5, -1), (0.6, 45, 1), "glow")
    m.cube("spear", (-0.9, -27, -0.9), (0.9, -24, 0.9), "gold")
    m.cube("spear", (-0.5, -30, -0.5), (0.5, -27, 0.5), "bright")
    return m


def ricardo():
    """Ricardo, an old travelling scholar: a wide-brimmed hat with a feather, a white beard and round
    spectacles, a long green coat with a belt of pouches, a big pack of scrolls and a lantern, boots and
    a walking staff with a little amethyst. Minecraft proportions, about two blocks tall."""
    m = Model("ricardo", "biped", seed=51)
    m.part("torso", None, (0, 12, 0))
    m.cube("torso", (-4, 0, -2), (4, 12, 2), "moss")
    m.cube("torso", (-4.3, 0, -2.3), (4.3, 1.5, 2.3), "leather")
    m.cube("torso", (-0.8, 0.2, -2.6), (0.8, 1.3, -2.2), "gold")
    m.cube("torso", (-4.4, -5, -2.4), (4.4, 0, 2.4), "moss")
    m.cube("torso", (-1, 2, -2.5), (1, 11, -2.1), "leather")
    m.cube("torso", (2.2, -1.5, -2.9), (4.2, 1, -2.2), "leather")
    m.cube("torso", (-4.2, -1.5, -2.9), (-2.4, 0.8, -2.2), "leather")
    # the pack on his back: rolled scrolls, a lantern
    m.cube("torso", (-3.5, 1, 2), (3.5, 10, 5), "leather")
    m.cube("torso", (-3, 10, 2.5), (-1, 13, 4.5), "paper")
    m.cube("torso", (1, 10, 2.5), (3, 12, 4.5), "paper")
    m.cube("torso", (2.5, 2, 5), (4.5, 5, 6.5), "gold")
    m.cube("torso", (3, 2.5, 5.3), (4, 4.5, 6.2), "glow")
    # head: face, beard, spectacles, the hat
    m.part("head", "torso", (0, 12, 0))
    m.cube("head", (-4, 0, -4), (4, 8, 4), "tan")
    m.cube("head", (-4.2, -2, -4.3), (4.2, 3, -3.6), "beard")
    m.cube("head", (-3, -3.5, -4.2), (3, -2, -3.4), "beard")
    m.cube("head", (-4.2, 1, -3.5), (-3.8, 4, -1), "beard")
    m.cube("head", (3.8, 1, -3.5), (4.2, 4, -1), "beard")
    m.cube("head", (-3.4, 4, -4.4), (-1, 5.4, -4.0), "gold")
    m.cube("head", (1, 4, -4.4), (3.4, 5.4, -4.0), "gold")
    m.cube("head", (-2.8, 4.3, -4.5), (-1.6, 5.1, -4.3), "eye")
    m.cube("head", (1.6, 4.3, -4.5), (2.8, 5.1, -4.3), "eye")
    m.cube("head", (-0.6, 2.8, -4.6), (0.6, 4.2, -4.0), "tan")
    m.cube("head", (-6.5, 7.5, -6.5), (6.5, 8.6, 6.5), "leather")
    m.cube("head", (-4.3, 8.6, -4.3), (4.3, 12, 4.3), "leather")
    m.cube("head", (-4.4, 8.6, -4.4), (4.4, 9.6, 4.4), "moss")
    m.spike("head", 3.5, 9.5, 1, 6, w=1, left=55, fwd=25, style="bright", tip="glow")
    for side, sx in (("l", 1), ("r", -1)):
        a = f"arm_{side}"
        m.part(a, "torso", (sx * 6, 11, 0))
        m.cube(a, (-2, -6, -2), (2, 1, 2), "moss")
        m.cube(a, (-2.3, -0.5, -2.3), (2.3, 1.3, 2.3), "leather")
        low = a + "_low"
        m.part(low, a, (0, -6, 0))
        m.cube(low, (-2, -4, -2), (2, 0, 2), "moss")
        m.cube(low, (-2.2, -2.5, -2.2), (2.2, -1.7, 2.2), "leather")
        m.cube(low, (-1.8, -6, -1.8), (1.8, -4, 1.8), "tan")
        l = f"leg_{side}"
        m.part(l, "torso", (sx * 2, 0, 0))
        m.cube(l, (-2, -12, -2), (2, 0, 2), "wood")
        m.cube(l, (-2.2, -12, -2.6), (2.2, -8, 2.2), "leather")
        m.cube(l, (-2.3, -9, -2.7), (2.3, -8.3, 2.3), "dark")
    # his walking staff with an amethyst on top
    m.part("staff", "arm_r_low", (0, -5, -1), (-10, 0, 0))
    m.cube("staff", (-0.6, -12, -0.6), (0.6, 18, 0.6), "wood")
    m.cube("staff", (-1, 16, -1), (1, 17, 1), "gold")
    m.cube("staff", (-1, 17, -1), (1, 20, 1), "crystal")
    m.cube("staff", (-0.5, 20, -0.5), (0.5, 21.5, 0.5), "bright")
    return m


def phantom_spear():
    """The spears Amitariel summons: his spear made of light, pointing along +X (its flight)."""
    m = Model("phantom_spear", "none", seed=44)
    m.part("spear", None, (0, 0, 0))
    m.cube("spear", (-22, -0.6, -0.6), (12, 0.6, 0.6), "dark")
    for x in (-14, -4, 6):
        m.cube("spear", (x, -0.9, -0.9), (x + 1, 0.9, 0.9), "gold")
    m.cube("spear", (11, -1.2, -1.2), (13.5, 1.2, 1.2), "gold")
    m.cube("spear", (12.5, -3.6, -0.5), (13.5, 3.6, 0.5), "bright")
    m.cube("spear", (13.5, -2, -0.6), (20, 2, 0.6), "crystal")
    m.cube("spear", (20, -1.4, -0.6), (24, 1.4, 0.6), "bright")
    m.cube("spear", (24, -0.7, -0.5), (27.5, 0.7, 0.5), "bright")
    m.cube("spear", (14.5, -0.5, -0.8), (23, 0.5, 0.8), "glow")
    m.cube("spear", (-24, -0.9, -0.9), (-22, 0.9, 0.9), "glow")
    return m


def heart():
    """The Amethyst Heart, drawn at 4x scale in game (1 px here = 1/4 block). Feet are 1 block above the
    arena floor (floor = y -4 here). ~11 blocks wide, roots into the floor, tendrils across the arena,
    four glowing weak-point nodes at the roots (matching the HeartNodeEntity offsets: 4.6 blocks out,
    0.9 up) and a ring of orbiting shards."""
    import math
    m = Model("amethyst_heart", "heart", seed=18)
    m.part("core", None, (0, 26, 0))
    # A rounded body: every row is split into segments, the middle ones bulge out further.
    rows = [(-18, -14, [(-3, 3)]), (-14, -10, [(-8, 8)]), (-10, -6, [(-13, 13)]), (-6, -2, [(-17, 17)]),
            (-2, 2, [(-20, 20)]), (2, 6, [(-20, 20)]), (6, 10, [(-20, 20)]), (10, 14, [(-19, -3), (3, 19)]),
            (14, 18, [(-16, -6), (6, 16)])]
    for i, (y0, y1, spans) in enumerate(rows):
        depth = 9 if 2 <= i <= 6 else 7
        for x0, x1 in spans:
            width = x1 - x0
            cuts = [x0, x0 + round(width * 0.2), x0 + round(width * 0.4), x1 - round(width * 0.4), x1 - round(width * 0.2), x1]
            for k in range(5):
                lo, hi = cuts[k], cuts[k + 1]
                if hi <= lo:
                    continue
                d = depth - (2 if k in (0, 4) else 1 if k in (1, 3) else 0)
                m.cube("core", (lo, y0, -d), (hi, y1, d), "rock" if (i + k) % 3 else "crystal")
    # the light of the core shows only through the seams between armour plates
    for z in (-1, 1):
        zin, zout = (9.0, 9.6) if z > 0 else (-9.6, -9.0)
        m.cube("core", (-9.5, -10.5, zin), (9.5, 9.5, zout), "glow")
        k = 0
        for (x0, x1) in ((-9.5, -3.5), (-2.5, 2.5), (3.5, 9.5)):
            for (y0, y1) in ((-10.5, -4.5), (-3.5, 2.5), (3.5, 9.5)):
                if x0 == -2.5 and y0 == -3.5:
                    continue
                p0, p1 = (9.6, 11.2) if z > 0 else (-11.2, -9.6)
                cx, cy, cz = (x0 + x1) / 2, (y0 + y1) / 2, (p0 + p1) / 2
                # each armour plate is its own part, so it can slide out when the Heart opens
                part = f"plate_{'b' if z > 0 else 'f'}_{k}"
                m.part(part, "core", (cx, cy, cz))
                m.cube(part, (x0 - cx, y0 - cy, p0 - cz), (x1 - cx, y1 - cy, p1 - cz), "crystal" if (x0 + y0) % 2 else "bright")
                k += 1
        # the heart-gem in the middle, standing out
        g0, g1 = (9.6, 12.4) if z > 0 else (-12.4, -9.6)
        m.cube("core", (-3, -3.5, g0), (3, 2.5, g1), "bright")
        h0, h1 = (12.4, 13.6) if z > 0 else (-13.6, -12.4)
        m.cube("core", (-2, -2.5, h0), (2, 1.5, h1), "glow")
    # rounded crystal caps on the two lobes, and seams of light along the sides
    for sx in (-1, 1):
        m.cube("core", (sx * 11 - 4, 18, -5), (sx * 11 + 4, 20, 5), "bright")
        m.cube("core", (sx * 11 - 2, 20, -3), (sx * 11 + 2, 21, 3), "glow")
        m.cube("core", (sx * 20.5 - 0.5 if sx > 0 else -21, -2, -6), (sx * 20.5 + 0.5 if sx > 0 else -20, 9, 6), "glow")
    m.cluster("core", 11, 20, 0, 3, 8, 14, 3, lean=(0, 15), w=(4,))
    m.cluster("core", -11, 20, 0, 3, 8, 14, 3, lean=(0, -15), w=(4,))
    m.cluster("core", 20, 4, 0, 4, 10, 20, 5, lean=(0, 75), w=(4, 6))
    m.cluster("core", -20, 4, 0, 4, 10, 20, 5, lean=(0, -75), w=(4, 6))
    # aortas: great crystal vessels rising from the top of the lobes
    for i, (x, z, h, lean) in enumerate(((-9, -2, 18, -14), (8, 3, 22, 12), (-1, 4, 14, 4))):
        n_ = f"aorta_{i}"
        m.part(n_, "core", (x, 17, z), (-8, 0, lean))
        m.cube(n_, (-3.5, 0, -3.5), (3.5, h * 0.55, 3.5), "dark")
        m.cube(n_, (-3, h * 0.55, -3), (3, h * 0.8, 3), "crystal")
        m.cube(n_, (-2, h * 0.8, -2), (2, h, 2), "bright")
        m.cube(n_, (-1, h, -1), (1, h + 3, 1), "glow")
    # roots: from the body down into the floor
    for i in range(6):
        a = math.radians(i * 60 + 30)
        n_ = f"sway_vein_{i}"
        m.part(n_, None, (math.cos(a) * 8, 12, math.sin(a) * 8), (math.sin(a) * 38, 0, -math.cos(a) * 38))
        m.cube(n_, (-2, -12, -2), (2, 0, 2), "dark")
        m.cube(n_, (-0.5, -12, -2.4), (0.5, 0, -2), "glow")
        m.part(n_ + "_b", n_, (0, -12, 0))
        m.cube(n_ + "_b", (-1.5, -9, -1.5), (1.5, 0, 1.5), "dark")
    # tendrils across the arena floor
    for i in range(8):
        n_ = f"tendril_{i}"
        m.part(n_, None, (0, -3, 0), (0, i * 45 + 22.5, 0))
        m.cube(n_, (16, -1, -1.5), (76, 1, 1.5), "dark")
        for k in range(4):
            m.cube(n_, (24 + k * 14, 1, -0.5), (27 + k * 14, 1.6, 0.5), "glow")
    # weak-point nodes (static, they do not pulse with the core)
    for i, (x, z) in enumerate(((18.4, 0), (-18.4, 0), (0, 18.4), (0, -18.4))):
        # a crystal shell seals each node; it lifts away when the Heart opens
        sh = f"node_shell_{i}"
        m.part(sh, None, (x, 4, z))
        m.cube(sh, (-3.5, -3.5, -3.5), (3.5, 3.5, 3.5), "crystal")
        m.cube(sh, (-2.5, 3.5, -2.5), (2.5, 5.5, 2.5), "bright")
        m.cube(sh, (-1, 5.5, -1), (1, 8.5, 1), "bright")
        n_ = f"node_{i}"
        m.part(n_, None, (x, 4, z))
        m.cube(n_, (-2.5, -2.5, -2.5), (2.5, 2.5, 2.5), "glow")
        m.cube(n_, (-3, -3, -1), (3, -2, 1), "bright")
        m.cube(n_, (-1, -3, -3), (1, -2, 3), "bright")
        m.cube(n_, (-1, 2.5, -1), (1, 4.5, 1), "bright")
    # phase 2 weak points: wounds where the lower armour plates (plate k0 / k5) broke off, front and back.
    # Offsets from the feet in blocks: (+-1.6, 4.6, +-2.8), i.e. core-relative px (+-6.5, -7.6, +-11.2).
    for i, (x, z) in enumerate(((6.5, -1), (-6.5, -1), (6.5, 1), (-6.5, 1))):
        # sunk into the body where the plate was, flush with the glowing layer under the plates
        n_ = f"weak2_{i}"
        m.part(n_, "core", (x, -7.5, z * 9.0))
        m.cube(n_, (-2.8, -2.8, -1.6), (2.8, 2.8, 1.6), "glow")
        e0, e1 = sorted((z * 1.0, z * 2.0))
        m.cube(n_, (-1.2, -1.2, e0), (1.2, 1.2, e1), "eye")
        for (cx, cy) in ((-2.4, 2.0), (2.0, -2.4)):
            m.cube(n_, (cx, cy, -1.8), (cx + 0.8, cy + 0.8, 1.8), "bright")
        c_ = f"weak2_cap_{i}"
        m.part(c_, "core", (x, -7.5, z * 10.4))
        m.cube(c_, (-3.0, -3.0, -0.8), (3.0, 3.0, 0.8), "crystal")
    # phase 3 weak points: crystal veins hanging under the belly. Offsets from the feet in blocks:
    # (+-2.4, 2.6, 0) and (0, 2.6, +-2.0), i.e. core-relative px (+-9.6, -15.6, 0) and (0, -15.6, +-8).
    for i, (x, z) in enumerate(((9.6, 0), (-9.6, 0), (0, 8), (0, -8))):
        n_ = f"weak3_{i}"
        m.part(n_, "core", (x, -15.6, z))
        m.cube(n_, (-1.6, 0, -1.6), (1.6, 6, 1.6), "dark")
        m.cube(n_, (-2.2, -2.4, -2.2), (2.2, 1.6, 2.2), "glow")
        m.cube(n_, (-1.0, -4.4, -1.0), (1.0, -2.4, 1.0), "eye")
        c_ = f"weak3_cap_{i}"
        m.part(c_, "core", (x, -15.6, z))
        m.cube(c_, (-2.8, -3.0, -2.8), (2.8, 2.2, 2.8), "crystal")
    # a second, tilted ring of shards turning the other way
    m.part("spin_ring2", None, (0, 26, 0), (0, 0, 18))
    for i in range(7):
        a = i * 2 * math.pi / 7
        n_ = f"ring2_shard_{i}"
        m.part(n_, "spin_ring2", (52 * math.cos(a), 0, 52 * math.sin(a)), (0, i * 51, 30))
        m.cube(n_, (-1.5, -5, -1.5), (1.5, 5, 1.5), "crystal")
        m.cube(n_, (-0.5, 5, -0.5), (0.5, 9, 0.5), "glow")
    # floating rune plates
    for i in range(4):
        a = i * math.pi / 2 + math.pi / 4
        n_ = f"rune_{i}"
        m.part(n_, None, (30 * math.cos(a), 46, 30 * math.sin(a)), (0, -math.degrees(a) + 90, 0))
        m.cube(n_, (-4, -6, -0.5), (4, 6, 0.5), "dark")
        # the eye of the Amethyst: a diamond with a glowing pupil
        for (x0, y0) in ((-0.5, 3), (-1.5, 2), (0.5, 2), (-2.5, 1), (1.5, 1), (-3, 0), (2, 0),
                         (-2.5, -1), (1.5, -1), (-1.5, -2), (0.5, -2), (-0.5, -3)):
            m.cube(n_, (x0, y0, -0.8), (x0 + 1, y0 + 1, 0.8), "glow")
        m.cube(n_, (-0.5, -0.5, -1.0), (0.5, 0.5, 1.0), "eye")
    # shards circling the heart
    m.part("spin_ring", None, (0, 26, 0))
    for i in range(10):
        a = i * math.pi / 5
        n_ = f"ring_shard_{i}"
        m.part(n_, "spin_ring", (40 * math.cos(a), (i % 3 - 1) * 7, 40 * math.sin(a)), (20 * (i % 3), i * 36, 0))
        m.cube(n_, (-2, -7, -2), (2, 7, 2), "bright")
        m.cube(n_, (-1, 7, -1), (1, 13, 1), "glow")
        m.cube(n_, (-1, -12, -1), (1, -7, 1), "crystal")
    return m


def heart_pylon():
    """One of the five crystals that shield the Heart (3.5 blocks tall)."""
    m = Model("heart_pylon", "crystal", seed=31)
    m.part("base", None, (0, 0, 0))
    m.cube("base", (-6, 0, -6), (6, 3, 6), "rock")
    m.cube("base", (-5, 3, -5), (5, 4, 5), "dark")
    m.part("bob", None, (0, 8, 0))
    m.cube("bob", (-4, 0, -4), (4, 30, 4), "crystal")
    m.cube("bob", (-2, 30, -2), (2, 44, 2), "bright")
    m.cube("bob", (-2.5, 8, -4.6), (2.5, 22, 4.6), "glow")
    m.cube("bob", (-4.6, 8, -2.5), (4.6, 22, 2.5), "glow")
    m.cluster("bob", 0, 2, 0, 6, 8, 16, 4, lean=(0, 0), w=(2, 4))
    m.part("spin_ring", None, (0, 22, 0))
    for i in range(4):
        n_ = f"spin_ring_{i}"
        import math
        a = i * math.pi / 2
        m.part(n_, "spin_ring", (11 * math.cos(a), 0, 11 * math.sin(a)), (25, i * 90, 0))
        m.cube(n_, (-1, -4, -1), (1, 4, 1), "glow")
    return m


def heart_sentinel():
    """A floating crystal summoned by the Heart (about 1.7 blocks tall)."""
    m = Model("heart_sentinel", "crystal", seed=32)
    m.part("bob", None, (0, 12, 0))
    m.cube("bob", (-4, -6, -4), (4, 6, 4), "crystal")
    m.cube("bob", (-2.5, 6, -2.5), (2.5, 13, 2.5), "bright")
    m.cube("bob", (-1, 13, -1), (1, 17, 1), "glow")
    m.cube("bob", (-2.5, -12, -2.5), (2.5, -6, 2.5), "bright")
    m.cube("bob", (-1, -15, -1), (1, -12, 1), "glow")
    m.cube("bob", (-2, -2, -4.6), (2, 2, 4.6), "glow")
    m.part("spin_ring", None, (0, 12, 0))
    for i in range(3):
        import math
        a = i * 2 * math.pi / 3
        n_ = f"spin_ring_{i}"
        m.part(n_, "spin_ring", (8 * math.cos(a), 0, 8 * math.sin(a)), (0, i * 120, 30))
        m.cube(n_, (-1, -3, -1), (1, 3, 1), "bright")
    return m


def infection_core():
    """The crystal at the heart of a patch of infection; one model holds its three phases (p1, p2, p3),
    the game shows only the current one. Phase 1 is about 1.3 blocks tall, phase 2 about 2.3, phase 3 3.4."""
    import math
    m = Model("infection_core", "core", seed=51)
    # phase 1: a lone crystal on a small rock, easy to break
    m.part("p1", None, (0, 0, 0))
    m.cube("p1", (-5, 0, -5), (5, 2, 5), "rock")
    m.cube("p1", (-3, 2, -3), (3, 3, 3), "dark")
    m.spike("p1", 3, 2, 1, 6, w=2, left=30, fwd=-5)
    m.spike("p1", -3, 2, -1, 5, w=2, left=-30)
    m.spike("p1", 0.5, 2, 3.5, 4, w=2, fwd=-35)
    m.part("p1_heart", "p1", (0, 3, 0))
    m.cube("p1_heart", (-2, 0, -2), (2, 11, 2), "crystal")
    m.cube("p1_heart", (-1, 11, -1), (1, 16, 1), "bright")
    m.cube("p1_heart", (-2.4, 3, -0.5), (2.4, 7, 0.5), "glow")
    # phase 2: a tall crystal in a ring of smaller ones, three shards circling it
    m.part("p2", None, (0, 0, 0))
    m.cube("p2", (-8, 0, -8), (8, 3, 8), "rock")
    m.cube("p2", (-6, 3, -6), (6, 4, 6), "dark")
    m.cluster("p2", 0, 3, 0, 7, 6, 14, 5.5, w=(2, 4))
    m.part("p2_heart", "p2", (0, 4, 0))
    m.cube("p2_heart", (-3, 0, -3), (3, 22, 3), "crystal")
    m.cube("p2_heart", (-2, 22, -2), (2, 30, 2), "bright")
    m.cube("p2_heart", (-1, 30, -1), (1, 33, 1), "glow")
    m.cube("p2_heart", (-3.6, 6, -1), (3.6, 16, 1), "glow")
    m.cube("p2_heart", (-1, 6, -3.6), (1, 16, 3.6), "glow")
    m.part("p2_ring", "p2", (0, 18, 0))
    for i in range(3):
        a = i * 2 * math.pi / 3
        n_ = f"p2_ring_{i}"
        m.part(n_, "p2_ring", (9 * math.cos(a), 0, 9 * math.sin(a)), (20, i * 120, 0))
        m.cube(n_, (-1, -3, -1), (1, 3, 1), "bright")
    # phase 3: a monolith with a glowing eye, a crown of crystals and four great shards circling it
    m.part("p3", None, (0, 0, 0))
    m.cube("p3", (-12, 0, -12), (12, 4, 12), "rock")
    m.cube("p3", (-9, 4, -9), (9, 6, 9), "dark")
    m.cube("p3", (-14, 0, -4), (14, 2, 4), "dark")
    m.cube("p3", (-4, 0, -14), (4, 2, 14), "dark")
    m.cluster("p3", 0, 5, 0, 12, 10, 22, 9, w=(2, 4, 6))
    m.part("p3_heart", "p3", (0, 6, 0))
    m.cube("p3_heart", (-5, 0, -5), (5, 32, 5), "crystal")
    m.cube("p3_heart", (-3, 32, -3), (3, 42, 3), "bright")
    m.cube("p3_heart", (-1.5, 42, -1.5), (1.5, 48, 1.5), "glow")
    m.cube("p3_heart", (-5.6, 8, -1.5), (5.6, 24, 1.5), "glow")
    m.cube("p3_heart", (-1.5, 8, -5.6), (1.5, 24, 5.6), "glow")
    m.cube("p3_heart", (-3, 14, -6), (3, 18, -5), "eye")
    m.part("p3_ring", "p3", (0, 28, 0))
    for i in range(4):
        a = i * math.pi / 2
        n_ = f"p3_ring_{i}"
        m.part(n_, "p3_ring", (14 * math.cos(a), 0, 14 * math.sin(a)), (25, i * 90, 0))
        m.cube(n_, (-1.5, -5, -1.5), (1.5, 5, 1.5), "bright")
        m.cube(n_, (-0.5, 5, -0.5), (0.5, 8, 0.5), "glow")
    return m


def infection_core_dim():
    """A dying core (after the Heart's fall): grey, cracked, its light almost out."""
    from PIL import Image
    from geo import RES
    for suffix in ("", "_glow"):
        img = Image.open(RES / "textures/entity" / f"infection_core{suffix}.png").convert("RGBA")
        px = img.load()
        for x in range(img.width):
            for y in range(img.height):
                r, g, b, a = px[x, y]
                if a:
                    v = (r + g + b) // 3
                    if suffix:
                        px[x, y] = (v // 4, v // 5, v // 4, a)
                    else:
                        px[x, y] = (int(v * 0.6 + r * 0.15), int(v * 0.6 + g * 0.1), int(v * 0.65 + b * 0.15), a)
        img.save(RES / "textures/entity" / f"infection_core_dim{suffix}.png")


def heart_variants():
    """Each phase the Heart glows hotter: violet, pink, red, white-hot."""
    from PIL import Image
    from geo import RES
    tints = {2: (1.15, 0.75, 0.95), 3: (1.3, 0.55, 0.65), 4: (1.45, 0.75, 0.6)}
    for stage, (fr, fg, fb) in tints.items():
        for suffix in ("", "_glow"):
            img = Image.open(RES / "textures/entity" / f"amethyst_heart{suffix}.png").convert("RGBA")
            px = img.load()
            for x in range(img.width):
                for y in range(img.height):
                    r, g, b, a = px[x, y]
                    if a:
                        boost = 30 * (stage - 1) if suffix else 0
                        px[x, y] = (min(255, int(r * fr) + boost), min(255, int(g * fg) + boost // 2), min(255, int(b * fb)), a)
            img.save(RES / "textures/entity" / f"amethyst_heart_p{stage}{suffix}.png")


ARMOR_PARTS = ("helm", "chest", "chest_arm_l", "chest_arm_r", "legs_l", "legs_r", "boots_l", "boots_r")


def armor_amethyst():
    """Crystal growths worn on top of the amethyst armor; each part hangs off a player body part."""
    m = Model("armor_amethyst", "none", seed=21)
    for p in ARMOR_PARTS:
        m.part(p, None, (0, 24, 0))
    m.cluster("helm", 0, 9, 0, 5, 4, 9, 2.5, lean=(-10, 0), w=(2, 4))
    m.spike("helm", 4.8, 7, 0, 7, w=2, left=55)
    m.spike("helm", -4.8, 7, 0, 7, w=2, left=-55)
    m.cube("helm", (-1.5, 4.5, -5.7), (1.5, 6.5, -4.9), "glow")
    m.cluster("chest", 0, -3, 3.2, 6, 6, 13, 3.2, lean=(-65, 0))
    m.cube("chest", (-2.5, -8.5, -3.4), (2.5, -3.5, -2.9), "crystal")
    m.cube("chest", (-1.5, -7.5, -3.7), (1.5, -4.5, -3.3), "glow")
    for side, sx in (("l", 1), ("r", -1)):
        arm = f"chest_arm_{side}"
        m.box(arm, sx * 1.0, 2.6, 0, 6.4, 2, 6.4, "bright")
        m.box(arm, sx * 1.0, 4.6, 0, 4.4, 1, 4.4, "crystal")
        m.spike(arm, sx * 2.5, 5.6, 0, 8, w=4, left=sx * 32)
        m.spike(arm, sx * 0.0, 5.6, -1.5, 6, w=2, left=sx * 10, fwd=25)
        leg = f"legs_{side}"
        m.box(leg, 0, -7, -2.9, 5.6, 3, 1, "bright")
        m.spike(leg, 0, -5.8, -3.4, 6, w=2, fwd=70)
        m.spike(leg, sx * 2.9, -2, 0, 7, w=2, left=sx * 40)
        boot = f"boots_{side}"
        m.spike(boot, 0, -11, -2.8, 6, w=2, fwd=75)
        m.spike(boot, sx * 2.6, -10.5, 0, 5, w=2, left=sx * 65)
        m.spike(boot, 0, -11, 2.6, 5, w=2, fwd=-70)
    return m


def armor_purified():
    """A halo, wings of light and gilded plates worn on top of the purifier's armor."""
    m = Model("armor_purified", "none", seed=22)
    for p in ARMOR_PARTS:
        m.part(p, None, (0, 24, 0))
    m.cube("helm", (-6, 11, -6), (6, 12, -5), "gold")
    m.cube("helm", (-6, 11, 5), (6, 12, 6), "gold")
    m.cube("helm", (-6, 11, -5), (-5, 12, 5), "gold")
    m.cube("helm", (5, 11, -5), (6, 12, 5), "gold")
    m.cube("helm", (-5.4, 11.2, -5.4), (5.4, 11.8, -5.1), "paleglow")
    m.spike("helm", 4.8, 6, 0, 8, w=2, left=65, style="pale", tip="paleglow")
    m.spike("helm", -4.8, 6, 0, 8, w=2, left=-65, style="pale", tip="paleglow")
    m.cube("helm", (-3, 3.5, -5.7), (3, 4.5, -4.9), "paleglow")
    for side, sx in (("l", 1), ("r", -1)):
        for i, (h, w) in enumerate(((17, 4), (15, 4), (12, 4), (9, 2))):
            m.spike("chest", sx * 2.0, -2.5 - i * 1.4, 3.3, h, w=w, fwd=-35 - i * 4, left=sx * (28 + i * 17), style="pale", tip="paleglow")
        arm = f"chest_arm_{side}"
        m.box(arm, sx * 1.0, 2.6, 0, 6.6, 2, 6.6, "gold")
        m.spike(arm, sx * 2.5, 4.6, 0, 6, w=4, left=sx * 25, style="pale", tip="paleglow")
        leg = f"legs_{side}"
        m.box(leg, 0, -7, -2.9, 5.6, 3, 1, "gold")
        m.spike(leg, 0, -5.8, -3.4, 5, w=2, fwd=70, style="pale", tip="paleglow")
        boot = f"boots_{side}"
        m.box(boot, 0, -11.6, -2.9, 5, 2, 1.2, "gold")
        m.spike(boot, sx * 2.6, -10.5, 0, 6, w=2, left=sx * 70, style="pale", tip="paleglow")
        m.spike(boot, sx * 2.6, -10.0, 1.2, 5, w=2, left=sx * 55, fwd=-40, style="pale", tip="paleglow")
    m.cube("chest", (-2.5, -8.5, -3.4), (2.5, -3.5, -2.9), "gold")
    m.cube("chest", (-1.5, -7.5, -3.7), (1.5, -4.5, -3.3), "paleglow")
    return m


def crystal_spike():
    """A crystal bursting out of the floor (scaled in game by the effect's size)."""
    m = Model("crystal_spike", "none", seed=41)
    m.part("base", None, (0, 0, 0))
    m.cube("base", (-6, 0, -6), (6, 2, 6), "rock")
    m.cube("base", (-3, 0, -3), (3, 14, 3), "crystal")
    m.cube("base", (-2, 14, -2), (2, 20, 2), "bright")
    m.cube("base", (-1, 20, -1), (1, 25, 1), "glow")
    m.cube("base", (-3.4, 4, -0.5), (3.4, 9, 0.5), "glow")
    for x, z, h, left, fwd in ((4, 1, 11, 28, 6), (-4, -1, 9, -30, -6), (1, 4, 8, 5, -28), (-1, -4, 10, -5, 26)):
        m.spike("base", x, 1, z, h, w=4, left=left, fwd=fwd)
    return m


def rune_ring():
    """A glowing rune circle drawn on the floor: one unit of radius is one block."""
    import math
    m = Model("rune_ring", "none", seed=42)
    m.part("ring", None, (0, 0.2, 0))
    for i in range(24):
        a = i * 2 * math.pi / 24
        x, z = 15 * math.cos(a), 15 * math.sin(a)
        m.cube("ring", (x - 1, 0, z - 1), (x + 1, 1, z + 1), "glow")
    for i in range(6):
        a = i * 2 * math.pi / 6
        x, z = 9 * math.cos(a), 9 * math.sin(a)
        m.cube("ring", (x - 1.5, 0, z - 0.5), (x + 1.5, 1, z + 0.5), "glow")
        m.cube("ring", (x - 0.5, 0, z - 1.5), (x + 0.5, 1, z + 1.5), "glow")
    m.cube("ring", (-3, 0, -0.5), (3, 1, 0.5), "glow")
    m.cube("ring", (-0.5, 0, -3), (0.5, 1, 3), "glow")
    m.cube("ring", (-1, 0, -1), (1, 1.4, 1), "eye")
    return m


def shard_bolt():
    """The crystal shard creatures and bosses shoot, pointing along +X (its flight)."""
    m = Model("shard_bolt", "none", seed=43)
    m.part("bolt", None, (0, 0, 0))
    m.cube("bolt", (-6, -1, -1), (4, 1, 1), "crystal")
    m.cube("bolt", (-2, -1.5, -1.5), (1, 1.5, 1.5), "bright")
    m.cube("bolt", (4, -0.5, -0.5), (9, 0.5, 0.5), "glow")
    m.cube("bolt", (-9, -0.5, -0.5), (-6, 0.5, 0.5), "bright")
    return m


# ---- extra detail layered on top of the base models ----------------------------------------

def _stalker(m):
    for x in (-1.5, 0, 1.5):
        m.cube("head", (x - 0.5, -4, -11.5), (x + 0.5, -3, -10.5), "bone")
    m.cube("head", (-2.6, 0.6, -12.4), (2.6, 1.4, -11.8), "dark")
    m.cube("body", (-1, 5, -9), (1, 6, 7), "glow")
    m.cube("tail", (-0.5, 2, 1), (0.5, 3.6, 8), "glow")
    for leg in ("leg_fl", "leg_fr", "leg_bl", "leg_br"):
        for x in (-1.6, 0, 1.6):
            m.spike(leg, x, -14, -4, 3, w=2, fwd=95, style="bright", tip="eye")
        m.cube(leg, (-2.4, -8, -2.4), (2.4, -7, 2.4), "dark")
    m.spike("head", 3, 3, -2, 5, w=2, left=40, fwd=-30)
    m.spike("head", -3, 3, -2, 5, w=2, left=-40, fwd=-30)


def _weaver(m):
    for i in range(4):
        for side in ("l", "r"):
            m.box(f"leg_{side}{i}_b", 0, -1.5, 0, 3, 3, 3, "glow")
    for z in (2, 5, 8):
        m.cube("abdomen", (-5.4, -1, z), (5.4, 0, z + 1), "glow")
    m.cube("head", (-2, -3.5, -6.6), (2, -2.6, -6.0), "bone")


def _walker(m):
    m.cube("torso", (-9, 14, -6), (9, 17, 6), "dark")
    for y in (2, 4):
        m.cube("torso", (-7, y, -5.6), (-4, y + 1, -5), "bright")
        m.cube("torso", (4, y, -5.6), (7, y + 1, -5), "bright")
    m.cube("torso", (-1, 0, 5), (1, 17, 6), "glow")
    for side in ("l", "r"):
        m.cube(f"arm_{side}_low", (-3.4, -3, -3.4), (3.4, -1.6, 3.4), "glow")
        m.cube(f"leg_{side}", (-4, -14, -4.2), (4, -11, -3.4), "bright")
        m.cube(f"leg_{side}", (-1, -13, -4.6), (1, -12, -4.2), "glow")
    m.cube("head", (-5.4, 0, -5.4), (5.4, 1, 4.4), "dark")
    m.cube("head", (-2, 0.5, -5.8), (2, 1.5, -5.2), "bone")


def _lurker(m):
    for x in (-1.5, 0, 1.5):
        m.cube("head", (x - 0.4, -3, -11.6), (x + 0.4, -2, -10.8), "bone")
    m.cube("torso", (-0.5, 4, -8), (0.5, 4.8, 5), "glow")
    m.cube("tail", (-0.5, 2, 1), (0.5, 2.8, 9), "glow")
    for side in ("l", "r"):
        for x in (-1.5, 0, 1.5):
            m.spike(f"leg_{side}", x, -17, -6, 3, w=2, fwd=95, style="bright", tip="eye")


def _colossus(m):
    for (x0, y0, x1, y1) in ((-11, 3, -10, 22), (10, 5, 11, 24), (-2, 21, 2, 22), (-12, 22, -6, 23), (7, 2, 12, 3)):
        m.cube("torso", (x0, y0, -8.6), (x1, y1, -8.1), "glow")
    m.cube("torso", (-14, -2, -9), (14, 2, 9), "dark")
    m.cube("torso", (-3, -2, -9.4), (3, 2, -8.9), "gold")
    for side in ("l", "r"):
        for dx in (-4, 0, 4):
            m.cube(f"arm_{side}_low", (dx - 1, -23, -7.4), (dx + 1, -21, -6.8), "glow")
        m.cube(f"arm_{side}", (-6, -9, -6), (6, -7, 6), "dark")
    m.cluster("torso", 0, 14, 8, 6, 4, 9, 6, lean=(-70, 0), w=(2, 4))


def _drifter(m):
    for side in ("l", "r"):
        for i in range(6):
            ln = 24 - i * 2
            lo, hi = (2, ln) if side == "l" else (-ln, -2)
            m.cube(f"wing_{side}_{i}", (lo, 1, -0.5), (hi, 1.6, 0.5), "glow")
    for y in (-6, 0, 6):
        m.cube("body", (-6.4, y, -6.4), (6.4, y + 1, 6.4), "dark")
    m.cluster("head", 0, 8, 0, 4, 3, 6, 4, lean=(0, 0), w=(2,))


EXTRAS = {"stalker": _stalker, "weaver": _weaver, "walker": _walker, "lurker": _lurker, "colossus": _colossus,
          "drifter": _drifter}


ALL = {f.__name__: f for f in (stalker, weaver, walker, lurker, colossus, drifter, amitariel, heart, heart_pylon,
                                   heart_sentinel, armor_amethyst, armor_purified,
                                   crystal_spike, rune_ring, shard_bolt, phantom_spear, ricardo, infection_core)}

def phantom_spear_pure():
    """The purified spear's skin: the violet light turned white and pale blue."""
    from PIL import Image
    from geo import RES
    for suffix in ("", "_glow"):
        img = Image.open(RES / "textures/entity" / f"phantom_spear{suffix}.png").convert("RGBA")
        px = img.load()
        for x in range(img.width):
            for y in range(img.height):
                r, g, b, a = px[x, y]
                if a:
                    v = (r + g + b) / 3
                    px[x, y] = (min(255, int(v * 0.85 + 50)), min(255, int(v * 0.95 + 60)), min(255, int(v + 75)), a)
        img.save(RES / "textures/entity" / f"phantom_spear_pure{suffix}.png")


def amitariel_p2():
    """Phase 2 skin: the violet armour burns red-magenta and the glow turns white-hot."""
    from PIL import Image
    from geo import RES
    for suffix, fn in (("", lambda r, g, b: (min(255, int(r * 1.15 + 25)), int(g * 0.42), int(b * 0.62 + 10))),
                       ("_glow", lambda r, g, b: (255, min(255, int(g * 0.5 + 60)), min(255, int(b * 0.55 + 40))))):
        img = Image.open(RES / "textures/entity" / f"amitariel{suffix}.png").convert("RGBA")
        px = img.load()
        for x in range(img.width):
            for y in range(img.height):
                r, g, b, a = px[x, y]
                if a:
                    px[x, y] = fn(r, g, b) + (a,)
        img.save(RES / "textures/entity" / f"amitariel_p2{suffix}.png")


if __name__ == "__main__":
    for name in sys.argv[1:] or ALL:
        model = ALL[name]()
        EXTRAS.get(name, lambda _: None)(model)
        model.export()
        if name == "amitariel":
            amitariel_p2()
        if name == "phantom_spear":
            phantom_spear_pure()
        if name == "heart":
            heart_variants()
        if name == "infection_core":
            infection_core_dim()
