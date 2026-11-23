package com.mentesvivas.backend.service.medication;

import com.mentesvivas.backend.config.MedicationProperties;
import com.mentesvivas.backend.dto.medication.AdjustStockRequest;
import com.mentesvivas.backend.dto.medication.CreateMedicationRequest;
import com.mentesvivas.backend.dto.medication.MedicationWarning;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.medication.Medication;
import com.mentesvivas.backend.entity.medication.MedicationOrigin;
import com.mentesvivas.backend.entity.medication.StockAdjustment;
import com.mentesvivas.backend.entity.patient.Allergy;
import com.mentesvivas.backend.entity.patient.AllergyCategory;
import com.mentesvivas.backend.entity.patient.AllergyStatus;
import com.mentesvivas.backend.entity.patient.PatientProfile;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.exception.patient.ValueNeedsConfirmationException;
import com.mentesvivas.backend.repository.medication.MedicationRepository;
import com.mentesvivas.backend.repository.medication.StockAdjustmentRepository;
import com.mentesvivas.backend.repository.patient.AllergyRepository;
import com.mentesvivas.backend.repository.patient.PatientProfileRepository;
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
import java.time.LocalTime;
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
class MedicationServiceTest {

    private static final UUID PATIENT = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");

    @Mock
    private PatientCareAccess access;
    @Mock
    private MedicationRepository repository;
    @Mock
    private StockAdjustmentRepository adjustmentRepository;
    @Mock
    private AllergyRepository allergyRepository;
    @Mock
    private PatientProfileRepository profileRepository;
    @Mock
    private AccessLog accessLog;

    private MedicationService service;
    private User author;

    @BeforeEach
    void setUp() {
        service = new MedicationService(access, repository, adjustmentRepository, allergyRepository,
                profileRepository, new MedicationAllergyChecker(),
                new MedicationResponseFactory(new MedicationStockCalculator(new MedicationProperties(5))),
                accessLog, Clock.fixed(NOW, ZoneOffset.UTC));
        author = new User();
        author.setId(UUID.randomUUID());
        lenient().when(access.requireWriter(PATIENT)).thenReturn(author);
        lenient().when(access.requireReader(PATIENT)).thenReturn(author);
        lenient().when(repository.save(any(Medication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(repository.findByPatientIdAndActiveTrueOrderByNameAsc(PATIENT)).thenReturn(List.of());
        lenient().when(allergyRepository.findByPatientIdOrderByCreatedAtAsc(PATIENT)).thenReturn(List.of());
        PatientProfile profile = new PatientProfile(PATIENT);
        profile.setAllergyStatus(AllergyStatus.NONE_KNOWN);
        lenient().when(profileRepository.findById(PATIENT)).thenReturn(Optional.of(profile));
    }

    private CreateMedicationRequest request(String name, Boolean confirm) {
        return new CreateMedicationRequest(name, "50 mg", new BigDecimal("1"),
                List.of(LocalTime.of(20, 0), LocalTime.of(8, 0), LocalTime.of(8, 0)), new BigDecimal("30"), confirm);
    }

    private Medication existing(String name, String strength) {
        Medication medication = new Medication();
        medication.setId(UUID.randomUUID());
        medication.setName(name);
        medication.setStrength(strength);
        medication.setUnitsPerDose(BigDecimal.ONE);
        medication.setStockQuantity(new BigDecimal("10"));
        medication.setTimes(List.of(LocalTime.NOON));
        return medication;
    }

    @Test
    void createsAManualMedicationWithSortedDistinctTimes() {
        var response = service.create(PATIENT, request("Losartana", null));

        ArgumentCaptor<Medication> saved = ArgumentCaptor.forClass(Medication.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getOrigin()).isEqualTo(MedicationOrigin.MANUAL);
        assertThat(saved.getValue().getCreatedBy()).isEqualTo(author.getId());
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(saved.getValue().isActive()).isTrue();
        assertThat(response.times()).containsExactly(LocalTime.of(8, 0), LocalTime.of(20, 0));
        assertThat(response.stockDaysLeft()).isEqualTo(15);
        assertThat(response.warnings()).isEmpty();
    }

    @Test
    void allergyMatchNeedsConfirmationAndIsNotSaved() {
        Allergy allergy = new Allergy();
        allergy.setCategory(AllergyCategory.MEDICATION);
        allergy.setDescription("Dipirona");
        when(allergyRepository.findByPatientIdOrderByCreatedAtAsc(PATIENT)).thenReturn(List.of(allergy));

        assertThatThrownBy(() -> service.create(PATIENT, request("Dipirona", null)))
                .isInstanceOf(ValueNeedsConfirmationException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void confirmedAllergyMatchIsSavedWithAWarning() {
        Allergy allergy = new Allergy();
        allergy.setCategory(AllergyCategory.MEDICATION);
        allergy.setDescription("Dipirona");
        when(allergyRepository.findByPatientIdOrderByCreatedAtAsc(PATIENT)).thenReturn(List.of(allergy));

        var response = service.create(PATIENT, request("Dipirona", true));

        assertThat(response.warnings()).containsExactly(MedicationWarning.ALLERGY_MATCH_CONFIRMED);
    }

    @Test
    void notInformedAllergiesAddAWarning() {
        when(profileRepository.findById(PATIENT)).thenReturn(Optional.empty());

        assertThat(service.create(PATIENT, request("Losartana", null)).warnings())
                .containsExactly(MedicationWarning.ALLERGY_NOT_INFORMED);
    }

    @Test
    void duplicateActiveMedicationIsRejected() {
        when(repository.findByPatientIdAndActiveTrueOrderByNameAsc(PATIENT))
                .thenReturn(List.of(existing("losartana", "50 MG")));

        assertThatThrownBy(() -> service.create(PATIENT, request("Losartana", null)))
                .isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void sameNameWithAnotherStrengthIsNotADuplicate() {
        when(repository.findByPatientIdAndActiveTrueOrderByNameAsc(PATIENT))
                .thenReturn(List.of(existing("Losartana", "100 mg")));

        assertThat(service.create(PATIENT, request("Losartana", null)).name()).isEqualTo("Losartana");
    }

    @Test
    void listLogsTheAccessAndHidesInactiveByDefault() {
        when(repository.findByPatientIdAndActiveTrueOrderByNameAsc(PATIENT)).thenReturn(List.of(existing("A", null)));

        assertThat(service.list(PATIENT, false)).hasSize(1);
        verify(accessLog).record(author.getId(), PATIENT, PatientDataArea.MEDICATIONS);
        verify(repository, never()).findByPatientIdOrderByNameAsc(PATIENT);
    }

    @Test
    void listCanIncludeInactiveMedications() {
        when(repository.findByPatientIdOrderByNameAsc(PATIENT)).thenReturn(List.of());

        assertThat(service.list(PATIENT, true)).isEmpty();
    }

    @Test
    void stockAdjustmentKeepsTheJustifiedHistory() {
        Medication medication = existing("Losartana", null);
        when(repository.findByIdAndPatientId(medication.getId(), PATIENT)).thenReturn(Optional.of(medication));

        var response = service.adjustStock(PATIENT, medication.getId(),
                new AdjustStockRequest(new BigDecimal("40"), "  contagem conferida  "));

        ArgumentCaptor<StockAdjustment> adjustment = ArgumentCaptor.forClass(StockAdjustment.class);
        verify(adjustmentRepository).save(adjustment.capture());
        assertThat(adjustment.getValue().getPreviousQuantity()).isEqualByComparingTo("10");
        assertThat(adjustment.getValue().getNewQuantity()).isEqualByComparingTo("40");
        assertThat(adjustment.getValue().getJustification()).isEqualTo("contagem conferida");
        assertThat(adjustment.getValue().getAdjustedBy()).isEqualTo(author.getId());
        assertThat(adjustment.getValue().getAdjustedAt()).isEqualTo(NOW);
        assertThat(response.stockQuantity()).isEqualByComparingTo("40");
    }

    @Test
    void adjustingAnotherPatientsMedicationIsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndPatientId(id, PATIENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.adjustStock(PATIENT, id, new AdjustStockRequest(BigDecimal.ONE, "x")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(adjustmentRepository, never()).save(any());
    }

    @Test
    void deactivatingKeepsTheRecord() {
        Medication medication = existing("Losartana", null);
        medication.setActive(true);
        when(repository.findByIdAndPatientId(medication.getId(), PATIENT)).thenReturn(Optional.of(medication));

        service.deactivate(PATIENT, medication.getId());

        assertThat(medication.isActive()).isFalse();
        verify(repository, never()).delete(any());
    }
}
