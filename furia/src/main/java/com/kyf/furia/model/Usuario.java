package com.kyf.furia.model;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.kyf.furia.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Table(name = "usuarios")
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "idUsuario")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(unique = true, name = "id_usuario")
    private UUID idUsuario;

    @Column(nullable = false, name = "nome")
    private String nome;

    @Column(nullable = false, name = "email")
    private String email;

    @Column(nullable = false, name = "senha")
    private String senha;


    @Column(nullable = false, name = "cpf")
    private String cpf;

    @Column(nullable = false, name = "endereco")
    private String endereco;

    @ElementCollection
    private List<String> interesses;

    @ElementCollection
    private List<String> atividades;

    @ElementCollection
    private List<String> eventos;

    @ElementCollection
    private List<String> compras;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "role")
    private Role role;


     @Column(name = "data_conta")
    private LocalDate dataConta;
}
