package com.mentesvivas.backend.repository.medication;

import com.mentesvivas.backend.entity.medication.DoseRecord;
import com.mentesvivas.backend.dto.clinical.TimelineDose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoseRecordRepository extends JpaRepository<DoseRecord, UUID> {

    Optional<DoseRecord> findFirstByMedicationIdAndScheduledForAndDuplicateFalse(UUID medicationId,
                                                                                Instant scheduledFor);

    Optional<DoseRecord> findByIdAndPatientId(UUID id, UUID patientId);

    Optional<DoseRecord> findByMedicationIdAndRecordedByAndClientOperationId(
            UUID medicationId, UUID recordedBy, UUID clientOperationId);

    List<DoseRecord> findByPatientIdAndScheduledForBetweenOrderByScheduledForDesc(UUID patientId, Instant from,
                                                                                  Instant to);

    @Query("""
            select new com.mentesvivas.backend.dto.clinical.TimelineDose(
                medication.name, dose.status, dose.scheduledFor, dose.recordedAt)
            from DoseRecord dose join Medication medication on medication.id = dose.medicationId
            where dose.patientId = :patientId and medication.patientId = :patientId
              and dose.scheduledFor between :from and :to and dose.duplicate = false
            order by dose.scheduledFor desc, dose.recordedAt desc
            """)
    List<TimelineDose> findTimelineDoses(@Param("patientId") UUID patientId, @Param("from") Instant from,
                                         @Param("to") Instant to);
}
