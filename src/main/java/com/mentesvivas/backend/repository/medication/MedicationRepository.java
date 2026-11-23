package com.mentesvivas.backend.repository.medication;

import com.mentesvivas.backend.entity.medication.Medication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicationRepository extends JpaRepository<Medication, UUID> {

    List<Medication> findByPatientIdOrderByNameAsc(UUID patientId);

    List<Medication> findByPatientIdAndActiveTrueOrderByNameAsc(UUID patientId);

    Optional<Medication> findByIdAndPatientId(UUID id, UUID patientId);
}
