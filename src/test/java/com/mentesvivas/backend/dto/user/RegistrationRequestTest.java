package com.mentesvivas.backend.dto.user;

import com.mentesvivas.backend.dto.address.AddressRequest;
import com.mentesvivas.backend.dto.patient.EmergencyContactRequest;
import com.mentesvivas.backend.entity.user.Role;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static AddressRequest address() {
        return new AddressRequest("01001000", "Praca da Se", "1", null, "Sao Paulo", "SP");
    }

    private static EmergencyContactRequest contact(String name, int order) {
        return new EmergencyContactRequest(name, "Filho", "11977776666", order);
    }

    private RegistrationRequest request(Role role, AddressRequest address, List<EmergencyContactRequest> contacts) {
        return new RegistrationRequest("Pessoa Teste", "52998224725", LocalDate.of(1950, 3, 10), "11999998888",
                "pessoa@exemplo.com", "SenhaForte1", "SenhaForte1", role, 1, true, true, null, null, null,
                address, contacts);
    }

    private boolean violates(RegistrationRequest request, String property) {
        return validator.validate(request).stream().anyMatch(v -> v.getPropertyPath().toString().equals(property));
    }

    @Test
    void patientWithAddressAndTwoContactsIsValid() {
        var request = request(Role.PATIENT, address(), List.of(contact("Ana", 1), contact("Bia", 2)));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void caregiverWithAddressAndNoContactsIsValid() {
        assertThat(validator.validate(request(Role.CAREGIVER, address(), null))).isEmpty();
    }

    @Test
    void addressIsRequired() {
        assertThat(violates(request(Role.CAREGIVER, null, null), "address")).isTrue();
    }

    @Test
    void invalidAddressIsRejected() {
        var invalid = new AddressRequest("123", "Praca da Se", "1", null, "Sao Paulo", "SP");

        assertThat(violates(request(Role.CAREGIVER, invalid, null), "address.cep")).isTrue();
    }

    @Test
    void patientNeedsAtLeastTwoContacts() {
        assertThat(violates(request(Role.PATIENT, address(), null), "emergencyContactsCountValid")).isTrue();
        assertThat(violates(request(Role.PATIENT, address(), List.of(contact("Ana", 1))),
                "emergencyContactsCountValid")).isTrue();
    }

    @Test
    void patientAcceptsAtMostTenContacts() {
        var eleven = IntStream.rangeClosed(1, 11).mapToObj(i -> contact("Ana", i)).toList();

        assertThat(violates(request(Role.PATIENT, address(), eleven), "emergencyContactsCountValid")).isTrue();
    }

    @Test
    void contactOrderCannotRepeat() {
        var duplicated = List.of(contact("Ana", 1), contact("Bia", 1));

        assertThat(violates(request(Role.PATIENT, address(), duplicated), "emergencyContactsOrderUnique")).isTrue();
    }

    @Test
    void caregiverCannotInformEmergencyContacts() {
        var request = request(Role.CAREGIVER, address(), List.of(contact("Ana", 1), contact("Bia", 2)));

        assertThat(violates(request, "emergencyContactsAllowedForRole")).isTrue();
    }

    @Test
    void invalidContactIsRejected() {
        var invalid = new EmergencyContactRequest("Ana", "Filha", "123", 1);

        assertThat(violates(request(Role.PATIENT, address(), List.of(invalid, contact("Bia", 2))),
                "emergencyContacts[0].phone")).isTrue();
    }
}
