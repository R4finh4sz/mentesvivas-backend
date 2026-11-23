package com.mentesvivas.backend.service.patient.hydration;

public record HydrationGoal(HydrationGoalStatus status, Integer minMl, Integer maxMl, boolean consultDoctor) {
}
