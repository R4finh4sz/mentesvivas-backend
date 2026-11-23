package com.mentesvivas.backend.service.clinical;

import com.mentesvivas.backend.dto.clinical.TimelineCategory;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.patient.MeasurementSource;
import com.mentesvivas.backend.entity.patient.RoutineRecord;
import com.mentesvivas.backend.entity.patient.RoutineType;
import com.mentesvivas.backend.entity.patient.VitalSign;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.repository.patient.BodyMeasurementRepository;
import com.mentesvivas.backend.repository.patient.OccurrenceRepository;
import com.mentesvivas.backend.repository.patient.RoutineRecordRepository;
import com.mentesvivas.backend.repository.patient.VitalSignRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import com.mentesvivas.backend.service.clinical.timeline.TimelinePeriod;
import com.mentesvivas.backend.service.clinical.timeline.TimelinePeriodResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicalTimelineServiceTest {

    private static final UUID PATIENT = UUID.randomUUID();
    private static final Instant FROM = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-11-08T00:00:00Z");

    @Mock
    private DoctorPatientAccess access;
    @Mock
    private TimelinePeriodResolver periodResolver;
    @Mock
    private VitalSignRepository vitalSignRepository;
    @Mock
    private RoutineRecordRepository routineRepository;
    @Mock
    private OccurrenceRepository occurrenceRepository;
    @Mock
    private BodyMeasurementRepository measurementRepository;
    @Mock
    private AccessLog accessLog;

    private ClinicalTimelineService service;
    private User doctor;

    @BeforeEach
    void setUp() {
        service = new ClinicalTimelineService(access, periodResolver, vitalSignRepository, routineRepository,
                occurrenceRepository, measurementRepository, accessLog);
        doctor = new User();
        doctor.setId(UUID.randomUUID());
    }

    private void emptyData() {
        when(vitalSignRepository.findByPatientIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(PATIENT, FROM, TO))
                .thenReturn(List.of());
        when(routineRepository.findByPatientIdAndTypeAndOccurredAtBetweenOrderByOccurredAtDesc(
                any(), any(), any(), any())).thenReturn(List.of());
        when(occurrenceRepository.findByPatientIdAndOccurredAtBetweenOrderByOccurredAtDesc(PATIENT, FROM, TO))
                .thenReturn(List.of());
        when(measurementRepository.findByPatientIdAndWeightKgIsNotNullAndMeasuredAtBetweenOrderByMeasuredAtDesc(
                PATIENT, FROM, TO)).thenReturn(List.of());
    }

    @Test
    void periodWithoutDataReportsEveryCategoryAsMissing() {
        when(access.requireDoctorOf(PATIENT)).thenReturn(doctor);
        when(periodResolver.resolve(null, null)).thenReturn(new TimelinePeriod(FROM, TO));
        emptyData();

        var timeline = service.timeline(PATIENT, null, null);

        assertThat(timeline.hasData()).isFalse();
        assertThat(timeline.categoriesWithoutData()).containsExactly(TimelineCategory.values());
        assertThat(timeline.vitalSigns()).isEmpty();
        assertThat(timeline.from()).isEqualTo(FROM);
        assertThat(timeline.to()).isEqualTo(TO);
    }

    @Test
    void onlyCategoriesWithoutRecordsAreReportedAsMissing() {
        when(access.requireDoctorOf(PATIENT)).thenReturn(doctor);
        when(periodResolver.resolve(FROM, TO)).thenReturn(new TimelinePeriod(FROM, TO));
        emptyData();
        VitalSign sign = new VitalSign();
        sign.setSource(MeasurementSource.TYPED);
        when(vitalSignRepository.findByPatientIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(PATIENT, FROM, TO))
                .thenReturn(List.of(sign));
        RoutineRecord mood = new RoutineRecord();
        mood.setType(RoutineType.MOOD);
        mood.setSource(MeasurementSource.TYPED);
        when(routineRepository.findByPatientIdAndTypeAndOccurredAtBetweenOrderByOccurredAtDesc(
                PATIENT, RoutineType.MOOD, FROM, TO)).thenReturn(List.of(mood));

        var timeline = service.timeline(PATIENT, FROM, TO);

        assertThat(timeline.hasData()).isTrue();
        assertThat(timeline.vitalSigns()).hasSize(1);
        assertThat(timeline.moods()).hasSize(1);
        assertThat(timeline.categoriesWithoutData())
                .containsExactly(TimelineCategory.SLEEP, TimelineCategory.OCCURRENCES, TimelineCategory.WEIGHT);
    }

    @Test
    void everyAreaReadIsLoggedWithTheDoctor() {
        when(access.requireDoctorOf(PATIENT)).thenReturn(doctor);
        when(periodResolver.resolve(null, null)).thenReturn(new TimelinePeriod(FROM, TO));
        emptyData();

        service.timeline(PATIENT, null, null);

        verify(accessLog).record(doctor.getId(), PATIENT, PatientDataArea.MEASUREMENTS);
        verify(accessLog).record(doctor.getId(), PATIENT, PatientDataArea.ROUTINE);
        verify(accessLog).record(doctor.getId(), PATIENT, PatientDataArea.OCCURRENCES);
    }

    @Test
    void doctorWithoutLinkReadsNothing() {
        when(access.requireDoctorOf(PATIENT)).thenThrow(new AccessDeniedException("sem vinculo"));

        assertThatThrownBy(() -> service.timeline(PATIENT, null, null)).isInstanceOf(AccessDeniedException.class);
        verify(vitalSignRepository, never()).findByPatientIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(any(), any(),
                any());
        verify(accessLog, never()).record(any(), any(), any());
    }
}
