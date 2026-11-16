(function () {
  const { api, iniciar, esc, TIPOS, STATUS, RECURSOS, quando, haTempo, classeScore, chipStatus, mapaBase } = window.CPET;

  const estado = { casos: [], selecionado: null, filtros: { status: "ABERTOS", tipo: "", bairro: "" } };
  let mapa;
  let camadaCasos;
  let camadaCelulas;
  const marcadores = new Map();
  let limitesDosBairros = null;
  let enquadrarAoAbrirMapa = false;

  const COR_SCORE = { alto: "#B8431D", medio: "#C9962E", baixo: "#6E7A73" };

  iniciar("painel.html", { exigir: "OPERADOR" }).then((usuario) => {
    if (!usuario) return;
    document.body.dataset.usuario = usuario.nome;
    montar();
  });

  function montar() {
    mapa = mapaBase("mapa", [-23.58, -46.63], 10);
    camadaCelulas = L.layerGroup().addTo(mapa);
    camadaCasos = L.layerGroup().addTo(mapa);

    Object.entries(TIPOS).forEach(([k, v]) => document.getElementById("f-tipo").insertAdjacentHTML("beforeend", `<option value="${k}">${v}</option>`));
    ["f-status", "f-tipo", "f-bairro"].forEach((id) =>
      document.getElementById(id).addEventListener("change", () => {
        estado.filtros = {
          status: document.getElementById("f-status").value,
          tipo: document.getElementById("f-tipo").value,
          bairro: document.getElementById("f-bairro").value,
        };
        desenhar(true);
      })
    );

    document.querySelectorAll(".abas button").forEach((b) => b.addEventListener("click", () => abrirAba(b.dataset.aba)));
    document.getElementById("btn-alertas").addEventListener("click", alternarAlertas);
    document.addEventListener("click", (ev) => {
      if (!ev.target.closest("#painel-alertas") && !ev.target.closest("#btn-alertas")) fecharAlertas();
    });
    document.addEventListener("keydown", (ev) => ev.key === "Escape" && fecharAlertas());

    api("/api/dados/bairros").then((bairros) => {
      if (!bairros.length) return;
      limitesDosBairros = bairros.map((b) => [b.lat, b.lng]);
      mapa.fitBounds(limitesDosBairros, { padding: [60, 60], maxZoom: 12 });
      enquadrarAoAbrirMapa = true;
    }).catch(() => {});

    iniciarFila();
    setInterval(carregarAlertas, 30000);
  }

  function iniciarFila() {
    recarregar()
      .then(() => {
        const id = Number(location.hash.replace("#caso-", ""));
        if (id) selecionar(id);
      })
      .catch((e) => {
        document.getElementById("fila").innerHTML = `<li class="vazio"><div class="alerta-erro mb-2">${esc(e.erro || "Não foi possível carregar a fila.")}</div><button type="button" class="btn-c sec pequeno" id="tentar-fila">Tentar de novo</button></li>`;
        document.getElementById("tentar-fila").addEventListener("click", iniciarFila);
      });
  }

  function abrirAba(nome) {
    const grade = document.getElementById("grade");
    grade.className = `painel-grade ver-${nome}`;
    document.querySelectorAll(".abas button").forEach((b) => b.setAttribute("aria-selected", String(b.dataset.aba === nome)));
    if (nome === "mapa") {
      setTimeout(() => {
        mapa.invalidateSize();
        if (enquadrarAoAbrirMapa && limitesDosBairros) {
          mapa.fitBounds(limitesDosBairros, { padding: [60, 60], maxZoom: 12 });
          enquadrarAoAbrirMapa = false;
        }
      }, 50);
    }
  }

  async function recarregar() {
    estado.casos = await api("/api/casos");
    const bairros = new Map(estado.casos.map((c) => [c.bairroId, c.bairro]));
    const sel = document.getElementById("f-bairro");
    const atual = sel.value;
    sel.innerHTML = '<option value="">Todos os bairros</option>' + [...bairros].sort((a, b) => a[1].localeCompare(b[1])).map(([id, nome]) => `<option value="${id}">${esc(nome)}</option>`).join("");
    sel.value = atual;
    desenhar(false);
    carregarAlertas();
  }

  function filtrados() {
    const { status, tipo, bairro } = estado.filtros;
    return estado.casos.filter((c) => {
      if (status === "ABERTOS" && !["REPORTADO", "TRIAGEM", "ENCAMINHADO"].includes(c.status)) return false;
      if (status !== "ABERTOS" && status !== "TODOS" && c.status !== status) return false;
      if (tipo && c.tipo !== tipo) return false;
      if (bairro && String(c.bairroId) !== bairro) return false;
      return true;
    });
  }

  function desenhar(ajustarMapa) {
    const lista = filtrados();
    document.getElementById("resumo-fila").textContent = `${lista.length} ${lista.length === 1 ? "caso" : "casos"}`;
    const fila = document.getElementById("fila");
    fila.innerHTML = lista.length
      ? lista
          .map((c) => {
            const aberto = ["REPORTADO", "TRIAGEM", "ENCAMINHADO"].includes(c.status);
            return `<li><button type="button" data-id="${c.id}" aria-current="${c.id === estado.selecionado}">
              <span class="score ${aberto ? classeScore(c.score) : "baixo"}" title="Prioridade">${aberto ? c.score : "-"}</span>
              <span class="linha1"><b>${TIPOS[c.tipo]}</b>${chipStatus(c.status)}</span>
              <span class="linha2"><span>${esc(c.bairro)}</span><span>${c.qtdAnimais} ${c.qtdAnimais === 1 ? "animal" : "animais"}</span><span>${haTempo(c.criadoEm)}</span>${c.reportes > 1 ? `<span>${c.reportes} reportes</span>` : ""}</span>
            </button></li>`;
          })
          .join("")
      : '<li class="vazio">Nenhum caso com esses filtros.</li>';
    fila.querySelectorAll("button").forEach((b) => b.addEventListener("click", () => selecionar(Number(b.dataset.id))));
    desenharMapa(lista, ajustarMapa);
  }

  function desenharMapa(lista, ajustar) {
    camadaCasos.clearLayers();
    camadaCelulas.clearLayers();
    marcadores.clear();

    const abertosPorCelula = new Map();
    estado.casos
      .filter((c) => ["REPORTADO", "TRIAGEM", "ENCAMINHADO"].includes(c.status))
      .forEach((c) => abertosPorCelula.set(c.celula, (abertosPorCelula.get(c.celula) || 0) + 1));
    abertosPorCelula.forEach((n, celula) => {
      if (n < 5) return;
      const [lat, lng] = celula.split(",").map(Number);
      L.rectangle([[lat - 0.005, lng - 0.005], [lat + 0.005, lng + 0.005]], {
        color: "#B8431D", weight: 1.5, dashArray: "5 4", fillColor: "#B8431D", fillOpacity: 0.08, interactive: false,
      }).addTo(camadaCelulas);
      L.marker([lat + 0.005, lng - 0.005], {
        interactive: false,
        icon: L.divIcon({ className: "", html: `<span class="chip" style="background:#fff;color:#8F3112;border-color:#B8431D;transform:translateY(-100%)">${n} abertos na célula</span>`, iconSize: [0, 0] }),
      }).addTo(camadaCelulas);
    });

    const pontos = [];
    lista.forEach((c) => {
      const aberto = ["REPORTADO", "TRIAGEM", "ENCAMINHADO"].includes(c.status);
      const m = L.circleMarker([c.lat, c.lng], {
        radius: aberto ? 7 + Math.round(c.score / 14) : 5,
        color: "#1B2320",
        weight: c.id === estado.selecionado ? 3 : 1,
        fillColor: aberto ? COR_SCORE[classeScore(c.score)] : "#B3AA95",
        fillOpacity: aberto ? 0.85 : 0.5,
      })
        .bindTooltip(`${TIPOS[c.tipo]}, ${esc(c.bairro)}${aberto ? `, prioridade ${c.score}` : ""}`)
        .on("click", () => selecionar(c.id))
        .addTo(camadaCasos);
      marcadores.set(c.id, m);
      pontos.push([c.lat, c.lng]);
    });
    if (ajustar && pontos.length) mapa.fitBounds(pontos, { padding: [40, 40], maxZoom: 15 });
  }

  async function selecionar(id) {
    estado.selecionado = id;
    history.replaceState(null, "", `#caso-${id}`);
    desenhar(false);
    const caixa = document.getElementById("detalhe");
    caixa.innerHTML = '<div class="vazio">Carregando caso...</div>';
    abrirAbaSeCompacto();
    try {
      const d = await api(`/api/casos/${id}`);
      renderDetalhe(d);
      const m = marcadores.get(id);
      if (m) mapa.panTo(m.getLatLng());
    } catch (e) {
      caixa.innerHTML = `<div class="vazio"><div class="alerta-erro">${esc(e.erro)}</div></div>`;
    }
  }

  function abrirAbaSeCompacto() {
    if (window.matchMedia("(max-width: 1100px)").matches) abrirAba("detalhe");
  }

  function renderDetalhe(d, aviso) {
    const c = d.caso;
    const aberto = ["REPORTADO", "TRIAGEM", "ENCAMINHADO"].includes(c.status);
    const reportes = d.reportes
      .map(
        (r) => `<div class="reporte">
          <span class="quando">${quando(r.criadoEm)}</span>
          <p>${r.descricao ? esc(r.descricao) : '<span class="fraco">Sem descrição.</span>'}</p>
          ${r.contato ? `<div class="contato">Contato: <span>${esc(r.contato)}</span></div>` : ""}
          ${r.fotoUrl ? `<img class="foto-caso" src="${esc(r.fotoUrl)}" alt="Foto enviada no reporte de ${quando(r.criadoEm)}">` : '<div class="sem-foto">Sem foto neste reporte.</div>'}
        </div>`
      )
      .join("");

    const fatores = d.score.fatores
      .map(
        (f) => `<div class="fator">
          <div class="topo-fator"><span>${esc(f.rotulo)}</span><span>${f.pontos} / ${f.maximo}</span></div>
          <div class="trilho"><i style="width:${f.maximo ? (f.pontos / f.maximo) * 100 : 0}%"></i></div>
          <small>${esc(f.explicacao)}</small>
        </div>`
      )
      .join("");

    let blocoRecurso = "";
    if (c.status === "REPORTADO" || c.status === "TRIAGEM") {
      blocoRecurso = `<div class="bloco"><h3>Recursos sugeridos</h3>${
        d.sugestoes.length
          ? d.sugestoes
              .map(
                (s) => `<div class="sugestao">
                  <b>${esc(s.nome)}</b>
                  <button class="btn-c pequeno" type="button" data-encaminhar="${s.recursoId}">Encaminhar</button>
                  <span class="info"><span>${RECURSOS[s.tipo]}</span><span class="mono">${String(s.distanciaKm).replace(".", ",")} km</span><span>${s.vagas} ${s.vagas === 1 ? "vaga" : "vagas"}</span></span>
                </div>`
              )
              .join("")
          : '<p class="suave">Nenhum recurso ativo e com vaga atende este tipo de caso agora.</p>'
      }<p class="nota-sim">O encaminhamento fica registrado no C-PET. Nenhuma mensagem sai para o recurso: simulado na demonstração.</p></div>`;
    } else if (c.status === "ENCAMINHADO") {
      blocoRecurso = `<div class="bloco"><h3>Encaminhamento</h3><p><b>${esc(c.recurso)}</b></p><p class="suave">${d.aceito ? "O recurso aceitou o caso." : "Aguardando o aceite do recurso."}</p><p class="nota-sim">Registro interno do C-PET. Nenhuma mensagem sai para o recurso: simulado na demonstração.</p></div>`;
    }

    const blocoArquivar = aberto
      ? `<div class="bloco"><details class="arquivar"><summary>Arquivar este caso</summary>
          <form id="form-arquivar">
            <label class="so-leitor" for="obs-arquivar">Motivo</label>
            <textarea class="form-control" id="obs-arquivar" rows="2" maxlength="500" placeholder="Por que o caso será arquivado"></textarea>
            <div class="alerta-erro mb-2" id="erro-arquivar" hidden></div>
            <button class="btn-c perigo pequeno" type="submit">Arquivar caso</button>
          </form></details></div>`
      : "";

    const historico = d.historico
      .map(
        (h) => `<li><span class="quando">${quando(h.criadoEm)}</span><span>${h.anterior === h.novo ? "Aceito pelo recurso" : chipStatus(h.novo)}<small>${esc(h.autor)}${h.observacao && h.anterior !== h.novo ? `: ${esc(h.observacao)}` : ""}</small></span></li>`
      )
      .join("");

    document.getElementById("detalhe").innerHTML = `<div class="detalhe">
      ${aviso ? `<div class="aviso-ok" role="status">${esc(aviso)}</div>` : ""}
      <h2>${TIPOS[c.tipo]}</h2>
      <div class="meta">${chipStatus(c.status)}<span class="mono">${esc(c.codigo)}</span><span>${esc(c.bairro)}</span><span>${c.qtdAnimais} ${c.qtdAnimais === 1 ? "animal" : "animais"}</span><span>aberto ${haTempo(c.criadoEm)}</span></div>
      <p class="mt-2" style="font-size:12.5px;color:var(--tinta-2)">Local exato: <span class="mono">${c.lat.toFixed(5)}, ${c.lng.toFixed(5)}</span>
        <a href="https://www.openstreetmap.org/?mlat=${c.lat}&mlon=${c.lng}#map=18/${c.lat}/${c.lng}" target="_blank" rel="noopener">abrir mapa</a></p>

      <div class="bloco"><h3>Por que esta prioridade</h3>
        <div class="porque">
          <div class="total ${aberto ? classeScore(d.score.total) : ""}">${d.score.total}<small>de 100</small></div>
          <div>${fatores}</div>
        </div>
      </div>

      ${blocoRecurso}

      <div class="bloco"><h3>Reportes (${d.reportes.length})</h3>${reportes}</div>
      ${blocoArquivar}
      <div class="bloco"><h3>Histórico</h3><ul class="historico">${historico}</ul></div>
    </div>`;

    document.querySelectorAll("[data-encaminhar]").forEach((b) =>
      b.addEventListener("click", () => encaminhar(c.id, Number(b.dataset.encaminhar), b))
    );
    const form = document.getElementById("form-arquivar");
    if (form) form.addEventListener("submit", (ev) => arquivarCaso(ev, c.id));
  }

  async function encaminhar(id, recursoId, botao) {
    botao.disabled = true;
    botao.textContent = "Encaminhando...";
    try {
      const r = await api(`/api/casos/${id}/encaminhar`, { json: { recursoId } });
      await recarregar();
      const d = await api(`/api/casos/${id}`);
      renderDetalhe(d, `Caso encaminhado para ${r.recurso}.`);
    } catch (e) {
      botao.disabled = false;
      botao.textContent = "Encaminhar";
      alert(e.erro);
    }
  }

  async function arquivarCaso(ev, id) {
    ev.preventDefault();
    const obs = document.getElementById("obs-arquivar").value.trim();
    const erro = document.getElementById("erro-arquivar");
    if (!obs) {
      erro.textContent = "Escreva o motivo do arquivamento.";
      erro.hidden = false;
      return;
    }
    try {
      await api(`/api/casos/${id}/arquivar`, { json: { observacao: obs } });
      await recarregar();
      renderDetalhe(await api(`/api/casos/${id}`), "Caso arquivado.");
    } catch (e) {
      erro.textContent = e.erro;
      erro.hidden = false;
    }
  }

  async function carregarAlertas() {
    try {
      const a = await api("/api/alertas");
      const selo = document.getElementById("selo-alertas");
      selo.textContent = a.total;
      selo.classList.toggle("zero", a.total === 0);
      const caixa = document.getElementById("painel-alertas");
      caixa.innerHTML = `
        <p class="nota-sim mb-2">Alertas só aparecem aqui. Nenhum aviso é enviado a ninguém: simulado na demonstração.</p>
        <h3>Células com 5 ou mais casos abertos</h3>
        ${a.celulasCriticas.length ? a.celulasCriticas.map((c) => `<button type="button" class="item-alerta" data-celula="${esc(c.celula)}"><span class="score alto">${c.abertos}</span><span class="quem"><b>${esc(c.bairro)}</b><small class="mono">${esc(c.celula)}</small></span></button>`).join("") : '<p class="suave">Nenhuma célula crítica.</p>'}
        <h3>Casos abertos atualizados nas últimas 24 h</h3>
        ${a.casosAtualizados.length ? a.casosAtualizados.map((c) => `<button type="button" class="item-alerta" data-caso="${c.id}"><span class="score ${classeScore(c.score)}">${c.score}</span><span class="quem"><b>${TIPOS[c.tipo]}</b>, ${esc(c.bairro)}<small>${STATUS[c.status]} ${haTempo(c.atualizadoEm)}</small></span></button>`).join("") : '<p class="suave">Nada novo.</p>'}`;
      caixa.querySelectorAll("[data-caso]").forEach((b) => b.addEventListener("click", () => { fecharAlertas(); selecionar(Number(b.dataset.caso)); }));
      caixa.querySelectorAll("[data-celula]").forEach((b) =>
        b.addEventListener("click", () => {
          fecharAlertas();
          const [lat, lng] = b.dataset.celula.split(",").map(Number);
          abrirAba("mapa");
          mapa.fitBounds([[lat - 0.005, lng - 0.005], [lat + 0.005, lng + 0.005]], { padding: [60, 60] });
        })
      );
    } catch (e) {
      /* o badge simplesmente não atualiza */
    }
  }

  function alternarAlertas() {
    const caixa = document.getElementById("painel-alertas");
    const abrir = caixa.hidden;
    caixa.hidden = !abrir;
    document.getElementById("btn-alertas").setAttribute("aria-expanded", String(abrir));
  }

  function fecharAlertas() {
    document.getElementById("painel-alertas").hidden = true;
    document.getElementById("btn-alertas").setAttribute("aria-expanded", "false");
  }
})();
