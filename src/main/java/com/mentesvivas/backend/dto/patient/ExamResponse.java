package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.entity.patient.ExamCategory;
import com.mentesvivas.backend.entity.patient.ExamFileType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExamResponse(
        UUID id,
        ExamCategory category,
        ExamFileType fileType,
        String title,
        String fileName,
        String contentType,
        long sizeBytes,
        LocalDate examDate,
        String notes,
        UUID uploadedBy,
        Instant createdAt
) {
}
