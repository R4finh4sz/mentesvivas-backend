package com.mentesvivas.backend.repository.patient;

import com.mentesvivas.backend.entity.patient.RoutineRecord;
import com.mentesvivas.backend.entity.patient.RoutineType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RoutineRecordRepository extends JpaRepository<RoutineRecord, UUID> {

    List<RoutineRecord> findByPatientIdOrderByOccurredAtDesc(UUID patientId);

    List<RoutineRecord> findByPatientIdAndTypeOrderByOccurredAtDesc(UUID patientId, RoutineType type);

    @Query("select coalesce(sum(r.amountMl), 0) from RoutineRecord r where r.patientId = :patientId"
            + " and r.type = com.mentesvivas.backend.entity.patient.RoutineType.HYDRATION"
            + " and r.occurredAt >= :from and r.occurredAt < :to")
    long sumHydrationMl(@Param("patientId") UUID patientId, @Param("from") Instant from, @Param("to") Instant to);
}
