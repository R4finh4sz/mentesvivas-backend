package com.mentesvivas.backend.service.game;

import com.mentesvivas.backend.dto.game.BingoSettingsResponse;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.game.BingoSettings;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.repository.game.BingoSettingsRepository;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.service.link.CaregiverPatientAccess;
import com.mentesvivas.backend.service.patient.PatientCareAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BingoSettingsService {

    private final PatientCareAccess access;
    private final CaregiverPatientAccess caregiverAccess;
    private final BingoSettingsRepository repository;
    private final AuditTrail auditTrail;
    private final Clock clock;

    @Transactional(readOnly = true)
    public BingoSettingsResponse get(UUID patientId) {
        access.requireWriter(patientId);
        return new BingoSettingsResponse(isNarrationEnabled(patientId));
    }

    @Transactional
    public BingoSettingsResponse changeNarration(UUID patientId, boolean enabled) {
        User caregiver = caregiverAccess.requireRepresentativeOf(patientId);
        boolean before = isNarrationEnabled(patientId);

        BingoSettings settings = repository.findById(patientId).orElseGet(BingoSettings::new);
        settings.setPatientId(patientId);
        settings.setNarrationEnabled(enabled);
        settings.setUpdatedBy(caregiver.getId());
        settings.setUpdatedAt(clock.instant());
        repository.save(settings);

        auditTrail.record(AuditRecord.of(AuditEventType.BINGO_NARRATION_CHANGED, caregiver.getId())
                .forPatient(patientId).onBehalfOf(patientId)
                .withValues(String.valueOf(before), String.valueOf(enabled)));
        return new BingoSettingsResponse(enabled);
    }

    public boolean isNarrationEnabled(UUID patientId) {
        return repository.findById(patientId).map(BingoSettings::isNarrationEnabled).orElse(false);
    }
}
