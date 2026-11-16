package br.com.cpet.servico;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.cpet.config.RegrasProperties;
import br.com.cpet.dominio.Bairro;
import br.com.cpet.dominio.Caso;
import br.com.cpet.dominio.Repositorios.BairroRepository;
import br.com.cpet.dominio.Repositorios.CasoRepository;
import br.com.cpet.dominio.StatusCaso;
import br.com.cpet.dominio.TipoCaso;

@Service
@Transactional(readOnly = true)
public class AlertaService {

    public record AlertaCaso(Long id, String codigo, TipoCaso tipo, StatusCaso status, String bairro, int score,
                             LocalDateTime atualizadoEm) {
    }

    public record AlertaCelula(String celula, String bairro, long abertos) {
    }

    public record Alertas(List<AlertaCaso> casosAtualizados, List<AlertaCelula> celulasCriticas, int total) {
    }

    private final CasoRepository casos;
    private final BairroRepository bairros;
    private final RegrasProperties regras;
    private final Clock relogio;

    public AlertaService(CasoRepository casos, BairroRepository bairros, RegrasProperties regras, Clock relogio) {
        this.casos = casos;
        this.bairros = bairros;
        this.regras = regras;
        this.relogio = relogio;
    }

    public Alertas gerar() {
        Map<Long, Bairro> mapaBairros = bairros.findAll().stream()
                .collect(Collectors.toMap(Bairro::getId, Function.identity()));
        LocalDateTime desde = LocalDateTime.now(relogio).minus(Duration.ofHours(regras.getAlertas().getJanelaHoras()));

        List<AlertaCaso> recentes = casos.findByAtualizadoEmAfter(desde).stream()
                .sorted(Comparator.comparing(Caso::getAtualizadoEm).reversed())
                .map(c -> new AlertaCaso(c.getId(), c.getCodigoAcompanhamento(), c.getTipo(), c.getStatus(),
                        mapaBairros.get(c.getBairroId()).getNome(), c.getScore(), c.getAtualizadoEm()))
                .toList();

        Map<String, List<Caso>> abertosPorCelula = casos.findByStatusIn(StatusCaso.ABERTOS).stream()
                .collect(Collectors.groupingBy(Caso::getCelula));
        List<AlertaCelula> criticas = abertosPorCelula.entrySet().stream()
                .filter(e -> e.getValue().size() >= regras.getAlertas().getCasosPorCelula())
                .map(e -> new AlertaCelula(e.getKey(),
                        mapaBairros.get(e.getValue().get(0).getBairroId()).getNome(), e.getValue().size()))
                .sorted(Comparator.comparingLong(AlertaCelula::abertos).reversed())
                .toList();

        return new Alertas(recentes, criticas, recentes.size() + criticas.size());
    }
}
