(function () {
  const { api, iniciar, esc, mapaBase, TIPOS } = window.CPET;
  iniciar("dados.html");

  const RAMPA = ["#F1E4C3", "#E3B66B", "#D2832E", "#B8431D", "#7E2410"];
  const FAIXAS = [
    [0, "nenhum"],
    [1, "1 a 2"],
    [3, "3 a 4"],
    [5, "5 a 6"],
    [7, "7 ou mais"],
  ];

  const filtro = document.getElementById("f-tipo");
  Object.entries(TIPOS).forEach(([k, v]) => filtro.insertAdjacentHTML("beforeend", `<option value="${k}">${v}</option>`));

  let mapa;
  let camada;
  let celulas = [];
  let bairros = [];

  function cor(abertos) {
    if (abertos >= 7) return RAMPA[4];
    if (abertos >= 5) return RAMPA[3];
    if (abertos >= 3) return RAMPA[2];
    if (abertos >= 1) return RAMPA[1];
    return RAMPA[0];
  }

  function abertos(item) {
    const tipo = filtro.value;
    return tipo ? (item.abertosPorTipo && item.abertosPorTipo[tipo]) || 0 : item.abertos;
  }

  function resumoTipos(item) {
    const partes = Object.entries(item.abertosPorTipo || {})
      .sort((a, b) => b[1] - a[1])
      .map(([t, n]) => `${n} ${TIPOS[t].toLowerCase()}`);
    return partes.join(", ");
  }

  function desenhar(ajustar) {
    camada.clearLayers();
    const visiveis = celulas.filter((c) => abertos(c) > 0);
    visiveis.forEach((c) => {
      const n = abertos(c);
      const d = 26 + Math.min(n, 9) * 4;
      L.marker([c.lat, c.lng], {
        icon: L.divIcon({
          className: "",
          iconSize: [d, d],
          html: `<span class="bolha" style="width:${d}px;height:${d}px;background:${cor(n)};color:${n >= 5 ? "#fff" : "#1B2320"}">${n}</span>`,
        }),
        keyboard: false,
      })
        .bindPopup(`<b>${esc(c.bairro)}</b><br>${n} em aberto nesta célula<br><small>${esc(resumoTipos(c))}</small>`)
        .addTo(camada);
    });
    if (ajustar && visiveis.length) mapa.fitBounds(visiveis.map((c) => [c.lat, c.lng]), { padding: [40, 40], maxZoom: 13 });

    document.getElementById("ranking").innerHTML = visiveis.length
      ? [...visiveis]
          .sort((a, b) => abertos(b) - abertos(a))
          .slice(0, 5)
          .map(
            (c) => `<li><span class="bolha-mini" style="background:${cor(abertos(c))};color:${abertos(c) >= 5 ? "#fff" : "#1B2320"}">${abertos(c)}</span>
              <span><b>${esc(c.bairro)}</b><small>${esc(resumoTipos(c))}</small></span></li>`
          )
          .join("")
      : '<li class="fraco">Nenhum caso em aberto para este tipo.</li>';

    const max = Math.max(1, ...bairros.map((b) => abertos(b)));
    document.getElementById("bairros").innerHTML = [...bairros]
      .sort((a, b) => abertos(b) - abertos(a))
      .map(
        (b) => `<tr>
          <th scope="row">${esc(b.nome)}</th>
          <td class="num">${abertos(b)}</td>
          <td><div class="barra" role="presentation"><i style="width:${(abertos(b) / max) * 100}%"></i></div></td>
          <td class="num">${b.atendidos}</td>
        </tr>`
      )
      .join("");
  }

  Promise.all([api("/api/dados/resumo"), api("/api/dados/celulas"), api("/api/dados/bairros")]).then(([resumo, c, b]) => {
    celulas = c;
    bairros = b;
    document.getElementById("numeros").innerHTML = `
      <div><div class="valor">${resumo.abertos}</div><div class="rotulo">casos em aberto</div></div>
      <div><div class="valor">${resumo.atendidos}</div><div class="rotulo">casos atendidos</div></div>
      <div><div class="valor">${resumo.total}</div><div class="rotulo">casos registrados</div></div>`;

    mapa = mapaBase("mapa", [-23.58, -46.63], 10);
    camada = L.layerGroup().addTo(mapa);
    document.getElementById("legenda").innerHTML =
      "<span>Casos em aberto por célula (o número está no círculo):</span>" +
      FAIXAS.map(([min, rot]) => `<span><span class="q" style="background:${cor(min)}"></span>${rot}</span>`).join("");

    filtro.addEventListener("change", () => desenhar(true));
    desenhar(true);
  });
})();
