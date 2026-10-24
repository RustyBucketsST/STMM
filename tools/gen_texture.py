"""Generates the mod's textures into src/main/resources/assets/skibidi/textures/.

entity/skibidi_toilet.png, gman_toilet.png, big_skibidi_toilet.png, scientist_toilet.png,
jetpack_toilet.png, skibidi_urinal.png               -- UVs match blockbench/skibidi_toilet.bbmodel (urinal:
                                                        skibidi_urinal.bbmodel) plus the extras in the toilet models
entity/spider_toilet.png                             -- UVs match SpiderToiletModel
entity/cameraman.png, speakerman.png, tv_man.png     -- UVs match blockbench/cameraman.bbmodel etc. (heads) and the
                                                        standard biped layout (suits)
entity/titan_*.png, titan_*_glow.png, gman_toilet.png -- UVs read from the .bbmodel of the same name
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


def skibidi_head():
    # Head 8x8x8 at (0, 0)
    box(0, 0, 8, 8, 8, SKIN)
    rect(8, 0, 8, 8, HAIR)                 # top of head
    rect(0, 8, 32, 2, HAIR)                # hairline around all sides
    rect(24, 8, 8, 5, HAIR)                # back of head
    fx, fy = 8, 8                          # front face origin
    for x in (1, 5):                       # eyebrows (raised, unhinged)
        rect(fx + x, fy + 2, 2, 1, HAIR, 0)
    for x in (1, 5):                       # eyes
        px[fx + x, fy + 3] = (255, 255, 255, 255)
        px[fx + x + 1, fy + 3] = (30, 30, 30, 255)
    rect(fx + 2, fy + 5, 4, 2, (90, 20, 20), 0)   # wide open singing mouth
    rect(fx + 3, fy + 5, 2, 1, (240, 240, 240), 0)  # teeth


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


def skin_extras():
    """Neck 4x6x4 at (108, 96) and ears 1x2x1 at (118, 80), in the skin tone of the head just drawn."""
    pixels = [px[x, y] for x in range(16, 24) for y in range(8)]   # underside of the chin is always bare skin
    skin = tuple(sorted(p[i] for p in pixels)[len(pixels) // 2] for i in range(3))
    dark = tuple(round(c * 0.84) for c in skin)
    f = faces(108, 96, 4, 6, 4)
    for r in sides(f):
        gradient(r, dark, skin, 4)                   # shadowed just under the jaw
    rect(*f["top"], skin, 4)
    rect(*f["bottom"], dark, 2)
    f = faces(118, 80, 1, 2, 1)
    box(118, 80, 1, 2, 1, skin, 4)
    rect(*f["front"], dark, 0)


SEAT_BOXES = [(24, 96, 10, 1, 2), (0, 96, 2, 1, 10), (48, 96, 8, 1, 2)]   # front, sides, back
TANK_LID = (38, 64, 15, 1, 6)
HANDLE = (122, 80, 1, 1, 2)


def trim(color, noise=3):
    """Colors the edges of the seat and tank lid, and the flush handle."""
    for u, v, w, h, d in SEAT_BOXES + [TANK_LID]:
        for r in sides(faces(u, v, w, h, d)):
            rect(*r, color, noise)
    box(*HANDLE, color, noise)


def toilet_body():
    # Tank 14x11x5 at (0, 64), with a seam shadow under the lid that overhangs it
    f = porcelain(0, 64, 14, 11, 5, GLAZE_HI, (218, 222, 228))
    for x, y, w, h in sides(f):
        rect(x, y, w, 1, SEAM, 2)
    # Tank lid 15x1x6 at (38, 64)
    f = porcelain(*TANK_LID)
    x, y, w, h = f["top"]
    rect(x + 1, y + 1, w - 2, 1, (255, 255, 255), 0)      # glossy highlight along the back
    # Seat lid 12x12x1 at (0, 108), standing open against the tank: an oval with a raised ring round its
    # underside and four rubber bumpers, rounded off at the top by 10x1x1 (26, 108) and 7x1x1 (48, 108)
    f = porcelain(0, 108, 12, 12, 1, (248, 249, 251), (226, 230, 235))
    x, y, w, h = f["front"]
    for k in range(h):
        for i in range(w):
            r = math.hypot((i - (w - 1) / 2) / (w / 2), (k - (h - 1) / 2) / (h / 2))
            if 0.62 < r < 0.86:
                px[x + i, y + k] = shade((230, 233, 237), 2)     # the ring
            elif r >= 1.0:
                px[x + i, y + k] = shade((214, 218, 224), 2)     # the corners curve away
    for i, k in ((2, 2), (w - 3, 2), (2, h - 3), (w - 3, h - 3)):
        px[x + i, y + k] = (176, 180, 188, 255)                  # bumpers
    rect(x + 3, y + 3, 1, 3, (255, 255, 255), 0)                 # shine
    porcelain(26, 108, 10, 1, 1, (248, 249, 251), (240, 242, 245))
    porcelain(48, 108, 7, 1, 1, (248, 249, 251), (240, 242, 245))
    # Bowl 12x3x12 at (80, 64): its top is the inside of the bowl, seen through the 8x9 hole in the seat
    f = porcelain(80, 64, 12, 3, 12, (246, 248, 250), (220, 224, 230))
    x, y, w, h = f["top"]
    rect(x + 2, y + 2, 8, 9, (204, 210, 218), 2)          # sloping inner wall
    rect(x + 3, y + 3, 6, 7, (186, 194, 204), 2)
    rect(x + 4, y + 4, 4, 5, WATER, 6)
    rect(x + 5, y + 5, 2, 3, WATER_DEEP, 6)
    px[x + 4, y + 4] = (150, 196, 232, 255)                # glint on the water
    x, y, w, h = f["front"]
    px[x + 1, y] = (255, 255, 255, 255)                    # shine on the rim
    porcelain(68, 96, 10, 3, 1, (246, 248, 250), (220, 224, 230))   # rounded front 10x3x1
    # Seat 1px thick: front 10x1x2 at (24, 96), sides 2x1x10 at (0, 96), back 8x1x2 at (48, 96)
    for u, v, w, h, d in SEAT_BOXES:
        f = faces(u, v, w, h, d)
        rect(*f["top"], (252, 252, 253), 1)
        rect(*f["bottom"], UNDERSIDE, 2)
    trim(SEAM, 2)
    box(*HANDLE, CHROME, 6)
    px[123, 82] = (236, 240, 246, 255)
    # Bowl base 10x3x13 at (0, 80) and its front 8x3x1 at (90, 96): narrower and in the bowl's shadow
    porcelain(0, 80, 10, 3, 13, (228, 232, 237), (206, 211, 217))
    porcelain(90, 96, 8, 3, 1, (228, 232, 237), (206, 211, 217))
    # Pedestal 6x5x10 at (46, 80) and foot 8x1x12 at (78, 80)
    porcelain(46, 80, 6, 5, 10, (236, 239, 242), (210, 215, 221))
    porcelain(78, 80, 8, 1, 12, (204, 209, 215), (204, 209, 215))


def head8(skin, hair, face, colors=None, top=True, side_rows=2, back_rows=5):
    """An 8x8x8 head at (0, 0): skin, hair over the top, sides and back, and an 8x8 pixel-art face, where
    H is hair, s skin, d shade, D dark lines, b brow, W eye white, p pupil, t teeth, m mouth, l lip."""
    box(0, 0, 8, 8, 8, skin)
    if top:
        rect(8, 0, 8, 8, hair)
    if side_rows:
        rect(0, 8, 32, side_rows, hair)
    if back_rows:
        rect(24, 8, 8, back_rows, hair)
    palette = {"H": hair, "s": skin, "d": tuple(round(c * 0.88) for c in skin),
               "D": tuple(round(c * 0.66) for c in skin), "b": tuple(round(c * 0.8) for c in hair),
               "W": (240, 240, 236), "p": (30, 30, 30), "t": (240, 238, 228), "m": (80, 18, 18),
               "l": (200, 90, 90), **(colors or {})}
    for k, row in enumerate(face):
        for i, ch in enumerate(row):
            px[8 + i, 8 + k] = shade(palette[ch], 4 if ch in "sdH" else 1)


def big_head():
    # The Giant Skibidi Toilet: short dark hair, raised worried brows, wide staring eyes, a huge toothy grin.
    head8((214, 170, 130), (52, 38, 30), ["HHHHHHHH", "HssssssH", "sbbssbbs", "sWpssWps",
                                          "sssdDsss", "sDttttDs", "ssDmmDss", "ssssssss"])


def scientist_head():
    # The Chief Scientist: bald, white hair round the sides and back, round glasses, a kindly smile.
    hair = (236, 236, 240)
    head8((230, 200, 180), hair, ["ssssssss", "hsddddsh", "sFFssFFs", "FgpFFpgF",
                                  "sFFddFFs", "sDssssDs", "ssDDDDss", "ssssssss"],
          {"h": hair, "F": (40, 40, 46), "g": (190, 215, 235)}, top=False, side_rows=0, back_rows=0)
    rect(0, 9, 8, 4, hair, 6)               # white hair over the ears
    rect(16, 9, 8, 4, hair, 6)
    rect(24, 9, 8, 6, hair, 6)              # and round the back


def jetpack_head():
    # Male_07 as the Jetpack Toilet: dark hair, thick brows, staring eyes, a snarl, stubble.
    head8((198, 152, 118), (30, 26, 24), ["HHHHHHHH", "HHssssHH", "bbbssbbb", "sWpsspWs",
                                          "sssdDsss", "sdDttDds", "dddmmddd", "dddddddd"],
          {"d": (150, 120, 96)}, side_rows=3)


def flying_head():
    # The Normal Flying Skibidi Toilet: bald, dark-skinned, scared, eyes darting to the side.
    head8((120, 86, 66), (120, 86, 66), ["ssssssss", "sdsddsds", "ssbssbss", "spWsspWs",
                                         "sssdDsss", "ssssssss", "ssdmmdss", "ssssssss"],
          {"b": (60, 40, 30)}, side_rows=0, back_rows=0)


def saint_face(eye_white, pupil):
    # The Saint: a young man with short dark hair and a wide, toothy grin.
    head8((176, 128, 92), (40, 28, 22), ["HHHHHHHH", "HssssssH", "sbbssbbs", "sWpssWps",
                                         "sssdDsss", "DttttttD", "sDmmmmDs", "ssssssss"],
          {"W": eye_white, "p": pupil})


def hat_head():
    # Male_07 as the Hat Toilet: staring eyes, a mustache and goatee, the tip of his tongue showing.
    head8((190, 142, 108), (40, 30, 26), ["HHHHHHHH", "HssssssH", "sbbssbbs", "sWpsspWs",
                                          "sssdDsss", "ssHHHHss", "sssmlsss", "ssHHHHss"], back_rows=4)


def spider_legs():
    # Leg 16x2x2 at (26, 50), used for all eight legs
    box(26, 50, 16, 2, 2, (44, 42, 46), 6)
    for x in range(28, 62, 6):             # joints
        rect(x, 52, 1, 2, (24, 22, 26), 0)


def jetpack():
    # Jetpacks 4x10x4 at (32, 0) with nozzles 3x2x3 at (48, 0), front armour 10x4x1 at (64, 0), side armour
    # 1x3x8 at (64, 5), flusher cover 2x3x4 at (88, 0)
    f = faces(32, 0, 4, 10, 4)
    for r in sides(f):
        gradient(r, (88, 90, 96), (62, 64, 70), 4)
        x, y, w, h = r
        for k in (2, 5, 8):
            rect(x, y + k, w, 1, (46, 48, 52), 0)          # segment bands
    rect(*f["top"], (100, 104, 110), 3)
    box(48, 0, 3, 2, 3, (40, 40, 44), 3)
    rect(*faces(48, 0, 3, 2, 3)["bottom"], (230, 120, 40), 6)
    box(64, 0, 10, 4, 1, (96, 98, 104), 5)
    x, y, w, h = faces(64, 0, 10, 4, 1)["front"]
    for i in (0, w - 1):
        for k in (0, h - 1):
            px[x + i, y + k] = (150, 152, 160, 255)         # rivets
    box(64, 5, 1, 3, 8, (90, 92, 98), 5)
    box(88, 0, 2, 3, 4, (100, 102, 108), 4)


def tint_body(factor):
    """Darkens the toilet body (the bottom half of a toilet texture) to grey, leaving the neck and ears."""
    keep = [(108, 96, 16, 10), (118, 80, 4, 3)]
    for x in range(128):
        for y in range(64, 128):
            if any(kx <= x < kx + kw and ky <= y < ky + kh for kx, ky, kw, kh in keep):
                continue
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = (round(r * factor), round(g * factor), round(b * factor * 1.04), a)


def jetpack_toilet():
    new_image(128, 7)
    jetpack_head()
    skin_extras()
    toilet_body()
    tint_body(0.52)                         # an armoured grey toilet
    jetpack()
    return img


DJ_SKIN = (190, 132, 92)
SHADES = (18, 18, 22)


def dj_head():
    # Head 8x8x8 at (0, 0): short black hair, dark shades, a cool little smile.
    box(0, 0, 8, 8, 8, DJ_SKIN)
    rect(8, 0, 8, 8, (25, 22, 22))
    rect(0, 8, 32, 1, (25, 22, 22))
    rect(24, 8, 8, 4, (25, 22, 22))
    fx, fy = 8, 8
    rect(fx, fy + 3, 8, 1, SHADES, 0)              # shades across the eyes
    rect(fx + 1, fy + 3, 2, 2, SHADES, 0)
    rect(fx + 5, fy + 3, 2, 2, SHADES, 0)
    px[fx + 1, fy + 3] = (200, 220, 255, 255)      # glint
    px[fx + 5, fy + 3] = (200, 220, 255, 255)
    rect(fx + 2, fy + 6, 4, 1, (110, 40, 40), 0)   # smile
    px[fx + 2, fy + 5] = (110, 40, 40, 255)
    px[fx + 5, fy + 5] = (110, 40, 40, 255)


def headphones():
    # The DJ's grey beanie 9x4x9 at (64, 0) and pink headphones: band 10x1x2 at (32, 0), band sides 1x3x1 at
    # (40, 4), cups 1x4x3 at (32, 4)
    f = faces(64, 0, 9, 4, 9)
    for r in sides(f):
        x, y, w, h = r
        rect(x, y, w, h, (118, 118, 122), 4)
        for i in range(0, w, 2):
            rect(x + i, y, 1, h - 1, (100, 100, 104), 2)    # knit ribs
        rect(x, y + h - 1, w, 1, (134, 134, 138), 3)        # turned-up edge
    rect(*f["top"], (112, 112, 116), 4)
    pink = (232, 110, 170)
    box(32, 0, 10, 1, 2, pink, 4)
    box(40, 4, 1, 3, 1, pink, 4)
    box(32, 4, 1, 4, 3, (210, 90, 150), 4)
    rect(*faces(32, 4, 1, 4, 3)["right"], (40, 40, 44), 2)  # cushion against the head


URINAL_HAIR = (60, 40, 28)


def urinal_head():
    # Head 8x8x8 at (0, 0): neat dark hair and a mustache, mouth open mid-song.
    box(0, 0, 8, 8, 8, SKIN)
    rect(8, 0, 8, 8, URINAL_HAIR)
    rect(0, 8, 32, 1, URINAL_HAIR)
    rect(24, 8, 8, 5, URINAL_HAIR)
    fx, fy = 8, 8
    for x in (1, 5):
        rect(fx + x, fy + 2, 2, 1, URINAL_HAIR, 0)
        px[fx + x, fy + 3] = (255, 255, 255, 255)
        px[fx + x + 1, fy + 3] = (30, 30, 30, 255)
    rect(fx + 2, fy + 5, 4, 1, URINAL_HAIR, 0)     # mustache
    rect(fx + 3, fy + 6, 2, 2, (90, 20, 20), 0)    # singing mouth


def urinal():
    new_image(128, 18)
    urinal_head()
    skin_extras()
    # Shell 10x21x3 at (0, 64): the tall back of a floor-standing urinal, with a chrome spreader at the top
    f = porcelain(0, 64, 10, 21, 3, GLAZE_HI, (214, 219, 225))
    x, y, w, h = f["front"]
    rect(x + 1, y + 4, w - 2, h - 5, (232, 235, 239), 2)  # the recessed back of the bowl
    rect(x + 3, y + 1, 4, 1, CHROME, 4)
    porcelain(0, 88, 8, 1, 3)                              # rounded top 8x1x3
    porcelain(26, 64, 1, 17, 3)                            # side wings 1x17x3
    # Bowl 8x5x7 at (34, 64): the top is the opening the head comes out of
    f = porcelain(34, 64, 8, 5, 7, (246, 248, 250), (222, 226, 232))
    x, y, w, h = f["top"]
    rect(x + 1, y + 1, w - 2, h - 2, (204, 210, 218), 2)
    rect(x + 2, y + 1, w - 4, h - 3, (70, 120, 160), 6)
    rect(x + 3, y + 2, w - 6, 1, (40, 80, 120), 6)
    porcelain(34, 76, 6, 3, 5, (222, 226, 232), (206, 211, 217))   # lower bowl 6x3x5
    # Flush pipe 2x5x2 at (80, 64) and valve 3x2x3 at (88, 64)
    box(80, 64, 2, 5, 2, CHROME, 6)
    box(88, 64, 3, 2, 3, (170, 177, 188), 6)
    rect(88, 67, 12, 1, (220, 226, 234), 3)
    return img


HALO_GOLD = (240, 200, 70)


def saint_head():
    saint_face((255, 255, 255), (60, 110, 200))


def saint_head_demonic():
    saint_face((200, 30, 30), (255, 230, 60))


def halo():
    # Bars 8x1x1 at (32, 0) and 1x1x6 at (32, 4)
    box(32, 0, 8, 1, 1, HALO_GOLD, 6)
    box(32, 4, 1, 1, 6, HALO_GOLD, 6)


def gold_trim():
    trim((214, 172, 56), 4)   # gold seat and tank lid edges and a gold flush handle


def head_strip(head, seed):
    """Draws a head on a scratch image and returns its 32x16 texture strip, to place anywhere."""
    global img, px
    saved = img, px
    new_image(64, seed)
    head()
    strip = img.crop((0, 0, 32, 16))
    img, px = saved
    return strip


TRIPLE_SKIN = (150, 106, 78)


def triple_face(face):
    return lambda: head8(TRIPLE_SKIN, TRIPLE_SKIN, face, {"b": (70, 48, 36)}, side_rows=0, back_rows=0)


def triple():
    # Two toilets joined on a platform and a third behind them (triple_skibidi_toilet.bbmodel), with three
    # bald heads grinning ear to ear: the top one at u 0 (with horns), the others' strips at u 64 and 96
    b = bb_boxes("triple_skibidi_toilet")
    new_image(128, 20)
    triple_face(["ssssssss", "sdssssds", "sbbssbbs", "sWpssWps", "sssdDsss", "DttttttD", "sDmmmmDs", "ssssssss"])()
    img.paste(head_strip(triple_face(["ssssssss", "ssssssss", "sbbssbbs", "WpWsWpWs", "sssdDsss",
                                      "sDttttDs", "ssDmmDss", "ssssssss"]), 21), (64, 0))
    img.paste(head_strip(triple_face(["ssssssss", "ssssssss", "bbsssbbs", "sWpssWps", "sssdDsss",
                                      "DttttttD", "ssDmmDss", "ssssssss"]), 22), (96, 0))
    for name in ("head_neck", "head_left_neck", "head_ear_right"):
        u, v, w, h, d = b[name]
        box(u, v, w, h, d, TRIPLE_SKIN, 4)
        for r in sides(faces(u, v, w, h, d)):
            x, y, rw, rh = r
            rect(x, y, rw, 1, tuple(round(c * 0.82) for c in TRIPLE_SKIN), 2)   # under the jaw
    f = faces(*b["horn_right"])
    for r in sides(f):
        gradient(r, (60, 54, 50), (110, 100, 92), 3)                            # dark horns, lighter at the base
    rect(*f["top"], (44, 40, 38), 2)
    # Porcelain, with the inside of each bowl seen from above and oval lids like the regular toilets
    for name in ("pedestal_right", "bowl_lower_right", "bowl_right", "lid_top_right", "tank", "tank_lid",
                 "back_pedestal", "back_bowl", "back_tank"):
        porcelain(*b[name])
    for name in ("bowl_right", "back_bowl"):
        x, y, w, h = faces(*b[name])["top"]
        rect(x + 2, y + 2, w - 4, h - 4, (204, 210, 218), 2)
        rect(x + 3, y + 3, w - 6, h - 6, WATER, 6)
    f = porcelain(*b["lid_right"], (248, 249, 251), (226, 230, 235))
    x, y, w, h = f["front"]
    for k in range(h):
        for i in range(w):
            r = math.hypot((i - (w - 1) / 2) / (w / 2), (k - (h - 1) / 2) / (h / 2))
            if 0.62 < r < 0.86:
                px[x + i, y + k] = shade((230, 233, 237), 2)
            elif r >= 1.0:
                px[x + i, y + k] = shade((214, 218, 224), 2)
    f = faces(*b["platform"])
    for r in f.values():
        rect(*r, (120, 124, 130), 4)                                            # a grey stone platform
    return img


def soft_cap():
    # A grey soldier's soft cap: crown 9x3x9 at (64, 0) with a darker band, visor 8x1x2 at (64, 12)
    f = faces(64, 0, 9, 3, 9)
    for r in sides(f):
        x, y, w, h = r
        rect(x, y, w, h, (108, 112, 102), 4)
        rect(x, y + h - 1, w, 1, (82, 86, 78), 3)
    rect(*f["top"], (114, 118, 108), 4)
    box(64, 12, 8, 1, 2, (72, 74, 68), 3)


def hat_toilet():
    new_image(128, 23)
    hat_head()
    skin_extras()
    toilet_body()
    soft_cap()
    return img


STEEL = (152, 156, 162)


def fast_armored():
    # The Fast Armored Skibidi Toilet (fast_armored_skibidi_toilet.bbmodel): a brushed-steel prison toilet with
    # a sink on top, a head in a black helmet with blank white eyes, a dark plate behind with six rockets
    b = bb_boxes("fast_armored_skibidi_toilet")
    new_image(128, 24, 64)
    skin = (186, 140, 108)
    u, v, w, h, d = b["head"]
    f = faces(u, v, w, h, d)
    for r in f.values():
        rect(*r, skin, 4)
    rect(*f["top"], (30, 26, 24), 3)
    pixel_art(*f["front"][:2], ["HHHHHHHH", "HHHHHHHH", "HssssssH", "sWWssWWs",
                                "sssdDsss", "sDttttDs", "ssDmmDss", "ssssssss"],
              {"H": (30, 26, 24), "s": skin, "d": (164, 122, 94), "D": (120, 88, 66), "W": (250, 250, 250),
               "t": (236, 232, 220), "m": (70, 16, 16)})
    for key in ("right", "left"):
        x, y, rw, rh = f[key]
        rect(x + rw // 2, y + 3, 1, rh - 3, (50, 44, 36), 0)   # helmet strap down to the chin
    rect(*f["bottom"], (164, 122, 94), 2)
    f = faces(*b["helmet"])
    for r in sides(f):
        gradient(r, (40, 40, 44), (22, 22, 26), 3)
    rect(*f["top"], (46, 46, 50), 3)
    rect(*f["bottom"], (16, 16, 18), 0)
    f = faces(*b["neck"])
    for r in sides(f):
        gradient(r, (150, 112, 86), skin, 3)
    for name in ("base", "bowl", "rim", "back_unit", "sink", "faucet"):
        f = faces(*b[name])
        for r in sides(f):
            x, y, rw, rh = r
            gradient(r, tuple(c + 10 for c in STEEL), tuple(c - 22 for c in STEEL), 3)
            for k in range(1, rh, 3):
                rect(x, y + k, rw, 1, tuple(c - 8 for c in STEEL), 2)   # brushed lines
        rect(*f["top"], tuple(c + 16 for c in STEEL), 3)
        rect(*f["bottom"], (90, 92, 98), 2)
    x, y, w, h = faces(*b["rim"])["top"]
    rect(x + 2, y + 2, w - 4, h - 4, (70, 74, 82), 2)          # the inside of the bowl
    rect(x + 3, y + 3, w - 6, h - 6, (60, 100, 140), 6)
    x, y, w, h = faces(*b["sink"])["top"]
    rect(x + 1, y + 1, w - 2, h - 2, (100, 104, 110), 2)       # basin
    px[x + w // 2, y + h // 2] = (40, 40, 44, 255)            # drain
    f = faces(*b["plate"])
    for r in f.values():
        rect(*r, (62, 64, 70), 3)
    x, y, w, h = f["back"]
    for k in range(0, h, 7):
        rect(x, y + k, w, 1, (48, 50, 54), 0)                  # panel seams
    for i in range(1, w, 4):
        for k in (1, h - 2):
            px[x + i, y + k] = (104, 106, 112, 255)            # rivets
    f = faces(*b["rocket0_right"])
    for r in sides(f):
        gradient(r, (124, 126, 132), (90, 92, 98), 3)
        x, y, rw, rh = r
        rect(x, y + rh - 1, rw, 1, (60, 62, 66), 0)
    for key in ("top", "bottom"):
        x, y, rw, rh = f[key]
        rect(x, y, rw, rh, (110, 112, 118), 3)
        for k in range(1, rh, 2):
            rect(x, y + k, rw, 1, (80, 82, 88), 0)             # bands along the rocket
    x, y, w, h = f["back"]
    rect(x, y, w, h, (40, 40, 44), 0)                          # nozzle...
    rect(x + 1, y + 1, w - 2, h - 2, (250, 170, 60), 6)        # ...glowing hot
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


def toilet(seed, head, *extras):
    new_image(128, seed)
    head()
    skin_extras()
    toilet_body()
    for extra in extras:
        extra()
    return img


# ---------------------------------------------------------------- alliance

SUIT = (38, 40, 46)
LAPEL = (28, 30, 34)
SHIRT = (235, 235, 235)
TIE = (18, 18, 22)
SHOE = (16, 16, 16)
HAND = (224, 172, 120)


def suit(suit_color=SUIT, tie_color=TIE):
    # Standard biped layout in the top half; left limbs mirror the right ones.
    # Leg 4x12x4 at (0, 16)
    box(0, 16, 4, 12, 4, suit_color, 4)
    rect(0, 30, 16, 2, SHOE, 2)            # shoes
    rect(8, 16, 4, 4, SHOE, 2)             # soles

    # Body 8x12x4 at (16, 16)
    box(16, 16, 8, 12, 4, suit_color, 4)
    fx, fy = 20, 20                        # front of the jacket
    rect(fx + 2, fy, 4, 2, SHIRT, 2)       # shirt collar
    rect(fx + 3, fy, 2, 8, tie_color, 2)   # tie
    rect(fx + 3, fy + 8, 1, 1, tie_color, 2)  # tie tip
    for y in (0, 1):                       # lapels
        rect(fx + 1, fy + y, 1, 1, LAPEL, 0)
        rect(fx + 6, fy + y, 1, 1, LAPEL, 0)

    # Arm 4x12x4 at (40, 16)
    box(40, 16, 4, 12, 4, suit_color, 4)
    rect(40, 29, 16, 1, SHIRT, 2)          # shirt cuffs
    rect(40, 30, 16, 2, HAND, 4)           # hands
    rect(48, 16, 4, 4, HAND, 4)            # palms


CAMERA_BODY = (178, 182, 190)
CAMERA_DARK = (52, 54, 60)
GLASS = (26, 44, 88)
MOUNT = (62, 64, 70)


def mount(u, v):
    # Neck mount 4x1x4: the bracket the device sits on
    box(u, v, 4, 1, 4, MOUNT, 4)


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


def cameraman_head():
    # Housing 6x6x10 at (0, 32): a CCTV camera, light grey with a seam down each side and a dark front plate
    f = faces(0, 32, 6, 6, 10)
    rect(*f["top"], (196, 200, 207), 3)
    rect(*f["bottom"], (128, 132, 140), 3)
    for r in sides(f):
        gradient(r, (190, 194, 201), (150, 155, 163), 3)
    for key in ("right", "left"):
        x, y, w, h = f[key]
        rect(x, y + 1, w, 1, (140, 145, 152), 2)          # seam
        for i in range(3):                                 # vent slots toward the back
            vx = x + 1 + 2 * i if key == "right" else x + w - 2 - 2 * i
            rect(vx, y + 3, 1, 2, (96, 100, 108), 0)
    x, y, w, h = f["front"]
    rect(x, y, w, h, CAMERA_DARK, 3)
    px[x + w - 1, y] = (230, 30, 30, 255)                 # red recording light
    x, y, w, h = f["back"]
    rect(x + 1, y + 1, w - 2, h - 2, (160, 165, 172), 2)  # back cover
    # Sun hood 8x1x12 at (0, 48) and its side skirts 1x2x6 at (40, 48)
    f = faces(0, 48, 8, 1, 12)
    rect(*f["top"], (206, 210, 216), 3)
    x, y, w, h = f["top"]
    rect(x + 3, y, 2, h, (214, 218, 224), 2)             # raised ridge down the middle
    rect(*f["bottom"], (70, 72, 78), 3)                    # shadow under the hood
    for r in sides(f):
        rect(*r, (164, 168, 176), 2)
    box(40, 48, 1, 2, 6, (164, 168, 176), 3)
    # Lens 4x4x2 at (32, 32): black barrel, glass on the front
    f = faces(32, 32, 4, 4, 2)
    box(32, 32, 4, 4, 2, (34, 34, 38), 3)
    lens_glass(*f["front"])
    # Cable socket 2x2x1 at (32, 40)
    box(32, 40, 2, 2, 1, (24, 24, 28), 2)
    mount(44, 32)


SPEAKER_BODY = (28, 28, 31)


def speaker_cone(i, k, n=6):
    """Color of pixel (i, k) of an n x n round speaker: dark surround, grey cone, dark dust cap."""
    r = math.hypot(i - (n - 1) / 2, k - (n - 1) / 2)
    if r <= n * 0.18:
        return (44, 44, 48)
    if r <= n * 0.4:
        return (128 - int(r * 10), 130 - int(r * 10), 138 - int(r * 10))
    return (70, 70, 76)


def speakerman_head():
    # Cabinet 8x10x7 at (0, 32): black, with a slightly lighter bevel round the front and a port on the back
    f = faces(0, 32, 8, 10, 7)
    for key, r in f.items():
        rect(*r, SPEAKER_BODY, 3)
    x, y, w, h = f["front"]
    for i in range(w):
        px[x + i, y] = shade((46, 46, 50), 2)
        px[x + i, y + h - 1] = shade((40, 40, 44), 2)
    for k in range(h):
        px[x, y + k] = shade((42, 42, 46), 2)
        px[x + w - 1, y + k] = shade((42, 42, 46), 2)
    x, y, w, h = f["back"]
    rect(x + 2, y + 6, 4, 2, (60, 60, 66), 2)            # terminal plate...
    px[x + 3, y + 7] = (190, 40, 40, 255)                 # ...with red and black terminals
    px[x + 4, y + 7] = (14, 14, 14, 255)
    rect(x + 3, y + 2, 2, 2, (10, 10, 12), 0)             # bass port
    # Woofer, a 6x6 octagon standing proud of the cabinet front: middle 4x6x1 at (30, 32), sides 1x4x1 at (40, 32)
    box(30, 32, 4, 6, 1, (50, 50, 56), 2)
    box(40, 32, 1, 4, 1, (50, 50, 56), 2)
    x, y = faces(30, 32, 4, 6, 1)["front"][:2]
    for i in range(4):
        for k in range(6):
            px[x + i, y + k] = shade(speaker_cone(i + 1, k), 2)
    x, y = faces(40, 32, 1, 4, 1)["front"][:2]
    for k in range(4):
        px[x, y + k] = shade(speaker_cone(0, k + 1), 2)   # mirrored for the left side, which is symmetric
    f = faces(44, 32, 2, 2, 1)
    box(44, 32, 2, 2, 1, (50, 50, 56), 2)
    x, y = f["front"][:2]
    rect(x, y, 2, 2, (168, 170, 178), 3)                 # silver dome
    px[x, y] = (220, 222, 228, 255)
    mount(48, 32)


TV_CASE = (58, 58, 64)
TV_BEVEL = (80, 80, 88)


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


def tv_man_head():
    # Body 12x9x4 at (0, 32): a CRT TV; the screen is the front face, seen through the bezel
    f = faces(0, 32, 12, 9, 4)
    for key, r in f.items():
        rect(*r, TV_CASE, 3)
    x, y, w, h = f["top"]
    for i in range(2, w - 2, 2):
        rect(x + i, y + 1, 1, h - 2, (40, 40, 44), 0)    # vent slots
    x, y, w, h = f["front"]
    tv_screen(x + 1, y + 1, w - 2, h - 3, 4)
    # Picture tube 8x7x3 at (32, 32), tapering out the back
    f = faces(32, 32, 8, 7, 3)
    for key, r in f.items():
        rect(*r, (50, 50, 56), 3)
    x, y, w, h = f["back"]
    for k in range(1, h - 1, 2):
        rect(x + 1, y + k, w - 2, 1, (36, 36, 40), 0)    # back vents
    # Bezel: top 12x1x1 at (0, 46), bottom 12x2x1 at (0, 49), sides 1x6x1 at (26, 46)
    for u, v, w, h in ((0, 46, 12, 1), (0, 49, 12, 2), (26, 46, 1, 6)):
        f = faces(u, v, w, h, 1)
        for key, r in f.items():
            rect(*r, TV_CASE, 3)
        rect(*f["front"], TV_BEVEL, 3)
    x, y, w, h = faces(0, 49, 12, 2, 1)["front"]
    rect(x + 5, y + 1, 2, 1, (150, 152, 160), 2)         # badge
    rect(x + 8, y + 1, 1, 1, (40, 40, 44), 0)             # buttons
    rect(x + 9, y + 1, 1, 1, (40, 40, 44), 0)
    px[x + 10, y + 1] = (60, 220, 90, 255)                 # power light
    mount(32, 46)


def alliance(seed, head):
    new_image(64, seed)
    suit()
    head()
    return img


def large_cameraman():
    # The Large Cameraman (large_cameraman.bbmodel): a long black coat over a white shirt and black tie,
    # black gloves, and a black camcorder for a head
    b = bb_boxes("large_cameraman")
    new_image(64, 9)
    coat, coat_dark = (30, 30, 34), (22, 22, 26)
    box(*b["mount"], (44, 44, 48), 3)
    f = faces(*b["camcorder"])
    for r in sides(f):
        gradient(r, (40, 40, 44), (24, 24, 28), 3)
    rect(*f["top"], (46, 46, 50), 3)
    x, y, w, h = f["right"]
    rect(x + 1, y + 2, w - 2, 1, (54, 54, 60), 0)           # seam
    x, y, w, h = f["front"]
    px[x + w - 1, y] = (230, 30, 30, 255)                   # red recording light
    box(*b["lens"], (20, 20, 24), 2)
    lens_glass(*faces(*b["lens"])["front"])
    for name in ("handle", "handle_front", "handle_back", "viewfinder"):
        box(*b[name], (26, 26, 30), 2)
    f = faces(*b["torso"])
    for r in sides(f):
        rect(*r, coat, 3)
    x, y, w, h = f["front"]
    rect(x + 2, y, 4, 3, SHIRT, 2)                          # shirt in the open collar...
    rect(x + 3, y, 2, 9, TIE, 2)                            # ...and a black tie
    rect(x + 1, y, 1, 4, coat_dark, 0)                      # lapels
    rect(x + 6, y, 1, 4, coat_dark, 0)
    for name in ("coat_back", "coat_side_right", "coat_front_right"):
        f = faces(*b[name])
        for r in sides(f):
            gradient(r, coat, coat_dark, 3)
    f = faces(*b["arm_right"])
    for r in sides(f):
        x, y, w, h = r
        rect(x, y, w, h - 3, coat, 3)
        rect(x, y + h - 4, w, 1, SHIRT, 2)                  # shirt cuff
        rect(x, y + h - 3, w, 3, (40, 40, 44), 3)           # black gloves
    rect(*f["top"], coat, 3)
    rect(*f["bottom"], (40, 40, 44), 3)
    f = faces(*b["leg_right"])
    for r in sides(f):
        x, y, w, h = r
        rect(x, y, w, h, (26, 26, 30), 3)
        rect(x, y + h - 2, w, 2, SHOE, 2)
    rect(*f["bottom"], SHOE, 2)
    return img


def large_speakerman():
    # The Large Speakerman (large_speakerman.bbmodel): a white dress shirt and dark red tie, black trousers and
    # gloves, and a tall black speaker for a head: a big woofer and a column of small drivers
    b = bb_boxes("large_speakerman")
    new_image(64, 10)
    shirt, shirt_shade = (232, 232, 228), (200, 200, 196)
    box(*b["mount"], (40, 40, 44), 3)
    f = faces(*b["cabinet"])
    for r in f.values():
        rect(*r, SPEAKER_BODY, 3)
    x, y, w, h = f["front"]
    for i in range(w):
        px[x + i, y] = shade((46, 46, 50), 2)
    px[x + 5, y + 1] = (160, 162, 170, 255)                 # tweeter
    for k in (3, 5, 7):
        rect(x + 5, y + k, 2, 1, (70, 70, 76), 0)           # small drivers down the side
        px[x + 6, y + k] = (110, 112, 120, 255)
    box(*b["woofer"], (50, 50, 56), 2)
    x, y, w, h = faces(*b["woofer"])["front"]
    for i in range(w):
        for k in range(h):
            px[x + i, y + k] = shade(speaker_cone(i, k, w), 2)
    f = faces(*b["torso"])
    for r in sides(f):
        gradient(r, shirt, shirt_shade, 3)
        x, y, w, h = r
        rect(x, y + h - 1, w, 1, (24, 24, 26), 0)           # belt
    x, y, w, h = f["front"]
    rect(x + 3, y, 2, 1, (90, 14, 18), 0)                   # tie knot...
    rect(x + 3, y + 1, 2, 8, (120, 22, 28), 2)              # ...and dark red tie
    px[x + 2, y] = (210, 210, 206, 255)                     # collar points
    px[x + 5, y] = (210, 210, 206, 255)
    f = faces(*b["arm_right"])
    for r in sides(f):
        x, y, w, h = r
        gradient((x, y, w, h - 3), shirt, shirt_shade, 3)
        rect(x, y + h - 3, w, 3, (34, 34, 38), 3)           # black gloves
    rect(*f["top"], shirt, 3)
    rect(*f["bottom"], (34, 34, 38), 3)
    f = faces(*b["leg_right"])
    for r in sides(f):
        x, y, w, h = r
        rect(x, y, w, h, (24, 24, 28), 3)
        rect(x, y + h - 2, w, 2, SHOE, 2)
    rect(*f["bottom"], SHOE, 2)
    return img


def crt_static(r, rnd, bright=(150, 240)):
    """A TV screen full of white static, every other row a little darker like scan lines."""
    x, y, w, h = r
    for i in range(w):
        for k in range(h):
            v = rnd.randint(*bright) * (0.88 if k % 2 else 1.0)
            px[x + i, y + k] = (int(v), int(v), min(255, int(v * 1.04)), 255)


def large_tv_man():
    # Large TV Man 1.0 (large_tv_man.bbmodel): a big black CRT showing static on a bracket and a thin pipe, a black
    # suit with a purple tie, black gloves, and four little static TVs on thin metal tendrils from his back
    b = bb_boxes("large_tv_man")
    new_image(64, 11)
    rnd = random.Random(45)
    plastic, plastic_dark = (36, 36, 40), (24, 24, 28)
    suit_black, suit_dark = (28, 28, 32), (20, 20, 24)
    purple = (118, 52, 186)
    metal = (112, 114, 122)
    for name in ("tv", "side_tv_upper_right", "side_tv_lower_right"):
        f = faces(*b[name])
        for r in sides(f):
            gradient(r, plastic, plastic_dark, 3)
        rect(*f["top"], (40, 40, 44), 3)
        rect(*f["bottom"], plastic_dark, 3)
        x, y, w, h = f["front"]
        rect(x, y, w, h, (18, 18, 20), 2)                    # bezel...
        crt_static((x + 1, y + 1, w - 2 - (2 if name == "tv" else 0), h - 2), rnd)   # ...around the static
        if name == "tv":
            rect(x + w - 2, y + 2, 1, 1, (90, 92, 100), 0)   # dials beside the screen
            rect(x + w - 2, y + 4, 1, 1, (90, 92, 100), 0)
            x, y, w, h = f["top"]
            for i in range(2, w - 2, 2):
                rect(x + i, y + 2, 1, h - 3, (26, 26, 30), 0)   # vents
    f = faces(*b["tv_back"])
    for r in f.values():
        rect(*r, plastic_dark, 3)
    x, y, w, h = f["back"]
    for k in range(1, h - 1, 2):
        rect(x + 1, y + k, w - 2, 1, (16, 16, 18), 0)
    for name in ("pipe", "bracket", "rod", "knob_right"):
        box(*b[name], metal, 4)
    for name in ("tendril_upper_right", "tendril_lower_right"):
        f = faces(*b[name])
        for r in f.values():
            rect(*r, (58, 58, 64), 3)
        for r in sides(f):
            x, y, w, h = r
            for i in range(1, w, 3):
                px[x + i, y] = shade((84, 84, 92), 2)        # joints along the tendril
    f = faces(*b["torso"])
    for r in sides(f):
        rect(*r, suit_black, 3)
    x, y, w, h = f["front"]
    rect(x + 2, y, 4, 4, SHIRT, 2)                           # shirt in the open collar...
    rect(x + 3, y, 2, 1, (86, 34, 140), 0)                   # ...and a purple tie
    rect(x + 3, y + 1, 2, 8, purple, 3)
    rect(x + 1, y, 1, 5, suit_dark, 0)                       # lapels
    rect(x + 6, y, 1, 5, suit_dark, 0)
    for k in (6, 9):
        px[x + 5, y + k] = (60, 60, 66, 255)                 # buttons
    rect(*f["top"], suit_black, 3)
    f = faces(*b["arm_right"])
    for r in sides(f):
        x, y, w, h = r
        rect(x, y, w, h - 3, suit_black, 3)
        rect(x, y + h - 4, w, 1, SHIRT, 2)                   # shirt cuff
        rect(x, y + h - 3, w, 3, (38, 38, 42), 3)            # black gloves
    rect(*f["top"], suit_black, 3)
    rect(*f["bottom"], (38, 38, 42), 3)
    f = faces(*b["leg_right"])
    for r in sides(f):
        x, y, w, h = r
        rect(x, y, w, h, (24, 24, 28), 3)
        rect(x, y + h - 2, w, 2, SHOE, 2)
    rect(*f["bottom"], SHOE, 2)
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
    # A Cameraman in a navy dress suit with a red tie, gold badge and gold shoulder boards.
    new_image(64, 9)
    suit((31, 42, 68), (178, 34, 34))
    cameraman_head()
    rect(21, 21, 1, 2, GOLD, 4)            # badge on the jacket front
    rect(40, 16, 4, 1, GOLD, 4)            # shoulder boards (top of the arm, and its front edge)
    rect(44, 20, 4, 1, GOLD, 4)
    rect(40, 29, 16, 1, GOLD, 3)           # gold cuffs instead of shirt cuffs
    return img


# ---------------------------------------------------------------- titans and G-Toilet
# Titans and G-Toilet are modelled at twice the detail (2 texels per pixel). Their UVs are read from the .bbmodel by cube
# name, so moving a cube's UV in Blockbench doesn't break the drawing. Glowing parts are drawn twice: on the
# texture and on a "_glow" texture that the game draws full-bright on top.

def bb_boxes(model):
    """Box UV (u, v, w, h, d) of every cube in blockbench/<model>.bbmodel, by cube name."""
    data = json.loads((Path(__file__).resolve().parent.parent / "blockbench" / f"{model}.bbmodel").read_text())
    return {e["name"]: (*e["uv_offset"], *(round(e["to"][i] - e["from"][i]) for i in range(3)))
            for e in data["elements"]}


TITAN_COAT = (44, 44, 48)
TITAN_COAT_DARK = (32, 32, 36)
TITAN_SHIRT = (26, 26, 30)


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


def titan_coat(b, coat, coat_dark, shirt):
    """A long coat hanging open over a shirt: wide lapels, a seam and a vent down the back, coat tails."""
    f = faces(*b["chest"])
    for r in sides(f):
        gradient(r, tuple(c + 6 for c in coat), coat, 3)
    rect(*f["top"], tuple(c + 8 for c in coat), 3)
    rect(*f["bottom"], coat_dark, 2)
    x, y, w, h = f["front"]
    rect(x + 7, y, w - 14, h, shirt, 2)                     # shirt in the open front
    edge = tuple(c + 16 for c in coat)
    for k in range(h):                                      # lapels widen toward the shoulders
        spread = max(0, 4 - k // 3)
        rect(x + 7 - spread, y + k, spread, 1, coat_dark, 2)
        rect(x + w - 7, y + k, spread, 1, coat_dark, 2)
        px[x + 6 - spread, y + k] = shade(edge, 2)          # lapel edges catching the light
        px[x + w - 7 + spread, y + k] = shade(edge, 2)
    x, y, w, h = f["back"]
    rect(x + w // 2, y + 2, 1, h - 2, coat_dark, 2)
    f = faces(*b["waist"])
    for r in sides(f):
        rect(*r, coat, 3)
    x, y, w, h = f["front"]
    rect(x + 5, y, w - 10, h, shirt, 2)
    for k in range(h):
        px[x + 4, y + k] = shade(edge, 2)
        px[x + w - 5, y + k] = shade(edge, 2)
    box(*b["collar"], coat_dark, 3)
    for name in ("coat_back", "coat_side_right", "coat_front_right"):
        f = faces(*b[name])
        for r in sides(f):
            gradient(r, coat, tuple(c - 8 for c in coat), 3)
        rect(*f["bottom"], coat_dark, 2)
    x, y, w, h = faces(*b["coat_back"])["back"]
    rect(x + w // 2, y + 4, 1, h - 4, tuple(c - 18 for c in coat), 0)  # back vent


def titan_belt(b):
    f = faces(*b["belt"])
    for r in sides(f):
        rect(*r, (30, 26, 22), 3)
    x, y, w, h = f["front"]
    rect(x + w // 2 - 2, y, 4, h, (182, 184, 192), 3)       # buckle
    rect(x + w // 2 - 1, y, 2, 1, (120, 122, 130), 0)


def titan_core_ring(b, metal=(112, 118, 128)):
    f = faces(*b["core_ring"])
    box(*b["core_ring"], metal, 4)
    x, y, w, h = f["front"]
    radial(x, y, w, h, w / 2 - 0.7, 9, lambda r: tuple(c + 16 for c in metal))
    for cx, cy in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)):
        px[x + cx, y + cy] = (80, 84, 92, 255)              # bolts


def titan_core_glow(b, ring, centre, edge):
    """A glowing core: a bright ring inside the metal housing and a blazing disc in the middle."""
    x, y, w, h = faces(*b["core_ring"])["front"]
    radial(x, y, w, h, w / 2 - 2.2, w / 2 - 0.7, lambda r: ring)
    f = faces(*b["core"])
    for key in ("right", "left", "top", "bottom"):
        rect(*f[key], edge, 0)
    x, y, w, h = f["front"]
    radial(x, y, w, h, 0, 9, lambda r: centre if r < w / 2 - 0.8 else edge)


def titan_arms(b, shoulder, sleeve, cuff, glove):
    """Shoulders, sleeves (a color, or a function drawing a face rectangle), cuffs and big gloved fists."""
    f = faces(*b["shoulder_right"])
    for r in sides(f):
        gradient(r, tuple(c + 8 for c in shoulder), shoulder, 3)
    rect(*f["top"], tuple(c + 10 for c in shoulder), 3)
    rect(*f["bottom"], tuple(c - 10 for c in shoulder), 2)
    for r in faces(*b["sleeve_right"]).values():
        sleeve(r) if callable(sleeve) else rect(*r, sleeve, 3)
    box(*b["cuff_right"], cuff, 3)
    f = faces(*b["hand_right"])
    box(*b["hand_right"], glove, 4)
    line = tuple(c - 16 for c in glove)
    x, y, w, h = f["front"]
    rect(x, y + 2, w, 1, line, 0)                           # knuckles
    for i in range(2, w, 2):
        rect(x + i, y + 3, 1, h - 3, line, 0)               # fingers
    x, y, w, h = f["bottom"]
    for i in range(2, w, 2):
        rect(x + i, y, 1, h, line, 0)


def titan_legs(b, trouser=(38, 38, 42), boots=False):
    f = faces(*b["trouser_right"])
    for r in sides(f):
        gradient(r, trouser, tuple(c - 6 for c in trouser), 3)
    x, y, w, h = f["front"]
    rect(x + w // 2, y, 1, h, tuple(c - 10 for c in trouser), 0)  # crease
    if boots:
        for r in sides(f):
            x, y, w, h = r
            rect(x, y + h - 6, w, 6, (24, 24, 26), 2)       # boot shafts up the shin
    f = faces(*b["shoe_right"])
    box(*b["shoe_right"], (20, 20, 22), 2)
    rect(*f["bottom"], (12, 12, 12), 0)
    x, y, w, h = f["top"]
    rect(x + 1, y + h - 3, w - 2, 2, (44, 44, 48), 2)       # shine on the toe caps
    if not boots:
        rect(x + 2, y + 2, w - 4, 2, (30, 30, 34), 0)       # laces


def black_camera_lens(x, y, w, h):
    radial(x, y, w, h, 0, 9, lambda r: (20, 20, 24))
    radial(x, y, w, h, 0, min(w, h) / 2 - 0.6, lambda r: GLASS if r > 0.9 else (14, 24, 52))
    px[x + w // 2 - 1, y + h // 2 - 1] = (150, 190, 240, 255)


# ---- Titan Cameraman 1.0

def titan_cameraman_lights(b):
    """What glows on the Titan Cameraman: his core, the LCD flip screens, the turbine exhausts, a recording light."""
    titan_core_glow(b, (40, 170, 255), (190, 245, 255), (60, 190, 255))
    x, y, w, h = faces(*b["flip_screen_right"])["front"]
    for k in range(1, h - 1):
        rect(x + 1, y + k, w - 2, 1, (130 - 8 * k, 160 - 8 * k, 180 - 6 * k), 0)
    px[x + 1, y + 1] = (200, 220, 235, 255)
    x, y, w, h = faces(*b["turbine_right"])["bottom"]
    radial(x, y, w, h, 0, 9, lambda r: (90, 210, 255) if r < 1.4 else (40, 140, 240))
    x, y, w, h = faces(*b["housing"])["front"]
    px[x + w - 1, y] = (255, 40, 30, 255)                   # red recording light
    x, y, w, h = faces(*b["shoulder_cam_right"])["front"]
    px[x + w - 2, y + 1] = (255, 40, 30, 255)


def titan_cameraman():
    b = bb_boxes("titan_cameraman")
    new_image(256, 30, 128)

    # Head: a CCTV camera like the regular Cameramen's, light grey with a dark front plate, under a sun hood,
    # with a 35mm camera on each side and two LCD flip screens on top
    box(*b["mount"], (56, 58, 64), 3)
    f = faces(*b["housing"])
    rect(*f["top"], (196, 200, 207), 3)
    rect(*f["bottom"], (128, 132, 140), 3)
    for r in sides(f):
        gradient(r, (190, 194, 201), (150, 155, 163), 3)
    for key in ("right", "left"):
        x, y, w, h = f[key]
        rect(x, y + 2, w, 1, (140, 145, 152), 2)            # seam
    x, y, w, h = f["front"]
    rect(x, y, w, h, CAMERA_DARK, 3)
    x, y, w, h = f["back"]
    rect(x + 1, y + 1, w - 2, h - 2, (160, 165, 172), 2)    # back cover
    f = faces(*b["hood"])
    rect(*f["top"], (206, 210, 216), 3)
    x, y, w, h = f["top"]
    rect(x + w // 2 - 1, y, 2, h, (214, 218, 224), 2)       # raised ridge down the middle
    rect(*f["bottom"], (70, 72, 78), 3)                     # shadow under the hood
    for r in sides(f):
        rect(*r, (164, 168, 176), 2)
    box(*b["hood_side_right"], (164, 168, 176), 3)
    box(*b["lens"], (34, 34, 38), 3)
    black_camera_lens(*faces(*b["lens"])["front"])
    box(*b["cable"], (24, 24, 28), 2)
    u, v, w, h, d = b["antenna"]
    box(u, v, w, h, d, (52, 52, 56), 3)
    rect(*faces(u, v, w, h, d)["top"], (160, 160, 166), 0)
    f = faces(*b["side_cam_right"])
    box(*b["side_cam_right"], (58, 60, 64), 3)
    rect(*f["top"], (150, 152, 158), 3)                     # silver top plate of a 35mm camera
    box(*b["side_cam_lens_right"], (26, 26, 30), 2)
    black_camera_lens(*faces(*b["side_cam_lens_right"])["front"])
    box(*b["flip_screen_right"], (44, 44, 48), 3)
    rect(*faces(*b["flip_screen_right"])["front"], (40, 40, 44), 2)

    # Body: a black trench coat over a dark shirt, the blue core, a riot shield and jetpack on his back,
    # black camera heads on his shoulders
    titan_coat(b, TITAN_COAT, TITAN_COAT_DARK, TITAN_SHIRT)
    titan_belt(b)
    titan_core_ring(b)
    f = faces(*b["shield"])
    box(*b["shield"], (62, 64, 70), 3)
    x, y, w, h = f["back"]
    rect(x, y, w, h, (90, 92, 98), 2)                       # rim
    rect(x + 1, y + 1, w - 2, h - 2, (64, 66, 72), 3)
    rect(x + 3, y + 3, w - 6, 6, (40, 44, 52), 2)           # viewing window
    rect(x + 4, y + 4, 3, 1, (90, 100, 116), 0)
    f = faces(*b["jetpack"])
    box(*b["jetpack"], (50, 52, 56), 3)
    x, y, w, h = f["back"]
    rect(x + 1, y + h // 2, w - 2, 1, (38, 40, 44), 0)
    f = faces(*b["turbine_right"])
    for r in sides(f):
        gradient(r, (84, 86, 92), (62, 64, 70), 3)
        x, y, w, h = r
        for k in (2, h - 4):
            rect(x, y + k, w, 1, (46, 48, 52), 0)           # bands
    rect(*f["top"], (100, 104, 110), 3)
    rect(*f["bottom"], (36, 36, 40), 2)
    f = faces(*b["shoulder_cam_right"])
    for r in sides(f):
        gradient(r, (40, 40, 44), (26, 26, 30), 3)
    rect(*f["top"], (48, 48, 52), 3)
    rect(*f["bottom"], (22, 22, 26), 2)
    x, y, w, h = f["front"]
    black_camera_lens(x + 1, y + 2, w - 2, w - 2)

    titan_arms(b, TITAN_COAT, lambda r: camo(r, (48, 48, 46), [(36, 38, 36), (62, 60, 54), (40, 42, 46)]),
               (38, 38, 42), (74, 74, 78))
    titan_legs(b)
    titan_cameraman_lights(b)
    return img


def titan_cameraman_glow():
    new_image(256, 30, 128)
    titan_cameraman_lights(bb_boxes("titan_cameraman"))
    return img


# ---- Titan Speakerman 1.0

TSM_RED = (196, 30, 36)
TSM_RED_HI = (232, 74, 74)
TSM_RED_DARK = (132, 16, 22)
SPEAKER_BLACK = (24, 24, 26)


def red_cone(i, k, n):
    """Pixel (i, k) of an n x n speaker lit red from inside: dark surround, glowing cone, bright dust cap."""
    r = math.hypot(i - (n - 1) / 2, k - (n - 1) / 2)
    if r > n / 2:
        return None
    if r > n / 2 - 0.5:
        return (90, 18, 20)
    return (255, 130, 120) if r < 0.8 else (240, 40, 40)


def draw_cone(x, y, n, cone):
    for i in range(n):
        for k in range(n):
            c = cone(i, k, n)
            if c:
                px[x + i, y + k] = c + (255,)


def white_cone(i, k, n):
    """A white driver with a dark dust cap, like the ones on his shoulder towers."""
    r = math.hypot(i - (n - 1) / 2, k - (n - 1) / 2)
    if r > n / 2:
        return None
    if r > n / 2 - 0.5:
        return (70, 70, 74)
    return (40, 40, 44) if r < 0.8 else (236, 236, 232)


def dark_cone(i, k, n):
    """A black driver on a black speaker: only the rings catch the light."""
    r = math.hypot(i - (n - 1) / 2, k - (n - 1) / 2)
    if r > n / 2:
        return None
    return (64, 64, 70) if r > n / 2 - 0.9 or r < n / 6 + 0.3 else (36, 36, 40)


def tweeter(x, y):
    px[x, y] = (150, 152, 160, 255)


def glossy_red(r):
    """Shiny red dress-shirt cloth: smooth, with one soft band of light where the satin catches it."""
    x, y, w, h = r
    gradient(r, TSM_RED, (172, 24, 30), 2)
    band = max(1, w // 5)
    rect(x + w // 4, y, band, h, (214, 48, 52), 2)
    if w > 6:
        px_row = x + w // 4 - 1
        rect(px_row, y, 1, h, (206, 40, 44), 2)


def titan_speakerman_lights(b):
    """What glows on the Titan Speakerman: the red core in its glass dome, the red drivers in his head, the
    cannon muzzle."""
    x, y, w, h = faces(*b["core_glass"])["front"]
    radial(x, y, w, h, 0, 3.7, lambda r: (255, 60, 50) if r < 2.6 else (210, 70, 74))
    f = faces(*b["core"])
    radial(*f["front"], 0, 9, lambda r: (255, 235, 225) if r < 1.0 else (255, 120, 100) if r < 1.9 else (255, 60, 50))
    for key in ("right", "left", "top", "bottom"):
        rect(*f[key], (255, 70, 56), 0)
    x, y, w, h = f["front"]
    px[x + 1, y + 1] = (255, 240, 240, 255)                  # glint on the glass
    x, y, w, h = faces(*b["lower_speaker"])["front"]
    draw_cone(x + 1, y, 5, red_cone)
    draw_cone(x + w - 6, y, 5, red_cone)
    x, y, w, h = faces(*b["center_speaker"])["front"]
    draw_cone(x + 1, y + 1, 6, red_cone)
    x, y, w, h = faces(*b["column_right"])["front"]
    draw_cone(x, y + 5, 6, red_cone)
    draw_cone(x, y + 12, 6, red_cone)
    x, y, w, h = faces(*b["cannon_muzzle"])["bottom"]
    radial(x, y, w, h, 0, 9, lambda r: (255, 120, 90) if r < 1.2 else (220, 40, 30))


def titan_speakerman():
    b = bb_boxes("titan_speakerman")
    new_image(256, 31, 128)

    # Head: a cluster of black speakers. Bevelled front edges; the red drivers are drawn as lights.
    box(*b["mount"], (40, 40, 44), 3)
    for name in ("lower_speaker", "center_speaker", "back_cabinet", "column_right"):
        f = faces(*b[name])
        for r in f.values():
            rect(*r, SPEAKER_BLACK, 3)
        x, y, w, h = f["front"]
        for i in range(w):
            px[x + i, y] = shade((46, 46, 50), 2)
            px[x + i, y + h - 1] = shade((18, 18, 20), 1)
        rect(*f["top"], (36, 36, 40), 2)
    x, y, w, h = faces(*b["lower_speaker"])["front"]
    tweeter(x + w // 2, y + 2)
    tweeter(x + w // 2 - 1, y + 2)
    x, y, w, h = faces(*b["center_speaker"])["front"]
    tweeter(x + w // 2, y + 9)
    tweeter(x + w // 2 - 1, y + 9)
    x, y, w, h = faces(*b["column_right"])["front"]
    tweeter(x + w // 2, y + 2)
    for k in range(19, h - 1, 2):
        px[x + w // 2, y + k] = (52, 52, 56, 255)           # port holes down the column

    # Body: a glossy red dress shirt, the domed core between two tall black speakers, a black jetpack and two
    # big speaker towers leaning out from the shoulders
    for name in ("chest", "waist"):
        f = faces(*b[name])
        for r in sides(f):
            glossy_red(r)
        rect(*f["top"], TSM_RED_HI, 3)
        rect(*f["bottom"], TSM_RED_DARK, 2)
    box(*b["collar"], TSM_RED_DARK, 3)
    titan_belt(b)
    box(*b["core_ring"], (54, 54, 60), 3)
    x, y, w, h = faces(*b["core_glass"])["front"]
    radial(x, y, w, h, 3.7, 9, lambda r: (54, 54, 60))       # the housing shows round the dome's corners
    f = faces(*b["core_glass"])
    for key in ("right", "left", "top", "bottom"):
        rect(*f[key], (170, 120, 126), 2)                    # the dome's glass seen from the side
    f = faces(*b["chest_speaker_right"])
    box(*b["chest_speaker_right"], SPEAKER_BLACK, 3)
    x, y, w, h = f["front"]
    draw_cone(x, y + 1, 5, dark_cone)
    draw_cone(x, y + 6, 5, dark_cone)
    f = faces(*b["jetpack"])
    box(*b["jetpack"], (30, 30, 34), 3)
    x, y, w, h = f["back"]
    rect(x + 2, y + 2, w - 4, 1, (44, 44, 48), 0)
    rect(x + 2, y + h - 3, w - 4, 1, (44, 44, 48), 0)
    f = faces(*b["jetpack_turbine_right"])
    for r in sides(f):
        gradient(r, (52, 52, 58), (34, 34, 38), 3)
    rect(*f["top"], (24, 24, 28), 0)                         # intake
    f = faces(*b["tower_right"])
    for r in f.values():
        rect(*r, SPEAKER_BLACK, 3)
    x, y, w, h = f["front"]
    for i in range(w):
        px[x + i, y] = shade((46, 46, 50), 2)
    tweeter(x + w // 2, y + 2)
    for top in (4, 13, 22):                                  # three white drivers down the tower
        draw_cone(x + 1, y + top, 7, white_cone)
    for k in range(4, h - 1, 3):
        px[x + w - 1, y + k] = (60, 60, 66, 255)             # screw holes along the edge

    titan_arms(b, TSM_RED, glossy_red, TSM_RED_DARK, (30, 30, 34))
    x, y, w, h = faces(*b["cuff_right"])["front"]
    px[x + 1, y + 1] = (230, 220, 210, 255)                  # cuff button
    titan_legs(b, (30, 30, 34))
    f = faces(*b["cannon"])
    for r in sides(f):
        gradient(r, (86, 88, 96), (60, 62, 68), 3)
        x, y, w, h = r
        for k in range(4, h, 6):
            rect(x, y + k, w, 1, (44, 46, 50), 0)            # cooling rings
    rect(*f["top"], (96, 98, 106), 3)
    box(*b["cannon_muzzle"], (40, 40, 44), 2)
    titan_speakerman_lights(b)
    return img


def titan_speakerman_glow():
    new_image(256, 31, 128)
    titan_speakerman_lights(bb_boxes("titan_speakerman"))
    return img


# ---- Titan TV Man 1.0

def titan_tv_man_lights(b):
    """What glows on the Titan TV Man: the static on his screen, his purple core, the accents on his claws."""
    x, y, w, h = faces(*b["tv"])["front"]
    rnd = random.Random(44)
    for i in range(w):
        for k in range(h):
            v = rnd.randint(150, 250) * (0.9 if k % 2 else 1.0)
            px[x + i, y + k] = (int(v), int(v), min(255, int(v * 1.04)), 255)
    titan_core_glow(b, (170, 70, 255), (240, 200, 255), (190, 100, 255))
    for i in range(3):
        f = faces(*b[f"claw{i}_right"])
        x, y, w, h = f["front"]
        rect(x + w - 1, y, 1, h, (180, 80, 255), 0)         # purple edge down each blade
        rect(*f["top"], (200, 110, 255), 0)


def titan_tv_man():
    b = bb_boxes("titan_tv_man")
    new_image(256, 32, 128)
    black, black_dark = (30, 30, 34), (22, 22, 26)

    # Head: a giant black TV, its screen set back inside the bezel
    box(*b["mount"], (40, 40, 44), 3)
    for name in ("tv", "tv_back"):
        for r in faces(*b[name]).values():
            rect(*r, (34, 34, 38), 3)
    x, y, w, h = faces(*b["tv"])["top"]
    for i in range(2, w - 2, 2):
        rect(x + i, y + 1, 1, h - 2, (24, 24, 28), 0)       # vents
    x, y, w, h = faces(*b["tv_back"])["back"]
    for k in range(1, h - 1, 2):
        rect(x + 1, y + k, w - 2, 1, (24, 24, 28), 0)
    for name in ("bezel_top", "bezel_bottom", "bezel_side_right"):
        f = faces(*b[name])
        box(*b[name], (40, 40, 44), 3)
        rect(*f["front"], (52, 52, 58), 3)
    x, y, w, h = faces(*b["bezel_bottom"])["front"]
    px[x + w - 3, y + 1] = (60, 220, 90, 255)               # power light

    # Body: a black trench coat, the purple core, triple-bladed claws on his back
    titan_coat(b, black, black_dark, (18, 18, 22))
    titan_belt(b)
    titan_core_ring(b, (54, 50, 62))
    box(*b["claw_mount"], (28, 26, 32), 3)
    for i in range(3):
        f = faces(*b[f"claw{i}_right"])
        for r in sides(f):
            gradient(r, (40, 36, 46), (24, 22, 28), 3)
        rect(*f["top"], (60, 56, 66), 2)

    titan_arms(b, black, black, black_dark, (34, 34, 38))
    for name in ("hook_right", "hook_left"):
        f = faces(*b[name])
        box(*b[name], (140, 144, 152), 4)
        x, y, w, h = f["back"]
        rect(x + 1, y + h - 2, w - 2, 1, (90, 94, 102), 0)  # the hook's curve
    titan_legs(b, (30, 30, 34), boots=True)
    titan_tv_man_lights(b)
    return img


def titan_tv_man_glow():
    new_image(256, 32, 128)
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


def titan_cameraman_2_lights(b):
    """What glows on Titan Cameraman 2.0: the cyan lens and core, the red antenna and fin lights, the railgun muzzle."""
    x, y, w, h = faces(*b["lens"])["front"]
    radial(x, y, w, h, 0, 9, lambda r: (230, 252, 255) if r < 1.4 else (120, 230, 255) if r < 2.6 else (40, 180, 240))
    titan_core_glow(b, (40, 200, 255), (220, 252, 255), (80, 220, 255))
    for name in ("antenna_light", "fin_light"):
        for r in faces(*b[name]).values():
            rect(*r, (255, 60, 50), 0)
    for r in faces(*b["railgun_muzzle"]).values():
        rect(*r, (90, 200, 255), 0)
    x, y, w, h = faces(*b["railgun_barrel"])["front"]
    rect(x + 1, y + h - 3, w - 2, 2, (110, 210, 255), 0)


def titan_cameraman_2():
    b = bb_boxes("titan_cameraman_2")
    new_image(256, 60, 128)
    dark, darker = (52, 54, 60), (34, 36, 40)
    steel = (150, 154, 162)

    # Head: a dark camera with a big round lens, antenna, fin, angled plates and camcorders either side
    box(*b["mount"], (40, 40, 44), 3)
    f = metal_box(b["housing"], dark)
    x, y, w, h = f["front"]
    rect(x, y, w, h, darker, 3)
    box(*b["lens_ring"], (30, 30, 34), 3)
    x, y, w, h = faces(*b["lens_ring"])["front"]
    radial(x, y, w, h, 3.2, 9, lambda r: (66, 68, 76))
    for name in ("antenna", "fin"):
        box(*b[name], (26, 26, 30), 2)
    metal_box(b["plate_right"], steel)
    f = metal_box(b["camcorder_right"], (44, 44, 48))
    x, y, w, h = f["right"]
    rect(x + 2, y + 1, w - 4, 2, (90, 92, 100), 2)          # side panel
    box(*b["camcorder_lens_right"], (24, 24, 28), 2)
    black_camera_lens(*faces(*b["camcorder_lens_right"])["front"])

    # Body: a black coat over a long vest, the core in its casing with a shield under it, black shoulder cameras,
    # a silver winged jetpack
    titan_coat(b, TITAN_COAT, TITAN_COAT_DARK, (40, 40, 44))
    titan_belt(b)
    titan_core_ring(b, (40, 42, 48))
    metal_box(b["core_shield"], steel)
    for name in ("core_claw_right", "core_claw_bottom"):
        box(*b[name], (70, 72, 80), 2)
    f = metal_box(b["jetpack"], (176, 180, 188))
    x, y, w, h = f["back"]
    rect(x + 2, y + 3, w - 4, 1, (130, 134, 142), 0)
    rect(x + 2, y + h - 4, w - 4, 1, (130, 134, 142), 0)
    for name in ("wing_right", "wing_top_right"):
        metal_box(b[name], (186, 190, 198))
    metal_box(b["shoulder_cam_right"], (128, 130, 136))      # grey CCTV cameras, as in the picture
    box(*b["shoulder_cam_lens_right"], (20, 20, 24), 2)
    x, y, w, h = faces(*b["shoulder_cam_lens_right"])["front"]
    black_camera_lens(x + 1, y, h, h)

    titan_arms(b, TITAN_COAT, (36, 36, 40), (30, 30, 34), (28, 28, 32))
    f = metal_box(b["hammer_head"], (120, 124, 134))
    x, y, w, h = f["front"]
    rect(x + w // 2 - 2, y + 1, 4, h - 2, (50, 130, 220), 4)  # blue hexagon in the middle of the head
    for cx, cy in ((x, y), (x + w - 1, y), (x, y + h - 1), (x + w - 1, y + h - 1)):
        px[cx, cy] = (70, 72, 80, 255)                       # rivets
    metal_box(b["hammer_handle"], (150, 154, 162))
    f = metal_box(b["railgun"], (110, 114, 122))
    x, y, w, h = f["right"]
    rect(x + 1, y + 3, w - 2, 1, (30, 30, 30), 0)           # the black and yellow feed cables
    rect(x + 1, y + 5, w - 2, 1, (220, 190, 40), 0)
    metal_box(b["railgun_barrel"], (60, 120, 200))
    titan_legs(b, (34, 34, 38), boots=True)
    metal_box(b["shin_plate_right"], (120, 124, 134))
    titan_cameraman_2_lights(b)
    return img


def titan_cameraman_2_glow():
    new_image(256, 60, 128)
    titan_cameraman_2_lights(bb_boxes("titan_cameraman_2"))
    return img


def titan_speakerman_2_lights(b):
    """What glows on Titan Speakerman 2.0: his core, the red drivers in his head and towers, the cannon muzzles."""
    x, y, w, h = faces(*b["core_glass"])["front"]
    radial(x, y, w, h, 0, 3.7, lambda r: (255, 60, 50) if r < 2.6 else (210, 70, 74))
    f = faces(*b["core"])
    radial(*f["front"], 0, 9, lambda r: (255, 235, 225) if r < 1.0 else (255, 120, 100) if r < 1.9 else (255, 60, 50))
    for key in ("right", "left", "top", "bottom"):
        rect(*f[key], (255, 70, 56), 0)
    x, y, w, h = faces(*b["lower_speaker"])["front"]
    draw_cone(x + 1, y, 5, red_cone)
    draw_cone(x + w - 6, y, 5, red_cone)
    x, y, w, h = faces(*b["center_speaker"])["front"]
    draw_cone(x + 1, y + 1, 6, red_cone)
    rect(x, y, w, 1, (255, 70, 60), 0)                       # the top of the main speaker glows
    x, y, w, h = faces(*b["column_right"])["front"]
    draw_cone(x, y + 5, 6, red_cone)
    draw_cone(x, y + 12, 6, red_cone)
    x, y, w, h = faces(*b["tower_right"])["front"]
    for top in (4, 13, 22):
        draw_cone(x + 1, y + top, 7, red_cone)
    for name in ("cannon_muzzle_right", "cannon_muzzle_left"):
        x, y, w, h = faces(*b[name])["bottom"]
        radial(x, y, w, h, 0, 9, lambda r: (255, 140, 100) if r < 1.5 else (240, 50, 30))


def titan_speakerman_2():
    b = bb_boxes("titan_speakerman_2")
    new_image(256, 61, 128)
    red, red_dark = (150, 22, 28), (104, 14, 18)
    steel = (140, 144, 152)

    # Head: the same speakers, under a metal helmet with a frame on top
    box(*b["mount"], (40, 40, 44), 3)
    for name in ("lower_speaker", "center_speaker", "back_cabinet", "column_right"):
        f = faces(*b[name])
        for r in f.values():
            rect(*r, SPEAKER_BLACK, 3)
        x, y, w, h = f["front"]
        for i in range(w):
            px[x + i, y] = shade((46, 46, 50), 2)
        rect(*f["top"], (36, 36, 40), 2)
    metal_box(b["helmet"], steel)
    box(*b["frame_right"], (90, 92, 100), 3)

    # Body: a darker red shirt, speakers round the core and under it, red towers plated at the back, a big jetpack
    for name in ("chest", "waist"):
        f = faces(*b[name])
        for r in sides(f):
            gradient(r, red, red_dark, 3)
            x, y, w, h = r
            rect(x + w // 4, y, max(1, w // 6), h, (172, 34, 40), 2)
        rect(*f["top"], (176, 40, 46), 3)
    box(*b["collar"], red_dark, 3)
    titan_belt(b)
    box(*b["core_ring"], (54, 54, 60), 3)
    x, y, w, h = faces(*b["core_glass"])["front"]
    radial(x, y, w, h, 3.7, 9, lambda r: (54, 54, 60))
    f = faces(*b["core_glass"])
    for key in ("right", "left", "top", "bottom"):
        rect(*f[key], (170, 120, 126), 2)
    for name in ("core_speaker_right", "belly_speaker_mid", "belly_speaker_right"):
        f = faces(*b[name])
        box(*b[name], SPEAKER_BLACK, 3)
        x, y, w, h = f["front"]
        n = min(w, h) - 1
        draw_cone(x + (w - n) // 2, y + (h - n) // 2, n, dark_cone)
    f = metal_box(b["jetpack"], (66, 68, 74))
    x, y, w, h = f["back"]
    rect(x + 2, y + 2, w - 4, 1, (44, 44, 48), 0)
    f = faces(*b["jetpack_turbine_right"])
    for r in sides(f):
        gradient(r, (80, 82, 88), (52, 54, 60), 3)
        x, y, w, h = r
        for k in (3, h - 5):
            rect(x, y + k, w, 1, (40, 42, 46), 0)
    rect(*f["top"], (24, 24, 28), 0)
    f = faces(*b["tower_right"])
    for r in f.values():
        gradient(r, red, red_dark, 3)
    x, y, w, h = f["front"]
    for top in (4, 13, 22):
        draw_cone(x + 1, y + top, 7, dark_cone)
    metal_box(b["tower_plate_right"], steel)

    titan_arms(b, red, red, red_dark, (30, 30, 34))
    titan_legs(b, (92, 94, 100))
    for name in ("cannon_right", "cannon_left"):
        f = faces(*b[name])
        for r in sides(f):
            gradient(r, (44, 44, 48), (28, 28, 32), 3)
            x, y, w, h = r
            for k in range(4, h, 6):
                rect(x, y + k, w, 1, (64, 66, 72), 0)        # armour bands
        rect(*f["top"], (60, 60, 66), 3)
    for name in ("cannon_muzzle_right", "cannon_muzzle_left"):
        box(*b[name], (36, 36, 40), 2)
    titan_speakerman_2_lights(b)
    return img


def titan_speakerman_2_glow():
    new_image(256, 61, 128)
    titan_speakerman_2_lights(bb_boxes("titan_speakerman_2"))
    return img


def titan_tv_man_2_lights(b):
    """What glows on Titan TV Man 2.0: the static on all his screens, his purple core and tentacle, the sword."""
    rnd = random.Random(62)
    for name in ("tv", "side_screen_right", "shoulder_tv_right", "belt_tv"):
        x, y, w, h = faces(*b[name])["front"]
        inset = 1 if min(w, h) > 4 else 0
        crt_static((x + inset, y + inset, w - 2 * inset, h - 2 * inset), rnd)
    titan_core_glow(b, (170, 70, 255), (250, 220, 255), (200, 110, 255))
    for r in faces(*b["tentacle"]).values():
        rect(*r, (170, 80, 240), 4)
    f = faces(*b["blade"])
    for key in ("right", "left", "front", "back"):
        x, y, w, h = f[key]
        gradient((x, y, w, h), (230, 150, 255), (170, 60, 240), 0)


def titan_tv_man_2():
    b = bb_boxes("titan_tv_man_2")
    new_image(256, 62, 128)
    black, black_dark = (28, 28, 32), (20, 20, 24)

    # Head: a thin wide TV with a thin screen at each side, a crown of spikes, turbines at the back, a ring at the neck
    for name in ("tv", "tv_back", "side_screen_right"):
        f = faces(*b[name])
        for r in f.values():
            rect(*r, (32, 32, 36), 3)
        rect(*f["top"], (40, 40, 44), 3)
    for name in ("crown", "crown_spike_mid", "crown_spike_right", "crown_spike_outer_right"):
        metal_box(b[name], (110, 112, 122))
    metal_box(b["ring"], (120, 124, 134))
    f = faces(*b["head_turbine_right"])
    for r in sides(f):
        gradient(r, (70, 72, 80), (44, 46, 52), 3)
    rect(*f["back"], (24, 24, 28), 0)

    # Body: a black coat, the big purple core with claws round it, TVs on his shoulders and belt, claws on his back
    titan_coat(b, black, black_dark, (16, 16, 20))
    titan_belt(b)
    titan_core_ring(b, (60, 56, 70))
    for name in ("core_claw_right", "core_claw_bottom"):
        box(*b[name], (150, 152, 160), 3)
    for name in ("shoulder_tv_right", "belt_tv"):
        for r in faces(*b[name]).values():
            rect(*r, (30, 30, 34), 3)
    box(*b["claw_mount"], (28, 26, 32), 3)
    for i in range(3):
        f = faces(*b[f"claw{i}_right"])
        for r in sides(f):
            gradient(r, (40, 36, 46), (24, 22, 28), 3)
        x, y, w, h = f["front"]
        rect(x + w - 1, y, 1, h, (150, 70, 230), 0)          # purple edge down each blade
    titan_arms(b, black, black, black_dark, (32, 32, 36))
    f = metal_box(b["blade_holster"], (70, 72, 80))
    x, y, w, h = f["right"]
    for k in range(2, h, 3):
        px[x + w // 2, y + k] = (200, 40, 40, 255)          # red marks at the straps
    titan_legs(b, (28, 28, 32), boots=True)
    titan_tv_man_2_lights(b)
    return img


def titan_tv_man_2_glow():
    new_image(256, 62, 128)
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
    "entity/skibidi_toilet.png": lambda: toilet(7, skibidi_head),
    "entity/gman_toilet.png": gman_toilet,
    "entity/big_skibidi_toilet.png": lambda: toilet(13, big_head),
    "entity/scientist_toilet.png": lambda: toilet(14, scientist_head),
    "entity/skibidi_urinal.png": urinal,
    "entity/flying_skibidi_toilet.png": lambda: toilet(15, flying_head),
    "entity/giant_flying_skibidi_toilet.png": giant_flying_toilet,
    "entity/dj_skibidi_toilet.png": lambda: toilet(17, dj_head, headphones),
    "entity/spider_toilet.png": lambda: toilet(7, skibidi_head, spider_legs),
    "entity/jetpack_toilet.png": jetpack_toilet,
    "entity/cameraman.png": lambda: alliance(9, cameraman_head),
    "entity/speakerman.png": lambda: alliance(9, speakerman_head),
    "entity/tv_man.png": lambda: alliance(9, tv_man_head),
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
    "entity/saint_skibidi_toilet.png": lambda: toilet(19, saint_head, halo, gold_trim),
    "entity/saint_skibidi_toilet_demonic.png": lambda: toilet(19, saint_head_demonic, halo, gold_trim),
    "entity/triple_skibidi_toilet.png": triple,
    "entity/hat_skibidi_toilet.png": hat_toilet,
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
