package com.mentesvivas.backend.dto.game;

import jakarta.validation.constraints.NotNull;

public record UpdateBingoSettingsRequest(
        @NotNull(message = "informe se a narracao por voz fica ativa") Boolean narrationEnabled
) {
}
