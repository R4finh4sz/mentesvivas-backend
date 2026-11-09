package com.mentesvivas.backend.repository.patient;

import com.mentesvivas.backend.entity.patient.SensitiveDataChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SensitiveDataChangeRepository extends JpaRepository<SensitiveDataChange, UUID> {

    List<SensitiveDataChange> findByPatientIdOrderByChangedAtDesc(UUID patientId);
}
