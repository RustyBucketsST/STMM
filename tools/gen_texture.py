"""Generates the mod's textures into src/main/resources/assets/skibidi/textures/.

entity/skibidi_urinal.png                            -- UVs match blockbench/skibidi_urinal.bbmodel
entity/skibidi_toilet.png and the other toilets on it -- UVs read from blockbench/skibidi_toilet.bbmodel by cube name
(big, scientist, flying, DJ, spider, jetpack, saint,     (the spider toilet's legs use its "spider_leg" cube)
hat)
entity/cameraman.png and the other Alliance members, -- UVs read from the .bbmodel of the same name
titan_*.png, titan_*_glow.png, gman_toilet.png, ...
item/plunger.png, item/alliance_blaster.png, item/stun_rifle.png, item/gravity_plunger.png, item/energy_knife.png
gui/power/*.png                                      -- 16x16 icons for the Powers menu and HUD
player/*.png                                         -- UVs match PlayerHeadModels; grey parts are dyed in game

For a box at uv (u, v) with size (w, h, d):
top (u+d, v) w x d, front (u+d, v+d) w x h, sides (u, v+d) and (u+d+w, v+d) d x h,
back (u+2d+w, v+d) w x h, bottom (u+d+w, v) w x d.

Usage: python3 tools/gen_texture.py [textures_dir]
"""
import json
import math
import random
import sys
from pathlib import Path

from PIL import Image

img = None
px = None


def new_image(size, seed, height=None):
    global img, px
    random.seed(seed)
    img = Image.new("RGBA", (size, height or size), (0, 0, 0, 0))
    px = img.load()
    return img


def shade(c, n=8):
    j = random.randint(-n, n)
    return tuple(max(0, min(255, x + j)) for x in c[:3]) + (255,)


def rect(x, y, w, h, c, noise=8):
    for i in range(x, x + w):
        for k in range(y, y + h):
            px[i, k] = shade(c, noise)


def box(u, v, w, h, d, c, noise=6):
    rect(u + d, v, w * 2, d, c, noise)        # top + bottom
    rect(u, v + d, 2 * (d + w), h, c, noise)  # four sides


def faces(u, v, w, h, d):
    """Where each face of a box at uv (u, v) sits on the texture, as (x, y, width, height). Front, back and
    sides read as seen from outside: "right" is the mob's right side (the -X side), "left" its left side.
    The top reads as seen from above and in front, so its first row is the back edge."""
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }


def gradient(r, top, bottom, noise=2):
    """Fills a face rectangle (x, y, w, h) with a vertical gradient from `top` to `bottom`."""
    x, y, w, h = r
    for k in range(h):
        t = k / (h - 1) if h > 1 else 0.0
        rect(x, y + k, w, 1, tuple(round(a + (b - a) * t) for a, b in zip(top, bottom)), noise)


def sides(f):
    return [f[k] for k in ("right", "front", "left", "back")]


def outline(c):
    """Adds a 1px outline around everything drawn so far (for item icons)."""
    w, h = img.size
    filled = {(x, y) for x in range(w) for y in range(h) if px[x, y][3]}
    for x, y in filled:
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if 0 <= nx < w and 0 <= ny < h and (nx, ny) not in filled:
                px[nx, ny] = c + (255,)


# ---------------------------------------------------------------- toilets

SKIN = (224, 172, 120)
HAIR = (70, 45, 30)


GLAZE = (250, 251, 252)       # tops catching the light
GLAZE_HI = (243, 245, 247)    # sides, upper edge
GLAZE_LO = (212, 217, 223)    # sides, lower edge
UNDERSIDE = (176, 182, 190)
SEAM = (206, 211, 218)        # the thin shadow where two pieces meet
CHROME = (190, 197, 208)
WATER = (96, 156, 206)
WATER_DEEP = (66, 122, 182)


def porcelain(u, v, w, h, d, hi=GLAZE_HI, lo=GLAZE_LO):
    f = faces(u, v, w, h, d)
    rect(*f["top"], GLAZE, 2)
    rect(*f["bottom"], UNDERSIDE, 2)
    for r in sides(f):
        gradient(r, hi, lo)
    return f


URINAL_HAIR = (60, 40, 28)


def urinal():
    # The Skibidi Urinal (skibidi_urinal.bbmodel): a floor-standing urinal with a chrome flush pipe, and a singer
    # with neat dark hair and a mustache, mouth open mid-song
    b = bb_boxes("skibidi_urinal")
    new_image(128, 18)
    head16(b, SKIN, URINAL_HAIR, [
        "HHHHHHHHHHHHHHHH",
        "HHHHHHHHHHHhHHHH",
        "HHHssssssssssHHH",
        "HssssssssssssssH",
        "ssbbbbssssbbbbss",
        "ssssssssssssssss",
        "ssWWWWssssWWWWss",
        "ssWpiWssssWipWss",
        "sssddssssssddsss",
        "ssssssddddssssss",
        "ssssHHHHHHHHssss",
        "sssHHHssssHHHsss",
        "sssssllllllsssss",
        "ssssslmmmmlsssss",
        "sssssslmmlssssss",
        "sssssssllsssssss"])
    f = porcelain(*b["shell"], GLAZE_HI, (214, 219, 225))
    x, y, w, h = f["front"]
    rect(x + 2, y + 8, w - 4, h - 10, (232, 235, 239), 2)   # the recessed back of the bowl
    rect(x + 6, y + 2, 8, 2, CHROME, 4)                     # chrome spreader
    porcelain(*b["shell_top"])
    porcelain(*b["wing_right"])
    f = porcelain(*b["bowl"], (246, 248, 250), (222, 226, 232))
    x, y, w, h = f["top"]                                   # the opening the head comes out of
    rect(x + 2, y + 2, w - 4, h - 4, (204, 210, 218), 2)
    rect(x + 4, y + 3, w - 8, h - 6, (70, 120, 160), 5)
    rect(x + 5, y + 4, w - 10, 2, (40, 80, 120), 5)
    porcelain(*b["bowl_front"], (246, 248, 250), (222, 226, 232))
    for name in ("bowl", "bowl_front"):
        x, y, w, h = faces(*b[name])["front"]
        rect(x + 1, y, w - 2, 1, (255, 255, 255), 1)        # shine on the rim
    porcelain(*b["bowl_lower"], (222, 226, 232), (206, 211, 217))
    f = faces(*b["drain"])
    box(*b["drain"], CHROME, 4)
    x, y, w, h = f["front"]
    for i in range(0, w, 2):
        px[x + i, y + 1] = (90, 96, 106, 255)               # drain holes
    box(*b["pipe"], CHROME, 6)
    x, y, w, h = faces(*b["pipe"])["front"]
    rect(x + 1, y, 1, h, (232, 238, 244), 2)                # shine down the pipe
    f = faces(*b["valve"])
    box(*b["valve"], (170, 177, 188), 6)
    for r in sides(f):
        rect(r[0], r[1] + r[3] - 1, r[2], 1, (220, 226, 234), 3)
    box(*b["flush_button"], (220, 226, 234), 2)
    return img


TRIPLE_SKIN = (150, 106, 78)


def triple():
    # Two toilets joined on a platform and a third behind them (triple_skibidi_toilet.bbmodel), with three
    # bald heads grinning ear to ear: the top one horned and angry, the left one goggle-eyed, the right one smug
    b = bb_boxes("triple_skibidi_toilet")
    new_image(256, 20)
    look = dict(top=False, side_rows=0, back_rows=0, colors={"b": (70, 48, 36)})
    head16(b, TRIPLE_SKIN, TRIPLE_SKIN, [
        "ssssssssssssssss",
        "sssssseessssssss",
        "ssddssssssssddss",
        "ssssssssssssssss",
        "sbbbssssssssbbbs",
        "sssbbbssssbbbsss",
        "ssWWWWssssWWWWss",
        "ssWWpWssssWpWWss",
        "ssssDDssssDDssss",
        "ssssssddddssssss",
        "DssssssssssssssD",
        "DlttttttttttttlD",
        "sDtTtTtTtTtTtTDs",
        "ssDmmmmmmmmmmDss",
        "sssDDDDDDDDDDsss",
        "ssssssssssssssss"], part="head", prefix="head_", **look)
    head16(b, TRIPLE_SKIN, TRIPLE_SKIN, [
        "ssssssssssssssss",
        "ssssssssssssssss",
        "sssssseessssssss",
        "ssssssssssssssss",
        "ssbbbbssssbbbbss",
        "ssssssssssssssss",
        "ssWWWWssssWWWWss",
        "sspWWWssssWWpWss",
        "ssWWWWssssWWWWss",
        "ssssssddddssssss",
        "ssDssssssssssDss",
        "sssDttttttttDsss",
        "sssDtTtTtTtTDsss",
        "ssssDmmmmmmDssss",
        "sssssDDDDDDsssss",
        "ssssssssssssssss"], part="head_left", prefix="head_left_", **look)
    head16(b, TRIPLE_SKIN, TRIPLE_SKIN, [
        "ssssssssssssssss",
        "ssssssssssssssss",
        "sssssseessssssss",
        "ssssssssssbbbbss",
        "ssbbbbssssssssss",
        "ssssssssssssssss",
        "ssWWWWssssWWWWss",
        "ssWpWWssssWpWWss",
        "sssddssssssddsss",
        "ssssssddddssssss",
        "sDssssssssssssDs",
        "DlttttttttttttlD",
        "sDttTttTttTttTDs",
        "sssDmmmmmmmmDsss",
        "ssssDDDDDDDDssss",
        "ssssssssssssssss"], part="head_right", prefix="head_right_", **look)
    for name in ("horn_right", "horn_tip_right"):
        f = faces(*b[name])
        for r in sides(f):
            gradient(r, (60, 54, 50), (110, 100, 92), 3)        # dark horns, lighter at the base
        rect(*f["top"], (44, 40, 38), 2)
    # Porcelain, with the inside of each bowl seen from above and oval lids like the regular toilets
    for name in ("pedestal_right", "bowl_lower_right", "bowl_right", "bowl_front_right", "lid_top_right", "tank",
                 "tank_lid", "back_pedestal", "back_bowl", "back_tank"):
        porcelain(*b[name])
    for name in ("bowl_right", "back_bowl"):
        x, y, w, h = faces(*b[name])["top"]
        rect(x + 3, y + 3, w - 6, h - 6, (210, 216, 223), 2)
        rect(x + 5, y + 5, w - 10, h - 10, (190, 198, 208), 2)
        rect(x + 7, y + 7, w - 14, h - 14, WATER, 5)
    x, y, w, h = faces(*b["tank_lid"])["top"]
    rect(x + 2, y + 2, w - 4, 1, (255, 255, 255), 0)            # glossy highlight
    lid_oval(porcelain(*b["lid_right"], (248, 249, 251), (226, 230, 235)))
    f = faces(*b["platform"])
    for r in f.values():
        rect(*r, (120, 124, 130), 4)                            # a grey stone platform
    x, y, w, h = f["top"]
    for i in range(0, w, 9):
        rect(x + i, y, 1, h, (104, 108, 114), 0)                # slabs
    return img


STEEL = (152, 156, 162)


def fast_armored():
    # The Fast Armored Skibidi Toilet (fast_armored_skibidi_toilet.bbmodel): a brushed-steel prison toilet with
    # a sink on top, a head in a black helmet with blank white eyes, a dark plate behind with six rockets
    b = bb_boxes("fast_armored_skibidi_toilet")
    new_image(256, 24, 128)
    skin = (186, 140, 108)
    head16(b, skin, (30, 26, 24), [
        "HHHHHHHHHHHHHHHH",
        "HHHHHHHHHHHHHHHH",
        "HHHHHHHHHHHHHHHH",
        "HHHHHHHHHHHHHHHH",
        "HHHHHHHHHHHHHHHH",
        "HHHHHHHHHHHHHHHH",
        "ssssssssssssssss",
        "ssWWWWssssWWWWss",
        "ssWWWWssssWWWWss",
        "ssssssddddssssss",
        "sDssssssssssssDs",
        "sDlttttttttttlDs",
        "ssDtTtTtTtTtTDss",
        "sssDmmmmmmmmDsss",
        "ssssDDDDDDDDssss",
        "ssssssssssssssss"], {"W": (250, 250, 250)}, side_rows=6, back_rows=8, sideburns=False)
    f = faces(*b["head"])
    for key in ("right", "left"):
        x, y, rw, rh = f[key]
        rect(x + rw // 2, y + 6, 2, rh - 6, (50, 44, 36), 0)    # helmet strap down to the chin
    for name in ("helmet", "helmet_brim"):
        f = faces(*b[name])
        for r in sides(f):
            gradient(r, (40, 40, 44), (22, 22, 26), 3)
        rect(*f["top"], (46, 46, 50), 3)
        rect(*f["bottom"], (16, 16, 18), 0)
    x, y, w, h = faces(*b["helmet"])["top"]
    rect(x + w // 2 - 1, y + 2, 2, h - 4, (60, 60, 66), 2)      # ridge
    for name in ("base", "bowl", "rim", "back_unit", "sink", "faucet", "faucet_spout"):
        f = faces(*b[name])
        for r in sides(f):
            x, y, rw, rh = r
            gradient(r, tuple(c + 10 for c in STEEL), tuple(c - 22 for c in STEEL), 3)
            for k in range(1, rh, 3):
                rect(x, y + k, rw, 1, tuple(c - 8 for c in STEEL), 2)   # brushed lines
        rect(*f["top"], tuple(c + 16 for c in STEEL), 3)
        rect(*f["bottom"], (90, 92, 98), 2)
    x, y, w, h = faces(*b["rim"])["top"]
    rect(x + 3, y + 3, w - 6, h - 6, (70, 74, 82), 2)          # the inside of the bowl
    rect(x + 5, y + 5, w - 10, h - 10, (60, 100, 140), 5)
    x, y, w, h = faces(*b["sink"])["top"]
    rect(x + 2, y + 2, w - 4, h - 4, (100, 104, 110), 2)       # basin
    rect(x + w // 2 - 1, y + h // 2 - 1, 2, 2, (40, 40, 44), 0)  # drain
    f = faces(*b["plate"])
    for r in f.values():
        rect(*r, (62, 64, 70), 3)
    for key in ("back", "front"):
        x, y, w, h = f[key]
        for k in range(0, h, 11):
            rect(x, y + k, w, 1, (48, 50, 54), 0)              # panel seams
        for i in range(2, w, 6):
            for k in (2, h - 3):
                px[x + i, y + k] = (104, 106, 112, 255)        # rivets
    f = faces(*b["rocket0_right"])
    for r in sides(f):
        gradient(r, (124, 126, 132), (90, 92, 98), 3)
        x, y, rw, rh = r
        rect(x, y + rh - 2, rw, 2, (60, 62, 66), 0)
    for key in ("top", "bottom"):
        x, y, rw, rh = f[key]
        rect(x, y, rw, rh, (110, 112, 118), 3)
        for k in range(2, rh, 4):
            rect(x, y + k, rw, 1, (80, 82, 88), 0)             # bands along the rocket
    x, y, w, h = f["back"]
    rect(x, y, w, h, (40, 40, 44), 0)                          # nozzle...
    rect(x + 1, y + 1, w - 2, h - 2, (250, 170, 60), 6)        # ...glowing hot
    rect(x + 3, y + 3, w - 6, h - 6, (255, 230, 140), 4)
    x, y, w, h = f["front"]
    rect(x + 2, y + 2, w - 4, h - 4, (150, 40, 36), 3)         # red warhead
    return img


def camera_strider():
    # The Camera Strider (camera_strider.bbmodel): a dark movie-camera body with a big glass lens, guns on top and
    # underneath, a screen on the back, on three long dark legs
    b = bb_boxes("camera_strider")
    new_image(128, 25, 64)
    f = faces(*b["housing"])
    for r in sides(f):
        gradient(r, (70, 72, 78), (48, 50, 56), 3)
    rect(*f["top"], (84, 86, 92), 3)
    rect(*f["bottom"], (40, 42, 46), 2)
    for key in ("right", "left"):
        x, y, w, h = f[key]
        rect(x + 1, y + 1, w - 2, h - 2, (62, 64, 70), 2)   # side panel
        for i, k in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
            px[x + i, y + k] = (110, 112, 118, 255)         # screws
    x, y, w, h = f["back"]
    rect(x + 2, y + 2, w - 4, h - 4, (40, 70, 96), 3)       # screen on the back
    rect(x + 2, y + 2, w - 4, 1, (90, 130, 160), 2)
    box(*b["lens"], (30, 30, 34), 3)
    f = faces(*b["lens"])
    for r in sides(f):
        x, y, w, h = r
        rect(x, y + 1, w, 1, (60, 60, 66), 0)               # focus rings
        rect(x, y + h - 2, w, 1, (60, 60, 66), 0)
    f = faces(*b["lens_hood"])
    box(*b["lens_hood"], (24, 24, 28), 2)
    x, y, w, h = f["front"]
    radial(x, y, w, h, 0, w / 2 - 0.6, lambda r: (20, 34, 70) if r > 1.2 else (10, 16, 36))
    px[x + 2, y + 2] = (150, 190, 240, 255)
    px[x + 3, y + 2] = (110, 150, 210, 255)
    for name in ("gun_top", "barrel_top", "gun_bottom", "barrel_bottom", "viewfinder"):
        box(*b[name], (42, 44, 48), 3)
    rect(*faces(*b["barrel_top"])["front"], (14, 14, 16), 0)
    rect(*faces(*b["barrel_bottom"])["front"], (14, 14, 16), 0)
    box(*b["light"], (230, 40, 40), 0)
    for name in ("left_leg_thigh", "left_leg_shin"):
        f = faces(*b[name])
        for r in sides(f):
            gradient(r, (78, 80, 86), (56, 58, 64), 3)
            x, y, w, h = r
            rect(x, y, w, 1, (34, 34, 38), 0)               # joint
        rect(*f["bottom"], (30, 30, 34), 0)
    x, y, w, h = faces(*b["left_leg_shin"])["front"]
    rect(x, y + h - 3, w, 3, (40, 40, 44), 2)               # foot
    return img


# ---- The toilets at twice the detail (skibidi_toilet.bbmodel, 256x256; UVs read by cube name)

def head16(b, skin, hair, face, colors=None, top=True, side_rows=4, back_rows=10, sideburns=True, part="head",
           prefix=""):
    """The 16x16x16 head (cube `part`), its nose, ears and neck (cubes `prefix` + "nose" etc.): skin, hair over
    the top, sides and back, and a 16x16 pixel-art face, where H is hair, h a lighter streak of it, s skin, e a
    highlight, d shade, w a wrinkle, D dark lines, b brow, W eye white, i iris, p pupil, t teeth, T the gaps
    between them, m mouth, l lip, n nostril."""
    u, v, w, h, d = b[part]
    box(u, v, w, h, d, skin, 4)
    f = faces(u, v, w, h, d)
    if top:
        rect(*f["top"], hair, 5)
        x, y, tw, th = f["top"]
        for i in range(2, tw - 2, 3):
            rect(x + i, y + 1, 1, th - 2, tuple(min(255, c + 14) for c in hair), 3)   # combed strands
    for name in ("right", "left"):
        x, y, sw, sh = f[name]
        if side_rows:
            rect(x, y, sw, side_rows, hair, 5)
            if sideburns:
                front = x + sw - 3 if name == "right" else x     # the front edge of each side
                rect(front, y + side_rows, 3, 4, hair, 5)
    if back_rows:
        x, y, bw, bh = f["back"]
        rect(x, y, bw, back_rows, hair, 5)
        rect(x + 2, y + back_rows, bw - 4, 1, hair, 5)           # tapering at the nape
    palette = {"H": hair, "h": tuple(min(255, c + 22) for c in hair), "s": skin,
               "e": tuple(min(255, round(c * 1.08)) for c in skin), "d": tuple(round(c * 0.88) for c in skin),
               "w": tuple(round(c * 0.78) for c in skin), "D": tuple(round(c * 0.62) for c in skin),
               "b": tuple(round(c * 0.8) for c in hair), "W": (240, 240, 236), "i": (70, 110, 150),
               "p": (24, 24, 26), "t": (242, 240, 230), "T": (196, 190, 176), "m": (76, 16, 18),
               "l": (196, 92, 88), "n": tuple(round(c * 0.5) for c in skin), **(colors or {})}
    x, y = f["front"][:2]
    for k, row in enumerate(face):
        assert len(row) == 16, row
        for i, ch in enumerate(row):
            px[x + i, y + k] = shade(palette[ch], 3 if ch in "sdeHh" else 1)
    nf = faces(*b[prefix + "nose"])
    box(*b[prefix + "nose"], skin, 3)
    for name in ("right", "left", "bottom"):
        rect(*nf[name], palette["d"], 2)
    x, y, nw, nh = nf["front"]
    rect(x, y, nw, 1, palette["e"], 2)                           # the bridge catching the light
    rect(x, y + nh - 1, nw, 1, palette["n"], 1)                  # nostrils
    ef = faces(*b[prefix + "ear_right"])
    box(*b[prefix + "ear_right"], skin, 3)
    rect(*ef["front"], palette["d"], 2)
    x, y, ew, eh = ef["right"]
    rect(x, y + 1, ew, eh - 2, palette["w"], 2)                  # the hollow of the ear
    nk = faces(*b[prefix + "neck"])
    for r in sides(nk):
        gradient(r, palette["D"], skin, 3)                       # shadowed just under the jaw
    rect(*nk["top"], skin, 3)
    rect(*nk["bottom"], palette["d"], 2)
    return palette


def skibidi_face(b):
    # The Skibidi Toilet: brown hair, eyebrows raised high, wide eyes, mouth wide open mid-song.
    head16(b, SKIN, HAIR, [
        "HHHHHHHHHHHHHHHH",
        "HHHHHHhhHHHHHHHH",
        "HHHssssssssssHHH",
        "HHssssssssssssHH",
        "HssbbbbssbbbbssH",
        "sbssssssssssssbs",
        "ssWWWWssssWWWWss",
        "ssWpiWssssWipWss",
        "ssdDDdssssdDDdss",
        "sDssssddddssssDs",
        "sDssllllllllssDs",
        "sDslttttttttlsDs",
        "ssslmmmmmmmmlsss",
        "sssslmmmmmmlssss",
        "sssssllllllsssss",
        "ssssssddddssssss"])


def big_face(b):
    # The Giant Skibidi Toilet: short dark hair, raised worried brows, wide staring eyes, a huge toothy grin.
    head16(b, (214, 170, 130), (52, 38, 30), [
        "HHHHHHHHHHHHHHHH",
        "HHHHHHHHHHHHHHHH",
        "HHssssssssssssHH",
        "HssssssssssssssH",
        "ssssbbbssbbbssss",
        "ssbbssssssssbbss",
        "sWWWWWssssWWWWWs",
        "sWWpWWssssWWpWWs",
        "sWWWWWssssWWWWWs",
        "ssdddsddddsdddss",
        "sDssssssssssssDs",
        "DlttttttttttttlD",
        "sDtTtTtTtTtTtTDs",
        "ssDmmmmmmmmmmDss",
        "sssDDmmmmmmDDsss",
        "ssssssDDDDssssss"], side_rows=3, back_rows=9)


def scientist_face(b):
    # The Chief Scientist: bald, white hair round the sides and back, round glasses, a kindly smile.
    hair = (236, 236, 240)
    head16(b, (230, 200, 180), hair, [
        "ssssssssssssssss",
        "sssssseessssssss",
        "ssssssssssssssss",
        "hssswwwwwwwwsssh",
        "hssbbbssssbbbssh",
        "hsFFFFssssFFFFsh",
        "hFggggFFFFggggFh",
        "hFgWpgFssFgpWgFh",
        "hsFFFFssssFFFFsh",
        "ssssssddddssssss",
        "sdssssssssssssds",
        "ssDssssssssssDss",
        "sssDssssssssDsss",
        "ssssDllllllDssss",
        "sssssDDDDDDsssss",
        "ssssssssssssssss"], {"h": hair, "F": (40, 40, 46), "g": (190, 215, 235)},
        top=False, side_rows=0, back_rows=0)
    u, v, w, h, d = b["head"]
    f = faces(u, v, w, h, d)
    for name in ("right", "left"):
        x, y, sw, sh = f[name]
        rect(x, y + 3, sw, 8, hair, 6)                           # white hair over the ears
    x, y, bw, bh = f["back"]
    rect(x, y + 3, bw, 10, hair, 6)                              # and round the back


def jetpack_face(b):
    # Male_07 as the Jetpack Toilet: dark hair, thick angry brows, staring eyes, a snarl, stubble.
    head16(b, (198, 152, 118), (30, 26, 24), [
        "HHHHHHHHHHHHHHHH",
        "HHHHHHHHHHHHHHHH",
        "HHHHssssssssHHHH",
        "HHssssssssssssHH",
        "sbbbssssssssbbbs",
        "sssbbbssssbbbsss",
        "ssWWWWssssWWWWss",
        "ssWWpWssssWpWWss",
        "ssssDDssssDDssss",
        "ssssssddddssssss",
        "sDssssssssssssDs",
        "eeeDllllllllDeee",
        "eeeDttTttTttDeee",
        "eeeeDmmmmmmDeeee",
        "eeeeeDDDDDDeeeee",
        "eeeeeeeeeeeeeeee"], {"e": (150, 120, 96)}, side_rows=5)


def flying_face(b):
    # The Normal Flying Skibidi Toilet: bald, dark-skinned, scared, eyes darting to the side.
    skin = (120, 86, 66)
    head16(b, skin, skin, [
        "ssssssssssssssss",
        "sssssseessssssss",
        "ssssssssssssssss",
        "sssswwwwwwwwssss",
        "sssssbbssbbsssss",
        "sssbbssssssbbsss",
        "ssWWWWssssWWWWss",
        "sspWWWsssspWWWss",
        "ssWWWWssssWWWWss",
        "sssddsddddsddsss",
        "ssssssssssssssss",
        "sssssDDDDDDsssss",
        "ssssDmmmmmmDssss",
        "ssssDmmmmmmDssss",
        "sssssDDDDDDsssss",
        "ssssssssssssssss"], {"b": (60, 40, 30)}, side_rows=0, back_rows=0)


def saint_face(b, eye_white, iris):
    # The Saint: a young man with short dark hair and a wide, toothy grin.
    head16(b, (176, 128, 92), (40, 28, 22), [
        "HHHHHHHHHHHHHHHH",
        "HHHHHHHHHHHHHHHH",
        "HHHssssssssssHHH",
        "HssssssssssssssH",
        "ssbbbbssssbbbbss",
        "ssssssssssssssss",
        "ssWWWWssssWWWWss",
        "ssWpiWssssWipWss",
        "sssddssssssddsss",
        "ssssssddddssssss",
        "sDssssssssssssDs",
        "DlttttttttttttlD",
        "sDtTtTtTtTtTtTDs",
        "ssDmmmmmmmmmmDss",
        "sssDDDDDDDDDDsss",
        "ssssssssssssssss"], {"W": eye_white, "i": iris})


def hat_face(b):
    # Male_07 as the Hat Toilet: staring eyes, a mustache and goatee, the tip of his tongue showing.
    head16(b, (190, 142, 108), (40, 30, 26), [
        "HHHHHHHHHHHHHHHH",
        "HHHHHHHHHHHHHHHH",
        "HHHssssssssssHHH",
        "HssssssssssssssH",
        "ssbbbbssssbbbbss",
        "ssssssssssssssss",
        "ssWWWWssssWWWWss",
        "ssWWpWssssWpWWss",
        "sssddssssssddsss",
        "ssssssddddssssss",
        "sssHHHHHHHHHHsss",
        "ssHHHssssssHHHss",
        "ssHssDmmmmDssHss",
        "ssssssDllDssssss",
        "ssssssHHHHssssss",
        "sssssHHHHHHsssss"], back_rows=8)


DJ_SKIN = (190, 132, 92)
SHADES = (18, 18, 22)


def dj_face(b):
    # The DJ: short black hair, dark shades, a cool little smile (the beanie covers the brows).
    head16(b, DJ_SKIN, (25, 22, 22), [
        "HHHHHHHHHHHHHHHH",
        "HHHHHHHHHHHHHHHH",
        "HHssssssssssssHH",
        "ssssssssssssssss",
        "ssbbbbssssbbbbss",
        "ssssssssssssssss",
        "SSSSSSSSSSSSSSSS",
        "sSgSSSSssSgSSSSs",
        "ssSSSSssssSSSSss",
        "ssssssddddssssss",
        "ssssssssssssssss",
        "ssssssssssssssss",
        "ssssDssssssDssss",
        "sssssDDDDDDsssss",
        "ssssssssssssssss",
        "ssssssssssssssss"], {"S": SHADES, "g": (200, 220, 255)}, side_rows=2, back_rows=8)


TOILET_PORCELAIN = ["foot", "pedestal", "pedestal_front", "bowl_base", "bowl_base_front", "bowl_base_tip", "bowl",
                    "bowl_front", "bowl_tip", "seat_front", "seat_tip", "seat_side_right", "seat_back", "lid",
                    "lid_top", "lid_crown", "tank", "tank_lid"]
TOILET_TRIM = ["seat_front", "seat_tip", "seat_side_right", "seat_back", "tank_lid"]


def toilet_trim(b, color, noise=3):
    """Colours the edges of the seat and tank lid, and the flush handle, lever and hinges."""
    for name in TOILET_TRIM:
        for r in sides(faces(*b[name])):
            rect(*r, color, noise)
    for name in ("handle", "handle_lever", "hinge_right"):
        box(*b[name], color, noise)


def lid_oval(f):
    """The open seat lid's face: an oval with a raised ring round it, rubber bumpers and a shine."""
    x, y, w, h = f["front"]
    for k in range(h):
        for i in range(w):
            r = math.hypot((i - (w - 1) / 2) / (w / 2), (k - (h - 1) / 2) / (h / 2))
            if 0.62 < r < 0.86:
                px[x + i, y + k] = shade((230, 233, 237), 2)     # the ring
            elif r >= 1.0:
                px[x + i, y + k] = shade((214, 218, 224), 2)     # the corners curve away
    for i, k in ((4, 4), (w - 5, 4), (4, h - 5), (w - 5, h - 5)):
        rect(x + i, y + k, 2, 2, (176, 180, 188), 1)             # bumpers
    rect(x + 6, y + 6, 1, 6, (255, 255, 255), 0)                 # shine
    rect(x + 7, y + 5, 1, 2, (255, 255, 255), 0)


def toilet_porcelain(b):
    """The toilet itself: glazed porcelain, the inside of the bowl with water seen through the seat, an oval
    lid standing open against the tank, chrome hinges and handle."""
    f = porcelain(*b["tank"], GLAZE_HI, (218, 222, 228))
    for x, y, w, h in sides(f):
        rect(x, y, w, 2, SEAM, 2)                                # shadow under the overhanging lid
    x, y, w, h = f["back"]
    rect(x + w // 2 - 3, y + h - 6, 6, 3, (198, 204, 212), 2)    # the water inlet
    f = porcelain(*b["tank_lid"])
    x, y, w, h = f["top"]
    rect(x + 2, y + 2, w - 4, 1, (255, 255, 255), 0)            # glossy highlight along the back
    f = porcelain(*b["lid"], (248, 249, 251), (226, 230, 235))
    lid_oval(f)
    porcelain(*b["lid_top"], (248, 249, 251), (240, 242, 245))
    porcelain(*b["lid_crown"], (248, 249, 251), (240, 242, 245))
    f = porcelain(*b["bowl"], (246, 248, 250), (220, 224, 230))
    x, y, w, h = f["top"]                    # the bowl's inside, seen through the 16x18 hole in the seat
    rect(x + 4, y + 4, 16, 18, (210, 216, 223), 2)              # sloping inner wall
    rect(x + 6, y + 5, 12, 15, (196, 203, 212), 2)
    rect(x + 7, y + 7, 10, 11, (180, 189, 200), 2)
    rect(x + 8, y + 9, 8, 8, WATER, 5)
    rect(x + 10, y + 11, 4, 4, WATER_DEEP, 5)
    rect(x + 9, y + 10, 2, 1, (150, 196, 232), 1)               # glint on the water
    porcelain(*b["bowl_front"], (246, 248, 250), (220, 224, 230))
    porcelain(*b["bowl_tip"], (246, 248, 250), (220, 224, 230))
    for name in ("bowl", "bowl_front", "bowl_tip"):
        x, y, w, h = faces(*b[name])["front"]
        rect(x + 2, y, w - 4, 1, (255, 255, 255), 1)            # shine on the rim
    for name in ("seat_front", "seat_tip", "seat_side_right", "seat_back"):
        f = faces(*b[name])
        rect(*f["top"], (252, 252, 253), 1)
        rect(*f["bottom"], UNDERSIDE, 2)
    toilet_trim(b, SEAM, 2)
    for name in ("handle", "handle_lever", "hinge_right"):
        f = faces(*b[name])
        box(*b[name], CHROME, 6)
        rect(*f["top"], (236, 240, 246), 2)
    for name in ("bowl_base", "bowl_base_front", "bowl_base_tip"):
        porcelain(*b[name], (228, 232, 237), (206, 211, 217))   # narrower and in the bowl's shadow
    for name in ("pedestal", "pedestal_front"):
        porcelain(*b[name], (236, 239, 242), (210, 215, 221))
    porcelain(*b["foot"], (204, 209, 215), (204, 209, 215))


def toilet_headphones(b):
    # The DJ's grey knit beanie and pink headphones
    f = faces(*b["beanie"])
    for r in sides(f):
        x, y, w, h = r
        rect(x, y, w, h, (118, 118, 122), 4)
        for i in range(0, w, 2):
            rect(x + i, y, 1, h - 2, (100, 100, 104), 2)        # knit ribs
        rect(x, y + h - 2, w, 2, (134, 134, 138), 3)            # turned-up edge
    rect(*f["top"], (112, 112, 116), 4)
    pink = (232, 110, 170)
    box(*b["band"], pink, 4)
    rect(*faces(*b["band"])["top"], (246, 140, 190), 3)
    box(*b["band_side_right"], pink, 4)
    f = faces(*b["cup_right"])
    box(*b["cup_right"], (210, 90, 150), 4)
    rect(*f["left"], (40, 40, 44), 2)                           # cushion against the head
    x, y, w, h = f["right"]
    rect(x + 1, y + 2, w - 2, h - 4, (246, 140, 190), 3)        # a lighter disc on the outside


HALO_GOLD = (240, 200, 70)


def toilet_halo(b):
    for name in ("halo_front", "halo_back", "halo_side_right"):
        f = faces(*b[name])
        box(*b[name], HALO_GOLD, 6)
        rect(*f["top"], (255, 226, 120), 4)


def toilet_cap(b):
    # A grey soldier's soft cap with a darker band and visor
    f = faces(*b["crown"])
    for r in sides(f):
        x, y, w, h = r
        rect(x, y, w, h, (108, 112, 102), 4)
        rect(x, y + h - 2, w, 2, (82, 86, 78), 3)
    rect(*f["top"], (114, 118, 108), 4)
    x, y, w, h = f["front"]
    rect(x + w // 2 - 1, y + 1, 2, 2, (150, 140, 90), 2)        # badge
    box(*b["visor"], (72, 74, 68), 3)


def toilet_jetpack(b):
    # Jetpacks on the sides of the tank, armour plates on the bowl and a cover on the flusher
    f = faces(*b["thruster_right"])
    for r in sides(f):
        gradient(r, (88, 90, 96), (62, 64, 70), 4)
        x, y, w, h = r
        for k in (4, 10, 16):
            rect(x, y + k, w, 1, (46, 48, 52), 0)               # segment bands
    rect(*f["top"], (100, 104, 110), 3)
    f = faces(*b["nozzle_right"])
    box(*b["nozzle_right"], (40, 40, 44), 3)
    rect(*f["bottom"], (230, 120, 40), 6)
    x, y, w, h = f["bottom"]
    rect(x + 2, y + 2, w - 4, h - 4, (255, 200, 90), 4)         # the hot core
    f = faces(*b["armor_front"])
    box(*b["armor_front"], (96, 98, 104), 5)
    x, y, w, h = f["front"]
    for i in (1, w - 2):
        for k in (1, h - 2):
            px[x + i, y + k] = (150, 152, 160, 255)              # rivets
    rect(x + w // 2, y, 1, h, (76, 78, 84), 0)                  # where the plates meet
    box(*b["armor_side_right"], (90, 92, 98), 5)
    box(*b["flusher_cover"], (100, 102, 108), 4)


def toilet_tint(b, factor):
    """Darkens the porcelain to grey (an armoured toilet), leaving the head and extras."""
    for name in TOILET_PORCELAIN + ["handle", "handle_lever", "hinge_right"]:
        u, v, w, h, d = b[name]
        for x in range(u, u + 2 * (w + d)):
            for y in range(v, v + d + h):
                r, g, bl, a = px[x, y]
                if a:
                    px[x, y] = (round(r * factor), round(g * factor), round(bl * factor * 1.04), a)


def toilet_spider_leg(b):
    # The spider toilet's legs (SpiderToiletModel uses this cube's UV for all eight)
    u, v, w, h, d = b["spider_leg"]
    box(u, v, w, h, d, (44, 42, 46), 6)
    for r in sides(faces(u, v, w, h, d)):
        x, y, rw, rh = r
        for i in range(4, rw, 10):
            rect(x + i, y, 1, rh, (24, 22, 26), 0)              # joints


def toilet(seed, face, *extras):
    b = bb_boxes("skibidi_toilet")
    new_image(256, seed)
    face(b)
    toilet_porcelain(b)
    for extra in extras:
        extra(b)
    return img


# ---------------------------------------------------------------- alliance

SHIRT = (235, 235, 235)
TIE = (18, 18, 22)
SHOE = (16, 16, 16)
CAMERA_DARK = (52, 54, 60)
GLASS = (26, 44, 88)


def lens_glass(x, y, w, h):
    """A round lens on a w x h face: dark rim, deep blue glass, one glint."""
    for i in range(w):
        for k in range(h):
            r = math.hypot(i - (w - 1) / 2, k - (h - 1) / 2) / (min(w, h) / 2)
            if r <= 0.62:
                px[x + i, y + k] = shade(GLASS, 4)
            elif r <= 1.15:
                px[x + i, y + k] = shade((22, 22, 26), 2)
    px[x + w // 2 - 1, y + h // 2 - 1] = (150, 190, 240, 255)


SPEAKER_BODY = (28, 28, 31)


def speaker_cone(i, k, n=6):
    """Color of pixel (i, k) of an n x n round speaker: dark surround, grey cone, dark dust cap."""
    r = math.hypot(i - (n - 1) / 2, k - (n - 1) / 2)
    if r <= n * 0.18:
        return (44, 44, 48)
    if r <= n * 0.4:
        return (128 - int(r * 10), 130 - int(r * 10), 138 - int(r * 10))
    return (70, 70, 76)


def tv_screen(x, y, w, h, seed):
    """Static on a CRT: grey-blue noise, darker scanlines, brighter in the middle, a glare in one corner."""
    rnd = random.Random(seed)
    for i in range(w):
        for k in range(h):
            centre = 1 - 0.5 * (abs(i - (w - 1) / 2) / w + abs(k - (h - 1) / 2) / h)
            v = rnd.randint(140, 235) * centre * (0.84 if k % 2 else 1.0)
            px[x + i, y + k] = (int(v * 0.92), int(v * 0.96), min(255, int(v * 1.08)), 255)
    px[x, y] = (236, 240, 248, 255)
    px[x + 1, y] = (214, 220, 232, 255)
    px[x, y + 1] = (214, 220, 232, 255)


# ---- The regular Alliance at twice the detail (cameraman.bbmodel etc., built on author_suited's body)

JACKET = (34, 35, 40)
JACKET_DARK = (24, 25, 29)
GLOVE = (30, 30, 34)


def suited_body(b, jacket=JACKET, jacket_dark=JACKET_DARK, shirt=SHIRT, tie=TIE, glove=GLOVE,
                trouser=(30, 31, 36), shoe=SHOE, v_width=5, v_depth=12, double_breasted=False):
    """A suit jacket open over a white shirt (lapels and tie are their own cubes), buttons and pocket flaps, white
    shirt cuffs, gloves, creased trousers and polished shoes. With the coat cubes (author_suited's coat=True) the
    jacket is an overcoat: a skirt to the knees and a turned-up collar; double_breasted gives it two rows of
    buttons, and v_width/v_depth size the opening at the neck."""
    edge = tuple(min(255, c + 14) for c in jacket)
    f = faces(*b["jacket"])
    for r in sides(f):
        gradient(r, tuple(min(255, c + 6) for c in jacket), jacket, 3)
    rect(*f["top"], tuple(min(255, c + 8) for c in jacket), 3)
    rect(*f["bottom"], jacket_dark, 2)
    x, y, w, h = f["front"]
    for k in range(h):                                      # the shirt shows in a V down to the top button
        half = max(0, v_width - k * v_width // v_depth)
        if half:
            rect(x + w // 2 - half, y + k, 2 * half, 1, shirt, 2)
    button = (70, 70, 76, 255) if not double_breasted else tuple(min(255, c + 34) for c in jacket) + (255,)
    if double_breasted:
        for k in (v_depth + 1, v_depth + 6):
            px[x + w // 2 - 3, y + k] = button
            px[x + w // 2 + 3, y + k] = button
    else:
        px[x + w // 2, y + v_depth] = button                # top button
    rect(x + 2, y + 10, 4, 1, jacket_dark, 0)               # breast pocket welt
    for name in ("right", "left"):
        x, y, w, h = f[name]
        rect(x + w // 2, y + 1, 1, h - 1, jacket_dark, 0)   # side seam
    x, y, w, h = f["back"]
    rect(x + w // 2, y + 2, 1, h - 2, jacket_dark, 0)       # centre back seam
    f = faces(*b["jacket_waist"])
    for r in sides(f):
        gradient(r, jacket, jacket_dark, 3)
    x, y, w, h = f["front"]
    if double_breasted:
        px[x + w // 2 - 3, y + 2] = button
        px[x + w // 2 + 3, y + 2] = button
        rect(x, y + 4, w, 1, jacket_dark, 0)                # belt line
    else:
        px[x + w // 2, y + 2] = button                      # lower button
    rect(x + 1, y + 5, 4, 1, jacket_dark, 0)                # pocket flaps
    rect(x + w - 5, y + 5, 4, 1, jacket_dark, 0)
    if "jacket_hem" in b:
        f = faces(*b["jacket_hem"])
        for r in sides(f):
            gradient(r, jacket, jacket_dark, 3)
        x, y, w, h = f["front"]
        rect(x + w // 2, y, 1, h, jacket_dark, 0)           # where the jacket opens
        x, y, w, h = f["back"]
        rect(x + w // 2, y, 1, h, jacket_dark, 0)           # back vent
    if "coat_back" in b:
        for name in ("coat_back", "coat_side_right", "coat_front_right"):
            f = faces(*b[name])
            for r in sides(f):
                gradient(r, jacket, jacket_dark, 3)
            rect(*f["bottom"], jacket_dark, 2)
        x, y, w, h = faces(*b["coat_back"])["back"]
        rect(x + w // 2, y + h // 2, 1, h - h // 2, jacket_dark, 0)  # back vent
        x, y, w, h = faces(*b["coat_front_right"])["front"]
        rect(x + w - 1, y, 1, h, jacket_dark, 0)            # the coat's front edge
    if "coat_collar" in b:
        f = faces(*b["coat_collar"])
        for r in f.values():
            rect(*r, tuple(min(255, c + 6) for c in jacket), 3)
        x, y, w, h = f["back"]
        rect(x, y + h - 1, w, 1, jacket_dark, 0)            # fold
    box(*b["collar"], shirt, 2)
    f = faces(*b["lapel_right"])
    for r in f.values():
        rect(*r, tuple(min(255, c + 10) for c in jacket), 2)  # satin lapels, a shade lighter than the cloth
    x, y, w, h = f["front"]
    for k in range(h):
        px[x + min(w - 1, k // 3), y + k] = shade(edge, 2)  # the lapel's edge catching the light
    if "tie" in b:
        f = faces(*b["tie"])
        for r in f.values():
            rect(*r, tie, 2)
        x, y, w, h = f["front"]
        rect(x, y, w, 1, tuple(max(0, c - 8) for c in tie), 0)  # knot
        rect(x + w - 1, y + 1, 1, h - 2, tuple(min(255, c + 20) for c in tie), 0)  # sheen
    f = faces(*b["sleeve_right"])
    for r in sides(f):
        gradient(r, tuple(min(255, c + 6) for c in jacket), jacket, 3)
        x, y, w, h = r
        rect(x + w // 2, y + h // 2, 1, h // 2, jacket_dark, 0)  # crease at the elbow
    rect(*f["top"], tuple(min(255, c + 8) for c in jacket), 3)
    rect(*f["bottom"], jacket_dark, 2)
    box(*b["cuff_right"], shirt, 2)
    gloves_trousers_shoes(b, glove, trouser, shoe)


def gloves_trousers_shoes(b, glove=GLOVE, trouser=(30, 31, 36), shoe=SHOE):
    """The gloved hands, creased trousers and polished shoes of the suited bodies."""
    f = faces(*b["hand_right"])
    box(*b["hand_right"], glove, 3)
    x, y, w, h = f["front"]
    line = tuple(max(0, c - 12) for c in glove)
    rect(x, y + 2, w, 1, line, 0)                           # knuckles
    for i in range(1, w, 2):
        rect(x + i, y + 3, 1, h - 3, line, 0)               # fingers
    f = faces(*b["trouser_right"])
    for r in sides(f):
        gradient(r, trouser, tuple(max(0, c - 6) for c in trouser), 3)
    x, y, w, h = f["front"]
    rect(x + w // 2, y, 1, h, tuple(max(0, c - 10) for c in trouser), 0)  # crease
    f = faces(*b["shoe_right"])
    box(*b["shoe_right"], shoe, 2)
    x, y, w, h = f["top"]
    rect(x + 1, y + h - 3, w - 2, 2, (40, 40, 44), 2)       # shine on the toe caps
    rect(x + 2, y + 2, w - 4, 2, (26, 26, 30), 0)           # laces
    rect(*f["bottom"], (10, 10, 10), 0)


CCTV_WHITE = (226, 228, 232)


def cameraman_cctv(b):
    """Cameraman 1.0's head: a white CCTV camera with a sun shield, black front plate, lens barrel and sticker."""
    box(*b["bracket"], (92, 94, 102), 3)
    box(*b["bracket_plate"], (110, 112, 120), 3)
    box(*b["cable_right"], (20, 20, 24), 1)
    f = faces(*b["housing"])
    for r in sides(f):
        gradient(r, CCTV_WHITE, (196, 199, 205), 3)
    rect(*f["top"], CCTV_WHITE, 2)
    rect(*f["bottom"], (170, 173, 180), 3)
    x, y, w, h = f["right"]
    rect(x + 3, y + 2, 4, 3, (236, 196, 40), 3)             # yellow warning sticker
    px[x + 5, y + 3] = (20, 20, 20, 255)
    rect(x + 9, y + 1, 5, 1, (200, 60, 50), 0)              # a red and orange stripe at the back
    rect(x + 9, y + 2, 5, 1, (230, 150, 50), 0)
    x, y, w, h = f["left"]
    rect(x + 2, y + 1, w - 4, 1, (180, 183, 190), 0)        # seam
    f = faces(*b["shield"])
    rect(*f["top"], (238, 240, 243), 2)
    rect(*f["bottom"], (150, 153, 160), 2)
    for r in sides(f):
        rect(*r, (214, 216, 222), 2)
    f = faces(*b["front_plate"])
    box(*b["front_plate"], (26, 26, 30), 2)
    x, y, w, h = f["front"]
    rect(x + 1, y + 1, w - 2, h - 2, (16, 22, 34), 2)       # the glass
    px[x + 2, y + 2] = (120, 150, 190, 255)                  # glint
    f = faces(*b["lens_barrel"])                              # the round flashlight under the camera
    for r in sides(f):
        gradient(r, (214, 216, 222), (172, 175, 182), 3)
    rect(*f["top"], (206, 208, 214), 2)
    rect(*f["bottom"], (160, 163, 170), 2)
    rect(*f["back"], (150, 153, 160), 2)
    x, y, w, h = f["front"]
    rect(x, y, w, h, (196, 199, 205), 2)
    lens_glass(x + 1, y + 1, w - 2, h - 2)
    box(*b["junction_box"], (150, 154, 162), 3)
    box(*b["back_cap"], (184, 187, 194), 3)


SPEAKER_GREY = (122, 124, 128)


def speakerman_box(b):
    """Speakerman 1.0's head: a grey speaker cabinet with grille cloth on the sides, a bevelled front with corner
    screws and a small logo, a big woofer standing proud of it, and a terminal plate on the back."""
    box(*b["neck"], (34, 34, 38), 2)
    f = faces(*b["cabinet"])
    for name in ("right", "left", "back"):
        x, y, w, h = f[name]
        gradient(f[name], (128, 130, 134), (108, 110, 114), 2)
        for i in range(1, w - 1):
            for k in range(1, h - 1):
                if (i + k) % 2 == 0:
                    px[x + i, y + k] = shade((100, 102, 106), 2)  # grille cloth
    rect(*f["front"], SPEAKER_GREY, 2)
    rect(*f["top"], (138, 140, 144), 2)
    rect(*f["bottom"], (96, 98, 102), 2)
    f = faces(*b["baffle"])
    for r in f.values():
        rect(*r, (140, 142, 146), 2)                        # the rounded edge catching the light
    x, y, w, h = f["front"]
    rect(x, y, w, h, (130, 132, 136), 2)
    for sx, sy in ((x, y), (x + w - 1, y), (x, y + h - 1), (x + w - 1, y + h - 1)):
        px[sx, sy] = (54, 54, 58, 255)                       # corner screws
    rect(x + w - 4, y + 1, 3, 1, (236, 236, 238), 0)        # logo
    px[x + w - 4, y + 2] = (200, 40, 40, 255)

    def woofer(i, k):
        """Colour at (i, k) of the 7x7 woofer, from its centre out: cone with rings, rubber surround, frame."""
        r = math.hypot(i - 3, k - 3)
        if r <= 2.0:
            return (84, 86, 90) if round(r * 2) % 2 else (70, 72, 76)
        if r <= 2.9:
            return (38, 38, 42)
        return (164, 166, 170)
    for name, ox, oy in (("woofer_wide", 0, 1), ("woofer_tall", 1, 0)):
        f = faces(*b[name])
        for r in f.values():
            rect(*r, (80, 82, 86), 2)
        x, y, w, h = f["front"]
        for i in range(w):
            for k in range(h):
                px[x + i, y + k] = shade(woofer(i + ox, k + oy), 2)
    f = faces(*b["dust_cap"])
    box(*b["dust_cap"], (40, 40, 44), 2)
    x, y, w, h = f["front"]
    px[x, y] = (120, 122, 128, 255)                          # glint
    f = faces(*b["terminal_plate"])
    box(*b["terminal_plate"], (22, 22, 24), 2)
    x, y, w, h = f["back"]
    rect(x + 1, y + 1, 1, 1, (200, 36, 36), 0)              # red and black terminals
    rect(x + 3, y + 1, 1, 1, (10, 10, 10), 0)
    rect(x + 1, y + h - 2, w - 2, 1, (180, 40, 40), 2)      # label


def speakerman():
    b = bb_boxes("speakerman")
    new_image(128, 9)
    suited_body(b, tie=(96, 196, 84))
    speakerman_box(b)
    return img


TV_DARK = (40, 42, 46)


def tv_man_crt(b):
    """TV Man 1.0's head: a dark CRT TV with a hooded top over a screen of static, a thick bottom bezel with knobs
    and a power light, and a stepped back with vents."""
    box(*b["neck"], (34, 34, 38), 2)
    f = faces(*b["case"])
    for r in sides(f):
        gradient(r, (46, 48, 52), (34, 36, 40), 2)
    x, y, w, h = f["top"]
    rect(x, y, w, h, (48, 50, 54), 2)
    for i in range(2, w - 2, 2):
        rect(x + i, y + 1, 1, h - 3, (30, 30, 34), 0)       # vent slots
    rect(*f["bottom"], (30, 32, 36), 2)
    x, y, w, h = f["front"]
    rect(x, y, w, h, (20, 20, 24), 0)                       # deep in the bezel
    tv_screen(x + 2, y + 2, w - 4, h - 5, 4)
    f = faces(*b["hood"])
    for r in f.values():
        rect(*r, (48, 50, 54), 2)
    x, y, w, h = f["front"]
    rect(x, y + h - 1, w, 1, (62, 64, 70), 2)               # the hood's lip
    rect(*f["bottom"], (26, 26, 30), 2)
    f = faces(*b["bezel_side_right"])
    for r in f.values():
        rect(*r, (50, 52, 56), 2)
    rect(*f["front"], (58, 60, 66), 2)
    f = faces(*b["bezel_bottom"])
    for r in f.values():
        rect(*r, (46, 48, 52), 2)
    x, y, w, h = f["front"]
    rect(x, y, w, h, (54, 56, 62), 2)
    rect(x + 2, y + 1, 3, 1, (150, 152, 160), 2)            # badge
    for i in (w - 6, w - 4):
        px[x + i, y + 1] = (24, 24, 28, 255)                # knobs
    px[x + w - 2, y + 1] = (60, 220, 90, 255)                # power light
    f = faces(*b["tube"])
    for r in sides(f):
        gradient(r, (40, 42, 46), (30, 32, 36), 2)
    rect(*f["top"], (44, 46, 50), 2)
    rect(*f["bottom"], (28, 30, 34), 2)
    for name in ("right", "left"):
        x, y, w, h = f[name]
        for k in range(2, h - 2, 2):
            rect(x + 1, y + k, w - 2, 1, (24, 24, 28), 0)   # side vents
    f = faces(*b["tube_back"])
    for r in f.values():
        rect(*r, (34, 36, 40), 2)
    x, y, w, h = f["back"]
    for k in range(1, h - 2, 2):
        rect(x + 1, y + k, w - 2, 1, (24, 24, 28), 0)       # back vents
    rect(x + 2, y + h - 2, 3, 1, (176, 176, 168), 1)        # label


def tv_man():
    b = bb_boxes("tv_man")
    new_image(128, 9)
    suited_body(b, jacket=(56, 58, 62), jacket_dark=(42, 44, 48), tie=(30, 30, 34), trouser=(36, 37, 42),
                v_width=3, v_depth=6, double_breasted=True)
    tv_man_crt(b)
    return img


def cameraman():
    b = bb_boxes("cameraman")
    new_image(128, 9)
    suited_body(b)
    cameraman_cctv(b)
    return img


def large_camcorder(b):
    """Large Cameraman 1.0's head: a black cinema camera on a baseplate and silver rails, a matte box with flags
    round the lens, a twin-reel film magazine on top, a top handle, a viewfinder and a little side monitor."""
    body, body_dark = (40, 42, 46), (28, 30, 34)
    box(*b["neck"], (30, 30, 34), 2)
    box(*b["baseplate"], (54, 56, 60), 2)
    box(*b["rail_right"], (156, 158, 164), 3)
    f = faces(*b["camera_body"])
    for r in sides(f):
        gradient(r, body, body_dark, 2)
    rect(*f["top"], (46, 48, 52), 2)
    rect(*f["bottom"], body_dark, 2)
    for name in ("right", "left"):
        x, y, w, h = f[name]
        rect(x + 1, y + 1, w - 2, 1, (56, 58, 64), 0)       # panel seams
        rect(x + 1, y + h - 2, w - 2, 1, (56, 58, 64), 0)
    x, y, w, h = f["left"]
    rect(x + 2, y + 3, 4, 1, (226, 226, 222), 0)            # a white label...
    rect(x + 2, y + 5, 2, 1, (222, 120, 40), 0)             # ...an orange tab
    for i in (8, 10):
        px[x + i, y + 4] = (90, 92, 98, 255)                 # buttons
    x, y, w, h = f["front"]
    px[x + w - 2, y + 1] = (230, 30, 30, 255)                # red recording light
    f = faces(*b["lens"])
    for r in sides(f):
        rect(*r, (30, 30, 34), 2)
        x, y, w, h = r
        rect(x, y + 1, w, 1, (70, 72, 78), 0)               # focus and zoom rings
    f = faces(*b["matte_box"])
    for r in f.values():
        rect(*r, (32, 32, 36), 2)
    x, y, w, h = f["front"]
    rect(x + 1, y + 1, w - 2, h - 2, (12, 12, 14), 0)       # looking into the box...
    lens_glass(x + w // 2 - 2, y + h // 2 - 2, 4, 4)        # ...at the lens
    for name in ("top_flag", "side_flag_right"):
        f = faces(*b[name])
        for r in f.values():
            rect(*r, (30, 30, 34), 2)
        rect(*f["bottom" if name == "top_flag" else "left"], (20, 20, 22), 0)

    def reel(face, cx, cy):
        x, y, w, h = face
        for i in range(w):
            for k in range(h):
                r = math.hypot(i - cx, k - cy)
                c = (150, 152, 158) if r < 0.9 else (34, 36, 40) if r < 1.9 else (52, 54, 58)
                px[x + i, y + k] = shade(c, 2)
    for which in ("front", "back"):
        f = faces(*b[f"reel_{which}_wide"])
        for r in f.values():
            rect(*r, (52, 54, 58), 2)
        for side in ("right", "left"):
            reel(f[side], 3.5, 2)
        f = faces(*b[f"reel_{which}_tall"])
        for r in f.values():
            rect(*r, (52, 54, 58), 2)
        rect(*f["top"], (64, 66, 70), 2)
        for side in ("right", "left"):
            reel(f[side], 2, 3.5)
    for name in ("top_handle", "handle_post"):
        box(*b[name], (24, 24, 28), 2)
    f = faces(*b["viewfinder"])
    box(*b["viewfinder"], (30, 30, 34), 2)
    f = faces(*b["eyepiece"])
    box(*b["eyepiece"], (20, 20, 22), 2)
    rect(*f["back"], (12, 12, 14), 0)                       # rubber eyecup
    f = faces(*b["side_monitor"])
    box(*b["side_monitor"], (30, 30, 34), 2)
    x, y, w, h = f["right"]
    rect(x + 1, y + 1, w - 2, h - 2, (60, 90, 120), 3)      # screen
    px[x + 1, y + 1] = (150, 190, 220, 255)
    f = faces(*b["battery"])
    box(*b["battery"], (46, 48, 52), 2)
    x, y, w, h = f["back"]
    rect(x + 1, y + 2, w - 2, 2, (200, 200, 196), 1)        # battery label
    px[x + w - 2, y + 1] = (60, 220, 90, 255)                # charge light


def large_cameraman():
    b = bb_boxes("large_cameraman")
    new_image(128, 9)
    suited_body(b, jacket=(30, 30, 34), jacket_dark=(20, 20, 24), shirt=(214, 112, 34), tie=(16, 16, 18))
    large_camcorder(b)
    return img


def shirt_body(b, shirt=SHIRT, tie=(150, 26, 32), belt=(24, 24, 26), glove=GLOVE, trouser=(34, 36, 46),
               shoe=SHOE):
    """No jacket: a white dress shirt with a button placket, a breast pocket and creases, a standing collar,
    buttoned cuffs, a tie and a belt with a buckle; then gloves, trousers and shoes."""
    shirt_shade = tuple(c - 30 for c in shirt)
    seam = tuple(c - 22 for c in shirt)
    f = faces(*b["shirt"])
    for r in sides(f):
        gradient(r, shirt, shirt_shade, 3)
    rect(*f["top"], shirt, 3)
    x, y, w, h = f["front"]
    rect(x + w // 2 + 1, y, 1, h, seam, 0)                  # button placket...
    for k in range(3, h, 4):
        px[x + w // 2 + 1, y + k] = (250, 250, 250, 255)     # ...and its buttons
    rect(x + w - 6, y + 4, 4, 1, seam, 0)                   # breast pocket
    rect(x + w - 6, y + 4, 1, 4, seam, 0)
    rect(x + w - 3, y + 4, 1, 4, seam, 0)
    rect(x + w - 6, y + 8, 4, 1, seam, 0)
    for k in (h - 6, h - 3):
        rect(x + 2, y + k, 3, 1, seam, 0)                   # creases where it's tucked in
        rect(x + w - 5, y + k + 1, 3, 1, seam, 0)
    x, y, w, h = f["back"]
    rect(x, y + 3, w, 1, seam, 0)                           # yoke
    rect(x + w // 2, y + 4, 1, 3, seam, 0)                  # box pleat
    for name in ("right", "left"):
        x, y, w, h = f[name]
        rect(x + w // 2, y, 1, h, seam, 0)                  # side seams
    f = faces(*b["belt"])
    for r in f.values():
        rect(*r, belt, 2)
    x, y, w, h = f["front"]
    rect(x + w // 2 - 1, y, 3, h, (176, 178, 186), 2)       # buckle
    px[x + w // 2, y + 1] = belt + (255,)
    f = faces(*b["collar"])
    for r in f.values():
        rect(*r, shirt, 2)
    x, y, w, h = f["front"]
    rect(x + w // 2 - 1, y, 3, h, seam, 0)                  # where the collar points meet
    f = faces(*b["tie"])
    for r in f.values():
        rect(*r, tie, 2)
    x, y, w, h = f["front"]
    rect(x, y, w, 1, tuple(max(0, c - 30) for c in tie), 0)  # knot
    rect(x + w - 1, y + 1, 1, h - 2, tuple(min(255, c + 30) for c in tie), 0)  # sheen
    f = faces(*b["shirt_sleeve_right"])
    for r in sides(f):
        gradient(r, shirt, shirt_shade, 3)
        x, y, w, h = r
        rect(x + 2, y + h // 2, w - 4, 1, seam, 0)          # crease at the elbow
        rect(x + w // 2, y + h // 2 + 3, 1, 3, seam, 0)
    rect(*f["top"], shirt, 3)
    rect(*f["bottom"], shirt_shade, 2)
    f = faces(*b["cuff_right"])
    box(*b["cuff_right"], shirt, 2)
    x, y, w, h = f["front"]
    px[x + w - 2, y + 1] = (250, 250, 250, 255)              # cuff button
    rect(x, y, w, 1, seam, 0)
    gloves_trousers_shoes(b, glove, trouser, shoe)


def large_speaker(b):
    """Large Speakerman 1.0's head: a tall black cabinet with a bevelled front, corner screws, a big grey woofer,
    a silver tweeter, a column of small drivers and a bass port on the back."""
    box(*b["neck"], (36, 36, 40), 2)
    f = faces(*b["cabinet"])
    for r in sides(f):
        gradient(r, (34, 34, 38), (24, 24, 27), 2)
    rect(*f["top"], (40, 40, 44), 2)
    rect(*f["bottom"], (22, 22, 24), 2)
    x, y, w, h = f["top"]
    rect(x + 4, y + h // 2, w - 8, 1, (16, 16, 18), 0)      # handle slot
    f = faces(*b["baffle"])
    for r in f.values():
        rect(*r, (50, 50, 56), 2)                           # the bevel catching the light
    x, y, w, h = f["front"]
    rect(x, y, w, h, (38, 38, 42), 2)
    for sx, sy in ((x, y), (x + w - 1, y), (x, y + h - 1), (x + w - 1, y + h - 1)):
        px[sx, sy] = (96, 98, 104, 255)                      # corner screws
    for k in (3, 7, 11):                                    # small drivers down his left side
        rect(x + w - 3, y + k, 2, 2, (20, 20, 22), 0)
        px[x + w - 3, y + k] = (120, 122, 128, 255)

    def woofer(i, k):
        """Colour at (i, k) of the 12x12 woofer: grey cone with rings, black surround, a silver frame."""
        r = math.hypot(i - 5.5, k - 5.5)
        if r <= 3.6:
            return (112, 114, 120) if round(r) % 2 else (98, 100, 106)
        if r <= 5.0:
            return (22, 22, 24)
        return (150, 152, 158)
    for name, ox, oy in (("woofer_wide", 0, 2), ("woofer_tall", 2, 0)):
        f = faces(*b[name])
        for r in f.values():
            rect(*r, (70, 72, 76), 2)
        x, y, w, h = f["front"]
        for i in range(w):
            for k in range(h):
                px[x + i, y + k] = shade(woofer(i + ox, k + oy), 2)
    f = faces(*b["dust_cap"])
    box(*b["dust_cap"], (44, 44, 48), 2)
    x, y, w, h = f["front"]
    rect(x + 1, y + 1, 2, 2, (60, 60, 66), 2)
    px[x + 1, y + 1] = (130, 132, 140, 255)                  # glint
    f = faces(*b["tweeter"])
    box(*b["tweeter"], (30, 30, 34), 2)
    x, y, w, h = f["front"]
    rect(x + 1, y + 1, 2, 2, (176, 178, 186), 3)            # silver dome
    px[x + 1, y + 1] = (232, 234, 240, 255)
    f = faces(*b["port"])
    box(*b["port"], (30, 30, 34), 2)
    x, y, w, h = f["back"]
    rect(x + 1, y + 1, w - 2, h - 2, (8, 8, 10), 0)         # bass port


def large_speakerman():
    b = bb_boxes("large_speakerman")
    new_image(128, 10)
    shirt_body(b)
    large_speaker(b)
    return img


def crt_static(r, rnd, bright=(150, 240)):
    """A TV screen full of white static, every other row a little darker like scan lines."""
    x, y, w, h = r
    for i in range(w):
        for k in range(h):
            v = rnd.randint(*bright) * (0.88 if k % 2 else 1.0)
            px[x + i, y + k] = (int(v), int(v), min(255, int(v * 1.04)), 255)


def large_tv_man():
    # Large TV Man 1.0 (large_tv_man.bbmodel): a big black CRT showing bright static on a bracket and a pipe, a
    # black suit with a purple tie, and four little static TVs on jointed metal cables from his back
    b = bb_boxes("large_tv_man")
    new_image(128, 11)
    rnd = random.Random(45)
    suited_body(b, jacket=(28, 28, 32), jacket_dark=(20, 20, 24), tie=(118, 52, 186))
    plastic, plastic_dark = (36, 36, 40), (24, 24, 28)
    metal = (112, 114, 122)
    for name in ("tv", "side_tv_upper_right", "side_tv_lower_right"):
        f = faces(*b[name])
        for r in sides(f):
            gradient(r, plastic, plastic_dark, 3)
        rect(*f["top"], (40, 40, 44), 3)
        rect(*f["bottom"], plastic_dark, 3)
        x, y, w, h = f["front"]
        if name == "tv":
            crt_static((x + 3, y + 2, w - 6, h - 6), rnd, (190, 255))  # behind the bezel cubes
            x, y, w, h = f["top"]
            for i in range(2, w - 2, 2):
                rect(x + i, y + 2, 1, h - 4, (26, 26, 30), 0)   # vents
        else:
            rect(x, y, w, h, (18, 18, 20), 2)                # bezel...
            crt_static((x + 1, y + 1, w - 2, h - 3), rnd)    # ...round the static
            px[x + w - 2, y + h - 1] = (60, 220, 90, 255)     # power light
            x, y, w, h = f["back"]
            for k in range(1, h - 1, 2):
                rect(x + 1, y + k, w - 2, 1, (18, 18, 20), 0)  # vents
    for name in ("bezel_top", "bezel_bottom", "bezel_side_right"):
        f = faces(*b[name])
        for r in f.values():
            rect(*r, (44, 44, 48), 2)
    x, y, w, h = faces(*b["bezel_bottom"])["front"]
    rect(x + 3, y + 1, 4, 1, (150, 152, 160), 2)            # badge
    for i in (w - 8, w - 5):
        rect(x + i, y + 1, 2, 2, (22, 22, 26), 0)           # dials
    px[x + w - 2, y + 2] = (60, 220, 90, 255)                # power light
    f = faces(*b["tv_back"])
    for r in f.values():
        rect(*r, plastic_dark, 3)
    x, y, w, h = f["back"]
    for k in range(1, h - 1, 2):
        rect(x + 1, y + k, w - 2, 1, (16, 16, 18), 0)       # vents
    for name in ("pipe", "rod", "knob_right"):
        box(*b[name], metal, 4)
    box(*b["bracket"], (40, 40, 44), 3)
    for name in ("cable_upper_inner_right", "cable_upper_outer_right", "cable_lower_inner_right",
                 "cable_lower_outer_right"):
        f = faces(*b[name])
        for r in f.values():
            rect(*r, (96, 98, 106), 3)
        for key in ("top", "bottom", "front", "back"):
            x, y, w, h = f[key]
            for i in range(0, w, 2):
                rect(x + i, y, 1, h, (62, 64, 70), 0)       # the joints along the cable
    return img


def gear():
    # The gear a player can buy (gear.bbmodel): a twin-tank jetpack, shoulder CCTV cameras, shoulder speakers,
    # back blades, chest plates and pauldrons. Each piece is drawn on whatever model the player has.
    b = bb_boxes("gear")
    new_image(64, 12)
    gunmetal, gunmetal_dark = (70, 72, 80), (48, 50, 56)
    steel, steel_dark = (150, 154, 162), (112, 116, 124)
    f = faces(*b["tank_right"])
    for r in sides(f):
        gradient(r, gunmetal, gunmetal_dark, 3)
        x, y, w, h = r
        rect(x, y + 2, w, 1, (230, 140, 40), 2)            # orange warning band
    rect(*f["top"], (86, 88, 96), 2)
    box(*b["pack"], gunmetal_dark, 3)
    f = faces(*b["nozzle_right"])
    for r in sides(f):
        rect(*r, (36, 36, 40), 2)
    rect(*f["bottom"], (255, 150, 60), 6)                  # hot nozzle
    f = faces(*b["camera_right"])
    for r in f.values():
        gradient(r, (196, 198, 204), (160, 162, 170), 3)
    x, y, w, h = f["front"]
    px[x + w - 1, y] = (230, 30, 30, 255)                   # recording light
    box(*b["camera_lens_right"], (30, 30, 34), 2)
    x, y, w, h = faces(*b["camera_lens_right"])["front"]
    rect(x, y, w, h, (70, 150, 220), 6)
    for name in ("camera_mount_right", "speaker_mount_right"):
        box(*b[name], (60, 60, 66), 2)
    f = faces(*b["speaker_right"])
    for r in f.values():
        rect(*r, (34, 34, 38), 3)
    x, y, w, h = f["front"]
    for i in range(w):
        for k in range(h):
            r = math.hypot(i - (w - 1) / 2, k - (h - 1) / 2)
            px[x + i, y + k] = shade((150, 152, 160) if r < 0.8 else (90, 92, 100) if r < 1.6 else (34, 34, 38), 2)
    box(*b["blade_mount"], (36, 36, 40), 3)
    for i in range(3):
        f = faces(*b[f"blade{i}_right"])
        for r in f.values():
            rect(*r, (30, 30, 34), 2)
        x, y, w, h = f["front"]
        rect(x, y, w, h, (170, 80, 240), 6)                 # purple edge
    for name in ("chest_plate", "belly_plate", "pauldron_right"):
        f = faces(*b[name])
        for r in f.values():
            gradient(r, steel, steel_dark, 4)
        for r in sides(f):
            x, y, w, h = r
            if w >= 3 and h >= 3:
                for cx, cy in ((x, y), (x + w - 1, y), (x, y + h - 1), (x + w - 1, y + h - 1)):
                    px[cx, cy] = (84, 86, 94, 255)          # rivets in the corners
    return img


GOLD = (212, 175, 55)


def commander():
    # A Cameraman (on the Cameraman's model) in a navy dress suit with a red tie, a gold badge, gold shoulder boards
    # and gold cuffs.
    b = bb_boxes("cameraman")
    new_image(128, 9)
    suited_body(b, jacket=(31, 42, 68), jacket_dark=(22, 30, 50), tie=(178, 34, 34))
    x, y, w, h = faces(*b["jacket"])["front"]
    rect(x + 2, y + 6, 2, 3, GOLD, 4)                       # badge on the jacket front
    rect(*faces(*b["sleeve_right"])["top"], GOLD, 4)        # shoulder boards
    box(*b["cuff_right"], GOLD, 4)                          # gold cuffs instead of shirt cuffs
    cameraman_cctv(b)
    return img


# ---------------------------------------------------------------- titans and G-Toilet
# Titans are modelled at four times the detail (4 texels per pixel) and G-Toilet at twice. Their UVs are read from the
# .bbmodel by cube name, so moving a cube's UV in Blockbench doesn't break the drawing. Glowing parts are drawn
# twice: on the texture and on a "_glow" texture that the game draws full-bright on top.

def bb_boxes(model):
    """Box UV (u, v, w, h, d) of every cube in blockbench/<model>.bbmodel, by cube name."""
    data = json.loads((Path(__file__).resolve().parent.parent / "blockbench" / f"{model}.bbmodel").read_text())
    return {e["name"]: (*e["uv_offset"], *(round(e["to"][i] - e["from"][i]) for i in range(3)))
            for e in data["elements"]}


def camo(r, base, spots, noise=3):
    """Mottled dark camouflage: a base with soft blotches."""
    x, y, w, h = r
    rect(x, y, w, h, base, noise)
    for _ in range(w * h // 10):
        cx, cy = x + random.randrange(w), y + random.randrange(h)
        c = random.choice(spots)
        for i in range(-1, 2):
            for k in range(-1, 1):
                if x <= cx + i < x + w and y <= cy + k < y + h and random.random() < 0.7:
                    px[cx + i, cy + k] = shade(c, noise)


def radial(x, y, w, h, inner, outer, color_at):
    """Calls color_at(r) for the pixels of a w x h face between radius `inner` and `outer` (in pixels)."""
    for i in range(w):
        for k in range(h):
            r = math.hypot(i - (w - 1) / 2, k - (h - 1) / 2)
            if inner <= r <= outer:
                px[x + i, y + k] = color_at(r) + (255,)


def black_camera_lens(x, y, w, h):
    radial(x, y, w, h, 0, 9, lambda r: (20, 20, 24))
    radial(x, y, w, h, 0, min(w, h) / 2 - 0.6, lambda r: GLASS if r > 0.9 else (14, 24, 52))
    px[x + w // 2 - 1, y + h // 2 - 1] = (150, 190, 240, 255)


# ---- The Titans at four model units per pixel (author_titan4: titan_*.bbmodel and titan_*_2.bbmodel)

def lighter(c, n):
    return tuple(max(0, min(255, v + n)) for v in c[:3])


def cloth(r, top, bottom, noise=3, weave=6):
    """Woven cloth: a top-to-bottom gradient with a faint diagonal weave."""
    x, y, w, h = r
    gradient(r, top, bottom, noise)
    for i in range(w):
        for k in range(h):
            if (i + 2 * k) % weave == 0:
                p = px[x + i, y + k]
                px[x + i, y + k] = (max(0, p[0] - 4), max(0, p[1] - 4), max(0, p[2] - 4), 255)


def folds(r, color, count, length=0.5):
    """A few soft vertical creases hanging down a face."""
    x, y, w, h = r
    for _ in range(count):
        i = random.randrange(1, max(2, w - 1))
        k0 = random.randrange(0, max(1, h // 2))
        for k in range(k0, min(h, k0 + int(h * length))):
            px[x + i, y + k] = shade(color, 2)


def stitch(x, y, n, color, dx=1, dy=0):
    """A dashed seam of n pixels."""
    for j in range(0, n, 2):
        px[x + j * dx, y + j * dy] = shade(color, 2)


def frame(r, color, inset=0):
    """A one-pixel outline round a face (or inset into it)."""
    x, y, w, h = r
    rect(x + inset, y + inset, w - 2 * inset, 1, color, 2)
    rect(x + inset, y + h - 1 - inset, w - 2 * inset, 1, color, 2)
    rect(x + inset, y + inset, 1, h - 2 * inset, color, 2)
    rect(x + w - 1 - inset, y + inset, 1, h - 2 * inset, color, 2)


def bolt(x, y, color=(150, 154, 162)):
    px[x, y] = color + (255,)
    px[x + 1, y + 1] = lighter(color, -60) + (255,)


def round_face(x, y, w, h, color_at):
    """Calls color_at(r, i, k) for every pixel of a w x h face, r measured from the centre in pixels; None skips."""
    for i in range(w):
        for k in range(h):
            c = color_at(math.hypot(i - (w - 1) / 2, k - (h - 1) / 2), i, k)
            if c:
                px[x + i, y + k] = shade(c, 2)


def disc_front(b, name, color_at):
    """Draws a round thing across the front faces of a disc (author_titan4.disc: `name`_wide, _mid and _tall) as
    one, color_at(r) giving each pixel's colour by its distance from the middle; the edges get the outermost one."""
    parts = [b[f"{name}_{p}"] for p in ("wide", "mid", "tall")]
    n = parts[0][2]                                          # the wide box's width is the disc's diameter
    edge = color_at(n / 2)
    for u, v, w, h, d in parts:
        f = faces(u, v, w, h, d)
        if edge:
            for key in ("right", "left", "top", "bottom", "back"):
                rect(*f[key], edge, 2)
        x, y = f["front"][:2]
        for i in range(w):
            for k in range(h):
                c = color_at(math.hypot(i - (w - 1) / 2, k - (h - 1) / 2))
                if c:
                    px[x + i, y + k] = shade(c, 2)


def titan4_gloves(b, glove):
    """Big gloves: a palm with a seam and a strap, a thumb, and four fingers with knuckles and joints."""
    dark, light = lighter(glove, -18), lighter(glove, 14)
    f = faces(*b["palm_right"])
    for r in sides(f):
        cloth(r, light, glove, 3, 5)
    rect(*f["top"], dark, 2)
    rect(*f["bottom"], glove, 2)
    x, y, w, h = f["right"]                                   # the back of the hand
    rect(x, y + 3, w, 2, dark, 2)                            # wrist strap
    bolt(x + w // 2, y + 3, (120, 122, 110))
    for i in range(2, w - 1, 4):
        rect(x + i, y + h - 3, 2, 2, light, 2)               # knuckles
    x, y, w, h = f["left"]                                    # the palm
    rect(x + 2, y + 2, w - 4, h - 4, lighter(glove, -8), 2)
    stitch(x + 2, y + 2, w - 4, dark)
    f = faces(*b["thumb_right"])
    for r in f.values():
        cloth(r, light, glove, 3, 5)
    x, y, w, h = f["front"]
    rect(x, y + h // 2, w, 1, dark, 1)                       # joint
    for i in range(4):
        f = faces(*b[f"finger{i}_right"])
        for r in f.values():
            cloth(r, light, glove, 3, 5)
        for key in ("right", "left", "front", "back"):
            x, y, w, h = f[key]
            rect(x, y + h // 3, w, 1, dark, 1)               # finger joints
            rect(x, y + 2 * h // 3, w, 1, dark, 1)
        rect(*f["bottom"], lighter(glove, -10), 2)


def titan4_legs(b, trouser, shoe, boots=False, boot=(24, 24, 28)):
    """Trousers with a crease and a seam, shoes with laces, a shiny toe cap, sole and heel; or tall boots."""
    dark = lighter(trouser, -12)
    for name in ("thigh_right", "shin_right"):
        f = faces(*b[name])
        for r in sides(f):
            cloth(r, trouser, lighter(trouser, -6), 3, 7)
        x, y, w, h = f["front"]
        rect(x + w // 2, y, 1, h, dark, 1)                   # crease
        for key in ("right", "left"):
            x, y, w, h = f[key]
            stitch(x + w // 2, y, h, dark, 0, 1)             # side seam
        rect(*f["top"], trouser, 2)
    x, y, w, h = faces(*b["shin_right"])["front"]
    for k in (2, 5):
        rect(x + 2, y + k, w - 4, 1, dark, 1)                # where the trousers bunch at the knee
    if boots:
        f = faces(*b["boot_right"])
        for r in sides(f):
            gradient(r, lighter(boot, 10), boot, 3)
            x, y, w, h = r
            rect(x, y + h - 3, w, 1, lighter(boot, -10), 1)
            for k in range(4, h - 5, 4):                     # buckled straps up the shaft
                rect(x, y + k, w, 1, lighter(boot, -12), 1)
        x, y, w, h = f["front"]
        for k in range(3, h - 4, 3):
            rect(x + w // 2 - 2, y + k, 4, 1, (120, 120, 128), 1)   # laces
        f = faces(*b["boot_top_right"])
        for r in f.values():
            rect(*r, lighter(boot, 14), 2)
    f = faces(*b["shoe_right"])
    for r in sides(f):
        gradient(r, lighter(shoe, 14), shoe, 2)
    x, y, w, h = f["top"]
    rect(x, y, w, h, lighter(shoe, 8), 2)
    for k in range(h - 12, h - 2, 2):
        rect(x + w // 2 - 3, y + k, 6, 1, (70, 70, 76), 1)   # laces
    f = faces(*b["toe_right"])
    for r in f.values():
        rect(*r, lighter(shoe, 10), 2)
    x, y, w, h = f["top"]
    rect(x + 2, y, w - 4, 1, lighter(shoe, 40), 1)           # polish on the toe cap
    for name in ("sole_right", "heel_right"):
        f = faces(*b[name])
        for r in f.values():
            rect(*r, (12, 12, 14), 1)
        for r in sides(f):
            rect(r[0], r[1], r[2], 1, (40, 40, 44), 1)       # the welt


def titan4_coat(b, coat, shirt, sleeve=None, cuff=None):
    """An open trench coat over a shirt: the shirt with its buttons shows between the coat panels; panels with
    buttonholes and stitched edges, lapels, a standing collar, a long skirt with a back vent and pocket flaps,
    shoulders with seams, sleeves (a colour, or a function drawing a face) and turned-back cuffs."""
    dark, light = lighter(coat, -10), lighter(coat, 10)
    for name in ("chest", "chest_low", "waist"):
        f = faces(*b[name])
        for r in sides(f):
            cloth(r, light, coat, 3)
        rect(*f["top"], light, 3)
        x, y, w, h = f["front"]
        cloth(f["front"], lighter(shirt, 6), shirt, 2, 5)    # the shirt between the panels
        for k in range(3, h, 6):
            rect(x + w // 2, y + k, 1, 1, lighter(shirt, 40), 0)   # buttons
        folds(f["front"], lighter(shirt, -6), 3)
        x, y, w, h = f["back"]
        rect(x + w // 2, y, 1, h, dark, 1)                   # centre back seam
    f = faces(*b["belt"])
    for r in f.values():
        rect(*r, (34, 26, 22), 2)
    for r in sides(f):
        x, y, w, h = r
        stitch(x, y + 1, w, (58, 46, 38))
        stitch(x + 1, y + h - 2, w - 1, (58, 46, 38))
    f = faces(*b["buckle"])
    box(*b["buckle"], (176, 180, 188), 4)
    x, y, w, h = f["front"]
    rect(x + 1, y + 1, w - 2, h - 2, (34, 26, 22), 1)        # the strap through the frame
    rect(x + w // 2, y + 1, 1, h - 2, (200, 204, 212), 1)    # the prong
    f = faces(*b["coat_panel_right"])
    for r in f.values():
        cloth(r, light, coat, 3)
    x, y, w, h = f["front"]
    stitch(x + 1, y + 1, h - 2, dark, 0, 1)
    stitch(x + w - 2, y + 1, h - 2, dark, 0, 1)
    for k in range(20, h - 4, 7):
        rect(x + w - 4, y + k, 2, 1, lighter(coat, -18), 0)  # buttonholes
    folds(f["front"], dark, 2)
    f = faces(*b["lapel_right"])
    for r in f.values():
        rect(*r, lighter(coat, 16), 2)
    x, y, w, h = f["front"]
    for k in range(h):
        px[x + min(w - 1, k // 6), y + k] = shade(lighter(coat, 30), 1)   # the edge catching the light
    for name in ("collar", "collar_side_right"):
        f = faces(*b[name])
        for r in f.values():
            cloth(r, lighter(coat, 14), light, 2)
        for r in sides(f):
            rect(r[0], r[1] + 2, r[2], 1, dark, 1)           # where the collar folds over
    for name in ("coat_back", "coat_side_right", "coat_front_right"):
        f = faces(*b[name])
        for r in sides(f):
            cloth(r, coat, lighter(coat, -6), 3)
            x, y, w, h = r
            rect(x, y + h - 2, w, 2, dark, 1)                # hem
            stitch(x, y + h - 4, w, dark)
            folds(r, dark, max(1, w // 6), 0.8)
        rect(*f["bottom"], dark, 2)
    x, y, w, h = faces(*b["coat_back"])["back"]
    rect(x + w // 2, y + h // 3, 1, h - h // 3, lighter(coat, -20), 0)   # back vent
    x, y, w, h = faces(*b["coat_front_right"])["front"]
    stitch(x + w - 2, y, h - 3, dark, 0, 1)
    f = faces(*b["pocket_right"])
    for r in f.values():
        rect(*r, light, 2)
    x, y, w, h = f["front"]
    rect(x, y + h - 1, w, 1, dark, 1)
    f = faces(*b["shoulder_right"])
    for r in sides(f):
        cloth(r, lighter(coat, 14), light, 3)
        x, y, w, h = r
        rect(x, y + h - 2, w, 1, dark, 1)                    # shoulder seam
    rect(*f["top"], lighter(coat, 16), 3)
    x, y, w, h = f["top"]
    rect(x + 2, y + h // 2, w - 4, 2, lighter(coat, 24), 2)  # epaulette
    bolt(x + w - 4, y + h // 2, (90, 90, 96))
    f = faces(*b["sleeve_right"])
    for r in sides(f):
        if sleeve:
            sleeve(r)
        else:
            cloth(r, light, coat, 3)
        x, y, w, h = r
        rect(x + 2, y + h // 2, w - 4, 1, dark, 1)           # creases at the elbow
        rect(x + 4, y + h // 2 + 3, w - 8, 1, dark, 1)
    rect(*f["top"], light, 2)
    f = faces(*b["cuff_right"])
    for r in sides(f):
        cloth(r, cuff or lighter(coat, 12), cuff or coat, 2)
        x, y, w, h = r
        rect(x, y, w, 1, lighter(coat, 24), 1)
    x, y, w, h = f["right"]
    bolt(x + w // 2 - 3, y + h // 2, (60, 60, 66))
    bolt(x + w // 2 + 2, y + h // 2, (60, 60, 66))
    rect(*f["bottom"], dark, 2)


def titan4_shirt(b, shirt_fn, shirt, cuff_button=(236, 236, 230)):
    """A shirt and no coat: the shirt all round the body (drawn by shirt_fn on each face), a standing collar with
    points, a button placket, sleeves with elbow creases and buttoned cuffs."""
    dark = lighter(shirt, -40)
    for name in ("chest", "chest_low", "waist", "shoulder_right", "sleeve_right"):
        f = faces(*b[name])
        for r in sides(f):
            shirt_fn(r)
        rect(*f["top"], lighter(shirt, 10), 3)
    for name in ("chest", "waist"):
        f = faces(*b[name])
        folds(f["front"], dark, 4)
        x, y, w, h = f["back"]
        rect(x + 2, y + 4, w - 4, 1, dark, 1)               # yoke
        folds(f["back"], dark, 3)
    f = faces(*b["shoulder_right"])
    for r in sides(f):
        rect(r[0], r[1] + r[3] - 2, r[2], 1, dark, 1)       # shoulder seam
    f = faces(*b["sleeve_right"])
    for r in sides(f):
        x, y, w, h = r
        rect(x + 2, y + h // 2, w - 4, 1, dark, 1)
        rect(x + 4, y + h // 2 + 3, w - 8, 1, dark, 1)
    for name in ("shirt_collar", "collar_point_right"):
        f = faces(*b[name])
        for r in f.values():
            shirt_fn(r)
        for r in sides(f):
            rect(r[0], r[1] + r[3] - 1, r[2], 1, dark, 1)
    f = faces(*b["placket"])
    for r in f.values():
        shirt_fn(r)
    x, y, w, h = f["front"]
    for k in range(3, h - 2, 6):
        rect(x + 1, y + k, 2, 2, cuff_button, 1)             # buttons
    rect(x, y, 1, h, dark, 1)
    f = faces(*b["belt"])
    for r in f.values():
        rect(*r, (20, 20, 22), 2)
    for r in sides(f):
        stitch(r[0], r[1] + 1, r[2], (46, 46, 50))
        stitch(r[0] + 1, r[1] + r[3] - 2, r[2] - 1, (46, 46, 50))
    f = faces(*b["buckle"])
    box(*b["buckle"], (176, 180, 188), 4)
    x, y, w, h = f["front"]
    rect(x + 1, y + 1, w - 2, h - 2, (20, 20, 22), 1)
    rect(x + w // 2, y + 1, 1, h - 2, (200, 204, 212), 1)
    f = faces(*b["cuff_right"])
    for r in sides(f):
        shirt_fn(r)
        rect(r[0], r[1], r[2], 1, dark, 1)
    x, y, w, h = f["right"]
    rect(x + w // 2 - 1, y + h // 2, 2, 2, cuff_button, 1)   # cuff buttons
    rect(x + w // 2 + 3, y + h // 2, 2, 2, cuff_button, 1)


# ---- Titan Cameraman 1.0 at four units per pixel

TC_COAT = (40, 40, 44)
TC_SHIRT = (28, 28, 32)
TC_GLOVE = (76, 78, 64)
TC_BLUE, TC_BLUE_HI = (60, 190, 255), (200, 248, 255)
CCTV_GREY, CCTV_GREY_DARK = (200, 204, 211), (156, 161, 169)


def titan_cameraman_lights(b):
    """What glows on the Titan Cameraman: his core, the LCD flip screens, the turbine exhausts, recording lights."""
    def core(r):
        if r < 1.6:
            return TC_BLUE_HI
        if r < 4:
            return (130, 225, 255)
        return TC_BLUE
    disc_front(b, "core_glass", core)
    x, y, w, h = faces(*b["core_center"])["front"]
    rect(x, y, w, h, TC_BLUE_HI, 3)
    for key in ("right", "left", "top", "bottom"):
        rect(*faces(*b["core_center"])[key], TC_BLUE, 2)
    x, y, w, h = faces(*b["flip_screen_face_right"])["front"]
    for k in range(h):
        for i in range(w):
            v = 150 - 6 * k
            px[x + i, y + k] = ((v - 30) if (i % 3 == 0 or k % 3 == 0) else v - 10, v + 10, v + 30, 255)
    px[x + 1, y + 1] = (220, 236, 250, 255)
    x, y, w, h = faces(*b["turbine_nozzle_right"])["bottom"]
    round_face(x, y, w, h, lambda r, i, k: (150, 230, 255) if r < 1.6 else (70, 170, 250) if r < 3.4 else None)
    x, y, w, h = faces(*b["front_frame"])["front"]
    rect(x + w - 3, y + 1, 2, 2, (255, 40, 30), 0)          # red recording light
    x, y, w, h = faces(*b["shoulder_cam_lens_right"])["front"]
    rect(x + w - 2, y, 2, 2, (255, 40, 30), 0)
    x, y, w, h = faces(*b["side_cam_right"])["front"]
    px[x + w - 2, y + 1] = (255, 60, 40, 255)


def titan_cameraman():
    b = bb_boxes("titan_cameraman")
    new_image(512, 30)

    # Head: a light grey CCTV housing with ribs down its sides, a dark front frame round a big silver-ringed lens,
    # a sun hood with a raised ridge, 35mm cameras on arms at the sides, two LCD flip screens on hinges and two
    # antennas on top, a vented back cap and a cable down to the neck
    box(*b["neck"], (46, 48, 54), 3)
    x, y, w, h = faces(*b["neck"])["front"]
    for k in range(1, h, 2):
        rect(x, y + k, w, 1, (36, 38, 42), 1)               # a ribbed hose for a neck
    f = faces(*b["neck_plate"])
    box(*b["neck_plate"], (70, 72, 78), 3)
    x, y, w, h = f["top"]
    for i, k in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
        bolt(x + i - 1, y + k - 1)
    f = faces(*b["housing"])
    for r in sides(f):
        gradient(r, CCTV_GREY, CCTV_GREY_DARK, 2)
    rect(*f["top"], lighter(CCTV_GREY, 8), 2)
    rect(*f["bottom"], (120, 124, 132), 2)
    for key in ("right", "left"):
        x, y, w, h = f[key]
        rect(x + 2, y + 3, w - 4, 1, (172, 176, 184), 1)     # seams
        rect(x + 2, y + h - 4, w - 4, 1, (172, 176, 184), 1)
        for i in range(4, 12, 2):
            rect(x + i, y + 6, 1, 6, (120, 124, 132), 0)     # vents toward the back
        rect(x + w - 10, y + 6, 6, 4, (226, 196, 60), 2)     # a yellow warning label
        rect(x + w - 8, y + 7, 2, 2, (30, 30, 30), 0)
    f = faces(*b["housing_rib_right"])
    for r in f.values():
        rect(*r, (180, 184, 192), 2)
    f = faces(*b["front_frame"])
    for r in f.values():
        rect(*r, (44, 46, 52), 2)
    frame(f["front"], (70, 72, 80))
    x, y, w, h = f["front"]
    for i, k in ((1, 1), (w - 3, 1), (1, h - 3), (w - 3, h - 3)):
        bolt(x + i, y + k, (120, 124, 132))
    disc_front(b, "lens", lambda r: (34, 34, 40) if r < 5.4 else (190, 196, 206) if r < 6.6 else (130, 136, 146))
    disc_front(b, "lens_glass", lambda r: (60, 70, 90) if r > 4.2 else GLASS if r > 2.2 else (14, 24, 52))
    x, y, w, h = faces(*b["lens_glass_mid"])["front"]
    rect(x + 2, y + 1, 2, 1, (170, 200, 240), 0)            # glint
    f = faces(*b["lens_core"])
    box(*b["lens_core"], (24, 26, 32), 2)
    x, y, w, h = f["front"]
    rect(x + 1, y + 1, 2, 2, (40, 70, 130), 1)
    f = faces(*b["hood"])
    rect(*f["top"], lighter(CCTV_GREY, 14), 2)
    rect(*f["bottom"], (70, 72, 78), 2)                       # shadow under the hood
    for r in sides(f):
        gradient(r, lighter(CCTV_GREY, 4), CCTV_GREY_DARK, 2)
    x, y, w, h = f["front"]
    rect(x, y + h - 1, w, 1, (140, 144, 152), 1)
    box(*b["hood_side_right"], CCTV_GREY_DARK, 3)
    box(*b["hood_ridge"], lighter(CCTV_GREY, 20), 2)
    box(*b["side_cam_arm_right"], (60, 62, 68), 3)
    f = faces(*b["side_cam_right"])
    for r in sides(f):
        gradient(r, (64, 66, 72), (44, 46, 50), 2)
    x, y, w, h = f["right"]
    rect(x + 3, y + 3, w - 6, h - 6, (52, 54, 58), 2)       # the 35mm camera's leatherette panel
    for k in range(4, h - 3, 2):
        rect(x + 3, y + k, w - 6, 1, (46, 48, 52), 0)
    f = faces(*b["side_cam_top_right"])
    for r in f.values():
        rect(*r, (176, 180, 188), 3)                         # silver top plate
    box(*b["side_cam_dial_right"], (200, 204, 212), 3)
    f = faces(*b["side_cam_lens_right"])
    box(*b["side_cam_lens_right"], (26, 26, 30), 2)
    for r in sides(f):
        rect(r[0], r[1] + 2, r[2], 1, (80, 82, 88), 0)       # focus ring
    black_camera_lens(*f["front"])
    box(*b["screen_hinge_right"], (60, 62, 68), 3)
    f = faces(*b["flip_screen_right"])
    for r in f.values():
        rect(*r, (34, 34, 38), 2)
    frame(f["front"], (60, 60, 66))
    f = faces(*b["flip_screen_face_right"])
    box(*b["flip_screen_face_right"], (40, 40, 44), 2)
    titan_lcd = (92, 118, 138)
    x, y, w, h = f["front"]
    for k in range(h):
        for i in range(w):
            c = lighter(titan_lcd, -4 * k)
            px[x + i, y + k] = shade(lighter(c, -26) if (i % 3 == 0 or k % 3 == 0) else c, 2)
    box(*b["antenna_base"], (60, 62, 68), 3)
    for name in ("antenna", "antenna_side"):
        f = faces(*b[name])
        box(*b[name], (36, 36, 40), 3)
        for r in sides(f):
            for k in range(3, r[3], 5):
                rect(r[0], r[1] + k, r[2], 1, (60, 62, 68), 0)   # segments
    box(*b["antenna_tip"], (200, 204, 212), 3)
    f = faces(*b["back_cap"])
    for r in f.values():
        rect(*r, (176, 180, 188), 2)
    x, y, w, h = f["back"]
    for k in range(2, h - 2, 2):
        rect(x + 3, y + k, w - 6, 1, (136, 140, 148), 0)    # vents
    for name in ("cable", "cable_drop"):
        f = faces(*b[name])
        box(*b[name], (24, 24, 28), 2)
        for r in sides(f):
            for k in range(1, r[3], 3):
                rect(r[0], r[1] + k, r[2], 1, (40, 40, 44), 0)

    # Body: a black trench coat over a dark shirt, worn mottled sleeves, olive gloves
    titan4_coat(b, TC_COAT, TC_SHIRT,
                sleeve=lambda r: camo(r, (46, 46, 46), [(36, 38, 36), (64, 62, 56), (40, 42, 46)]))
    titan4_gloves(b, TC_GLOVE)
    titan4_legs(b, (34, 34, 38), (20, 20, 22))

    # The core on his left chest: a ribbed steel ring round blue glass, lit by the glow texture
    disc_front(b, "core_ring", lambda r: (150, 156, 168) if r > 7.4 else (96, 102, 112) if r > 6.4 else (60, 64, 72))
    x, y, w, h = faces(*b["core_ring_mid"])["front"]
    for i, k in ((0, 0), (w - 2, 0), (0, h - 2), (w - 2, h - 2)):
        bolt(x + i, y + k)
    disc_front(b, "core_glass", lambda r: (60, 160, 230) if r < 5 else (40, 110, 180))
    box(*b["core_center"], (150, 230, 255), 2)

    # The riot shield on his back, the jetpack over it, its turbines
    f = faces(*b["shield"])
    for r in f.values():
        rect(*r, (60, 62, 68), 3)
    x, y, w, h = f["back"]
    rect(x, y, w, h, (70, 72, 78), 3)
    frame((x, y, w, h), (96, 98, 106))
    frame((x, y, w, h), (50, 52, 58), 2)
    rect(x + 6, y + 6, w - 12, 10, (34, 40, 50), 2)          # viewing window
    rect(x + 7, y + 7, 8, 1, (90, 104, 124), 0)
    for i in range(3, w - 2, 6):
        for k in (3, h - 4):
            bolt(x + i, y + k, (120, 124, 132))
    x0 = x + w // 2
    for k in range(h // 2, h - 6):                           # the round emblem below the jetpack
        for i in range(-10, 11):
            r = math.hypot(i, k - (h - 18))
            if 7 < r < 9 or (r < 9 and abs(i) < 1) or (r < 9 and abs(k - (h - 18)) < 1):
                px[x0 + i, y + k] = shade((40, 42, 48), 1)
    box(*b["shield_rim"], (90, 92, 100), 3)
    box(*b["shield_handle"], (40, 40, 44), 2)
    f = faces(*b["jetpack"])
    for r in sides(f):
        gradient(r, (64, 66, 72), (44, 46, 50), 2)
    x, y, w, h = f["back"]
    frame((x + 2, y + 2, w - 4, h - 4), (36, 38, 42))
    for k in range(6, h - 6, 3):
        rect(x + 5, y + k, w - 10, 1, (36, 38, 42), 0)       # cooling fins
    rect(x + w // 2 - 3, y + 3, 6, 2, (200, 60, 50), 1)      # red warning strip
    rect(*f["top"], (70, 72, 78), 2)
    box(*b["jetpack_top"], (80, 82, 88), 3)
    f = faces(*b["turbine_right"])
    for r in sides(f):
        gradient(r, (150, 154, 162), (104, 108, 116), 3)
        x, y, w, h = r
        for k in (3, 4, h - 6, h - 5):
            rect(x, y + k, w, 1, (80, 84, 92), 0)           # bands
        rect(x, y + h // 2, w, 2, (190, 50, 44), 1)          # a red stripe round the middle
    rect(*f["top"], (170, 174, 182), 3)
    box(*b["turbine_cap_right"], (120, 124, 132), 3)
    f = faces(*b["turbine_nozzle_right"])
    box(*b["turbine_nozzle_right"], (40, 40, 44), 3)
    for r in sides(f):
        for i in range(0, r[2], 2):
            rect(r[0] + i, r[1], 1, r[3], (30, 30, 34), 0)   # nozzle petals
    x, y, w, h = f["bottom"]
    round_face(x, y, w, h, lambda r, i, k: (40, 140, 220) if r < 3.4 else None)

    # Black CCTV cameras behind his shoulders
    f = faces(*b["shoulder_cam_right"])
    for r in sides(f):
        gradient(r, (44, 44, 48), (26, 26, 30), 2)
        x, y, w, h = r
        rect(x + 2, y + 6, w - 4, 1, (56, 56, 62), 1)         # seams
        rect(x + 2, y + h - 6, w - 4, 1, (56, 56, 62), 1)
    rect(*f["top"], (52, 52, 56), 2)
    x, y, w, h = f["back"]
    for k in range(10, 22, 2):
        rect(x + 3, y + k, w - 6, 1, (20, 20, 24), 0)        # vents
    f = faces(*b["shoulder_cam_hood_right"])
    for r in f.values():
        rect(*r, (56, 56, 60), 2)
    rect(*f["top"], (66, 66, 70), 2)
    f = faces(*b["shoulder_cam_lens_right"])
    box(*b["shoulder_cam_lens_right"], (20, 20, 24), 2)
    black_camera_lens(*f["front"])
    box(*b["shoulder_cam_mount_right"], (60, 62, 68), 3)

    titan_cameraman_lights(b)
    return img


def titan_cameraman_glow():
    new_image(512, 30)
    titan_cameraman_lights(bb_boxes("titan_cameraman"))
    return img


# ---- Titan Speakerman 1.0

TSM_RED = (196, 30, 36)
SPEAKER_BLACK = (24, 24, 26)


def glossy_red(r):
    """Shiny red dress-shirt cloth: smooth, with one soft band of light where the satin catches it."""
    x, y, w, h = r
    gradient(r, TSM_RED, (172, 24, 30), 2)
    band = max(1, w // 5)
    rect(x + w // 4, y, band, h, (214, 48, 52), 2)
    if w > 6:
        px_row = x + w // 4 - 1
        rect(px_row, y, 1, h, (206, 40, 44), 2)


def red_driver(r, n):
    """A driver n across lit red from inside: black surround, a dark rim, the glowing cone, a bright dust cap."""
    if r > n / 2 - 0.3:
        return (30, 18, 20)
    if r > n / 2 - 1.3:
        return (110, 20, 24)
    if r < 1.6:
        return (255, 150, 140)
    return (236, 40 + int(r * 6), 40 + int(r * 4))


def white_driver(r, n):
    """A white driver with a grey surround and a dark dust cap, like the ones on his shoulder towers."""
    if r > n / 2 - 0.3:
        return (40, 40, 44)
    if r > n / 2 - 1.4:
        return (96, 96, 102)
    if r < 1.6:
        return (34, 34, 38)
    return (238 - int(r * 3), 238 - int(r * 3), 232 - int(r * 3))


def black_driver(r, n):
    """A black driver on a black speaker: only the rings catch the light."""
    if r > n / 2 - 1 or abs(r - n / 4) < 0.5 or r < n * 0.12:
        return (70, 70, 76)
    return (34, 34, 38)


def speaker_box(b, name, black=SPEAKER_BLACK, grille=False):
    """A black speaker cabinet with a bevelled front edge, screws in the corners and (optionally) grille cloth."""
    f = faces(*b[name])
    for r in sides(f):
        gradient(r, lighter(black, 8), black, 2)
    rect(*f["top"], lighter(black, 12), 2)
    rect(*f["bottom"], lighter(black, -6), 2)
    x, y, w, h = f["front"]
    if grille:
        for i in range(1, w - 1):
            for k in range(1, h - 1):
                if (i + k) % 2 == 0:
                    px[x + i, y + k] = shade(lighter(black, -8), 1)
    frame(f["front"], lighter(black, 22))
    for i, k in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
        px[x + i, y + k] = (90, 90, 96, 255)
    for key in ("right", "left"):
        x, y, w, h = f[key]
        rect(x, y, w, 1, lighter(black, 18), 1)             # the edge catching the light


def speaker_head_tex(b):
    """Titan Speakerman's head (both his forms): black speakers with bevelled edges and corner screws, red-lit woofers,
    silver tweeters, a grille-fronted cabinet behind with a port and terminals."""
    box(*b["neck"], (30, 30, 34), 3)
    x, y, w, h = faces(*b["neck"])["front"]
    for k in range(1, h, 2):
        rect(x, y + k, w, 1, (22, 22, 26), 1)
    for name in ("lower_speaker", "center_speaker", "column_right"):
        speaker_box(b, name)
    speaker_box(b, "back_cabinet", grille=True)
    for name in ("lower_trim", "back_cabinet_top", "column_cap_right"):
        f = faces(*b[name])
        for r in f.values():
            rect(*r, (44, 44, 48), 2)
        rect(*f["top"], (54, 54, 58), 2)
    x, y, w, h = faces(*b["back_cabinet"])["back"]
    rect(x + 4, y + 4, w - 8, 6, (16, 16, 18), 1)            # bass port
    rect(x + w // 2 - 4, y + h - 8, 8, 4, (60, 60, 66), 2)   # terminal plate
    px[x + w // 2 - 2, y + h - 6] = (200, 40, 40, 255)
    px[x + w // 2 + 1, y + h - 6] = (14, 14, 14, 255)
    for name, n in (("lower_woofer", 9), ("column_woofer", 9), ("column_woofer_low", 9)):
        for p in ("wide", "mid", "tall"):
            u, v, w, h, d = b[f"{name}_{p}_right"]
            f = faces(u, v, w, h, d)
            for key in ("right", "left", "top", "bottom"):
                rect(*f[key], (40, 20, 22), 1)
            x, y = f["front"][:2]
            for i in range(w):
                for k in range(h):
                    px[x + i, y + k] = shade(red_driver(math.hypot(i - (w - 1) / 2, k - (h - 1) / 2), n), 2)
    disc_front(b, "center_woofer", lambda r: red_driver(r, 13))
    disc_front(b, "center_cone", lambda r: (255, 150, 140) if r < 1.5 else (250, 70, 64))
    for name in ("lower_tweeter", "center_tweeter", "column_tweeter_right"):
        f = faces(*b[name])
        box(*b[name], (40, 40, 44), 2)
        x, y, w, h = f["front"]
        round_face(x, y, w, h, lambda r, i, k: (200, 202, 210) if r < 1.2 else (150, 152, 160))


def speaker_head_lights(b):
    """The red-lit drivers of Titan Speakerman's head of speakers (both his forms)."""
    for name, n in (("lower_woofer", 9), ("center_woofer", 13), ("column_woofer", 9), ("column_woofer_low", 9)):
        parts = [b[f"{name}_{p}" + ("" if name.startswith("center") else "_right")] for p in ("wide", "mid", "tall")]
        for u, v, w, h, d in parts:
            x, y = faces(u, v, w, h, d)["front"][:2]
            for i in range(w):
                for k in range(h):
                    rr = math.hypot(i - (w - 1) / 2, k - (h - 1) / 2)
                    if rr < n / 2 - 1.3:
                        px[x + i, y + k] = shade(red_driver(rr, n), 2)
    disc_front(b, "center_cone", lambda r: (255, 150, 140) if r < 1.5 else (250, 70, 64))


def titan_speakerman_lights(b):
    """What glows on the Titan Speakerman: his red drivers, the core, the cannon's coil and muzzle."""
    speaker_head_lights(b)
    x, y, w, h = faces(*b["core_light"])["front"]
    round_face(x, y, w, h, lambda r, i, k: (255, 200, 190) if r < 1.2 else (255, 70, 60))
    for key in ("right", "left", "top", "bottom"):
        rect(*faces(*b["core_light"])[key], (230, 50, 44), 2)
    disc_front(b, "core_dome", lambda r: (255, 90, 80) if r < 3.4 else None)
    disc_front(b, "core_glass", lambda r: (200, 40, 40) if r < 4.4 else None)
    x, y, w, h = faces(*b["cannon_coil"])["front"]
    for k in range(h):
        rect(x, y + k, w, 1, (255, 60, 50) if k % 3 else (255, 160, 140), 1)
    x, y, w, h = faces(*b["cannon_muzzle"])["bottom"]
    round_face(x, y, w, h, lambda r, i, k: (255, 120, 100) if r < 2 else (230, 40, 36) if r < 4 else None)
    x, y, w, h = faces(*b["cannon_cell"])["right"]
    rect(x + 1, y + 2, w - 2, 2, (255, 60, 50), 1)
    for name in ("jetpack_nozzle_right",):
        x, y, w, h = faces(*b[name])["bottom"]
        round_face(x, y, w, h, lambda r, i, k: (255, 120, 70) if r < 2.6 else None)


def titan_speakerman():
    b = bb_boxes("titan_speakerman")
    new_image(512, 31)

    speaker_head_tex(b)

    # Body: a glossy red shirt, black gloves, trousers and shoes
    titan4_shirt(b, glossy_red, TSM_RED)
    titan4_gloves(b, (32, 32, 36))
    titan4_legs(b, (30, 30, 34), (18, 18, 20))

    # The glass-domed core between two black chest speakers
    disc_front(b, "core_ring", lambda r: (170, 174, 182) if r > 8.6 else (120, 124, 132) if r > 7.4 else (90, 94, 102))
    x, y, w, h = faces(*b["core_ring_mid"])["front"]
    for i, k in ((0, 0), (w - 2, 0), (0, h - 2), (w - 2, h - 2)):
        bolt(x + i, y + k)
    disc_front(b, "core_glass", lambda r: (200, 206, 214) if r > 6.4 else (150, 60, 62) if r > 4.4 else (190, 40, 40))
    disc_front(b, "core_dome", lambda r: (230, 120, 116) if r < 2 else (210, 60, 58))
    x, y, w, h = faces(*b["core_dome_mid"])["front"]
    rect(x + 1, y + 1, 2, 1, (255, 230, 226), 0)            # the glass catching the light
    box(*b["core_light"], (255, 90, 80), 2)
    speaker_box(b, "chest_speaker_right")
    for name in ("chest_driver", "chest_driver_low"):
        for p in ("wide", "mid", "tall"):
            u, v, w, h, d = b[f"{name}_{p}_right"]
            f = faces(u, v, w, h, d)
            for key in ("right", "left", "top", "bottom"):
                rect(*f[key], (50, 50, 56), 1)
            x, y = f["front"][:2]
            for i in range(w):
                for k in range(h):
                    px[x + i, y + k] = shade(black_driver(math.hypot(i - (w - 1) / 2, k - (h - 1) / 2), 7), 1)

    # The black jetpack and its turbines
    f = faces(*b["jetpack"])
    for r in sides(f):
        gradient(r, (40, 40, 44), (26, 26, 30), 2)
    rect(*f["top"], (46, 46, 50), 2)
    f = faces(*b["jetpack_panel"])
    for r in f.values():
        rect(*r, (34, 34, 38), 2)
    x, y, w, h = f["back"]
    frame((x, y, w, h), (56, 56, 62))
    for k in range(4, h - 3, 3):
        rect(x + 3, y + k, w - 6, 1, (22, 22, 26), 0)        # vents
    rect(x + w // 2 - 3, y + 1, 6, 2, (200, 40, 40), 1)
    f = faces(*b["jetpack_turbine_right"])
    for r in sides(f):
        gradient(r, (70, 72, 78), (46, 48, 54), 3)
        x, y, w, h = r
        for k in (3, h - 4):
            rect(x, y + k, w, 1, (36, 38, 42), 0)
        rect(x, y + h // 2, w, 1, (180, 36, 36), 1)
    rect(*f["top"], (86, 88, 94), 3)
    f = faces(*b["jetpack_nozzle_right"])
    box(*b["jetpack_nozzle_right"], (30, 30, 34), 3)
    x, y, w, h = f["bottom"]
    round_face(x, y, w, h, lambda r, i, k: (120, 50, 30) if r < 3 else None)

    # The speaker towers leaning out from his shoulders, with white drivers and little tweeters
    speaker_box(b, "tower_right")
    x, y, w, h = faces(*b["tower_right"])["front"]
    for k in (8, 23, 38, 53):
        rect(x + w // 2 - 1, y + k, 2, 2, (220, 220, 216), 1)   # tweeters between the drivers
    f = faces(*b["tower_cap_right"])
    for r in f.values():
        rect(*r, (46, 46, 50), 2)
    for name in ("tower_driver", "tower_driver_mid", "tower_driver_low"):
        for p in ("wide", "mid", "tall"):
            u, v, w, h, d = b[f"{name}_{p}_right"]
            f = faces(u, v, w, h, d)
            for key in ("right", "left", "top", "bottom"):
                rect(*f[key], (60, 60, 66), 1)
            x, y = f["front"][:2]
            for i in range(w):
                for k in range(h):
                    px[x + i, y + k] = shade(white_driver(math.hypot(i - (w - 1) / 2, k - (h - 1) / 2), 13), 2)

    # The plasma cannon on his left arm
    f = faces(*b["cannon"])
    for r in sides(f):
        gradient(r, (84, 86, 94), (58, 60, 66), 3)
        x, y, w, h = r
        for k in range(4, h - 2, 8):
            rect(x, y + k, w, 1, (44, 46, 52), 0)            # panel lines
    rect(*f["top"], (96, 98, 106), 3)
    for name in ("cannon_ring0", "cannon_ring1", "cannon_ring2"):
        f = faces(*b[name])
        for r in f.values():
            rect(*r, (46, 48, 54), 2)
        for r in sides(f):
            rect(r[0], r[1], r[2], 1, (100, 102, 110), 1)
    f = faces(*b["cannon_muzzle"])
    box(*b["cannon_muzzle"], (36, 36, 40), 3)
    x, y, w, h = f["bottom"]
    round_face(x, y, w, h, lambda r, i, k: (150, 30, 28) if r < 4 else (24, 24, 26))
    box(*b["cannon_coil"], (40, 40, 44), 2)
    x, y, w, h = faces(*b["cannon_coil"])["front"]
    for k in range(h):
        rect(x, y + k, w, 1, (200, 40, 36) if k % 3 else (230, 120, 110), 1)
    f = faces(*b["cannon_cell"])
    box(*b["cannon_cell"], (60, 62, 68), 3)
    x, y, w, h = f["right"]
    rect(x + 1, y + 2, w - 2, 2, (200, 40, 36), 1)

    titan_speakerman_lights(b)
    return img


def titan_speakerman_glow():
    new_image(512, 31)
    titan_speakerman_lights(bb_boxes("titan_speakerman"))
    return img


# ---- Titan TV Man 1.0

TTV_PURPLE, TTV_PURPLE_HI = (178, 84, 255), (236, 196, 255)


def titan_tv_man_lights(b):
    """What glows on the Titan TV Man: the static on his screen, his purple core, the edges of his claws."""
    x, y, w, h = faces(*b["tv"])["front"]
    rnd = random.Random(44)
    for i in range(4, w - 4):                                # the screen, inside the bezel cubes
        for k in range(3, h - 5):
            centre = 1 - 0.25 * (abs(i - w / 2) / w + abs(k - h / 2) / h)
            v = rnd.randint(150, 252) * centre * (0.9 if k % 2 else 1.0)
            px[x + i, y + k] = (int(v), int(v), min(255, int(v * 1.04)), 255)
    disc_front(b, "core_glass", lambda r: TTV_PURPLE_HI if r < 2.4 else (210, 140, 255) if r < 4.4 else TTV_PURPLE)
    x, y, w, h = faces(*b["core_light"])["front"]
    rect(x, y, w, h, (250, 230, 255), 2)
    for key in ("right", "left", "top", "bottom"):
        rect(*faces(*b["core_light"])[key], TTV_PURPLE_HI, 2)
    for i in range(3):
        f = faces(*b[f"claw{i}_edge_right"])
        for r in f.values():
            rect(*r, TTV_PURPLE, 3)
        rect(*f["front"], (210, 150, 255), 3)
        f = faces(*b[f"claw{i}_tip_right"])
        x, y, w, h = f["front"]
        rect(x + w - 1, y, 1, h, TTV_PURPLE, 2)
    for name in ("core_clamp_top", "core_clamp_bottom", "core_clamp_side_right"):
        x, y, w, h = faces(*b[name])["front"]
        rect(x + w // 2, y + 1, 1, h - 2, TTV_PURPLE, 1)     # purple lines on the clamps


def titan_tv_man():
    b = bb_boxes("titan_tv_man")
    new_image(512, 32)
    black = (30, 30, 34)

    # Head: a giant black TV with a thick bezel, the screen set back inside it, side panels and a stepped back
    box(*b["neck"], (36, 36, 40), 3)
    x, y, w, h = faces(*b["neck"])["front"]
    for k in range(1, h, 2):
        rect(x, y + k, w, 1, (26, 26, 30), 1)
    box(*b["neck_bracket"], (50, 50, 56), 3)
    f = faces(*b["tv"])
    for r in sides(f):
        gradient(r, (40, 40, 44), (28, 28, 32), 2)
    rect(*f["top"], (42, 42, 46), 2)
    rect(*f["bottom"], (24, 24, 28), 2)
    x, y, w, h = f["front"]
    rect(x + 3, y + 2, w - 6, h - 4, (14, 14, 16), 0)        # the screen's dark surround (lit by the glow)
    for key in ("right", "left"):
        x, y, w, h = f[key]
        for k in range(4, h - 4, 3):
            rect(x + 2, y + k, w - 4, 1, (22, 22, 26), 0)    # ventilation slots
    f = faces(*b["tv_back"])
    for r in f.values():
        gradient(r, (32, 32, 36), (24, 24, 28), 2)
    x, y, w, h = f["back"]
    frame(f["back"], (42, 42, 48), 1)
    for k in range(4, h - 3, 2):
        rect(x + 4, y + k, w - 8, 1, (18, 18, 20), 0)
    f = faces(*b["tv_back_box"])
    box(*b["tv_back_box"], (36, 36, 40), 2)
    x, y, w, h = f["back"]
    rect(x + 3, y + 3, 8, 4, (176, 176, 168), 1)             # rating label
    for i in range(13, w - 2, 3):
        rect(x + i, y + h - 5, 2, 2, (14, 14, 16), 0)        # ports
    for name in ("bezel_top", "bezel_bottom", "bezel_side_right"):
        f = faces(*b[name])
        for r in f.values():
            rect(*r, (44, 44, 50), 2)
        rect(*f["front"], (50, 50, 56), 2)
        rect(*f["top"], (58, 58, 64), 2)
    x, y, w, h = faces(*b["bezel_bottom"])["front"]
    rect(x + w // 2 - 4, y + 1, 8, 2, (150, 152, 160), 2)    # brand plate
    rect(x + w - 6, y + 2, 2, 1, (60, 220, 90), 0)           # power light
    for i in range(4, 14, 3):
        rect(x + i, y + 2, 2, 2, (30, 30, 34), 0)            # buttons
    x, y, w, h = faces(*b["bezel_top"])["bottom"]
    rect(x, y, w, h, (20, 20, 24), 0)                        # shadow over the screen
    f = faces(*b["side_panel_right"])
    for r in f.values():
        rect(*r, (38, 38, 42), 2)
    x, y, w, h = f["right"]
    for k in range(2, h - 2, 2):
        rect(x + 2, y + k, w - 4, 1, (24, 24, 28), 0)        # speaker grille
    box(*b["tv_top_vent"], (24, 24, 28), 1)

    # Body: a black trench coat over a black shirt, black gloves, tall boots
    titan4_coat(b, black, (18, 18, 22))
    titan4_gloves(b, (34, 34, 38))
    titan4_legs(b, (30, 30, 34), (18, 18, 20), boots=True)

    # The purple core in a dark ring, held by four clamps
    disc_front(b, "core_ring", lambda r: (84, 78, 96) if r > 8.4 else (60, 56, 70) if r > 7.4 else (40, 36, 48))
    x, y, w, h = faces(*b["core_ring_mid"])["front"]
    for i, k in ((0, 0), (w - 2, 0), (0, h - 2), (w - 2, h - 2)):
        bolt(x + i, y + k, (110, 104, 122))
    disc_front(b, "core_glass", lambda r: (150, 70, 230))
    box(*b["core_light"], (230, 200, 255), 2)
    for name in ("core_clamp_top", "core_clamp_bottom", "core_clamp_side_right"):
        f = faces(*b[name])
        for r in f.values():
            gradient(r, (70, 66, 80), (46, 42, 54), 2)

    # The claws on his back: black blades with glowing purple edges, on a riveted mount
    f = faces(*b["claw_mount"])
    for r in f.values():
        gradient(r, (34, 32, 40), (24, 22, 28), 2)
    f = faces(*b["claw_mount_plate"])
    for r in f.values():
        rect(*r, (40, 38, 46), 2)
    x, y, w, h = f["back"]
    for i in range(2, w - 1, 6):
        for k in (1, h - 3):
            bolt(x + i, y + k, (100, 96, 110))
    for i in range(3):
        f = faces(*b[f"claw{i}_right"])
        for r in sides(f):
            gradient(r, (46, 42, 52), (24, 22, 28), 3)
            x, y, w, h = r
            rect(x + w // 2, y + 2, 1, h - 4, (60, 56, 68), 1)   # the blade's ridge
        rect(*f["top"], (60, 56, 66), 2)
        f = faces(*b[f"claw{i}_tip_right"])
        for r in f.values():
            gradient(r, (58, 54, 64), (36, 32, 42), 2)
        box(*b[f"claw{i}_edge_right"], (120, 60, 190), 3)

    # Grappling hooks on his elbows
    for name in ("hook_right", "hook_left"):
        f = faces(*b[name])
        for r in sides(f):
            gradient(r, (156, 160, 168), (112, 116, 124), 3)
        x, y, w, h = f["back"]
        rect(x + 1, y + 2, w - 2, 1, (90, 94, 102), 0)
        rect(x + w // 2, y + 4, 1, h - 5, (60, 62, 68), 0)   # the cable slot
    for name in ("hook_claw_right", "hook_claw_left"):
        f = faces(*b[name])
        box(*b[name], (130, 134, 142), 3)
        x, y, w, h = f["bottom"]
        rect(x + 1, y + 1, w - 2, h - 2, (70, 72, 78), 0)    # the hook's points

    titan_tv_man_lights(b)
    return img


def titan_tv_man_glow():
    new_image(512, 32)
    titan_tv_man_lights(bb_boxes("titan_tv_man"))
    return img


# ---- Titan 2.0s (the players' Titan 2.0 form): titan_*_2.bbmodel

def metal_box(r_box, base, noise=4):
    """A metal part: lighter on top, darker underneath, a highlight along the top edge of each side."""
    f = faces(*r_box)
    for r in sides(f):
        gradient(r, tuple(min(255, c + 10) for c in base), tuple(max(0, c - 14) for c in base), noise)
        x, y, w, h = r
        rect(x, y, w, 1, tuple(min(255, c + 26) for c in base), 2)
    rect(*f["top"], tuple(min(255, c + 18) for c in base), noise)
    rect(*f["bottom"], tuple(max(0, c - 24) for c in base), 2)
    return f


def driver_disc(b, name, n, color, edge, side="_right"):
    """Paints a round driver across the front faces of disc `name` (author_titan4.disc), color(r, n) by radius."""
    for p in ("wide", "mid", "tall"):
        u, v, w, h, d = b[f"{name}_{p}{side}"]
        f = faces(u, v, w, h, d)
        for key in ("right", "left", "top", "bottom"):
            rect(*f[key], edge, 1)
        x, y = f["front"][:2]
        for i in range(w):
            for k in range(h):
                px[x + i, y + k] = shade(color(math.hypot(i - (w - 1) / 2, k - (h - 1) / 2), n), 2)


def static_screen(r, rnd, inset=1, bright=(160, 252)):
    """A screen of white static inside a dark rim."""
    x, y, w, h = r
    rect(x, y, w, h, (16, 16, 18), 0)
    crt_static((x + inset, y + inset, w - 2 * inset, h - 2 * inset), rnd, bright)


# ---- Titan Cameraman 2.0

TC2_DARK, TC2_DARKER = (52, 54, 60), (34, 36, 40)
TC2_STEEL, TC2_SILVER = (150, 154, 162), (184, 188, 196)
TC2_CYAN, TC2_CYAN_HI = (40, 200, 255), (220, 252, 255)


def titan_cameraman_2_lights(b):
    """What glows on Titan Cameraman 2.0: the cyan lens and core, the red antenna and fin lights, the railgun's barrel
    and muzzle, the hammer's hexagon and the jetpack's engines."""
    disc_front(b, "lens", lambda r: TC2_CYAN_HI if r < 1.6 else (120, 230, 255) if r < 3.6 else TC2_CYAN)
    disc_front(b, "lens_guard", lambda r: (236, 252, 255) if r < 1.2 else (150, 236, 255))
    disc_front(b, "core_lens", lambda r: TC2_CYAN_HI if r < 1.6 else (120, 230, 255) if r < 3.2 else TC2_CYAN)
    for name in ("antenna_light", "fin_light"):
        for r in faces(*b[name]).values():
            rect(*r, (255, 60, 50), 2)
    for r in faces(*b["railgun_muzzle"]).values():
        rect(*r, (90, 210, 255), 2)
    x, y, w, h = faces(*b["railgun_muzzle"])["bottom"]
    round_face(x, y, w, h, lambda r, i, k: TC2_CYAN_HI if r < 1.6 else None)
    x, y, w, h = faces(*b["railgun_barrel"])["front"]
    for k in range(4, h - 2, 4):
        rect(x + 2, y + k, w - 4, 1, (110, 220, 255), 1)    # charge rings down the barrel
    x, y, w, h = faces(*b["hammer_hex"])["right"]
    round_face(x, y, w, h, lambda r, i, k: (120, 200, 255) if r < 2.4 else (50, 130, 230) if r < 3.8 else None)
    x, y, w, h = faces(*b["jetpack_engine_right"])["bottom"]
    round_face(x, y, w, h, lambda r, i, k: (150, 230, 255) if r < 1.6 else (60, 170, 250) if r < 3.2 else None)


def titan_cameraman_2():
    b = bb_boxes("titan_cameraman_2")
    new_image(512, 60)

    # Head: a dark camera housing with a big cyan lens behind a protective glass, an antenna braced by steel plates,
    # corner posts, a fin, and a camcorder on each side
    box(*b["neck"], (40, 40, 44), 3)
    x, y, w, h = faces(*b["neck"])["front"]
    for k in range(1, h, 2):
        rect(x, y + k, w, 1, (30, 30, 34), 1)
    f = metal_box(b["neck_plate"], (70, 72, 78))
    x, y, w, h = f["top"]
    for i, k in ((1, 1), (w - 3, 1), (1, h - 3), (w - 3, h - 3)):
        bolt(x + i, y + k)
    f = metal_box(b["housing"], TC2_DARK)
    for key in ("right", "left"):
        x, y, w, h = f[key]
        rect(x + 2, y + 3, w - 4, 1, TC2_DARKER, 1)
        rect(x + 2, y + h - 4, w - 4, 1, TC2_DARKER, 1)
        for i in range(w - 12, w - 3, 2):
            rect(x + i, y + 6, 1, 7, (30, 30, 34), 0)          # vents
        rect(x + 3, y + 6, 6, 4, (210, 60, 50), 2)             # a red warning label
    x, y, w, h = f["top"]
    for i in range(3, w - 3, 3):
        rect(x + i, y + 4, 1, h - 8, (40, 42, 46), 0)
    metal_box(b["housing_chamfer_right"], (66, 68, 76))
    f = faces(*b["face_plate"])
    for r in f.values():
        rect(*r, TC2_DARKER, 2)
    frame(f["front"], (70, 72, 80))
    x, y, w, h = f["front"]
    for i, k in ((1, 1), (w - 3, 1), (1, h - 3), (w - 3, h - 3)):
        bolt(x + i, y + k, (110, 114, 122))
    disc_front(b, "lens_ring", lambda r: (100, 104, 114) if r > 7.4 else (52, 54, 62) if r > 6.2 else (30, 30, 34))
    disc_front(b, "lens", lambda r: (60, 180, 230) if r < 4 else (30, 130, 200))
    disc_front(b, "lens_guard", lambda r: (170, 230, 250))
    metal_box(b["antenna_base"], TC2_STEEL)
    f = faces(*b["antenna"])
    box(*b["antenna"], (26, 26, 30), 2)
    for r in sides(f):
        for k in range(3, r[3], 6):
            rect(r[0], r[1] + k, r[2], 1, (60, 62, 68), 0)    # segments
    box(*b["antenna_light"], (220, 40, 36), 2)
    f = metal_box(b["plate_right"], TC2_STEEL)
    for r in sides(f):
        for k in (2, r[3] - 3):
            bolt(r[0] + r[2] // 2 - 1, r[1] + k, (110, 114, 122))
    metal_box(b["corner_post_right"], (80, 82, 90))
    f = faces(*b["fin"])
    box(*b["fin"], (28, 28, 32), 2)
    for key in ("right", "left"):
        x, y, w, h = f[key]
        rect(x + 2, y + h // 2, w - 4, 1, (50, 52, 58), 1)   # ridge
    box(*b["fin_light"], (220, 40, 36), 2)
    f = metal_box(b["camcorder_right"], (44, 44, 48))
    x, y, w, h = f["right"]
    rect(x + 3, y + 3, w - 6, h - 6, (36, 36, 40), 2)       # side panel
    rect(x + 4, y + 4, 6, 1, (226, 226, 222), 0)            # a white logo stripe
    rect(x + w - 8, y + 4, 4, 2, (200, 60, 50), 1)
    for i in range(6, w - 4, 4):
        bolt(x + i, y + h - 4, (90, 92, 100))
    f = faces(*b["camcorder_lens_right"])
    box(*b["camcorder_lens_right"], (24, 24, 28), 2)
    for r in sides(f):
        rect(r[0], r[1] + 2, r[2], 1, (70, 72, 78), 0)        # focus ring
    black_camera_lens(*f["front"])
    f = faces(*b["camcorder_hood_right"])
    for r in f.values():
        rect(*r, (30, 30, 34), 2)
    x, y, w, h = f["front"]
    rect(x + 1, y + 1, w - 2, h - 2, (12, 12, 14), 0)       # looking into the hood
    black_camera_lens(x + 2, y + 2, w - 4, h - 4)
    metal_box(b["camcorder_handle_right"], (36, 36, 40))
    f = metal_box(b["back_box"], (70, 72, 80))
    x, y, w, h = f["back"]
    for k in range(2, h - 2, 2):
        rect(x + 3, y + k, w - 6, 1, (44, 46, 52), 0)

    # Body: an unbuttoned black coat over a long vest, matte-black gloves, military boots with steel shin plates
    titan4_coat(b, (40, 40, 44), (30, 30, 34))
    f = faces(*b["vest"])
    for r in f.values():
        cloth(r, (54, 54, 60), (44, 44, 50), 2)
    x, y, w, h = f["front"]
    rect(x + w // 2, y, 1, h, (34, 34, 38), 1)               # where the vest closes
    for k in range(4, h - 4, 6):
        rect(x + w // 2 + 2, y + k, 2, 2, (100, 102, 110), 1)   # buttons
    for side in (2, w - 8):
        rect(x + side, y + h - 14, 6, 1, (34, 34, 38), 1)    # pockets
    titan4_gloves(b, (30, 30, 34))
    titan4_legs(b, (34, 34, 38), (18, 18, 20), boots=True)
    f = metal_box(b["shin_plate_right"], (120, 124, 134))
    x, y, w, h = f["front"]
    for i, k in ((1, 1), (w - 3, 1), (1, h - 3), (w - 3, h - 3)):
        bolt(x + i, y + k, (170, 174, 182))
    rect(x + w // 2, y + 3, 1, h - 6, (90, 94, 102), 1)

    # The core: a cyan lens in a black casing, a steel ring, three claws, the iron shield under it
    disc_front(b, "core_case", lambda r: (50, 52, 58) if r > 9.6 else (30, 30, 34))
    x, y, w, h = faces(*b["core_case_mid"])["front"]
    for i, k in ((0, 0), (w - 2, 0), (0, h - 2), (w - 2, h - 2)):
        bolt(x + i, y + k, (90, 92, 100))
    disc_front(b, "core_ring", lambda r: (150, 156, 168) if r > 6.4 else (90, 96, 106))
    disc_front(b, "core_lens", lambda r: (60, 180, 230))
    f = metal_box(b["core_shield"], TC2_STEEL)
    x, y, w, h = f["front"]
    for i in range(2, w - 1, 4):
        bolt(x + i, y + 1, (110, 114, 122))
    for name in ("core_claw_right", "core_claw_bottom"):
        metal_box(b[name], (80, 82, 90))
        x, y, w, h = faces(*b[name])["front"]
        rect(x, y + h - 2, w, 2, (230, 120, 40), 1)            # the orange-tipped claws

    # A silver jetpack with four wings and two engines, grey CCTV cameras on his shoulders
    f = metal_box(b["jetpack"], TC2_SILVER)
    x, y, w, h = f["back"]
    frame((x + 2, y + 2, w - 4, h - 4), (140, 144, 152))
    for i, k in ((3, 3), (w - 5, 3), (3, h - 5), (w - 5, h - 5)):
        bolt(x + i, y + k, (120, 124, 132))
    f = metal_box(b["jetpack_spine"], (150, 154, 162))
    x, y, w, h = f["back"]
    for k in range(3, h - 2, 5):
        rect(x + 1, y + k, w - 2, 1, (110, 114, 122), 0)
    f = metal_box(b["jetpack_engine_right"], (120, 124, 132))
    x, y, w, h = f["bottom"]
    round_face(x, y, w, h, lambda r, i, k: (40, 60, 80) if r < 3.4 else None)
    for name in ("wing_right", "wing_top_right"):
        f = metal_box(b[name], (192, 196, 204))
        x, y, w, h = f["top"]
        for i in range(3, w - 2, 4):
            rect(x + i, y + 1, 1, h - 2, (160, 164, 172), 0)  # wing panels
        x, y, w, h = f["front"]
        rect(x, y, 3, h, (200, 50, 44), 1)                    # red wing tips
    f = metal_box(b["shoulder_cam_right"], (128, 130, 136))
    for key in ("right", "left"):
        x, y, w, h = f[key]
        rect(x + 3, y + 3, w - 6, 1, (100, 102, 108), 1)
        for i in range(w - 10, w - 3, 2):
            rect(x + i, y + 5, 1, h - 8, (100, 102, 108), 0)  # vents
    metal_box(b["shoulder_cam_hood_right"], (110, 112, 118))
    f = faces(*b["shoulder_cam_lens_right"])
    box(*b["shoulder_cam_lens_right"], (20, 20, 24), 2)
    x, y, w, h = f["front"]
    black_camera_lens(x + (w - h) // 2, y, h, h)
    rect(x + w - 2, y, 2, 2, (220, 40, 36), 0)

    # The mechanical hammer in his right hand: steel head with rivets, a ribbed top, a blue hexagon, a jet at the back
    f = metal_box(b["hammer_head"], (120, 124, 134))
    for r in sides(f):
        x, y, w, h = r
        for i, k in ((1, 1), (w - 3, 1), (1, h - 3), (w - 3, h - 3)):
            bolt(x + i, y + k, (160, 164, 172))
        rect(x, y + h // 2, w, 1, (90, 94, 104), 0)
    f = faces(*b["hammer_ribs"])
    for r in sides(f):
        x, y, w, h = r
        rect(x, y, w, h, (176, 180, 188), 3)
        for k in range(0, h, 2):
            rect(x, y + k, w, 1, (120, 124, 132), 0)          # the ribs
    rect(*f["top"], (190, 194, 202), 2)
    f = faces(*b["hammer_hex"])
    box(*b["hammer_hex"], (40, 70, 120), 2)
    x, y, w, h = f["right"]
    round_face(x, y, w, h, lambda r, i, k: (60, 140, 230) if r < 3.8 else None)
    for name in ("hammer_cap_front", "hammer_cap_back"):
        metal_box(b[name], (100, 104, 114))
    f = metal_box(b["hammer_jet"], (60, 62, 70))
    x, y, w, h = f["back"]
    round_face(x, y, w, h, lambda r, i, k: (24, 24, 28) if r < 3.4 else None)
    f = metal_box(b["hammer_handle"], (150, 154, 162))
    for r in sides(f):
        for k in range(4, r[3], 6):
            rect(r[0], r[1] + k, r[2], 2, (50, 110, 210), 1)  # blue bands up the handle

    # The plasma railgun on his left forearm: a grey engine with a warning label, a black panel on top, black and
    # yellow feed cables, two cartridges and a blue barrel
    f = metal_box(b["railgun"], (110, 114, 122))
    x, y, w, h = f["left"]
    rect(x + 2, y + 6, w - 4, 6, (226, 200, 60), 1)          # 'ISOLATE LASER BEFORE REMOVING'
    for i in range(3, w - 3, 2):
        rect(x + i, y + 8, 1, 2, (30, 30, 30), 0)
    for i in range(0, w - 4, 4):
        rect(x + 2 + i, y + h - 4, 2, 2, (30, 30, 30), 0)     # hazard stripes
    metal_box(b["railgun_top"], (34, 34, 38))
    f = metal_box(b["railgun_barrel"], (60, 120, 200))
    for r in sides(f):
        rect(r[0], r[1], r[2], 2, (40, 80, 150), 1)
    box(*b["railgun_muzzle"], (90, 200, 250), 3)
    box(*b["railgun_cable_black"], (24, 24, 26), 2)
    box(*b["railgun_cable_yellow"], (220, 190, 40), 3)
    for name in ("railgun_cartridge0", "railgun_cartridge1"):
        f = metal_box(b[name], (70, 72, 80))
        rect(*f["top"], (180, 150, 60), 2)                   # brass caps

    titan_cameraman_2_lights(b)
    return img


def titan_cameraman_2_glow():
    new_image(512, 60)
    titan_cameraman_2_lights(bb_boxes("titan_cameraman_2"))
    return img


# ---- Titan Speakerman 2.0

TSM2_RED, TSM2_RED_DARK = (150, 34, 34), (108, 22, 24)


def dark_red_shirt(r):
    """His 2.0 shirt: a darker, duller red than before, with a soft band of light."""
    x, y, w, h = r
    gradient(r, TSM2_RED, TSM2_RED_DARK, 3)
    rect(x + w // 4, y, max(1, w // 6), h, (166, 44, 44), 2)


def titan_speakerman_2_lights(b):
    """What glows on Titan Speakerman 2.0: the red drivers in his head and towers, his core, the cannon muzzles and
    the jetpack turbines."""
    speaker_head_lights(b)
    for name in ("tower_driver", "tower_driver_mid", "tower_driver_low"):
        for p in ("wide", "mid", "tall"):
            u, v, w, h, d = b[f"{name}_{p}_right"]
            x, y = faces(u, v, w, h, d)["front"][:2]
            for i in range(w):
                for k in range(h):
                    rr = math.hypot(i - (w - 1) / 2, k - (h - 1) / 2)
                    if rr < 13 / 2 - 1.3:
                        px[x + i, y + k] = shade(red_driver(rr, 13), 2)
    x, y, w, h = faces(*b["core_light"])["front"]
    round_face(x, y, w, h, lambda r, i, k: (255, 210, 200) if r < 1.2 else (255, 80, 64))
    for key in ("right", "left", "top", "bottom"):
        rect(*faces(*b["core_light"])[key], (240, 60, 50), 2)
    disc_front(b, "core_dome", lambda r: (255, 100, 84) if r < 4 else None)
    disc_front(b, "core_glass", lambda r: (220, 50, 44) if r < 6 else None)
    for name in ("cannon_muzzle_right", "cannon_muzzle_left"):
        x, y, w, h = faces(*b[name])["bottom"]
        round_face(x, y, w, h, lambda r, i, k: (255, 160, 120) if r < 2 else (250, 60, 36) if r < 5 else None)
    x, y, w, h = faces(*b["turbine_nozzle_right"])["bottom"]
    round_face(x, y, w, h, lambda r, i, k: (255, 140, 80) if r < 2.4 else (240, 70, 40) if r < 4.4 else None)


def titan_speakerman_2():
    b = bb_boxes("titan_speakerman_2")
    new_image(512, 61)
    steel = (140, 144, 152)

    # Head: his speakers under a riveted metal helmet with a frame of struts above it
    speaker_head_tex(b)
    f = metal_box(b["helmet"], steel)
    for r in sides(f):
        x, y, w, h = r
        for i in range(2, w - 1, 5):
            bolt(x + i, y + h - 3, (176, 180, 188))
    x, y, w, h = f["top"]
    rect(x + 3, y + h // 2, w - 6, 2, (110, 114, 122), 1)
    f = faces(*b["helmet_visor"])
    for r in f.values():
        rect(*r, (30, 30, 34), 2)
    rect(*f["front"], (60, 30, 30), 2)
    for name in ("frame_right", "frame_top"):
        metal_box(b[name], (90, 92, 100))

    # Body: a darker red shirt, black gloves, dark grey trousers, boots
    titan4_shirt(b, dark_red_shirt, TSM2_RED)
    titan4_gloves(b, (30, 30, 34))
    titan4_legs(b, (70, 72, 78), (18, 18, 20), boots=True)

    # A bigger red core in a glass dome between two speakers, and a black box of speakers under it
    disc_front(b, "core_ring", lambda r: (170, 174, 182) if r > 9.6 else (120, 124, 132) if r > 8.4 else (80, 84, 92))
    x, y, w, h = faces(*b["core_ring_mid"])["front"]
    for i, k in ((0, 0), (w - 2, 0), (0, h - 2), (w - 2, h - 2)):
        bolt(x + i, y + k)
    disc_front(b, "core_glass", lambda r: (200, 206, 214) if r > 7.4 else (160, 60, 60) if r > 6 else (200, 44, 44))
    disc_front(b, "core_dome", lambda r: (236, 120, 110) if r < 2 else (214, 60, 56))
    x, y, w, h = faces(*b["core_dome_mid"])["front"]
    rect(x + 1, y + 1, 2, 1, (255, 230, 226), 0)
    box(*b["core_light"], (255, 100, 84), 2)
    for name in ("core_speaker_right", "belly_box"):
        speaker_box(b, name)
    for name, n in (("core_driver", 7), ("core_driver_low", 7), ("belly_driver_side", 5)):
        driver_disc(b, name, n, black_driver, (50, 50, 56))
    driver_disc(b, "belly_driver", 8, black_driver, (50, 50, 56), side="")

    # A huge dark jetpack with two big turbines
    f = metal_box(b["jetpack"], (62, 64, 70))
    f = faces(*b["jetpack_panel"])
    for r in f.values():
        rect(*r, (50, 52, 58), 2)
    x, y, w, h = f["back"]
    frame((x, y, w, h), (80, 82, 90))
    for k in range(4, h - 3, 3):
        rect(x + 3, y + k, w - 6, 1, (34, 34, 38), 0)        # vents
    for i, k in ((1, 1), (w - 3, 1), (1, h - 3), (w - 3, h - 3)):
        bolt(x + i, y + k, (120, 124, 132))
    f = faces(*b["jetpack_turbine_right"])
    for r in sides(f):
        gradient(r, (84, 86, 92), (56, 58, 64), 3)
        x, y, w, h = r
        for k in range(6, h - 4, 8):
            rect(x, y + k, w, 1, (44, 46, 52), 0)
        rect(x + w // 2, y + 2, 1, h - 4, (100, 102, 110), 1)
    rect(*f["top"], (30, 30, 34), 2)
    x, y, w, h = f["top"]
    round_face(x, y, w, h, lambda r, i, k: (60, 62, 68) if r > 4 else (20, 20, 24))   # intake
    metal_box(b["turbine_ring_right"], (110, 112, 120))
    f = metal_box(b["turbine_nozzle_right"], (40, 40, 44))
    for r in sides(f):
        for i in range(0, r[2], 2):
            rect(r[0] + i, r[1], 1, r[3], (28, 28, 32), 0)   # nozzle petals

    # The towers on his shoulders: black, red-lit drivers, steel plates at the back
    speaker_box(b, "tower_right")
    x, y, w, h = faces(*b["tower_right"])["front"]
    for k in (8, 23, 38, 53):
        rect(x + w // 2 - 1, y + k, 2, 2, (220, 220, 216), 1)
    f = faces(*b["tower_cap_right"])
    for r in f.values():
        rect(*r, (46, 46, 50), 2)
    for name in ("tower_driver", "tower_driver_mid", "tower_driver_low"):
        driver_disc(b, name, 13, red_driver, (40, 20, 22))
    f = metal_box(b["tower_plate_right"], steel)
    x, y, w, h = f["back"]
    for i, k in ((1, 1), (w - 3, 1), (1, h - 3), (w - 3, h - 3)):
        bolt(x + i, y + k, (176, 180, 188))
    for k in range(6, h - 4, 6):
        rect(x + 2, y + k, w - 4, 1, (110, 114, 122), 0)     # armour seams

    # Long cannons along both forearms, a frame and hook at the top, banded, muzzles glowing red
    for side in ("right", "left"):
        f = faces(*b[f"cannon_{side}"])
        for r in sides(f):
            gradient(r, (48, 48, 52), (28, 28, 32), 3)
            x, y, w, h = r
            for k in range(6, h - 2, 10):
                rect(x, y + k, w, 1, (64, 66, 72), 0)        # armour plates
            rect(x + w // 2, y + 4, 1, h - 8, (36, 36, 40), 0)
        rect(*f["top"], (60, 60, 66), 3)
        x, y, w, h = f["front"] if side == "right" else f["front"]
        for k in range(10, h - 8, 12):
            rect(x + 2, y + k, 3, 2, (200, 40, 36), 1)       # red status lights
        for name in (f"cannon_frame_{side}", f"cannon_hook_{side}", f"cannon_band0_{side}", f"cannon_band1_{side}"):
            metal_box(b[name], (70, 72, 80))
        f = faces(*b[f"cannon_muzzle_{side}"])
        box(*b[f"cannon_muzzle_{side}"], (36, 36, 40), 2)
        x, y, w, h = f["bottom"]
        round_face(x, y, w, h, lambda r, i, k: (150, 30, 24) if r < 5 else (24, 24, 26))

    titan_speakerman_2_lights(b)
    return img


def titan_speakerman_2_glow():
    new_image(512, 61)
    titan_speakerman_2_lights(bb_boxes("titan_speakerman_2"))
    return img


# ---- Titan TV Man 2.0

def titan_tv_man_2_lights(b):
    """What glows on Titan TV Man 2.0: the static on all his screens, his purple core and tentacle, the edges of his
    claws, his energy sword and the purple lines down his sleeves."""
    rnd = random.Random(62)
    x, y, w, h = faces(*b["tv"])["front"]
    crt_static((x + 3, y + 3, w - 6, h - 6), rnd, (170, 252))
    for name in ("side_screen_right", "shoulder_tv_screen_right"):
        x, y, w, h = faces(*b[name])["front"]
        crt_static((x + 1, y + 1, w - 2, h - 2), rnd, (170, 252))
    x, y, w, h = faces(*b["belt_tv"])["front"]
    crt_static((x + 2, y + 2, w - 4, h - 4), rnd, (170, 252))
    disc_front(b, "core_inner", lambda r: TTV_PURPLE_HI if r < 2.6 else (210, 140, 255) if r < 5 else TTV_PURPLE)
    x, y, w, h = faces(*b["core_light"])["front"]
    rect(x, y, w, h, (252, 236, 255), 2)
    for key in ("right", "left", "top", "bottom"):
        rect(*faces(*b["core_light"])[key], TTV_PURPLE_HI, 2)
    for r in faces(*b["tentacle"]).values():
        cloth(r, (200, 110, 255), (150, 70, 230), 3, 3)
    for i in range(3):
        f = faces(*b[f"claw{i}_edge_right"])
        for r in f.values():
            rect(*r, TTV_PURPLE, 3)
        rect(*f["front"], (210, 150, 255), 3)
    f = faces(*b["blade"])
    for key in ("right", "left", "front", "back"):
        x, y, w, h = f[key]
        gradient((x, y, w, h), (236, 170, 255), (170, 60, 240), 1)
        rect(x + w // 2, y, 1, h, (255, 230, 255), 1)       # the hot core of the blade
    for key in ("right",):
        x, y, w, h = faces(*b["sleeve_right"])[key]
        rect(x + w // 2, y + 2, 1, h - 4, TTV_PURPLE, 2)     # a purple line down each sleeve
    x, y, w, h = faces(*b["head_turbine_nozzle_right"])["back"]
    rect(x + 1, y + 1, w - 2, h - 2, (160, 80, 240), 2)


def titan_tv_man_2():
    b = bb_boxes("titan_tv_man_2")
    new_image(512, 62)
    black = (28, 28, 32)

    # Head: a wide, thin TV in a thick bezel with a thin screen clamped at each side, a crown of armour spikes and fins,
    # turbines at the back, and a purple tentacle in a steel ring for a neck
    f = faces(*b["tv"])
    for r in sides(f):
        gradient(r, (40, 40, 44), (28, 28, 32), 2)
    rect(*f["top"], (44, 44, 48), 2)
    x, y, w, h = f["front"]
    rect(x + 3, y + 3, w - 6, h - 6, (14, 14, 16), 0)
    f = faces(*b["tv_back"])
    for r in f.values():
        rect(*r, (30, 30, 34), 2)
    x, y, w, h = f["back"]
    for k in range(3, h - 2, 2):
        rect(x + 4, y + k, w - 8, 1, (20, 20, 24), 0)
    for name in ("bezel_top", "bezel_bottom", "bezel_side_right"):
        f = faces(*b[name])
        for r in f.values():
            rect(*r, (44, 44, 50), 2)
        rect(*f["front"], (52, 52, 58), 2)
    x, y, w, h = faces(*b["bezel_bottom"])["front"]
    rect(x + w - 6, y + 1, 2, 1, (180, 100, 255), 0)        # a purple power light
    f = faces(*b["side_screen_right"])
    for r in f.values():
        rect(*r, (34, 34, 38), 2)
    rect(*f["front"], (16, 16, 18), 0)
    metal_box(b["side_screen_clamp_right"], (110, 114, 122))
    for name in ("crown", "crown_spike_mid", "crown_spike_right", "crown_spike_outer_right", "crown_fin_right"):
        f = metal_box(b[name], (70, 72, 82))
        rect(*f["top"], (120, 122, 132), 2)
    f = faces(*b["head_turbine_right"])
    for r in sides(f):
        gradient(r, (76, 78, 86), (46, 48, 54), 3)
        x, y, w, h = r
        rect(x, y + 3, w, 1, (36, 36, 40), 0)
        rect(x, y + h - 4, w, 1, (36, 36, 40), 0)
    box(*b["head_turbine_nozzle_right"], (34, 34, 38), 2)
    f = metal_box(b["ring"], (120, 124, 134))
    x, y, w, h = f["top"]
    for i, k in ((1, 1), (w - 3, 1), (1, h - 3), (w - 3, h - 3)):
        bolt(x + i, y + k)
    box(*b["tentacle"], (150, 70, 230), 3)

    # Body: a long black coat, black gloves and boots
    titan4_coat(b, black, (16, 16, 20))
    titan4_gloves(b, (32, 32, 36))
    titan4_legs(b, (28, 28, 32), (18, 18, 20), boots=True)

    # The big purple core in a round housing with three claws, a TV on his belt
    disc_front(b, "core_case", lambda r: (84, 78, 96) if r > 10.6 else (56, 52, 66))
    x, y, w, h = faces(*b["core_case_mid"])["front"]
    for i, k in ((0, 0), (w - 2, 0), (0, h - 2), (w - 2, h - 2)):
        bolt(x + i, y + k, (110, 104, 122))
    disc_front(b, "core_inner", lambda r: (150, 70, 230))
    box(*b["core_light"], (240, 210, 255), 2)
    for name in ("core_claw_right", "core_claw_bottom"):
        metal_box(b[name], (150, 152, 160))
    f = faces(*b["belt_tv"])
    for r in f.values():
        rect(*r, (34, 34, 38), 2)
    rect(*f["front"], (16, 16, 18), 0)

    # TVs on his shoulders and claws on his back
    f = faces(*b["shoulder_tv_right"])
    for r in f.values():
        gradient(r, (40, 40, 44), (28, 28, 32), 2)
    box(*b["shoulder_tv_screen_right"], (16, 16, 18), 0)
    f = faces(*b["claw_mount"])
    for r in f.values():
        gradient(r, (34, 32, 40), (24, 22, 28), 2)
    f = faces(*b["claw_mount_plate"])
    for r in f.values():
        rect(*r, (40, 38, 46), 2)
    x, y, w, h = f["back"]
    for i in range(2, w - 1, 6):
        for k in (1, h - 3):
            bolt(x + i, y + k, (100, 96, 110))
    for i in range(3):
        f = faces(*b[f"claw{i}_right"])
        for r in sides(f):
            gradient(r, (46, 42, 52), (24, 22, 28), 3)
            x, y, w, h = r
            rect(x + w // 2, y + 2, 1, h - 4, (60, 56, 68), 1)
        rect(*f["top"], (60, 56, 66), 2)
        f = faces(*b[f"claw{i}_tip_right"])
        for r in f.values():
            gradient(r, (58, 54, 64), (36, 32, 42), 2)
        box(*b[f"claw{i}_edge_right"], (120, 60, 190), 3)

    # The energy sword in its holster on his right forearm
    f = metal_box(b["blade_holster"], (70, 72, 80))
    x, y, w, h = f["right"]
    for k in range(3, h - 2, 7):
        rect(x, y + k, w, 2, (40, 40, 44), 1)                # straps
        px[x + w // 2, y + k] = (200, 40, 40, 255)           # red marks at the straps
    f = metal_box(b["blade_hilt"], (44, 44, 50))
    rect(*f["top"], (150, 70, 230), 2)
    box(*b["blade"], (180, 90, 250), 3)

    titan_tv_man_2_lights(b)
    return img


def titan_tv_man_2_glow():
    new_image(512, 62)
    titan_tv_man_2_lights(bb_boxes("titan_tv_man_2"))
    return img


GMAN_SKIN = (230, 208, 174)
GMAN_HAIR = (34, 28, 36)
G_PORCELAIN = (222, 224, 226)

# G-Man's face, 14x16, as in the series: hair receding at the temples, huge furrowed brows over dark eye
# sockets, pale teal eyes, a big nose, deep smile lines and an enormous crescent grin showing his upper teeth.
# The brows and nose stand out of the face as their own cubes.
GMAN_FACE = [
    "HHHHHHhHHHHHHH",
    "HHHHHHHHHHHHHH",
    "ssHHHHHHHHHHss",
    "ssssssssssssss",
    "ssssssssssssss",
    "sbbssssssssbbs",
    "ssbbbbssbbbbss",
    "sDDDDDssDDDDDs",
    "sDWipWssWpiWDs",
    "ssdDDdssdDDdss",
    "rmsDdnnnndDsmr",
    "smmllllllllmms",
    "sDmmmmmmmmmmDs",
    "ssmttttttttmss",
    "sssmllllllmsss",
    "dssssddddssssd",
]
GMAN_COLORS = {
    "H": GMAN_HAIR, "h": (62, 52, 66), "s": GMAN_SKIN, "d": (202, 178, 144), "D": (150, 124, 96),
    "r": (230, 196, 166), "b": (40, 32, 30), "W": (214, 232, 226), "i": (110, 190, 180), "p": (36, 60, 60),
    "n": (130, 86, 66), "l": (156, 84, 72), "t": (236, 232, 216), "m": (58, 14, 14),
}


def pixel_art(x, y, rows, colors, noise=3):
    for k, row in enumerate(rows):
        for i, ch in enumerate(row):
            px[x + i, y + k] = shade(colors[ch], noise if ch in "sdrH" else 1)


# The colours of G-Man's head beyond his face palette; other heads in the same tall toilet pass their own.
GMAN_LOOK = {
    "skin": GMAN_SKIN, "hair": GMAN_HAIR, "streak": (54, 46, 58), "top_streak": (56, 48, 60),
    "jaw": (200, 166, 130), "nape": (210, 176, 140), "chin": (190, 154, 120), "brow_shadow": (150, 110, 85),
    "bridge": (232, 204, 170), "nose": (220, 186, 150), "nostril": (120, 78, 60), "nose_bottom": (150, 108, 84),
    "ear": (214, 178, 142), "inner_ear": (190, 150, 118), "neck_top": (176, 140, 110), "neck_bottom": (214, 180, 146),
}


def hair_side(r, front_at_right, look):
    """A side of the head: hair over the top, down to the nape at the back; skin and jaw below."""
    x, y, w, h = r
    rect(x, y, w, h, look["skin"], 3)
    for i in range(w):
        back = (w - 1 - i if front_at_right else i) / (w - 1)       # 1 at the back edge, 0 at the front
        line = round(5 + 6 * back ** 2)
        rect(x + i, y, 1, line, look["hair"], 3)
        if i % 3 == 1:
            rect(x + i, y + 1, 1, max(1, line - 3), look["streak"], 2)  # combed streaks
    rect(x, y + h - 2, w, 2, look["jaw"], 3)                        # jaw shadow


def gman_toilet():
    return infra_toilet("gman_toilet", 8, GMAN_FACE, ["bbssssssssbb", "sbbbbssbbbbs", "ssssbbbbssss"],
                        GMAN_COLORS, GMAN_LOOK)


def infra_toilet(model, seed, face, brow, colors, look):
    """G-Toilet's tall toilet (gman_toilet.bbmodel's layout) with a 14x16 face and a 12x3 brow."""
    b = bb_boxes("gman_toilet")
    new_image(256, seed, 128)

    # Head
    f = faces(*b["head"])
    pixel_art(*f["front"][:2], face, colors)
    hair_side(f["right"], True, look)
    hair_side(f["left"], False, look)
    x, y, w, h = f["top"]
    rect(x, y, w, h, look["hair"], 3)
    for i in range(1, w, 3):
        rect(x + i, y, 1, h, look["top_streak"], 2)                 # combed back, front to back
    x, y, w, h = f["back"]
    rect(x, y, w, h, look["hair"], 3)
    rect(x, y + h - 4, w, 4, look["nape"], 3)                       # nape
    rect(*f["bottom"], look["chin"], 2)
    f = faces(*b["brow"])
    box(*b["brow"], look["skin"], 3)
    pixel_art(*f["front"][:2], brow, colors)
    rect(*f["top"], look["skin"], 3)
    rect(*f["bottom"], look["brow_shadow"], 2)                      # shadow over the eyes
    box(*b["nose_bridge"], look["bridge"], 3)
    f = faces(*b["nose"])
    box(*b["nose"], look["nose"], 3)
    x, y, w, h = f["front"]
    px[x + 1, y + h - 1] = look["nostril"] + (255,)                 # nostrils
    px[x + 2, y + h - 1] = look["nostril"] + (255,)
    rect(*f["bottom"], look["nose_bottom"], 2)
    f = faces(*b["ear_right"])
    box(*b["ear_right"], look["ear"], 3)
    rect(*f["right"], look["inner_ear"], 0)                         # inner ear
    f = faces(*b["neck"])
    for r in sides(f):
        gradient(r, look["neck_top"], look["neck_bottom"], 3)       # in the shadow of the jaw

    # Toilet: smooth, slightly dull porcelain, darker toward the floor
    for name, hi, lo in (("rim", (240, 241, 243), (226, 228, 230)), ("bowl", (232, 234, 236), (214, 216, 220)),
                         ("bowl_lower", (214, 216, 220), (200, 203, 208)),
                         ("pedestal", (206, 209, 213), (190, 194, 199)), ("base", (196, 199, 204), (184, 188, 193))):
        for part in (name, name + "_side_right"):
            f = faces(*b[part])
            rect(*f["top"], (244, 245, 246), 2)
            rect(*f["bottom"], (170, 174, 180), 2)
            for r in sides(f):
                gradient(r, hi, lo, 2)
    x, y, w, h = faces(*b["rim"])["top"]                            # the inside of the bowl
    rect(x + 1, y + 3, w - 2, h - 6, (176, 180, 186), 2)
    rect(x + 3, y + 5, w - 6, h - 10, (150, 155, 162), 2)
    rect(x + 5, y + 8, w - 10, h - 16, (128, 134, 142), 2)
    for name, hi, lo in (("tank_base", (214, 216, 220), (190, 194, 199)), ("tank", G_PORCELAIN, (210, 213, 217)),
                         ("ledge", (236, 238, 240), (226, 228, 231)), ("tank_cap", (232, 234, 236), (226, 228, 231))):
        f = faces(*b[name])
        rect(*f["top"], (240, 241, 243), 2)
        rect(*f["bottom"], (170, 174, 180), 2)
        for r in sides(f):
            gradient(r, hi, lo, 2)
    box(*b["flush_knob"], (200, 204, 210), 3)
    rect(*faces(*b["flush_knob"])["top"], (226, 230, 236), 2)
    return img


# Male_09 as the Giant Flying Skibidi Toilet: short dark hair, heavy brows, wide eyes, a huge toothy grin.
GIANT_FACE = [
    "HHHHHHHHHHHHHH",
    "HHHHHHHHHHHHHH",
    "HHssssssssssHH",
    "ssssssssssssss",
    "ssssssssssssss",
    "ssssssssssssss",
    "sbbbbssssbbbbs",
    "sdDDDdssdDDDds",
    "sDWWpDssDpWWDs",
    "ssdddsssdddsss",
    "rssDdnnnndDssr",
    "sDttttttttttDs",
    "sDmmmmmmmmmmDs",
    "ssDttttttttDss",
    "sssDllllllDsss",
    "dssssddddssssd",
]


def giant_flying_toilet():
    skin, hair = (214, 160, 118), (40, 32, 28)
    colors = {**GMAN_COLORS, "H": hair, "h": (60, 48, 42), "s": skin, "d": (190, 140, 104), "D": (150, 106, 78),
              "r": (220, 158, 120), "b": (34, 26, 22), "W": (236, 236, 230), "i": (90, 70, 50), "p": (30, 24, 20)}
    look = {"skin": skin, "hair": hair, "streak": (56, 46, 40), "top_streak": (52, 42, 36), "jaw": (188, 138, 102),
            "nape": (200, 148, 110), "chin": (180, 130, 96), "brow_shadow": (140, 100, 74), "bridge": (222, 170, 128),
            "nose": (208, 154, 114), "nostril": (110, 70, 52), "nose_bottom": (150, 106, 78), "ear": (204, 150, 110),
            "inner_ear": (176, 126, 92), "neck_top": (168, 120, 88), "neck_bottom": (206, 152, 112)}
    return infra_toilet("gman_toilet", 16, GIANT_FACE, ["ssssssssssss", "bbbbbssbbbbb", "ssssssssssss"], colors, look)


# ---------------------------------------------------------------- items

def plunger():
    new_image(16, 11)
    for t in range(8):                     # wooden handle, bottom-left to top-right
        px[2 + t, 13 - t] = shade((168, 120, 70), 6)
        px[2 + t, 14 - t] = shade((118, 80, 44), 6)
    cx, cy = 11.0, 5.0                     # rubber cup at the top
    for x in range(16):
        for y in range(16):
            if math.hypot(x - cx, y - cy) <= 3.3:
                along = ((x - cx) - (y - cy)) / math.sqrt(2)  # toward the top-right
                if along > 1.6:
                    px[x, y] = shade((120, 14, 14), 4)        # cup opening
                else:
                    px[x, y] = shade((192, 32, 32), 6)
    px[9, 4] = (236, 96, 96, 255)          # shine
    px[10, 3] = (236, 96, 96, 255)
    outline((40, 16, 16))
    return img


def alliance_blaster():
    new_image(16, 12)
    light, dark, energy = (214, 219, 228), (72, 78, 90), (80, 220, 255)
    rect(2, 4, 8, 5, light, 5)             # body
    rect(10, 5, 4, 2, light, 5)            # barrel
    rect(14, 5, 1, 2, dark, 3)             # muzzle
    rect(3, 6, 6, 1, energy, 8)            # energy cell
    rect(12, 4, 1, 1, dark, 3)             # sight
    for y in range(9, 14):                 # grip, leaning back
        rect(3 + (13 - y) // 3, y, 2, 1, dark, 4)
    px[7, 9] = dark + (255,)               # trigger
    px[7, 10] = dark + (255,)
    outline((30, 32, 40))
    px[15, 5] = energy + (255,)            # glowing muzzle tip
    px[15, 6] = energy + (255,)
    return img


def stun_rifle():
    new_image(16, 21)
    body, dark, coil = (92, 98, 110), (40, 42, 50), (255, 226, 92)
    rect(1, 7, 3, 3, dark, 4)              # stock
    rect(4, 6, 6, 4, body, 5)              # body
    rect(10, 7, 4, 2, body, 5)             # barrel
    rect(5, 8, 4, 1, coil, 6)              # power cell
    for x in (11, 13):                     # glowing coils round the barrel
        rect(x, 6, 1, 4, coil, 6)
    rect(6, 4, 3, 2, dark, 3)              # scope
    px[9, 4] = (140, 200, 255, 255)        # scope lens
    for y in range(10, 14):                # grip
        rect(5 + (13 - y) // 3, y, 2, 1, dark, 4)
    outline((26, 26, 32))
    px[15, 6] = coil + (255,)              # prongs crackling at the tip
    px[15, 9] = coil + (255,)
    px[14, 6] = (255, 255, 220, 255)
    px[14, 9] = (255, 255, 220, 255)
    return img


def gravity_plunger():
    new_image(16, 22)
    for t in range(8):                     # metal handle with a power stripe
        px[2 + t, 13 - t] = shade(METAL, 5)
        px[2 + t, 14 - t] = shade((120, 124, 132), 5)
    for t in (2, 5):
        px[2 + t, 13 - t] = (190, 120, 255, 255)
    cx, cy = 11.0, 5.0                     # dark cup with a purple field in it
    for x in range(16):
        for y in range(16):
            r = math.hypot(x - cx, y - cy)
            if r <= 3.3:
                along = ((x - cx) - (y - cy)) / math.sqrt(2)
                if along > 1.6:
                    px[x, y] = shade((170, 90, 255), 8)
                elif r > 2.4:
                    px[x, y] = shade((62, 64, 76), 4)
                else:
                    px[x, y] = shade((38, 40, 48), 4)
    outline((24, 18, 40))
    px[14, 2] = (220, 180, 255, 255)       # field glowing past the rim
    px[15, 3] = (200, 150, 255, 255)
    return img


def energy_knife():
    new_image(16, 23)
    for t in range(4):                     # black grip, bottom-left
        px[2 + t, 13 - t] = shade((34, 34, 40), 4)
        px[3 + t, 13 - t] = shade((58, 58, 66), 4)
    rect(5, 9, 2, 2, (150, 154, 162), 4)  # guard
    px[4, 9] = (150, 154, 162, 255)
    px[7, 11] = (150, 154, 162, 255)
    for t in range(7):                     # glowing blade, white core
        px[7 + t, 8 - t] = (220, 250, 255, 255)
        px[8 + t, 8 - t] = shade((70, 200, 255), 8)
        px[7 + t, 7 - t] = shade((70, 200, 255), 8)
    outline((16, 60, 110))
    return img


# ---------------------------------------------------------------- power icons
# 16x16, drawn on a dark rounded tile so they read on any background.

def icon_tile(seed, rim):
    new_image(16, seed)
    for x in range(16):
        for y in range(16):
            corner = min(x, 15 - x) + min(y, 15 - y)
            if corner >= 2:
                edge = min(x, y, 15 - x, 15 - y) == 0 or corner == 2
                px[x, y] = rim + (255,) if edge else shade((30, 32, 40), 2)
    return img


def dot(x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        px[x, y] = c + (255,)


def icon_holoshield():
    icon_tile(31, (60, 160, 200))
    for y in range(3, 13):                 # shield shape, narrowing to a point
        half = 5 if y < 9 else 5 - (y - 8)
        for x in range(8 - half, 8 + half):
            edge = x in (8 - half, 8 + half - 1) or y == 3
            dot(x, y, (150, 240, 255) if edge else (60, 190, 230) if (x + y) % 2 else (80, 210, 240))
    for y in range(5, 11):
        dot(7, y, (220, 255, 255))
    return img


def icon_record_scan():
    icon_tile(32, (80, 190, 110))
    for x in range(16):
        for y in range(16):
            r = math.hypot(x - 7.5, y - 8.5)
            if r <= 4.6:
                dot(x, y, (200, 205, 215) if r > 3.6 else (40, 60, 90) if r > 1.8 else (120, 200, 255))
    dot(6, 7, (230, 245, 255))
    for x, y in ((11, 3), (12, 3), (11, 4), (12, 4)):
        dot(x, y, (240, 50, 50))           # REC light
    for x in (2, 3, 12, 13):               # scan brackets
        dot(x, 2, (124, 255, 155))
        dot(x, 14, (124, 255, 155))
    for y in (3, 13):
        dot(2, y, (124, 255, 155))
        dot(13, y, (124, 255, 155))
    return img


def icon_sonic_blast():
    icon_tile(33, (110, 170, 220))
    rect(2, 6, 2, 4, (150, 150, 158), 3)   # speaker
    for i in range(3):
        rect(4 + i, 5 - i, 1, 6 + 2 * i, (110, 110, 118), 3)
    for k, r in enumerate((5.0, 7.5, 10.0)):
        for a in range(-40, 41, 8):
            x = 4 + r * math.cos(math.radians(a))
            y = 8 + r * math.sin(math.radians(a))
            dot(int(round(x)), int(round(y)), (154, 216, 255) if k < 2 else (110, 170, 220))
    return img


def icon_sonic_boots():
    icon_tile(34, (110, 170, 220))
    rect(6, 3, 4, 7, (60, 60, 66), 3)      # boot
    rect(6, 10, 7, 3, (60, 60, 66), 3)
    rect(6, 12, 7, 1, (154, 216, 255), 6)  # glowing sole
    for y, length in ((6, 3), (9, 4), (12, 3)):
        for x in range(1, 1 + length):
            dot(x, y, (154, 216, 255))
    return img


def icon_anthem_dance():
    icon_tile(35, (110, 170, 220))
    gold = (255, 214, 102)
    for y in range(3, 11):                 # two stems with a beam
        dot(6, y, gold)
        dot(11, y - 1, gold)
    rect(6, 3, 6, 2, gold, 6)
    for cx, cy in ((4.5, 11.0), (9.5, 10.0)):
        for x in range(16):
            for y in range(16):
                if math.hypot((x - cx) * 0.8, y - cy) <= 1.6:
                    dot(x, y, gold)
    return img


def icon_hypnosis():
    icon_tile(36, (160, 90, 230))
    for t in range(0, 420):                # spiral, two and a bit turns
        a = t * 0.03
        r = 0.4 + a * 0.47
        x, y = 7.5 + r * math.cos(a), 7.5 + r * math.sin(a)
        dot(int(x + 0.5), int(y + 0.5), (220, 170, 255) if r < 3.5 else (170, 100, 240))
    return img


def icon_teleport():
    icon_tile(37, (160, 90, 230))
    for x in range(16):
        for y in range(16):
            r = math.hypot(x - 7.5, (y - 7.5) * 0.75)
            if 4.0 <= r <= 5.4:
                dot(x, y, (90, 50, 140) if (x + y) % 3 else (200, 200, 210))
            elif r < 4.0:
                dot(x, y, (14, 12, 20))
    for x, y in ((6, 7), (7, 7), (8, 7), (9, 7), (8, 6), (8, 8)):
        dot(x, y, (230, 230, 240))         # arrow through the portal
    return img


def icon_burning_light():
    icon_tile(38, (230, 140, 60))
    rect(2, 5, 4, 6, (70, 72, 80), 3)      # little TV
    rect(3, 6, 2, 4, (255, 240, 200), 4)
    for x in range(6, 15):                 # cone of light
        spread = (x - 6) * 0.55
        for y in range(16):
            if abs(y - 8) <= 1 + spread:
                inner = abs(y - 8) <= spread * 0.4
                dot(x, y, (255, 230, 150) if inner else (255, 150, 60))
    return img


def icon_stun_flash():
    icon_tile(39, (240, 220, 90))
    bolt = [(9, 2), (8, 3), (8, 4), (7, 5), (7, 6), (6, 7), (7, 7), (8, 7), (9, 7), (9, 8), (8, 9), (8, 10),
            (7, 11), (7, 12), (6, 13)]
    for x, y in bolt:
        dot(x, y, (255, 242, 122))
        dot(x + 1, y, (255, 255, 230))
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12), (2, 8), (13, 8)):
        dot(x, y, (255, 255, 255))
    return img


def figure(cx, bottom, h, suit, head, core=None):
    """A blocky Alliance member h pixels tall standing on row `bottom`, centred on column cx: a boxy device
    head, a suit with arms, two legs, and a glowing core on the chest if given."""
    head_h = max(1, round(h * 0.28))
    body_h = max(1, round(h * 0.4))
    leg_h = h - head_h - body_h
    half = max(1, round(h * 0.22))
    top = bottom - h + 1
    for x in range(cx - half, cx + half + 1):               # head
        for y in range(top, top + head_h):
            dot(x, y, head)
    for x in range(cx - half - 1, cx + half + 2):           # shoulders, chest and arms
        for y in range(top + head_h, top + head_h + body_h):
            arm = x in (cx - half - 1, cx + half + 1)
            if arm and y == top + head_h and h >= 8:
                continue
            dot(x, y, shade(suit, 4)[:3] if not arm else tuple(max(0, v - 25) for v in suit))
    for x in (cx - max(1, half // 2) - (1 if h >= 8 else 0), cx + max(1, half // 2)):   # legs
        for w in range(2 if h >= 8 else 1):
            for y in range(bottom - leg_h + 1, bottom + 1):
                dot(x + w, y, tuple(max(0, v - 15) for v in suit))
    if core:
        cy = top + head_h + body_h // 3
        dot(cx, cy, core)
        if h >= 10:
            for x, y in ((cx - 1, cy), (cx + 1, cy), (cx, cy - 1), (cx, cy + 1)):
                dot(x, y, tuple(min(255, v // 2 + 60) for v in core))


def icon_form_large():
    icon_tile(40, (255, 196, 90))
    figure(4, 13, 6, (110, 114, 126), (176, 178, 186))
    figure(10, 13, 10, (92, 96, 110), (214, 216, 224))
    for x, y in ((6, 2), (5, 3), (6, 3), (7, 3), (4, 4), (6, 4), (8, 4), (6, 5), (6, 6)):   # arrow up
        dot(x, y, (255, 214, 102))
    return img


def icon_form_titan():
    icon_tile(41, (255, 196, 90))
    figure(8, 14, 13, (92, 96, 110), (206, 208, 216), core=(120, 220, 255))
    return img


def icon_form_titan_2():
    icon_tile(42, (255, 196, 90))
    for x, y in ((2, 3), (13, 3), (2, 9), (13, 9), (3, 6), (12, 6)):     # power crackling off him
        dot(x, y, (255, 230, 140))
    figure(8, 14, 13, (104, 100, 92), (240, 206, 120), core=(255, 240, 160))
    return img


def icon_core_beam():
    icon_tile(43, (70, 200, 255))
    for x in range(16):
        for y in range(16):
            r = math.hypot(x - 4.5, y - 7.5)
            if r <= 3.2:
                dot(x, y, (230, 250, 255) if r < 1.3 else (110, 215, 255) if r < 2.4 else (40, 110, 170))
    for x in range(7, 15):                 # the beam, white in the middle
        dot(x, 6, (110, 215, 255))
        dot(x, 7, (235, 252, 255))
        dot(x, 8, (110, 215, 255))
    return img


def icon_titan_jetpack():
    icon_tile(44, (255, 150, 60))
    rect(4, 2, 3, 7, (70, 72, 80), 3)       # two thrusters
    rect(9, 2, 3, 7, (70, 72, 80), 3)
    rect(4, 2, 8, 1, (100, 102, 110), 2)
    for cx in (5, 10):                     # flames
        for y, half in ((9, 1), (10, 1), (11, 1), (12, 0), (13, 0)):
            for x in range(cx - half, cx + half + 2):
                dot(x, y, (255, 230, 120) if y < 11 else (255, 140, 50))
    return img


def icon_titan_soundwaves():
    icon_tile(45, (230, 70, 70))
    for y0 in (2, 6, 10):                  # a column of speakers with red-lit drivers
        rect(2, y0, 4, 4, (96, 96, 104), 2)
        rect(3, y0 + 1, 2, 2, (255, 80, 80), 0)
    for k, r in enumerate((4.0, 6.5, 9.0)):
        for a in range(-50, 51, 7):
            x = 5 + r * math.cos(math.radians(a))
            y = 8 + r * math.sin(math.radians(a))
            dot(int(round(x)), int(round(y)), (255, 150, 150) if k < 2 else (230, 70, 70))
    return img


def icon_plasma_cannon():
    icon_tile(46, (230, 70, 70))
    for t in range(7):                     # barrel, pointing up-right
        rect(2 + t, 12 - t, 2, 2, (86, 88, 96), 2)
    for t in range(4):                     # plasma bolt
        dot(10 + t, 5 - t, (255, 120, 100))
        dot(11 + t, 5 - t, (255, 230, 210))
        dot(10 + t, 4 - t, (255, 74, 58))
    return img


def icon_death_screen():
    icon_tile(47, (180, 90, 255))
    rect(3, 4, 10, 8, (30, 30, 34), 2)      # TV
    rnd = random.Random(47)
    for x in range(4, 12):
        for y in range(5, 11):
            v = rnd.randint(150, 250)
            px[x, y] = (v, v, min(255, v + 10), 255)
    for x, y in ((1, 2), (14, 2), (1, 13), (14, 13), (8, 1), (8, 14)):
        dot(x, y, (200, 130, 255))         # flash rays
    return img


def icon_claw_sweep():
    icon_tile(48, (180, 90, 255))
    for k in range(3):                     # three curved blades
        for t in range(9):
            x = 3 + t
            y = 3 + k * 3 + int(round(0.08 * (t - 4) ** 2))
            dot(x, y, (40, 40, 46))
            dot(x, y + 1, (200, 130, 255) if t > 5 else (70, 70, 78))
    return img


def icon_grappling_hook():
    icon_tile(49, (150, 150, 165))
    for t in range(6):                     # the line
        dot(2 + t, 13 - t, (170, 170, 182))
    hook = ["..XX..",                       # a hook on the end of it
            ".X..X.",
            ".X..X.",
            "....X.",
            "X...X.",
            "XX.X..",
            ".XX..."]
    for k, row in enumerate(hook):
        for i, c in enumerate(row):
            if c == "X":
                dot(7 + i, 2 + k, (220, 222, 230))
    return img


def icon_gear_jetpack():
    icon_tile(50, (255, 196, 90))
    rect(4, 2, 3, 8, (78, 80, 88), 3)       # twin tanks with an orange band
    rect(9, 2, 3, 8, (78, 80, 88), 3)
    rect(4, 4, 3, 1, (230, 140, 40), 2)
    rect(9, 4, 3, 1, (230, 140, 40), 2)
    rect(7, 3, 2, 5, (56, 58, 64), 2)
    for cx in (5, 10):                     # little flames
        dot(cx, 10, (255, 230, 120))
        dot(cx + 1, 10, (255, 230, 120))
        dot(cx, 11, (255, 140, 50))
        dot(cx + 1, 12, (255, 140, 50))
    return img


def icon_gear_shoulders():
    icon_tile(51, (255, 196, 90))
    rect(3, 9, 10, 4, (112, 114, 124), 3)   # shoulders
    rect(6, 6, 4, 3, (112, 114, 124), 3)    # neck
    rect(2, 5, 4, 3, (210, 212, 218), 3)    # a camera on one shoulder...
    dot(2, 6, (70, 150, 220))
    rect(11, 4, 3, 4, (24, 24, 28), 2)      # ...a speaker on the other
    dot(12, 5, (200, 202, 210))
    dot(12, 6, (150, 152, 160))
    return img


def icon_gear_plates():
    icon_tile(52, (255, 196, 90))
    for y in range(3, 13):                 # a chest plate with shoulder guards
        half = 5 if y < 6 else 4 if y < 10 else 3
        for x in range(8 - half, 8 + half):
            dot(x, y, (176, 180, 188) if (x + y) % 5 else (140, 144, 152))
    rect(2, 3, 3, 3, (150, 154, 162), 3)
    rect(11, 3, 3, 3, (150, 154, 162), 3)
    for x, y in ((5, 4), (10, 4), (6, 11), (9, 11)):
        dot(x, y, (90, 92, 100))           # rivets
    return img


# ---------------------------------------------------------------- player characters
# Each head/suit is drawn in several passes over the same model: a grey "casing" and "accent" layer that the
# game dyes with the player's chosen colors, then an undyed "detail" layer. Transparent pixels are skipped.

DYE = (238, 238, 238)       # grey base for dyed layers: near white so the chosen color shows true
DYE_SHADE = (196, 196, 196)
DARK = (44, 44, 50)
METAL = (196, 198, 204)


def disc(cx, cy, x0, y0, w, h, radius):
    """Pixels of a w x h face at (x0, y0) within `radius` of (cx, cy) in face coordinates."""
    return [(x0 + i, y0 + j) for i in range(w) for j in range(h) if math.hypot(i + 0.5 - cx, j + 0.5 - cy) <= radius]


def camera_casing():
    new_image(64, 21)
    box(0, 0, 6, 6, 10, DYE, 6)
    rect(0, 13, 32, 1, DYE_SHADE, 3)       # panel seam around the sides
    return img


def camera_accent():
    new_image(64, 22)
    for x0, y0, w, h in ((34, 2, 4, 4), (35, 11, 6, 6), (46, 2, 2, 3)):  # lens glass: normal, big, double
        rect(x0, y0, w, h, DYE, 4)
        rect(x0, y0 + h - 1, w, 1, DYE_SHADE, 2)
    return img


def camera_detail():
    new_image(64, 23)
    rect(10, 10, 6, 6, DARK, 3)            # dark front around the lens
    box(32, 0, 4, 4, 2, DARK, 3)           # lens barrels...
    box(32, 8, 6, 6, 3, DARK, 3)
    box(44, 0, 2, 3, 2, DARK, 3)
    for x0, y0, w, h in ((34, 2, 4, 4), (35, 11, 6, 6), (46, 2, 2, 3)):  # ...with the glass left clear
        for i in range(x0, x0 + w):
            for k in range(y0, y0 + h):
                px[i, k] = (0, 0, 0, 0)
        px[x0, y0] = (255, 255, 255, 255)  # glint
    box(52, 0, 2, 2, 2, (220, 30, 30), 6)  # recording light
    px[54, 2] = (255, 140, 140, 255)
    return img


def speaker_casing():
    new_image(64, 24)
    box(0, 0, 10, 10, 8, DYE, 5)
    return img


def speaker_accent(count, grille):
    new_image(64, 25 + count * 3 + grille)
    fx, fy = 8, 8                          # 10x10 front face
    cones = [(5.0, 5.0, 4.2)] if count == 0 else [(5.0, 2.6, 2.3), (5.0, 7.2, 2.6)]
    for cx, cy, radius in cones:
        for x, y in disc(cx, cy, fx, fy, 10, 10, radius):
            d = math.hypot(x - fx + 0.5 - cx, y - fy + 0.5 - cy) / radius
            if grille == 1:                # mesh
                shade_value = 250 if (x + y) % 2 == 0 else 150
            elif grille == 2:              # bars
                shade_value = 250 if (y - fy) % 2 == 0 else 120
            else:                          # cone: dark dust cap, lighter cone, darker surround
                shade_value = 150 if d < 0.3 else 240 if d < 0.8 else 185
            px[x, y] = shade((shade_value,) * 3, 3)
    return img


def speaker_detail():
    new_image(64, 31)
    fx, fy = 8, 8
    rect(fx, fy, 10, 1, DARK, 2)           # frame around the front
    rect(fx, fy + 9, 10, 1, DARK, 2)
    rect(fx, fy, 1, 10, DARK, 2)
    rect(fx + 9, fy, 1, 10, DARK, 2)
    for x, y in ((fx, fy), (fx + 9, fy), (fx, fy + 9), (fx + 9, fy + 9)):  # screws
        px[x, y] = METAL + (255,)
    return img


def tv_casing():
    new_image(64, 32)
    box(0, 0, 12, 9, 7, DYE, 5)
    return img


def tv_accent():
    new_image(64, 33)
    for y in range(8, 15):                 # 10x7 screen inside a 1px bezel
        rect(8, y, 10, 1, (250, 250, 250) if y % 2 == 0 else (222, 222, 222), 2)
    return img


def tv_detail():
    new_image(64, 34)
    rect(16, 15, 2, 1, METAL, 0)           # knobs under the screen
    box(40, 0, 1, 6, 1, METAL, 6)          # antenna rods
    box(44, 0, 1, 5, 1, METAL, 6)
    rect(41, 0, 1, 1, DARK, 0)             # tips
    rect(45, 0, 1, 1, DARK, 0)
    return img


TV_FACES = {
    1: [(10, 9), (11, 9), (14, 9), (15, 9), (10, 12), (15, 12), (11, 13), (12, 13), (13, 13), (14, 13)],  # smile
    2: [(9, 8), (10, 9), (16, 8), (15, 9), (11, 10), (14, 10), (11, 13), (12, 12), (13, 12), (14, 13)],   # angry
    3: [(10, 9), (11, 9), (10, 10), (11, 10), (14, 9), (15, 9), (14, 10), (15, 10)],                      # eyes
}


def tv_face(face):
    new_image(64, 40 + face)
    if face == 4:                          # static
        for x in range(8, 18):
            for y in range(8, 15):
                v = random.choice((40, 120, 200, 255))
                px[x, y] = (v, v, v, 255)
    else:
        for x, y in TV_FACES[face]:
            px[x, y] = (255, 255, 255, 255)
        if face == 3:                      # pupils
            px[11, 10] = (60, 60, 60, 255)
            px[14, 10] = (60, 60, 60, 255)
    return img


def suit_base():
    new_image(64, 50, 32)
    box(16, 16, 8, 12, 4, DYE, 5)          # jacket
    box(40, 16, 4, 12, 4, DYE, 5)          # sleeves...
    rect(40, 30, 16, 2, (0, 0, 0), 0)      # ...ending above the hands, which stay the player's own
    rect(48, 16, 4, 4, (0, 0, 0), 0)
    box(0, 16, 4, 12, 4, DYE, 5)           # trousers
    for x in range(64):
        for y in range(32):
            if px[x, y][:3] == (0, 0, 0):
                px[x, y] = (0, 0, 0, 0)
    return img


def suit_tie():
    new_image(64, 51, 32)
    rect(23, 20, 2, 8, DYE, 3)
    px[23, 28] = DYE + (255,)
    return img


def suit_detail():
    new_image(64, 52, 32)
    for y in (20, 21):                     # shirt collar either side of the tie
        px[22, y] = (240, 240, 240, 255)
        px[25, y] = (240, 240, 240, 255)
    rect(40, 29, 16, 1, (240, 240, 240), 2)  # shirt cuffs
    rect(0, 30, 16, 2, (18, 18, 18), 2)    # shoes
    rect(8, 16, 4, 4, (18, 18, 18), 2)
    return img


# ---------------------------------------------------------------- particles

def soft_glow():
    new_image(16, 31)
    for x in range(16):
        for y in range(16):
            r = math.hypot(x - 7.5, y - 7.5) / 8.0
            a = max(0.0, 1.0 - r) ** 1.8
            px[x, y] = (255, 255, 255, int(255 * a))
    return img


def ring():
    new_image(32, 32)
    for x in range(32):
        for y in range(32):
            r = math.hypot(x - 15.5, y - 15.5)
            a = max(0.0, 1.0 - abs(r - 12.5) / 3.4)
            px[x, y] = (255, 255, 255, int(255 * a ** 0.9))
    return img


def droplet():
    new_image(8, 33)
    for x in range(8):
        for y in range(8):
            r = math.hypot(x - 3.5, (y - 4.5) * 1.15)
            if r < 3.2 or (y < 3 and abs(x - 3.5) < (y + 1) * 0.6):
                light = 255 if (x, y) in ((2, 3), (2, 4)) else 230
                px[x, y] = (light, light, 255, 230)
    return img


def tv_static(seed):
    new_image(8, 40 + seed)
    for x in range(8):
        for y in range(8):
            if random.random() < 0.7:
                v = random.choice((40, 120, 200, 255))
                px[x, y] = (v, v, v, 255)
    return img


out = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parent.parent / \
    "src/main/resources/assets/skibidi/textures"
outputs = {
    "entity/skibidi_toilet.png": lambda: toilet(7, skibidi_face),
    "entity/gman_toilet.png": gman_toilet,
    "entity/big_skibidi_toilet.png": lambda: toilet(13, big_face),
    "entity/scientist_toilet.png": lambda: toilet(14, scientist_face),
    "entity/skibidi_urinal.png": urinal,
    "entity/flying_skibidi_toilet.png": lambda: toilet(15, flying_face),
    "entity/giant_flying_skibidi_toilet.png": giant_flying_toilet,
    "entity/dj_skibidi_toilet.png": lambda: toilet(17, dj_face, toilet_headphones),
    "entity/spider_toilet.png": lambda: toilet(7, skibidi_face, toilet_spider_leg),
    "entity/jetpack_toilet.png": lambda: toilet(7, jetpack_face, lambda b: toilet_tint(b, 0.52), toilet_jetpack),
    "entity/cameraman.png": cameraman,
    "entity/speakerman.png": speakerman,
    "entity/tv_man.png": tv_man,
    "entity/commander.png": commander,
    "entity/large_cameraman.png": large_cameraman,
    "entity/large_speakerman.png": large_speakerman,
    "entity/large_tv_man.png": large_tv_man,
    "entity/titan_cameraman_2.png": titan_cameraman_2,
    "entity/titan_cameraman_2_glow.png": titan_cameraman_2_glow,
    "entity/titan_speakerman_2.png": titan_speakerman_2,
    "entity/titan_speakerman_2_glow.png": titan_speakerman_2_glow,
    "entity/titan_tv_man_2.png": titan_tv_man_2,
    "entity/titan_tv_man_2_glow.png": titan_tv_man_2_glow,
    "entity/gear.png": gear,
    "entity/camera_strider.png": camera_strider,
    "entity/titan_cameraman.png": titan_cameraman,
    "entity/titan_cameraman_glow.png": titan_cameraman_glow,
    "entity/titan_speakerman.png": titan_speakerman,
    "entity/titan_speakerman_glow.png": titan_speakerman_glow,
    "entity/titan_tv_man.png": titan_tv_man,
    "entity/titan_tv_man_glow.png": titan_tv_man_glow,
    "entity/saint_skibidi_toilet.png": lambda: toilet(19, lambda b: saint_face(b, (255, 255, 255), (60, 110, 200)),
                                                      toilet_halo, lambda b: toilet_trim(b, (214, 172, 56), 4)),
    "entity/saint_skibidi_toilet_demonic.png": lambda: toilet(19, lambda b: saint_face(b, (200, 30, 30), (255, 230, 60)),
                                                              toilet_halo, lambda b: toilet_trim(b, (214, 172, 56), 4)),
    "entity/triple_skibidi_toilet.png": triple,
    "entity/hat_skibidi_toilet.png": lambda: toilet(23, hat_face, toilet_cap),
    "entity/fast_armored_skibidi_toilet.png": fast_armored,
    "item/plunger.png": plunger,
    "player/cameraman_casing.png": camera_casing,
    "player/cameraman_accent.png": camera_accent,
    "player/cameraman_detail.png": camera_detail,
    "player/speakerman_casing.png": speaker_casing,
    "player/speakerman_detail.png": speaker_detail,
    "player/tv_man_casing.png": tv_casing,
    "player/tv_man_accent.png": tv_accent,
    "player/tv_man_detail.png": tv_detail,
    "player/suit_base.png": suit_base,
    "player/suit_tie.png": suit_tie,
    "player/suit_detail.png": suit_detail,
    **{f"player/speakerman_accent_{c}_{g}.png": (lambda c=c, g=g: speaker_accent(c, g)) for c in range(2) for g in range(3)},
    **{f"player/tv_man_face_{f}.png": (lambda f=f: tv_face(f)) for f in range(1, 5)},
    "item/alliance_blaster.png": alliance_blaster,
    "item/stun_rifle.png": stun_rifle,
    "item/gravity_plunger.png": gravity_plunger,
    "item/energy_knife.png": energy_knife,
    "gui/power/holoshield.png": icon_holoshield,
    "gui/power/record_scan.png": icon_record_scan,
    "gui/power/sonic_blast.png": icon_sonic_blast,
    "gui/power/sonic_boots.png": icon_sonic_boots,
    "gui/power/anthem_dance.png": icon_anthem_dance,
    "gui/power/hypnosis.png": icon_hypnosis,
    "gui/power/teleport.png": icon_teleport,
    "gui/power/burning_light.png": icon_burning_light,
    "gui/power/stun_flash.png": icon_stun_flash,
    "gui/power/form_large.png": icon_form_large,
    "gui/power/form_titan.png": icon_form_titan,
    "gui/power/form_titan_2.png": icon_form_titan_2,
    "gui/power/core_beam.png": icon_core_beam,
    "gui/power/titan_jetpack.png": icon_titan_jetpack,
    "gui/power/titan_soundwaves.png": icon_titan_soundwaves,
    "gui/power/plasma_cannon.png": icon_plasma_cannon,
    "gui/power/death_screen.png": icon_death_screen,
    "gui/power/claw_sweep.png": icon_claw_sweep,
    "gui/power/grappling_hook.png": icon_grappling_hook,
    "gui/power/gear_jetpack.png": icon_gear_jetpack,
    "gui/power/gear_shoulders.png": icon_gear_shoulders,
    "gui/power/gear_plates.png": icon_gear_plates,
    "particle/glow.png": soft_glow,
    "particle/ring.png": ring,
    "particle/droplet.png": droplet,
    **{f"particle/static_{i}.png": (lambda i=i: tv_static(i)) for i in range(4)},
}
for name, draw in outputs.items():
    path = out / name
    path.parent.mkdir(parents=True, exist_ok=True)
    draw().save(path)
    print("wrote", path)
