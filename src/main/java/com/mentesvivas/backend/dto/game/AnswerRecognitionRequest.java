package com.mentesvivas.backend.dto.game;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record AnswerRecognitionRequest(@Positive int position, @NotNull UUID memberId) {}
