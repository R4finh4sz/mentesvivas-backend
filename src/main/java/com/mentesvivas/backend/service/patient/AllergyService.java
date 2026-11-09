package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.dto.patient.AddAllergyRequest;
import com.mentesvivas.backend.dto.patient.AllergyResponse;
import com.mentesvivas.backend.dto.patient.SetAllergyStatusRequest;
import com.mentesvivas.backend.entity.patient.Allergy;
import com.mentesvivas.backend.entity.patient.AllergyStatus;
import com.mentesvivas.backend.entity.patient.ChangeType;
import com.mentesvivas.backend.entity.patient.PatientProfile;
import com.mentesvivas.backend.entity.patient.SensitiveField;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.mapper.patient.PatientMapper;
import com.mentesvivas.backend.repository.patient.AllergyRepository;
import com.mentesvivas.backend.security.CurrentPatient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AllergyService {

    private final CurrentPatient currentPatient;
    private final AllergyRepository allergyRepository;
    private final PatientProfileLoader profileLoader;
    private final SensitiveChangeRecorder changeRecorder;

    @Transactional(readOnly = true)
    public List<AllergyResponse> list() {
        return allergyRepository.findByPatientIdOrderByCreatedAtAsc(currentPatient.get().getId()).stream()
                .map(PatientMapper::toResponse)
                .toList();
    }

    @Transactional
    public AllergyResponse add(AddAllergyRequest request) {
        User patient = currentPatient.get();
        Allergy allergy = new Allergy();
        allergy.setPatientId(patient.getId());
        allergy.setCategory(request.category());
        allergy.setDescription(request.description());
        allergy.setSeverity(request.severity());
        allergy.setCreatedBy(patient.getId());
        Allergy saved = allergyRepository.save(allergy);

        profileLoader.getOrCreate(patient.getId()).setAllergyStatus(AllergyStatus.INFORMED);
        changeRecorder.record(patient.getId(), SensitiveField.ALLERGY, ChangeType.CREATED,
                "Alergia adicionada: %s (%s)".formatted(request.category(), request.severity()), patient.getId());
        return PatientMapper.toResponse(saved);
    }

    @Transactional
    public AllergyStatus setStatus(SetAllergyStatusRequest request) {
        User patient = currentPatient.get();
        if (allergyRepository.existsByPatientId(patient.getId())) {
            throw new ConflictException("Existem alergias cadastradas; o estado permanece como informada.");
        }
        PatientProfile profile = profileLoader.getOrCreate(patient.getId());
        if (profile.getAllergyStatus() != request.status()) {
            profile.setAllergyStatus(request.status());
            changeRecorder.record(patient.getId(), SensitiveField.ALLERGY_STATUS, ChangeType.UPDATED,
                    "Estado das alergias alterado para %s".formatted(request.status()), patient.getId());
        }
        return profile.getAllergyStatus();
    }
}
