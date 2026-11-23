package com.mentesvivas.backend.service.medication;

import com.mentesvivas.backend.config.MedicationProperties;
import com.mentesvivas.backend.entity.medication.Medication;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
public class MedicationStockCalculator {

    private final MedicationProperties properties;

    public int daysLeft(Medication medication) {
        BigDecimal dailyUse = medication.getUnitsPerDose().multiply(BigDecimal.valueOf(medication.getTimes().size()));
        if (dailyUse.signum() <= 0) {
            return 0;
        }
        return medication.getStockQuantity().divide(dailyUse, 0, RoundingMode.FLOOR).intValue();
    }

    public boolean isCritical(Medication medication) {
        return daysLeft(medication) <= properties.criticalStockDays();
    }
}
