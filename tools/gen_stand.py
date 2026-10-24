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
# Reference: hunched crow / plague-doctor bird, long pointed beak, black mask with a red diamond eye,
# tilted golden disc on the crown, golden disc at the side of the head, ragged pink-purple cloak.

# --- long pointed beak, bending down towards the tip
part("beakA", "head", (0, -2.6, -4), boxes=[(-1.8, -1.8, -3.2, 3.6, 3.4, 3.4, 0, "beak")])
part("beakB", "beakA", (0, 0, -3.2), rot=(0.06, 0, 0), boxes=[(-1.5, -1.5, -3.6, 3.0, 2.9, 3.6, 0, "beak")])
part("beakC", "beakB", (0, 0.1, -3.6), rot=(0.12, 0, 0), boxes=[(-1.15, -1.2, -3.6, 2.3, 2.3, 3.6, 0, "beak")])
part("beakD", "beakC", (0, 0.1, -3.6), rot=(0.2, 0, 0), boxes=[(-0.75, -0.9, -3.4, 1.5, 1.7, 3.4, 0, "beaktip")])
part("beakE", "beakD", (0, 0.2, -3.3), rot=(0.3, 0, 0), boxes=[(-0.4, -0.5, -2.2, 0.8, 1.0, 2.2, 0, "beaktip")])
part("beakRidge", "beakA", (0, 0, 0), boxes=[(-0.45, -2.2, -9.4, 0.9, 0.5, 9.4, 0, "beakdark")])
# open lower jaw with a dark mouth inside
part("jaw", "head", (0, -0.9, -4), boxes=[(-1.45, 0.4, -6.8, 2.9, 1.0, 6.8, 0, "beak"),
                                           (-1.2, -0.1, -6.4, 2.4, 0.5, 6.4, 0, "mouth")])
# --- black face mask + red diamond eyes on the sides of the head
part("faceMask", "head", (0, -4.6, -4.05), boxes=[(-3.9, -2.4, 0, 7.8, 4.8, 0.3, 0, "mask")])
for s, name in ((1, "R"), (-1, "L")):
    part("eyeMask" + name, "head", (s * 4.05, -4.9, -3.0), boxes=[(-0.3, -2.0, 0, 0.6, 4.0, 5.6, 0, "mask")])
# --- tilted golden disc on the crown (two squares rotated 45 deg = octagon) with a raised boss
part("halo", "head", (0, -8.5, 0.3), rot=(-0.18, 0, 0.2),
     boxes=[(-4.0, -0.5, -4.0, 8.0, 1.0, 8.0, 0, "gold"), (-1.6, -1.1, -1.6, 3.2, 0.7, 3.2, 0, "goldhi")])
part("halo2", "halo", (0, 0, 0), rot=(0, math.pi / 4, 0), boxes=[(-4.0, -0.5, -4.0, 8.0, 1.0, 8.0, 0, "gold")])
# --- golden disc at the side of the head (vertical, octagonal)
for s, name in ((1, "R"), (-1, "L")):
    part("disc" + name, "head", (s * 4.5, -4.4, 1.3), boxes=[(-0.5, -3.1, -3.1, 1.0, 6.2, 6.2, 0, "gold"),
                                                            ((0.4 if s > 0 else -1.1), -1.2, -1.2, 0.7, 2.4, 2.4, 0, "goldhi")])
    part("disc2" + name, "disc" + name, (0, 0, 0), rot=(math.pi / 4, 0, 0), boxes=[(-0.5, -3.1, -3.1, 1.0, 6.2, 6.2, 0, "gold")])
# --- spiky feathers swept back from the nape
for i, (x, ln, rz) in enumerate(((-2.6, 3.2, 0.25), (-1.3, 4.4, 0.1), (0, 5.0, 0.0), (1.3, 4.4, -0.1), (2.6, 3.2, -0.25))):
    part("spike%d" % i, "head", (x, -2.0, 3.6), rot=(-1.05, 0, rz), boxes=[(-0.55, -ln, -0.3, 1.1, ln, 0.6, 0, "spike")])

# ---- body (box spans x -4..4, y 0..12, z -2..2): the cloak hides almost all of it
part("mantle", "body", (0, 0, 0), boxes=[(-6.2, -1.0, -3.4, 12.4, 3.5, 6.8, 0, "cloakm")])
part("collar", "body", (0, 0, 0), boxes=[(-3.6, -2.2, -3.0, 7.2, 2.0, 6.0, 0, "cloakp")])
part("hump", "body", (0, 0.5, 2.0), rot=(-0.35, 0, 0), boxes=[(-4.8, -1.0, 0, 9.6, 8.0, 4.4, 0, "cloakd")])
part("humpHi", "hump", (0, 0, 0), boxes=[(-1.6, -1.4, 0.6, 3.2, 6.0, 4.1, 0, "cloakp")])
# long ragged strips hanging at the back, alternating dark / mid / pink highlight
back_len = (17.0, 22.0, 15.0, 25.0, 19.0, 26.0, 16.0, 23.0, 14.0)
for i, ln in enumerate(back_len):
    mat = ("cloakd", "cloakp", "cloakm")[i % 3]
    part("back%d" % i, "body", (-5.2 + i * 1.3, 3.0, 4.6 - 0.25 * abs(i - 4)), rot=(0.1 + 0.03 * (i % 3), 0, (0.06 if i % 2 == 0 else -0.06)),
         boxes=[(-0.85, 0, 0, 1.7, ln, 0.9, 0, mat)])
# side drapes
for s, name in ((1, "R"), (-1, "L")):
    for j, (z, ln) in enumerate(((-1.4, 15.0), (0.2, 21.0), (1.8, 17.0))):
        mat = ("cloakm", "cloakp", "cloakd")[j]
        part("side%s%d" % (name, j), "body", (s * 5.4, 1.2, z), rot=(0, 0, s * (0.07 + 0.03 * j)),
             boxes=[(-0.45, 0, -0.95, 0.9, ln, 1.9, 0, mat)])
# two front tails hanging from the shoulders (the middle stays open)
for s, name in ((1, "R"), (-1, "L")):
    part("front" + name, "body", (s * 3.0, 1.5, -3.3), rot=(-0.05, 0, s * 0.04), boxes=[(-1.5, 0, 0, 3.0, 19.0 if s > 0 else 22.0, 0.8, 0, "cloakm")])
    part("frontHi" + name, "front" + name, (s * 0.3, 1.0, -0.1), boxes=[(-0.6, 0, 0, 1.2, 12.0, 0.4, 0, "cloakp")])

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

# palette sampled from the reference art
MAT = {
    "beak": ((150, 142, 170), 6), "beaktip": ((126, 118, 146), 5), "beakdark": ((78, 68, 98), 4), "mouth": ((30, 24, 40), 3),
    "spike": ((172, 164, 190), 6), "mask": ((26, 20, 34), 3),
    "gold": ((196, 160, 66), 6), "goldhi": ((238, 208, 120), 5),
    "cloakd": ((128, 76, 124), 7), "cloakm": ((166, 102, 158), 8), "cloakp": ((226, 160, 214), 8),
}
BEAK_GREY = (150, 142, 170)
CLOAK_D, CLOAK_M, CLOAK_P = (128, 76, 124), (166, 102, 158), (226, 160, 214)
INK = (34, 20, 40, 255)

def hatch(d, r, col, step=2, vertical=True):
    """Manga-style cross-hatching over a texel rect."""
    if vertical:
        for xx in range(r[0], r[2], step):
            d.line((xx, r[1], xx, r[3] - 1), fill=col)
    else:
        for yy in range(r[1], r[3], step):
            d.line((r[0], yy, r[2] - 1, yy), fill=col)

def paint_box(img, d, b):
    x, y, z, w, h, dp, infl, mat, u, v = b
    base, amt = MAT[mat]
    fr = faces(u, v, w, h, dp)
    fill(img, (u, v, u + 2 * ceil(dp) + 2 * ceil(w), v + ceil(dp) + ceil(h)), base, amt)
    dark = tuple(int(c * 0.62) for c in base) + (255,)
    light = tuple(min(255, int(c * 1.2)) for c in base) + (255,)
    if mat in ("beak", "beaktip", "spike"):
        for name in ("front", "back", "left", "right"):
            hatch(d, fr[name], dark, 3)
        for name in ("front", "back", "left", "right"):
            r = fr[name]; d.line((r[0], r[3] - 1, r[2] - 1, r[3] - 1), fill=dark)
    elif mat == "mask":
        for name in ("left", "right"):          # red diamond eye with a pale glint, centred on the side faces
            r = fr[name]
            cx, cy = (r[0] + r[2]) // 2, (r[1] + r[3]) // 2
            for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
                d.point((cx + dx, cy + dy), (206, 36, 52, 255))
            d.point((cx, cy), (255, 150, 150, 255))
            d.point((cx - 2, cy), (230, 36, 52, 255)); d.point((cx + 2, cy), (230, 36, 52, 255))
        r = fr["front"]
        d.line((r[0], r[1] + 1, r[2] - 1, r[1] + 1), fill=(60, 46, 74, 255))
    elif mat in ("gold", "goldhi"):
        for name in ("top", "bottom", "front", "back", "left", "right"):
            r = fr[name]
            d.rectangle((r[0], r[1], r[2] - 1, r[3] - 1), outline=dark)
            if name in ("top", "bottom", "left", "right"):  # radial / concentric pattern
                cx, cy = (r[0] + r[2] - 1) / 2.0, (r[1] + r[3] - 1) / 2.0
                for k in range(6):
                    a = k * math.pi / 3
                    d.line((cx, cy, cx + math.cos(a) * (r[2] - r[0]) / 2.0, cy + math.sin(a) * (r[3] - r[1]) / 2.0), fill=dark)
                d.point((int(cx), int(cy)), light)
    elif mat in ("cloakd", "cloakm", "cloakp"):
        ink = (60, 30, 66, 255) if mat != "cloakp" else (150, 84, 150, 255)
        for name in ("front", "back", "left", "right"):
            r = fr[name]
            hatch(d, r, tuple(int(c * 0.78) for c in base) + (255,), 2)
            for _ in range(max(1, (r[2] - r[0]) // 3)):          # dark slits like the tears in the art
                xx = random.randint(r[0], r[2] - 1); yy = random.randint(r[1], max(r[1], r[3] - 4))
                d.line((xx, yy, xx, min(r[3] - 1, yy + random.randint(2, 6))), fill=ink)
            for xx in range(r[0], r[2], 2):                       # ragged hem
                d.line((xx, r[3] - random.randint(1, 3), xx, r[3] - 1), fill=ink)
        for name in ("top", "bottom"):
            hatch(d, fr[name], ink, 2)

def paint_base(img, d):
    """Humanoid skin (64x64): cloak-coloured body / legs, grey-lavender head, hands and feet with claws."""
    fill(img, (0, 0, 64, 64), CLOAK_M, 8)
    for _ in range(40):                                   # pink highlight patches + dark shadows
        x, y = random.randint(0, 60), random.randint(0, 60)
        w, h = random.randint(2, 5), random.randint(4, 12)
        tone = random.choice([CLOAK_P, CLOAK_D, CLOAK_D])
        for yy in range(y, min(64, y + h)):
            for xx in range(x, min(64, x + w)):
                img.putpixel((xx, yy), jit(tone, 6))
    hatch(d, (0, 0, 64, 64), (96, 52, 100, 255), 3)
    # ---- head (UV 0,0): right 0..8 / front 8..16 / left 16..24 / back 24..32 at rows 8..16, top 8..16 at rows 0..8
    fill(img, (0, 0, 32, 16), BEAK_GREY, 6)
    hatch(d, (0, 0, 32, 16), (92, 84, 118, 255), 2)
    fill(img, (8, 8, 16, 16), (26, 20, 34), 3)           # front: black mask under the beak
    fill(img, (24, 8, 32, 16), (88, 70, 112), 5)          # back of head: darker plumage
    hatch(d, (24, 8, 32, 16), (56, 40, 76, 255), 2)
    # ---- body (16,16): keep the cloak colours, darker under the open front
    fill(img, (20, 20, 28, 32), CLOAK_D, 6)
    hatch(d, (20, 20, 28, 32), (80, 40, 84, 255), 2)
    # ---- arms (40,16) and (32,48): purple sleeve, grey clawed hand on the last rows
    for (ux, vy) in ((40, 20), (32, 52)):
        for yy in range(vy + 7, vy + 12):
            for xx in range(ux, ux + 16):
                img.putpixel((xx, yy), jit(BEAK_GREY, 5))
        hatch(d, (ux, vy + 7, ux + 16, vy + 12), (92, 84, 118, 255), 2)
        d.line((ux, vy + 11, ux + 15, vy + 11), fill=(40, 30, 54, 255))        # claws
        for xx in range(ux, ux + 16, 2):
            d.point((xx, vy + 11), (214, 208, 224, 255))
    # ---- legs (0,16) and (16,48): dark purple with grey feet / talons
    for (ux, vy) in ((0, 20), (16, 52)):
        for yy in range(vy + 8, vy + 12):
            for xx in range(ux, ux + 16):
                img.putpixel((xx, yy), jit(BEAK_GREY, 5))
        d.line((ux, vy + 11, ux + 15, vy + 11), fill=(40, 30, 54, 255))

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
 * extra boxes to its head and body: a long pointed crow beak with an open jaw, black face mask with red diamond eyes, a tilted golden disc
 * on the crown, a golden disc at each side of the head, spiky nape feathers and a big hunched pink-purple cloak
 * (shoulder mantle, hump, ragged back strips, side drapes, two front tails).
 * Limb details (grey clawed hands, talons, cloak-coloured sleeves) are painted on the humanoid skin.
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
     * model (cloak, beak, discs...). The torso and head are held still here, so only the arms flurry.
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
