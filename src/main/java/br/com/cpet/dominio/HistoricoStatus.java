package br.com.cpet.dominio;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "HISTORICO_STATUS")
@Getter
@Setter
public class HistoricoStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_historico")
    @SequenceGenerator(name = "seq_historico", sequenceName = "SEQ_HISTORICO", allocationSize = 1)
    private Long id;
    @Column(name = "CASO_ID")
    private Long casoId;
    @Column(name = "STATUS_ANTERIOR")
    @Enumerated(EnumType.STRING)
    private StatusCaso statusAnterior;
    @Column(name = "STATUS_NOVO")
    @Enumerated(EnumType.STRING)
    private StatusCaso statusNovo;
    private String autor;
    private String observacao;
    @Column(name = "CRIADO_EM")
    private LocalDateTime criadoEm;
}
