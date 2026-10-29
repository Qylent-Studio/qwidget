#!/usr/bin/env python3
"""QWidget launcher icon assets.

Single source of truth for the launcher icon: the adaptive-icon vector
drawables, the density mipmap fallbacks and the masked preview images are all
produced from the geometry constants below. Standard library only, on purpose
(no third-party packages are added to this project for icon rendering).

Design: a rounded square ring (the widget surface) in the app accent green
"#B8F39B" over a graphite gradient ground, with a short diagonal tail at the
bottom-right corner so the mark reads as a "Q". No third-party logos, wordmarks
or brand motifs are used or imitated.

Usage:
    python scripts/make-icon-assets.py                 # resources + previews
    python scripts/make-icon-assets.py --previews-only # artifacts only
"""

from __future__ import annotations

import argparse
import math
import pathlib
import struct
import zlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res"
ARTIFACTS = ROOT / "artifacts"

# --- geometry (108x108 adaptive-icon viewport) -------------------------------
CANVAS = 108.0
BOX_MIN = 33.0
BOX_MAX = 75.0
CORNER = 12.0
STROKE = 8.0
TAIL_START = (61.0, 61.0)
TAIL_END = (72.5, 72.5)

# --- palette (matches dev.qwidget.core.theme.WidgetPalette graphite/accent) --
ACCENT = (0xB8, 0xF3, 0x9B)
BG_START = (0x23, 0x2B, 0x2E)
BG_END = (0x0F, 0x14, 0x16)
GLOW = (0xB8, 0xF3, 0x9B)
GLOW_ALPHA = 0.10
GLOW_CENTER = (0.28, 0.22)
GLOW_RADIUS = 0.55

SS = 4  # supersampling factor per axis
LEGACY_SCALE = 1.5  # legacy icons show the 72dp visible area edge to edge
LEGACY_CORNER = 20.0  # legacy PNG corner radius, keeps launcher icons off every pixel
DENSITY_SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}
PREVIEW_SIZES = (48, 96, 192)
PREVIEW_MASKS = ("round", "square", "squircle")
PREVIEW_BACKDROP = (0xEF, 0xF2, 0xF0)


# --- geometry helpers -------------------------------------------------------
def _sd_round_rect(px: float, py: float, hw: float, hh: float, r: float) -> float:
    qx = abs(px) - (hw - r)
    qy = abs(py) - (hh - r)
    ax = qx if qx > 0.0 else 0.0
    ay = qy if qy > 0.0 else 0.0
    return math.hypot(ax, ay) + min(max(qx, qy), 0.0) - r


def _sd_segment(px: float, py: float, ax: float, ay: float, bx: float, by: float) -> float:
    pax, pay = px - ax, py - ay
    bax, bay = bx - ax, by - ay
    h = (pax * bax + pay * bay) / (bax * bax + bay * bay)
    h = 0.0 if h < 0.0 else (1.0 if h > 1.0 else h)
    return math.hypot(pax - bax * h, pay - bay * h)


def _mark_distance(cx: float, cy: float) -> float:
    """Signed distance to the Q mark in canvas units (<= 0 means inside)."""
    half = (BOX_MAX - BOX_MIN) / 2.0
    mid = (BOX_MIN + BOX_MAX) / 2.0
    ring = abs(_sd_round_rect(cx - mid, cy - mid, half, half, CORNER)) - STROKE / 2.0
    tail = _sd_segment(cx, cy, TAIL_START[0], TAIL_START[1], TAIL_END[0], TAIL_END[1]) - STROKE / 2.0
    return min(ring, tail)


def _mask_inside(mask: str, cx: float, cy: float) -> bool:
    if mask == "square":
        return True
    half = CANVAS / 2.0
    dx = abs(cx - half)
    dy = abs(cy - half)
    if mask == "round":
        return math.hypot(dx, dy) <= half
    if mask == "legacy":
        return _sd_round_rect(cx - half, cy - half, half, half, LEGACY_CORNER) <= 0.0
    if mask == "squircle":
        return (dx / half) ** 4 + (dy / half) ** 4 <= 1.0
    raise ValueError(f"unknown mask: {mask}")


def _background(cx: float, cy: float) -> tuple[float, float, float]:
    t = max(0.0, min(1.0, (cx + cy) / (CANVAS * 2.0)))
    base = [BG_START[i] + (BG_END[i] - BG_START[i]) * t for i in range(3)]
    gx = GLOW_CENTER[0] * CANVAS
    gy = GLOW_CENTER[1] * CANVAS
    gr = GLOW_RADIUS * CANVAS
    falloff = max(0.0, 1.0 - math.hypot(cx - gx, cy - gy) / gr)
    a = GLOW_ALPHA * falloff
    return tuple(base[i] + (GLOW[i] - base[i]) * a for i in range(3))  # type: ignore[return-value]


# --- rasterizer -------------------------------------------------------------
def render_icon(size: int, mask: str = "square", scale: float = 1.0) -> bytearray:
    """Render the icon as straight-alpha RGBA bytes, size x size."""
    out = bytearray(size * size * 4)
    samples = SS * SS
    art_offset = CANVAS / 2.0
    for y in range(size):
        for x in range(size):
            mask_hits = 0
            mark_hits = 0
            for sy in range(SS):
                py = (y + (sy + 0.5) / SS) / size * CANVAS
                for sx in range(SS):
                    px = (x + (sx + 0.5) / SS) / size * CANVAS
                    if _mask_inside(mask, px, py):
                        mask_hits += 1
                    acx = (px - art_offset) / scale + art_offset
                    acy = (py - art_offset) / scale + art_offset
                    if _mark_distance(acx, acy) <= 0.0:
                        mark_hits += 1
            mask_a = mask_hits / samples
            mark_a = mark_hits / samples
            # The art is scaled about the canvas centre, so the ground must be
            # sampled in the same space; otherwise the glow/gradient drift.
            centre_x = (x + 0.5) / size * CANVAS
            centre_y = (y + 0.5) / size * CANVAS
            r, g, b = _background(
                (centre_x - art_offset) / scale + art_offset,
                (centre_y - art_offset) / scale + art_offset,
            )
            r = r + (ACCENT[0] - r) * mark_a
            g = g + (ACCENT[1] - g) * mark_a
            b = b + (ACCENT[2] - b) * mark_a
            i = (y * size + x) * 4
            out[i] = int(round(r))
            out[i + 1] = int(round(g))
            out[i + 2] = int(round(b))
            out[i + 3] = int(round(mask_a * 255))
    return out


def _blend_over(dst: tuple[float, float, float, float], src: tuple[float, float, float, float]) -> tuple[float, float, float, float]:
    sa, da = src[3], dst[3]
    oa = sa + da * (1.0 - sa)
    if oa <= 0.0:
        return (0.0, 0.0, 0.0, 0.0)
    rgb = [(src[i] * sa + dst[i] * da * (1.0 - sa)) / oa for i in range(3)]
    return (rgb[0], rgb[1], rgb[2], oa)


class Canvas:
    def __init__(self, width: int, height: int, backdrop: tuple[int, int, int] | None = None):
        self.width = width
        self.height = height
        fill = (0, 0, 0, 0) if backdrop is None else (*backdrop, 255)
        self.px = bytearray(bytes(fill) * (width * height))

    def put(self, x: int, y: int, color: tuple[float, float, float, float]) -> None:
        i = (y * self.width + x) * 4
        dst = (
            self.px[i] / 255.0,
            self.px[i + 1] / 255.0,
            self.px[i + 2] / 255.0,
            self.px[i + 3] / 255.0,
        )
        r, g, b, a = _blend_over(dst, color)
        self.px[i] = int(round(r * 255))
        self.px[i + 1] = int(round(g * 255))
        self.px[i + 2] = int(round(b * 255))
        self.px[i + 3] = int(round(a * 255))

    def blit(self, rgba: bytearray, ox: int, oy: int, size: int) -> None:
        for y in range(size):
            for x in range(size):
                i = (y * size + x) * 4
                self.put(
                    ox + x,
                    oy + y,
                    (
                        rgba[i] / 255.0,
                        rgba[i + 1] / 255.0,
                        rgba[i + 2] / 255.0,
                        rgba[i + 3] / 255.0,
                    ),
                )


def write_png(path: pathlib.Path, width: int, height: int, rgba: bytes | bytearray) -> None:
    raw = bytearray()
    stride = width * 4
    for y in range(height):
        raw.append(0)
        raw += rgba[y * stride : (y + 1) * stride]

    def chunk(tag: bytes, data: bytes) -> bytes:
        return (
            struct.pack(">I", len(data))
            + tag
            + data
            + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        )

    path.parent.mkdir(parents=True, exist_ok=True)
    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    path.write_bytes(png)


# --- generated resources ----------------------------------------------------
def _hex(rgb: tuple[int, int, int], alpha: int = 0xFF) -> str:
    return f"#{alpha:02X}{rgb[0]:02X}{rgb[1]:02X}{rgb[2]:02X}"


def _num(value: float) -> str:
    """Format a viewport coordinate without a trailing ".0"."""
    text = f"{value:.2f}".rstrip("0").rstrip(".")
    return text if text else "0"


RING_PATH = (
    f"M{_num(BOX_MIN + CORNER)},{_num(BOX_MIN)} H{_num(BOX_MAX - CORNER)} "
    f"A{_num(CORNER)},{_num(CORNER)} 0 0 1 {_num(BOX_MAX)},{_num(BOX_MIN + CORNER)} "
    f"V{_num(BOX_MAX - CORNER)} A{_num(CORNER)},{_num(CORNER)} 0 0 1 {_num(BOX_MAX - CORNER)},{_num(BOX_MAX)} "
    f"H{_num(BOX_MIN + CORNER)} A{_num(CORNER)},{_num(CORNER)} 0 0 1 {_num(BOX_MIN)},{_num(BOX_MAX - CORNER)} "
    f"V{_num(BOX_MIN + CORNER)} A{_num(CORNER)},{_num(CORNER)} 0 0 1 {_num(BOX_MIN + CORNER)},{_num(BOX_MIN)} Z"
)
TAIL_PATH = f"M{_num(TAIL_START[0])},{_num(TAIL_START[1])} L{_num(TAIL_END[0])},{_num(TAIL_END[1])}"


def _header(comment: str) -> str:
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<!-- Generated by scripts/make-icon-assets.py - edit the geometry there and re-run. -->\n"
        f"<!-- {comment} -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    xmlns:aapt="http://schemas.android.com/aapt"\n'
        '    android:width="108dp"\n'
        '    android:height="108dp"\n'
        '    android:viewportWidth="108"\n'
        '    android:viewportHeight="108">\n'
    )


def background_xml() -> str:
    return (
        _header("Adaptive icon ground layer: graphite gradient with a faint accent glow.")
        + '    <path android:pathData="M0,0h108v108h-108z">\n'
        + '        <aapt:attr name="android:fillColor">\n'
        + '            <gradient\n'
        + '                android:type="linear"\n'
        + '                android:startX="0" android:startY="0"\n'
        + '                android:endX="108" android:endY="108"\n'
        + f'                android:startColor="{_hex(BG_START)}"\n'
        + f'                android:endColor="{_hex(BG_END)}" />\n'
        + "        </aapt:attr>\n"
        + "    </path>\n"
        + '    <path android:pathData="M0,0h108v108h-108z">\n'
        + '        <aapt:attr name="android:fillColor">\n'
        + '            <gradient\n'
        + '                android:type="radial"\n'
        + f'                android:centerX="{_num(GLOW_CENTER[0] * CANVAS)}"\n'
        + f'                android:centerY="{_num(GLOW_CENTER[1] * CANVAS)}"\n'
        + f'                android:gradientRadius="{_num(GLOW_RADIUS * CANVAS)}"\n'
        + f'                android:startColor="{_hex(GLOW, int(round(GLOW_ALPHA * 255)))}"\n'
        + f'                android:endColor="{_hex(GLOW, 0)}" />\n'
        + "        </aapt:attr>\n"
        + "    </path>\n"
        + "</vector>\n"
    )


def mark_xml(comment: str, color: tuple[int, int, int]) -> str:
    stroke = f'android:strokeColor="{_hex(color)}" android:strokeWidth="{_num(STROKE)}"'
    return (
        _header(comment)
        + f'    <path\n        android:pathData="{RING_PATH}"\n        {stroke}\n'
        + '        android:strokeLineJoin="round" android:strokeLineCap="round" />\n'
        + f'    <path\n        android:pathData="{TAIL_PATH}"\n        {stroke}\n'
        + '        android:strokeLineCap="round" />\n'
        + "</vector>\n"
    )


def adaptive_xml() -> str:
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<!-- Generated by scripts/make-icon-assets.py - edit the geometry there and re-run. -->\n"
        "<adaptive-icon xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
        '    <background android:drawable="@drawable/ic_launcher_background" />\n'
        '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
        '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n'
        "</adaptive-icon>\n"
    )


def write_resources() -> list[pathlib.Path]:
    written = []
    targets = {
        RES / "drawable" / "ic_launcher_background.xml": background_xml(),
        RES / "drawable" / "ic_launcher_foreground.xml": mark_xml(
            "Adaptive icon foreground layer: accent Q mark.", ACCENT
        ),
        RES / "drawable" / "ic_launcher_monochrome.xml": mark_xml(
            "Adaptive icon monochrome layer: same mark, single colour, alpha only.", (0xFF, 0xFF, 0xFF)
        ),
        RES / "mipmap-anydpi" / "ic_launcher.xml": adaptive_xml(),
        RES / "mipmap-anydpi" / "ic_launcher_round.xml": adaptive_xml(),
    }
    for path, text in targets.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
        written.append(path)

    for density, size in DENSITY_SIZES.items():
        folder = RES / f"mipmap-{density}"
        square = render_icon(size, "legacy", LEGACY_SCALE)
        round_icon = render_icon(size, "round", LEGACY_SCALE)
        for name, data in (("ic_launcher.png", square), ("ic_launcher_round.png", round_icon)):
            path = folder / name
            write_png(path, size, size, data)
            written.append(path)
    return written


def write_previews() -> list[pathlib.Path]:
    written = []
    for size in PREVIEW_SIZES:
        for mask in PREVIEW_MASKS:
            icon = render_icon(size, mask, LEGACY_SCALE)
            canvas = Canvas(size, size, PREVIEW_BACKDROP)
            canvas.blit(icon, 0, 0, size)
            path = ARTIFACTS / f"icon-preview-{size}-{mask}.png"
            write_png(path, size, size, canvas.px)
            written.append(path)

    cell = PREVIEW_SIZES[-1] + 24
    sheet = Canvas(len(PREVIEW_MASKS) * cell, len(PREVIEW_SIZES) * cell, PREVIEW_BACKDROP)
    for row, size in enumerate(PREVIEW_SIZES):
        for col, mask in enumerate(PREVIEW_MASKS):
            icon = render_icon(size, mask, LEGACY_SCALE)
            ox = col * cell + (cell - size) // 2
            oy = row * cell + (cell - size) // 2
            sheet.blit(icon, ox, oy, size)
    grid = ARTIFACTS / "icon-preview-grid.png"
    write_png(grid, sheet.width, sheet.height, sheet.px)
    written.append(grid)
    return written


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate QWidget launcher icon assets.")
    parser.add_argument(
        "--previews-only",
        action="store_true",
        help="only regenerate the preview images under artifacts/",
    )
    args = parser.parse_args()

    ARTIFACTS.mkdir(parents=True, exist_ok=True)
    if not args.previews_only:
        for path in write_resources():
            print(f"resource  {path.relative_to(ROOT)}")
    for path in write_previews():
        print(f"preview   {path.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
