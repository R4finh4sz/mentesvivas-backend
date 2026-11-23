package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.entity.patient.BathCooperation;
import com.mentesvivas.backend.entity.patient.MealPeriod;
import com.mentesvivas.backend.entity.patient.MeasurementSource;
import com.mentesvivas.backend.entity.patient.Mood;
import com.mentesvivas.backend.entity.patient.RoutineType;
import com.mentesvivas.backend.entity.patient.SleepQuality;
import com.mentesvivas.backend.entity.patient.UrineAspect;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RoutineRecordResponse(
        UUID id,
        RoutineType type,
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
        String notes,
        MeasurementSource source,
        boolean checkAgainstOriginal,
        UUID recordedBy,
        Instant occurredAt,
        Instant recordedAt
) {
}
