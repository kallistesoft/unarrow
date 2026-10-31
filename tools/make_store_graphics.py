"""Draws the Play Store graphics from the app's own screenshot-test references.

Writes, under store/:
- screenshots/<lang>/<n>_<screen>.png: 1080 x 1920 phone screenshots with a caption, in both themes;
- feature-graphic-<lang>.png: the 1024 x 500 feature graphic.

The references are real Compose screens, so they carry no ads. The tests only render light English and dark French;
store/source-screens holds the other two pairs, recorded once with ScreenshotDevice's languages swapped (other tests
fail during that run, as they look for English labels; restore the constants and references afterwards). Rerun after
the screens change. Run: python3 tools/make_store_graphics.py  (needs Pillow).
"""
import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.join(os.path.dirname(__file__), '..')
MANROPE = os.path.join(ROOT, 'core/designsystem/src/main/res/font/manrope.ttf')

NAVY = (0x06, 0x0D, 0x24)
NAVY_LIGHT = (0x0B, 0x1B, 0x52)
PAPER = (0xF6, 0xF8, 0xFD)
PAPER_DEEP = (0xE4, 0xE9, 0xF4)
ACCENT = (0x2F, 0x6B, 0xFF)
AMBER = (0xF2, 0xB1, 0x34)
INK = (0x0B, 0x14, 0x33)
MUTED = (0xB9, 0xC6, 0xE8)

WIDTH, HEIGHT = 1080, 1920
CAPTION_TOP = 90
SHOT_TOP = 330
SHOT_WIDTH = 800
CORNER = 56

# Name, source module (None: store/source-screens), reference, theme, caption.
SCREENS = {
    'en': [
        ('home', 'feature/home', 'home', 'light', 'A new puzzle every day'),
        ('mechanics', None, 'game_mechanics', 'dark', 'Keys, locks and turning arrows'),
        ('won', 'feature/game', 'game_won', 'light', 'Three stars for a perfect run'),
        ('history', None, 'history', 'dark', 'Every puzzle you solved'),
        ('dark', None, 'home', 'dark', 'Light and dark themes'),
    ],
    'fr': [
        ('home', None, 'home', 'light', 'Un nouveau défi chaque jour'),
        ('mechanics', 'feature/game', 'game_mechanics', 'dark', 'Clés, cadenas et flèches qui tournent'),
        ('won', None, 'game_won', 'light', 'Trois étoiles pour un sans-faute'),
        ('history', 'feature/history', 'history', 'dark', 'Tous vos casse-têtes résolus'),
        ('dark', 'feature/home', 'home', 'dark', 'Thèmes clair et sombre'),
    ],
}
TAGLINES = {'en': 'Tap the arrows out.', 'fr': 'Faites sortir les flèches.'}
SUBLINES = {'en': 'A new puzzle every day', 'fr': 'Un nouveau défi chaque jour'}


def font(size, weight=800):
    result = ImageFont.truetype(MANROPE, size)
    result.set_variation_by_axes([weight])
    return result


def gradient(width, height, top, bottom):
    image = Image.new('RGB', (1, height))
    for y in range(height):
        t = y / max(height - 1, 1)
        image.putpixel((0, y), tuple(round(a + (b - a) * t) for a, b in zip(top, bottom)))
    return image.resize((width, height))


def rounded(image, radius):
    mask = Image.new('L', (image.width * 4, image.height * 4), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, mask.width - 1, mask.height - 1), radius * 4, fill=255)
    result = image.convert('RGBA')
    result.putalpha(mask.resize(image.size, Image.LANCZOS))
    return result


def paste_with_shadow(canvas, image, position, colour):
    shadow = Image.new('RGBA', canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(
        (position[0], position[1] + 12, position[0] + image.width, position[1] + image.height + 12),
        CORNER, fill=colour + (110,))
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(30)))
    canvas.alpha_composite(image, position)


def wrapped(draw, text, typeface, max_width):
    lines = ['']
    for word in text.split():
        candidate = (lines[-1] + ' ' + word).strip()
        if draw.textlength(candidate, font=typeface) <= max_width:
            lines[-1] = candidate
        else:
            lines.append(word)
    return lines


def screenshot(language, index, name, module, reference, theme, caption):
    folder = f'{module}/src/test/screenshots' if module else 'store/source-screens'
    source = Image.open(os.path.join(ROOT, folder, f'{reference}_{theme}_{language}.png')).convert('RGB')
    dark = theme == 'dark'
    canvas = gradient(WIDTH, HEIGHT, *((NAVY, NAVY_LIGHT) if dark else (PAPER, PAPER_DEEP))).convert('RGBA')
    draw = ImageDraw.Draw(canvas)
    typeface = font(74)
    y = CAPTION_TOP
    lines = wrapped(draw, caption, typeface, WIDTH - 160)
    for line in lines:
        draw.text((WIDTH / 2, y), line, font=typeface, fill=(255, 255, 255) if dark else INK, anchor='ma')
        y += 92
    height = HEIGHT - SHOT_TOP + CORNER
    shot = source.resize((SHOT_WIDTH, round(source.height * SHOT_WIDTH / source.width)), Image.LANCZOS)
    shot = rounded(shot.crop((0, 0, SHOT_WIDTH, min(height, shot.height))), CORNER)
    paste_with_shadow(canvas, shot, ((WIDTH - SHOT_WIDTH) // 2, SHOT_TOP), ACCENT if dark else (0x8F, 0xA3, 0xD6))
    folder = os.path.join(ROOT, 'store/screenshots', language)
    os.makedirs(folder, exist_ok=True)
    canvas.convert('RGB').save(os.path.join(folder, f'{index}_{name}.png'), optimize=True)


def feature_graphic(language):
    canvas = gradient(1024, 500, NAVY, NAVY_LIGHT).convert('RGBA')
    source = Image.open(os.path.join(ROOT, 'feature/game/src/test/screenshots/game_mechanics_dark_fr.png'))
    # The board, as drawn by the game; its position comes from the reference's layout.
    board = source.convert('RGB').crop((36, 568, 786, 1318)).resize((400, 400), Image.LANCZOS)
    glow = Image.new('RGBA', canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(glow).rounded_rectangle((584, 50, 984, 450), 40, fill=ACCENT + (150,))
    canvas.alpha_composite(glow.filter(ImageFilter.GaussianBlur(36)))
    canvas.alpha_composite(rounded(board, 26), (584, 50))
    draw = ImageDraw.Draw(canvas)
    title = font(92)
    draw.text((56, 132), 'UN', font=title, fill=ACCENT)
    draw.text((56 + draw.textlength('UN', font=title), 132), 'ARROW', font=title, fill=AMBER)
    draw.text((58, 262), TAGLINES[language], font=font(42, 700), fill=(255, 255, 255))
    draw.text((58, 324), SUBLINES[language], font=font(30, 500), fill=MUTED)
    canvas.convert('RGB').save(os.path.join(ROOT, f'store/feature-graphic-{language}.png'), optimize=True)


def main():
    for language, screens in SCREENS.items():
        for index, screen in enumerate(screens, start=1):
            screenshot(language, index, *screen)
        feature_graphic(language)


if __name__ == '__main__':
    main()
