package com.kyf.furia.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kyf.furia.model.PerfilExterno;

@Repository
public interface PerfilExternoRepository extends JpaRepository<PerfilExterno, UUID>{
    
}
