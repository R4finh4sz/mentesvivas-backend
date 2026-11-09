package com.mentesvivas.backend.controller.auth;

import com.mentesvivas.backend.dto.auth.CodeRequestedResponse;
import com.mentesvivas.backend.dto.auth.ResetPasswordRequest;
import com.mentesvivas.backend.dto.auth.RequestPasswordRecoveryRequest;
import com.mentesvivas.backend.service.auth.PasswordRecoveryService;
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
public class PasswordRecoveryController {

    private final PasswordRecoveryService passwordRecoveryService;

    @PostMapping("/forgot")
    public ResponseEntity<CodeRequestedResponse> requestRecovery(@Valid @RequestBody RequestPasswordRecoveryRequest body) {
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore())
                .body(passwordRecoveryService.requestRecovery(body.email()));
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetPasswordRequest body) {
        passwordRecoveryService.reset(body);
        return ResponseEntity.noContent().build();
    }
}
