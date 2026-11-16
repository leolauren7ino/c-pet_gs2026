package br.com.cpet.servico;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.com.cpet.dominio.Bairro;
import br.com.cpet.dominio.Caso;
import br.com.cpet.dominio.HistoricoStatus;
import br.com.cpet.dominio.Recurso;
import br.com.cpet.dominio.Reporte;
import br.com.cpet.dominio.Repositorios.BairroRepository;
import br.com.cpet.dominio.Repositorios.CasoRepository;
import br.com.cpet.dominio.Repositorios.HistoricoRepository;
import br.com.cpet.dominio.Repositorios.RecursoRepository;
import br.com.cpet.dominio.Repositorios.ReporteRepository;
import br.com.cpet.dominio.StatusCaso;
import br.com.cpet.dominio.TipoCaso;
import br.com.cpet.dominio.Usuario;
import br.com.cpet.regras.CalculadoraScore;
import br.com.cpet.regras.CalculadoraScore.ScoreDetalhe;
import br.com.cpet.regras.DetectorDuplicata;
import br.com.cpet.regras.Geo;
import br.com.cpet.regras.SugestorRecursos;
import br.com.cpet.servico.Visoes.Acompanhamento;
import br.com.cpet.servico.Visoes.CasoDetalhe;
import br.com.cpet.servico.Visoes.CasoResumo;
import br.com.cpet.servico.Visoes.EtapaPublica;
import br.com.cpet.servico.Visoes.HistoricoVisao;
import br.com.cpet.servico.Visoes.NovoReporte;
import br.com.cpet.servico.Visoes.ReporteVisao;
import br.com.cpet.servico.Visoes.ResultadoReporte;

@Service
@Transactional
public class CasoService {

    private static final String ALFABETO_CODIGO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final CasoRepository casos;
    private final ReporteRepository reportes;
    private final HistoricoRepository historicos;
    private final RecursoRepository recursos;
    private final BairroRepository bairros;
    private final CalculadoraScore calculadora;
    private final DetectorDuplicata detector;
    private final SugestorRecursos sugestor;
    private final FotoService fotos;
    private final Clock relogio;

    public CasoService(CasoRepository casos, ReporteRepository reportes, HistoricoRepository historicos,
                       RecursoRepository recursos, BairroRepository bairros, CalculadoraScore calculadora,
                       DetectorDuplicata detector, SugestorRecursos sugestor, FotoService fotos, Clock relogio) {
        this.casos = casos;
        this.reportes = reportes;
        this.historicos = historicos;
        this.recursos = recursos;
        this.bairros = bairros;
        this.calculadora = calculadora;
        this.detector = detector;
        this.sugestor = sugestor;
        this.fotos = fotos;
        this.relogio = relogio;
    }

    public ResultadoReporte reportar(NovoReporte novo, MultipartFile foto) {
        validar(novo);
        LocalDateTime agora = LocalDateTime.now(relogio);
        String fotoPath = foto != null && !foto.isEmpty() ? fotos.salvar(foto) : null;

        Optional<Caso> existente = detector.encontrar(novo.tipo(), novo.lat(), novo.lng(),
                casos.findByStatusIn(StatusCaso.ABERTOS), agora);

        Caso caso;
        boolean duplicata = existente.isPresent();
        if (duplicata) {
            caso = existente.get();
            caso.setQtdAnimais(Math.max(caso.getQtdAnimais(), novo.qtdAnimais()));
            caso.setAtualizadoEm(agora);
        } else {
            caso = new Caso();
            caso.setTipo(novo.tipo());
            caso.setQtdAnimais(novo.qtdAnimais());
            caso.setLat(novo.lat());
            caso.setLng(novo.lng());
            caso.setCelula(Geo.celula(novo.lat(), novo.lng()));
            caso.setBairroId(bairroMaisProximo(novo.lat(), novo.lng()).getId());
            caso.setStatus(StatusCaso.REPORTADO);
            caso.setCodigoAcompanhamento(gerarCodigo());
            caso.setCriadoEm(agora);
            caso.setAtualizadoEm(agora);
            caso.setSimulado(0);
            caso = casos.save(caso);
            registrarHistorico(caso, null, StatusCaso.REPORTADO, "Cidadão", "Reporte recebido", agora);
        }

        Reporte reporte = new Reporte();
        reporte.setCasoId(caso.getId());
        reporte.setDescricao(vazioParaNulo(novo.descricao()));
        reporte.setFotoPath(fotoPath);
        reporte.setContatoOpcional(vazioParaNulo(novo.contato()));
        reporte.setCriadoEm(agora);
        reportes.save(reporte);

        atualizarScore(caso, agora, contextoAtual());
        return new ResultadoReporte(caso.getCodigoAcompanhamento(), duplicata);
    }

    @Transactional(readOnly = true)
    public Acompanhamento acompanhar(String codigo) {
        Caso caso = casos.findByCodigoAcompanhamento(codigo.trim().toUpperCase())
                .orElseThrow(() -> new Erros.NaoEncontrado("Não encontramos esse código. Confira e tente de novo."));
        Bairro bairro = bairros.findById(caso.getBairroId()).orElseThrow();
        List<EtapaPublica> etapas = historicos.findByCasoIdOrderByCriadoEmAscIdAsc(caso.getId()).stream()
                .filter(h -> h.getStatusAnterior() != h.getStatusNovo())
                .map(h -> new EtapaPublica(h.getStatusNovo(), tituloPublico(h.getStatusNovo()), h.getCriadoEm()))
                .toList();
        return new Acompanhamento(caso.getCodigoAcompanhamento(), caso.getTipo(), bairro.getNome(),
                caso.getStatus(), caso.getCriadoEm(), caso.getAtualizadoEm(), etapas);
    }

    public List<CasoResumo> listar(StatusCaso status, TipoCaso tipo, Long bairroId) {
        LocalDateTime agora = LocalDateTime.now(relogio);
        Contexto ctx = contextoAtual();
        List<Caso> todos = casos.findAll();
        todos.stream().filter(c -> c.getStatus().aberto()).forEach(c -> atualizarScore(c, agora, ctx));
        Map<Long, Long> reportesPorCaso = contarReportes();
        return todos.stream()
                .filter(c -> status == null || c.getStatus() == status)
                .filter(c -> tipo == null || c.getTipo() == tipo)
                .filter(c -> bairroId == null || c.getBairroId().equals(bairroId))
                .map(c -> resumo(c, ctx, reportesPorCaso.getOrDefault(c.getId(), 0L)))
                .sorted(Comparator.comparingInt(CasoResumo::score).reversed()
                        .thenComparing(CasoResumo::criadoEm))
                .toList();
    }

    public CasoDetalhe detalhe(Long id, boolean comSugestoes) {
        Caso caso = buscar(id);
        LocalDateTime agora = LocalDateTime.now(relogio);
        Contexto ctx = contextoAtual();
        ScoreDetalhe score = atualizarScore(caso, agora, ctx);
        List<HistoricoStatus> hist = historicos.findByCasoIdOrderByCriadoEmAscIdAsc(id);
        List<ReporteVisao> listaReportes = reportes.findByCasoIdOrderByCriadoEmAscIdAsc(id).stream()
                .map(r -> new ReporteVisao(r.getId(), r.getDescricao(),
                        urlDaFoto(r.getFotoPath()),
                        r.getContatoOpcional(), r.getCriadoEm()))
                .toList();
        boolean elegivelParaSugestao = comSugestoes
                && (caso.getStatus() == StatusCaso.REPORTADO || caso.getStatus() == StatusCaso.TRIAGEM);
        return new CasoDetalhe(
                resumo(caso, ctx, listaReportes.size()),
                listaReportes,
                score,
                elegivelParaSugestao ? sugestor.sugerir(caso, recursos.findAll()) : List.of(),
                hist.stream().map(h -> new HistoricoVisao(h.getStatusAnterior(), h.getStatusNovo(), h.getAutor(),
                        h.getObservacao(), h.getCriadoEm())).toList(),
                foiAceito(hist));
    }

    public List<CasoDetalhe> casosDoRecurso(Long recursoId) {
        return casos.findByRecursoIdAndStatusIn(recursoId, List.of(StatusCaso.ENCAMINHADO)).stream()
                .sorted(Comparator.comparingInt(Caso::getScore).reversed())
                .map(c -> detalhe(c.getId(), false))
                .toList();
    }

    public CasoResumo encaminhar(Long id, Long recursoId, Usuario operador) {
        Caso caso = buscar(id);
        LocalDateTime agora = LocalDateTime.now(relogio);
        if (caso.getStatus() == StatusCaso.REPORTADO) {
            mudarStatus(caso, StatusCaso.TRIAGEM, operador.getNome(), "Triagem iniciada", agora);
        }
        if (!caso.getStatus().podeIrPara(StatusCaso.ENCAMINHADO)) {
            throw new Erros.TransicaoInvalida("Não é possível encaminhar um caso " + caso.getStatus() + ".");
        }
        boolean elegivel = sugestor.elegiveis(caso, recursos.findAll()).stream()
                .anyMatch(s -> s.recursoId().equals(recursoId));
        if (!elegivel) {
            throw new Erros.RegraDeNegocio("Esse recurso não pode receber o caso (tipo, vagas ou situação).");
        }
        Recurso recurso = recursos.findById(recursoId).orElseThrow();
        recurso.setVagasDisponiveis(recurso.getVagasDisponiveis() - 1);
        caso.setRecursoId(recurso.getId());
        mudarStatus(caso, StatusCaso.ENCAMINHADO, operador.getNome(), "Encaminhado para " + recurso.getNome(), agora);
        return resumoSimples(caso);
    }

    public CasoResumo arquivar(Long id, String observacao, Usuario operador) {
        Caso caso = buscar(id);
        if (caso.getStatus() == StatusCaso.ENCAMINHADO) {
            devolverVaga(caso);
            caso.setRecursoId(null);
        }
        mudarStatus(caso, StatusCaso.ARQUIVADO, operador.getNome(), observacao, LocalDateTime.now(relogio));
        return resumoSimples(caso);
    }

    public CasoResumo aceitar(Long id, Usuario agente) {
        Caso caso = buscarDoRecurso(id, agente);
        List<HistoricoStatus> hist = historicos.findByCasoIdOrderByCriadoEmAscIdAsc(id);
        if (caso.getStatus() != StatusCaso.ENCAMINHADO || foiAceito(hist)) {
            throw new Erros.TransicaoInvalida("Este caso não está aguardando aceite.");
        }
        LocalDateTime agora = LocalDateTime.now(relogio);
        registrarHistorico(caso, StatusCaso.ENCAMINHADO, StatusCaso.ENCAMINHADO, agente.getNome(),
                "Aceito pelo recurso", agora);
        caso.setAtualizadoEm(agora);
        return resumoSimples(caso);
    }

    public CasoResumo recusar(Long id, String observacao, Usuario agente) {
        exigirObservacao(observacao, "Informe o motivo da recusa.");
        Caso caso = buscarDoRecurso(id, agente);
        LocalDateTime agora = LocalDateTime.now(relogio);
        devolverVaga(caso);
        caso.setRecursoId(null);
        mudarStatus(caso, StatusCaso.TRIAGEM, agente.getNome(), "Recusado: " + observacao.trim(), agora);
        return resumoSimples(caso);
    }

    public CasoResumo atender(Long id, String observacao, Usuario agente) {
        exigirObservacao(observacao, "Descreva o atendimento realizado.");
        Caso caso = buscarDoRecurso(id, agente);
        devolverVaga(caso);
        mudarStatus(caso, StatusCaso.ATENDIDO, agente.getNome(), observacao.trim(), LocalDateTime.now(relogio));
        return resumoSimples(caso);
    }

    public void recalcularTodos() {
        LocalDateTime agora = LocalDateTime.now(relogio);
        Contexto ctx = contextoAtual();
        casos.findAll().forEach(c -> atualizarScore(c, agora, ctx));
    }

    private record Contexto(Map<String, Long> abertosPorCelula, Map<Long, Bairro> bairros, Map<Long, String> recursos) {
    }

    private Contexto contextoAtual() {
        Map<String, Long> porCelula = casos.findByStatusIn(StatusCaso.ABERTOS).stream()
                .collect(Collectors.groupingBy(Caso::getCelula, Collectors.counting()));
        Map<Long, Bairro> mapaBairros = bairros.findAll().stream()
                .collect(Collectors.toMap(Bairro::getId, Function.identity()));
        Map<Long, String> nomesRecursos = recursos.findAll().stream()
                .collect(Collectors.toMap(Recurso::getId, Recurso::getNome));
        return new Contexto(porCelula, mapaBairros, nomesRecursos);
    }

    private ScoreDetalhe atualizarScore(Caso caso, LocalDateTime agora, Contexto ctx) {
        LocalDateTime fim = caso.getStatus().aberto() ? agora : caso.getAtualizadoEm();
        long dias = Duration.between(caso.getCriadoEm(), fim).toDays();
        ScoreDetalhe detalhe = calculadora.calcular(caso.getTipo(),
                ctx.abertosPorCelula().getOrDefault(caso.getCelula(), 0L).intValue(),
                ctx.bairros().get(caso.getBairroId()).getIndiceVulnerabilidade(), dias);
        caso.setScore(detalhe.total());
        return detalhe;
    }

    private void mudarStatus(Caso caso, StatusCaso novo, String autor, String observacao, LocalDateTime agora) {
        StatusCaso anterior = caso.getStatus();
        if (!anterior.podeIrPara(novo)) {
            throw new Erros.TransicaoInvalida("Transição inválida: " + anterior + " para " + novo + ".");
        }
        registrarHistorico(caso, anterior, novo, autor, observacao, agora);
        caso.setStatus(novo);
        caso.setAtualizadoEm(agora);
        atualizarScore(caso, agora, contextoAtual());
    }

    private void registrarHistorico(Caso caso, StatusCaso anterior, StatusCaso novo, String autor,
                                    String observacao, LocalDateTime agora) {
        HistoricoStatus h = new HistoricoStatus();
        h.setCasoId(caso.getId());
        h.setStatusAnterior(anterior);
        h.setStatusNovo(novo);
        h.setAutor(autor);
        h.setObservacao(observacao == null ? null : abreviar(observacao, 500));
        h.setCriadoEm(agora);
        historicos.save(h);
    }

    private void devolverVaga(Caso caso) {
        if (caso.getRecursoId() != null) {
            recursos.findById(caso.getRecursoId())
                    .ifPresent(r -> r.setVagasDisponiveis(r.getVagasDisponiveis() + 1));
        }
    }

    private boolean foiAceito(List<HistoricoStatus> historico) {
        boolean aceito = false;
        for (HistoricoStatus h : historico) {
            if (h.getStatusNovo() == StatusCaso.ENCAMINHADO) {
                aceito = h.getStatusAnterior() == StatusCaso.ENCAMINHADO;
            }
        }
        return aceito;
    }

    private Caso buscar(Long id) {
        return casos.findById(id).orElseThrow(() -> new Erros.NaoEncontrado("Caso não encontrado."));
    }

    private Caso buscarDoRecurso(Long id, Usuario agente) {
        Caso caso = buscar(id);
        if (agente.getRecursoId() == null || !agente.getRecursoId().equals(caso.getRecursoId())) {
            throw new Erros.AcessoNegado("Este caso não está encaminhado ao seu recurso.");
        }
        return caso;
    }

    private Bairro bairroMaisProximo(double lat, double lng) {
        return bairros.findAll().stream()
                .min(Comparator.comparingDouble(b -> Geo.distanciaMetros(lat, lng, b.getLat(), b.getLng())))
                .orElseThrow(() -> new Erros.RegraDeNegocio("Nenhum bairro cadastrado."));
    }

    private CasoResumo resumoSimples(Caso caso) {
        return resumo(caso, contextoAtual(), reportes.countByCasoId(caso.getId()));
    }

    private CasoResumo resumo(Caso c, Contexto ctx, long qtdReportes) {
        return new CasoResumo(c.getId(), c.getCodigoAcompanhamento(), c.getTipo(), c.getQtdAnimais(), c.getLat(),
                c.getLng(), c.getCelula(), c.getBairroId(), ctx.bairros().get(c.getBairroId()).getNome(),
                c.getStatus(), c.getScore(), c.getRecursoId(),
                c.getRecursoId() == null ? null : ctx.recursos().get(c.getRecursoId()),
                c.getCriadoEm(), c.getAtualizadoEm(), c.getSimulado() == 1, qtdReportes);
    }

    /** Fotos do seed são ilustrações públicas em static/seed-fotos; as enviadas por cidadãos só saem autenticadas. */
    private static String urlDaFoto(String fotoPath) {
        if (fotoPath == null) {
            return null;
        }
        return fotoPath.startsWith("seed-fotos/") ? "/" + fotoPath : "/api/fotos/" + fotoPath;
    }

    private Map<Long, Long> contarReportes() {
        return reportes.findAll().stream()
                .collect(Collectors.groupingBy(Reporte::getCasoId, Collectors.counting()));
    }

    private String gerarCodigo() {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder("CP-");
            for (int i = 0; i < 6; i++) {
                sb.append(ALFABETO_CODIGO.charAt(ALEATORIO.nextInt(ALFABETO_CODIGO.length())));
            }
            codigo = sb.toString();
        } while (casos.existsByCodigoAcompanhamento(codigo));
        return codigo;
    }

    private static void validar(NovoReporte n) {
        if (n.tipo() == null) {
            throw new Erros.RegraDeNegocio("Escolha o tipo da situação.");
        }
        if (n.qtdAnimais() < 1 || n.qtdAnimais() > 999) {
            throw new Erros.RegraDeNegocio("Informe quantos animais há no local (1 ou mais).");
        }
        if (n.lat() < -90 || n.lat() > 90 || n.lng() < -180 || n.lng() > 180) {
            throw new Erros.RegraDeNegocio("Marque no mapa onde os animais estão.");
        }
        if (n.descricao() != null && n.descricao().length() > 1000) {
            throw new Erros.RegraDeNegocio("A descrição passa de 1000 caracteres.");
        }
        if (n.contato() != null && n.contato().length() > 120) {
            throw new Erros.RegraDeNegocio("O contato passa de 120 caracteres.");
        }
    }

    private static void exigirObservacao(String observacao, String mensagem) {
        if (observacao == null || observacao.isBlank()) {
            throw new Erros.RegraDeNegocio(mensagem);
        }
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String abreviar(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String tituloPublico(StatusCaso status) {
        return switch (status) {
            case REPORTADO -> "Reporte recebido";
            case TRIAGEM -> "Em análise pela central";
            case ENCAMINHADO -> "Encaminhado a um parceiro da rede";
            case ATENDIDO -> "Atendido";
            case ARQUIVADO -> "Encerrado sem atendimento";
        };
    }
}
