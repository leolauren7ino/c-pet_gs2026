"""Gera src/main/resources/db/seed.sql.

Uso: python -X utf8 tools/gerar_seed.py

Datas sao relativas a LOCALTIMESTAMP, entao o seed sempre parece "de hoje".
Determinístico: a mesma execucao produz o mesmo arquivo.
"""
import math
import random
from pathlib import Path

DESTINO = Path(__file__).resolve().parent.parent / "src/main/resources/db/seed.sql"
rng = random.Random(2026)

BAIRROS = {
    1: ("Capão Redondo", -23.6600, -46.7700, 18),
    2: ("Brasilândia", -23.4600, -46.6900, 17),
    3: ("Itaquera", -23.5400, -46.4560, 11),
    4: ("Grajaú", -23.7620, -46.6850, 15),
    5: ("Mooca", -23.5580, -46.5990, 6),
    6: ("Pinheiros", -23.5670, -46.6910, 3),
}

# id: nome, tipo, lat, lng, vagas, tipos aceitos
RECURSOS = {
    1: ("Clínica Popular Pata Cuidada", "CLINICA", -23.6400, -46.7500, 4, "FERIDO,DOENTE"),
    2: ("Clínica Veterinária Raio de Sol", "CLINICA", -23.5100, -46.6600, 2, "FERIDO,DOENTE"),
    3: ("Clínica Bicho Bom Zona Leste", "CLINICA", -23.5450, -46.4700, 0, "FERIDO,DOENTE"),
    4: ("Instituto Patas Unidas", "ONG", -23.5530, -46.6580, 6, "FERIDO,DOENTE,FILHOTE,COLONIA"),
    5: ("Casa Lar Focinho Feliz", "ONG", -23.7000, -46.7000, 3, "FILHOTE,COLONIA"),
    6: ("Unidade Móvel Castra-Rua 01", "UNIDADE_MOVEL", -23.5800, -46.6800, 5, "COLONIA"),
    7: ("Núcleo de Bem-Estar Animal Sul", "ORGAO_PUBLICO", -23.6700, -46.7200, 8, "MAUS_TRATOS,RISCO_SANITARIO"),
    8: ("Posto de Vigilância Sanitária Norte", "ORGAO_PUBLICO", -23.4900, -46.6700, 5, "RISCO_SANITARIO,MAUS_TRATOS"),
}

USUARIOS = [
    (1, "Marina Duarte", "OPERADOR", None),
    (2, "Rafael Nogueira", "AGENTE", 1),
    (3, "Beatriz Lemos", "AGENTE", 4),
]

DESCRICOES = {
    "FERIDO": [
        "Cachorro de porte médio com ferimento na pata traseira, não consegue andar direito.",
        "Gato atropelado na calçada, está vivo mas não se mexe.",
        "Cadela com um corte fundo no flanco, moradores estão dando água.",
    ],
    "DOENTE": [
        "Cachorro muito magro, com feridas na pele e tosse seca.",
        "Gato com secreção nos olhos e espirrando muito, fica embaixo de um carro.",
        "Animal com a pele toda pelada e se coçando sem parar.",
    ],
    "FILHOTE": [
        "Ninhada de filhotes numa caixa de papelão perto da feira, sem a mãe.",
        "Filhotes chorando dentro de um bueiro seco na esquina.",
        "Quatro filhotes largados na entrada de uma obra parada.",
    ],
    "COLONIA": [
        "Mais de cinco gatos vivem no terreno baldio, moradores alimentam todo dia.",
        "Grupo de cachorros soltos ao redor da praça, sem coleira e sem castração.",
        "Colônia de gatos atrás do mercado, aumentando a cada mês.",
    ],
    "MAUS_TRATOS": [
        "Cachorro preso em corrente curta no quintal, sem água nem abrigo.",
        "Vizinho deixa o animal sem comida há dias, ele late o tempo todo.",
        "Animal sendo agredido na frente da escola, várias pessoas viram.",
    ],
    "RISCO_SANITARIO": [
        "Carcaça de animal na calçada há dois dias, cheiro forte.",
        "Acúmulo de lixo com muitos animais e ratos ao redor do ponto de coleta.",
        "Animais doentes aglomerados perto da creche municipal.",
    ],
}

OBS_ATENDIMENTO = [
    "Animal avaliado, tratado e devolvido ao local com acompanhamento.",
    "Castrado, vacinado e identificado com microchip.",
    "Resgatado e levado para lar temporário.",
    "Atendimento concluído no local pela equipe, sem necessidade de internação.",
    "Fiscalização realizada e tutor orientado.",
]

CONTATOS = ["joana.s@exemplo.com", "(11) 90000-1234", "seu.antonio@exemplo.com", "(11) 90000-5678"]

# bairro, tipo, qtd, dx(m), dy(m), status, horas desde a criacao, opcoes
CASOS = [
    (1, "FERIDO", 1, 40, -60, "REPORTADO", 5, {"extra": 2}),
    (1, "COLONIA", 6, -220, 130, "REPORTADO", 26, {"extra": 1}),
    (1, "FILHOTE", 4, 310, 220, "REPORTADO", 70, {}),
    (1, "DOENTE", 1, -300, -210, "TRIAGEM", 60, {}),
    (1, "RISCO_SANITARIO", 1, 150, 330, "TRIAGEM", 110, {}),
    (1, "FERIDO", 1, -120, -330, "ENCAMINHADO", 55, {"rec": 1, "aceito": True, "extra": 1}),
    (1, "COLONIA", 8, 260, -250, "ENCAMINHADO", 130, {"rec": 5, "extra": 1}),
    (1, "MAUS_TRATOS", 2, -80, 90, "ATENDIDO", 230, {"rec": 7}),
    (1, "DOENTE", 1, 380, 40, "ATENDIDO", 190, {"rec": 1}),
    (1, "FILHOTE", 3, -350, 300, "ARQUIVADO", 160, {"obs": "Filhotes adotados por moradores antes da visita."}),
    (2, "FERIDO", 1, 100, 120, "REPORTADO", 9, {"extra": 1}),
    (2, "COLONIA", 10, -200, -100, "REPORTADO", 48, {"extra": 2}),
    (2, "DOENTE", 2, 240, -180, "TRIAGEM", 75, {}),
    (2, "MAUS_TRATOS", 1, -90, 210, "TRIAGEM", 50, {"recusa": (8, "Fora da área de atuação do posto.")}),
    (2, "FILHOTE", 5, -250, 170, "ENCAMINHADO", 100, {"rec": 4, "extra": 1}),
    (2, "FERIDO", 1, 180, 260, "ENCAMINHADO", 38, {"rec": 4, "aceito": True}),
    (2, "RISCO_SANITARIO", 1, -40, -250, "ATENDIDO", 210, {"rec": 8}),
    (2, "DOENTE", 1, 300, 50, "ATENDIDO", 140, {"rec": 2}),
    (2, "COLONIA", 4, -310, -60, "ARQUIVADO", 120, {"obs": "Local visitado e nenhum animal encontrado."}),
    (3, "FERIDO", 1, 800, -400, "ENCAMINHADO", 30, {"rec": 3, "aceito": True}),
    (3, "FILHOTE", 3, -900, 600, "REPORTADO", 14, {}),
    (3, "COLONIA", 7, 300, 1100, "TRIAGEM", 90, {"recusa": (5, "Sem espaço para tantos animais no momento.")}),
    (3, "DOENTE", 1, -500, -900, "ATENDIDO", 250, {"rec": 3}),
    (3, "MAUS_TRATOS", 1, 1200, 300, "ATENDIDO", 300, {"rec": 7}),
    (3, "RISCO_SANITARIO", 1, -1300, 100, "ARQUIVADO", 200, {"obs": "Duplicado de caso já atendido."}),
    (4, "DOENTE", 1, 400, -700, "REPORTADO", 20, {}),
    (4, "FILHOTE", 2, -800, 500, "TRIAGEM", 66, {}),
    (4, "FERIDO", 1, 900, 900, "ENCAMINHADO", 82, {"rec": 1}),
    (4, "COLONIA", 5, -300, -1100, "ATENDIDO", 180, {"rec": 6}),
    (4, "MAUS_TRATOS", 1, 1100, -200, "ATENDIDO", 260, {"rec": 7}),
    (4, "FILHOTE", 3, -1200, -600, "ARQUIVADO", 140, {"obs": "Filhotes levados pela tutora antes da visita."}),
    (5, "FERIDO", 1, 200, 300, "REPORTADO", 3, {}),
    (5, "COLONIA", 3, -600, -300, "TRIAGEM", 40, {}),
    (5, "DOENTE", 1, 500, -500, "ATENDIDO", 120, {"rec": 4}),
    (5, "FILHOTE", 2, -100, 700, "ATENDIDO", 200, {"rec": 4}),
    (5, "RISCO_SANITARIO", 1, 800, 100, "ATENDIDO", 280, {"rec": 8}),
    (6, "FERIDO", 1, -200, 200, "TRIAGEM", 28, {}),
    (6, "COLONIA", 4, 400, -300, "REPORTADO", 6, {}),
    (6, "DOENTE", 1, -500, -200, "ATENDIDO", 100, {"rec": 4}),
    (6, "MAUS_TRATOS", 1, 100, 500, "ATENDIDO", 170, {"rec": 8}),
]

M_POR_GRAU = 111_000.0
ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"


def esc(texto):
    return "'" + texto.replace("'", "''") + "'"


def ts(minutos_atras):
    """Expressao SQL de LOCALTIMESTAMP menos N minutos, valida em Oracle e H2."""
    m = int(round(minutos_atras))
    dias, resto = divmod(m, 1440)
    horas, mins = divmod(resto, 60)
    partes = ["LOCALTIMESTAMP"]
    if dias:
        partes.append(f"INTERVAL '{dias}' DAY")
    if horas:
        partes.append(f"INTERVAL '{horas}' HOUR")
    if mins:
        partes.append(f"INTERVAL '{mins}' MINUTE")
    return " - ".join(partes)


def codigo(usados):
    while True:
        c = "CP-" + "".join(rng.choice(ALFABETO) for _ in range(6))
        if c not in usados:
            usados.add(c)
            return c


def distancia_km(lat1, lng1, lat2, lng2):
    p1, p2 = math.radians(lat1), math.radians(lat2)
    a = math.sin((p2 - p1) / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(math.radians(lng2 - lng1) / 2) ** 2
    return 12742 * math.asin(math.sqrt(a))


TIPOS_RECURSO = {
    "FERIDO": ["CLINICA", "ONG"], "DOENTE": ["CLINICA", "ONG"], "FILHOTE": ["ONG"],
    "COLONIA": ["UNIDADE_MOVEL", "ONG"], "MAUS_TRATOS": ["ORGAO_PUBLICO"], "RISCO_SANITARIO": ["ORGAO_PUBLICO"],
}


def recurso_mais_proximo(tipo, lat, lng):
    candidatos = [
        (distancia_km(lat, lng, r[2], r[3]), rid) for rid, r in RECURSOS.items()
        if r[1] in TIPOS_RECURSO[tipo] and tipo in r[5].split(",")
    ]
    return min(candidatos)[1]


def main():
    linhas = ["-- Gerado por tools/gerar_seed.py. Todos os registros sao simulados (SIMULADO = 1).", ""]
    vagas = {rid: r[4] for rid, r in RECURSOS.items()}
    usados = set()
    historico, reportes, casos_sql = [], [], []
    hist_id = rep_id = 0
    descr_idx = {t: 0 for t in DESCRICOES}

    for caso_id, (bid, tipo, qtd, dx, dy, status, criado_h, opc) in enumerate(CASOS, start=1):
        _, blat, blng, _ = BAIRROS[bid]
        lat = round(blat + dy / M_POR_GRAU, 6)
        lng = round(blng + dx / (M_POR_GRAU * math.cos(math.radians(blat))), 6)
        celula = f"{round(lat, 2):.2f},{round(lng, 2):.2f}"
        criado = criado_h * 60
        ultimo = max(25, criado * 0.12)

        etapas = [(None, "REPORTADO", "Cidadão", "Reporte recebido")]
        rec = None
        if status != "REPORTADO":
            etapas.append(("REPORTADO", "TRIAGEM", "Marina Duarte", "Triagem iniciada"))
        if status in ("ENCAMINHADO", "ATENDIDO") or "recusa" in opc:
            if "recusa" in opc:
                rec = opc["recusa"][0]
            else:
                rec = opc.get("rec") or recurso_mais_proximo(tipo, lat, lng)
            etapas.append(("TRIAGEM", "ENCAMINHADO", "Marina Duarte", f"Encaminhado para {RECURSOS[rec][0]}"))
        if "recusa" in opc:
            etapas.append(("ENCAMINHADO", "TRIAGEM", RECURSOS[rec][0], "Recusado: " + opc["recusa"][1]))
            rec = None
        if status == "ATENDIDO":
            etapas.append(("ENCAMINHADO", "ATENDIDO", RECURSOS[rec][0], OBS_ATENDIMENTO[caso_id % len(OBS_ATENDIMENTO)]))
        if status == "ARQUIVADO":
            etapas.append(("TRIAGEM", "ARQUIVADO", "Marina Duarte", opc["obs"]))
        if status == "ENCAMINHADO":
            vagas[rec] = max(0, vagas[rec] - 1)
            if opc.get("aceito"):
                etapas.append(("ENCAMINHADO", "ENCAMINHADO", RECURSOS[rec][0], "Aceito pelo recurso"))

        passo = (criado - ultimo) / max(1, len(etapas) - 1)
        for i, (ant, novo, autor, obs) in enumerate(etapas):
            hist_id += 1
            quando = criado - i * passo
            historico.append(
                f"INSERT INTO HISTORICO_STATUS (ID, CASO_ID, STATUS_ANTERIOR, STATUS_NOVO, AUTOR, OBSERVACAO, CRIADO_EM) VALUES "
                f"({hist_id}, {caso_id}, {esc(ant) if ant else 'NULL'}, {esc(novo)}, {esc(autor)}, {esc(obs)}, {ts(quando)});"
            )
        atualizado = ultimo

        n_reportes = 1 + opc.get("extra", 0)
        for k in range(n_reportes):
            rep_id += 1
            idx = descr_idx[tipo]
            descr_idx[tipo] += 1
            texto = DESCRICOES[tipo][idx % len(DESCRICOES[tipo])] if k == 0 else rng.choice(
                ["Continua no mesmo lugar.", "Mais um morador avisando: a situação segue igual.", "Passei hoje e o animal ainda está aqui."]
            )
            quando = criado if k == 0 else rng.uniform(ultimo, criado * 0.9)
            contato = esc(rng.choice(CONTATOS)) if rng.random() < 0.35 else "NULL"
            reportes.append(
                f"INSERT INTO REPORTE (ID, CASO_ID, DESCRICAO, FOTO_PATH, CONTATO_OPCIONAL, CRIADO_EM) VALUES "
                f"({rep_id}, {caso_id}, {esc(texto)}, NULL, {contato}, {ts(quando)});"
            )

        casos_sql.append(
            "INSERT INTO CASO (ID, TIPO, QTD_ANIMAIS, LAT, LNG, CELULA, BAIRRO_ID, STATUS, SCORE, RECURSO_ID, "
            "CODIGO_ACOMPANHAMENTO, CRIADO_EM, ATUALIZADO_EM, SIMULADO) VALUES "
            f"({caso_id}, {esc(tipo)}, {qtd}, {lat}, {lng}, {esc(celula)}, {bid}, {esc(status)}, 0, "
            f"{rec if rec else 'NULL'}, {esc(codigo(usados))}, {ts(criado)}, {ts(atualizado)}, 1);"
        )

    linhas += [
        f"INSERT INTO BAIRRO (ID, NOME, LAT, LNG, INDICE_VULNERABILIDADE) VALUES ({i}, {esc(b[0])}, {b[1]}, {b[2]}, {b[3]});"
        for i, b in BAIRROS.items()
    ]
    linhas.append("")
    linhas += [
        f"INSERT INTO RECURSO (ID, NOME, TIPO, LAT, LNG, VAGAS_DISPONIVEIS, TIPOS_ACEITOS, ATIVO) VALUES "
        f"({i}, {esc(r[0])}, {esc(r[1])}, {r[2]}, {r[3]}, {vagas[i]}, {esc(r[5])}, 1);"
        for i, r in RECURSOS.items()
    ]
    linhas.append("")
    linhas += [
        f"INSERT INTO USUARIO (ID, NOME, PERFIL, RECURSO_ID) VALUES ({u[0]}, {esc(u[1])}, {esc(u[2])}, {u[3] if u[3] else 'NULL'});"
        for u in USUARIOS
    ]
    linhas.append("")
    linhas += casos_sql + [""] + reportes + [""] + historico + [""]
    DESTINO.write_text("\n".join(linhas), encoding="utf-8")
    print(f"{DESTINO}: {len(casos_sql)} casos, {len(reportes)} reportes, {len(historico)} historicos")


main()
