package com.mentesvivas.backend.dto.usuario;

import com.mentesvivas.backend.util.Normalizacao;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

import java.util.Objects;

public record RegistroMedicoRequest(
        @NotBlank @Size(max = 150) String nomeCompleto,
        @NotBlank @CPF String cpf,
        @NotBlank @Pattern(regexp = "^\\d{10,11}$", message = "informe DDD e numero, apenas digitos") String telefone,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72, message = "a senha deve ter entre 8 e 72 caracteres") String senha,
        @NotBlank String confirmacaoSenha,
        @NotBlank @Pattern(regexp = "^\\d{1,10}$", message = "CRM deve conter apenas digitos") String crm,
        @NotBlank @Pattern(
                regexp = "^(AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO)$",
                message = "UF invalida") String crmUf,
        @NotNull Integer versaoTermos,
        @AssertTrue(message = "e necessario aceitar os Termos de Uso e a Politica de Privacidade") boolean aceiteTermos
) {

    public RegistroMedicoRequest {
        nomeCompleto = Normalizacao.nome(nomeCompleto);
        cpf = Normalizacao.cpf(cpf);
        telefone = Normalizacao.telefone(telefone);
        email = Normalizacao.email(email);
        crm = Normalizacao.crm(crm);
        crmUf = Normalizacao.uf(crmUf);
    }

    @AssertTrue(message = "a confirmacao deve ser igual a senha")
    public boolean isConfirmacaoSenhaIgual() {
        return Objects.equals(senha, confirmacaoSenha);
    }
}
