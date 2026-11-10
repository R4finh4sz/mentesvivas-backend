package com.mentesvivas.backend.service.clinical;

import com.mentesvivas.backend.dto.clinical.DeclareDiseaseRequest;
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
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.mapper.patient.PatientMapper;
import com.mentesvivas.backend.repository.patient.AllergyRepository;
import com.mentesvivas.backend.service.patient.PatientProfileLoader;
import com.mentesvivas.backend.service.patient.SensitiveChangeRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DoctorClinicalService {

    private final DoctorPatientAccess access;
    private final AllergyRepository allergyRepository;
    private final PatientProfileLoader profileLoader;
    private final SensitiveChangeRecorder changeRecorder;

    @Transactional
    public AllergyResponse addAllergy(UUID patientId, AddAllergyRequest request) {
        User doctor = access.requireDoctorOf(patientId);
        Allergy allergy = new Allergy();
        allergy.setPatientId(patientId);
        allergy.setCreatedBy(doctor.getId());
        apply(allergy, request);
        Allergy saved = allergyRepository.save(allergy);
        profileLoader.getOrCreate(patientId).setAllergyStatus(AllergyStatus.INFORMED);
        changeRecorder.record(patientId, SensitiveField.ALLERGY, ChangeType.CREATED,
                "Alergia adicionada pelo medico: %s (%s)".formatted(request.category(), request.severity()),
                doctor.getId());
        return PatientMapper.toResponse(saved);
    }

    @Transactional
    public AllergyResponse updateAllergy(UUID patientId, UUID allergyId, AddAllergyRequest request) {
        User doctor = access.requireDoctorOf(patientId);
        Allergy allergy = findAllergy(patientId, allergyId);
        apply(allergy, request);
        changeRecorder.record(patientId, SensitiveField.ALLERGY, ChangeType.UPDATED,
                "Alergia alterada pelo medico: %s (%s)".formatted(request.category(), request.severity()),
                doctor.getId());
        return PatientMapper.toResponse(allergy);
    }

    @Transactional
    public void removeAllergy(UUID patientId, UUID allergyId) {
        User doctor = access.requireDoctorOf(patientId);
        Allergy allergy = findAllergy(patientId, allergyId);
        allergyRepository.delete(allergy);
        allergyRepository.flush();
        if (!allergyRepository.existsByPatientId(patientId)) {
            profileLoader.getOrCreate(patientId).setAllergyStatus(AllergyStatus.NOT_INFORMED);
        }
        changeRecorder.record(patientId, SensitiveField.ALLERGY, ChangeType.DELETED,
                "Alergia removida pelo medico: %s".formatted(allergy.getCategory()), doctor.getId());
    }

    @Transactional
    public AllergyStatus setAllergyStatus(UUID patientId, SetAllergyStatusRequest request) {
        User doctor = access.requireDoctorOf(patientId);
        if (allergyRepository.existsByPatientId(patientId)) {
            throw new ConflictException("Existem alergias cadastradas; o estado permanece como informada.");
        }
        PatientProfile profile = profileLoader.getOrCreate(patientId);
        if (profile.getAllergyStatus() != request.status()) {
            profile.setAllergyStatus(request.status());
            changeRecorder.record(patientId, SensitiveField.ALLERGY_STATUS, ChangeType.UPDATED,
                    "Estado das alergias alterado pelo medico para %s".formatted(request.status()), doctor.getId());
        }
        return profile.getAllergyStatus();
    }

    @Transactional
    public void validateDisease(UUID patientId, DeclareDiseaseRequest request) {
        User doctor = access.requireDoctorOf(patientId);
        PatientProfile profile = profileLoader.getOrCreate(patientId);
        profile.setDeclaredDisease(request.disease());
        profile.setDeclaredDiseaseValidated(true);
        changeRecorder.record(patientId, SensitiveField.DECLARED_DISEASE, ChangeType.UPDATED,
                "Doenca registrada e validada pelo medico", doctor.getId());
    }

    private void apply(Allergy allergy, AddAllergyRequest request) {
        allergy.setCategory(request.category());
        allergy.setDescription(request.description());
        allergy.setSeverity(request.severity());
    }

    private Allergy findAllergy(UUID patientId, UUID allergyId) {
        return allergyRepository.findByIdAndPatientId(allergyId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Alergia nao encontrada."));
    }
}
