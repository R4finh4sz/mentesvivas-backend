package com.mentesvivas.backend.repository.patient;

import com.mentesvivas.backend.entity.patient.BodyMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BodyMeasurementRepository extends JpaRepository<BodyMeasurement, UUID> {

    List<BodyMeasurement> findByPatientIdOrderByMeasuredAtDesc(UUID patientId);

    Optional<BodyMeasurement> findFirstByPatientIdAndWeightKgIsNotNullOrderByMeasuredAtDesc(UUID patientId);

    Optional<BodyMeasurement> findFirstByPatientIdAndHeightMIsNotNullOrderByMeasuredAtDesc(UUID patientId);
}
