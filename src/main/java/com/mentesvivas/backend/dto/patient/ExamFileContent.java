package com.mentesvivas.backend.dto.patient;

public record ExamFileContent(String fileName, String contentType, byte[] content) {
}
