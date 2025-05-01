package com.kyf.furia.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import com.kyf.furia.dto.AuthResponse;
import com.kyf.furia.dto.ChangePasswordDTO;
import com.kyf.furia.dto.LoginDTO;
import com.kyf.furia.dto.RegisterDTO;
import com.kyf.furia.dto.UsuarioDTO;
import com.kyf.furia.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/usuario")
public class UsuarioController {
    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping("/user/register")
    public ResponseEntity<UsuarioDTO> register(@RequestBody RegisterDTO dto) {
        return ResponseEntity.ok(service.register(dto));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginDTO dto,
            HttpServletResponse response) {
        return ResponseEntity.ok(service.login(dto, response));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        return ResponseEntity.ok(service.refreshToken(request, response));
    }

    @PostMapping("/user/password")
    public ResponseEntity<Void> changePassword(
            @RequestBody ChangePasswordDTO dto,
            Authentication auth) {
        service.changePassword(dto, auth);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/user/delete")
    public ResponseEntity<Void> deleteAccount(Authentication auth) {
        service.deleteAccount(auth);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(
            HttpServletResponse response,
            Authentication auth) {
        service.logout(response, auth);
        return ResponseEntity.noContent().build();
    }
}
