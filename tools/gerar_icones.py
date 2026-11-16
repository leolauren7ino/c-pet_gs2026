"""Gera favicon.ico e apple-touch-icon.png a partir da geometria de marca-cpet.svg (Pillow)."""
from PIL import Image, ImageDraw
import os
TEAL, TERRA, CREME = (19, 89, 105), (169, 67, 30), (242, 238, 228)
NOS = [(9, 23), (18, 10), (30, 10), (39, 23)]
PAD = (24, 37)

def desenhar(tam):
    s = 8
    im = Image.new("RGBA", (tam * s, tam * s), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([0, 0, tam * s - 1, tam * s - 1], radius=tam * s * 0.18, fill=CREME)
    k = tam * s / 48 * 0.78
    off = tam * s * 0.11
    p = lambda x, y: (off + x * k, off + y * k + tam * s * 0.01)
    w = max(2, int(2.6 * k))
    d.line([p(*n) for n in NOS], fill=TEAL, width=w, joint="curve")
    for n in NOS:
        d.line([p(*n), p(*PAD)], fill=TEAL, width=w)
    for n in NOS:
        x, y = p(*n); r = 4.4 * k
        d.ellipse([x - r, y - r, x + r, y + r], fill=TEAL)
    x, y = p(*PAD); r = 6 * k
    d.ellipse([x - r, y - r, x + r, y + r], fill=TERRA)
    return im.resize((tam, tam), Image.LANCZOS)

dest = os.path.join("src", "main", "resources", "static", "img")
desenhar(180).save(os.path.join(dest, "apple-touch-icon.png"))
desenhar(256).save(os.path.join(dest, "favicon.ico"), sizes=[(16, 16), (32, 32), (48, 48)])
desenhar(96).save(os.path.join("tools", "_prev96.png"))
desenhar(32).resize((128, 128), Image.NEAREST).save(os.path.join("tools", "_prev32.png"))
