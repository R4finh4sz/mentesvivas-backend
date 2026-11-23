package com.mentesvivas.backend.service.medication;

import com.mentesvivas.backend.config.AdherenceProperties;
import com.mentesvivas.backend.dto.medication.AdherenceResponse;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.medication.DoseRecord;
import com.mentesvivas.backend.entity.medication.Medication;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.repository.medication.DoseRecordRepository;
import com.mentesvivas.backend.repository.medication.MedicationRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import com.mentesvivas.backend.service.patient.PatientCareAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdherenceService {

    private final PatientCareAccess access;
    private final MedicationRepository medicationRepository;
    private final DoseRecordRepository doseRepository;
    private final AdherenceCalculator calculator;
    private final AdherenceProperties properties;
    private final AccessLog accessLog;
    private final Clock clock;

    @Transactional
    public AdherenceResponse calculate(UUID patientId) {
        User reader = access.requireReader(patientId);
        accessLog.record(reader.getId(), patientId, PatientDataArea.MEDICATIONS);
        Instant to = clock.instant();
        Instant from = to.minus(Duration.ofDays(properties.periodDays()));
        List<Medication> medications = medicationRepository.findByPatientIdAndActiveTrueOrderByNameAsc(patientId);
        Set<UUID> activeIds = medications.stream().map(Medication::getId).collect(Collectors.toSet());
        List<DoseRecord> doses = doseRepository
                .findByPatientIdAndScheduledForBetweenOrderByScheduledForDesc(patientId, from, to).stream()
                .filter(dose -> !dose.isDuplicate() && activeIds.contains(dose.getMedicationId())).toList();
        return calculator.calculate(medications, doses, from, to, clock.getZone());
    }
}
