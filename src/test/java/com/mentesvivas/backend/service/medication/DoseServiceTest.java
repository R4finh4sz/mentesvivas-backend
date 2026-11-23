package com.mentesvivas.backend.service.medication;

import com.mentesvivas.backend.config.DoseProperties;
import com.mentesvivas.backend.dto.medication.CorrectDoseRequest;
import com.mentesvivas.backend.dto.medication.RecordDoseRequest;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.medication.DoseCorrection;
import com.mentesvivas.backend.entity.medication.DoseRecord;
import com.mentesvivas.backend.entity.medication.DoseStatus;
import com.mentesvivas.backend.entity.medication.Medication;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.medication.DoseCorrectionRepository;
import com.mentesvivas.backend.repository.medication.DoseRecordRepository;
import com.mentesvivas.backend.repository.medication.MedicationRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import com.mentesvivas.backend.service.patient.PatientCareAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
class DoseServiceTest {

    private static final UUID PATIENT = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");
    private static final Instant SLOT = Instant.parse("2026-11-23T08:00:00Z");

    @Mock
    private PatientCareAccess access;
    @Mock
    private MedicationRepository medicationRepository;
    @Mock
    private DoseRecordRepository repository;
    @Mock
    private DoseCorrectionRepository correctionRepository;
    @Mock
    private AccessLog accessLog;

    private DoseService service;
    private User author;
    private Medication medication;

    @BeforeEach
    void setUp() {
        service = new DoseService(access, medicationRepository, repository, correctionRepository,
                new DoseStockEffect(), new DoseProperties(3), accessLog, Clock.fixed(NOW, ZoneOffset.UTC));
        author = new User();
        author.setId(UUID.randomUUID());
        medication = new Medication();
        medication.setId(UUID.randomUUID());
        medication.setActive(true);
        medication.setUnitsPerDose(new BigDecimal("2"));
        medication.setStockQuantity(new BigDecimal("10"));
        lenient().when(access.requireWriter(PATIENT)).thenReturn(author);
        lenient().when(access.requireReader(PATIENT)).thenReturn(author);
        lenient().when(medicationRepository.lockByIdAndPatientId(medication.getId(), PATIENT))
                .thenReturn(Optional.of(medication));
        lenient().when(repository.findFirstByMedicationIdAndScheduledForAndDuplicateFalse(medication.getId(), SLOT))
                .thenReturn(Optional.empty());
        lenient().when(repository.save(any(DoseRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private DoseRecord stored(DoseStatus status, UUID recordedBy, Instant recordedAt, String discounted) {
        DoseRecord dose = new DoseRecord();
        dose.setId(UUID.randomUUID());
        dose.setMedicationId(medication.getId());
        dose.setPatientId(PATIENT);
        dose.setScheduledFor(SLOT);
        dose.setStatus(status);
        dose.setStockDiscounted(new BigDecimal(discounted));
        dose.setRecordedBy(recordedBy);
        dose.setRecordedAt(recordedAt);
        return dose;
    }

    @Test
    void takenDoseIsRecordedAndDiscountsTheStock() {
        var response = service.record(PATIENT, medication.getId(), new RecordDoseRequest(SLOT, DoseStatus.TAKEN));

        assertThat(response.status()).isEqualTo(DoseStatus.TAKEN);
        assertThat(response.duplicate()).isFalse();
        assertThat(response.recordedBy()).isEqualTo(author.getId());
        assertThat(response.recordedAt()).isEqualTo(NOW);
        assertThat(medication.getStockQuantity()).isEqualByComparingTo("8");
    }

    @Test
    void refusedAndPostponedDosesKeepTheStock() {
        service.record(PATIENT, medication.getId(), new RecordDoseRequest(SLOT, DoseStatus.REFUSED));

        assertThat(medication.getStockQuantity()).isEqualByComparingTo("10");
    }

    @Test
    void stockNeverGoesNegative() {
        medication.setStockQuantity(new BigDecimal("1"));

        service.record(PATIENT, medication.getId(), new RecordDoseRequest(SLOT, DoseStatus.TAKEN));

        assertThat(medication.getStockQuantity()).isEqualByComparingTo("0");
        ArgumentCaptor<DoseRecord> saved = ArgumentCaptor.forClass(DoseRecord.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getStockDiscounted()).isEqualByComparingTo("1");
    }

    @Test
    void inactiveMedicationRejectsDoses() {
        medication.setActive(false);

        assertThatThrownBy(() -> service.record(PATIENT, medication.getId(),
                new RecordDoseRequest(SLOT, DoseStatus.TAKEN))).isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void unknownMedicationIsNotFound() {
        UUID id = UUID.randomUUID();
        when(medicationRepository.lockByIdAndPatientId(id, PATIENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.record(PATIENT, id, new RecordDoseRequest(SLOT, DoseStatus.TAKEN)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void sameAuthorWithinTheWindowIsBlockedAsDoubleClick() {
        when(repository.findFirstByMedicationIdAndScheduledForAndDuplicateFalse(medication.getId(), SLOT))
                .thenReturn(Optional.of(stored(DoseStatus.TAKEN, author.getId(), NOW.minusSeconds(2), "2")));

        assertThatThrownBy(() -> service.record(PATIENT, medication.getId(),
                new RecordDoseRequest(SLOT, DoseStatus.TAKEN))).isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any());
        assertThat(medication.getStockQuantity()).isEqualByComparingTo("10");
    }

    @Test
    void anotherAuthorConfirmingTheSameDoseIsSavedAsDuplicateWithoutDiscount() {
        when(repository.findFirstByMedicationIdAndScheduledForAndDuplicateFalse(medication.getId(), SLOT))
                .thenReturn(Optional.of(stored(DoseStatus.TAKEN, UUID.randomUUID(), NOW.minusSeconds(1), "2")));

        var response = service.record(PATIENT, medication.getId(), new RecordDoseRequest(SLOT, DoseStatus.TAKEN));

        assertThat(response.duplicate()).isTrue();
        assertThat(medication.getStockQuantity()).isEqualByComparingTo("10");
    }

    @Test
    void sameAuthorAfterTheWindowIsSavedAsDuplicateWithoutDiscount() {
        when(repository.findFirstByMedicationIdAndScheduledForAndDuplicateFalse(medication.getId(), SLOT))
                .thenReturn(Optional.of(stored(DoseStatus.TAKEN, author.getId(), NOW.minusSeconds(3), "2")));

        var response = service.record(PATIENT, medication.getId(), new RecordDoseRequest(SLOT, DoseStatus.TAKEN));

        assertThat(response.duplicate()).isTrue();
        assertThat(medication.getStockQuantity()).isEqualByComparingTo("10");
    }

    @Test
    void correctingFromTakenRestoresTheStockAndKeepsTheJustification() {
        DoseRecord dose = stored(DoseStatus.TAKEN, author.getId(), NOW.minusSeconds(60), "2");
        medication.setStockQuantity(new BigDecimal("8"));
        when(repository.findByIdAndPatientId(dose.getId(), PATIENT)).thenReturn(Optional.of(dose));

        var response = service.correct(PATIENT, dose.getId(),
                new CorrectDoseRequest(DoseStatus.REFUSED, "  paciente cuspiu  "));

        ArgumentCaptor<DoseCorrection> correction = ArgumentCaptor.forClass(DoseCorrection.class);
        verify(correctionRepository).save(correction.capture());
        assertThat(correction.getValue().getPreviousStatus()).isEqualTo(DoseStatus.TAKEN);
        assertThat(correction.getValue().getNewStatus()).isEqualTo(DoseStatus.REFUSED);
        assertThat(correction.getValue().getJustification()).isEqualTo("paciente cuspiu");
        assertThat(correction.getValue().getCorrectedBy()).isEqualTo(author.getId());
        assertThat(correction.getValue().getCorrectedAt()).isEqualTo(NOW);
        assertThat(response.status()).isEqualTo(DoseStatus.REFUSED);
        assertThat(response.corrected()).isTrue();
        assertThat(medication.getStockQuantity()).isEqualByComparingTo("10");
        assertThat(dose.getStockDiscounted()).isEqualByComparingTo("0");
        verify(repository, never()).delete(any());
    }

    @Test
    void correctingToTakenDiscountsTheStock() {
        DoseRecord dose = stored(DoseStatus.POSTPONED, author.getId(), NOW.minusSeconds(60), "0");
        when(repository.findByIdAndPatientId(dose.getId(), PATIENT)).thenReturn(Optional.of(dose));

        service.correct(PATIENT, dose.getId(), new CorrectDoseRequest(DoseStatus.TAKEN, "tomou depois"));

        assertThat(medication.getStockQuantity()).isEqualByComparingTo("8");
        assertThat(dose.getStockDiscounted()).isEqualByComparingTo("2");
    }

    @Test
    void duplicateDosesAndSameStatusCannotBeCorrected() {
        DoseRecord duplicate = stored(DoseStatus.TAKEN, author.getId(), NOW, "0");
        duplicate.setDuplicate(true);
        DoseRecord same = stored(DoseStatus.TAKEN, author.getId(), NOW, "2");
        when(repository.findByIdAndPatientId(duplicate.getId(), PATIENT)).thenReturn(Optional.of(duplicate));
        when(repository.findByIdAndPatientId(same.getId(), PATIENT)).thenReturn(Optional.of(same));

        assertThatThrownBy(() -> service.correct(PATIENT, duplicate.getId(),
                new CorrectDoseRequest(DoseStatus.REFUSED, "x"))).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.correct(PATIENT, same.getId(),
                new CorrectDoseRequest(DoseStatus.TAKEN, "x"))).isInstanceOf(ConflictException.class);
        verify(correctionRepository, never()).save(any());
    }

    @Test
    void correctingAnotherPatientsDoseIsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndPatientId(id, PATIENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.correct(PATIENT, id, new CorrectDoseRequest(DoseStatus.REFUSED, "x")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listLogsTheAccessAndReturnsThePeriod() {
        Instant from = NOW.minusSeconds(3600);
        when(repository.findByPatientIdAndScheduledForBetweenOrderByScheduledForDesc(PATIENT, from, NOW))
                .thenReturn(List.of(stored(DoseStatus.TAKEN, author.getId(), NOW, "2")));

        assertThat(service.list(PATIENT, from, NOW)).hasSize(1);
        verify(accessLog).record(author.getId(), PATIENT, PatientDataArea.MEDICATIONS);
    }
}
