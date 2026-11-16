package br.com.cpet.regras;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.cpet.RegrasTeste;
import br.com.cpet.dominio.Caso;
import br.com.cpet.dominio.StatusCaso;
import br.com.cpet.dominio.TipoCaso;

class DetectorDuplicataTest {

    private static final double LAT = -23.5600;
    private static final double LNG = -46.6500;
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 16, 12, 0);

    private final DetectorDuplicata detector = new DetectorDuplicata(RegrasTeste.carregar());

    @Test
    void haversineCalculaDistanciaEmMetros() {
        double metros = Geo.distanciaMetros(LAT, LNG, LAT + 0.001, LNG);

        assertThat(metros).isBetween(110.0, 112.5);
    }

    @Test
    void celulaArredondaParaDuasCasas() {
        assertThat(Geo.celula(-23.6649, -46.7701)).isEqualTo("-23.66,-46.77");
        assertThat(Geo.celula(-23.6651, -46.7749)).isEqualTo("-23.67,-46.77");
    }

    @Test
    void mesmoTipoA30mViraDuplicata() {
        Caso aberto = caso(1L, TipoCaso.FERIDO, StatusCaso.TRIAGEM, LAT, LNG, AGORA.minusHours(5));

        var achado = detector.encontrar(TipoCaso.FERIDO, LAT + 0.0003, LNG, List.of(aberto), AGORA);

        assertThat(achado).containsSame(aberto);
    }

    @Test
    void alemDe50mNaoEhDuplicata() {
        Caso aberto = caso(1L, TipoCaso.FERIDO, StatusCaso.REPORTADO, LAT, LNG, AGORA.minusHours(5));

        var achado = detector.encontrar(TipoCaso.FERIDO, LAT + 0.0006, LNG, List.of(aberto), AGORA);

        assertThat(achado).isEmpty();
    }

    @Test
    void tipoDiferenteNaoEhDuplicata() {
        Caso aberto = caso(1L, TipoCaso.FERIDO, StatusCaso.REPORTADO, LAT, LNG, AGORA.minusHours(5));

        assertThat(detector.encontrar(TipoCaso.DOENTE, LAT, LNG, List.of(aberto), AGORA)).isEmpty();
    }

    @Test
    void casoCriadoHaMaisDe48hNaoContaComoDuplicata() {
        Caso antigo = caso(1L, TipoCaso.COLONIA, StatusCaso.REPORTADO, LAT, LNG, AGORA.minusHours(49));
        Caso limite = caso(2L, TipoCaso.COLONIA, StatusCaso.REPORTADO, LAT, LNG, AGORA.minusHours(47));

        assertThat(detector.encontrar(TipoCaso.COLONIA, LAT, LNG, List.of(antigo), AGORA)).isEmpty();
        assertThat(detector.encontrar(TipoCaso.COLONIA, LAT, LNG, List.of(limite), AGORA)).containsSame(limite);
    }

    @Test
    void casoFechadoNaoRecebeNovosReportes() {
        Caso atendido = caso(1L, TipoCaso.FERIDO, StatusCaso.ATENDIDO, LAT, LNG, AGORA.minusHours(5));
        Caso arquivado = caso(2L, TipoCaso.FERIDO, StatusCaso.ARQUIVADO, LAT, LNG, AGORA.minusHours(5));

        assertThat(detector.encontrar(TipoCaso.FERIDO, LAT, LNG, List.of(atendido, arquivado), AGORA)).isEmpty();
    }

    @Test
    void escolheOCasoMaisProximoQuandoHaVarios() {
        Caso longe = caso(1L, TipoCaso.FERIDO, StatusCaso.REPORTADO, LAT + 0.0004, LNG, AGORA.minusHours(2));
        Caso perto = caso(2L, TipoCaso.FERIDO, StatusCaso.REPORTADO, LAT + 0.0001, LNG, AGORA.minusHours(2));

        var achado = detector.encontrar(TipoCaso.FERIDO, LAT, LNG, List.of(longe, perto), AGORA);

        assertThat(achado).containsSame(perto);
    }

    private static Caso caso(Long id, TipoCaso tipo, StatusCaso status, double lat, double lng, LocalDateTime criado) {
        Caso c = new Caso();
        c.setId(id);
        c.setTipo(tipo);
        c.setStatus(status);
        c.setLat(lat);
        c.setLng(lng);
        c.setCriadoEm(criado);
        return c;
    }
}
