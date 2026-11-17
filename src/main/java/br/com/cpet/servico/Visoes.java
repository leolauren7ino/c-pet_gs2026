package br.com.cpet.servico;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.com.cpet.dominio.StatusCaso;
import br.com.cpet.dominio.TipoCaso;
import br.com.cpet.regras.CalculadoraScore.ScoreDetalhe;
import br.com.cpet.regras.SugestorRecursos.Sugestao;

public final class Visoes {

    private Visoes() {
    }

    public record NovoReporte(TipoCaso tipo, int qtdAnimais, double lat, double lng, String descricao, String contato) {
    }

    public record ResultadoReporte(String codigo, boolean duplicata) {
    }

    public record CasoResumo(Long id, String codigo, TipoCaso tipo, int qtdAnimais, double lat, double lng,
                             String celula, Long bairroId, String bairro, StatusCaso status, int score,
                             Long recursoId, String recurso, LocalDateTime criadoEm, LocalDateTime atualizadoEm,
                             boolean simulado, long reportes) {
    }

    public record ReporteVisao(Long id, String descricao, String fotoUrl, String contato, LocalDateTime criadoEm) {
    }

    public record HistoricoVisao(StatusCaso anterior, StatusCaso novo, String autor, String observacao,
                                 LocalDateTime criadoEm) {
    }

    public record CasoDetalhe(CasoResumo caso, List<ReporteVisao> reportes, ScoreDetalhe score,
                              List<Sugestao> sugestoes, List<HistoricoVisao> historico, boolean aceito,
                              boolean contatoRegistrado, CampanhaVisao campanha) {
    }

    public record EtapaPublica(StatusCaso status, String titulo, LocalDateTime em) {
    }

    public record Acompanhamento(String codigo, TipoCaso tipo, String bairro, StatusCaso status,
                                 LocalDateTime criadoEm, LocalDateTime atualizadoEm, List<EtapaPublica> etapas,
                                 CampanhaVisao campanha) {
    }

    public record CampanhaVisao(Long id, BigDecimal meta, BigDecimal valorArrecadado, boolean ativa) {
    }
}
