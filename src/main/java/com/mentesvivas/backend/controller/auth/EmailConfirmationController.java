package com.mentesvivas.backend.controller.auth;

import com.mentesvivas.backend.dto.auth.CodeRequestedResponse;
import com.mentesvivas.backend.dto.auth.ConfirmEmailRequest;
import com.mentesvivas.backend.dto.auth.ResendEmailConfirmationRequest;
import com.mentesvivas.backend.service.auth.EmailConfirmationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/email")
@RequiredArgsConstructor
public class EmailConfirmationController {

    private final EmailConfirmationService emailConfirmationService;

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(@Valid @RequestBody ConfirmEmailRequest body) {
        emailConfirmationService.confirm(body.email(), body.code());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend")
    public ResponseEntity<CodeRequestedResponse> resend(@Valid @RequestBody ResendEmailConfirmationRequest body) {
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore())
                .body(emailConfirmationService.resend(body.email()));
    }
}
