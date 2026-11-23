package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.dto.patient.RecordVitalSignRequest;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.patient.GlucoseType;
import com.mentesvivas.backend.entity.patient.MeasurementSource;
import com.mentesvivas.backend.entity.patient.VitalSign;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import com.mentesvivas.backend.repository.patient.VitalSignRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VitalSignServiceTest {

    private static final UUID PATIENT = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");

    @Mock
    private VitalSignAccess access;
    @Mock
    private VitalSignRepository repository;
    @Mock
    private VitalSignValidator validator;
    @Mock
    private AccessLog accessLog;

    private VitalSignService service;
    private User author;

    @BeforeEach
    void setUp() {
        service = new VitalSignService(access, repository, validator, accessLog, Clock.fixed(NOW, ZoneOffset.UTC));
        author = new User();
        author.setId(UUID.randomUUID());
    }

    private RecordVitalSignRequest request(MeasurementSource source) {
        return new RecordVitalSignRequest(120, 80, 72, 95, GlucoseType.FASTING, new BigDecimal("36.54"), 98,
                "tudo bem", source);
    }

    @Test
    void recordsWithAuthorshipAndRoundedTemperature() {
        when(access.requireWriter(PATIENT)).thenReturn(author);
        when(repository.save(any(VitalSign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.record(PATIENT, request(MeasurementSource.DICTATED));

        ArgumentCaptor<VitalSign> saved = ArgumentCaptor.forClass(VitalSign.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getPatientId()).isEqualTo(PATIENT);
        assertThat(saved.getValue().getRecordedBy()).isEqualTo(author.getId());
        assertThat(saved.getValue().getMeasuredAt()).isEqualTo(NOW);
        assertThat(saved.getValue().getGlucoseType()).isEqualTo(GlucoseType.FASTING);
        assertThat(response.temperature()).isEqualByComparingTo("36.5");
        assertThat(response.source()).isEqualTo(MeasurementSource.DICTATED);
        assertThat(response.checkAgainstOriginal()).isFalse();
    }

    @Test
    void sourceDefaultsToTyped() {
        when(access.requireWriter(PATIENT)).thenReturn(author);
        when(repository.save(any(VitalSign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.record(PATIENT, request(null)).source()).isEqualTo(MeasurementSource.TYPED);
    }

    @Test
    void ocrValuesAreFlaggedToCheckAgainstTheOriginal() {
        when(access.requireWriter(PATIENT)).thenReturn(author);
        when(repository.save(any(VitalSign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.record(PATIENT, request(MeasurementSource.OCR)).checkAgainstOriginal()).isTrue();
    }

    @Test
    void invalidValueIsNotSaved() {
        when(access.requireWriter(PATIENT)).thenReturn(author);
        RecordVitalSignRequest request = request(null);
        doThrow(new ImpossibleValueException("invalido")).when(validator).validate(request);

        assertThatThrownBy(() -> service.record(PATIENT, request)).isInstanceOf(ImpossibleValueException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void historyLogsTheAccessAndReturnsNewestFirst() {
        when(access.requireReader(PATIENT)).thenReturn(author);
        VitalSign sign = new VitalSign();
        sign.setSource(MeasurementSource.TYPED);
        sign.setMeasuredAt(NOW);
        when(repository.findByPatientIdOrderByMeasuredAtDesc(PATIENT)).thenReturn(List.of(sign));

        var history = service.history(PATIENT);

        assertThat(history).hasSize(1);
        verify(accessLog).record(author.getId(), PATIENT, PatientDataArea.MEASUREMENTS);
    }
}
