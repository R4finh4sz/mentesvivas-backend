package com.mentesvivas.backend.dto.game;

import java.util.UUID;

public record StartBingoResponse(UUID sessionId, int totalRounds, BingoRoundResponse round) {
}
