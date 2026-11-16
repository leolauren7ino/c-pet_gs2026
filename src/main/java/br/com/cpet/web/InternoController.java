package br.com.cpet.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.cpet.dominio.Perfil;
import br.com.cpet.dominio.Repositorios.RecursoRepository;
import br.com.cpet.dominio.StatusCaso;
import br.com.cpet.dominio.TipoCaso;
import br.com.cpet.dominio.Usuario;
import br.com.cpet.servico.AlertaService;
import br.com.cpet.servico.AlertaService.Alertas;
import br.com.cpet.servico.CasoService;
import br.com.cpet.servico.FotoService;
import br.com.cpet.servico.Visoes.CasoDetalhe;
import br.com.cpet.servico.Visoes.CasoResumo;
import jakarta.servlet.http.HttpSession;

/** Endpoints que exigem usuário entrando pela tela "Entrar como". */
@RestController
@RequestMapping("/api")
public class InternoController {

    public record EntradaSessao(Long usuarioId) {
    }

    public record Acao(Long recursoId, String observacao) {
    }

    private final SessaoService sessao;
    private final CasoService casos;
    private final AlertaService alertas;
    private final FotoService fotos;
    private final RecursoRepository recursos;

    public InternoController(SessaoService sessao, CasoService casos, AlertaService alertas, FotoService fotos,
                             RecursoRepository recursos) {
        this.sessao = sessao;
        this.casos = casos;
        this.alertas = alertas;
        this.fotos = fotos;
        this.recursos = recursos;
    }

    @PostMapping("/sessao")
    public Map<String, Object> entrar(@RequestBody EntradaSessao entrada, HttpSession http) {
        return visao(sessao.entrar(http, entrada.usuarioId()));
    }

    @GetMapping("/sessao")
    public Map<String, Object> atual(HttpSession http) {
        return visao(sessao.exigir(http));
    }

    @DeleteMapping("/sessao")
    public void sair(HttpSession http) {
        sessao.sair(http);
    }

    @GetMapping("/casos")
    public List<CasoResumo> listar(@RequestParam(required = false) StatusCaso status,
                                   @RequestParam(required = false) TipoCaso tipo,
                                   @RequestParam(required = false) Long bairroId, HttpSession http) {
        sessao.exigir(http, Perfil.OPERADOR);
        return casos.listar(status, tipo, bairroId);
    }

    @GetMapping("/casos/{id}")
    public CasoDetalhe detalhe(@PathVariable Long id, HttpSession http) {
        sessao.exigir(http, Perfil.OPERADOR);
        return casos.detalhe(id, true);
    }

    @PostMapping("/casos/{id}/encaminhar")
    public CasoResumo encaminhar(@PathVariable Long id, @RequestBody Acao acao, HttpSession http) {
        Usuario operador = sessao.exigir(http, Perfil.OPERADOR);
        return casos.encaminhar(id, acao.recursoId(), operador);
    }

    @PostMapping("/casos/{id}/arquivar")
    public CasoResumo arquivar(@PathVariable Long id, @RequestBody Acao acao, HttpSession http) {
        Usuario operador = sessao.exigir(http, Perfil.OPERADOR);
        return casos.arquivar(id, acao.observacao(), operador);
    }

    @GetMapping("/recurso")
    public Map<String, Object> meuRecurso(HttpSession http) {
        Usuario agente = sessao.exigir(http, Perfil.AGENTE);
        return recursos.findById(agente.getRecursoId())
                .map(r -> Map.<String, Object>of("id", r.getId(), "nome", r.getNome(), "tipo", r.getTipo().name(),
                        "vagas", r.getVagasDisponiveis()))
                .orElseThrow();
    }

    @GetMapping("/recurso/casos")
    public List<CasoDetalhe> casosDoRecurso(HttpSession http) {
        Usuario agente = sessao.exigir(http, Perfil.AGENTE);
        return casos.casosDoRecurso(agente.getRecursoId());
    }

    @PostMapping("/recurso/casos/{id}/aceitar")
    public CasoResumo aceitar(@PathVariable Long id, HttpSession http) {
        return casos.aceitar(id, sessao.exigir(http, Perfil.AGENTE));
    }

    @PostMapping("/recurso/casos/{id}/recusar")
    public CasoResumo recusar(@PathVariable Long id, @RequestBody Acao acao, HttpSession http) {
        return casos.recusar(id, acao.observacao(), sessao.exigir(http, Perfil.AGENTE));
    }

    @PostMapping("/recurso/casos/{id}/atender")
    public CasoResumo atender(@PathVariable Long id, @RequestBody Acao acao, HttpSession http) {
        return casos.atender(id, acao.observacao(), sessao.exigir(http, Perfil.AGENTE));
    }

    @GetMapping("/alertas")
    public Alertas alertas(HttpSession http) {
        sessao.exigir(http);
        return alertas.gerar();
    }

    @GetMapping("/fotos/{nome}")
    public ResponseEntity<Resource> foto(@PathVariable String nome, HttpSession http) throws IOException {
        sessao.exigir(http);
        Path caminho = fotos.localizar(nome);
        MediaType tipo = nome.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(tipo).cacheControl(CacheControl.noStore())
                .contentLength(Files.size(caminho)).body(new FileSystemResource(caminho));
    }

    private static Map<String, Object> visao(Usuario u) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("nome", u.getNome());
        m.put("perfil", u.getPerfil().name());
        m.put("recursoId", u.getRecursoId());
        return m;
    }
}
