package com.mentesvivas.backend.repository.patient;

import com.mentesvivas.backend.entity.patient.DiagnosisRevision;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiagnosisRevisionRepository extends JpaRepository<DiagnosisRevision, UUID> {

    Optional<DiagnosisRevision> findFirstByPatientIdOrderByRecordedAtDescIdDesc(UUID patientId);

    List<DiagnosisRevision> findByPatientIdOrderByRecordedAtDescIdDesc(UUID patientId, Pageable page);
}
