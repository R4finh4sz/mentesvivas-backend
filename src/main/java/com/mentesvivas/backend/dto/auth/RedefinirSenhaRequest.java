package com.mentesvivas.backend.dto.auth;

import com.mentesvivas.backend.util.Normalizacao;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Objects;

public record RedefinirSenhaRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "o codigo possui seis digitos") String codigo,
        @NotBlank @Size(min = 8, max = 72, message = "a senha deve ter entre 8 e 72 caracteres") String novaSenha,
        @NotBlank String confirmacaoSenha
) {

    public RedefinirSenhaRequest {
        email = Normalizacao.email(email);
        codigo = Normalizacao.token(codigo);
    }

    @AssertTrue(message = "a confirmacao deve ser igual a senha")
    public boolean isConfirmacaoSenhaIgual() {
        return Objects.equals(novaSenha, confirmacaoSenha);
    }
}
