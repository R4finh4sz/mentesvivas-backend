package com.mentesvivas.backend.dto.game;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AnswerBingoRequest(
        @NotNull(message = "informe a rodada") UUID roundId,
        @NotNull(message = "informe o instrumento escolhido") UUID soundId
) {
}
