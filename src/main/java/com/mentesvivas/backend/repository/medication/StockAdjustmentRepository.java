package com.mentesvivas.backend.repository.medication;

import com.mentesvivas.backend.entity.medication.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, UUID> {

    List<StockAdjustment> findByMedicationIdOrderByAdjustedAtDesc(UUID medicationId);
}
