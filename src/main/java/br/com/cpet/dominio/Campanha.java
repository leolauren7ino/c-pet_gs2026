package br.com.cpet.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "CAMPANHA")
@Getter
@Setter
public class Campanha {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_campanha")
    @SequenceGenerator(name = "seq_campanha", sequenceName = "SEQ_CAMPANHA", allocationSize = 1)
    private Long id;
    @Column(name = "CASO_ID")
    private Long casoId;
    private BigDecimal meta;
    @Column(name = "VALOR_ARRECADADO")
    private BigDecimal valorArrecadado;
    private int ativa;
    @Column(name = "CRIADO_EM")
    private LocalDateTime criadoEm;
}
