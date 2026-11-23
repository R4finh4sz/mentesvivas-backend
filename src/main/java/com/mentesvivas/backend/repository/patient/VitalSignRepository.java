package com.mentesvivas.backend.repository.patient;

import com.mentesvivas.backend.entity.patient.VitalSign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VitalSignRepository extends JpaRepository<VitalSign, UUID> {

    List<VitalSign> findByPatientIdOrderByMeasuredAtDesc(UUID patientId);
}
