/* Utilitários compartilhados por todas as telas. */
(function () {
  const TIPOS = {
    FERIDO: "Animal ferido",
    DOENTE: "Animal doente",
    FILHOTE: "Filhote desamparado",
    COLONIA: "Colônia de animais",
    MAUS_TRATOS: "Maus-tratos",
    RISCO_SANITARIO: "Risco sanitário",
  };
  const STATUS = {
    REPORTADO: "Reportado",
    TRIAGEM: "Triagem",
    ENCAMINHADO: "Encaminhado",
    ATENDIDO: "Atendido",
    ADOTADO: "Adotado",
    ARQUIVADO: "Arquivado",
  };
  const RECURSOS = { ONG: "ONG", CLINICA: "Clínica", UNIDADE_MOVEL: "Unidade móvel", ORGAO_PUBLICO: "Órgão público" };

  async function api(url, opcoes = {}) {
    const init = { credentials: "same-origin", ...opcoes };
    if (init.json !== undefined) {
      init.method = init.method || "POST";
      init.headers = { "Content-Type": "application/json" };
      init.body = JSON.stringify(init.json);
    }
    const leitura = !init.method || init.method.toUpperCase() === "GET";
    let resp;
    for (let tentativa = 0; ; tentativa++) {
      try {
        resp = await fetch(url, init);
        break;
      } catch (e) {
        if (leitura && tentativa === 0) {
          await new Promise((r) => setTimeout(r, 400));
          continue;
        }
        throw { status: 0, erro: "Sem conexão com o servidor. Tente de novo em instantes." };
      }
    }
    const texto = await resp.text();
    const corpo = texto ? JSON.parse(texto) : null;
    if (!resp.ok) throw { status: resp.status, erro: (corpo && corpo.erro) || "Algo deu errado. Tente de novo." };
    return corpo;
  }

  function esc(valor) {
    return String(valor ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
  }

  const dataHora = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" });

  function quando(iso) {
    return dataHora.format(new Date(iso)).replace(",", "");
  }

  const dataCompleta = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" });

  function quandoCompleto(iso) {
    return dataCompleta.format(new Date(iso)).replace(",", "");
  }

  function haTempo(iso) {
    const min = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 60000));
    if (min < 1) return "agora";
    if (min < 60) return `há ${min} min`;
    const h = Math.round(min / 60);
    if (h < 48) return `há ${h} h`;
    return `há ${Math.round(h / 24)} dias`;
  }

  function classeScore(score) {
    return score >= 70 ? "alto" : score >= 45 ? "medio" : "baixo";
  }

  function chipStatus(status) {
    return `<span class="chip ${status}">${STATUS[status]}</span>`;
  }

  function mapaBase(id, centro, zoom) {
    const mapa = L.map(id, { zoomControl: true, attributionControl: true }).setView(centro, zoom);
    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: "&copy; colaboradores do OpenStreetMap",
    }).addTo(mapa);
    return mapa;
  }

  function cor(nome) {
    return getComputedStyle(document.documentElement).getPropertyValue(nome).trim();
  }

  function cabecalho(ativo, usuario) {
    const links = usuario
      ? usuario.perfil === "OPERADOR"
        ? [["painel.html", "Painel"], ["dados.html", "Dados abertos"], ["sobre.html", "Sobre"]]
        : [["recurso.html", "Meus casos"], ["dados.html", "Dados abertos"], ["sobre.html", "Sobre"]]
      : [["index.html", "Reportar"], ["acompanhar.html", "Acompanhar"], ["dados.html", "Dados abertos"], ["sobre.html", "Sobre"], ["entrar.html", "Entrar"]];
    const nav = links.map(([href, rotulo]) => `<a href="${href}"${href === ativo ? ' aria-current="page"' : ""}>${rotulo}</a>`).join("");
    const quem = usuario
      ? `<span class="usuario"><span>${esc(usuario.nome)}</span><button type="button" id="sair">Sair</button></span>`
      : "";
    return `<header class="topo"><a class="marca" href="${usuario ? (usuario.perfil === "OPERADOR" ? "painel.html" : "recurso.html") : "index.html"}"><img class="emblema" src="img/emblema.png" alt="" width="52" height="52"><span class="wordmark" role="img" aria-label="C-PET">C-<b>PET</b></span><small>Reporte, prioridade e encaminhamento de animais de rua</small></a><nav aria-label="Principal">${nav}${quem}</nav></header>`;
  }

  async function iniciar(ativo, { usuario = null, exigir = null } = {}) {
    let atual = usuario;
    if (exigir) {
      try {
        atual = await api("/api/sessao");
      } catch (e) {
        if (e.status === 401 || e.status === 403) {
          location.replace("entrar.html");
        } else {
          document.getElementById("cabecalho").innerHTML = cabecalho(ativo, null);
          document.querySelector("main").innerHTML = `<div class="pagina"><div class="alerta-erro mb-3">${esc(e.erro)}</div><button type="button" class="btn-c sec" onclick="location.reload()">Tentar de novo</button></div>`;
        }
        return null;
      }
      if (atual.perfil !== exigir) {
        location.replace("entrar.html");
        return null;
      }
    }
    const raiz = document.getElementById("cabecalho");
    raiz.innerHTML = '<div id="faixa-demo"></div>' + cabecalho(ativo, atual);
    api("/api/info").then((info) => {
      if (info.simulados) {
        document.getElementById("faixa-demo").innerHTML = '<div class="faixa-demo">Dados simulados para demonstração</div>';
      }
    });
    if (!document.body.classList.contains("tela-cheia") && !document.querySelector(".rodape")) {
      document.body.insertAdjacentHTML("beforeend", '<footer class="rodape"><span>Dados simulados para demonstração</span><a href="rastreabilidade.html">Rastreabilidade: casos de uso, telas e pilares</a></footer>');
    }
    const sair = document.getElementById("sair");
    if (sair) {
      sair.addEventListener("click", async () => {
        await api("/api/sessao", { method: "DELETE" });
        location.href = "entrar.html";
      });
    }
    return atual;
  }

  window.CPET = { TIPOS, STATUS, RECURSOS, api, esc, quando, quandoCompleto, haTempo, classeScore, chipStatus, mapaBase, cor, iniciar };
})();
