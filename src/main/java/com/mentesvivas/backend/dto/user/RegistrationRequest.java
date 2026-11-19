package com.mentesvivas.backend.dto.user;

import com.mentesvivas.backend.dto.address.AddressRequest;
import com.mentesvivas.backend.dto.patient.EmergencyContactRequest;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.util.Normalization;
import com.mentesvivas.backend.validation.StrongPassword;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record RegistrationRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @CPF String cpf,
        @NotNull @Past LocalDate birthDate,
        @NotBlank @Pattern(regexp = "^\\d{10,11}$", message = "informe DDD e numero, apenas digitos") String phone,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @StrongPassword @Size(min = 8, max = 72, message = "a senha deve ter entre 8 e 72 caracteres") String password,
        @NotBlank String passwordConfirmation,
        @NotNull Role role,
        @NotNull Integer termsVersion,
        @AssertTrue(message = "e necessario aceitar os Termos de Uso e a Politica de Privacidade") boolean acceptTerms,
        @AssertTrue(message = "e necessario consentir com o tratamento de dados de saude") boolean acceptHealthData,
        Integer privacyPolicyVersion,
        Integer specificConsentVersion,
        @Size(max = 500) @Pattern(regexp = PhotoUrl.REGEX, message = PhotoUrl.MESSAGE) String photoUrl,
        @NotNull @Valid AddressRequest address,
        @Valid List<EmergencyContactRequest> emergencyContacts
) {

    public RegistrationRequest {
        photoUrl = PhotoUrl.normalize(photoUrl);
        fullName = Normalization.name(fullName);
        cpf = Normalization.cpf(cpf);
        phone = Normalization.phone(phone);
        email = Normalization.email(email);
    }

    @AssertTrue(message = "a confirmacao deve ser igual a senha")
    public boolean isPasswordConfirmationEqual() {
        return Objects.equals(password, passwordConfirmation);
    }

    @AssertTrue(message = "o paciente deve informar de dois a dez contatos de emergencia")
    public boolean isEmergencyContactsCountValid() {
        if (role != Role.PATIENT) {
            return true;
        }
        return emergencyContacts != null && emergencyContacts.size() >= 2 && emergencyContacts.size() <= 10;
    }

    @AssertTrue(message = "a ordem de acionamento nao pode se repetir")
    public boolean isEmergencyContactsOrderUnique() {
        if (emergencyContacts == null) {
            return true;
        }
        return emergencyContacts.stream().map(EmergencyContactRequest::order).distinct().count()
                == emergencyContacts.size();
    }

    @AssertTrue(message = "o cuidador nao informa contatos de emergencia no cadastro")
    public boolean isEmergencyContactsAllowedForRole() {
        return role != Role.CAREGIVER || emergencyContacts == null || emergencyContacts.isEmpty();
    }

    @AssertTrue(message = "no aplicativo o cadastro e apenas de PACIENTE ou CUIDADOR")
    public boolean isRoleAllowed() {
        return role == null || role == Role.PATIENT || role == Role.CAREGIVER;
    }
}
