package com.mentesvivas.backend.service.medication;

import com.mentesvivas.backend.dto.medication.MedicationResponse;
import com.mentesvivas.backend.dto.medication.MedicationWarning;
import com.mentesvivas.backend.entity.medication.Medication;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MedicationResponseFactory {

    private final MedicationStockCalculator stockCalculator;

    public MedicationResponse create(Medication medication, List<MedicationWarning> warnings) {
        List<LocalTime> times = medication.getTimes().stream().sorted().toList();
        return new MedicationResponse(medication.getId(), medication.getName(), medication.getStrength(),
                medication.getUnitsPerDose(), times, medication.getStockQuantity(),
                stockCalculator.daysLeft(medication), stockCalculator.isCritical(medication),
                medication.getOrigin(), medication.isActive(), medication.getCreatedBy(),
                medication.getCreatedAt(), warnings);
    }
}
