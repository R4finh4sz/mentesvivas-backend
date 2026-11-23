package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.entity.patient.MeasurementSource;
import com.mentesvivas.backend.entity.patient.OccurrenceSeverity;
import com.mentesvivas.backend.entity.patient.OccurrenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecordOccurrenceRequest(
        @NotNull(message = "informe o tipo da ocorrencia") OccurrenceType type,
        @NotNull(message = "informe a gravidade") OccurrenceSeverity severity,
        @NotBlank(message = "descreva a ocorrencia")
        @Size(max = 1000, message = "a descricao aceita ate 1000 caracteres") String description,
        @NotNull(message = "informe se ha necessidade de ir ao hospital") Boolean needsHospital,
        MeasurementSource source
) {
}
