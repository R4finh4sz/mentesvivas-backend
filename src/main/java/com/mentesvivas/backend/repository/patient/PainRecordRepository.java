package com.mentesvivas.backend.repository.patient;

import com.mentesvivas.backend.entity.patient.PainRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PainRecordRepository extends JpaRepository<PainRecord, UUID> {

    List<PainRecord> findByPatientIdOrderByRecordedAtDescIdDesc(UUID patientId, Pageable page);
}
