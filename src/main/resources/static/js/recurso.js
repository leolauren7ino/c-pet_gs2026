(function () {
  const { api, iniciar, esc, TIPOS, RECURSOS, quando, haTempo, classeScore, chipStatus, mapaBase } = window.CPET;

  let mapa;
  let camada;

  iniciar("recurso.html", { exigir: "AGENTE" }).then((usuario) => {
    if (!usuario) return;
    mapa = mapaBase("mapa", [-23.58, -46.63], 11);
    camada = L.layerGroup().addTo(mapa);
    document.querySelectorAll(".abas button").forEach((b) =>
      b.addEventListener("click", () => {
        document.getElementById("grade").className = `recurso-grade ver-${b.dataset.aba}`;
        document.querySelectorAll(".abas button").forEach((x) => x.setAttribute("aria-selected", String(x === b)));
        if (b.dataset.aba === "mapa") setTimeout(() => mapa.invalidateSize(), 50);
      })
    );
    iniciarLista();
  });

  function iniciarLista() {
    carregar().catch((e) => {
      document.getElementById("lista").innerHTML = `<div class="alerta-erro">${esc(e.erro || "Não foi possível carregar os casos.")}</div><button type="button" class="btn-c sec pequeno" id="tentar-lista">Tentar de novo</button>`;
      document.getElementById("tentar-lista").addEventListener("click", iniciarLista);
    });
  }

  async function carregar(aviso) {
    const [recurso, recebidos] = await Promise.all([api("/api/recurso"), api("/api/recurso/casos")]);
    const casos = [...recebidos].sort((a, b) => Number(a.aceito) - Number(b.aceito) || b.caso.score - a.caso.score);
    const novos = casos.filter((d) => !d.aceito).length;
    document.getElementById("nome-recurso").textContent = recurso.nome;
    document.getElementById("tipo-recurso").textContent = RECURSOS[recurso.tipo];
    document.getElementById("vagas").innerHTML = `${recurso.vagas}<small>${recurso.vagas === 1 ? "vaga disponível" : "vagas disponíveis"}</small>`;
    document.getElementById("resumo-casos").innerHTML = novos
      ? `<b>${novos} aguardando seu aceite</b>, ${casos.length} no total`
      : `${casos.length} ${casos.length === 1 ? "caso encaminhado" : "casos encaminhados"}, nenhum aguardando aceite`;

    const lista = document.getElementById("lista");
    lista.innerHTML =
      (aviso ? `<div class="aviso-ok" role="status">${esc(aviso)}</div>` : "") +
      (casos.length ? casos.map(cartao).join("") : '<div class="vazio">Nenhum caso encaminhado ao seu recurso agora. Quando a central encaminhar, ele aparece aqui.</div>');
    casos.forEach((d) => ligar(d));

    camada.clearLayers();
    const pontos = [];
    casos.forEach((d) => {
      const c = d.caso;
      pontos.push([c.lat, c.lng]);
      L.circleMarker([c.lat, c.lng], {
        radius: 8 + Math.round(c.score / 14), color: "#17252A", weight: 1,
        fillColor: c.score >= 70 ? "#A9431E" : "#C9962E", fillOpacity: 0.85,
      })
        .bindTooltip(`${TIPOS[c.tipo]}, ${esc(c.bairro)}, prioridade ${c.score}`)
        .on("click", () => document.getElementById(`caso-${c.id}`)?.scrollIntoView({ behavior: "smooth", block: "center" }))
        .addTo(camada);
    });
    if (pontos.length) mapa.fitBounds(pontos, { padding: [50, 50], maxZoom: 14 });
  }

  function linkContato(texto) {
    const digitos = texto.replace(/\D/g, "");
    if (texto.includes("@")) return `<a href="mailto:${esc(texto)}">${esc(texto)}</a>`;
    if (digitos.length >= 8) return `<a href="tel:+55${digitos}">${esc(texto)}</a>`;
    return `<span>${esc(texto)}</span>`;
  }

  function cartao(d) {
    const c = d.caso;
    const foto = d.reportes.find((r) => r.fotoUrl);
    const contatos = [...new Set(d.reportes.map((r) => r.contato).filter(Boolean))];
    const descricoes = d.reportes.filter((r) => r.descricao).map((r) => `<p>${esc(r.descricao)}</p>`).join("");
    const notaCentral = d.historico.filter((h) => h.novo === "ENCAMINHADO" && h.anterior !== "ENCAMINHADO").pop();
    return `<article class="caso-card ${d.aceito ? "" : "destaque"}" id="caso-${c.id}">
      <header>
        <span class="score ${classeScore(c.score)}" title="Prioridade">${c.score}</span>
        <h2>${TIPOS[c.tipo]}</h2>
        <span class="chip ${d.aceito ? "ENCAMINHADO" : "REPORTADO"}">${d.aceito ? "Aceito" : "Aguardando aceite"}</span>
        <span class="direita"><span class="mono">${esc(c.codigo)}</span></span>
      </header>
      <div class="corpo ${foto ? "" : "sem-foto-col"}">
        <div>
          ${descricoes || '<p class="fraco">Sem descrição.</p>'}
          <dl class="dados-campo">
            <dt>Bairro</dt><dd>${esc(c.bairro)}, ${c.qtdAnimais} ${c.qtdAnimais === 1 ? "animal" : "animais"}</dd>
            <dt>Local exato</dt><dd><span class="mono">${c.lat.toFixed(5)}, ${c.lng.toFixed(5)}</span> <a class="btn-c sec pequeno" href="https://www.openstreetmap.org/?mlat=${c.lat}&mlon=${c.lng}#map=18/${c.lat}/${c.lng}" target="_blank" rel="noopener">Ver o ponto no mapa</a></dd>
            <dt>Contato</dt><dd>${contatos.length ? contatos.map(linkContato).join(", ") : '<span class="fraco">Não informado</span>'}</dd>
            <dt>Reportes</dt><dd>${d.reportes.length}, o primeiro ${haTempo(c.criadoEm)}</dd>
            ${notaCentral ? `<dt>Encaminhado</dt><dd>${quando(notaCentral.criadoEm)} por ${esc(notaCentral.autor)}</dd>` : ""}
          </dl>
        </div>
        ${foto ? `<img src="${esc(foto.fotoUrl)}" alt="Foto do caso">` : ""}
      </div>
      <div class="acoes" id="acoes-${c.id}">
        ${d.aceito ? "" : `<button class="btn-c pequeno" type="button" data-acao="aceitar">Aceitar caso</button>`}
        <button class="btn-c ${d.aceito ? "" : "sec"} pequeno" type="button" data-acao="atender">Marcar atendido</button>
        <button class="btn-c perigo pequeno" type="button" data-acao="recusar">Recusar</button>
      </div>
      <div id="form-${c.id}"></div>
    </article>`;
  }

  function ligar(d) {
    const id = d.caso.id;
    const acoes = document.getElementById(`acoes-${id}`);
    acoes.querySelectorAll("button").forEach((b) =>
      b.addEventListener("click", () => {
        if (b.dataset.acao === "aceitar") executar(id, "aceitar", null, b);
        else abrirFormulario(id, b.dataset.acao);
      })
    );
  }

  function abrirFormulario(id, acao) {
    const rotulos = {
      atender: ["Observação do atendimento", "Descreva o que foi feito", "Registrar atendimento", "btn-c"],
      recusar: ["Motivo da recusa", "Por que o recurso não pode receber o caso", "Confirmar recusa", "btn-c perigo"],
    };
    const [titulo, dica, botao, classe] = rotulos[acao];
    const alvo = document.getElementById(`form-${id}`);
    alvo.innerHTML = `<form class="obs" novalidate>
      <label class="rotulo" for="obs-${id}" style="font-weight:600">${titulo}</label>
      <textarea class="form-control" id="obs-${id}" rows="2" maxlength="480" placeholder="${dica}"></textarea>
      <div class="alerta-erro mb-2" hidden></div>
      <div class="d-flex gap-2">
        <button class="${classe} pequeno" type="submit">${botao}</button>
        <button class="btn-c sec pequeno" type="button" data-cancelar>Cancelar</button>
      </div>
    </form>`;
    const form = alvo.querySelector("form");
    form.querySelector("textarea").focus();
    form.querySelector("[data-cancelar]").addEventListener("click", () => (alvo.innerHTML = ""));
    form.addEventListener("submit", (ev) => {
      ev.preventDefault();
      const obs = form.querySelector("textarea").value.trim();
      const erro = form.querySelector(".alerta-erro");
      if (!obs) {
        erro.textContent = acao === "atender" ? "Descreva o atendimento realizado." : "Informe o motivo da recusa.";
        erro.hidden = false;
        return;
      }
      executar(id, acao, obs, form.querySelector("button[type=submit]"));
    });
  }

  async function executar(id, acao, observacao, botao) {
    botao.disabled = true;
    try {
      await api(`/api/recurso/casos/${id}/${acao}`, { json: observacao ? { observacao } : {} });
      const mensagens = {
        aceitar: "Caso aceito. Quando o animal for atendido, registre o atendimento.",
        atender: "Atendimento registrado. A vaga foi devolvida ao seu recurso.",
        recusar: "Caso recusado e devolvido para a triagem da central. A vaga foi devolvida.",
      };
      await carregar(mensagens[acao]);
    } catch (e) {
      botao.disabled = false;
      const erro = document.querySelector(`#form-${id} .alerta-erro`);
      if (erro) {
        erro.textContent = e.erro;
        erro.hidden = false;
      } else {
        alert(e.erro);
      }
    }
  }
})();
