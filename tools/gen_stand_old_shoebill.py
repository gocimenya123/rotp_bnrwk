#!/usr/bin/env python3
"""
Single source of truth for the Stand's look (requires Pillow + numpy):

    python3 tools/gen_stand.py            # writes BnrwkModel.java + stand texture
    python3 tools/gen_stand.py --preview  # also writes tools/stand_preview.png (front / 3-4 / side / back)

PARTS below describe every extra box attached to the humanoid rig's `head` / `body`.
UV space is allocated automatically (shelf packing outside the 64x64 humanoid skin area) and the
texture of each box is painted from its material, so the Java model and the PNG can never drift apart.
"""
import math, os, random, sys
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "src", "main")
TEX_OUT = os.path.join(ROOT, "resources", "assets", "rotp_bnrwk", "textures", "entity", "stand", "bnrwk.png")
JAVA_OUT = os.path.join(ROOT, "java", "com", "github", "yourname", "rotp_bnrwk", "client", "render", "BnrwkModel.java")
random.seed(11)

# ------------------------------------------------------------------------------------------------ model spec
# part: name, parent ("head"/"body"/other part name), pos, rot (x,y,z radians), boxes [(x,y,z,w,h,d,inflate,material)]
PARTS = []
def part(name, parent, pos=(0, 0, 0), rot=(0, 0, 0), boxes=()):
    PARTS.append(dict(name=name, parent=parent, pos=pos, rot=rot, boxes=list(boxes)))

# ---- head (box spans x -4..4, y -8..0, z -4..4; front is -z)
part("brow", "head", (0, -6.1, -4), boxes=[(-4.25, -0.5, -0.55, 8.5, 1.0, 1.1, 0, "cloak")])
part("billBase", "head", (0, -3, -4), boxes=[(-2.3, -2.1, -3.0, 4.6, 3.6, 3.4, 0, "bill")])
part("billMid", "billBase", (0, 0, -3.0), boxes=[(-1.95, -1.8, -3.2, 3.9, 3.1, 3.4, 0, "billnostril")])
part("billTip", "billMid", (0, 0.1, -3.2), rot=(0.1, 0, 0), boxes=[(-1.45, -1.5, -2.7, 2.9, 2.5, 2.9, 0, "bill")])
part("billHook", "billTip", (0, 0.5, -2.6), rot=(0.65, 0, 0), boxes=[(-0.95, -0.5, -0.5, 1.9, 2.4, 1.3, 0, "billtip")])
part("billRidge", "billBase", (0, 0, 0), boxes=[(-0.55, -2.45, -9.0, 1.1, 0.7, 9.0, 0, "billdark")])
part("jaw", "head", (0, -0.9, -4), boxes=[(-1.7, 0, -6.6, 3.4, 1.1, 6.8, 0, "bill"),
                                           (-1.4, 1.0, -5.0, 2.8, 1.5, 4.6, 0, "pouch")])
part("crest", "head", (0, -8, 2.2))
for i in (-2, -1, 0, 1, 2):
    part("crest%d" % (i + 2), "crest", (i * 1.35, 0, 0.35 * abs(i)), rot=(-0.85 - 0.12 * (2 - abs(i)), 0, i * 0.2),
         boxes=[(-0.55, -4.2 - (2 - abs(i)) * 0.7, -0.35, 1.1, 4.2 + (2 - abs(i)) * 0.7, 0.7, 0, "feather")])
for s, name in ((1, "R"), (-1, "L")):
    part("disc" + name, "head", (s * 4.3, -4.6, 0.8),
         boxes=[(-0.5, -2.6, -1.5, 1.0, 5.2, 3.0, 0, "disc"), (-0.5, -1.5, -2.6, 1.0, 3.0, 5.2, 0, "disc"),
                ((0.45 if s > 0 else -1.15), -0.8, -0.8, 0.7, 1.6, 1.6, 0, "brasshi")])

# ---- body (box spans x -4..4, y 0..12, z -2..2)
part("ruff", "body", (0, 0, 0), boxes=[(-3.7, -1.3, -3.0, 7.4, 1.7, 6.0, 0, "feather")])
part("mantle", "body", (0, 0, 0), boxes=[(-5.6, -0.4, -3.1, 11.2, 2.4, 6.2, 0, "cloak")])
for i, h in enumerate((1.0, 1.9, 0.9, 1.6)):
    part("mantleEdge%d" % i, "body", (0, 0, 0), boxes=[(-6.1 + i * 3.05, 1.9, -3.5, 3.05, h, 7.0, 0, "cloaklight")])
for s, name in ((1, "R"), (-1, "L")):
    part("pauldron" + name, "body", (s * 5.5, 0.1, 0), rot=(0, 0, s * 0.28),
         boxes=[(-1.4, -1.1, -2.5, 2.8, 1.7, 5.0, 0, "brass"), (-0.9, -1.6, -1.6, 1.8, 0.6, 3.2, 0, "brasshi")])
for i, ln in enumerate((10.0, 13.0, 8.0, 12.0, 9.0, 14.0, 7.5)):
    part("strip%d" % i, "body", (-4.5 + i * 1.5, 2.2, 2.2), rot=(0.07 + 0.025 * (i % 3), 0, (0.05 if i % 2 == 0 else -0.05)),
         boxes=[(-0.85, 0, 0, 1.7, ln, 0.8, 0, "cloak")])
part("belt", "body", (0, 9.7, 0), boxes=[(-4.3, 0, -2.3, 8.6, 1.7, 4.6, 0, "belt"),
                                          (-1.5, -0.25, -2.75, 3.0, 2.2, 0.5, 0, "clockface")])
for i, x in enumerate((-3.4, -2.3, 2.3, 3.4)):
    part("beltPin%d" % i, "body", (x, 11.4, -2.4), rot=(0, 0, 0.05 * (1 if x > 0 else -1)),
         boxes=[(-0.25, 0, -0.25, 0.5, 2.2, 0.5, 0, "belt"), (-0.55, 2.0, -0.45, 1.1, 1.3, 0.9, 0, "brasshi")])
part("patch", "body", (0, 2.6, -2), boxes=[(-2.2, 0, -0.5, 4.4, 4.4, 0.55, 0, "patch")])
part("gear", "body", (0, 5.6, 2.9), boxes=[(-2.6, -2.6, 0, 5.2, 5.2, 0.7, 0, "gear")])
part("gear2", "gear", (0, 0, 0), rot=(0, 0, math.pi / 4), boxes=[(-2.6, -2.6, 0, 5.2, 5.2, 0.7, 0, "gear")])
part("hub", "gear", (0, 0, 0), boxes=[(-0.9, -0.9, 0.6, 1.8, 1.8, 0.6, 0, "brasshi")])

# ------------------------------------------------------------------------------------------------ UV packing
TEX = 128
def ceil(v): return int(math.ceil(v - 1e-6))

def footprint(b):
    x, y, z, w, h, d = b[:6]
    return 2 * ceil(d) + 2 * ceil(w), ceil(d) + ceil(h)

def pack():
    # free areas: x 0..63 / y 64..127, then x 64..127 / y 0..127  (humanoid skin owns 0..63 x 0..63)
    shelves = [dict(x0=0, x1=64, y=64, ymax=128, cx=0, rowh=0), dict(x0=64, x1=128, y=0, ymax=128, cx=64, rowh=0)]
    items = [(footprint(b), pi, bi) for pi, p in enumerate(PARTS) for bi, b in enumerate(p["boxes"])]
    items.sort(key=lambda t: -t[0][1])
    for (w, h), pi, bi in items:
        for sh in shelves:
            if sh["cx"] + w > sh["x1"]:
                sh["y"] += sh["rowh"]; sh["cx"] = sh["x0"]; sh["rowh"] = 0
            if sh["y"] + h <= sh["ymax"] and sh["cx"] + w <= sh["x1"]:
                PARTS[pi]["boxes"][bi] = PARTS[pi]["boxes"][bi][:8] + (sh["cx"], sh["y"])
                sh["cx"] += w; sh["rowh"] = max(sh["rowh"], h)
                break
        else:
            raise SystemExit("texture atlas full")

# ------------------------------------------------------------------------------------------------ painting
def jit(c, a):
    return tuple(max(0, min(255, v + random.randint(-a, a))) for v in c[:3]) + (255,)

def fill(img, box, base, amt=6):
    px = img.load()
    for y in range(box[1], box[3]):
        for x in range(box[0], box[2]):
            px[x, y] = jit(base, amt)

def faces(u, v, w, h, d):
    """Texel rects (x0,y0,x1,y1) of the six faces, in Minecraft's box-UV layout."""
    w, h, d = ceil(w), ceil(h), ceil(d)
    return {
        "right": (u, v + d, u + d, v + d + h), "front": (u + d, v + d, u + d + w, v + d + h),
        "left": (u + d + w, v + d, u + 2 * d + w, v + d + h), "back": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
        "top": (u + d, v, u + d + w, v + d), "bottom": (u + d + w, v, u + d + 2 * w, v + d),
    }

MAT = {
    "bill": ((122, 134, 142), 5), "billnostril": ((122, 134, 142), 5), "billdark": ((70, 78, 86), 4),
    "billtip": ((46, 44, 44), 3), "pouch": ((176, 144, 136), 6), "feather": ((98, 112, 82), 7),
    "disc": ((176, 142, 62), 5), "brasshi": ((232, 202, 112), 5), "brass": ((162, 130, 58), 6),
    "cloak": ((64, 48, 40), 6), "cloaklight": ((98, 78, 62), 8), "belt": ((72, 46, 32), 5),
    "clockface": ((236, 228, 204), 3), "patch": ((156, 148, 122), 6), "gear": ((150, 124, 58), 6),
}

def paint_box(img, d, b):
    x, y, z, w, h, dp, infl, mat, u, v = b
    base, amt = MAT[mat]
    fr = faces(u, v, w, h, dp)
    fill(img, (u, v, u + 2 * ceil(dp) + 2 * ceil(w), v + ceil(dp) + ceil(h)), base, amt)
    dark = tuple(int(c * 0.6) for c in base) + (255,)
    light = tuple(min(255, int(c * 1.25)) for c in base) + (255,)
    if mat == "feather":      # vertical barbs
        for name, r in fr.items():
            for xx in range(r[0], r[2], 2):
                d.line((xx, r[1], xx, r[3] - 1), fill=dark)
    elif mat == "billnostril":
        for name in ("left", "right"):
            r = fr[name]; d.rectangle((r[0] + 1, r[1] + 1, r[0] + 2, r[1] + 1), fill=(24, 26, 30, 255))
        r = fr["top"]
        d.line((r[0], r[1], r[0], r[3] - 1), fill=dark); d.line((r[2] - 1, r[1], r[2] - 1, r[3] - 1), fill=dark)
    elif mat in ("bill", "billtip", "pouch"):
        for name in ("front", "back", "left", "right"):
            r = fr[name]; d.line((r[0], r[3] - 1, r[2] - 1, r[3] - 1), fill=dark)
    elif mat == "clockface":
        r = fr["front"]
        cx, cy = (r[0] + r[2]) // 2, (r[1] + r[3]) // 2
        d.rectangle((r[0], r[1], r[2] - 1, r[3] - 1), outline=(40, 34, 30, 255))
        d.point((cx, r[1] + 1), (30, 26, 24, 255)); d.point((cx, r[3] - 2), (30, 26, 24, 255))
        d.line((cx, cy, cx, cy - 1), fill=(30, 26, 24, 255)); d.point((cx + 1, cy), (200, 30, 30, 255))
    elif mat == "patch":
        r = fr["front"]
        d.rectangle((r[0], r[1], r[2] - 1, r[3] - 1), outline=(74, 62, 50, 255))
        d.line((r[0] + 1, r[1] + 1, r[2] - 2, r[3] - 2), fill=(60, 48, 40, 255))
        d.line((r[2] - 2, r[1] + 1, r[0] + 1, r[3] - 2), fill=(60, 48, 40, 255))
    elif mat == "gear":
        for name in ("front", "back"):
            r = fr[name]
            for xx in range(r[0], r[2], 2):
                d.point((xx, r[1]), dark); d.point((xx, r[3] - 1), dark)
            d.rectangle((r[0] + 2, r[1] + 2, r[2] - 3, r[3] - 3), outline=dark)
    elif mat in ("cloak", "cloaklight"):
        for name in ("front", "back", "left", "right"):
            r = fr[name]
            for xx in range(r[0], r[2], 3):
                d.line((xx, r[3] - 2, xx, r[3] - 1), fill=(24, 18, 14, 255))
            d.line((r[0], r[1] + 2, r[2] - 1, r[1] + 2), fill=dark)  # seam

def stitch_h(d, x0, x1, y, col):
    for x in range(x0, x1, 2):
        d.point((x, y), col)
        if x % 4 == 0:
            d.point((x, y - 1), col); d.point((x, y + 1), col)

def stitch_v(d, x, y0, y1, col):
    for y in range(y0, y1, 2):
        d.point((x, y), col)
        if y % 4 == 0:
            d.point((x - 1, y), col); d.point((x + 1, y), col)

def paint_base(img, d):
    """Humanoid skin (64x64): patchwork stitched hide, yellow eyes, bandaged arms/legs, belt."""
    thread = (58, 46, 38, 255)
    skin = (180, 174, 146)
    fill(img, (0, 0, 64, 64), skin, 6)
    # patchwork: random tone rectangles over the whole sheet
    for _ in range(26):
        x, y = random.randint(0, 58), random.randint(0, 58)
        w, h = random.randint(3, 8), random.randint(3, 7)
        tone = random.choice([(166, 170, 140), (190, 184, 156), (174, 166, 142)])
        for yy in range(y, min(64, y + h)):
            for xx in range(x, min(64, x + w)):
                img.putpixel((xx, yy), jit(tone, 5))
        if random.random() < 0.35:
            stitch_h(d, x, min(64, x + w), y, thread)
        if random.random() < 0.2:
            stitch_v(d, x, y, min(64, y + h), thread)
    # ---- head
    fill(img, (8, 0, 16, 8), (112, 124, 96), 6)                      # top: feathery
    for xx in range(8, 16, 2):
        d.line((xx, 0, xx, 7), fill=(80, 92, 70, 255))
    for (x0, y0) in ((9, 11), (13, 11)):                              # eyes on the front face
        d.rectangle((x0 - 1, y0 - 1, x0 + 1, y0 - 1), fill=(36, 30, 26, 255))   # heavy lid
        d.rectangle((x0 - 1, y0, x0 + 1, y0 + 1), fill=(238, 204, 64, 255))
        d.point((x0 + (1 if x0 < 12 else -1), y0), (10, 10, 10, 255)); d.point((x0 + (1 if x0 < 12 else -1), y0 + 1), (10, 10, 10, 255))
    stitch_v(d, 15, 9, 16, thread); stitch_h(d, 8, 16, 15, thread)
    for (u0, u1) in ((0, 8), (16, 24)):                               # side faces: eye bump + seam
        d.point((u0 + (6 if u0 == 0 else 1), 11), (238, 204, 64, 255))
        stitch_h(d, u0, u1, 14, thread)
    stitch_v(d, 28, 8, 16, thread)                                    # back of head seam
    # ---- body (16,16): front 20..28 x 20..32
    for name_u in (16, 20, 28, 32):
        pass
    stitch_v(d, 24, 20, 31, thread)                                   # chest seam
    for yy in range(21, 30, 3):
        d.line((21, yy, 27, yy), fill=(150, 142, 118, 255))           # ribs
        stitch_h(d, 21, 27, yy + 1, thread)
    fill(img, (16, 30, 40, 32), (72, 46, 32), 4)                      # belt row all around
    stitch_h(d, 16, 40, 30, (30, 22, 16, 255))
    fill(img, (20, 16, 36, 20), (140, 140, 112), 6)                   # shoulders / hips top-bottom
    # ---- arms: bandages on forearms, dark gloves   right arm (40,16): u 40..56, rows 20..32
    for (ux, vy) in ((40, 20), (32, 52)):
        for yy in range(vy + 5, vy + 10):                             # bandage
            for xx in range(ux, ux + 16):
                img.putpixel((xx, yy), jit((222, 214, 188), 4) if (xx + yy) % 5 else jit((178, 168, 142), 4))
        for yy in range(vy + 10, vy + 12):                            # glove
            for xx in range(ux, ux + 16):
                img.putpixel((xx, yy), jit((58, 44, 36), 3))
        stitch_h(d, ux, ux + 16, vy + 3, thread)
    # ---- legs: wraps + boots   right leg (0,16): rows 20..32, left leg (16,48): rows 52..64
    for (ux, vy) in ((0, 20), (16, 52)):
        for yy in range(vy + 6, vy + 9):
            for xx in range(ux, ux + 16):
                img.putpixel((xx, yy), jit((214, 206, 180), 4) if (xx + yy) % 4 else jit((170, 160, 134), 4))
        for yy in range(vy + 9, vy + 12):
            for xx in range(ux, ux + 16):
                img.putpixel((xx, yy), jit((62, 46, 36), 3))
        stitch_h(d, ux, ux + 16, vy + 3, thread)
    # underside faces of limbs stay plain; keep top faces dark so the shoulders blend with the mantle
    fill(img, (44, 16, 52, 20), (110, 100, 84), 5); fill(img, (36, 48, 44, 52), (110, 100, 84), 5)

# ------------------------------------------------------------------------------------------------ outputs
def build_texture():
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    paint_base(img, d)
    for p in PARTS:
        for b in p["boxes"]:
            paint_box(img, d, b)
    os.makedirs(os.path.dirname(TEX_OUT), exist_ok=True)
    img.save(TEX_OUT)
    return img

def f(v):
    s = ("%.3f" % v).rstrip("0").rstrip(".")
    return (s if s not in ("", "-0") else "0") + "F"

def write_java():
    L = []
    L.append("""package com.github.yourname.rotp_bnrwk.client.render;

import com.github.standobyte.jojo.client.render.entity.model.stand.HumanoidStandModel;
import com.github.yourname.rotp_bnrwk.entity.stand.BnrwkEntity;

import net.minecraft.client.renderer.model.ModelRenderer;

/**
 * Boku no Rhythm wo Kiitekure.
 *
 * GENERATED by tools/gen_stand.py - edit the PARTS list there and re-run the script (it also repaints the texture).
 *
 * Uses RotP's humanoid rig (summon / idle / punch / barrage / block animations come for free) and attaches
 * extra boxes to its head and body: shoebill bill with hooked tip and throat pouch, heavy brow, feather crest,
 * two brass discs, feather ruff, tattered double mantle, brass pauldrons, ragged back strips, belt with a clock buckle
 * and hanging pins, stitched chest patch and a clockwork gear on the back.
 * Limb details (bandages, gloves, boots, stitches, eyes) are painted on the humanoid skin.
 */
public class BnrwkModel extends HumanoidStandModel<BnrwkEntity> {

    public BnrwkModel() {
        super();
        addHumanoidBaseBoxes(null);
        texWidth = 128;
        texHeight = 128;
""")
    names = {"head": "head", "body": "body"}
    for p in PARTS:
        n = p["name"]; names[n] = n
        L.append("        ModelRenderer %s = child(%s, %s, %s, %s);" % (n, names[p["parent"]], f(p["pos"][0]), f(p["pos"][1]), f(p["pos"][2])))
        for ax, val in zip("xyz", p["rot"]):
            if abs(val) > 1e-6:
                L.append("        %s.%sRot = %s;" % (n, ax, f(val)))
        for b in p["boxes"]:
            x, y, z, w, h, dp, infl, mat, u, v = b
            L.append("        %s.texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s, %s);" % (n, u, v, f(x), f(y), f(z), f(w), f(h), f(dp), f(infl)))
    L.append("""    }

    /**
     * During a barrage the humanoid pose twists / leans the torso and head with every punch, which shakes the whole
     * model (cloak, bill, gear...). The torso and head are held still here, so only the arms flurry.
     */
    @Override
    public void setupAnim(BnrwkEntity entity, float walkAnimPos, float walkAnimSpeed, float ticks,
                          float yRotationOffset, float xRotation) {
        super.setupAnim(entity, walkAnimPos, walkAnimSpeed, ticks, yRotationOffset, xRotation);
        if (entity.getStandPose() == StandPose.BARRAGE) {
            hold(body);
            hold(head);
        }
    }

    private static void hold(ModelRenderer part) {
        part.xRot = 0F;
        part.yRot = 0F;
        part.zRot = 0F;
    }

    /** Creates an empty ModelRenderer attached to {@code parent} at the given local position. */
    private ModelRenderer child(ModelRenderer parent, float x, float y, float z) {
        ModelRenderer part = new ModelRenderer(this);
        part.setPos(x, y, z);
        parent.addChild(part);
        return part;
    }
}
""")
    open(JAVA_OUT, "w").write("\n".join(L))

# ------------------------------------------------------------------------------------------------ preview
def preview(tex):
    import numpy as np
    S = 12
    base_boxes = [  # (parent transform handled by caller) approximations of the humanoid rig for context
        ("head", (-4, -8, -4, 8, 8, 8), (0, 0)), ("body", (-4, 0, -2, 8, 12, 4), (16, 16)),
        ("rarm", (-8, -2, -2, 4, 12, 4), (40, 16)), ("larm", (4, -2, -2, 4, 12, 4), (32, 48)),
        ("rleg", (-3.9, 12, -2, 4, 12, 4), (0, 16)), ("lleg", (-0.1, 12, -2, 4, 12, 4), (16, 48)),
    ]
    T = np.array(tex.convert("RGBA"))

    def rot(m, rx, ry, rz):
        def R(a, ax):
            c, s = math.cos(a), math.sin(a)
            if ax == "x": return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])
            if ax == "y": return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])
            return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])
        return m @ R(rz, "z") @ R(ry, "y") @ R(rx, "x")

    # world transforms (rotation matrix, translation) per part
    xf = {"head": (np.eye(3), np.zeros(3)), "body": (np.eye(3), np.zeros(3))}
    for p in PARTS:
        Rm, t = xf[p["parent"]]
        pos = np.array(p["pos"], float)
        xf[p["name"]] = (rot(Rm, *p["rot"]), t + Rm @ pos)
    items = []  # (R, t, box(x,y,z,w,h,d,infl), uv)
    for nm, (x, y, z, w, h, d), (u, v) in base_boxes:
        items.append((np.eye(3), np.zeros(3), (x, y, z, w, h, d, 0), (u, v)))
    for p in PARTS:
        Rm, t = xf[p["name"]]
        for b in p["boxes"]:
            items.append((Rm, t, b[:7], (b[8], b[9])))

    def render(yaw, W=330, H=420):
        col = np.zeros((H, W, 4), np.uint8); col[:] = (36, 38, 46, 255)
        zb = np.full((H, W), 1e9)
        cy, sy = math.cos(yaw), math.sin(yaw)
        Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
        for Rm, t, (x, y, z, w, h, d, infl), (u, v) in items:
            x0, x1, y0, y1, z0, z1 = x - infl, x + w + infl, y - infl, y + h + infl, z - infl, z + d + infl
            fw, fh, fd = ceil(w), ceil(h), ceil(d)
            # face: (origin, axis_a, axis_b, uv rect (u0,v0,u1,v1), flipA, flipB)
            faces_ = {
                "front":  ((x0, y0, z0), (w + 2 * infl, 0, 0), (0, h + 2 * infl, 0), (u + fd, v + fd, u + fd + fw, v + fd + fh)),
                "back":   ((x1, y0, z1), (-(w + 2 * infl), 0, 0), (0, h + 2 * infl, 0), (u + 2 * fd + fw, v + fd, u + 2 * fd + 2 * fw, v + fd + fh)),
                "right":  ((x0, y0, z1), (0, 0, -(d + 2 * infl)), (0, h + 2 * infl, 0), (u, v + fd, u + fd, v + fd + fh)),
                "left":   ((x1, y0, z0), (0, 0, d + 2 * infl), (0, h + 2 * infl, 0), (u + fd + fw, v + fd, u + 2 * fd + fw, v + fd + fh)),
                "top":    ((x0, y0, z1), (w + 2 * infl, 0, 0), (0, 0, -(d + 2 * infl)), (u + fd, v, u + fd + fw, v + fd)),
                "bottom": ((x0, y1, z0), (w + 2 * infl, 0, 0), (0, 0, d + 2 * infl), (u + fd + fw, v, u + fd + 2 * fw, v + fd)),
            }
            for fn, (o, A, B, (tu0, tv0, tu1, tv1)) in faces_.items():
                o, A, B = np.array(o, float), np.array(A, float), np.array(B, float)
                nu, nv = max(1, int(tu1 - tu0)), max(1, int(tv1 - tv0))
                n = 8
                for iu in range(nu * n):
                    a = (iu + 0.5) / (nu * n)
                    for iv in range(nv * n):
                        bb = (iv + 0.5) / (nv * n)
                        tx = int(tu0 + a * (tu1 - tu0)); ty = int(tv0 + bb * (tv1 - tv0))
                        c = T[min(ty, TEX - 1), min(tx, TEX - 1)]
                        if c[3] == 0: continue
                        P = Ry @ (Rm @ (o + A * a + B * bb) + t)
                        sx = int(W / 2 + P[0] * S); sy_ = int(60 + (P[1] + 8) * S)
                        for ox_ in (0, 1):
                            for oy_ in (0, 1):
                                qx, qy = sx + ox_, sy_ + oy_
                                if 0 <= qx < W and 0 <= qy < H and P[2] < zb[qy, qx]:
                                    zb[qy, qx] = P[2]; col[qy, qx] = c
        return Image.fromarray(col)

    views = [render(0.0), render(0.7), render(math.pi / 2), render(math.pi)]
    sheet = Image.new("RGBA", (330 * 4, 420), (36, 38, 46, 255))
    for i, im in enumerate(views):
        sheet.paste(im, (330 * i, 0))
    out = os.path.join(HERE, "stand_preview.png")
    sheet.save(out)
    print("preview:", out)

if __name__ == "__main__":
    pack()
    tex = build_texture()
    write_java()
    print("wrote", TEX_OUT, "and", JAVA_OUT, "-", sum(len(p["boxes"]) for p in PARTS), "extra boxes")
    if "--preview" in sys.argv:
        preview(tex)
