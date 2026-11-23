package com.mentesvivas.backend.service.clinical;

import com.mentesvivas.backend.dto.clinical.ClinicalSummaryResponse;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.patient.AllergyStatus;
import com.mentesvivas.backend.entity.patient.PatientProfile;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.mapper.patient.PatientMapper;
import com.mentesvivas.backend.repository.patient.AllergyRepository;
import com.mentesvivas.backend.repository.patient.BodyMeasurementRepository;
import com.mentesvivas.backend.repository.patient.EmergencyContactRepository;
import com.mentesvivas.backend.repository.patient.PatientProfileRepository;
import com.mentesvivas.backend.repository.user.UserRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import com.mentesvivas.backend.service.patient.CareTeamLoader;
import com.mentesvivas.backend.util.BodyMassIndex;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicalSummaryService {

    private final DoctorPatientAccess access;
    private final UserRepository userRepository;
    private final PatientProfileRepository profileRepository;
    private final AllergyRepository allergyRepository;
    private final BodyMeasurementRepository measurementRepository;
    private final EmergencyContactRepository contactRepository;
    private final CareTeamLoader careTeamLoader;
    private final AccessLog accessLog;

    @Transactional
    public ClinicalSummaryResponse summarize(UUID patientId) {
        User doctor = access.requireDoctorOf(patientId);
        accessLog.record(doctor.getId(), patientId, PatientDataArea.PROFILE);

        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente nao encontrado."));
        PatientProfile profile = profileRepository.findById(patientId).orElse(null);
        BigDecimal weight = measurementRepository
                .findFirstByPatientIdAndWeightKgIsNotNullOrderByMeasuredAtDesc(patientId)
                .map(measurement -> measurement.getWeightKg()).orElse(null);
        BigDecimal height = measurementRepository
                .findFirstByPatientIdAndHeightMIsNotNullOrderByMeasuredAtDesc(patientId)
                .map(measurement -> measurement.getHeightM()).orElse(null);

        return new ClinicalSummaryResponse(
                patient.getId(), patient.getFullName(), patient.getBirthDate(), patient.getPhone(),
                patient.getEmail(), patient.getPhotoUrl(),
                profile == null ? null : profile.getBloodType(),
                weight, height, BodyMassIndex.calculate(weight, height),
                profile == null ? null : profile.getDeclaredDisease(),
                profile != null && profile.isDeclaredDiseaseValidated(),
                profile == null ? AllergyStatus.NOT_INFORMED : profile.getAllergyStatus(),
                allergyRepository.findByPatientIdOrderByCreatedAtAsc(patientId).stream()
                        .map(PatientMapper::toResponse).toList(),
                contactRepository.findByPatientIdOrderByContactOrderAsc(patientId).stream()
                        .map(PatientMapper::toResponse).toList(),
                careTeamLoader.load(patientId));
    }
}
