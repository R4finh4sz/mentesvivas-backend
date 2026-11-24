package com.mentesvivas.backend.repository.patient;

import com.mentesvivas.backend.entity.patient.ClinicalRangePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClinicalRangePolicyRepository extends JpaRepository<ClinicalRangePolicy, UUID> {

    Optional<ClinicalRangePolicy> findFirstByOrderByApprovedAtDescIdDesc();
}
