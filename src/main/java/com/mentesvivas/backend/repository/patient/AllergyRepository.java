package com.mentesvivas.backend.repository.patient;

import com.mentesvivas.backend.entity.patient.Allergy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AllergyRepository extends JpaRepository<Allergy, UUID> {

    List<Allergy> findByPatientIdOrderByCreatedAtAsc(UUID patientId);

    boolean existsByPatientId(UUID patientId);

    Optional<Allergy> findByIdAndPatientId(UUID id, UUID patientId);
}
