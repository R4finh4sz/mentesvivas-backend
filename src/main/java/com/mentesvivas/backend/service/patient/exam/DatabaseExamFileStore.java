package com.mentesvivas.backend.service.patient.exam;

import com.mentesvivas.backend.entity.patient.ExamFile;
import com.mentesvivas.backend.repository.patient.ExamFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DatabaseExamFileStore implements ExamFileStore {

    private final ExamFileRepository repository;

    @Override
    public void save(UUID examId, byte[] content) {
        ExamFile file = new ExamFile();
        file.setExamId(examId);
        file.setContent(content);
        repository.save(file);
    }

    @Override
    public Optional<byte[]> find(UUID examId) {
        return repository.findById(examId).map(ExamFile::getContent);
    }

    @Override
    public void delete(UUID examId) {
        repository.deleteById(examId);
    }
}
