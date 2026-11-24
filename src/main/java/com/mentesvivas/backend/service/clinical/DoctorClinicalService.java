package com.mentesvivas.backend.service.clinical;

import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.service.audit.AccessLog;
import com.mentesvivas.backend.dto.clinical.DeclareDiseaseRequest;
import com.mentesvivas.backend.dto.clinical.DeclareDiagnosisRequest;
import com.mentesvivas.backend.dto.clinical.DiagnosisResponse;
import com.mentesvivas.backend.dto.clinical.DefineHydrationGoalRequest;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorClinicalService {

    private final DoctorPatientAccess access;
    private final AllergyRepository allergyRepository;
    private final PatientProfileLoader profileLoader;
    private final SensitiveChangeRecorder changeRecorder;
    private final AccessLog accessLog;
    private final DiagnosisService diagnosis;

    @Transactional
    public AllergyResponse addAllergy(UUID patientId, AddAllergyRequest request) {
        User doctor = access.requireDoctorOf(patientId);
        accessLog.record(doctor.getId(), patientId, PatientDataArea.ALLERGIES);
        Allergy allergy = new Allergy();
        allergy.setPatientId(patientId);
        allergy.setCreatedBy(doctor.getId());
        apply(allergy, request);
        Allergy saved = allergyRepository.save(allergy);
        profileLoader.getOrCreate(patientId).setAllergyStatus(AllergyStatus.INFORMED);
        changeRecorder.record(patientId, SensitiveField.ALLERGY, ChangeType.CREATED,
                "Alergia adicionada pelo medico: %s (%s)".formatted(request.category(), request.severity()),
                doctor.getId(), null, describe(allergy));
        return PatientMapper.toResponse(saved);
    }

    @Transactional
    public AllergyResponse updateAllergy(UUID patientId, UUID allergyId, AddAllergyRequest request) {
        User doctor = access.requireDoctorOf(patientId);
        accessLog.record(doctor.getId(), patientId, PatientDataArea.ALLERGIES);
        Allergy allergy = findAllergy(patientId, allergyId);
        String before = describe(allergy);
        apply(allergy, request);
        changeRecorder.record(patientId, SensitiveField.ALLERGY, ChangeType.UPDATED,
                "Alergia alterada pelo medico: %s (%s)".formatted(request.category(), request.severity()),
                doctor.getId(), before, describe(allergy));
        return PatientMapper.toResponse(allergy);
    }

    @Transactional
    public void removeAllergy(UUID patientId, UUID allergyId) {
        User doctor = access.requireDoctorOf(patientId);
        accessLog.record(doctor.getId(), patientId, PatientDataArea.ALLERGIES);
        Allergy allergy = findAllergy(patientId, allergyId);
        allergyRepository.delete(allergy);
        allergyRepository.flush();
        if (!allergyRepository.existsByPatientId(patientId)) {
            profileLoader.getOrCreate(patientId).setAllergyStatus(AllergyStatus.NOT_INFORMED);
        }
        changeRecorder.record(patientId, SensitiveField.ALLERGY, ChangeType.DELETED,
                "Alergia removida pelo medico: %s".formatted(allergy.getCategory()), doctor.getId(),
                describe(allergy), null);
    }

    @Transactional
    public AllergyStatus setAllergyStatus(UUID patientId, SetAllergyStatusRequest request) {
        User doctor = access.requireDoctorOf(patientId);
        accessLog.record(doctor.getId(), patientId, PatientDataArea.ALLERGIES);
        if (allergyRepository.existsByPatientId(patientId)) {
            throw new ConflictException("Existem alergias cadastradas; o estado permanece como informada.");
        }
        PatientProfile profile = profileLoader.getOrCreate(patientId);
        if (profile.getAllergyStatus() != request.status()) {
            AllergyStatus previous = profile.getAllergyStatus();
            profile.setAllergyStatus(request.status());
            changeRecorder.record(patientId, SensitiveField.ALLERGY_STATUS, ChangeType.UPDATED,
                    "Estado das alergias alterado pelo medico para %s".formatted(request.status()), doctor.getId(),
                    String.valueOf(previous), String.valueOf(request.status()));
        }
        return profile.getAllergyStatus();
    }

    @Transactional
    public void validateDisease(UUID patientId, DeclareDiseaseRequest request) {
        User doctor = access.requireDoctorOf(patientId);
        updateDeclaredDisease(patientId, doctor.getId(), request.disease());
        diagnosis.legacy(patientId, doctor.getId(), request.disease());
    }

    @Transactional
    public DiagnosisResponse confirmDiagnosis(UUID patientId, DeclareDiagnosisRequest request) {
        User doctor = access.requireDoctorOf(patientId);
        DiagnosisResponse recorded = diagnosis.coded(patientId, doctor.getId(), request);
        updateDeclaredDisease(patientId, doctor.getId(), recorded.disease());
        return recorded;
    }

    @Transactional
    public List<DiagnosisResponse> diagnoses(UUID patientId, int page, int size) {
        User doctor = access.requireDoctorOf(patientId);
        accessLog.record(doctor.getId(), patientId, PatientDataArea.DECLARED_DISEASE);
        return diagnosis.history(patientId, page, size);
    }

    private void updateDeclaredDisease(UUID patientId, UUID doctorId, String disease) {
        accessLog.record(doctorId, patientId, PatientDataArea.DECLARED_DISEASE);
        PatientProfile profile = profileLoader.getOrCreate(patientId);
        String before = profile.getDeclaredDisease();
        profile.setDeclaredDisease(disease);
        profile.setDeclaredDiseaseValidated(true);
        changeRecorder.record(patientId, SensitiveField.DECLARED_DISEASE, ChangeType.UPDATED,
                "Doenca registrada e validada pelo medico", doctorId, before, disease);
    }

    @Transactional
    public void defineHydrationGoal(UUID patientId, DefineHydrationGoalRequest request) {
        User doctor = access.requireDoctorOf(patientId);
        accessLog.record(doctor.getId(), patientId, PatientDataArea.ROUTINE);
        PatientProfile profile = profileLoader.getOrCreate(patientId);
        Integer before = profile.getHydrationGoalMl();
        profile.setHydrationGoalMl(request.goalMl());
        changeRecorder.record(patientId, SensitiveField.HYDRATION_GOAL,
                before == null ? ChangeType.CREATED : ChangeType.UPDATED,
                "Meta hidrica definida pelo medico", doctor.getId(),
                before == null ? null : String.valueOf(before), String.valueOf(request.goalMl()));
    }

    private String describe(Allergy allergy) {
        return "%s | %s | %s".formatted(allergy.getCategory(), allergy.getDescription(), allergy.getSeverity());
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
