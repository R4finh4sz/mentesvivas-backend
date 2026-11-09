package com.mentesvivas.backend.dto.patient;

public record EmergencyContactResponse(
        String name,
        String relationship,
        String phone,
        Integer order
) {
}
