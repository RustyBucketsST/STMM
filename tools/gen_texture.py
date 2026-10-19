"""Generates src/main/resources/assets/skibidi/textures/entity/skibidi_toilet.png.

UV offsets must match SkibidiToiletModel. For a box at uv (u, v) with size (w, h, d):
top (u+d, v) w x d, front (u+d, v+d) w x h, sides (u, v+d) and (u+d+w, v+d) d x h,
back (u+2d+w, v+d) w x h, bottom (u+d+w, v) w x d.
"""
import random
import sys
from pathlib import Path

from PIL import Image

random.seed(7)
img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
px = img.load()


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


PORCELAIN = (238, 240, 242)
SHADOW = (196, 200, 206)
SKIN = (224, 172, 120)
HAIR = (70, 45, 30)

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

out = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parent.parent / \
    "src/main/resources/assets/skibidi/textures/entity/skibidi_toilet.png"
img.save(out)
print("wrote", out)
