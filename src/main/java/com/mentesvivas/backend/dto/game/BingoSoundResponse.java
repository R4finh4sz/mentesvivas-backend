package com.mentesvivas.backend.dto.game;

import java.util.UUID;

public record BingoSoundResponse(UUID id, String instrument, String audioUrl, boolean active) {
}
