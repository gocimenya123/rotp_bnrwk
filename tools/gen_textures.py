#!/usr/bin/env python3
"""
Generates PLACEHOLDER textures for the addon (the Stand model/skin itself: tools/gen_stand.py) (requires Pillow):
    python3 tools/gen_textures.py
Output goes to src/main/resources/assets/rotp_bnrwk/textures/.
Replace them with hand-painted art when you have it - the UV layout used by BnrwkModel / ClockBombModel is:

  stand (128x128)   0..63 x 0..63   humanoid base (skin layout), stitches are drawn over it
                    0..30 x 64..90  shoebill bill / hook / lower mandible
                    32..44 x 64..80 head discs
                    64..100 x 64..74 cloak mantle,  64..116 x 80..95 cloak strips
  clock_bomb (32x32)  6..12 x 0..6  clock face (outward side),  0..8 x 8..13 pin,  0..8 x 16..19 hand
"""
import os, random
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "rotp_bnrwk", "textures")
random.seed(7)

def save(img, *path):
    p = os.path.join(ROOT, *path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)

def jitter(c, a=8):
    return tuple(max(0, min(255, v + random.randint(-a, a))) for v in c)

def noise_fill(img, box, base, amt=8):
    x0, y0, x1, y1 = box
    px = img.load()
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = jitter(base, amt) + (255,)

# ------------------------------------------------------------------ stand
def stand():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    skin, skin_dark = (186, 178, 150), (92, 84, 70)
    noise_fill(img, (0, 0, 64, 64), skin, 7)                     # humanoid base
    noise_fill(img, (0, 64, 30, 90), (120, 132, 140), 6)         # bill (slate grey, shoebill)
    noise_fill(img, (20, 64, 30, 72), (84, 78, 66), 4)           # hooked tip, darker
    noise_fill(img, (32, 64, 48, 80), (176, 150, 84), 5)         # head discs (brass)
    noise_fill(img, (64, 64, 100, 74), (62, 48, 40), 6)          # cloak mantle
    noise_fill(img, (64, 80, 118, 96), (54, 42, 36), 8)          # cloak strips (tattered)
    d = ImageDraw.Draw(img)
    # stitches: short dashes crossed by small ticks over the skin area
    for _ in range(46):
        x, y = random.randint(1, 60), random.randint(1, 60)
        horiz = random.random() < 0.5
        n = random.randint(3, 6)
        for i in range(n):
            if horiz:
                d.point((x + i * 2, y), skin_dark + (255,)); d.point((x + i * 2, y - 1), skin_dark + (255,))
            else:
                d.point((x, y + i * 2), skin_dark + (255,)); d.point((x - 1, y + i * 2), skin_dark + (255,))
    # disc detail: dark centre dot / rim
    d.rectangle((33, 65, 35, 67), outline=(110, 88, 40, 255))
    # cloak tears: transparent-ish dark notches along the strips' lower edge
    for x in range(64, 118, 3):
        y = random.randint(90, 95)
        d.line((x, y, x, 95), fill=(30, 24, 20, 255))
    save(img, "entity", "stand", "bnrwk.png")

# ------------------------------------------------------------------ clock bomb
def clock_bomb():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 17, 7), fill=(150, 150, 154, 255))        # sides of the face box
    d.rectangle((6, 0, 11, 5), fill=(235, 228, 205, 255), outline=(40, 36, 34, 255))   # clock face (outward side)
    for p in [(8, 0), (8, 5), (6, 3), (11, 3)]:
        d.point(p, (30, 26, 24, 255))                            # 12/6/9/3 ticks
    d.point((8, 2), (200, 30, 30, 255)); d.point((8, 3), (30, 26, 24, 255))
    d.rectangle((0, 8, 8, 13), fill=(205, 170, 60, 255))         # brass pin
    d.rectangle((0, 16, 8, 19), fill=(30, 26, 24, 255))          # hand
    save(img, "entity", "clock_bomb.png")

# ------------------------------------------------------------------ icons
def icon_base(col):
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((1, 1, 30, 30), radius=5, fill=col + (255,), outline=(20, 20, 24, 255), width=2)
    return img, d

def clock(d, cx, cy, r, hand=True):
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(238, 232, 210, 255), outline=(25, 22, 22, 255), width=1)
    if hand:
        d.line((cx, cy, cx, cy - r + 2), fill=(25, 22, 22, 255)); d.line((cx, cy, cx + r - 3, cy), fill=(200, 30, 30, 255))

def icons():
    # punch
    img, d = icon_base((84, 98, 76)); d.rectangle((9, 10, 22, 22), fill=(205, 198, 170, 255), outline=(30, 30, 30, 255)); d.line((9, 16, 22, 16), fill=(60, 56, 48, 255))
    save(img, "action", "bnrwk_punch.png")
    # barrage
    img, d = icon_base((84, 98, 76))
    for i, (x, y) in enumerate([(6, 6), (13, 12), (6, 17), (14, 22)]):
        d.rectangle((x, y, x + 9, y + 5), fill=(205, 198, 170, 255), outline=(30, 30, 30, 255))
    save(img, "action", "bnrwk_barrage.png")
    # block
    img, d = icon_base((84, 98, 76)); d.polygon([(16, 5), (26, 9), (24, 21), (16, 27), (8, 21), (6, 9)], fill=(150, 160, 168, 255), outline=(30, 30, 30, 255))
    save(img, "action", "bnrwk_block.png")
    # plant bomb: one clock with a pin
    img, d = icon_base((120, 60, 50)); clock(d, 16, 14, 9); d.rectangle((15, 22, 17, 28), fill=(205, 170, 60, 255))
    save(img, "action", "bnrwk_plant_bomb.png")
    # bomb throw: three clocks fanning out
    img, d = icon_base((120, 60, 50))
    for cx, cy in [(9, 20), (16, 12), (23, 20)]:
        clock(d, cx, cy, 5, hand=False)
    save(img, "action", "bnrwk_bomb_throw.png")
    # smoke bomb: grey puffs with tiny clocks
    img, d = icon_base((90, 90, 100))
    for cx, cy, r in [(11, 18, 7), (20, 16, 8), (16, 11, 6)]:
        d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(176, 176, 184, 255))
    clock(d, 12, 18, 3, hand=False); clock(d, 21, 15, 3, hand=False)
    save(img, "action", "bnrwk_smoke_bomb.png")
    # item bomb: a sword-ish item with a clock pin
    img, d = icon_base((120, 60, 50)); d.line((8, 24, 22, 8), fill=(190, 196, 205, 255), width=3); clock(d, 21, 21, 6)
    save(img, "action", "bnrwk_item_bomb.png")
    # entity bomb: crosshair over a target with a clock pin
    img, d = icon_base((120, 60, 50))
    d.ellipse((7, 7, 25, 25), outline=(230, 230, 230, 255), width=2)
    d.line((16, 3, 16, 29), fill=(230, 230, 230, 255)); d.line((3, 16, 29, 16), fill=(230, 230, 230, 255))
    clock(d, 16, 16, 5, hand=False)
    save(img, "action", "bnrwk_entity_bomb.png")
    # power icon (HUD / stand list)
    img, d = icon_base((140, 154, 123))
    d.ellipse((7, 8, 24, 25), fill=(186, 178, 150, 255), outline=(30, 30, 30, 255))     # head
    d.polygon([(14, 15), (30, 17), (28, 22), (14, 21)], fill=(120, 132, 140, 255), outline=(30, 30, 30, 255))  # bill
    d.ellipse((12, 12, 14, 14), fill=(20, 20, 20, 255))
    d.ellipse((9, 10, 13, 14), outline=(176, 150, 84, 255))                              # disc
    save(img, "power", "bnrwk.png")

if __name__ == "__main__":
    clock_bomb(); icons()   # the Stand skin + model come from tools/gen_stand.py
    print("textures written to", os.path.abspath(ROOT))
