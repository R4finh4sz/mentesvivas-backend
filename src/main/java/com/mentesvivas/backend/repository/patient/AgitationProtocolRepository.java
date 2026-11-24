package com.mentesvivas.backend.repository.patient;

import com.mentesvivas.backend.entity.patient.AgitationProtocol;
import com.mentesvivas.backend.entity.patient.OccurrenceSeverity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgitationProtocolRepository extends JpaRepository<AgitationProtocol, UUID> {

    Optional<AgitationProtocol> findFirstBySeverityOrderByApprovedAtDescIdDesc(OccurrenceSeverity severity);
}
