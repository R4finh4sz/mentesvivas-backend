package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.entity.patient.AppointmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record SaveAppointmentRequest(
        @NotNull(message = "informe se e consulta ou exame") AppointmentType type,
        @NotBlank(message = "informe o titulo")
        @Size(max = 120, message = "o titulo aceita ate 120 caracteres") String title,
        @Size(max = 200, message = "o local aceita ate 200 caracteres") String location,
        @Size(max = 500, message = "as observacoes aceitam ate 500 caracteres") String notes,
        @NotNull(message = "informe a data e a hora") Instant scheduledAt
) {
}
