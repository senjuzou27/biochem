#!/usr/bin/env python3
"""Generates every texture of the mod (item icons, armour layers, NPC skins) as simple pixel art.

Run from the ufa-mod folder:  python3 tools/gen_textures.py
Requires Pillow. The PNG files are committed, so this is only needed when you change the art.
"""
import os
import random
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "ufamod", "textures")


def save(img, *path):
    full = os.path.join(ROOT, *path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


def new(w, h):
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)


# ----------------------------------------------------------------------------- items

def item_lab_coat():
    im = new(16, 16); d = ImageDraw.Draw(im)
    w, g, o = (245, 245, 245, 255), (200, 205, 210, 255), (120, 125, 135, 255)
    d.rectangle([4, 2, 11, 14], fill=w)
    d.rectangle([1, 3, 3, 9], fill=w); d.rectangle([12, 3, 14, 9], fill=w)
    d.line([7, 2, 7, 14], fill=g); d.line([8, 2, 8, 14], fill=g)
    d.point([(6, 2), (9, 2), (5, 3), (10, 3)], fill=g)
    for y in (5, 8, 11):
        d.point((9, y), fill=o)
    d.rectangle([5, 10, 6, 12], outline=g)
    d.rectangle([4, 2, 11, 14], outline=None)
    for x, y in [(4, 14), (11, 14), (1, 9), (14, 9)]:
        d.point((x, y), fill=g)
    return im


def item_record_book():
    im = new(16, 16); d = ImageDraw.Draw(im)
    c, dk, gold = (36, 60, 120, 255), (24, 40, 86, 255), (228, 190, 80, 255)
    d.rectangle([3, 1, 12, 14], fill=c, outline=dk)
    d.line([4, 2, 4, 13], fill=shade(c, 1.25))
    d.rectangle([6, 4, 10, 5], fill=gold)
    d.line([6, 8, 10, 8], fill=gold); d.line([7, 10, 9, 10], fill=gold)
    return im


def item_summons():
    im = new(16, 16); d = ImageDraw.Draw(im)
    p, ln, red = (238, 232, 214, 255), (120, 116, 104, 255), (176, 36, 36, 255)
    d.rectangle([2, 2, 13, 13], fill=p, outline=(170, 162, 140, 255))
    d.rectangle([4, 3, 11, 4], fill=red)
    for y in (6, 8, 10):
        d.line([4, y, 11, y], fill=ln)
    d.ellipse([9, 9, 13, 13], outline=(60, 60, 150, 255))
    return im


def item_echpochmak():
    im = new(16, 16); d = ImageDraw.Draw(im)
    crust, dark, light = (206, 150, 70, 255), (150, 98, 40, 255), (236, 196, 120, 255)
    d.polygon([(8, 2), (14, 13), (2, 13)], fill=crust, outline=dark)
    d.line([8, 4, 5, 11], fill=light); d.line([8, 6, 8, 11], fill=dark)
    d.rectangle([7, 8, 9, 10], fill=(120, 70, 36, 255))
    return im


def item_chak_chak():
    im = new(16, 16); d = ImageDraw.Draw(im)
    rnd = random.Random(7)
    cols = [(232, 176, 56, 255), (246, 204, 96, 255), (204, 140, 36, 255)]
    for y in range(5, 14):
        half = (y - 3) // 2 + 1
        for x in range(8 - half, 8 + half):
            d.point((x, y), fill=rnd.choice(cols))
    d.rectangle([2, 14, 13, 14], fill=(230, 230, 235, 255))
    d.point([(7, 4), (8, 4)], fill=(250, 220, 130, 255))
    return im


def item_kumis():
    im = new(16, 16); d = ImageDraw.Draw(im)
    glass, milk, cork = (190, 210, 220, 255), (250, 250, 244, 255), (150, 104, 60, 255)
    d.rectangle([5, 6, 10, 14], fill=milk, outline=glass)
    d.rectangle([7, 3, 8, 5], fill=milk, outline=glass)
    d.rectangle([6, 1, 9, 2], fill=cork)
    d.rectangle([5, 9, 10, 11], fill=(60, 130, 80, 255))
    d.point((6, 7), fill=(255, 255, 255, 255))
    return im


def camo_pixels(im, box, seed):
    rnd = random.Random(seed)
    cols = [(78, 92, 56, 255), (102, 112, 70, 255), (58, 66, 44, 255), (124, 112, 78, 255)]
    x1, y1, x2, y2 = box
    for y in range(y1, y2 + 1):
        for x in range(x1, x2 + 1):
            im.putpixel((x, y), cols[(rnd.randrange(4) if rnd.random() < 0.45 else ((x // 2 + y // 3) % 4))])


def item_camo(kind):
    im = new(16, 16); d = ImageDraw.Draw(im)
    mask = new(16, 16); m = ImageDraw.Draw(mask)
    if kind == "helmet":
        m.pieslice([2, 3, 13, 18], 180, 360, fill=(255, 255, 255, 255)); m.rectangle([2, 10, 13, 11], fill=(255, 255, 255, 255))
    elif kind == "jacket":
        m.rectangle([4, 2, 11, 13], fill=(255, 255, 255, 255)); m.rectangle([1, 3, 3, 9], fill=(255, 255, 255, 255)); m.rectangle([12, 3, 14, 9], fill=(255, 255, 255, 255))
    elif kind == "trousers":
        m.rectangle([4, 2, 11, 5], fill=(255, 255, 255, 255)); m.rectangle([4, 5, 6, 14], fill=(255, 255, 255, 255)); m.rectangle([9, 5, 11, 14], fill=(255, 255, 255, 255))
    full = new(16, 16)
    camo_pixels(full, (0, 0, 15, 15), hash(kind) % 1000 if False else len(kind))
    im.paste(full, (0, 0), mask)
    if kind == "jacket":
        d.line([7, 2, 7, 13], fill=(40, 46, 30, 255))
    if kind == "trousers":
        d.rectangle([4, 2, 11, 2], fill=(60, 44, 30, 255))
    return im


def item_boots():
    im = new(16, 16); d = ImageDraw.Draw(im)
    b, h = (34, 30, 28, 255), (70, 62, 56, 255)
    for ox in (2, 9):
        d.rectangle([ox, 5, ox + 3, 12], fill=b)
        d.rectangle([ox, 11, ox + 5, 13], fill=b)
        d.line([ox, 5, ox + 3, 5], fill=h); d.line([ox, 14, ox + 5, 14], fill=(16, 14, 12, 255))
        d.point((ox + 1, 8), fill=h)
    return im


def item_shovel():
    im = new(16, 16); d = ImageDraw.Draw(im)
    wood, steel, dark = (132, 92, 48, 255), (176, 184, 190, 255), (96, 104, 110, 255)
    d.line([3, 12, 9, 6], fill=wood, width=2)
    d.polygon([(9, 2), (14, 2), (14, 7), (11, 8), (8, 5)], fill=steel, outline=dark)
    d.rectangle([2, 12, 4, 14], fill=shade(wood, 0.8))
    return im


# ----------------------------------------------------------------------------- armour layers (64x32)

def armor_coat(layer):
    im = new(64, 32); d = ImageDraw.Draw(im)
    w, g = (244, 244, 246, 255), (206, 210, 216, 255)
    d.rectangle([0, 0, 63, 31], fill=w)
    if layer == 1:
        # body front (20..27, 20..31): placket, buttons, pocket
        d.line([23, 20, 23, 31], fill=g); d.line([24, 20, 24, 31], fill=g)
        for y in (22, 25, 28):
            d.point((25, y), fill=(120, 126, 136, 255))
        d.rectangle([21, 27, 22, 29], outline=g)
        d.line([20, 20, 27, 20], fill=g)
        # cuffs
        d.line([40, 31, 55, 31], fill=g)
    return im


def armor_camo(layer):
    im = new(64, 32)
    camo_pixels(im, (0, 0, 63, 31), 40 + layer)
    d = ImageDraw.Draw(im)
    if layer == 1:
        d.rectangle([0, 0, 31, 15], fill=(74, 84, 54, 255))            # helmet: plain olive
        d.line([8, 15, 15, 15], fill=(50, 56, 38, 255))
        d.rectangle([0, 26, 15, 31], fill=(34, 30, 28, 255))           # boots
        d.line([20, 31, 27, 31], fill=(60, 44, 30, 255))               # belt
    return im


# ----------------------------------------------------------------------------- NPC skins (64x64)

SKIN_TONES = [(232, 190, 160), (214, 168, 132), (240, 204, 178), (198, 150, 116)]


def box_faces(u, v, w, h, dd):
    """Texture rectangles of a model box: top, bottom, right, front, left, back."""
    return {
        "top": (u + dd, v, u + dd + w - 1, v + dd - 1),
        "bottom": (u + dd + w, v, u + dd + 2 * w - 1, v + dd - 1),
        "right": (u, v + dd, u + dd - 1, v + dd + h - 1),
        "front": (u + dd, v + dd, u + dd + w - 1, v + dd + h - 1),
        "left": (u + dd + w, v + dd, u + 2 * dd + w - 1, v + dd + h - 1),
        "back": (u + 2 * dd + w, v + dd, u + 2 * dd + 2 * w - 1, v + dd + h - 1),
    }


HEAD = box_faces(0, 0, 8, 8, 8)
HAT = box_faces(32, 0, 8, 8, 8)
BODY = box_faces(16, 16, 8, 12, 4)
RARM = box_faces(40, 16, 4, 12, 4)
LARM = box_faces(32, 48, 4, 12, 4)
RLEG = box_faces(0, 16, 4, 12, 4)
LLEG = box_faces(16, 48, 4, 12, 4)


def fill_box(d, faces, color, noise=None, rnd=None):
    for r in faces.values():
        d.rectangle(r, fill=color + (255,))
        if noise and rnd:
            for y in range(r[1], r[3] + 1):
                for x in range(r[0], r[2] + 1):
                    if rnd.random() < 0.25:
                        d.point((x, y), fill=shade(color, rnd.choice([0.9, 1.08])))


def skin(seed, tone, hair, shirt, pants, shoes=(40, 36, 34), coat=None, hat=None, glasses=False, beard=None,
         scarf=None, long_hair=False, camo=False, apron=None, tie=None):
    rnd = random.Random(seed)
    im = new(64, 64); d = ImageDraw.Draw(im)
    fill_box(d, HEAD, tone)
    # hair: top, back, upper sides and a fringe
    d.rectangle(HEAD["top"], fill=hair + (255,))
    d.rectangle(HEAD["back"], fill=hair + (255,))
    for side in ("right", "left"):
        r = HEAD[side]
        d.rectangle([r[0], r[1], r[2], r[1] + (6 if long_hair else 2)], fill=hair + (255,))
    f = HEAD["front"]
    d.rectangle([f[0], f[1], f[2], f[1] + 1], fill=hair + (255,))
    if long_hair:
        d.rectangle([f[0], f[1], f[0], f[3]], fill=hair + (255,)); d.rectangle([f[2], f[1], f[2], f[3]], fill=hair + (255,))
    # face
    ey = f[1] + 4
    for ex in (f[0] + 1, f[0] + 5):
        d.point((ex, ey), fill=(255, 255, 255, 255)); d.point((ex + 1, ey), fill=(50, 40, 34, 255))
    d.point([(f[0] + 3, ey + 1), (f[0] + 4, ey + 1)], fill=shade(tone, 0.86))
    d.line([f[0] + 3, ey + 2, f[0] + 4, ey + 2], fill=shade(tone, 0.68))
    if glasses:
        d.rectangle([f[0] + 1, ey - 1, f[0] + 2, ey], outline=(30, 30, 30, 255)); d.rectangle([f[0] + 5, ey - 1, f[0] + 6, ey], outline=(30, 30, 30, 255))
        d.point([(f[0] + 3, ey - 1), (f[0] + 4, ey - 1)], fill=(30, 30, 30, 255))
        for ex in (f[0] + 1, f[0] + 5):
            d.point((ex, ey), fill=(210, 230, 240, 255)); d.point((ex + 1, ey), fill=(50, 40, 34, 255))
    if beard:
        d.rectangle([f[0] + 1, f[3] - 1, f[2] - 1, f[3]], fill=beard + (255,)); d.point([(f[0], f[3] - 1), (f[2], f[3] - 1)], fill=beard + (255,))
        d.line([f[0] + 2, ey + 2, f[0] + 5, ey + 2], fill=beard + (255,))
    if scarf:
        fill_box(d, HAT, scarf)
        h = HAT["front"]
        d.rectangle([h[0] + 1, h[1] + 2, h[2] - 1, h[3] - 1], fill=(0, 0, 0, 0))
        d.rectangle(HAT["bottom"], fill=(0, 0, 0, 0))
    if hat:
        kind, col = hat
        d.rectangle(HAT["top"], fill=col + (255,))
        for side in ("right", "front", "left", "back"):
            r = HAT[side]
            d.rectangle([r[0], r[1], r[2], r[1] + (1 if kind != "helmet" else 3)], fill=col + (255,))
        if kind == "cap":
            h = HAT["front"]
            d.rectangle([h[0], h[1] + 2, h[2], h[1] + 2], fill=shade(col, 0.6))
            d.point([(h[0] + 3, h[1]), (h[0] + 4, h[1])], fill=(212, 175, 55, 255))
        if kind == "chef":
            d.rectangle(HAT["top"], fill=(250, 250, 250, 255))
    # clothes
    if camo:
        for part in (BODY, RARM, LARM, RLEG, LLEG):
            for r in part.values():
                camo_pixels(im, r, rnd.randrange(1000))
        d = ImageDraw.Draw(im)
        d.line([BODY["front"][0], BODY["front"][3], BODY["front"][2], BODY["front"][3]], fill=(60, 44, 30, 255))
    else:
        fill_box(d, BODY, shirt, True, rnd)
        fill_box(d, RARM, shirt, True, rnd); fill_box(d, LARM, shirt, True, rnd)
        fill_box(d, RLEG, pants, True, rnd); fill_box(d, LLEG, pants, True, rnd)
    # hands
    for arm in (RARM, LARM):
        for side in ("right", "front", "left", "back"):
            r = arm[side]
            d.rectangle([r[0], r[3] - 1, r[2], r[3]], fill=tone + (255,))
        d.rectangle(arm["bottom"], fill=tone + (255,))
    # shoes
    for leg in (RLEG, LLEG):
        for side in ("right", "front", "left", "back"):
            r = leg[side]
            d.rectangle([r[0], r[3] - 1, r[2], r[3]], fill=shoes + (255,))
        d.rectangle(leg["bottom"], fill=shoes + (255,))
    if coat:
        # an open white coat over the clothes, reaching down to the knees
        for part in (RARM, LARM):
            for side in ("right", "front", "left", "back"):
                r = part[side]
                d.rectangle([r[0], r[1], r[2], r[3] - 2], fill=coat + (255,))
            d.rectangle(part["top"], fill=coat + (255,))
        for side in ("right", "left", "back"):
            d.rectangle(BODY[side], fill=coat + (255,))
        b = BODY["front"]
        d.rectangle([b[0], b[1], b[0] + 2, b[3]], fill=coat + (255,)); d.rectangle([b[2] - 2, b[1], b[2], b[3]], fill=coat + (255,))
        d.point((b[0] + 2, b[1] + 4), fill=shade(coat, 0.8)); d.point((b[0] + 2, b[1] + 7), fill=shade(coat, 0.8))
        for leg in (RLEG, LLEG):
            for side in ("right", "front", "left", "back"):
                r = leg[side]
                d.rectangle([r[0], r[1], r[2], r[1] + 4], fill=coat + (255,))
        # the gap of the open coat on the legs
        d.rectangle([RLEG["front"][2], RLEG["front"][1], RLEG["front"][2], RLEG["front"][1] + 4], fill=pants + (255,))
        d.rectangle([LLEG["front"][0], LLEG["front"][1], LLEG["front"][0], LLEG["front"][1] + 4], fill=pants + (255,))
    if apron:
        b = BODY["front"]
        d.rectangle([b[0] + 1, b[1] + 2, b[2] - 1, b[3]], fill=apron + (255,))
        for leg in (RLEG, LLEG):
            r = leg["front"]
            d.rectangle([r[0], r[1], r[2], r[1] + 3], fill=apron + (255,))
    if tie:
        b = BODY["front"]
        d.line([b[0] + 3, b[1], b[0] + 3, b[1] + 6], fill=tie + (255,)); d.line([b[0] + 4, b[1], b[0] + 4, b[1] + 6], fill=tie + (255,))
    return im


def main():
    items = {
        "lab_coat": item_lab_coat(), "record_book": item_record_book(), "summons": item_summons(),
        "echpochmak": item_echpochmak(), "chak_chak": item_chak_chak(), "kumis": item_kumis(),
        "camo_helmet": item_camo("helmet"), "camo_jacket": item_camo("jacket"), "camo_trousers": item_camo("trousers"),
        "army_boots": item_boots(), "sapper_shovel": item_shovel(),
    }
    for name, img in items.items():
        save(img, "item", name + ".png")
    for layer in (1, 2):
        save(armor_coat(layer), "models", "armor", "lab_coat_layer_%d.png" % layer)
        save(armor_camo(layer), "models", "armor", "camo_layer_%d.png" % layer)

    white = (244, 244, 246)
    brown, black, grey, blond, red = (84, 56, 36), (30, 26, 24), (168, 168, 172), (196, 160, 96), (140, 60, 36)
    npc = {
        "citizen_0": skin(1, SKIN_TONES[0], brown, (52, 92, 150), (50, 54, 70)),
        "citizen_1": skin(2, SKIN_TONES[2], grey, (150, 60, 90), (90, 70, 60), scarf=(190, 60, 60), long_hair=True),
        "citizen_2": skin(3, SKIN_TONES[1], black, (70, 120, 80), (60, 60, 64), hat=("cap", (40, 40, 44))),
        "citizen_3": skin(4, SKIN_TONES[0], blond, (200, 160, 60), (44, 60, 100), long_hair=True),
        "student_0": skin(5, SKIN_TONES[2], brown, (120, 140, 190), (40, 44, 60), coat=white, long_hair=True),
        "student_1": skin(6, SKIN_TONES[1], black, (90, 90, 96), (36, 40, 56), coat=white),
        "watchman_0": skin(7, SKIN_TONES[1], black, (36, 40, 52), (30, 32, 40), hat=("cap", (30, 34, 46)), tie=(20, 20, 24)),
        "watchman_1": skin(8, SKIN_TONES[2], grey, (120, 90, 130), (70, 60, 70), scarf=(90, 110, 160), long_hair=True, glasses=True),
        "cook_0": skin(9, SKIN_TONES[0], brown, (240, 240, 240), (220, 220, 224), hat=("chef", (250, 250, 250)), apron=(230, 236, 240), long_hair=True),
        "lecturer_0": skin(10, SKIN_TONES[2], red, (110, 50, 70), (40, 40, 48), coat=white, long_hair=True, glasses=True),
        "librarian_0": skin(11, SKIN_TONES[0], grey, (120, 100, 80), (70, 60, 50), long_hair=True, glasses=True),
        "professor_0": skin(12, SKIN_TONES[0], grey, (70, 100, 150), (40, 40, 46), coat=white, glasses=True, beard=(200, 200, 204), tie=(120, 30, 30)),
        "commissar_0": skin(13, SKIN_TONES[1], black, (64, 78, 52), (64, 78, 52), hat=("cap", (64, 78, 52)), tie=(40, 46, 32), shoes=(24, 22, 20)),
        "soldier_0": skin(14, SKIN_TONES[0], brown, (0, 0, 0), (0, 0, 0), camo=True, hat=("helmet", (74, 84, 54)), shoes=(24, 22, 20)),
        "sergeant_0": skin(15, SKIN_TONES[3], black, (0, 0, 0), (0, 0, 0), camo=True, hat=("cap", (58, 66, 44)), beard=(40, 34, 30), shoes=(24, 22, 20)),
        "recruit_0": skin(16, SKIN_TONES[2], brown, (0, 0, 0), (0, 0, 0), camo=True, shoes=(24, 22, 20)),
    }
    for name, img in npc.items():
        save(img, "entity", "npc", name + ".png")
    # vehicles are drawn from plain coloured boxes and only need a white texture
    save(Image.new("RGBA", (4, 4), (255, 255, 255, 255)), "entity", "white.png")
    # traffic light head: the model uses the middle six columns of the texture
    lamps = {"red": (0, (255, 40, 30)), "yellow": (1, (255, 200, 30)), "green": (2, (40, 230, 80))}
    for name, (lit, colour) in lamps.items():
        img = Image.new("RGBA", (16, 16), (28, 28, 30, 255))
        d = ImageDraw.Draw(img)
        d.rectangle([5, 0, 10, 15], outline=(12, 12, 14, 255))
        off = [(70, 20, 18), (80, 64, 16), (18, 60, 28)]
        for i in range(3):
            c = colour if i == lit else off[i]
            d.rectangle([6, 1 + i * 5, 9, 4 + i * 5], fill=c + (255,))
            if i == lit:
                d.point([(7, 2 + i * 5)], fill=(255, 255, 255, 255))
        save(img, "block", "traffic_light_%s.png" % name)
    side = Image.new("RGBA", (16, 16), (28, 28, 30, 255))
    save(side, "block", "traffic_light_top.png")
    print("textures written to", os.path.normpath(ROOT))


if __name__ == "__main__":
    main()
