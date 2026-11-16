package br.com.cpet.dominio;

import static br.com.cpet.dominio.StatusCaso.ARQUIVADO;
import static br.com.cpet.dominio.StatusCaso.ATENDIDO;
import static br.com.cpet.dominio.StatusCaso.ENCAMINHADO;
import static br.com.cpet.dominio.StatusCaso.REPORTADO;
import static br.com.cpet.dominio.StatusCaso.TRIAGEM;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StatusCasoTest {

    @ParameterizedTest
    @CsvSource({
        "REPORTADO, TRIAGEM",
        "REPORTADO, ARQUIVADO",
        "TRIAGEM, ENCAMINHADO",
        "TRIAGEM, ARQUIVADO",
        "ENCAMINHADO, ATENDIDO",
        "ENCAMINHADO, TRIAGEM",
        "ENCAMINHADO, ARQUIVADO"
    })
    void transicoesPermitidas(StatusCaso de, StatusCaso para) {
        assertThat(de.podeIrPara(para)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        "REPORTADO, ENCAMINHADO",
        "REPORTADO, ATENDIDO",
        "TRIAGEM, ATENDIDO",
        "TRIAGEM, REPORTADO",
        "ENCAMINHADO, REPORTADO",
        "ATENDIDO, TRIAGEM",
        "ATENDIDO, ARQUIVADO",
        "ARQUIVADO, TRIAGEM",
        "ARQUIVADO, ATENDIDO",
        "TRIAGEM, TRIAGEM"
    })
    void transicoesProibidas(StatusCaso de, StatusCaso para) {
        assertThat(de.podeIrPara(para)).isFalse();
    }

    @Test
    void estadosFinaisNaoTemSaida() {
        assertThat(ATENDIDO.destinos()).isEmpty();
        assertThat(ARQUIVADO.destinos()).isEmpty();
    }

    @Test
    void abertosSaoOsTresPrimeiros() {
        Set<StatusCaso> abertos = EnumSet.of(REPORTADO, TRIAGEM, ENCAMINHADO);

        assertThat(StatusCaso.ABERTOS).isEqualTo(abertos);
        assertThat(ATENDIDO.aberto()).isFalse();
        assertThat(ARQUIVADO.aberto()).isFalse();
    }
}
