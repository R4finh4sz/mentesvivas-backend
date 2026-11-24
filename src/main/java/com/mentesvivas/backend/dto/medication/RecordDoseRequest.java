package com.mentesvivas.backend.dto.medication;

import com.mentesvivas.backend.entity.medication.DoseStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record RecordDoseRequest(
        @NotNull(message = "informe o horario previsto da dose") Instant scheduledFor,
        @NotNull(message = "informe o status da dose") DoseStatus status,
        UUID clientOperationId
) {
    public RecordDoseRequest(Instant scheduledFor, DoseStatus status) {
        this(scheduledFor, status, null);
    }
}
