package com.mentesvivas.backend.mapper.patient;

import com.mentesvivas.backend.dto.patient.AllergyResponse;
import com.mentesvivas.backend.dto.patient.AppointmentResponse;
import com.mentesvivas.backend.dto.patient.EmergencyContactResponse;
import com.mentesvivas.backend.dto.patient.ExamResponse;
import com.mentesvivas.backend.dto.patient.MeasurementResponse;
import com.mentesvivas.backend.dto.patient.OccurrenceResponse;
import com.mentesvivas.backend.dto.patient.SensitiveChangeResponse;
import com.mentesvivas.backend.dto.patient.RoutineRecordResponse;
import com.mentesvivas.backend.dto.patient.VitalSignResponse;
import com.mentesvivas.backend.entity.patient.Allergy;
import com.mentesvivas.backend.entity.patient.Appointment;
import com.mentesvivas.backend.entity.patient.BodyMeasurement;
import com.mentesvivas.backend.entity.patient.EmergencyContact;
import com.mentesvivas.backend.entity.patient.Exam;
import com.mentesvivas.backend.entity.patient.MeasurementSource;
import com.mentesvivas.backend.entity.patient.Occurrence;
import com.mentesvivas.backend.entity.patient.SensitiveDataChange;
import com.mentesvivas.backend.entity.patient.RoutineRecord;
import com.mentesvivas.backend.entity.patient.VitalSign;

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

    public static VitalSignResponse toResponse(VitalSign sign) {
        return new VitalSignResponse(sign.getId(), sign.getSystolic(), sign.getDiastolic(), sign.getHeartRate(),
                sign.getGlucose(), sign.getGlucoseType(), sign.getTemperature(), sign.getOxygenSaturation(),
                sign.getNotes(), sign.getSource(), sign.getSource() == MeasurementSource.OCR, sign.getRecordedBy(),
                sign.getMeasuredAt());
    }

    public static RoutineRecordResponse toResponse(RoutineRecord record) {
        return new RoutineRecordResponse(record.getId(), record.getType(), record.getMood(), record.getSleepQuality(),
                record.getSleepHours(), record.getMealPeriod(), record.getMealAccepted(), record.getAmountMl(),
                record.getBathCooperation(), record.getUrineFrequency(), record.getUrineAspect(),
                record.getStoolFrequency(), record.getBristolScale(), record.getNotes(), record.getSource(),
                record.getSource() == MeasurementSource.OCR, record.getRecordedBy(), record.getOccurredAt(),
                record.getRecordedAt());
    }

    public static OccurrenceResponse toResponse(Occurrence occurrence) {
        return new OccurrenceResponse(occurrence.getId(), occurrence.getType(), occurrence.getSeverity(),
                occurrence.getDescription(), occurrence.isNeedsHospital(), occurrence.getSource(),
                occurrence.getRecordedBy(), occurrence.getOccurredAt());
    }

    public static AppointmentResponse toResponse(Appointment appointment) {
        return new AppointmentResponse(appointment.getId(), appointment.getType(), appointment.getTitle(),
                appointment.getLocation(), appointment.getNotes(), appointment.getScheduledAt(),
                appointment.getRemindAt(), appointment.getCreatedBy());
    }

    public static ExamResponse toResponse(Exam exam) {
        return new ExamResponse(exam.getId(), exam.getCategory(), exam.getFileType(), exam.getTitle(),
                exam.getFileName(), exam.getContentType(), exam.getSizeBytes(), exam.getExamDate(), exam.getNotes(),
                exam.getUploadedBy(), exam.getCreatedAt());
    }
}
