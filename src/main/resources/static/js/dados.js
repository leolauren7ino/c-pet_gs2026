(function () {
  const { api, iniciar, esc, mapaBase } = window.CPET;
  iniciar("dados.html");

  const RAMPA = ["#F1E4C3", "#E3B66B", "#D2832E", "#B8431D", "#7E2410"];
  const FAIXAS = [
    [0, "nenhum"],
    [1, "1 a 2"],
    [3, "3 a 4"],
    [5, "5 a 6"],
    [7, "7 ou mais"],
  ];

  function cor(abertos) {
    if (abertos >= 7) return RAMPA[4];
    if (abertos >= 5) return RAMPA[3];
    if (abertos >= 3) return RAMPA[2];
    if (abertos >= 1) return RAMPA[1];
    return RAMPA[0];
  }

  Promise.all([api("/api/dados/resumo"), api("/api/dados/celulas"), api("/api/dados/bairros")]).then(
    ([resumo, celulas, bairros]) => {
      document.getElementById("numeros").innerHTML = `
        <div><div class="valor">${resumo.abertos}</div><div class="rotulo">casos em aberto</div></div>
        <div><div class="valor">${resumo.atendidos}</div><div class="rotulo">casos atendidos</div></div>
        <div><div class="valor">${resumo.total}</div><div class="rotulo">casos registrados</div></div>`;

      const mapa = mapaBase("mapa", [-23.58, -46.63], 10);
      const limites = [];
      celulas.forEach((c) => {
        limites.push([c.lat, c.lng]);
        L.circleMarker([c.lat, c.lng], {
          radius: 7 + c.abertos * 2.5,
          color: "#1B2320",
          weight: 1,
          fillColor: cor(c.abertos),
          fillOpacity: c.abertos ? 0.78 : 0.35,
        })
          .bindTooltip(`${esc(c.bairro)}: ${c.abertos} em aberto, ${c.atendidos} atendidos`)
          .addTo(mapa);
      });
      if (limites.length) mapa.fitBounds(limites, { padding: [24, 24] });

      document.getElementById("legenda").innerHTML =
        "<span>Casos em aberto por célula:</span>" +
        FAIXAS.map(([min, rot]) => `<span><span class="q" style="background:${cor(min)}"></span>${rot}</span>`).join("");

      const maxAbertos = Math.max(1, ...bairros.map((b) => b.abertos));
      document.getElementById("bairros").innerHTML = bairros
        .map(
          (b) => `<tr>
            <th scope="row">${esc(b.nome)}</th>
            <td class="num">${b.abertos}</td>
            <td><div class="barra" role="presentation"><i style="width:${(b.abertos / maxAbertos) * 100}%"></i></div></td>
            <td class="num">${b.atendidos}</td>
          </tr>`
        )
        .join("");
    }
  );
})();
