"""Gera static/img/emblema.png a partir do logo oficial (logo-cpet.png).

Recorta o emblema circular, apaga o texto do topo e os fragmentos dos cantos e
troca o fundo #121212 do PNG pelo fundo do tema (#0F0F0E), para nao aparecer disco.
Uso: python tools/gerar_emblema.py
"""
from PIL import Image

FUNDO_PNG = (18, 18, 18)
FUNDO_TEMA = (15, 15, 14)
SAIDA = 480

im = Image.open("src/main/resources/static/img/logo-cpet.png").convert("RGB")
w = im.size[0]
lado = round(w / 2.01)
x0, y0 = round(0.527 / 2.01 * w), round(0.604 / 2.01 * w)
rec = im.crop((x0, y0, x0 + lado, y0 + lado))

px = rec.load()
def apagar(x1, y1, x2, y2):
    for y in range(max(0, y1), min(lado, y2)):
        for x in range(max(0, x1), min(lado, x2)):
            px[x, y] = FUNDO_PNG

apagar(int(lado * .29), 0, int(lado * .71), int(lado * .15))   # texto C-PET do topo
apagar(int(lado * .86), 0, lado, int(lado * .12))              # fragmentos do canto superior direito
apagar(0, int(lado * .76), int(lado * .14), int(lado * .97))   # fragmentos do canto inferior esquerdo

dr, dg, db = (FUNDO_TEMA[i] - FUNDO_PNG[i] for i in range(3))
for y in range(lado):
    for x in range(lado):
        r, g, b = px[x, y]
        if max(r, g, b) <= 24:
            px[x, y] = FUNDO_TEMA
        else:
            px[x, y] = (max(0, r + dr), max(0, g + dg), max(0, b + db))

rec.resize((SAIDA, SAIDA), Image.LANCZOS).save("src/main/resources/static/img/emblema.png", optimize=True)
print("ok", SAIDA)
