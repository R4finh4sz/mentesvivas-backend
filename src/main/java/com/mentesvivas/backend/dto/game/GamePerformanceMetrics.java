package com.mentesvivas.backend.dto.game;

public record GamePerformanceMetrics(
        long completedSessions,
        long answeredQuestions,
        long errors,
        Double averageResponseMs
) {}
