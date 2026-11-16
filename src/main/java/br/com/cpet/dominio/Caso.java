package br.com.cpet.dominio;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "CASO")
@Getter
@Setter
public class Caso {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_caso")
    @SequenceGenerator(name = "seq_caso", sequenceName = "SEQ_CASO", allocationSize = 1)
    private Long id;
    @Enumerated(EnumType.STRING)
    private TipoCaso tipo;
    @Column(name = "QTD_ANIMAIS")
    private int qtdAnimais;
    private double lat;
    private double lng;
    private String celula;
    @Column(name = "BAIRRO_ID")
    private Long bairroId;
    @Enumerated(EnumType.STRING)
    private StatusCaso status;
    private int score;
    @Column(name = "RECURSO_ID")
    private Long recursoId;
    @Column(name = "CODIGO_ACOMPANHAMENTO")
    private String codigoAcompanhamento;
    @Column(name = "CRIADO_EM")
    private LocalDateTime criadoEm;
    @Column(name = "ATUALIZADO_EM")
    private LocalDateTime atualizadoEm;
    private int simulado;
}
