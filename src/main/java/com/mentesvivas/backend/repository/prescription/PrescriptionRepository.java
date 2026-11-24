package com.mentesvivas.backend.repository.prescription;

import com.mentesvivas.backend.entity.prescription.Prescription;
import com.mentesvivas.backend.entity.prescription.PrescriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {

    List<Prescription> findByPatientIdAndDoctorIdOrderByCreatedAtDesc(UUID patientId, UUID doctorId);

    List<Prescription> findByPatientIdAndStatusOrderBySignedAtDesc(UUID patientId, PrescriptionStatus status);

    Optional<Prescription> findByIdAndDoctorId(UUID id, UUID doctorId);

    Optional<Prescription> findByIdAndPatientIdAndStatus(UUID id, UUID patientId, PrescriptionStatus status);

    Optional<Prescription> findByVerificationCodeAndStatus(String verificationCode, PrescriptionStatus status);

    boolean existsByReplacesIdAndStatus(UUID replacesId, PrescriptionStatus status);

    List<Prescription> findByStatusAndReplacedByIdIsNullAndExpiryNoticeSentAtIsNullAndValidUntilBetween(
            PrescriptionStatus status, Instant from, Instant to);
}
