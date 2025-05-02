package com.kyf.furia.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.kyf.furia.service.DocumentoService;

@RestController
@RequestMapping("/documento")
public class DocumentoController {

    private final DocumentoService documentoService;

    @Autowired
    public DocumentoController(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> uploadDocumento(
            @RequestPart("arquivo") MultipartFile arquivo,
            Authentication authentication) {
        return ResponseEntity.ok(documentoService.processarDocumento(arquivo, authentication));
    }
}