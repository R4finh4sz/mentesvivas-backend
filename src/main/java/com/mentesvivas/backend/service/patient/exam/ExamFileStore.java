package com.mentesvivas.backend.service.patient.exam;

import java.util.Optional;
import java.util.UUID;

public interface ExamFileStore {

    void save(UUID examId, byte[] content);

    Optional<byte[]> find(UUID examId);

    void delete(UUID examId);
}
