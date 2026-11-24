package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.entity.patient.ClinicalColor;
import com.mentesvivas.backend.entity.patient.ClinicalMetric;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record PublishClinicalRangesRequest(
        @NotNull @Size(min = 7, max = 100) List<@NotNull @Valid Band> ranges
) {
    public record Band(@NotNull ClinicalMetric metric, @Digits(integer = 8, fraction = 6) BigDecimal lowerBound,
                       @NotNull ClinicalColor color) {}
}
