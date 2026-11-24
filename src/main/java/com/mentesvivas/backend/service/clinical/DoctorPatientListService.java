package com.mentesvivas.backend.service.clinical;

import com.mentesvivas.backend.dto.clinical.DoctorPatientListItem;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.service.audit.AccessLog;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DoctorPatientListService {

    private static final String QUERY = """
            WITH linked AS (
                SELECT patient_id FROM doctor_patient_links WHERE doctor_id = :doctor AND status = 'ACTIVE'
            ), activity AS (
                SELECT patient_id, measured_at AS activity_at FROM vital_signs WHERE patient_id IN (SELECT patient_id FROM linked)
                UNION ALL
                SELECT patient_id, recorded_at FROM routine_records WHERE patient_id IN (SELECT patient_id FROM linked)
                UNION ALL
                SELECT patient_id, occurred_at FROM occurrences WHERE patient_id IN (SELECT patient_id FROM linked)
                UNION ALL
                SELECT patient_id, recorded_at FROM dose_records WHERE NOT duplicate AND patient_id IN (SELECT patient_id FROM linked)
                UNION ALL
                SELECT patient_id, measured_at FROM body_measurements WHERE patient_id IN (SELECT patient_id FROM linked)
                UNION ALL
                SELECT patient_id, recorded_at FROM pain_records WHERE patient_id IN (SELECT patient_id FROM linked)
            ), latest AS (
                SELECT patient_id, MAX(activity_at) AS last_activity_at FROM activity GROUP BY patient_id
            ), directory AS (
                SELECT u.id, u.full_name, u.birth_date, l.last_activity_at,
                    EXISTS (SELECT 1 FROM sos_alerts a WHERE a.patient_id = u.id AND a.status <> 'RESOLVED') AS pending_alert
                FROM linked
                JOIN users u ON u.id = linked.patient_id
                LEFT JOIN latest l ON l.patient_id = u.id
            )
            SELECT id, full_name, birth_date, last_activity_at, pending_alert FROM directory
            WHERE (CAST(:pending AS BOOLEAN) IS NULL OR pending_alert = :pending)
              AND (CAST(:since AS TIMESTAMP WITH TIME ZONE) IS NULL OR last_activity_at >= :since)
            ORDER BY last_activity_at DESC NULLS LAST, full_name, id
            LIMIT :size OFFSET :offset
            """;

    private final DoctorPatientAccess doctors;
    private final JdbcClient jdbc;
    private final AccessLog accessLog;

    @Transactional
    public List<DoctorPatientListItem> list(Boolean pending, Instant since, int page, int size) {
        UUID doctorId = doctors.requireDoctor().getId();
        List<DoctorPatientListItem> patients = jdbc.sql(QUERY)
                .param("doctor", doctorId)
                .param("pending", pending)
                .param("since", since == null ? null : since.atOffset(ZoneOffset.UTC))
                .param("size", size)
                .param("offset", (long) page * size)
                .query((row, index) -> {
                    OffsetDateTime activity = row.getObject("last_activity_at", OffsetDateTime.class);
                    return new DoctorPatientListItem(row.getObject("id", UUID.class), row.getString("full_name"),
                            row.getObject("birth_date", LocalDate.class), activity == null ? null : activity.toInstant(),
                            row.getBoolean("pending_alert"));
                }).list();
        patients.forEach(patient -> accessLog.record(doctorId, patient.id(), PatientDataArea.PATIENT_LIST));
        return patients;
    }
}
