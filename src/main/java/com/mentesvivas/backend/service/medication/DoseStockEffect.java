package com.mentesvivas.backend.service.medication;

import com.mentesvivas.backend.entity.medication.DoseRecord;
import com.mentesvivas.backend.entity.medication.DoseStatus;
import com.mentesvivas.backend.entity.medication.Medication;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DoseStockEffect {

    public void apply(Medication medication, DoseRecord dose, DoseStatus newStatus) {
        boolean wasTaken = dose.getStatus() == DoseStatus.TAKEN;
        boolean willBeTaken = newStatus == DoseStatus.TAKEN;
        if (wasTaken && !willBeTaken) {
            medication.setStockQuantity(medication.getStockQuantity().add(dose.getStockDiscounted()));
            dose.setStockDiscounted(BigDecimal.ZERO);
        } else if (!wasTaken && willBeTaken) {
            dose.setStockDiscounted(discount(medication));
        }
    }

    public BigDecimal discount(Medication medication) {
        BigDecimal discounted = medication.getUnitsPerDose().min(medication.getStockQuantity());
        medication.setStockQuantity(medication.getStockQuantity().subtract(discounted));
        return discounted;
    }
}
