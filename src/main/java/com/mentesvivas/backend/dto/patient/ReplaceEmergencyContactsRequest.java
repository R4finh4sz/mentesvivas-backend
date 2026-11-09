package com.mentesvivas.backend.dto.patient;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReplaceEmergencyContactsRequest(
        @NotNull @Size(min = 2, max = 10, message = "informe de dois a dez contatos de emergencia") List<@Valid EmergencyContactRequest> contacts
) {

    @AssertTrue(message = "a ordem de acionamento nao pode se repetir")
    public boolean isOrderUnique() {
        if (contacts == null) {
            return true;
        }
        return contacts.stream().map(EmergencyContactRequest::order).distinct().count() == contacts.size();
    }
}
