package br.com.cpet.regras;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.cpet.config.RegrasProperties;
import br.com.cpet.dominio.Caso;
import br.com.cpet.dominio.TipoCaso;

@Component
public class DetectorDuplicata {

    private final RegrasProperties regras;

    public DetectorDuplicata(RegrasProperties regras) {
        this.regras = regras;
    }

    public Optional<Caso> encontrar(TipoCaso tipo, double lat, double lng, List<Caso> candidatos, LocalDateTime agora) {
        RegrasProperties.Duplicata cfg = regras.getDuplicata();
        LocalDateTime limite = agora.minus(Duration.ofHours(cfg.getJanelaHoras()));
        return candidatos.stream()
                .filter(c -> c.getStatus().aberto())
                .filter(c -> c.getTipo() == tipo)
                .filter(c -> !c.getCriadoEm().isBefore(limite))
                .filter(c -> Geo.distanciaMetros(lat, lng, c.getLat(), c.getLng()) <= cfg.getRaioMetros())
                .min(Comparator.comparingDouble(c -> Geo.distanciaMetros(lat, lng, c.getLat(), c.getLng())));
    }
}
