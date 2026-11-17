"""Confere o contraste (WCAG) dos pares texto/fundo dos tokens de css/tokens.css.
Uso: python tools/contraste.py   (sai com codigo 1 se algum par ficar abaixo do minimo)"""
import re, sys
css = open("src/main/resources/static/css/tokens.css", encoding="utf-8").read()
tok = dict(re.findall(r"--([a-z0-9-]+):\s*([^;]+);", css))

def cor(nome, sobre=None):
    v = tok[nome].strip()
    if v.startswith("var("): return cor(v[4:-1].lstrip("-"), sobre)
    if v.startswith("#"):
        h = v[1:]; return tuple(int(h[i:i+2], 16) for i in (0, 2, 4))
    m = re.match(r"rgba\((\d+),\s*(\d+),\s*(\d+),\s*([\d.]+)\)", v)
    r, g, b, a = int(m[1]), int(m[2]), int(m[3]), float(m[4])
    fr, fg, fb = sobre
    return tuple(round(c * a + f * (1 - a)) for c, f in ((r, fr), (g, fg), (b, fb)))

def lum(c):
    f = lambda x: (x / 255) / 12.92 if x / 255 <= .03928 else (((x / 255) + .055) / 1.055) ** 2.4
    return .2126 * f(c[0]) + .7152 * f(c[1]) + .0722 * f(c[2])

def razao(a, b):
    la, lb = sorted((lum(a), lum(b)), reverse=True)
    return (la + .05) / (lb + .05)

F, A, B = cor("fundo"), cor("superficie-a"), cor("superficie-b")
pares = []  # (texto, fundo, minimo)
for t in ("texto", "texto-2", "menta", "menta-claro", "menta-claro-2", "ambar", "erro"):
    for nome, f in (("fundo", F), ("superficie-a", A), ("superficie-b", B)):
        pares.append((t, nome, f, 4.5))
for t in ("menta", "menta-claro", "texto"):   # sobre os fundos translucidos
    for fundo in ("menta-fundo", "menta-fundo-forte"):
        pares.append((t, fundo + " sobre superficie-a", cor(fundo, A), 4.5))
pares.append(("ambar", "ambar-fundo sobre superficie-a", cor("ambar-fundo", A), 4.5))
pares.append(("erro", "erro-fundo sobre superficie-a", cor("erro-fundo", A), 4.5))
pares.append(("sobre-menta", "menta (botao primario)", cor("menta"), 4.5))
pares.append(("sobre-erro", "erro-solido", cor("erro-solido"), 4.5))
for i in range(1, 6):
    pares.append((f"sobre-rampa-{i}", f"rampa-{i}", cor(f"rampa-{i}"), 4.5))

ruim = 0
print(f"{'texto':18} {'fundo':34} razao")
for t, nome, f, minimo in pares:
    c = cor(t)
    r = razao(c, f)
    ok = r >= minimo
    ruim += not ok
    print(f"{t:18} {nome:34} {r:5.2f}  {'ok' if ok else 'ABAIXO DE ' + str(minimo)}")
# informativo: teal profundo e bordas (nao sao texto)
print("informativo teal-profundo sobre fundo: %.2f (so logotipo e decoracao)" % razao(cor("teal-profundo"), F))
print("informativo borda 0,35 sobre superficie-a: %.2f (contorno; o campo tem rotulo e foco proprios)" % razao(cor("borda", A), A))
r = razao(cor("borda-campo", A), A)
print("borda de campo sobre superficie-a: %.2f (minimo 3 para componentes de interface)" % r)
ruim += r < 3
print("PARES ABAIXO DO MINIMO:", ruim)
sys.exit(1 if ruim else 0)
