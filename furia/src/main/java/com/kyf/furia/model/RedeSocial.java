package com.kyf.furia.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class RedeSocial {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(unique = true, name = "id_redesocial")
    private UUID idRedeSocial;

    @ManyToOne
    private Usuario user;

    private String tipo;
    private String username; 
    private String dadoRelacionado;
}
