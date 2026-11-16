package br.com.cpet.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "BAIRRO")
@Getter
@Setter
public class Bairro {
    @Id
    private Long id;
    private String nome;
    private double lat;
    private double lng;
    @Column(name = "INDICE_VULNERABILIDADE")
    private int indiceVulnerabilidade;
}
