package br.com.cpet.servico;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.cpet.dominio.Bairro;
import br.com.cpet.dominio.Caso;
import br.com.cpet.dominio.Repositorios.BairroRepository;
import br.com.cpet.dominio.Repositorios.CasoRepository;
import br.com.cpet.dominio.StatusCaso;
import br.com.cpet.dominio.TipoCaso;
import br.com.cpet.regras.Geo;

/** Somente agregados. Nenhum método devolve a coordenada exata de um caso. */
@Service
@Transactional(readOnly = true)
public class DadosAbertosService {

    public record CelulaAgregada(String celula, double lat, double lng, String bairro, long abertos, long atendidos,
                                 long total, Map<TipoCaso, Long> abertosPorTipo) {
    }

    public record BairroAgregado(Long id, String nome, double lat, double lng, int indiceVulnerabilidade,
                                 long abertos, long atendidos, long total, Map<TipoCaso, Long> abertosPorTipo) {
    }

    public record Resumo(long abertos, long atendidos, long total, long simulados) {
    }

    private final CasoRepository casos;
    private final BairroRepository bairros;

    public DadosAbertosService(CasoRepository casos, BairroRepository bairros) {
        this.casos = casos;
        this.bairros = bairros;
    }

    public List<CelulaAgregada> celulas() {
        Map<Long, Bairro> mapaBairros = bairros.findAll().stream()
                .collect(Collectors.toMap(Bairro::getId, b -> b));
        return casos.findAll().stream()
                .collect(Collectors.groupingBy(Caso::getCelula)).entrySet().stream()
                .map(e -> {
                    double[] centro = Geo.centroDaCelula(e.getKey());
                    List<Caso> lista = e.getValue();
                    return new CelulaAgregada(e.getKey(), centro[0], centro[1],
                            mapaBairros.get(lista.get(0).getBairroId()).getNome(),
                            contar(lista, true), contar(lista, false), lista.size(), porTipo(lista));
                })
                .sorted(Comparator.comparingLong(CelulaAgregada::abertos).reversed())
                .toList();
    }

    public List<BairroAgregado> bairros() {
        Map<Long, List<Caso>> porBairro = casos.findAll().stream()
                .collect(Collectors.groupingBy(Caso::getBairroId));
        return bairros.findAll().stream()
                .map(b -> {
                    List<Caso> lista = porBairro.getOrDefault(b.getId(), List.of());
                    return new BairroAgregado(b.getId(), b.getNome(), b.getLat(), b.getLng(),
                            b.getIndiceVulnerabilidade(), contar(lista, true), contar(lista, false), lista.size(),
                            porTipo(lista));
                })
                .sorted(Comparator.comparingLong(BairroAgregado::abertos).reversed())
                .toList();
    }

    public Resumo resumo() {
        List<Caso> todos = casos.findAll();
        return new Resumo(contar(todos, true),
                todos.stream().filter(c -> c.getStatus() == StatusCaso.ATENDIDO).count(),
                todos.size(), casos.countBySimulado(1));
    }

    private static Map<TipoCaso, Long> porTipo(List<Caso> lista) {
        return lista.stream()
                .filter(c -> c.getStatus().aberto())
                .collect(Collectors.groupingBy(Caso::getTipo, () -> new EnumMap<>(TipoCaso.class), Collectors.counting()));
    }

    private static long contar(List<Caso> lista, boolean abertos) {
        return lista.stream()
                .filter(c -> abertos ? c.getStatus().aberto() : c.getStatus() == StatusCaso.ATENDIDO)
                .count();
    }
}
