(function () {
  const { api, iniciar, TIPOS, STATUS, chipStatus, quando, quandoCompleto, esc } = window.CPET;
  iniciar("acompanhar.html");

  const campo = document.getElementById("codigo");
  const erro = document.getElementById("erro");
  const resultado = document.getElementById("resultado");

  const PROXIMOS = {
    REPORTADO: ["Em análise pela central", "Encaminhado a um parceiro da rede", "Atendido ou adotado"],
    TRIAGEM: ["Encaminhado a um parceiro da rede", "Atendido ou adotado"],
    ENCAMINHADO: ["Atendido ou adotado"],
    ATENDIDO: [],
    ADOTADO: [],
    ARQUIVADO: [],
  };

  async function buscar(codigo) {
    erro.hidden = true;
    resultado.hidden = true;
    const limpo = codigo.trim();
    if (!limpo) {
      erro.textContent = "Digite o código do seu reporte.";
      erro.hidden = false;
      return;
    }
    try {
      const r = await api(`/api/acompanhar/${encodeURIComponent(limpo)}`);
      mostrar(r);
    } catch (e) {
      erro.textContent = e.erro;
      erro.hidden = false;
    }
  }

  function mostrar(r) {
    document.getElementById("r-codigo").textContent = r.codigo;
    document.getElementById("r-tipo").textContent = TIPOS[r.tipo];
    document.getElementById("r-bairro").textContent = r.bairro;
    document.getElementById("r-status").innerHTML = chipStatus(r.status);
    document.getElementById("r-atualizado").textContent = quando(r.atualizadoEm);
    const feitas = r.etapas.map((e, i) => {
      const anterior = i > 0 ? STATUS[r.etapas[i - 1].status] : "início";
      const quem = i === 0 ? "Cidadão" : "Equipe de Atendimento";
      return `<li><strong>${esc(e.titulo)}</strong><span class="quando">${quandoCompleto(e.em)}</span><span class="quando">${anterior} → ${STATUS[e.status]} · Responsável: ${quem}</span></li>`;
    });
    const futuras = (PROXIMOS[r.status] || []).map(
      (t) => `<li class="futuro"><strong>${esc(t)}</strong><span class="quando">Próxima etapa</span></li>`
    );
    document.getElementById("etapas").innerHTML = feitas.concat(futuras).join("");
    mostrarCampanha(r.campanha);
    resultado.hidden = false;
  }

  const brl = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

  function mostrarCampanha(c) {
    const caixa = document.getElementById("campanha");
    if (!c) {
      caixa.hidden = true;
      return;
    }
    const pct = Math.min(100, Math.round((Number(c.valorArrecadado) / Number(c.meta)) * 100));
    caixa.innerHTML = `<h2 class="mb-2">Campanha de apoio</h2>
      <p class="suave">O recurso responsável pediu apoio extra para este caso.</p>
      <p class="campanha-valores"><span class="mono">${brl.format(Number(c.valorArrecadado))}</span> de <span class="mono">${brl.format(Number(c.meta))}</span> (${pct}%)</p>
      <div class="trilho-campanha" role="img" aria-label="${pct}% da meta"><i style="width:${pct}%"></i></div>
      ${c.ativa
        ? `<form class="contribuir" novalidate>
            <label class="rotulo" for="valor-contrib">Valor (R$)</label>
            <div class="linha-contribuir"><input class="form-control mono" id="valor-contrib" type="number" min="1" max="10000" step="1" inputmode="numeric" value="20">
            <button class="btn-c" type="submit">Contribuir (simulado)</button></div>
            <p class="nota-sim">Contribuição simulada, sem pagamento real. O valor só soma no total da campanha.</p>
            <div class="alerta-erro mt-2" hidden></div>
          </form>`
        : '<p class="nota-sim">Campanha encerrada: a meta foi atingida. Os valores desta demonstração são simulados.</p>'}`;
    caixa.hidden = false;
    const form = caixa.querySelector("form");
    if (!form) return;
    form.addEventListener("submit", async (ev) => {
      ev.preventDefault();
      const erroForm = form.querySelector(".alerta-erro");
      erroForm.hidden = true;
      try {
        const nova = await api(`/api/campanhas/${c.id}/contribuicoes`, { json: { valor: Number(form.querySelector("input").value) } });
        mostrarCampanha(nova);
      } catch (e) {
        erroForm.textContent = e.erro;
        erroForm.hidden = false;
      }
    });
  }

  document.getElementById("busca").addEventListener("submit", (ev) => {
    ev.preventDefault();
    buscar(campo.value);
  });

  function codigosGuardados() {
    try {
      return JSON.parse(localStorage.getItem("cpet.codigos") || "[]");
    } catch (e) {
      return [];
    }
  }

  function mostrarGuardados(lista) {
    const caixa = document.getElementById("guardados");
    if (!lista.length) return;
    caixa.innerHTML =
      '<p class="titulo-secao">Reportes feitos neste aparelho</p>' +
      lista.map((c) => `<button type="button" class="btn-c sec pequeno mono" data-codigo="${esc(c.codigo)}">${esc(c.codigo)}</button>`).join(" ");
    caixa.hidden = false;
    caixa.querySelectorAll("button").forEach((b) =>
      b.addEventListener("click", () => {
        campo.value = b.dataset.codigo;
        buscar(b.dataset.codigo);
      })
    );
  }

  const guardados = codigosGuardados();
  mostrarGuardados(guardados);
  const inicial = new URLSearchParams(location.search).get("codigo") || (guardados[0] && guardados[0].codigo);
  if (inicial) {
    campo.value = inicial;
    buscar(inicial);
  }
})();
