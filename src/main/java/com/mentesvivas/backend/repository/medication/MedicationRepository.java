package com.mentesvivas.backend.repository.medication;

import com.mentesvivas.backend.entity.medication.Medication;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicationRepository extends JpaRepository<Medication, UUID> {

    List<Medication> findByPatientIdOrderByNameAsc(UUID patientId);

    List<Medication> findByPatientIdAndActiveTrueOrderByNameAsc(UUID patientId);

    Optional<Medication> findByIdAndPatientId(UUID id, UUID patientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Medication m where m.id = :id and m.patientId = :patientId")
    Optional<Medication> lockByIdAndPatientId(@Param("id") UUID id, @Param("patientId") UUID patientId);
}
