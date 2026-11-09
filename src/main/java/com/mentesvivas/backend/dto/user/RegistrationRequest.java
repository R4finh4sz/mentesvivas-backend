package com.mentesvivas.backend.dto.user;

import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.util.Normalization;
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

public record RegistrationRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @CPF String cpf,
        @NotNull @Past LocalDate birthDate,
        @NotBlank @Pattern(regexp = "^\\d{10,11}$", message = "informe DDD e numero, apenas digitos") String phone,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72, message = "a senha deve ter entre 8 e 72 caracteres") String password,
        @NotBlank String passwordConfirmation,
        @NotNull Role role,
        @NotNull Integer termsVersion,
        @AssertTrue(message = "e necessario aceitar os Termos de Uso e a Politica de Privacidade") boolean acceptTerms,
        @AssertTrue(message = "e necessario consentir com o tratamento de dados de saude") boolean acceptHealthData
) {

    public RegistrationRequest {
        fullName = Normalization.name(fullName);
        cpf = Normalization.cpf(cpf);
        phone = Normalization.phone(phone);
        email = Normalization.email(email);
    }

    @AssertTrue(message = "a confirmacao deve ser igual a senha")
    public boolean isPasswordConfirmationEqual() {
        return Objects.equals(password, passwordConfirmation);
    }

    @AssertTrue(message = "no aplicativo o cadastro e apenas de PACIENTE ou CUIDADOR")
    public boolean isRoleAllowed() {
        return role == null || role == Role.PATIENT || role == Role.CAREGIVER;
    }
}
