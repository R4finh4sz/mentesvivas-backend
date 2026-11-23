package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.config.HydrationProperties;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.patient.BodyMeasurement;
import com.mentesvivas.backend.entity.patient.PatientProfile;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.repository.patient.BodyMeasurementRepository;
import com.mentesvivas.backend.repository.patient.PatientProfileRepository;
import com.mentesvivas.backend.repository.patient.RoutineRecordRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import com.mentesvivas.backend.service.patient.hydration.HydrationConditionClassifier;
import com.mentesvivas.backend.service.patient.hydration.HydrationGoalCalculator;
import com.mentesvivas.backend.service.patient.hydration.HydrationGoalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HydrationServiceTest {

    private static final UUID PATIENT = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-23T01:30:00Z");

    @Mock
    private PatientCareAccess access;
    @Mock
    private PatientProfileRepository profileRepository;
    @Mock
    private BodyMeasurementRepository measurementRepository;
    @Mock
    private RoutineRecordRepository routineRepository;
    @Mock
    private AccessLog accessLog;

    private HydrationService service;
    private User reader;

    @BeforeEach
    void setUp() {
        HydrationProperties properties = new HydrationProperties(new BigDecimal("25"), new BigDecimal("30"),
                ZoneId.of("America/Sao_Paulo"), List.of("renal"), List.of("cardi"), List.of("hipertens"));
        service = new HydrationService(access, profileRepository, measurementRepository, routineRepository,
                new HydrationGoalCalculator(properties, new HydrationConditionClassifier(properties)), properties,
                accessLog, Clock.fixed(NOW, ZoneOffset.UTC));
        reader = new User();
        reader.setId(UUID.randomUUID());
        when(access.requireReader(PATIENT)).thenReturn(reader);
    }

    private void weight(String kg) {
        BodyMeasurement measurement = new BodyMeasurement();
        measurement.setWeightKg(new BigDecimal(kg));
        when(measurementRepository.findFirstByPatientIdAndWeightKgIsNotNullOrderByMeasuredAtDesc(PATIENT))
                .thenReturn(Optional.of(measurement));
    }

    @Test
    void summarizesGoalConsumptionAndRemaining() {
        weight("80");
        when(profileRepository.findById(PATIENT)).thenReturn(Optional.of(new PatientProfile(PATIENT)));
        when(routineRepository.sumHydrationMl(eq(PATIENT), any(), any())).thenReturn(500L);

        var response = service.today(PATIENT);

        assertThat(response.goalStatus()).isEqualTo(HydrationGoalStatus.FORMULA);
        assertThat(response.goalMinMl()).isEqualTo(2000);
        assertThat(response.goalMaxMl()).isEqualTo(2400);
        assertThat(response.consumedTodayMl()).isEqualTo(500);
        assertThat(response.remainingMinMl()).isEqualTo(1500);
        assertThat(response.remainingMaxMl()).isEqualTo(1900);
        verify(accessLog).record(reader.getId(), PATIENT, PatientDataArea.ROUTINE);
    }

    @Test
    void remainingNeverGoesBelowZero() {
        weight("80");
        when(profileRepository.findById(PATIENT)).thenReturn(Optional.empty());
        when(routineRepository.sumHydrationMl(eq(PATIENT), any(), any())).thenReturn(5000L);

        var response = service.today(PATIENT);

        assertThat(response.remainingMinMl()).isZero();
        assertThat(response.remainingMaxMl()).isZero();
    }

    @Test
    void usesTheLocalDayOfThePatient() {
        when(profileRepository.findById(PATIENT)).thenReturn(Optional.empty());
        when(measurementRepository.findFirstByPatientIdAndWeightKgIsNotNullOrderByMeasuredAtDesc(PATIENT))
                .thenReturn(Optional.empty());
        when(routineRepository.sumHydrationMl(eq(PATIENT), any(), any())).thenReturn(0L);

        var response = service.today(PATIENT);

        ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);
        verify(routineRepository).sumHydrationMl(eq(PATIENT), from.capture(), to.capture());
        assertThat(from.getValue()).isEqualTo(Instant.parse("2026-11-22T03:00:00Z"));
        assertThat(to.getValue()).isEqualTo(Instant.parse("2026-11-23T03:00:00Z"));
        assertThat(response.goalStatus()).isEqualTo(HydrationGoalStatus.WEIGHT_MISSING);
        assertThat(response.remainingMinMl()).isNull();
    }

    @Test
    void restrictedConditionWithoutDoctorGoalHasNoNumbers() {
        weight("80");
        PatientProfile profile = new PatientProfile(PATIENT);
        profile.setDeclaredDisease("Doenca renal");
        when(profileRepository.findById(PATIENT)).thenReturn(Optional.of(profile));
        when(routineRepository.sumHydrationMl(eq(PATIENT), any(), any())).thenReturn(300L);

        var response = service.today(PATIENT);

        assertThat(response.goalStatus()).isEqualTo(HydrationGoalStatus.DOCTOR_GOAL_REQUIRED);
        assertThat(response.goalMinMl()).isNull();
        assertThat(response.consumedTodayMl()).isEqualTo(300);
        assertThat(response.conditionValidated()).isFalse();
    }

    @Test
    void doctorGoalIsUsedAndTheValidatedFlagIsExposed() {
        weight("80");
        PatientProfile profile = new PatientProfile(PATIENT);
        profile.setDeclaredDisease("Doenca renal");
        profile.setDeclaredDiseaseValidated(true);
        profile.setHydrationGoalMl(1200);
        when(profileRepository.findById(PATIENT)).thenReturn(Optional.of(profile));
        when(routineRepository.sumHydrationMl(eq(PATIENT), any(), any())).thenReturn(200L);

        var response = service.today(PATIENT);

        assertThat(response.goalStatus()).isEqualTo(HydrationGoalStatus.DOCTOR_DEFINED);
        assertThat(response.remainingMinMl()).isEqualTo(1000);
        assertThat(response.conditionValidated()).isTrue();
    }
}
