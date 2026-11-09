package com.mentesvivas.backend.dto.auth;

import com.mentesvivas.backend.util.Normalization;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Objects;

public record ResetPasswordRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "o codigo possui seis digitos") String code,
        @NotBlank @Size(min = 8, max = 72, message = "a senha deve ter entre 8 e 72 caracteres") String newPassword,
        @NotBlank String passwordConfirmation
) {

    public ResetPasswordRequest {
        email = Normalization.email(email);
        code = Normalization.token(code);
    }

    @AssertTrue(message = "a confirmacao deve ser igual a senha")
    public boolean isPasswordConfirmationEqual() {
        return Objects.equals(newPassword, passwordConfirmation);
    }
}
