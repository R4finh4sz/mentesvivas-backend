package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.entity.patient.MissingItem;

import java.util.List;

public record CompletenessResponse(
        boolean modulesUnlocked,
        List<MissingItem> missing
) {
}
