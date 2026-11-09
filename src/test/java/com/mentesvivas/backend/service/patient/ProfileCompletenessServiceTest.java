package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.entity.patient.AllergyStatus;
import com.mentesvivas.backend.entity.patient.BodyMeasurement;
import com.mentesvivas.backend.entity.patient.MissingItem;
import com.mentesvivas.backend.repository.patient.BodyMeasurementRepository;
import com.mentesvivas.backend.repository.patient.EmergencyContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ProfileCompletenessServiceTest {

    private static final UUID PATIENT = UUID.randomUUID();

    @Mock
    private BodyMeasurementRepository measurementRepository;
    @Mock
    private EmergencyContactRepository contactRepository;

    private ProfileCompletenessService service;

    @BeforeEach
    void setUp() {
        service = new ProfileCompletenessService(measurementRepository, contactRepository);
    }

    private void withWeight(boolean present) {
        lenient().when(measurementRepository.findFirstByPatientIdAndWeightKgIsNotNullOrderByMeasuredAtDesc(PATIENT))
                .thenReturn(present ? Optional.of(new BodyMeasurement()) : Optional.empty());
    }

    private void withHeight(boolean present) {
        lenient().when(measurementRepository.findFirstByPatientIdAndHeightMIsNotNullOrderByMeasuredAtDesc(PATIENT))
                .thenReturn(present ? Optional.of(new BodyMeasurement()) : Optional.empty());
    }

    private void withContacts(long count) {
        lenient().when(contactRepository.countByPatientId(PATIENT)).thenReturn(count);
    }

    @Test
    void nothingInformedListsEverythingAndKeepsModulesLocked() {
        withWeight(false);
        withHeight(false);
        withContacts(0);

        var result = service.evaluate(PATIENT, AllergyStatus.NOT_INFORMED);

        assertThat(result.modulesUnlocked()).isFalse();
        assertThat(result.missing()).containsExactly(MissingItem.ALLERGY_STATUS, MissingItem.WEIGHT,
                MissingItem.HEIGHT, MissingItem.EMERGENCY_CONTACTS);
    }

    @Test
    void noKnownAllergiesCountsAsInformed() {
        withWeight(true);
        withHeight(true);
        withContacts(2);

        var result = service.evaluate(PATIENT, AllergyStatus.NONE_KNOWN);

        assertThat(result.modulesUnlocked()).isTrue();
        assertThat(result.missing()).isEmpty();
    }

    @Test
    void emergencyContactsDoNotLockTheModulesButAreReported() {
        withWeight(true);
        withHeight(true);
        withContacts(1);

        var result = service.evaluate(PATIENT, AllergyStatus.INFORMED);

        assertThat(result.modulesUnlocked()).isTrue();
        assertThat(result.missing()).containsExactly(MissingItem.EMERGENCY_CONTACTS);
    }

    @Test
    void missingWeightOrHeightLocksTheModules() {
        withWeight(false);
        withHeight(true);
        withContacts(2);

        var result = service.evaluate(PATIENT, AllergyStatus.INFORMED);

        assertThat(result.modulesUnlocked()).isFalse();
        assertThat(result.missing()).containsExactly(MissingItem.WEIGHT);
    }
}
