package com.mentesvivas.backend.repository.patient;

import com.mentesvivas.backend.entity.patient.ConsentScope;
import com.mentesvivas.backend.entity.patient.PatientConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientConsentRepository extends JpaRepository<PatientConsent, UUID> {

    Optional<PatientConsent> findByPatientIdAndScope(UUID patientId, ConsentScope scope);

    List<PatientConsent> findByPatientId(UUID patientId);
}
