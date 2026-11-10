package com.mentesvivas.backend.controller.mfa;

import com.mentesvivas.backend.dto.mfa.AuthenticatorSetupResponse;
import com.mentesvivas.backend.dto.mfa.MfaCodeRequest;
import com.mentesvivas.backend.dto.mfa.MfaStatusResponse;
import com.mentesvivas.backend.service.mfa.AuthenticatorEnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/mfa")
@RequiredArgsConstructor
public class MfaController {

    private final AuthenticatorEnrollmentService service;

    @GetMapping
    public MfaStatusResponse status() {
        return service.status();
    }

    @PostMapping("/authenticator/setup")
    public AuthenticatorSetupResponse setup() {
        return service.begin();
    }

    @PostMapping("/authenticator/confirm")
    public MfaStatusResponse confirm(@Valid @RequestBody MfaCodeRequest body) {
        return service.confirm(body.code());
    }

    @PostMapping("/authenticator/disable")
    public MfaStatusResponse disable(@Valid @RequestBody MfaCodeRequest body) {
        return service.disable(body.code());
    }
}
