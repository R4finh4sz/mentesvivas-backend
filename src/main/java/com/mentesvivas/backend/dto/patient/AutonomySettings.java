package com.mentesvivas.backend.dto.patient;

import jakarta.validation.constraints.NotNull;

public record AutonomySettings(@NotNull Mode mode, boolean restrictCareRecords, boolean restrictGames) {

    public enum Mode { AUTONOMOUS, ASSISTED }

    public static AutonomySettings defaults() {
        return new AutonomySettings(Mode.AUTONOMOUS, false, false);
    }
}
