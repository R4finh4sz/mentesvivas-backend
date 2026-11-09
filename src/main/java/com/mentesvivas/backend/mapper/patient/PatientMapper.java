package com.mentesvivas.backend.mapper.patient;

import com.mentesvivas.backend.dto.patient.AllergyResponse;
import com.mentesvivas.backend.dto.patient.EmergencyContactResponse;
import com.mentesvivas.backend.dto.patient.MeasurementResponse;
import com.mentesvivas.backend.dto.patient.SensitiveChangeResponse;
import com.mentesvivas.backend.entity.patient.Allergy;
import com.mentesvivas.backend.entity.patient.BodyMeasurement;
import com.mentesvivas.backend.entity.patient.EmergencyContact;
import com.mentesvivas.backend.entity.patient.SensitiveDataChange;

public final class PatientMapper {

    private PatientMapper() {
    }

    public static AllergyResponse toResponse(Allergy allergy) {
        return new AllergyResponse(allergy.getId(), allergy.getCategory(), allergy.getDescription(),
                allergy.getSeverity(), allergy.getCreatedAt());
    }

    public static MeasurementResponse toResponse(BodyMeasurement measurement) {
        return new MeasurementResponse(measurement.getWeightKg(), measurement.getHeightM(), measurement.getMeasuredAt());
    }

    public static EmergencyContactResponse toResponse(EmergencyContact contact) {
        return new EmergencyContactResponse(contact.getName(), contact.getRelationship(), contact.getPhone(),
                contact.getContactOrder());
    }

    public static SensitiveChangeResponse toResponse(SensitiveDataChange change) {
        return new SensitiveChangeResponse(change.getField(), change.getChangeType(), change.getSummary(),
                change.getChangedBy(), change.getChangedAt());
    }
}
