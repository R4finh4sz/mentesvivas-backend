package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.dto.patient.RecordMeasurementRequest;
import com.mentesvivas.backend.entity.patient.BodyMeasurement;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.patient.ValueNeedsConfirmationException;
import com.mentesvivas.backend.repository.patient.BodyMeasurementRepository;
import com.mentesvivas.backend.security.CurrentPatient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeasurementServiceTest {

    private static final UUID PATIENT_ID = UUID.randomUUID();

    @Mock
    private CurrentPatient currentPatient;
    @Mock
    private BodyMeasurementRepository repository;
    @Mock
    private MeasurementValidator validator;

    private MeasurementService service;

    @BeforeEach
    void setUp() {
        service = new MeasurementService(currentPatient, repository, validator);
        when(currentPatient.get()).thenReturn(new User() {
            @Override
            public UUID getId() {
                return PATIENT_ID;
            }
        });
    }

    @Test
    void roundsValuesValidatesThemAndSavesWithAuthorship() {
        when(repository.save(any(BodyMeasurement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.record(new RecordMeasurementRequest(new BigDecimal("72.46"), new BigDecimal("1.678"), false));

        assertThat(response.weightKg()).isEqualByComparingTo("72.5");
        assertThat(response.heightM()).isEqualByComparingTo("1.68");
        verify(validator).validateWeight(new BigDecimal("72.5"), false);
        verify(validator).validateHeight(new BigDecimal("1.68"), false);
        ArgumentCaptor<BodyMeasurement> saved = ArgumentCaptor.forClass(BodyMeasurement.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(saved.getValue().getRecordedBy()).isEqualTo(PATIENT_ID);
        assertThat(saved.getValue().getMeasuredAt()).isNotNull();
    }

    @Test
    void onlyTheInformedValueIsValidated() {
        when(repository.save(any(BodyMeasurement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.record(new RecordMeasurementRequest(new BigDecimal("80"), null, true));

        assertThat(response.heightM()).isNull();
        verify(validator).validateWeight(new BigDecimal("80.0"), true);
        verify(validator, never()).validateHeight(any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void invalidValueIsNotSaved() {
        doThrow(new ValueNeedsConfirmationException("confirme")).when(validator).validateWeight(any(), org.mockito.ArgumentMatchers.anyBoolean());

        assertThatThrownBy(() -> service.record(new RecordMeasurementRequest(new BigDecimal("25"), null, false)))
                .isInstanceOf(ValueNeedsConfirmationException.class);

        verify(repository, never()).save(any());
    }
}
