package com.mentesvivas.backend.service.clinical;

import com.mentesvivas.backend.dto.patient.CareTeamResponse;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.patient.Allergy;
import com.mentesvivas.backend.entity.patient.AllergyCategory;
import com.mentesvivas.backend.entity.patient.AllergySeverity;
import com.mentesvivas.backend.entity.patient.AllergyStatus;
import com.mentesvivas.backend.entity.patient.BloodType;
import com.mentesvivas.backend.entity.patient.BodyMeasurement;
import com.mentesvivas.backend.entity.patient.PatientProfile;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.patient.AllergyRepository;
import com.mentesvivas.backend.repository.patient.BodyMeasurementRepository;
import com.mentesvivas.backend.repository.patient.EmergencyContactRepository;
import com.mentesvivas.backend.repository.patient.PatientProfileRepository;
import com.mentesvivas.backend.repository.user.UserRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import com.mentesvivas.backend.service.patient.CareTeamLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicalSummaryServiceTest {

    private static final UUID PATIENT = UUID.randomUUID();

    @Mock
    private DoctorPatientAccess access;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PatientProfileRepository profileRepository;
    @Mock
    private AllergyRepository allergyRepository;
    @Mock
    private BodyMeasurementRepository measurementRepository;
    @Mock
    private EmergencyContactRepository contactRepository;
    @Mock
    private CareTeamLoader careTeamLoader;
    @Mock
    private AccessLog accessLog;

    private ClinicalSummaryService service;
    private User doctor;
    private User patient;

    @BeforeEach
    void setUp() {
        service = new ClinicalSummaryService(access, userRepository, profileRepository, allergyRepository,
                measurementRepository, contactRepository, careTeamLoader, accessLog);
        doctor = new User();
        doctor.setId(UUID.randomUUID());
        patient = new User();
        patient.setId(PATIENT);
        patient.setFullName("Maria Souza");
        patient.setBirthDate(LocalDate.of(1950, 3, 2));
        lenient().when(access.requireDoctorOf(PATIENT)).thenReturn(doctor);
        lenient().when(careTeamLoader.load(PATIENT)).thenReturn(new CareTeamResponse(List.of(), List.of()));
    }

    private void measurement(String weight, String height) {
        BodyMeasurement weightEntry = new BodyMeasurement();
        weightEntry.setWeightKg(new BigDecimal(weight));
        BodyMeasurement heightEntry = new BodyMeasurement();
        heightEntry.setHeightM(new BigDecimal(height));
        when(measurementRepository.findFirstByPatientIdAndWeightKgIsNotNullOrderByMeasuredAtDesc(PATIENT))
                .thenReturn(Optional.of(weightEntry));
        when(measurementRepository.findFirstByPatientIdAndHeightMIsNotNullOrderByMeasuredAtDesc(PATIENT))
                .thenReturn(Optional.of(heightEntry));
    }

    @Test
    void summarizesTheRegistrationAndLogsTheAccess() {
        when(userRepository.findById(PATIENT)).thenReturn(Optional.of(patient));
        PatientProfile profile = new PatientProfile(PATIENT);
        profile.setBloodType(BloodType.O_POSITIVE);
        profile.setDeclaredDisease("Alzheimer");
        profile.setAllergyStatus(AllergyStatus.INFORMED);
        when(profileRepository.findById(PATIENT)).thenReturn(Optional.of(profile));
        Allergy allergy = new Allergy();
        allergy.setCategory(AllergyCategory.MEDICATION);
        allergy.setDescription("Dipirona");
        allergy.setSeverity(AllergySeverity.SEVERE);
        when(allergyRepository.findByPatientIdOrderByCreatedAtAsc(PATIENT)).thenReturn(List.of(allergy));
        when(contactRepository.findByPatientIdOrderByContactOrderAsc(PATIENT)).thenReturn(List.of());
        measurement("72.5", "1.70");

        var summary = service.summarize(PATIENT);

        assertThat(summary.fullName()).isEqualTo("Maria Souza");
        assertThat(summary.bloodType()).isEqualTo(BloodType.O_POSITIVE);
        assertThat(summary.declaredDisease()).isEqualTo("Alzheimer");
        assertThat(summary.declaredDiseaseValidated()).isFalse();
        assertThat(summary.allergyStatus()).isEqualTo(AllergyStatus.INFORMED);
        assertThat(summary.allergies()).hasSize(1);
        assertThat(summary.weightKg()).isEqualByComparingTo("72.5");
        assertThat(summary.bmi()).isNotNull();
        verify(accessLog).record(doctor.getId(), PATIENT, PatientDataArea.PROFILE);
    }

    @Test
    void missingProfileNeverLooksLikeNoAllergies() {
        when(userRepository.findById(PATIENT)).thenReturn(Optional.of(patient));
        when(profileRepository.findById(PATIENT)).thenReturn(Optional.empty());
        when(allergyRepository.findByPatientIdOrderByCreatedAtAsc(PATIENT)).thenReturn(List.of());
        when(contactRepository.findByPatientIdOrderByContactOrderAsc(PATIENT)).thenReturn(List.of());
        when(measurementRepository.findFirstByPatientIdAndWeightKgIsNotNullOrderByMeasuredAtDesc(PATIENT))
                .thenReturn(Optional.empty());
        when(measurementRepository.findFirstByPatientIdAndHeightMIsNotNullOrderByMeasuredAtDesc(PATIENT))
                .thenReturn(Optional.empty());

        var summary = service.summarize(PATIENT);

        assertThat(summary.allergyStatus()).isEqualTo(AllergyStatus.NOT_INFORMED);
        assertThat(summary.declaredDisease()).isNull();
        assertThat(summary.declaredDiseaseValidated()).isFalse();
        assertThat(summary.weightKg()).isNull();
        assertThat(summary.bmi()).isNull();
    }

    @Test
    void validatedDiseaseIsFlagged() {
        when(userRepository.findById(PATIENT)).thenReturn(Optional.of(patient));
        PatientProfile profile = new PatientProfile(PATIENT);
        profile.setDeclaredDisease("Alzheimer");
        profile.setDeclaredDiseaseValidated(true);
        when(profileRepository.findById(PATIENT)).thenReturn(Optional.of(profile));
        when(allergyRepository.findByPatientIdOrderByCreatedAtAsc(PATIENT)).thenReturn(List.of());
        when(contactRepository.findByPatientIdOrderByContactOrderAsc(PATIENT)).thenReturn(List.of());
        when(measurementRepository.findFirstByPatientIdAndWeightKgIsNotNullOrderByMeasuredAtDesc(PATIENT))
                .thenReturn(Optional.empty());
        when(measurementRepository.findFirstByPatientIdAndHeightMIsNotNullOrderByMeasuredAtDesc(PATIENT))
                .thenReturn(Optional.empty());

        assertThat(service.summarize(PATIENT).declaredDiseaseValidated()).isTrue();
    }

    @Test
    void unknownPatientIsNotFound() {
        when(userRepository.findById(PATIENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.summarize(PATIENT)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void doctorWithoutLinkIsDeniedBeforeAnyRead() {
        UUID other = UUID.randomUUID();
        when(access.requireDoctorOf(other)).thenThrow(new AccessDeniedException("sem vinculo"));

        assertThatThrownBy(() -> service.summarize(other)).isInstanceOf(AccessDeniedException.class);
        verify(userRepository, never()).findById(any());
        verify(accessLog, never()).record(any(), any(), any());
    }
}
