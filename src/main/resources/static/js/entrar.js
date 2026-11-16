(function () {
  const { api, iniciar, esc } = window.CPET;
  iniciar("entrar.html");

  const lista = document.getElementById("usuarios");
  const erro = document.getElementById("erro");

  async function entrar(id) {
    try {
      const usuario = await api("/api/sessao", { json: { usuarioId: id } });
      location.href = usuario.perfil === "OPERADOR" ? "painel.html" : "recurso.html";
    } catch (e) {
      erro.textContent = e.erro;
      erro.hidden = false;
    }
  }

  document.getElementById("reset").addEventListener("click", async (ev) => {
    const msg = document.getElementById("reset-msg");
    ev.target.disabled = true;
    try {
      await api("/api/demo/reset", { method: "POST" });
      msg.textContent = "Dados simulados recarregados.";
    } catch (e) {
      msg.textContent = e.erro;
    }
    ev.target.disabled = false;
  });

  api("/api/usuarios")
    .then((usuarios) => {
      lista.innerHTML = usuarios
        .map(
          (u) => `<button type="button" class="usuario-cartao" data-id="${u.id}">
            <b>${esc(u.nome)}</b>
            <span class="onde">${esc(u.recurso)}</span>
            <span class="chip perfil ${u.perfil === "OPERADOR" ? "TRIAGEM" : "ENCAMINHADO"}">${u.perfil === "OPERADOR" ? "Operador" : "Agente"}</span>
          </button>`
        )
        .join("");
      lista.querySelectorAll("button").forEach((b) => b.addEventListener("click", () => entrar(Number(b.dataset.id))));
      const direto = new URLSearchParams(location.search).get("usuario");
      if (direto) entrar(Number(direto));
    })
    .catch((e) => {
      erro.textContent = e.erro;
      erro.hidden = false;
    });
})();
