package com.mentesvivas.backend.dto.clinical;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record ExportTimelineRequest(Instant from, Instant to, @NotBlank String password, @NotBlank String code) {}
