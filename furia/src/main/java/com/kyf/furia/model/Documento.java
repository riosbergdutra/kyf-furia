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
public class Documento {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(unique = true, name = "id_documento")
    private UUID idDocumento;

    @ManyToOne
    private Usuario idUsuario;

    private String arquivoPath; // Caminho do arquivo salvo
    private Boolean valido; // Resultado da validação com IA
}

