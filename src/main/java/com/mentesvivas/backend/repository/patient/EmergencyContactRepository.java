package com.mentesvivas.backend.repository.patient;

import com.mentesvivas.backend.entity.patient.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, UUID> {

    List<EmergencyContact> findByPatientIdOrderByContactOrderAsc(UUID patientId);

    long countByPatientId(UUID patientId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from EmergencyContact c where c.patientId = :patientId")
    void deleteAllByPatientId(UUID patientId);
}
