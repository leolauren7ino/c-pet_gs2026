package br.com.cpet.regras;

import java.util.List;

import org.springframework.stereotype.Component;

import br.com.cpet.config.RegrasProperties;
import br.com.cpet.dominio.TipoCaso;

@Component
public class CalculadoraScore {

    public record Fator(String chave, String rotulo, int pontos, int maximo, String explicacao) {
    }

    public record ScoreDetalhe(int total, List<Fator> fatores) {
    }

    private final RegrasProperties regras;

    public CalculadoraScore(RegrasProperties regras) {
        this.regras = regras;
    }

    public ScoreDetalhe calcular(TipoCaso tipo, int casosAbertosNaCelula, int vulnerabilidadeBairro, long diasAberto) {
        RegrasProperties.Score cfg = regras.getScore();

        int urgenciaMax = cfg.getUrgencia().values().stream().mapToInt(Integer::intValue).max().orElse(0);
        int urgencia = cfg.getUrgencia().getOrDefault(tipo, 0);

        int densidadeBruta = casosAbertosNaCelula * cfg.getDensidade().getPontosPorCaso();
        int densidade = Math.min(densidadeBruta, cfg.getDensidade().getTeto());

        int vulnerabilidade = Math.max(0, Math.min(vulnerabilidadeBairro, cfg.getVulnerabilidadeMax()));

        long envelhecimentoBruto = Math.max(0, diasAberto) * cfg.getEnvelhecimento().getPontosPorDia();
        int envelhecimento = (int) Math.min(envelhecimentoBruto, cfg.getEnvelhecimento().getTeto());

        List<Fator> fatores = List.of(
                new Fator("urgencia", "Urgência do tipo", urgencia, urgenciaMax,
                        "Tipo do caso: " + tipo.name().toLowerCase().replace('_', ' ')),
                new Fator("densidade", "Densidade da região", densidade, cfg.getDensidade().getTeto(),
                        casosAbertosNaCelula + (casosAbertosNaCelula == 1 ? " caso aberto" : " casos abertos")
                                + " na mesma célula do mapa"),
                new Fator("vulnerabilidade", "Vulnerabilidade do bairro", vulnerabilidade, cfg.getVulnerabilidadeMax(),
                        "Índice " + vulnerabilidadeBairro + " de " + cfg.getVulnerabilidadeMax()),
                new Fator("envelhecimento", "Tempo em aberto", envelhecimento, cfg.getEnvelhecimento().getTeto(),
                        diasAberto + (diasAberto == 1 ? " dia" : " dias") + " sem resolução"));

        int total = Math.min(100, fatores.stream().mapToInt(Fator::pontos).sum());
        return new ScoreDetalhe(total, fatores);
    }
}
