package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.entity.patient.BathCooperation;
import com.mentesvivas.backend.entity.patient.MealPeriod;
import com.mentesvivas.backend.entity.patient.MeasurementSource;
import com.mentesvivas.backend.entity.patient.Mood;
import com.mentesvivas.backend.entity.patient.RoutineType;
import com.mentesvivas.backend.entity.patient.SleepQuality;
import com.mentesvivas.backend.entity.patient.UrineAspect;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record RecordRoutineRequest(
        @NotNull(message = "informe o tipo do registro") RoutineType type,
        Mood mood,
        SleepQuality sleepQuality,
        BigDecimal sleepHours,
        MealPeriod mealPeriod,
        Boolean mealAccepted,
        Integer amountMl,
        BathCooperation bathCooperation,
        Integer urineFrequency,
        UrineAspect urineAspect,
        Integer stoolFrequency,
        Integer bristolScale,
        @Size(max = 500, message = "as observacoes aceitam ate 500 caracteres") String notes,
        MeasurementSource source,
        @PastOrPresent(message = "a data e hora nao pode estar no futuro") Instant occurredAt
) {
}
