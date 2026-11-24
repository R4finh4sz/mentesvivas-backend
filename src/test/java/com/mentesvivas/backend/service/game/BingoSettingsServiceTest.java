package com.mentesvivas.backend.service.game;

import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.game.BingoSettings;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.repository.game.BingoSettingsRepository;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.service.link.CaregiverPatientAccess;
import com.mentesvivas.backend.service.patient.PatientCareAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BingoSettingsServiceTest {

    private static final UUID PATIENT = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-24T12:00:00Z");

    @Mock
    private PatientCareAccess access;
    @Mock
    private CaregiverPatientAccess caregiverAccess;
    @Mock
    private BingoSettingsRepository repository;
    @Mock
    private AuditTrail auditTrail;

    private BingoSettingsService service;
    private User caregiver;

    @BeforeEach
    void setUp() {
        service = new BingoSettingsService(access, caregiverAccess, repository, auditTrail,
                Clock.fixed(NOW, ZoneOffset.UTC));
        caregiver = new User();
        caregiver.setId(UUID.randomUUID());
    }

    @Test
    void narrationIsOffByDefault() {
        when(repository.findById(PATIENT)).thenReturn(Optional.empty());

        assertThat(service.get(PATIENT).narrationEnabled()).isFalse();
        verify(access).requireWriter(PATIENT);
    }

    @Test
    void caregiverTurnsNarrationOnAndItIsAudited() {
        when(caregiverAccess.requireRepresentativeOf(PATIENT)).thenReturn(caregiver);
        when(repository.findById(PATIENT)).thenReturn(Optional.empty());

        var response = service.changeNarration(PATIENT, true);

        assertThat(response.narrationEnabled()).isTrue();
        ArgumentCaptor<BingoSettings> saved = ArgumentCaptor.forClass(BingoSettings.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getPatientId()).isEqualTo(PATIENT);
        assertThat(saved.getValue().isNarrationEnabled()).isTrue();
        assertThat(saved.getValue().getUpdatedBy()).isEqualTo(caregiver.getId());
        assertThat(saved.getValue().getUpdatedAt()).isEqualTo(NOW);

        ArgumentCaptor<AuditRecord> audit = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditTrail).record(audit.capture());
        assertThat(audit.getValue().type()).isEqualTo(AuditEventType.BINGO_NARRATION_CHANGED);
        assertThat(audit.getValue().patientUserId()).isEqualTo(PATIENT);
        assertThat(audit.getValue().onBehalfOfUserId()).isEqualTo(PATIENT);
        assertThat(audit.getValue().beforeValue()).isEqualTo("false");
        assertThat(audit.getValue().afterValue()).isEqualTo("true");
    }

    @Test
    void updatesTheExistingRow() {
        BingoSettings existing = new BingoSettings();
        existing.setPatientId(PATIENT);
        existing.setNarrationEnabled(true);
        when(caregiverAccess.requireRepresentativeOf(PATIENT)).thenReturn(caregiver);
        when(repository.findById(PATIENT)).thenReturn(Optional.of(existing));

        service.changeNarration(PATIENT, false);

        assertThat(existing.isNarrationEnabled()).isFalse();
        verify(repository).save(existing);
    }

    @Test
    void nonCaregiverCannotChangeNarration() {
        when(caregiverAccess.requireRepresentativeOf(PATIENT)).thenThrow(new AccessDeniedException("negado"));

        assertThatThrownBy(() -> service.changeNarration(PATIENT, true)).isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).save(any());
        verify(auditTrail, never()).record(any());
    }
}
