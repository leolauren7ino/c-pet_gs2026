(function () {
  const { api, iniciar, mapaBase } = window.CPET;
  iniciar("index.html");

  const SAO_PAULO = [-23.5505, -46.6333];
  const mapa = mapaBase("mapa", SAO_PAULO, 11);
  window.CPET.mapaReporte = mapa;
  let pino = null;
  let ponto = null;

  const leitura = document.getElementById("leitura-local");
  const erroLocal = document.getElementById("erro-local");

  function marcar(lat, lng, aproximar) {
    ponto = { lat, lng };
    if (!pino) {
      pino = L.marker([lat, lng], { draggable: true, keyboard: true, title: "Local do animal" }).addTo(mapa);
      pino.on("dragend", () => {
        const p = pino.getLatLng();
        marcar(p.lat, p.lng, false);
      });
    } else {
      pino.setLatLng([lat, lng]);
    }
    if (aproximar) mapa.setView([lat, lng], Math.max(mapa.getZoom(), 16));
    leitura.textContent = "Ponto marcado no mapa. Arraste o pino se precisar ajustar.";
    leitura.classList.add("ok");
    erroLocal.hidden = true;
  }

  mapa.on("click", (e) => marcar(e.latlng.lat, e.latlng.lng, false));

  document.getElementById("localizar").addEventListener("click", () => {
    if (!navigator.geolocation) {
      mostrarErroLocal("Seu navegador não informa a localização. Toque no mapa para marcar o ponto.");
      return;
    }
    leitura.textContent = "Buscando sua localização...";
    navigator.geolocation.getCurrentPosition(
      (pos) => marcar(pos.coords.latitude, pos.coords.longitude, true),
      () => {
        leitura.textContent = "Nenhum ponto marcado.";
        mostrarErroLocal("Não conseguimos pegar sua localização. Toque no mapa para marcar o ponto.");
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  });

  function mostrarErroLocal(msg) {
    erroLocal.textContent = msg;
    erroLocal.hidden = false;
  }

  const foto = document.getElementById("foto");
  const nomeFoto = document.getElementById("nome-foto");
  foto.addEventListener("change", () => {
    const erro = document.getElementById("erro-foto");
    erro.hidden = true;
    const arq = foto.files[0];
    nomeFoto.textContent = arq ? arq.name : "Nenhuma foto escolhida";
    if (!arq) return;
    if (!/^image\/(jpeg|png)$/.test(arq.type)) {
      erro.textContent = "Escolha uma foto em JPG ou PNG.";
      erro.hidden = false;
      foto.value = "";
      nomeFoto.textContent = "Nenhuma foto escolhida";
    } else if (arq.size > 5 * 1024 * 1024) {
      erro.textContent = "A foto passa de 5 MB. Escolha uma imagem menor.";
      erro.hidden = false;
      foto.value = "";
      nomeFoto.textContent = "Nenhuma foto escolhida";
    }
  });

  document.getElementById("form").addEventListener("submit", async (ev) => {
    ev.preventDefault();
    const erroTipo = document.getElementById("erro-tipo");
    const erroGeral = document.getElementById("erro-geral");
    erroTipo.hidden = erroGeral.hidden = true;

    const tipo = document.querySelector('input[name="tipo"]:checked');
    const faltas = [];
    if (!ponto) {
      mostrarErroLocal("Marque no mapa onde o animal está.");
      faltas.push("marcar no mapa onde o animal está");
    }
    if (!tipo) {
      erroTipo.textContent = "Escolha o que você viu.";
      erroTipo.hidden = false;
      faltas.push("escolher o que você viu");
    }
    if (faltas.length) {
      erroGeral.textContent = `Ainda falta ${faltas.join(" e ")}. O aviso em vermelho mostra onde.`;
      erroGeral.hidden = false;
      const primeiroErro = !ponto ? erroLocal : erroTipo;
      primeiroErro.scrollIntoView({ block: "center" });
      return;
    }

    const dados = new FormData();
    dados.append("tipo", tipo.value);
    dados.append("qtdAnimais", document.getElementById("qtd").value || "1");
    dados.append("lat", ponto.lat);
    dados.append("lng", ponto.lng);
    dados.append("descricao", document.getElementById("descricao").value);
    dados.append("contato", document.getElementById("contato").value);
    if (foto.files[0]) dados.append("foto", foto.files[0]);

    const botao = document.getElementById("enviar");
    botao.disabled = true;
    botao.textContent = "Enviando...";
    try {
      const r = await api("/api/reportes", { method: "POST", body: dados });
      mostrarRecebido(r);
    } catch (e) {
      erroGeral.textContent = e.erro;
      erroGeral.hidden = false;
      botao.disabled = false;
      botao.textContent = "Enviar reporte";
    }
  });

  function guardarCodigo(codigo) {
    try {
      const lista = JSON.parse(localStorage.getItem("cpet.codigos") || "[]").filter((c) => c.codigo !== codigo);
      lista.unshift({ codigo, em: new Date().toISOString() });
      localStorage.setItem("cpet.codigos", JSON.stringify(lista.slice(0, 10)));
    } catch (e) {
      /* navegação privada: o código continua na tela */
    }
  }

  function mostrarRecebido(r) {
    guardarCodigo(r.codigo);
    document.getElementById("tela-form").hidden = true;
    document.getElementById("tela-recebido").hidden = false;
    document.getElementById("codigo").textContent = r.codigo;
    document.getElementById("ver-andamento").href = `acompanhar.html?codigo=${encodeURIComponent(r.codigo)}`;
    document.getElementById("rec-titulo").textContent = r.duplicata ? "Já havia um reporte neste ponto." : "Reporte recebido.";
    document.getElementById("rec-texto").textContent = r.duplicata
      ? "Somamos o seu relato ao caso que já estava aberto. Mais de um aviso no mesmo lugar aumenta a prioridade. Seu código de acompanhamento é:"
      : "A central vai analisar o caso e encaminhar para quem pode atender. Seu código de acompanhamento é:";
    window.scrollTo(0, 0);
  }

  document.getElementById("copiar").addEventListener("click", async (ev) => {
    const codigo = document.getElementById("codigo").textContent;
    try {
      await navigator.clipboard.writeText(codigo);
      ev.target.textContent = "Código copiado";
    } catch (e) {
      ev.target.textContent = "Copie manualmente";
    }
  });
})();
