package com.mentesvivas.backend.dto.game;

public record BingoAnswerResponse(boolean correct, String message, boolean finished, BingoRoundResponse round) {
}
