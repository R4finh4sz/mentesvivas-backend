package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.dto.patient.AddAllergyRequest;
import com.mentesvivas.backend.dto.patient.SetAllergyStatusRequest;
import com.mentesvivas.backend.entity.patient.Allergy;
import com.mentesvivas.backend.entity.patient.AllergyCategory;
import com.mentesvivas.backend.entity.patient.AllergySeverity;
import com.mentesvivas.backend.entity.patient.AllergyStatus;
import com.mentesvivas.backend.entity.patient.ChangeType;
import com.mentesvivas.backend.entity.patient.PatientProfile;
import com.mentesvivas.backend.entity.patient.SensitiveField;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.repository.patient.AllergyRepository;
import com.mentesvivas.backend.security.CurrentPatient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllergyServiceTest {

    private static final UUID PATIENT_ID = UUID.randomUUID();

    @Mock
    private CurrentPatient currentPatient;
    @Mock
    private AllergyRepository allergyRepository;
    @Mock
    private PatientProfileLoader profileLoader;
    @Mock
    private SensitiveChangeRecorder changeRecorder;

    private AllergyService service;
    private PatientProfile profile;

    @BeforeEach
    void setUp() {
        service = new AllergyService(currentPatient, allergyRepository, profileLoader, changeRecorder);
        User patient = new User() {
            @Override
            public UUID getId() {
                return PATIENT_ID;
            }
        };
        profile = new PatientProfile(PATIENT_ID);
        when(currentPatient.get()).thenReturn(patient);
    }

    @Test
    void addingAnAllergyMarksTheStatusAsInformedAndRecordsTheChange() {
        when(profileLoader.getOrCreate(PATIENT_ID)).thenReturn(profile);
        when(allergyRepository.save(any(Allergy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.add(new AddAllergyRequest(AllergyCategory.MEDICATION, "  Dipirona ", AllergySeverity.SEVERE));

        assertThat(response.description()).isEqualTo("Dipirona");
        assertThat(profile.getAllergyStatus()).isEqualTo(AllergyStatus.INFORMED);
        ArgumentCaptor<Allergy> saved = ArgumentCaptor.forClass(Allergy.class);
        verify(allergyRepository).save(saved.capture());
        assertThat(saved.getValue().getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(saved.getValue().getCreatedBy()).isEqualTo(PATIENT_ID);
        verify(changeRecorder).record(eq(PATIENT_ID), eq(SensitiveField.ALLERGY), eq(ChangeType.CREATED),
                eq("Alergia adicionada: MEDICATION (SEVERE)"), eq(PATIENT_ID));
    }

    @Test
    void statusCannotBeSetWhileAllergiesExist() {
        when(allergyRepository.existsByPatientId(PATIENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.setStatus(new SetAllergyStatusRequest(AllergyStatus.NONE_KNOWN)))
                .isInstanceOf(ConflictException.class);

        verify(changeRecorder, never()).record(any(), any(), any(), anyString(), any());
    }

    @Test
    void settingNoKnownAllergiesIsRecorded() {
        when(allergyRepository.existsByPatientId(PATIENT_ID)).thenReturn(false);
        when(profileLoader.getOrCreate(PATIENT_ID)).thenReturn(profile);

        var status = service.setStatus(new SetAllergyStatusRequest(AllergyStatus.NONE_KNOWN));

        assertThat(status).isEqualTo(AllergyStatus.NONE_KNOWN);
        verify(changeRecorder).record(eq(PATIENT_ID), eq(SensitiveField.ALLERGY_STATUS), eq(ChangeType.UPDATED),
                anyString(), eq(PATIENT_ID));
    }

    @Test
    void settingTheSameStatusAgainRecordsNothing() {
        when(allergyRepository.existsByPatientId(PATIENT_ID)).thenReturn(false);
        when(profileLoader.getOrCreate(PATIENT_ID)).thenReturn(profile);

        service.setStatus(new SetAllergyStatusRequest(AllergyStatus.NOT_INFORMED));

        verify(changeRecorder, never()).record(any(), any(), any(), anyString(), any());
    }
}
