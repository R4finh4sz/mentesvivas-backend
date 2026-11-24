package com.mentesvivas.backend.dto.game;

import java.util.List;
import java.util.UUID;

public record BingoRoundResponse(
        UUID roundId,
        int position,
        int totalRounds,
        String soundUrl,
        int soundSeconds,
        boolean narrationEnabled,
        String narrationText,
        List<Option> options
) {

    public record Option(UUID soundId, String instrument) {
    }
}
