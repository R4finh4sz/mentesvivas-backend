package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.dto.patient.CompletenessResponse;
import com.mentesvivas.backend.dto.patient.UpdatePatientProfileRequest;
import com.mentesvivas.backend.entity.patient.AllergyStatus;
import com.mentesvivas.backend.entity.patient.BloodType;
import com.mentesvivas.backend.entity.patient.ChangeType;
import com.mentesvivas.backend.entity.patient.PatientProfile;
import com.mentesvivas.backend.entity.patient.SensitiveField;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.repository.patient.AllergyRepository;
import com.mentesvivas.backend.repository.patient.BodyMeasurementRepository;
import com.mentesvivas.backend.repository.patient.EmergencyContactRepository;
import com.mentesvivas.backend.repository.patient.SensitiveDataChangeRepository;
import com.mentesvivas.backend.security.CurrentPatient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientProfileServiceTest {

    private static final UUID PATIENT_ID = UUID.randomUUID();

    @Mock
    private CurrentPatient currentPatient;
    @Mock
    private PatientProfileLoader profileLoader;
    @Mock
    private AllergyRepository allergyRepository;
    @Mock
    private BodyMeasurementRepository measurementRepository;
    @Mock
    private EmergencyContactRepository contactRepository;
    @Mock
    private SensitiveDataChangeRepository changeRepository;
    @Mock
    private ProfileCompletenessService completenessService;
    @Mock
    private SensitiveChangeRecorder changeRecorder;

    private PatientProfileService service;
    private PatientProfile profile;

    @BeforeEach
    void setUp() {
        service = new PatientProfileService(currentPatient, profileLoader, allergyRepository, measurementRepository,
                contactRepository, changeRepository, completenessService, changeRecorder);
        profile = new PatientProfile(PATIENT_ID);
        when(currentPatient.get()).thenReturn(new User() {
            @Override
            public UUID getId() {
                return PATIENT_ID;
            }
        });
        when(profileLoader.getOrCreate(PATIENT_ID)).thenReturn(profile);
        when(allergyRepository.findByPatientIdOrderByCreatedAtAsc(PATIENT_ID)).thenReturn(List.of());
        when(contactRepository.findByPatientIdOrderByContactOrderAsc(PATIENT_ID)).thenReturn(List.of());
        when(measurementRepository.findFirstByPatientIdAndWeightKgIsNotNullOrderByMeasuredAtDesc(PATIENT_ID))
                .thenReturn(Optional.empty());
        when(measurementRepository.findFirstByPatientIdAndHeightMIsNotNullOrderByMeasuredAtDesc(PATIENT_ID))
                .thenReturn(Optional.empty());
        when(completenessService.evaluate(eq(PATIENT_ID), any(AllergyStatus.class)))
                .thenReturn(new CompletenessResponse(false, List.of()));
    }

    @Test
    void newProfileStartsWithAllergiesNotInformedAndDiseaseNotValidated() {
        var response = service.get();

        assertThat(response.allergyStatus()).isEqualTo(AllergyStatus.NOT_INFORMED);
        assertThat(response.declaredDiseaseValidated()).isFalse();
        assertThat(response.weightKg()).isNull();
    }

    @Test
    void changingTheDeclaredDiseaseKeepsItNotValidatedAndRecordsTheChange() {
        profile.setDeclaredDiseaseValidated(true);

        var response = service.update(new UpdatePatientProfileRequest(BloodType.O_POSITIVE, "  Alzheimer  inicial "));

        assertThat(response.declaredDisease()).isEqualTo("Alzheimer inicial");
        assertThat(response.declaredDiseaseValidated()).isFalse();
        assertThat(response.bloodType()).isEqualTo(BloodType.O_POSITIVE);
        verify(changeRecorder).record(eq(PATIENT_ID), eq(SensitiveField.DECLARED_DISEASE), eq(ChangeType.UPDATED),
                anyString(), eq(PATIENT_ID));
    }

    @Test
    void sendingTheSameDiseaseAgainRecordsNothing() {
        profile.setDeclaredDisease("Alzheimer");
        profile.setDeclaredDiseaseValidated(true);

        service.update(new UpdatePatientProfileRequest(null, "Alzheimer"));

        assertThat(profile.isDeclaredDiseaseValidated()).isTrue();
        verify(changeRecorder, never()).record(any(), any(), any(), anyString(), any());
    }

    @Test
    void bloodTypeAloneIsNotASensitiveChange() {
        service.update(new UpdatePatientProfileRequest(BloodType.AB_NEGATIVE, null));

        assertThat(profile.getBloodType()).isEqualTo(BloodType.AB_NEGATIVE);
        verify(changeRecorder, never()).record(any(), any(), any(), anyString(), any());
    }
}
