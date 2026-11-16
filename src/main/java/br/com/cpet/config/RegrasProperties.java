package br.com.cpet.config;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import br.com.cpet.dominio.TipoCaso;
import br.com.cpet.dominio.TipoRecurso;
import lombok.Data;

@Data
@ConfigurationProperties(prefix = "cpet.regras")
public class RegrasProperties {

    private Duplicata duplicata = new Duplicata();
    private Score score = new Score();
    private Encaminhamento encaminhamento = new Encaminhamento();
    private Alertas alertas = new Alertas();

    @Data
    public static class Duplicata {
        private double raioMetros;
        private int janelaHoras;
    }

    @Data
    public static class Score {
        private Map<TipoCaso, Integer> urgencia = new EnumMap<>(TipoCaso.class);
        private Densidade densidade = new Densidade();
        private int vulnerabilidadeMax;
        private Envelhecimento envelhecimento = new Envelhecimento();
    }

    @Data
    public static class Densidade {
        private int pontosPorCaso;
        private int teto;
    }

    @Data
    public static class Envelhecimento {
        private int pontosPorDia;
        private int teto;
    }

    @Data
    public static class Encaminhamento {
        private int maxSugestoes;
        private Map<TipoCaso, List<TipoRecurso>> tiposDeRecurso = new EnumMap<>(TipoCaso.class);
    }

    @Data
    public static class Alertas {
        private int janelaHoras;
        private int casosPorCelula;
    }
}
