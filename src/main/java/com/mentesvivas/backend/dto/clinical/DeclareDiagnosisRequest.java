package com.mentesvivas.backend.dto.clinical;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DeclareDiagnosisRequest(
        @NotBlank @Size(max = 300) String disease,
        @NotBlank @Size(max = 5) String primaryCid,
        @Size(max = 20) List<@NotBlank @Size(max = 5) String> secondaryCids
) {}
