"""Mede as cores do deck (docs/identidade/Pitch_C-PET_para_CitizenTech.pdf): renderiza as paginas
de identidade e, para cada cor-alvo, conta os pixels proximos e mostra a cor mais frequente.
Uso: pip install pymupdf pillow; python tools/medir_cores_pdf.py"""
import collections
import pymupdf
from PIL import Image

PAGINAS = (1, 3, 5, 6, 7, 8, 9, 12)
ALVO = {"fundo 0F0F0E": (15, 15, 14), "superficie 0A1D21": (10, 29, 33), "superficie 0E1A1B": (14, 26, 27),
        "menta 3CC3A4": (60, 195, 164), "menta claro 90D8C8": (144, 216, 200), "menta claro 2 ADE4D8": (173, 228, 216),
        "teal profundo 105868": (16, 88, 104), "texto EAF1EF": (234, 241, 239), "texto 2 9DB2AE": (157, 178, 174)}
doc = pymupdf.open("docs/identidade/Pitch_C-PET_para_CitizenTech.pdf")
cont = collections.Counter()
for n in PAGINAS:
    pm = doc[n - 1].get_pixmap(dpi=40)
    im = Image.frombytes("RGB", (pm.width, pm.height), pm.samples)
    cont.update(im.getdata())
total = sum(cont.values())
perto = lambda c, t, tol=14: all(abs(a - b) <= tol for a, b in zip(c, t))
for nome, t in ALVO.items():
    pix = [(v, c) for c, v in cont.items() if perto(c, t)]
    frac = 100 * sum(v for v, _ in pix) / total
    melhor = max(pix, default=None)
    print(f"{nome:24} {frac:6.2f}%  mais frequente: " + ("#%02X%02X%02X" % melhor[1] if melhor else "-"))
