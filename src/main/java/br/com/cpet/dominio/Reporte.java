package br.com.cpet.dominio;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "REPORTE")
@Getter
@Setter
public class Reporte {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_reporte")
    @SequenceGenerator(name = "seq_reporte", sequenceName = "SEQ_REPORTE", allocationSize = 1)
    private Long id;
    @Column(name = "CASO_ID")
    private Long casoId;
    private String descricao;
    @Column(name = "FOTO_PATH")
    private String fotoPath;
    @Column(name = "CONTATO_OPCIONAL")
    private String contatoOpcional;
    @Column(name = "CRIADO_EM")
    private LocalDateTime criadoEm;
}
