package br.com.cpet.regras;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import br.com.cpet.RegrasTeste;
import br.com.cpet.dominio.TipoCaso;
import br.com.cpet.regras.CalculadoraScore.Fator;
import br.com.cpet.regras.CalculadoraScore.ScoreDetalhe;

class CalculadoraScoreTest {

    private final CalculadoraScore calculadora = new CalculadoraScore(RegrasTeste.carregar());

    @Test
    void somaUrgenciaDensidadeVulnerabilidadeEEnvelhecimento() {
        ScoreDetalhe score = calculadora.calcular(TipoCaso.FERIDO, 4, 10, 3);

        assertThat(pontos(score, "urgencia")).isEqualTo(40);
        assertThat(pontos(score, "densidade")).isEqualTo(12);
        assertThat(pontos(score, "vulnerabilidade")).isEqualTo(10);
        assertThat(pontos(score, "envelhecimento")).isEqualTo(6);
        assertThat(score.total()).isEqualTo(68);
    }

    @Test
    void urgenciaDependeDoTipo() {
        assertThat(calculadora.calcular(TipoCaso.FERIDO, 0, 0, 0).total()).isEqualTo(40);
        assertThat(calculadora.calcular(TipoCaso.DOENTE, 0, 0, 0).total()).isEqualTo(30);
        assertThat(calculadora.calcular(TipoCaso.FILHOTE, 0, 0, 0).total()).isEqualTo(25);
        assertThat(calculadora.calcular(TipoCaso.MAUS_TRATOS, 0, 0, 0).total()).isEqualTo(25);
        assertThat(calculadora.calcular(TipoCaso.RISCO_SANITARIO, 0, 0, 0).total()).isEqualTo(25);
        assertThat(calculadora.calcular(TipoCaso.COLONIA, 0, 0, 0).total()).isEqualTo(15);
    }

    @Test
    void densidadeTemTetoDe25() {
        assertThat(pontos(calculadora.calcular(TipoCaso.COLONIA, 8, 0, 0), "densidade")).isEqualTo(24);
        assertThat(pontos(calculadora.calcular(TipoCaso.COLONIA, 9, 0, 0), "densidade")).isEqualTo(25);
        assertThat(pontos(calculadora.calcular(TipoCaso.COLONIA, 40, 0, 0), "densidade")).isEqualTo(25);
    }

    @Test
    void envelhecimentoTemTetoDe15() {
        assertThat(pontos(calculadora.calcular(TipoCaso.COLONIA, 0, 0, 7), "envelhecimento")).isEqualTo(14);
        assertThat(pontos(calculadora.calcular(TipoCaso.COLONIA, 0, 0, 8), "envelhecimento")).isEqualTo(15);
        assertThat(pontos(calculadora.calcular(TipoCaso.COLONIA, 0, 0, 90), "envelhecimento")).isEqualTo(15);
    }

    @Test
    void vulnerabilidadeVaiDe0A20() {
        assertThat(pontos(calculadora.calcular(TipoCaso.COLONIA, 0, 20, 0), "vulnerabilidade")).isEqualTo(20);
        assertThat(pontos(calculadora.calcular(TipoCaso.COLONIA, 0, 35, 0), "vulnerabilidade")).isEqualTo(20);
        assertThat(pontos(calculadora.calcular(TipoCaso.COLONIA, 0, -3, 0), "vulnerabilidade")).isZero();
    }

    @Test
    void maximoEh100() {
        ScoreDetalhe score = calculadora.calcular(TipoCaso.FERIDO, 30, 20, 30);

        assertThat(score.total()).isEqualTo(100);
    }

    @Test
    void detalhamentoTrazQuatroFatoresQueSomamOTotal() {
        ScoreDetalhe score = calculadora.calcular(TipoCaso.DOENTE, 5, 17, 2);

        assertThat(score.fatores()).extracting(Fator::chave)
                .containsExactly("urgencia", "densidade", "vulnerabilidade", "envelhecimento");
        assertThat(score.fatores().stream().mapToInt(Fator::pontos).sum()).isEqualTo(score.total());
        assertThat(score.fatores()).allSatisfy(f -> assertThat(f.explicacao()).isNotBlank());
    }

    private static int pontos(ScoreDetalhe score, String chave) {
        return score.fatores().stream().filter(f -> f.chave().equals(chave)).findFirst().orElseThrow().pontos();
    }
}
