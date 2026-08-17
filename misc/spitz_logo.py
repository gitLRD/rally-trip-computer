#!/usr/bin/env python3
"""Generates the Japanese Spitz logo as SVG.

Written as a generator rather than hand-typed path data so the fur, which needs a few
dozen alternating points to read as fluff rather than as a scalloped circle, can be
retuned by changing a number instead of by rewriting a path.

Drawn in a 108x108 space: that is the adaptive-icon viewport, whose central 72x72 is the
only part guaranteed to survive an OEM mask. Everything that matters stays inside it.
"""
import math
import random

# --- palette -------------------------------------------------------------------------
GREEN_BG = "#14472F"      # British racing green; a white dog needs a dark ground
FUR = "#FFFFFF"
FUR_SHADE = "#D9E2EA"     # cool, so the white reads as white rather than cream
LINE = "#22272E"
EAR_INNER = "#E9A79C"
NOSE = "#22272E"
BRASS = "#C9982F"
BRASS_DARK = "#8F6A1C"
LENS = "#F2A93C"
LENS_HI = "#FFD98A"
STRAP = "#6B4526"
TONGUE = "#E8748A"

CX, CY = 54.0, 57.0       # centre of the head


def fluff(cx, cy, r_out, r_in, points, start=0.0, sweep=360.0, seed=7, jitter=0.14):
    """A closed furry outline: alternating long and short spikes, joined with quadratics
    so the result looks like fur rather than like a saw blade.

    Perfectly regular spikes read as a cog wheel, so both the radius and the angle are
    jittered. Seeded rather than random, so regenerating the logo produces byte-identical
    output and the file does not churn in git.
    """
    rng = random.Random(seed)
    steps = points * 2
    coords = []
    for i in range(steps):
        wobble = 1.0 + rng.uniform(-jitter, jitter)
        ang_off = rng.uniform(-0.35, 0.35) * (sweep / steps)
        ang = math.radians(start + sweep * i / steps + ang_off)
        r = (r_out if i % 2 == 0 else r_in) * wobble
        coords.append((cx + r * math.cos(ang), cy + r * math.sin(ang)))

    d = f"M{coords[0][0]:.2f},{coords[0][1]:.2f}"
    for i in range(1, steps + 1):
        cur = coords[i % steps]
        prev = coords[i - 1]
        # Control point pushed outwards on the peaks gives a soft tuft.
        mx, my = (prev[0] + cur[0]) / 2, (prev[1] + cur[1]) / 2
        k = 1.10 if i % 2 == 1 else 0.94
        d += f" Q{mx * k + cx * (1 - k):.2f},{my * k + cy * (1 - k):.2f} {cur[0]:.2f},{cur[1]:.2f}"
    return d + " Z"


def ear(x_tip, y_tip, x_a, y_a, x_b, y_b, inner=0.62):
    """An erect, slightly rounded triangle plus its inner ear.

    A Japanese Spitz's ears are small, upright and set well forward. Drawn as flat
    triangles they read as a collie's, so the outer edges bow very slightly outwards.
    """
    mx, my = (x_a + x_b) / 2, (y_a + y_b) / 2
    bow = 1.6
    outer = (f"M{x_tip},{y_tip} "
             f"Q{(x_tip + x_a) / 2 - bow:.2f},{(y_tip + y_a) / 2:.2f} {x_a},{y_a} "
             f"L{x_b},{y_b} "
             f"Q{(x_tip + x_b) / 2 + bow:.2f},{(y_tip + y_b) / 2:.2f} {x_tip},{y_tip} Z")

    def toward_tip(x, y, t):
        return x_tip + (x - x_tip) * t, y_tip + (y - y_tip) * t

    ax, ay = toward_tip(x_a, y_a, inner)
    bx, by = toward_tip(x_b, y_b, inner)
    tx, ty = toward_tip(mx, my, 0.18)
    inner_p = f"M{tx:.2f},{ty:.2f} L{ax:.2f},{ay:.2f} L{bx:.2f},{by:.2f} Z"
    return outer, inner_p


def goggle(cx, cy, r):
    return f"""
    <circle cx="{cx}" cy="{cy}" r="{r}" fill="{LENS}" fill-opacity="0.76"
            stroke="{BRASS}" stroke-width="3.4"/>
    <circle cx="{cx}" cy="{cy}" r="{r}" fill="none" stroke="{LINE}" stroke-width="1.5"/>
    <circle cx="{cx}" cy="{cy}" r="{r + 1.7}" fill="none" stroke="{BRASS_DARK}" stroke-width="1"
            opacity="0.55"/>
    <path d="M{cx - r * 0.55},{cy - r * 0.45} Q{cx - r * 0.1},{cy - r * 0.8} {cx + r * 0.35},{cy - r * 0.55}"
          fill="none" stroke="{LENS_HI}" stroke-width="2.6" stroke-linecap="round" opacity="0.9"/>"""


def build(variant: str = "square") -> str:
    """variant: square (bg), round (bg, circular), foreground (transparent, mask-safe)."""
    lx, rx, ey, er = 42.0, 66.0, 51.5, 9.0   # goggle lens geometry

    # Upright and set forward, tips just inside the safe zone.
    l_out, l_in = ear(38.0, 14.0, 30.5, 38.0, 49.0, 33.0)
    r_out, r_in = ear(70.0, 14.0, 77.5, 38.0, 59.0, 33.0)

    bg = "" if variant == "foreground" else f'<rect width="108" height="108" fill="{GREEN_BG}"/>'

    # An adaptive icon's foreground is only guaranteed inside a 72dp circle, and the ear
    # tips sit 43 from centre — outside it. Scaling about the centre keeps the ears rather
    # than letting a circular launcher mask crop them off; a little ruff clips instead,
    # which on fur nobody notices.
    open_g, close_g = "", ""
    if variant == "foreground":
        open_g = '<g transform="translate(54,54) scale(0.82) translate(-54,-54)">'
        close_g = "</g>"
    elif variant == "round":
        open_g = ('<defs><clipPath id="r"><circle cx="54" cy="54" r="54"/></clipPath></defs>'
                  '<g clip-path="url(#r)">')
        close_g = "</g>"

    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="108" height="108">
  <title>Japanese Spitz in racing goggles</title>
  {open_g}
  {bg}
  <g stroke="{LINE}" stroke-width="2.2" stroke-linejoin="round" stroke-linecap="round">

    <!-- Ears sit behind the head so the join is hidden by the skull. -->
    <path d="{l_out}" fill="{FUR}"/>
    <path d="{r_out}" fill="{FUR}"/>
    <path d="{l_in}" fill="{EAR_INNER}" stroke-width="1.4"/>
    <path d="{r_in}" fill="{EAR_INNER}" stroke-width="1.4"/>

    <!-- The ruff: a Japanese Spitz's defining feature, and what carries the
         silhouette at 48px once the detail inside it has vanished. Deliberately
         much fluffier than the head, so the two do not merge into one blob. -->
    <path d="{fluff(CX, CY + 5, 33.5, 29.2, 22)}" fill="{FUR_SHADE}"/>

    <!-- Head. Only faintly furry: spiking it as hard as the ruff turned the whole
         thing into a startled cat. -->
    <path d="{fluff(CX, CY - 2, 25.0, 23.6, 22, seed=19, jitter=0.06)}" fill="{FUR}"/>

    <!-- Goggle strap, drawn under the lenses so they sit on top of it. -->
    <path d="M21,49.5 Q54,41.5 87,49.5 L87,55.5 Q54,47.5 21,55.5 Z" fill="{STRAP}"/>

    <!-- Muzzle: a soft wedge, fox-like rather than square. -->
    <path d="M{CX - 12},{CY + 5} Q{CX},{CY + 2.5} {CX + 12},{CY + 5}
             Q{CX + 11},{CY + 17} {CX},{CY + 20.5}
             Q{CX - 11},{CY + 17} {CX - 12},{CY + 5} Z" fill="{FUR}"/>
    <!-- Tongue, tucked under the jaw so it cannot be mistaken for a beard. -->
    <path d="M{CX - 4.4},{CY + 17.5} Q{CX},{CY + 16.5} {CX + 4.4},{CY + 17.5}
             Q{CX + 3.4},{CY + 23.5} {CX},{CY + 23.8}
             Q{CX - 3.4},{CY + 23.5} {CX - 4.4},{CY + 17.5} Z"
          fill="{TONGUE}" stroke-width="1.5"/>
    <ellipse cx="{CX}" cy="{CY + 8}" rx="5.8" ry="4.4" fill="{NOSE}" stroke-width="1.6"/>
    <!-- Mouth: kept short and well inside the muzzle. -->
    <path d="M{CX},{CY + 12} L{CX},{CY + 15.2}
             M{CX},{CY + 15.2} Q{CX - 4.2},{CY + 19.4} {CX - 7.6},{CY + 15.0}
             M{CX},{CY + 15.2} Q{CX + 4.2},{CY + 19.4} {CX + 7.6},{CY + 15.0}"
          fill="none" stroke-width="2.0"/>
    <path d="M{CX - 2.4},{CY + 6.4} Q{CX - 1.1},{CY + 5.5} {CX + 0.3},{CY + 6.4}"
          fill="none" stroke="#7C848E" stroke-width="1.2" stroke-linecap="round"/>

    <!-- Eyes, drawn before the lenses and seen through them. Opaque goggles were
         period-correct and completely lifeless. -->
    <g stroke="none">
      <ellipse cx="{lx}" cy="{ey + 0.4}" rx="4.0" ry="4.5" fill="{LINE}"/>
      <ellipse cx="{rx}" cy="{ey + 0.4}" rx="4.0" ry="4.5" fill="{LINE}"/>
      <circle cx="{lx + 1.5}" cy="{ey - 1.4}" r="1.5" fill="{FUR}" opacity="0.95"/>
      <circle cx="{rx + 1.5}" cy="{ey - 1.4}" r="1.5" fill="{FUR}" opacity="0.95"/>
    </g>
  </g>

  <!-- Goggles last: they read as worn over the face rather than drawn on it. -->
  <g>{goggle(lx, ey, er)}{goggle(rx, ey, er)}</g>
  <path d="M{lx + er + 1},{ey} L{rx - er - 1},{ey}" stroke="{BRASS}" stroke-width="4"
        stroke-linecap="round"/>
  <path d="M{lx + er + 1},{ey} L{rx - er - 1},{ey}" stroke="{LINE}" stroke-width="1.4"
        stroke-linecap="round"/>
  {close_g}
</svg>
'''


if __name__ == "__main__":
    import sys
    out, variant = sys.argv[1], (sys.argv[2] if len(sys.argv) > 2 else "square")
    with open(out, "w") as f:
        f.write(build(variant))
    print(f"wrote {out} ({variant})")
