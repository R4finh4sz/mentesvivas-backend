package com.mentesvivas.backend.dto.patient;

import com.mentesvivas.backend.entity.patient.ExamCategory;
import com.mentesvivas.backend.entity.patient.ExamFileType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UploadExamRequest(
        @NotNull(message = "informe a categoria do exame") ExamCategory category,
        @NotNull(message = "informe se o arquivo e imagem ou PDF") ExamFileType fileType,
        @NotBlank(message = "informe o titulo")
        @Size(max = 120, message = "o titulo aceita ate 120 caracteres") String title,
        @NotBlank(message = "informe o link do arquivo")
        @Size(max = 500, message = "o link aceita ate 500 caracteres")
        @Pattern(regexp = "^https://\\S+$", message = "informe o link do arquivo comecando com https://")
        String fileUrl,
        @NotNull(message = "informe a data do exame") LocalDate examDate,
        @Size(max = 500, message = "as observacoes aceitam ate 500 caracteres") String notes
) {
}
