package com.mentesvivas.backend.repository.patient;

import com.mentesvivas.backend.entity.patient.PatientAutonomy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PatientAutonomyRepository extends JpaRepository<PatientAutonomy, UUID> {}
