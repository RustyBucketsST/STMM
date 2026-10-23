"""Generates the mod's textures into src/main/resources/assets/skibidi/textures/.

entity/skibidi_toilet.png, gman_toilet.png, big_skibidi_toilet.png, scientist_toilet.png,
jetpack_toilet.png                                   -- UVs match SkibidiToiletModel
entity/spider_toilet.png                             -- UVs match SpiderToiletModel
entity/cameraman.png, speakerman.png, tv_man.png     -- UVs match AllianceModel
item/plunger.png, item/alliance_blaster.png, item/stun_rifle.png, item/gravity_plunger.png, item/energy_knife.png
gui/power/*.png                                      -- 16x16 icons for the Powers menu and HUD
player/*.png                                         -- UVs match PlayerHeadModels; grey parts are dyed in game

For a box at uv (u, v) with size (w, h, d):
top (u+d, v) w x d, front (u+d, v+d) w x h, sides (u, v+d) and (u+d+w, v+d) d x h,
back (u+2d+w, v+d) w x h, bottom (u+d+w, v) w x d.

Usage: python3 tools/gen_texture.py [textures_dir]
"""
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


def outline(c):
    """Adds a 1px outline around everything drawn so far (for item icons)."""
    w, h = img.size
    filled = {(x, y) for x in range(w) for y in range(h) if px[x, y][3]}
    for x, y in filled:
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if 0 <= nx < w and 0 <= ny < h and (nx, ny) not in filled:
                px[nx, ny] = c + (255,)


# ---------------------------------------------------------------- toilets

PORCELAIN = (238, 240, 242)
SHADOW = (196, 200, 206)
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


GMAN_SKIN = (232, 188, 158)
GMAN_HAIR = (34, 30, 30)


def gman_head():
    # Head 8x8x8 at (0, 0): slicked-back black hair, angry brows, staring eyes, toothy grin.
    box(0, 0, 8, 8, 8, GMAN_SKIN)
    rect(8, 0, 8, 8, GMAN_HAIR)            # top of head
    rect(0, 8, 32, 1, GMAN_HAIR)           # hairline around all sides
    rect(0, 9, 8, 2, GMAN_HAIR)            # hair over the ears
    rect(16, 9, 8, 2, GMAN_HAIR)
    rect(24, 8, 8, 6, GMAN_HAIR)           # back of head
    fx, fy = 8, 8
    for x, y in ((1, 1), (2, 1), (3, 2), (6, 1), (5, 1), (4, 2)):  # brows angled down to the middle
        px[fx + x, fy + y] = GMAN_HAIR + (255,)
    for x, pupil in ((1, 2), (6, 5)):      # eyes staring straight at you
        rect(fx + x, fy + 3, 1, 1, (255, 255, 255), 0)
        rect(fx + pupil, fy + 3, 1, 1, (20, 20, 20), 0)
    rect(fx + 3, fy + 4, 2, 1, (205, 160, 130), 0)  # nose shadow
    rect(fx + 1, fy + 5, 6, 2, (80, 15, 15), 0)     # grin
    rect(fx + 2, fy + 5, 4, 1, (245, 245, 240), 0)  # teeth


def toilet_body():
    # Bowl 10x6x11 at (0, 16)
    box(0, 16, 10, 6, 11, PORCELAIN)
    rect(0, 32, 42, 1, SHADOW)             # shadow under the rim
    rect(12, 17, 8, 9, (60, 140, 200), 10)  # water inside the bowl (top face, rim left white)
    rect(13, 18, 6, 7, (45, 115, 180), 10)

    # Flush handle 1x1x2 at (42, 16)
    box(42, 16, 1, 1, 2, (170, 175, 185), 4)

    # Tank 12x13x4 at (0, 33)
    box(0, 33, 12, 13, 4, PORCELAIN)
    rect(4, 33, 12, 1, SHADOW)             # tank lid seam

    # Lid 10x9x1 at (32, 33)
    box(32, 33, 10, 9, 1, PORCELAIN)
    rect(35, 36, 6, 5, SHADOW)             # inner seat ring on the front of the lid
    rect(36, 37, 4, 3, PORCELAIN, 4)

    # Pedestal 6x6x7 at (0, 50)
    box(0, 50, 6, 6, 7, PORCELAIN)
    rect(0, 62, 26, 1, SHADOW)


BIG_SKIN = (222, 162, 122)


def big_head():
    # Head 8x8x8 at (0, 0): bald, furious, screaming.
    box(0, 0, 8, 8, 8, BIG_SKIN)
    fx, fy = 8, 8
    for x, y in ((1, 1), (2, 1), (3, 2), (6, 1), (5, 1), (4, 2)):  # brows angled down to the middle
        px[fx + x, fy + y] = (60, 40, 30, 255)
    rect(fx + 1, fy + 3, 2, 2, (255, 255, 255), 0)  # wide eyes
    rect(fx + 5, fy + 3, 2, 2, (255, 255, 255), 0)
    px[fx + 2, fy + 3] = (20, 20, 20, 255)
    px[fx + 5, fy + 3] = (20, 20, 20, 255)
    rect(fx + 2, fy + 5, 4, 3, (85, 15, 15), 0)     # screaming mouth
    rect(fx + 2, fy + 5, 4, 1, (245, 245, 240), 0)  # teeth


SCIENTIST_SKIN = (236, 200, 170)
SCIENTIST_HAIR = (225, 225, 230)
FRAME = (40, 40, 46)


def scientist_head():
    # Head 8x8x8 at (0, 0): wild white hair, thick goggle-like glasses, white mustache.
    box(0, 0, 8, 8, 8, SCIENTIST_SKIN)
    rect(8, 0, 8, 8, SCIENTIST_HAIR, 12)   # top of head
    rect(0, 8, 32, 1, SCIENTIST_HAIR, 12)  # hairline around all sides
    rect(0, 9, 8, 3, SCIENTIST_HAIR, 12)   # bushy sides
    rect(16, 9, 8, 3, SCIENTIST_HAIR, 12)
    rect(24, 8, 8, 6, SCIENTIST_HAIR, 12)  # back of head
    fx, fy = 8, 8
    rect(fx, fy + 2, 8, 1, FRAME, 0)       # glasses frame top
    rect(fx, fy + 4, 8, 1, FRAME, 0)       # frame bottom
    rect(fx, fy + 3, 8, 1, FRAME, 0)       # bridge and sides...
    rect(fx + 1, fy + 3, 2, 1, (170, 210, 235), 0)  # ...with a lens over each eye
    rect(fx + 5, fy + 3, 2, 1, (170, 210, 235), 0)
    px[fx + 2, fy + 3] = (30, 50, 80, 255)
    px[fx + 5, fy + 3] = (30, 50, 80, 255)
    rect(fx + 2, fy + 5, 4, 1, SCIENTIST_HAIR, 0)   # mustache
    rect(fx + 3, fy + 6, 2, 1, (100, 30, 30), 0)    # mouth


def spider_legs():
    # Leg 16x2x2 at (26, 50), used for all eight legs
    box(26, 50, 16, 2, 2, (44, 42, 46), 6)
    for x in range(28, 62, 6):             # joints
        rect(x, 52, 1, 2, (24, 22, 26), 0)


def jetpack():
    # Tank 4x10x3 at (32, 0), used for both tanks
    box(32, 0, 4, 10, 3, (175, 50, 40), 8)
    rect(32, 5, 14, 1, (210, 210, 215), 0)  # metal straps
    rect(32, 10, 14, 1, (210, 210, 215), 0)
    # Nozzle 2x2x2 at (48, 0)
    box(48, 0, 2, 2, 2, (60, 60, 66), 4)


FLYER_HAIR = (214, 176, 92)


def flying_head():
    # Head 8x8x8 at (0, 0): blond, wide-eyed, grinning ear to ear.
    box(0, 0, 8, 8, 8, SKIN)
    rect(8, 0, 8, 8, FLYER_HAIR)           # top of head
    rect(0, 8, 32, 2, FLYER_HAIR)          # hairline around all sides
    rect(24, 8, 8, 4, FLYER_HAIR)          # back of head
    fx, fy = 8, 8
    for x in (1, 5):
        rect(fx + x, fy + 2, 2, 1, (150, 115, 50), 0)   # eyebrows
        px[fx + x, fy + 3] = (255, 255, 255, 255)       # eyes
        px[fx + x + 1, fy + 3] = (30, 30, 30, 255)
    rect(fx + 1, fy + 5, 6, 1, (245, 245, 240), 0)  # grin: a full row of teeth...
    rect(fx + 2, fy + 6, 4, 1, (90, 20, 20), 0)     # ...over an open mouth


GIANT_SKIN = (214, 160, 118)
GIANT_HAIR = (40, 32, 28)


def giant_flying_head():
    # Head 8x8x8 at (0, 0): slicked dark hair, heavy brows, stubble and a huge toothy grin.
    box(0, 0, 8, 8, 8, GIANT_SKIN)
    rect(8, 0, 8, 8, GIANT_HAIR)
    rect(0, 8, 32, 1, GIANT_HAIR)
    rect(24, 8, 8, 5, GIANT_HAIR)
    fx, fy = 8, 8
    rect(fx + 1, fy + 2, 2, 1, GIANT_HAIR, 0)       # heavy brows
    rect(fx + 5, fy + 2, 2, 1, GIANT_HAIR, 0)
    for x in (1, 5):
        rect(fx + x, fy + 3, 2, 1, (255, 255, 255), 0)
        px[fx + x + (1 if x == 1 else 0), fy + 3] = (20, 20, 20, 255)
    rect(fx, fy + 5, 8, 3, (180, 130, 100), 6)      # stubble
    rect(fx + 1, fy + 5, 6, 2, (70, 12, 12), 0)     # grin
    rect(fx + 1, fy + 5, 6, 1, (245, 245, 240), 0)  # teeth


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
    # Band 10x1x2 at (32, 0)
    box(32, 0, 10, 1, 2, (30, 30, 34), 4)
    # Ear cup 1x3x3 at (32, 4), used for both cups
    box(32, 4, 1, 3, 3, (48, 48, 54), 4)
    rect(33, 7, 1, 3, (200, 40, 60), 0)            # red trim on the outside


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
    new_image(64, 18)
    urinal_head()
    # Back plate 10x20x2 at (0, 16)
    box(0, 16, 10, 20, 2, PORCELAIN)
    rect(2, 18, 10, 20, PORCELAIN, 4)              # front face, smooth
    # Bowl 8x9x6 at (0, 38): the top face is the dark opening the head comes out of
    box(0, 38, 8, 9, 6, PORCELAIN)
    rect(6, 38, 8, 6, (70, 120, 160), 8)
    rect(7, 39, 6, 4, (40, 80, 120), 6)
    rect(0, 44, 28, 1, SHADOW)                     # shadow under the rim
    # Flush pipe 2x4x2 at (32, 0)
    box(32, 0, 2, 4, 2, (178, 184, 194), 6)
    return img


SAINT_SKIN = (238, 204, 176)
SAINT_HAIR = (236, 236, 240)
HALO_GOLD = (240, 200, 70)


def saint_face(eye_white, pupil):
    # Head 8x8x8 at (0, 0): an old man with white hair at the sides, a long white beard.
    box(0, 0, 8, 8, 8, SAINT_SKIN)
    rect(8, 0, 8, 8, SAINT_SKIN)            # bald on top
    rect(0, 9, 8, 3, SAINT_HAIR, 6)         # white hair over the ears
    rect(16, 9, 8, 3, SAINT_HAIR, 6)
    rect(24, 9, 8, 4, SAINT_HAIR, 6)        # and around the back
    fx, fy = 8, 8
    for x in (1, 5):
        rect(fx + x, fy + 2, 2, 1, SAINT_HAIR, 0)          # bushy white brows
        rect(fx + x, fy + 3, 2, 1, eye_white, 0)           # eyes
        px[fx + x + (1 if x == 1 else 0), fy + 3] = pupil + (255,)
    rect(fx + 1, fy + 5, 6, 3, SAINT_HAIR, 6)              # beard
    rect(fx + 3, fy + 5, 2, 1, (110, 40, 40), 0)          # mouth, singing
    for side in (0, 16):                                    # beard carries round the jaw
        rect(side + 1, 13, 6, 3, SAINT_HAIR, 6)


def saint_head():
    saint_face((255, 255, 255), (60, 110, 200))


def saint_head_demonic():
    saint_face((200, 30, 30), (255, 230, 60))


def halo():
    # Bars 8x1x1 at (32, 0) and 1x1x6 at (32, 4)
    box(32, 0, 8, 1, 1, HALO_GOLD, 6)
    box(32, 4, 1, 1, 6, HALO_GOLD, 6)


def gold_trim():
    rect(0, 32, 42, 1, (200, 160, 50), 4)  # gold rim under the bowl instead of a plain shadow
    rect(4, 33, 12, 1, (200, 160, 50), 4)  # gold seam on the tank lid
    box(42, 16, 1, 1, 2, HALO_GOLD, 4)     # gold flush handle


def head_strip(head, seed):
    """Draws a head on a scratch image and returns its 32x16 texture strip, to place anywhere."""
    global img, px
    saved = img, px
    new_image(64, seed)
    head()
    strip = img.crop((0, 0, 32, 16))
    img, px = saved
    return strip


def triple():
    new_image(128, 20, 64)
    big_head()                              # middle head
    toilet_body()
    img.paste(head_strip(urinal_head, 21), (64, 0))
    img.paste(head_strip(giant_flying_head, 22), (96, 0))
    box(64, 16, 3, 8, 3, SKIN)              # neck 3x8x3 at (64, 16)
    return img


HAT_SKIN = (228, 180, 132)


def hat_head():
    # Head 8x8x8 at (0, 0): sly smirk, one raised brow.
    box(0, 0, 8, 8, 8, HAT_SKIN)
    rect(0, 8, 32, 1, HAIR)
    rect(24, 8, 8, 4, HAIR)
    fx, fy = 8, 8
    rect(fx + 1, fy + 2, 2, 1, HAIR, 0)
    rect(fx + 5, fy + 1, 2, 1, HAIR, 0)     # raised brow
    for x in (1, 5):
        px[fx + x, fy + 3] = (255, 255, 255, 255)
        px[fx + x + 1, fy + 3] = (30, 30, 30, 255)
    rect(fx + 3, fy + 6, 3, 1, (110, 40, 40), 0)   # smirk
    px[fx + 6, fy + 5] = (110, 40, 40, 255)


def top_hat():
    # Brim 10x1x10 at (64, 0), crown 6x6x6 at (64, 12), with a red band round the bottom of the crown
    box(64, 0, 10, 1, 10, (26, 26, 30), 4)
    box(64, 12, 6, 6, 6, (30, 30, 34), 4)
    rect(64, 23, 24, 1, (150, 25, 30), 4)


def hat_toilet():
    new_image(128, 23, 64)
    hat_head()
    toilet_body()
    top_hat()
    return img


ARMOR = (34, 34, 38)


def fast_armored():
    new_image(64, 24)
    # Shell 16x14x16 at (0, 0)
    box(0, 0, 16, 14, 16, ARMOR, 5)
    for x in range(0, 64, 8):               # plate seams down every side
        rect(x, 16, 1, 14, (22, 22, 25), 2)
    rect(0, 22, 64, 1, (22, 22, 25), 2)     # and one seam around the middle
    for x in range(2, 64, 4):               # rivets along the top and bottom edges of the sides
        px[x, 17] = (88, 88, 96, 255)
        px[x, 28] = (88, 88, 96, 255)
    fx, fy = 16, 16                         # front face
    rect(fx + 2, fy + 4, 12, 3, (8, 8, 10), 0)     # visor slit...
    rect(fx + 4, fy + 5, 2, 1, (255, 50, 40), 0)   # ...with eyes glaring out
    rect(fx + 10, fy + 5, 2, 1, (255, 50, 40), 0)
    # Plate 12x2x12 at (0, 30)
    box(0, 30, 12, 2, 12, (52, 52, 58), 4)
    return img


STRIDER_BODY = (128, 133, 140)
STRIDER_DARK = (58, 60, 66)


def camera_strider():
    new_image(64, 25)
    # Body 12x10x16 at (0, 0)
    box(0, 0, 12, 10, 16, STRIDER_BODY, 6)
    rect(16, 0, 12, 16, (150, 155, 162), 6)        # lighter top
    rect(0, 21, 56, 1, (100, 104, 110), 2)         # panel seam around the sides
    rect(16, 16, 12, 10, STRIDER_DARK, 3)          # dark front around the lens
    rect(40, 16, 12, 10, (110, 114, 120), 4)       # back panel
    # Lens 6x6x6 at (0, 26): dark barrel with blue glass on the front
    box(0, 26, 6, 6, 6, (36, 36, 42), 3)
    rect(7, 33, 4, 4, (50, 110, 200), 6)
    px[8, 34] = (170, 210, 255, 255)
    # Leg 2x30x2 at (24, 26)
    box(24, 26, 2, 30, 2, STRIDER_DARK, 4)
    for y in (36, 46):                             # joints
        rect(24, y, 8, 1, (30, 30, 34), 0)
    # Light 2x1x2 at (32, 26)
    box(32, 26, 2, 1, 2, (230, 40, 40), 0)
    return img


def toilet(seed, head, *extras):
    new_image(64, seed)
    head()
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


def cameraman_head():
    # Camera body 6x6x10 at (0, 32)
    box(0, 32, 6, 6, 10, (150, 155, 162))
    rect(10, 32, 6, 10, (172, 177, 184))   # lighter top
    rect(0, 44, 32, 1, (118, 123, 130))    # panel seam around the sides
    rect(10, 42, 6, 6, (62, 64, 70), 3)    # dark front around the lens
    px[15, 42] = (230, 30, 30, 255)        # red recording light

    # Lens 4x4x2 at (32, 32)
    box(32, 32, 4, 4, 2, (48, 48, 54), 3)
    rect(35, 35, 2, 2, (20, 30, 60), 0)    # glass
    px[35, 35] = (130, 175, 235, 255)      # glint


def speakerman_head():
    # Speaker cabinet 10x10x8 at (0, 32)
    box(0, 32, 10, 10, 8, (28, 28, 30), 3)
    fx, fy = 8, 40
    for i in range(10):
        for j in range(10):
            woofer = math.hypot(i - 4.5, j - 6.0)
            tweeter = math.hypot(i - 4.5, j - 1.0)
            if woofer <= 1.2:
                px[fx + i, fy + j] = shade((36, 36, 40), 2)   # dust cap
            elif woofer <= 2.7:
                px[fx + i, fy + j] = shade((108, 108, 116), 4)  # cone
            elif woofer <= 3.5:
                px[fx + i, fy + j] = shade((70, 70, 76), 2)   # surround
            elif tweeter <= 0.8:
                px[fx + i, fy + j] = shade((150, 150, 158), 2)


def tv_man_head():
    # TV set 12x9x7 at (0, 32)
    box(0, 32, 12, 9, 7, (56, 56, 62), 4)
    fx, fy = 7, 39
    rect(fx + 1, fy + 1, 10, 7, (40, 90, 170), 6)        # screen
    for y in range(fy + 2, fy + 8, 2):
        rect(fx + 1, y, 10, 1, (62, 122, 212), 6)        # scanlines
    rect(fx + 9, fy + 8, 2, 1, (170, 170, 176), 0)       # knobs

    # Antenna 1x3x1 at (40, 32), used for both antennas
    box(40, 32, 1, 3, 1, (175, 175, 182), 4)


def alliance(seed, head):
    new_image(64, seed)
    suit()
    head()
    return img


def large_alliance(head, suit_color, tie_color):
    new_image(64, 9)
    suit(suit_color, tie_color)
    head()
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
    "entity/gman_toilet.png": lambda: toilet(8, gman_head),
    "entity/big_skibidi_toilet.png": lambda: toilet(13, big_head),
    "entity/scientist_toilet.png": lambda: toilet(14, scientist_head),
    "entity/skibidi_urinal.png": urinal,
    "entity/flying_skibidi_toilet.png": lambda: toilet(15, flying_head),
    "entity/giant_flying_skibidi_toilet.png": lambda: toilet(16, giant_flying_head),
    "entity/dj_skibidi_toilet.png": lambda: toilet(17, dj_head, headphones),
    "entity/spider_toilet.png": lambda: toilet(7, skibidi_head, spider_legs),
    "entity/jetpack_toilet.png": lambda: toilet(7, skibidi_head, jetpack),
    "entity/cameraman.png": lambda: alliance(9, cameraman_head),
    "entity/speakerman.png": lambda: alliance(9, speakerman_head),
    "entity/tv_man.png": lambda: alliance(9, tv_man_head),
    "entity/commander.png": commander,
    "entity/large_cameraman.png": lambda: large_alliance(cameraman_head, (34, 44, 66), (150, 155, 165)),
    "entity/large_speakerman.png": lambda: large_alliance(speakerman_head, (78, 56, 40), (20, 20, 22)),
    "entity/camera_strider.png": camera_strider,
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
