package br.com.cpet.web;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.cpet.dominio.Recurso;
import br.com.cpet.dominio.Repositorios.RecursoRepository;
import br.com.cpet.dominio.Repositorios.UsuarioRepository;
import br.com.cpet.dominio.TipoCaso;
import br.com.cpet.servico.BancoService;
import br.com.cpet.servico.CasoService;
import br.com.cpet.servico.DadosAbertosService;
import br.com.cpet.servico.DadosAbertosService.BairroAgregado;
import br.com.cpet.servico.DadosAbertosService.CelulaAgregada;
import br.com.cpet.servico.DadosAbertosService.Resumo;
import br.com.cpet.servico.Erros;
import br.com.cpet.servico.Visoes.Acompanhamento;
import br.com.cpet.servico.Visoes.NovoReporte;
import br.com.cpet.servico.Visoes.ResultadoReporte;

/** Endpoints sem login. Só devolvem agregados por célula ou o andamento de um código. */
@RestController
@RequestMapping("/api")
public class PublicoController {

    private final CasoService casos;
    private final DadosAbertosService dados;
    private final BancoService banco;
    private final UsuarioRepository usuarios;
    private final RecursoRepository recursos;
    private final boolean resetHabilitado;

    public PublicoController(CasoService casos, DadosAbertosService dados, BancoService banco,
                             UsuarioRepository usuarios, RecursoRepository recursos,
                             @Value("${cpet.demo.reset-habilitado}") boolean resetHabilitado) {
        this.casos = casos;
        this.dados = dados;
        this.banco = banco;
        this.usuarios = usuarios;
        this.recursos = recursos;
        this.resetHabilitado = resetHabilitado;
    }

    @PostMapping(value = "/reportes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResultadoReporte reportar(@RequestParam TipoCaso tipo,
                                     @RequestParam(defaultValue = "1") int qtdAnimais,
                                     @RequestParam double lat,
                                     @RequestParam double lng,
                                     @RequestParam(required = false) String descricao,
                                     @RequestParam(required = false) String contato,
                                     @RequestParam(required = false) MultipartFile foto) {
        return casos.reportar(new NovoReporte(tipo, qtdAnimais, lat, lng, descricao, contato), foto);
    }

    @GetMapping("/acompanhar/{codigo}")
    public Acompanhamento acompanhar(@PathVariable String codigo) {
        return casos.acompanhar(codigo);
    }

    @GetMapping("/dados/celulas")
    public List<CelulaAgregada> celulas() {
        return dados.celulas();
    }

    @GetMapping("/dados/bairros")
    public List<BairroAgregado> bairros() {
        return dados.bairros();
    }

    @GetMapping("/dados/resumo")
    public Resumo resumo() {
        return dados.resumo();
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of("simulados", dados.resumo().simulados() > 0);
    }

    @GetMapping("/usuarios")
    public List<Map<String, Object>> usuarios() {
        Map<Long, String> nomes = recursos.findAll().stream()
                .collect(Collectors.toMap(Recurso::getId, Recurso::getNome));
        return usuarios.findAll().stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "nome", u.getNome(),
                        "perfil", u.getPerfil().name(),
                        "recurso", u.getRecursoId() == null ? "Central de triagem" : nomes.get(u.getRecursoId())))
                .toList();
    }

    @PostMapping("/demo/reset")
    public Map<String, String> reset() {
        if (!resetHabilitado) {
            throw new Erros.AcessoNegado("O reset da demonstração está desligado.");
        }
        banco.recriarSeed();
        return Map.of("status", "ok");
    }
}
