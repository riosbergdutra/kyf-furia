package com.kyf.furia.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.kyf.furia.dto.AuthResponse;
import com.kyf.furia.dto.ChangePasswordDTO;
import com.kyf.furia.dto.LoginDTO;
import com.kyf.furia.dto.RegisterDTO;
import com.kyf.furia.dto.UsuarioDTO;
import com.kyf.furia.enums.Role;
import com.kyf.furia.model.Usuario;
import com.kyf.furia.repository.UsuarioRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final long accessTokenExpiryDuration;
    private final long refreshTokenExpiryDuration;

    public UsuarioService(UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            JwtDecoder jwtDecoder,
            @Value("${jwt.access.expiry}") long accessTokenExpiryDuration,
            @Value("${jwt.refresh.expiry}") long refreshTokenExpiryDuration) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.accessTokenExpiryDuration = accessTokenExpiryDuration;
        this.refreshTokenExpiryDuration = refreshTokenExpiryDuration;
    }

    public UsuarioDTO register(RegisterDTO dto) {
        Usuario user = new Usuario();
        user.setNome(dto.nome());
        user.setCpf(dto.cpf());
        user.setEndereco(dto.endereco());
        user.setEmail(dto.email());
        user.setSenha(passwordEncoder.encode(dto.senha()));
        user.setRole(Role.USER); // exemplo
        usuarioRepository.save(user);
        return new UsuarioDTO(
                user.getIdUsuario(),
                user.getNome(),
                user.getEmail(),
                user.getCpf(),
                user.getEndereco(),
                user.getRole());
    }

    public AuthResponse login(LoginDTO dto, HttpServletResponse response) {
        // Buscar usuário pelo nome ou e-mail
        Usuario user = usuarioRepository.findByEmail(dto.email())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Verifica a senha manualmente
        if (!passwordEncoder.matches(dto.senha(), user.getSenha())) {
            throw new RuntimeException("Senha inválida");
        }

        // Constrói o DTO para uso nos tokens
        UsuarioDTO userDTO = new UsuarioDTO(
                user.getIdUsuario(),
                user.getNome(),
                user.getEmail(),
                user.getCpf(),
                user.getEndereco(),
                user.getRole());

        // Gera os tokens
        String accessToken = generateAccessToken(userDTO);
        String refreshToken = generateRefreshToken(userDTO);

        // Define cookie com refresh token
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .path("/")
                .maxAge(refreshTokenExpiryDuration)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());

        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {
        Optional<String> maybeToken = Optional.ofNullable(
                request.getCookies() != null ? Arrays.stream(request.getCookies())
                        .filter(c -> "refreshToken".equals(c.getName()))
                        .map(Cookie::getValue)
                        .findFirst().orElse(null)
                        : null);
        if (maybeToken.isEmpty())
            throw new RuntimeException("Refresh token missing");
        String refresh = maybeToken.get();
        var jwt = jwtDecoder.decode(refresh);
        String userId = jwt.getClaimAsString("userId");

        Usuario user = usuarioRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));
        UsuarioDTO dto = new UsuarioDTO(
                user.getIdUsuario(),
                user.getNome(),
                user.getEmail(),
                user.getCpf(),
                user.getEndereco(),
                user.getRole());
        String newAccess = generateAccessToken(dto);
        String newrefresh = generateRefreshToken(dto);

        return new AuthResponse(newAccess, newrefresh);
    }

    public void changePassword(ChangePasswordDTO dto, Authentication authentication) {
        UsuarioDTO principal = (UsuarioDTO) authentication.getPrincipal();
        Usuario user = usuarioRepository.findById(principal.idUsuario())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(dto.oldPassword(), user.getSenha())) {
            throw new RuntimeException("Senha atual incorreta");
        }
        user.setSenha(passwordEncoder.encode(dto.newPassword()));
        usuarioRepository.save(user);
    }

    public void deleteAccount(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        usuarioRepository.deleteById(userId);
        SecurityContextHolder.clearContext();
    }    

    public void logout(HttpServletResponse response, Authentication authentication) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        SecurityContextHolder.clearContext();
    }

    private String generateAccessToken(UsuarioDTO user) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("mybackend")
                .subject(user.idUsuario().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(accessTokenExpiryDuration))
                .claim("role", user.role())
                .claim("userId", user.idUsuario())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private String generateRefreshToken(UsuarioDTO user) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("mybackend")
                .subject(user.idUsuario().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(refreshTokenExpiryDuration))
                .claim("userId", user.idUsuario())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
