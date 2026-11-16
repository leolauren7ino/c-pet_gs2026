package br.com.cpet.regras;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.cpet.RegrasTeste;
import br.com.cpet.dominio.Caso;
import br.com.cpet.dominio.Recurso;
import br.com.cpet.dominio.TipoCaso;
import br.com.cpet.dominio.TipoRecurso;
import br.com.cpet.regras.SugestorRecursos.Sugestao;

class SugestorRecursosTest {

    private static final double LAT = -23.5600;
    private static final double LNG = -46.6500;

    private final SugestorRecursos sugestor = new SugestorRecursos(RegrasTeste.carregar());

    @Test
    void devolveOsTresMaisProximosComDistanciaEmKm() {
        List<Recurso> todos = List.of(
                recurso(1, TipoRecurso.CLINICA, 0.05, 2, "FERIDO"),
                recurso(2, TipoRecurso.CLINICA, 0.01, 2, "FERIDO"),
                recurso(3, TipoRecurso.ONG, 0.03, 2, "FERIDO"),
                recurso(4, TipoRecurso.ONG, 0.02, 2, "FERIDO"));

        List<Sugestao> sugestoes = sugestor.sugerir(caso(TipoCaso.FERIDO), todos);

        assertThat(sugestoes).extracting(Sugestao::recursoId).containsExactly(2L, 4L, 3L);
        assertThat(sugestoes.get(0).distanciaKm()).isBetween(1.0, 1.2);
        assertThat(sugestoes.get(2).distanciaKm()).isBetween(3.3, 3.4);
    }

    @Test
    void ignoraRecursoInativoSemVagaOuDeTipoErrado() {
        Recurso inativo = recurso(1, TipoRecurso.CLINICA, 0.01, 3, "FERIDO");
        inativo.setAtivo(0);
        Recurso semVaga = recurso(2, TipoRecurso.CLINICA, 0.01, 0, "FERIDO");
        Recurso tipoErrado = recurso(3, TipoRecurso.ORGAO_PUBLICO, 0.01, 3, "FERIDO");
        Recurso naoAceitaOCaso = recurso(4, TipoRecurso.CLINICA, 0.01, 3, "FILHOTE");
        Recurso valido = recurso(5, TipoRecurso.CLINICA, 0.09, 1, "FERIDO");

        List<Sugestao> sugestoes = sugestor.sugerir(caso(TipoCaso.FERIDO),
                List.of(inativo, semVaga, tipoErrado, naoAceitaOCaso, valido));

        assertThat(sugestoes).extracting(Sugestao::recursoId).containsExactly(5L);
    }

    @Test
    void tabelaDeTiposDeRecursoPorTipoDeCaso() {
        List<Recurso> todos = List.of(
                recurso(1, TipoRecurso.ONG, 0.01, 1, "FERIDO,DOENTE,FILHOTE,COLONIA,MAUS_TRATOS,RISCO_SANITARIO"),
                recurso(2, TipoRecurso.CLINICA, 0.01, 1, "FERIDO,DOENTE,FILHOTE,COLONIA,MAUS_TRATOS,RISCO_SANITARIO"),
                recurso(3, TipoRecurso.UNIDADE_MOVEL, 0.01, 1, "FERIDO,DOENTE,FILHOTE,COLONIA,MAUS_TRATOS,RISCO_SANITARIO"),
                recurso(4, TipoRecurso.ORGAO_PUBLICO, 0.01, 1, "FERIDO,DOENTE,FILHOTE,COLONIA,MAUS_TRATOS,RISCO_SANITARIO"));

        assertThat(ids(TipoCaso.FERIDO, todos)).containsExactlyInAnyOrder(1L, 2L);
        assertThat(ids(TipoCaso.DOENTE, todos)).containsExactlyInAnyOrder(1L, 2L);
        assertThat(ids(TipoCaso.FILHOTE, todos)).containsExactly(1L);
        assertThat(ids(TipoCaso.COLONIA, todos)).containsExactlyInAnyOrder(1L, 3L);
        assertThat(ids(TipoCaso.MAUS_TRATOS, todos)).containsExactly(4L);
        assertThat(ids(TipoCaso.RISCO_SANITARIO, todos)).containsExactly(4L);
    }

    private List<Long> ids(TipoCaso tipo, List<Recurso> todos) {
        return sugestor.sugerir(caso(tipo), todos).stream().map(Sugestao::recursoId).toList();
    }

    private static Caso caso(TipoCaso tipo) {
        Caso c = new Caso();
        c.setTipo(tipo);
        c.setLat(LAT);
        c.setLng(LNG);
        return c;
    }

    private static Recurso recurso(long id, TipoRecurso tipo, double deslocamentoLat, int vagas, String aceitos) {
        Recurso r = new Recurso();
        r.setId(id);
        r.setNome("Recurso " + id);
        r.setTipo(tipo);
        r.setLat(LAT + deslocamentoLat);
        r.setLng(LNG);
        r.setVagasDisponiveis(vagas);
        r.setTiposAceitos(aceitos);
        r.setAtivo(1);
        return r;
    }
}
