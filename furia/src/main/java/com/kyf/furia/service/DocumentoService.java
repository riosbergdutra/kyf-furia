package com.kyf.furia.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import com.kyf.furia.dto.DocumentoProcessadoDTO;
import com.kyf.furia.model.Documento;
import com.kyf.furia.model.Usuario;
import com.kyf.furia.repository.DocumentoRepository;
import com.kyf.furia.repository.UsuarioRepository;

import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final UsuarioRepository usuarioRepository;

    @Value("${deepseek.api.key}")
    private String deepseekApiKey;

    @Value("${deepseek.api.url}")
    private String deepseekApiUrl;

    private static final Pattern CPF_PATTERN = Pattern.compile("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");
    private static final Pattern RG_PATTERN = Pattern.compile("\\d{2}\\.\\d{3}\\.\\d{3}-\\d{1}");

    @Autowired
    public DocumentoService(
            DocumentoRepository documentoRepository,
            UsuarioRepository usuarioRepository) {
        this.documentoRepository = documentoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public DocumentoProcessadoDTO processarDocumento(MultipartFile arquivo, Authentication authentication) {
        try {
            String textoExtraido = realizarOCRviaDeepSeek(arquivo);
            String tipoDocumento = determinarTipoDocumento(textoExtraido);
            boolean valido = validarDocumento(textoExtraido, tipoDocumento);
            String caminhoArquivo = salvarArquivo(arquivo);
            
            Documento documento = salvarDocumento(authentication, caminhoArquivo, tipoDocumento, valido);
            
            return new DocumentoProcessadoDTO(
                documento.getId(),
                documento.getTipoDocumento(),
                documento.getValido(),
                textoExtraido
            );
        } catch (Exception e) {
            throw new RuntimeException("Falha ao processar documento: " + e.getMessage(), e);
        }
    }

    private String realizarOCRviaDeepSeek(MultipartFile arquivo) throws IOException {
        HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofSeconds(30));
    
        WebClient client = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .baseUrl(deepseekApiUrl)
            .defaultHeader("Authorization", "Bearer " + deepseekApiKey)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();
    
        try {
            // Converter imagem para Base64
            String base64Image = Base64.getEncoder().encodeToString(arquivo.getBytes());
    
            // Montar payload textual com imagem embutida no prompt
            String prompt = "Extraia o texto completo deste documento mantendo a formatação original. "
                    + "A imagem está codificada em base64:\n\n"
                    + "data:image/jpeg;base64," + base64Image;
    
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", "deepseek-chat");
            payload.put("temperature", 0.1);
    
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
    
            messages.add(message);
            payload.put("messages", messages);
    
            // Enviar requisição
            Map<String, Object> response = client.post()
                .bodyValue(payload)
                .retrieve()
                .onStatus(status -> status.isError(), resp -> 
                    resp.bodyToMono(String.class)
                        .flatMap(error -> Mono.error(new RuntimeException("Erro na API: " + error)))
                )
                .bodyToMono(Map.class)
                .block();
    
            // Processar resposta
            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> messageResponse = (Map<String, Object>) choice.get("message");
                    return (String) messageResponse.get("content");
                }
            }
    
            throw new RuntimeException("Resposta da API não contém dados válidos");
    
        } catch (Exception e) {
            throw new IOException("Erro na comunicação com a API DeepSeek: " + e.getMessage(), e);
        }
    }
    

    private String salvarArquivo(MultipartFile arquivo) throws IOException {
        Path diretorioUpload = Path.of("uploads");
        if (!Files.exists(diretorioUpload)) {
            Files.createDirectories(diretorioUpload);
        }
        
        String nomeArquivo = UUID.randomUUID() + "_" + arquivo.getOriginalFilename();
        Path destino = diretorioUpload.resolve(nomeArquivo);
        Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        
        return destino.toString();
    }

    private String determinarTipoDocumento(String texto) {
        if (CPF_PATTERN.matcher(texto).find()) {
            return "CPF";
        } else if (RG_PATTERN.matcher(texto).find()) {
            return "RG";
        }
        return "OUTRO";
    }

    private boolean validarDocumento(String texto, String tipo) {
        return switch (tipo) {
            case "CPF" -> validarCPF(texto);
            case "RG" -> validarRG(texto);
            default -> false;
        };
    }

    private Documento salvarDocumento(Authentication authentication, String caminhoArquivo, String tipo, boolean valido) {
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Documento documento = new Documento();
        documento.setUsuario(usuario);
        documento.setTipoDocumento(tipo);
        documento.setCaminhoArquivo(caminhoArquivo);
        documento.setValido(valido);
        documento.setDataUpload(LocalDateTime.now());
        
        return documentoRepository.save(documento);
    }

    private boolean validarCPF(String texto) {
        String cpf = texto.replaceAll("[^\\d]", "");
        
        // Verifica tamanho e dígitos repetidos
        if (cpf.length() != 11 || cpf.matches("(\\d)\\1{10}")) {
            return false;
        }
    
        // Calcula primeiro dígito verificador
        int soma = 0;
        for (int i = 0; i < 9; i++) {
            soma += Character.getNumericValue(cpf.charAt(i)) * (10 - i);
        }
        int primeiroDigito = 11 - (soma % 11);
        if (primeiroDigito >= 10) {
            primeiroDigito = 0;
        }
    
        // Calcula segundo dígito verificador
        soma = 0;
        for (int i = 0; i < 10; i++) {
            soma += Character.getNumericValue(cpf.charAt(i)) * (11 - i);
        }
        int segundoDigito = 11 - (soma % 11);
        if (segundoDigito >= 10) {
            segundoDigito = 0;
        }
    
        // Verifica dígitos calculados
        return primeiroDigito == Character.getNumericValue(cpf.charAt(9)) 
            && segundoDigito == Character.getNumericValue(cpf.charAt(10));
    }
    
    private boolean validarRG(String texto) {
        String rg = texto.replaceAll("[^\\dXx]", "").toUpperCase();
        
        // Validação para RG padrão São Paulo (9 dígitos)
        if (rg.length() != 9) {
            return false;
        }
    
        // Cálculo do dígito verificador
        int soma = 0;
        int[] pesos = {2, 3, 4, 5, 6, 7, 8, 9};
        
        for (int i = 0; i < 8; i++) {
            soma += Character.getNumericValue(rg.charAt(i)) * pesos[i];
        }
        
        int resto = soma % 11;
        char digitoVerificador = rg.charAt(8);
        
        // Tratamento especial para dígito 'X' (quando resto é 10)
        if (resto == 10) {
            return digitoVerificador == 'X';
        }
        
        return Character.getNumericValue(digitoVerificador) == (11 - resto);
    }
}