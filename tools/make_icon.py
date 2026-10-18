"""Draws the mod icon (assets/spawnalerts/icon.png): a speech balloon with an exclamation mark and a
sparkle, as 32x32 pixel art scaled to 128x128. Drawn from scratch, so it is MIT like the rest of
the mod.

    py -I tools/make_icon.py src/main/resources/assets/spawnalerts/icon.png
"""
import sys

from PIL import Image

SIZE = 32
SCALE = 4

OUTLINE = (40, 36, 60, 255)
BALLOON = (250, 248, 240, 255)
SHADE = (206, 200, 222, 255)
MARK = (232, 64, 52, 255)
SPARK = (255, 214, 64, 255)


def main(out: str) -> None:
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()

    # Rounded balloon from (3,4) to (24,20), shaded at the bottom.
    left, top, right, bottom = 3, 4, 24, 20
    for y in range(top, bottom + 1):
        for x in range(left, right + 1):
            corner = (x in (left, right)) and (y in (top, bottom))
            if corner:
                continue
            edge = x in (left, right) or y in (top, bottom) or ((x in (left + 1, right - 1)) and (y in (top, bottom)))
            px[x, y] = OUTLINE if edge else (SHADE if y >= bottom - 2 else BALLOON)
    # Rounded corners.
    for x, y in [(left + 1, top + 1), (right - 1, top + 1), (left + 1, bottom - 1), (right - 1, bottom - 1)]:
        px[x, y] = OUTLINE

    # Tail at the bottom left.
    for x, y in [(7, 21), (8, 21), (7, 22), (6, 23), (8, 22)]:
        px[x, y] = OUTLINE
    px[8, 20] = SHADE
    px[9, 20] = SHADE

    # Exclamation mark.
    for y in range(7, 15):
        px[13, y] = MARK
        px[14, y] = MARK
    for y in (16, 17):
        px[13, y] = MARK
        px[14, y] = MARK

    # Sparkle at the top right: a rare one.
    for x, y in [(27, 3), (27, 4), (27, 5), (26, 4), (28, 4), (27, 2), (27, 6), (25, 4), (29, 4)]:
        px[x, y] = SPARK

    img.resize((SIZE * SCALE, SIZE * SCALE), Image.NEAREST).save(out)
    print(f"wrote {out}")


if __name__ == "__main__":
    main(sys.argv[1])
