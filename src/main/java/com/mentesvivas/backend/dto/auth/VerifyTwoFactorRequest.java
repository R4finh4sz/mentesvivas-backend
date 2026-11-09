package com.mentesvivas.backend.dto.auth;

import com.mentesvivas.backend.util.Normalization;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyTwoFactorRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "o codigo possui seis digitos") String code
) {

    public VerifyTwoFactorRequest {
        email = Normalization.email(email);
        code = Normalization.token(code);
    }
}
