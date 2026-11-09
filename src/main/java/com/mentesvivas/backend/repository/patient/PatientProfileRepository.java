package com.mentesvivas.backend.repository.patient;

import com.mentesvivas.backend.entity.patient.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PatientProfileRepository extends JpaRepository<PatientProfile, UUID> {
}
