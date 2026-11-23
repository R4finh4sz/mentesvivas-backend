package com.mentesvivas.backend.repository.patient;

import com.mentesvivas.backend.entity.patient.RoutineRecord;
import com.mentesvivas.backend.entity.patient.RoutineType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoutineRecordRepository extends JpaRepository<RoutineRecord, UUID> {

    List<RoutineRecord> findByPatientIdOrderByOccurredAtDesc(UUID patientId);

    List<RoutineRecord> findByPatientIdAndTypeOrderByOccurredAtDesc(UUID patientId, RoutineType type);
}
