package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.service.patient.hydration.HydrationGoalStatus;

import java.math.BigDecimal;

public record HydrationResponse(
        HydrationGoalStatus goalStatus,
        BigDecimal weightKg,
        Integer goalMinMl,
        Integer goalMaxMl,
        boolean consultDoctorAboutGoal,
        boolean conditionValidated,
        int consumedTodayMl,
        Integer remainingMinMl,
        Integer remainingMaxMl
) {
}
