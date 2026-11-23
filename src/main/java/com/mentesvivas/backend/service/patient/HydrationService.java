package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.config.HydrationProperties;
import com.mentesvivas.backend.dto.patient.HydrationResponse;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.patient.BodyMeasurement;
import com.mentesvivas.backend.entity.patient.PatientProfile;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.repository.patient.BodyMeasurementRepository;
import com.mentesvivas.backend.repository.patient.PatientProfileRepository;
import com.mentesvivas.backend.repository.patient.RoutineRecordRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import com.mentesvivas.backend.service.patient.hydration.HydrationGoal;
import com.mentesvivas.backend.service.patient.hydration.HydrationGoalCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HydrationService {

    private final PatientCareAccess access;
    private final PatientProfileRepository profileRepository;
    private final BodyMeasurementRepository measurementRepository;
    private final RoutineRecordRepository routineRepository;
    private final HydrationGoalCalculator calculator;
    private final HydrationProperties properties;
    private final AccessLog accessLog;
    private final Clock clock;

    @Transactional
    public HydrationResponse today(UUID patientId) {
        User reader = access.requireReader(patientId);
        accessLog.record(reader.getId(), patientId, PatientDataArea.ROUTINE);

        PatientProfile profile = profileRepository.findById(patientId).orElse(null);
        BigDecimal weight = measurementRepository
                .findFirstByPatientIdAndWeightKgIsNotNullOrderByMeasuredAtDesc(patientId)
                .map(BodyMeasurement::getWeightKg)
                .orElse(null);
        String disease = profile == null ? null : profile.getDeclaredDisease();
        Integer doctorGoal = profile == null ? null : profile.getHydrationGoalMl();
        HydrationGoal goal = calculator.calculate(disease, doctorGoal, weight);

        int consumed = (int) consumedToday(patientId);
        return new HydrationResponse(goal.status(), weight, goal.minMl(), goal.maxMl(), goal.consultDoctor(),
                profile != null && profile.isDeclaredDiseaseValidated(), consumed,
                remaining(goal.minMl(), consumed), remaining(goal.maxMl(), consumed));
    }

    private long consumedToday(UUID patientId) {
        LocalDate today = LocalDate.now(clock.withZone(properties.zone()));
        Instant from = today.atStartOfDay(properties.zone()).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(properties.zone()).toInstant();
        return routineRepository.sumHydrationMl(patientId, from, to);
    }

    private Integer remaining(Integer goal, int consumed) {
        return goal == null ? null : Math.max(0, goal - consumed);
    }
}
