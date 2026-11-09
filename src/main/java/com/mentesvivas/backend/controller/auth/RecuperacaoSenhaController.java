package com.mentesvivas.backend.controller.auth;

import com.mentesvivas.backend.dto.auth.RecuperacaoSolicitadaResponse;
import com.mentesvivas.backend.dto.auth.RedefinirSenhaRequest;
import com.mentesvivas.backend.dto.auth.SolicitarRecuperacaoRequest;
import com.mentesvivas.backend.service.auth.RecuperacaoSenhaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
public class RecuperacaoSenhaController {

    private final RecuperacaoSenhaService recuperacaoSenhaService;

    @PostMapping("/forgot")
    public ResponseEntity<RecuperacaoSolicitadaResponse> solicitar(@Valid @RequestBody SolicitarRecuperacaoRequest body) {
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore())
                .body(recuperacaoSenhaService.solicitar(body.email()));
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> redefinir(@Valid @RequestBody RedefinirSenhaRequest body) {
        recuperacaoSenhaService.redefinir(body);
        return ResponseEntity.noContent().build();
    }
}
