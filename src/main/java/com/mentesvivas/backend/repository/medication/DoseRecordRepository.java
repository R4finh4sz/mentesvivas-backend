package com.mentesvivas.backend.repository.medication;

import com.mentesvivas.backend.entity.medication.DoseRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoseRecordRepository extends JpaRepository<DoseRecord, UUID> {

    Optional<DoseRecord> findFirstByMedicationIdAndScheduledForAndDuplicateFalse(UUID medicationId,
                                                                                Instant scheduledFor);

    Optional<DoseRecord> findByIdAndPatientId(UUID id, UUID patientId);

    List<DoseRecord> findByPatientIdAndScheduledForBetweenOrderByScheduledForDesc(UUID patientId, Instant from,
                                                                                  Instant to);
}
