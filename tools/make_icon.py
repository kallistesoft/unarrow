"""Draws the Unarrow app icon: a corner of the game's board plus the Kallistesoft brand marks.

Adapted from Block Daily's tools/make_icon.py, which defines the brand marks shared by every Kallistesoft game icon:
the gold sun (top right), the light-blue waves (bottom), and K / S in navy on the hero's top-left and bottom-right
elements. Only the hero and the background change between games. Here the hero is a 3 x 3 corner of Unarrow's board
(tiles in the Nocturne cell colour with bold navy arrows, one tile empty as if its arrow just left) and the
background is Nocturne's navy (--bg of the dark theme, #060d24), on which the sun, the waves and the light tiles all
stand out.

Writes, under app/src:
- main/res/mipmap-<density>/ic_launcher_{background,foreground,monochrome}.png: the adaptive icon layers, with the
  design shrunk into the 72/108 area Android never crops (monochrome: white tiles with the arrows and K / S cut out,
  for themed icons);
- main/res/mipmap-<density>/ic_launcher.png and ic_launcher_round.png: legacy icons (square with rounded corners,
  and a circle);
- debug/res/mipmap-<density>/...: the same on slate, so the Dev build is told apart at a glance;
main/res/drawable-nodpi/splash_icon.png (the foreground layer at full size, sharp on the 240 dp splash icon, which
the system shows on a navy circle); and store/icon-512.png for the Play Console.
Run: python3 tools/make_icon.py  (needs Pillow and macOS's Arial Black).
"""
import math
import os

from PIL import Image, ImageDraw, ImageFont

SIZE = 1024
SS = 4  # supersampling for smooth edges
S = SIZE * SS

# Kallistesoft brand (kallistesoft.ca, and Block Daily's icon).
SUN = (0xF2, 0xB1, 0x34)
WAVE = (0x63, 0xB6, 0xE6)
INK = (0x16, 0x21, 0x3E)  # K and S
FONT = '/System/Library/Fonts/Supplemental/Arial Black.ttf'

# Unarrow, in Nocturne colours.
BACKGROUND = (0x06, 0x0D, 0x24)
DEBUG_BACKGROUND = (0x37, 0x47, 0x4F)
TILE = (0xE4, 0xE9, 0xF4)  # --cell (light)
EMPTY = (0x13, 0x20, 0x4A)  # --cell (dark): the tile an arrow just left
ARROW = (0x0A, 0x13, 0x30)  # --text (light)
UP, RIGHT, DOWN, LEFT = 270, 0, 90, 180
# K and S take the top-left and bottom-right tiles; the empty tile is where a → just slid out.
GRID = [
    ['K', DOWN, UP],
    [RIGHT, RIGHT, None],
    [UP, LEFT, 'S'],
]

# Android shows only the middle 72/108 of an adaptive icon, sometimes as a circle.
ADAPTIVE_SCALE = 72 / 108
# Adaptive layers are 108 dp; legacy icons 48 dp.
DENSITIES = {'mdpi': 1, 'hdpi': 1.5, 'xhdpi': 2, 'xxhdpi': 3, 'xxxhdpi': 4}


class Canvas:
    """Draws in design units (0..1 of the full icon), optionally shrunk around the center."""

    def __init__(self, img, scale=1.0):
        self.img, self.draw, self.scale = img, ImageDraw.Draw(img), scale

    def p(self, v, axis_center=0.5):
        return (axis_center + (v - axis_center) * self.scale) * S

    def len(self, v):
        return v * self.scale * S


def waves(cv, top, color=WAVE):
    amp, pts = 0.02, []
    for i in range(0, 201):
        x = i / 200
        pts.append((cv.p(x), cv.p(top + amp * math.sin(x * 2 * math.pi * 2.5))))
    pts[0], pts[-1] = (0, pts[0][1]), (S, pts[-1][1])
    cv.draw.polygon(pts + [(S, S), (0, S)], fill=color)


def sun(cv, color=SUN):
    cx, cy, r = cv.p(0.79), cv.p(0.19), cv.len(0.055)
    cv.draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=color)


def tile(cv, x, y, c, color):
    """Like the board: a 4% gap and 20% corners."""
    d = c * 0.04
    cv.draw.rounded_rectangle([x + d, y + d, x + c - d, y + c - d], radius=c * 0.2, fill=color)


def arrow(cv, x, y, c, degrees, color):
    """The board's arrow (BoardView.drawArrow), bolder so it reads at 48 dp."""
    cx, cy = x + c / 2, y + c / 2
    a = math.radians(degrees)

    def rot(px, py):
        return (cx + px * math.cos(a) - py * math.sin(a), cy + px * math.sin(a) + py * math.cos(a))

    w = c * 0.15
    shaft = [rot(-c * 0.30, -w / 2), rot(c * 0.06, -w / 2), rot(c * 0.06, w / 2), rot(-c * 0.30, w / 2)]
    cv.draw.polygon(shaft, fill=color)
    tail = rot(-c * 0.30, 0)
    cv.draw.ellipse([tail[0] - w / 2, tail[1] - w / 2, tail[0] + w / 2, tail[1] + w / 2], fill=color)
    cv.draw.polygon([rot(c * 0.34, 0), rot(c * 0.02, -c * 0.24), rot(c * 0.02, c * 0.24)], fill=color)


def letter(cv, x, y, c, ch, color):
    font = ImageFont.truetype(FONT, int(c * 0.62))
    b = cv.draw.textbbox((0, 0), ch, font=font)
    cv.draw.text((x + c / 2 - (b[0] + b[2]) / 2, y + c / 2 - (b[1] + b[3]) / 2), ch, font=font, fill=color)


def hero(cv, tile_color=TILE, ink=ARROW, letters=INK, empty=EMPTY):
    """The board corner, slightly left and down, leaving the top-right corner to the sun."""
    c = cv.len(0.52 / 3)
    left, top = cv.p(0.46) - c * 1.5, cv.p(0.48) - c * 1.5
    for r, row in enumerate(GRID):
        for q, item in enumerate(row):
            x, y = left + q * c, top + r * c
            if item is None:
                if empty is not None:
                    tile(cv, x, y, c, empty)
                continue
            tile(cv, x, y, c, tile_color)
            if isinstance(item, str):
                letter(cv, x, y, c, item, letters)
            else:
                arrow(cv, x, y, c, item, ink)


def downscale(img, size):
    return img.resize((size, size), Image.LANCZOS)


def rounded_mask(size, radius_ratio):
    mask = Image.new('L', (size * SS, size * SS), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size * SS, size * SS], radius=size * SS * radius_ratio, fill=255)
    return mask.resize((size, size), Image.LANCZOS)


def full_icon(background):
    cv = Canvas(Image.new('RGBA', (S, S), background + (255,)))
    waves(cv, 0.80)
    sun(cv)
    hero(cv)
    return cv.img


def layers(background):
    back = Canvas(Image.new('RGBA', (S, S), background + (255,)), ADAPTIVE_SCALE)
    waves(back, 0.80)
    front = Canvas(Image.new('RGBA', (S, S), (0, 0, 0, 0)), ADAPTIVE_SCALE)
    sun(front)
    hero(front)
    # Themed icon: one colour, the system tints it. White tiles with the arrows and K / S cut out.
    mono = Canvas(Image.new('RGBA', (S, S), (0, 0, 0, 0)), ADAPTIVE_SCALE)
    hero(mono, tile_color=(255, 255, 255, 255), ink=(0, 0, 0, 0), letters=(0, 0, 0, 0), empty=None)
    return back.img, front.img, mono.img


def write_mipmaps(res, background):
    full = full_icon(background)
    back, front, mono = layers(background)
    for density, factor in DENSITIES.items():
        folder = os.path.join(res, f'mipmap-{density}')
        os.makedirs(folder, exist_ok=True)
        layer = int(108 * factor)
        downscale(back, layer).convert('RGB').save(os.path.join(folder, 'ic_launcher_background.png'))
        downscale(front, layer).save(os.path.join(folder, 'ic_launcher_foreground.png'))
        downscale(mono, layer).save(os.path.join(folder, 'ic_launcher_monochrome.png'))
        legacy = int(48 * factor)
        icon = downscale(full, legacy)
        square = Image.new('RGBA', (legacy, legacy), (0, 0, 0, 0))
        square.paste(icon, (0, 0), rounded_mask(legacy, 0.22))
        square.save(os.path.join(folder, 'ic_launcher.png'))
        round_icon = Image.new('RGBA', (legacy, legacy), (0, 0, 0, 0))
        round_icon.paste(icon, (0, 0), rounded_mask(legacy, 0.5))
        round_icon.save(os.path.join(folder, 'ic_launcher_round.png'))
    return full


def main():
    root = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
    full = write_mipmaps(os.path.join(root, 'app', 'src', 'main', 'res'), BACKGROUND)
    write_mipmaps(os.path.join(root, 'app', 'src', 'debug', 'res'), DEBUG_BACKGROUND)
    splash = os.path.join(root, 'app', 'src', 'main', 'res', 'drawable-nodpi')
    os.makedirs(splash, exist_ok=True)
    downscale(layers(BACKGROUND)[1], SIZE).save(os.path.join(splash, 'splash_icon.png'))
    downscale(full, 512).convert('RGB').save(os.path.join(root, 'store', 'icon-512.png'))


if __name__ == '__main__':
    main()
