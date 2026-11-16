package br.com.cpet.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "USUARIO")
@Getter
@Setter
public class Usuario {
    @Id
    private Long id;
    private String nome;
    @Enumerated(EnumType.STRING)
    private Perfil perfil;
    @Column(name = "RECURSO_ID")
    private Long recursoId;
}
