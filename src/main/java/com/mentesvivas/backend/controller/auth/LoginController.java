package com.mentesvivas.backend.controller.auth;

import com.mentesvivas.backend.dto.auth.LoginRequest;
import com.mentesvivas.backend.dto.auth.LoginResult;
import com.mentesvivas.backend.dto.auth.RefreshRequest;
import com.mentesvivas.backend.dto.auth.TokenResponse;
import com.mentesvivas.backend.dto.auth.VerifyTwoFactorRequest;
import com.mentesvivas.backend.service.auth.LoginService;
import com.mentesvivas.backend.service.auth.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;
    private final TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest body) {
        LoginResult result = loginService.login(body);
        if (result.requiresTwoFactor()) {
            return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(result.challenge());
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result.tokens());
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<TokenResponse> verifyTwoFactor(@Valid @RequestBody VerifyTwoFactorRequest body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(loginService.verifyTwoFactor(body));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(tokenService.renew(body.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest body) {
        tokenService.end(body.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
