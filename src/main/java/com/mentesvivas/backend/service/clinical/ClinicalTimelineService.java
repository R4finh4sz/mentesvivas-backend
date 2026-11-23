package com.mentesvivas.backend.service.clinical;

import com.mentesvivas.backend.dto.clinical.TimelineCategory;
import com.mentesvivas.backend.dto.clinical.TimelineResponse;
import com.mentesvivas.backend.dto.patient.MeasurementResponse;
import com.mentesvivas.backend.dto.patient.OccurrenceResponse;
import com.mentesvivas.backend.dto.patient.RoutineRecordResponse;
import com.mentesvivas.backend.dto.patient.VitalSignResponse;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.patient.RoutineType;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.mapper.patient.PatientMapper;
import com.mentesvivas.backend.repository.patient.BodyMeasurementRepository;
import com.mentesvivas.backend.repository.patient.OccurrenceRepository;
import com.mentesvivas.backend.repository.patient.RoutineRecordRepository;
import com.mentesvivas.backend.repository.patient.VitalSignRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import com.mentesvivas.backend.service.clinical.timeline.TimelinePeriod;
import com.mentesvivas.backend.service.clinical.timeline.TimelinePeriodResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicalTimelineService {

    private final DoctorPatientAccess access;
    private final TimelinePeriodResolver periodResolver;
    private final VitalSignRepository vitalSignRepository;
    private final RoutineRecordRepository routineRepository;
    private final OccurrenceRepository occurrenceRepository;
    private final BodyMeasurementRepository measurementRepository;
    private final AccessLog accessLog;

    @Transactional
    public TimelineResponse timeline(UUID patientId, Instant from, Instant to) {
        User doctor = access.requireDoctorOf(patientId);
        TimelinePeriod period = periodResolver.resolve(from, to);
        accessLog.record(doctor.getId(), patientId, PatientDataArea.MEASUREMENTS);
        accessLog.record(doctor.getId(), patientId, PatientDataArea.ROUTINE);
        accessLog.record(doctor.getId(), patientId, PatientDataArea.OCCURRENCES);

        List<VitalSignResponse> vitalSigns = vitalSignRepository
                .findByPatientIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(patientId, period.from(), period.to())
                .stream().map(PatientMapper::toResponse).toList();
        List<RoutineRecordResponse> moods = routine(patientId, RoutineType.MOOD, period);
        List<RoutineRecordResponse> sleeps = routine(patientId, RoutineType.SLEEP, period);
        List<OccurrenceResponse> occurrences = occurrenceRepository
                .findByPatientIdAndOccurredAtBetweenOrderByOccurredAtDesc(patientId, period.from(), period.to())
                .stream().map(PatientMapper::toResponse).toList();
        List<MeasurementResponse> weights = measurementRepository
                .findByPatientIdAndWeightKgIsNotNullAndMeasuredAtBetweenOrderByMeasuredAtDesc(
                        patientId, period.from(), period.to())
                .stream().map(PatientMapper::toResponse).toList();

        List<TimelineCategory> missing = new ArrayList<>();
        addIfEmpty(missing, TimelineCategory.VITAL_SIGNS, vitalSigns);
        addIfEmpty(missing, TimelineCategory.MOOD, moods);
        addIfEmpty(missing, TimelineCategory.SLEEP, sleeps);
        addIfEmpty(missing, TimelineCategory.OCCURRENCES, occurrences);
        addIfEmpty(missing, TimelineCategory.WEIGHT, weights);

        return new TimelineResponse(period.from(), period.to(), missing.size() < TimelineCategory.values().length,
                missing, vitalSigns, moods, sleeps, occurrences, weights);
    }

    private List<RoutineRecordResponse> routine(UUID patientId, RoutineType type, TimelinePeriod period) {
        return routineRepository
                .findByPatientIdAndTypeAndOccurredAtBetweenOrderByOccurredAtDesc(patientId, type, period.from(),
                        period.to())
                .stream().map(PatientMapper::toResponse).toList();
    }

    private void addIfEmpty(List<TimelineCategory> missing, TimelineCategory category, List<?> items) {
        if (items.isEmpty()) {
            missing.add(category);
        }
    }
}
