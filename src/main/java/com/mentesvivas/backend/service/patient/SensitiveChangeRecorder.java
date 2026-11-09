package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.entity.patient.ChangeType;
import com.mentesvivas.backend.entity.patient.SensitiveDataChange;
import com.mentesvivas.backend.entity.patient.SensitiveField;
import com.mentesvivas.backend.repository.patient.SensitiveDataChangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SensitiveChangeRecorder {

    private final SensitiveDataChangeRepository repository;

    public void record(UUID patientId, SensitiveField field, ChangeType type, String summary, UUID authorId) {
        repository.save(new SensitiveDataChange(patientId, field, type, summary, authorId, Instant.now()));
    }
}
