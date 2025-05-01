package com.kyf.furia.dto;

import java.util.UUID;

import com.kyf.furia.enums.Role;

public record UsuarioDTO(
     UUID idUsuario,
     String nome,
     String email,
     String cpf,
     String endereco,
     Role role
) {
} 