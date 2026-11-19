package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.patient.ChangeType;
import com.mentesvivas.backend.entity.patient.SensitiveDataChange;
import com.mentesvivas.backend.entity.patient.SensitiveField;
import com.mentesvivas.backend.repository.patient.SensitiveDataChangeRepository;
import com.mentesvivas.backend.service.audit.AuditTrail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SensitiveChangeRecorderTest {

    private static final UUID PATIENT = UUID.randomUUID();
    private static final UUID AUTHOR = UUID.randomUUID();

    @Mock
    private SensitiveDataChangeRepository repository;
    @Mock
    private AuditTrail auditTrail;

    private SensitiveChangeRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new SensitiveChangeRecorder(repository, auditTrail);
    }

    private AuditRecord audited() {
        ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditTrail).record(captor.capture());
        return captor.getValue();
    }

    @Test
    void keepsTheReadableChangeAndAddsTheAuditEventWithBeforeAndAfter() {
        recorder.record(PATIENT, SensitiveField.DECLARED_DISEASE, ChangeType.UPDATED, "resumo", AUTHOR, "antes", "depois");

        verify(repository).save(org.mockito.ArgumentMatchers.any(SensitiveDataChange.class));
        AuditRecord record = audited();
        assertThat(record.type()).isEqualTo(AuditEventType.DECLARED_DISEASE_CHANGED);
        assertThat(record.actorUserId()).isEqualTo(AUTHOR);
        assertThat(record.patientUserId()).isEqualTo(PATIENT);
        assertThat(record.beforeValue()).isEqualTo("antes");
        assertThat(record.afterValue()).isEqualTo("depois");
    }

    @Test
    void changeWithoutValuesUsesTheSummaryAsTheAfterValue() {
        recorder.record(PATIENT, SensitiveField.EMERGENCY_CONTACTS, ChangeType.REPLACED, "2 contatos", AUTHOR);

        AuditRecord record = audited();
        assertThat(record.type()).isEqualTo(AuditEventType.EMERGENCY_CONTACTS_REPLACED);
        assertThat(record.beforeValue()).isNull();
        assertThat(record.afterValue()).isEqualTo("2 contatos");
    }

    @Test
    void mapsEachFieldAndChangeTypeToItsAuditType() {
        recorder.record(PATIENT, SensitiveField.ALLERGY, ChangeType.CREATED, "s", AUTHOR);
        recorder.record(PATIENT, SensitiveField.ALLERGY, ChangeType.UPDATED, "s", AUTHOR);
        recorder.record(PATIENT, SensitiveField.ALLERGY, ChangeType.DELETED, "s", AUTHOR);
        recorder.record(PATIENT, SensitiveField.ALLERGY_STATUS, ChangeType.UPDATED, "s", AUTHOR);

        ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditTrail, org.mockito.Mockito.times(4)).record(captor.capture());
        assertThat(captor.getAllValues()).extracting(AuditRecord::type).containsExactly(
                AuditEventType.ALLERGY_CREATED, AuditEventType.ALLERGY_UPDATED, AuditEventType.ALLERGY_DELETED,
                AuditEventType.ALLERGY_STATUS_CHANGED);
    }
}
