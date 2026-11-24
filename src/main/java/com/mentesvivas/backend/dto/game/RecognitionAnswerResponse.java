package com.mentesvivas.backend.dto.game;

public record RecognitionAnswerResponse(
        boolean correct, String message, String audioUrl, boolean finished, RecognitionQuestionResponse nextQuestion
) {}
