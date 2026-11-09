package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.util.Normalization;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmergencyContactRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 50) String relationship,
        @NotBlank @Pattern(regexp = "^\\d{10,11}$", message = "informe DDD e numero, apenas digitos") String phone,
        @NotNull @Min(1) Integer order
) {

    public EmergencyContactRequest {
        name = Normalization.name(name);
        relationship = Normalization.name(relationship);
        phone = Normalization.phone(phone);
    }
}
