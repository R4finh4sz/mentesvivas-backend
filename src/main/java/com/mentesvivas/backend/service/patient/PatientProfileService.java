package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.dto.patient.PatientProfileResponse;
import com.mentesvivas.backend.dto.patient.SensitiveChangeResponse;
import com.mentesvivas.backend.dto.patient.UpdatePatientProfileRequest;
import com.mentesvivas.backend.entity.patient.ChangeType;
import com.mentesvivas.backend.entity.patient.PatientProfile;
import com.mentesvivas.backend.entity.patient.SensitiveField;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.mapper.patient.PatientMapper;
import com.mentesvivas.backend.repository.patient.AllergyRepository;
import com.mentesvivas.backend.repository.patient.BodyMeasurementRepository;
import com.mentesvivas.backend.repository.patient.EmergencyContactRepository;
import com.mentesvivas.backend.repository.patient.SensitiveDataChangeRepository;
import com.mentesvivas.backend.security.CurrentPatient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PatientProfileService {

    private final CurrentPatient currentPatient;
    private final PatientProfileLoader profileLoader;
    private final AllergyRepository allergyRepository;
    private final BodyMeasurementRepository measurementRepository;
    private final EmergencyContactRepository contactRepository;
    private final SensitiveDataChangeRepository changeRepository;
    private final ProfileCompletenessService completenessService;
    private final SensitiveChangeRecorder changeRecorder;

    @Transactional
    public PatientProfileResponse get() {
        User patient = currentPatient.get();
        return toResponse(patient, profileLoader.getOrCreate(patient.getId()));
    }

    @Transactional
    public PatientProfileResponse update(UpdatePatientProfileRequest request) {
        User patient = currentPatient.get();
        PatientProfile profile = profileLoader.getOrCreate(patient.getId());

        if (request.bloodType() != null) {
            profile.setBloodType(request.bloodType());
        }
        if (request.declaredDisease() != null && !Objects.equals(request.declaredDisease(), profile.getDeclaredDisease())) {
            profile.setDeclaredDisease(request.declaredDisease());
            profile.setDeclaredDiseaseValidated(false);
            changeRecorder.record(patient.getId(), SensitiveField.DECLARED_DISEASE, ChangeType.UPDATED,
                    "Doenca declarada alterada pelo paciente (nao validada)", patient.getId());
        }
        return toResponse(patient, profile);
    }

    @Transactional(readOnly = true)
    public List<SensitiveChangeResponse> changes() {
        return changeRepository.findByPatientIdOrderByChangedAtDesc(currentPatient.get().getId()).stream()
                .map(PatientMapper::toResponse)
                .toList();
    }

    private PatientProfileResponse toResponse(User patient, PatientProfile profile) {
        var weight = measurementRepository.findFirstByPatientIdAndWeightKgIsNotNullOrderByMeasuredAtDesc(patient.getId());
        var height = measurementRepository.findFirstByPatientIdAndHeightMIsNotNullOrderByMeasuredAtDesc(patient.getId());
        return new PatientProfileResponse(
                profile.getBloodType(),
                profile.getDeclaredDisease(),
                profile.isDeclaredDiseaseValidated(),
                profile.getAllergyStatus(),
                allergyRepository.findByPatientIdOrderByCreatedAtAsc(patient.getId()).stream()
                        .map(PatientMapper::toResponse).toList(),
                weight.map(m -> m.getWeightKg()).orElse(null),
                height.map(m -> m.getHeightM()).orElse(null),
                contactRepository.findByPatientIdOrderByContactOrderAsc(patient.getId()).stream()
                        .map(PatientMapper::toResponse).toList(),
                completenessService.evaluate(patient.getId(), profile.getAllergyStatus()));
    }
}
