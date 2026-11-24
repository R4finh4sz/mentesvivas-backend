package com.mentesvivas.backend.dto.game;

public record GamePerformanceReport(
        GamePerformanceMetrics familyRecognition,
        GamePerformanceMetrics musicalBingo,
        String notice
) {}
