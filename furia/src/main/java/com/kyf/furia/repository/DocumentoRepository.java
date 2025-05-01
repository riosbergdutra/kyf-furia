package com.kyf.furia.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kyf.furia.model.Documento;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, UUID> {
    
}
