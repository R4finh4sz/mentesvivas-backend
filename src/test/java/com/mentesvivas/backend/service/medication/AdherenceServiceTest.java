package com.mentesvivas.backend.service.medication;

import com.mentesvivas.backend.config.AdherenceProperties;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.medication.DoseRecord;
import com.mentesvivas.backend.entity.medication.DoseStatus;
import com.mentesvivas.backend.entity.medication.Medication;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.repository.medication.DoseRecordRepository;
import com.mentesvivas.backend.repository.medication.MedicationRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import com.mentesvivas.backend.service.patient.PatientCareAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdherenceServiceTest {

    private static final UUID PATIENT = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-30T12:00:00Z");

    @Mock
    private PatientCareAccess access;
    @Mock
    private MedicationRepository medicationRepository;
    @Mock
    private DoseRecordRepository doseRepository;
    @Mock
    private AccessLog accessLog;

    private AdherenceService service;
    private User reader;

    @BeforeEach
    void setUp() {
        AdherenceProperties properties = new AdherenceProperties(30, 70);
        service = new AdherenceService(access, medicationRepository, doseRepository,
                new AdherenceCalculator(properties), properties, accessLog, Clock.fixed(NOW, ZoneOffset.UTC));
        reader = new User();
        reader.setId(UUID.randomUUID());
        when(access.requireReader(PATIENT)).thenReturn(reader);
    }

    private DoseRecord dose(UUID medicationId, DoseStatus status, boolean duplicate) {
        DoseRecord dose = new DoseRecord();
        dose.setMedicationId(medicationId);
        dose.setStatus(status);
        dose.setDuplicate(duplicate);
        return dose;
    }

    @Test
    void usesThePeriodFromTheConfigurationAndLogsTheAccess() {
        when(medicationRepository.findByPatientIdAndActiveTrueOrderByNameAsc(PATIENT)).thenReturn(List.of());
        when(doseRepository.findByPatientIdAndScheduledForBetweenOrderByScheduledForDesc(PATIENT,
                NOW.minusSeconds(30L * 24 * 3600), NOW)).thenReturn(List.of());

        var result = service.calculate(PATIENT);

        assertThat(result.to()).isEqualTo(NOW);
        assertThat(result.from()).isEqualTo(NOW.minusSeconds(30L * 24 * 3600));
        assertThat(result.thresholdPercent()).isEqualTo(70);
        verify(accessLog).record(reader.getId(), PATIENT, PatientDataArea.MEDICATIONS);
    }

    @Test
    void ignoresDuplicatesAndDosesOfInactiveMedications() {
        Medication active = new Medication();
        active.setId(UUID.randomUUID());
        active.setCreatedAt(NOW.minusSeconds(40L * 24 * 3600));
        active.setTimes(List.of(LocalTime.of(8, 0)));
        when(medicationRepository.findByPatientIdAndActiveTrueOrderByNameAsc(PATIENT)).thenReturn(List.of(active));
        when(doseRepository.findByPatientIdAndScheduledForBetweenOrderByScheduledForDesc(PATIENT,
                NOW.minusSeconds(30L * 24 * 3600), NOW)).thenReturn(List.of(
                dose(active.getId(), DoseStatus.TAKEN, false),
                dose(active.getId(), DoseStatus.TAKEN, true),
                dose(UUID.randomUUID(), DoseStatus.TAKEN, false)));

        var result = service.calculate(PATIENT);

        assertThat(result.taken()).isEqualTo(1);
    }
}
