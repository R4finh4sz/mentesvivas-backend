package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.entity.patient.ExamCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UploadExamRequest(
        @NotNull(message = "informe a categoria do exame") ExamCategory category,
        @NotBlank(message = "informe o titulo")
        @Size(max = 120, message = "o titulo aceita ate 120 caracteres") String title,
        @NotNull(message = "informe a data do exame") LocalDate examDate,
        @Size(max = 500, message = "as observacoes aceitam ate 500 caracteres") String notes
) {
}
