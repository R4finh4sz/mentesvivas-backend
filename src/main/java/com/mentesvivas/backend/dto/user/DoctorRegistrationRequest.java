package com.mentesvivas.backend.dto.user;

import com.mentesvivas.backend.util.BrazilianState;
import com.mentesvivas.backend.util.Normalization;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

import java.util.Objects;

public record DoctorRegistrationRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @CPF String cpf,
        @NotBlank @Pattern(regexp = "^\\d{10,11}$", message = "informe DDD e numero, apenas digitos") String phone,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72, message = "a senha deve ter entre 8 e 72 caracteres") String password,
        @NotBlank String passwordConfirmation,
        @NotBlank @Pattern(regexp = "^\\d{1,10}$", message = "CRM deve conter apenas digitos") String crm,
        @NotBlank @Pattern(
                regexp = BrazilianState.REGEX,
                message = "UF invalida") String crmState,
        @NotNull Integer termsVersion,
        @AssertTrue(message = "e necessario aceitar os Termos de Uso e a Politica de Privacidade") boolean acceptTerms
) {

    public DoctorRegistrationRequest {
        fullName = Normalization.name(fullName);
        cpf = Normalization.cpf(cpf);
        phone = Normalization.phone(phone);
        email = Normalization.email(email);
        crm = Normalization.crm(crm);
        crmState = Normalization.state(crmState);
    }

    @AssertTrue(message = "a confirmacao deve ser igual a senha")
    public boolean isPasswordConfirmationEqual() {
        return Objects.equals(password, passwordConfirmation);
    }
}
