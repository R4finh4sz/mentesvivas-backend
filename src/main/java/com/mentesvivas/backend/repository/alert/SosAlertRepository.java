package com.mentesvivas.backend.repository.alert;

import com.mentesvivas.backend.entity.alert.AlertStatus;
import com.mentesvivas.backend.entity.alert.SosAlert;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SosAlertRepository extends JpaRepository<SosAlert, UUID> {

    List<SosAlert> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    List<SosAlert> findByPatientIdAndStatusOrderByCreatedAtDesc(UUID patientId, AlertStatus status);

    List<SosAlert> findByPatientIdAndStatusNotOrderByCreatedAtDesc(UUID patientId, AlertStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SosAlert> findByIdAndPatientId(UUID id, UUID patientId);
}
