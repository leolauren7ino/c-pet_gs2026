"""Gera favicon.ico e apple-touch-icon.png a partir do emblema de img/logo-cpet.png (Pillow).
Uso: python tools/gerar_icones.py [centro]   (com 'centro', recorta so o miolo do emblema)"""
import os
import sys
from PIL import Image, ImageDraw

DEST = os.path.join("src", "main", "resources", "static", "img")
FUNDO = (18, 18, 18)
CX, CY = 988 * 1.04, 1060 * 1.04
src = Image.open(os.path.join(DEST, "logo-cpet.png")).convert("RGB")

def emblema(tam, centro=False):
    r = (300 if centro else 480) * 1.04
    cx, cy = (CX - 10, CY + 10) if centro else (CX, CY)
    rec = src.crop((int(cx - r), int(cy - r), int(cx + r), int(cy + r))).resize((tam, tam), Image.LANCZOS)
    if not centro:
        d = ImageDraw.Draw(rec)
        d.rectangle([int(tam * 0.295), 0, int(tam * 0.715), int(tam * 0.15)], fill=FUNDO)
    return rec

if __name__ == "__main__":
    centro = len(sys.argv) > 1 and sys.argv[1] == "centro"
    emblema(180, centro).save(os.path.join(DEST, "apple-touch-icon.png"))
    emblema(256, centro).save(os.path.join(DEST, "favicon.ico"), sizes=[(16, 16), (32, 32), (48, 48)])
    if "--prev" in sys.argv:
        emblema(32, centro).resize((160, 160), Image.NEAREST).save(sys.argv[sys.argv.index("--prev") + 1])
