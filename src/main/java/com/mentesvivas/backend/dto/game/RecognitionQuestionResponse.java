package com.mentesvivas.backend.dto.game;

import java.util.List;
import java.util.UUID;

public record RecognitionQuestionResponse(
        UUID sessionId, int position, int totalQuestions, String prompt, String photoUrl, List<Option> options
) {
    public record Option(UUID id, String name, String kinship) {}
}
