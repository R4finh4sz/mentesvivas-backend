package com.mentesvivas.backend.service.prescription;

import com.mentesvivas.backend.entity.prescription.DocumentKind;
import com.mentesvivas.backend.entity.prescription.Prescription;
import com.mentesvivas.backend.entity.prescription.PrescriptionItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Component
public class PrescriptionCompleteness {

    public List<String> missingFields(Prescription prescription, List<PrescriptionItem> items) {
        List<String> missing = new ArrayList<>();
        if (prescription.getKind() == DocumentKind.PRESCRIPTION) {
            if (items.isEmpty()) {
                missing.add("items");
            }
        } else if (isBlank(prescription.getOrientations())) {
            missing.add("orientations");
        }
        for (int index = 0; index < items.size(); index++) {
            checkItem(items.get(index), index, missing);
        }
        return missing;
    }

    private void checkItem(PrescriptionItem item, int index, List<String> missing) {
        require(item, index, "medicationName", PrescriptionItem::getMedicationName, missing);
        if (isBlank(item.getMedicationName()) && item.getPresentation() == null) {
            return;
        }
        require(item, index, "presentation", PrescriptionItem::getPresentation, missing);
        require(item, index, "dose", PrescriptionItem::getDose, missing);
        require(item, index, "route", PrescriptionItem::getRoute, missing);
        require(item, index, "frequency", PrescriptionItem::getFrequency, missing);
        require(item, index, "duration", PrescriptionItem::getDuration, missing);
        require(item, index, "quantity", PrescriptionItem::getQuantity, missing);
    }

    private void require(PrescriptionItem item, int index, String field,
                         Function<PrescriptionItem, String> getter, List<String> missing) {
        if (isBlank(getter.apply(item))) {
            missing.add("items[%d].%s".formatted(index, field));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
