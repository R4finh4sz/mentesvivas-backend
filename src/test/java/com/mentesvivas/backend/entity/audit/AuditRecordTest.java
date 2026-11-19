package com.mentesvivas.backend.entity.audit;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditRecordTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PATIENT = UUID.randomUUID();
    private static final UUID REPRESENTED = UUID.randomUUID();

    @Test
    void ofCarriesOnlyTheTypeAndTheActor() {
        AuditRecord record = AuditRecord.of(AuditEventType.LOGIN_SUCCEEDED, ACTOR);

        assertThat(record.type()).isEqualTo(AuditEventType.LOGIN_SUCCEEDED);
        assertThat(record.actorUserId()).isEqualTo(ACTOR);
        assertThat(record.patientUserId()).isNull();
        assertThat(record.onBehalfOfUserId()).isNull();
        assertThat(record.beforeValue()).isNull();
        assertThat(record.afterValue()).isNull();
    }

    @Test
    void withersReturnNewRecordsKeepingTheOtherFields() {
        AuditRecord base = AuditRecord.of(AuditEventType.ALLERGY_UPDATED, ACTOR);

        AuditRecord full = base.forPatient(PATIENT).onBehalfOf(REPRESENTED).withValues("antes", "depois");

        assertThat(base.patientUserId()).isNull();
        assertThat(full.actorUserId()).isEqualTo(ACTOR);
        assertThat(full.patientUserId()).isEqualTo(PATIENT);
        assertThat(full.onBehalfOfUserId()).isEqualTo(REPRESENTED);
        assertThat(full.beforeValue()).isEqualTo("antes");
        assertThat(full.afterValue()).isEqualTo("depois");
    }

    @Test
    void errataPointsToTheCorrectedEventWithTheJustification() {
        UUID corrected = UUID.randomUUID();

        AuditRecord errata = AuditRecord.errata(ACTOR, corrected, PATIENT, "Valor digitado errado");

        assertThat(errata.type()).isEqualTo(AuditEventType.ERRATA);
        assertThat(errata.correctsEventId()).isEqualTo(corrected);
        assertThat(errata.justification()).isEqualTo("Valor digitado errado");
        assertThat(errata.patientUserId()).isEqualTo(PATIENT);
    }

    @Test
    void valuesLongerThanTheColumnAreTruncated() {
        String longValue = "x".repeat(1500);

        AuditRecord record = AuditRecord.of(AuditEventType.ALLERGY_CREATED, ACTOR).withValues(longValue, longValue);

        assertThat(record.beforeValue()).hasSize(1000);
        assertThat(record.afterValue()).hasSize(1000);
    }
}
