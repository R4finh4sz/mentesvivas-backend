package com.mentesvivas.backend.entity.audit;

import java.util.UUID;

public record AuditRecord(
        AuditEventType type,
        UUID actorUserId,
        UUID patientUserId,
        UUID onBehalfOfUserId,
        UUID correctsEventId,
        String justification,
        String beforeValue,
        String afterValue
) {

    private static final int VALUE_LIMIT = 1000;

    public AuditRecord {
        beforeValue = limit(beforeValue);
        afterValue = limit(afterValue);
    }

    public static AuditRecord of(AuditEventType type, UUID actorUserId) {
        return new AuditRecord(type, actorUserId, null, null, null, null, null, null);
    }

    public AuditRecord forPatient(UUID patientUserId) {
        return new AuditRecord(type, actorUserId, patientUserId, onBehalfOfUserId, correctsEventId, justification,
                beforeValue, afterValue);
    }

    public AuditRecord onBehalfOf(UUID onBehalfOfUserId) {
        return new AuditRecord(type, actorUserId, patientUserId, onBehalfOfUserId, correctsEventId, justification,
                beforeValue, afterValue);
    }

    public AuditRecord withValues(String before, String after) {
        return new AuditRecord(type, actorUserId, patientUserId, onBehalfOfUserId, correctsEventId, justification,
                before, after);
    }

    public static AuditRecord errata(UUID actorUserId, UUID correctedEventId, UUID patientUserId, String justification) {
        return new AuditRecord(AuditEventType.ERRATA, actorUserId, patientUserId, null, correctedEventId,
                justification, null, null);
    }

    private static String limit(String value) {
        if (value == null || value.length() <= VALUE_LIMIT) {
            return value;
        }
        return value.substring(0, VALUE_LIMIT);
    }
}
