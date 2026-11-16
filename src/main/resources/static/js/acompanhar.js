(function () {
  const { api, iniciar, TIPOS, chipStatus, quando, esc } = window.CPET;
  iniciar("acompanhar.html");

  const campo = document.getElementById("codigo");
  const erro = document.getElementById("erro");
  const resultado = document.getElementById("resultado");

  const PROXIMOS = {
    REPORTADO: ["Em análise pela central", "Encaminhado a um parceiro da rede", "Atendido"],
    TRIAGEM: ["Encaminhado a um parceiro da rede", "Atendido"],
    ENCAMINHADO: ["Atendido"],
    ATENDIDO: [],
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
    const feitas = r.etapas.map(
      (e) => `<li><strong>${esc(e.titulo)}</strong><span class="quando">${quando(e.em)}</span></li>`
    );
    const futuras = (PROXIMOS[r.status] || []).map(
      (t) => `<li class="futuro"><strong>${esc(t)}</strong><span class="quando">Próxima etapa</span></li>`
    );
    document.getElementById("etapas").innerHTML = feitas.concat(futuras).join("");
    resultado.hidden = false;
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
