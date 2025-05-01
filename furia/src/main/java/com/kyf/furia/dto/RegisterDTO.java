package com.kyf.furia.dto;

public record RegisterDTO(
    String nome,
    String cpf,
    String endereco,
    String email,
    String senha    
) {
    
}
