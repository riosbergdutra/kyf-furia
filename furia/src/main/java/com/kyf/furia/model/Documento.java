package com.kyf.furia.model;

import java.time.LocalDateTime;
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
    private UUID id;

    @ManyToOne
    private Usuario usuario; // Relacionamento corrigido (não use "idUsuario")

    private String tipoDocumento; // Ex: "RG", "CPF", "CNH"
    private String caminhoArquivo; // Path no sistema de arquivos ou S3
    private Boolean valido; // Resultado da validação
    
    @Column(updatable = false)
    private LocalDateTime dataUpload = LocalDateTime.now();
}

