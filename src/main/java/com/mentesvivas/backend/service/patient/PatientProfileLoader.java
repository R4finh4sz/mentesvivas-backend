package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.entity.patient.PatientProfile;
import com.mentesvivas.backend.repository.patient.PatientProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PatientProfileLoader {

    private final PatientProfileRepository repository;

    public PatientProfile getOrCreate(UUID patientId) {
        return repository.findById(patientId)
                .orElseGet(() -> repository.save(new PatientProfile(patientId)));
    }
}
