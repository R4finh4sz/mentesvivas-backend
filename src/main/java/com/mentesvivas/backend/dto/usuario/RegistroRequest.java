package com.mentesvivas.backend.dto.usuario;

import com.mentesvivas.backend.entity.usuario.Role;
import com.mentesvivas.backend.util.Normalizacao;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;
import java.util.Objects;

public record RegistroRequest(
        @NotBlank @Size(max = 150) String nomeCompleto,
        @NotBlank @CPF String cpf,
        @NotNull @Past LocalDate dataNascimento,
        @NotBlank @Pattern(regexp = "^\\d{10,11}$", message = "informe DDD e numero, apenas digitos") String telefone,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72, message = "a senha deve ter entre 8 e 72 caracteres") String senha,
        @NotBlank String confirmacaoSenha,
        @NotNull Role role,
        @NotNull Integer versaoTermos,
        @AssertTrue(message = "e necessario aceitar os Termos de Uso e a Politica de Privacidade") boolean aceiteTermos,
        @AssertTrue(message = "e necessario consentir com o tratamento de dados de saude") boolean aceiteDadosSaude
) {

    public RegistroRequest {
        nomeCompleto = Normalizacao.nome(nomeCompleto);
        cpf = Normalizacao.cpf(cpf);
        telefone = Normalizacao.telefone(telefone);
        email = Normalizacao.email(email);
    }

    @AssertTrue(message = "a confirmacao deve ser igual a senha")
    public boolean isConfirmacaoSenhaIgual() {
        return Objects.equals(senha, confirmacaoSenha);
    }

    @AssertTrue(message = "no aplicativo o cadastro e apenas de PACIENTE ou CUIDADOR")
    public boolean isRolePermitida() {
        return role == null || role == Role.PACIENTE || role == Role.CUIDADOR;
    }
}
