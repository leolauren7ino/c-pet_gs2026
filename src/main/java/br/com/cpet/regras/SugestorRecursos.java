package br.com.cpet.regras;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import br.com.cpet.config.RegrasProperties;
import br.com.cpet.dominio.Caso;
import br.com.cpet.dominio.Recurso;
import br.com.cpet.dominio.TipoRecurso;

@Component
public class SugestorRecursos {

    public record Sugestao(Long recursoId, String nome, TipoRecurso tipo, double distanciaKm, int vagas) {
    }

    private final RegrasProperties regras;

    public SugestorRecursos(RegrasProperties regras) {
        this.regras = regras;
    }

    public List<Sugestao> sugerir(Caso caso, List<Recurso> recursos) {
        return elegiveis(caso, recursos).stream()
                .limit(regras.getEncaminhamento().getMaxSugestoes())
                .toList();
    }

    public List<Sugestao> elegiveis(Caso caso, List<Recurso> recursos) {
        List<TipoRecurso> tiposPermitidos = regras.getEncaminhamento().getTiposDeRecurso()
                .getOrDefault(caso.getTipo(), List.of());
        return recursos.stream()
                .filter(Recurso::estaAtivo)
                .filter(r -> r.getVagasDisponiveis() > 0)
                .filter(r -> tiposPermitidos.contains(r.getTipo()))
                .filter(r -> r.tiposAceitosSet().contains(caso.getTipo()))
                .map(r -> new Sugestao(r.getId(), r.getNome(), r.getTipo(),
                        arredondar1(Geo.distanciaKm(caso.getLat(), caso.getLng(), r.getLat(), r.getLng())),
                        r.getVagasDisponiveis()))
                .sorted(Comparator.comparingDouble(Sugestao::distanciaKm).thenComparing(Sugestao::nome))
                .toList();
    }

    private static double arredondar1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
