package com.mentesvivas.backend.dto.link;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RequestDoctorLinkRequest(
        @NotNull UUID doctorId,
        UUID patientId
) {
}
