package br.com.cpet.dominio;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "RECURSO")
@Getter
@Setter
public class Recurso {
    @Id
    private Long id;
    private String nome;
    @Enumerated(EnumType.STRING)
    private TipoRecurso tipo;
    private double lat;
    private double lng;
    @Column(name = "VAGAS_DISPONIVEIS")
    private int vagasDisponiveis;
    @Column(name = "TIPOS_ACEITOS")
    private String tiposAceitos;
    private int ativo;

    public boolean estaAtivo() {
        return ativo == 1;
    }

    public Set<TipoCaso> tiposAceitosSet() {
        if (tiposAceitos == null || tiposAceitos.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(tiposAceitos.split(","))
                .map(String::trim)
                .map(TipoCaso::valueOf)
                .collect(Collectors.toSet());
    }
}
