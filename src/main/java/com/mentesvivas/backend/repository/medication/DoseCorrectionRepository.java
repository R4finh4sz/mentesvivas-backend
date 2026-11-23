package com.mentesvivas.backend.repository.medication;

import com.mentesvivas.backend.entity.medication.DoseCorrection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DoseCorrectionRepository extends JpaRepository<DoseCorrection, UUID> {

    List<DoseCorrection> findByDoseIdOrderByCorrectedAtAsc(UUID doseId);
}
