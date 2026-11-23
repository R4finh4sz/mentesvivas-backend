package com.mentesvivas.backend.service.alert;

import com.mentesvivas.backend.dto.alert.AlertDetailResponse;
import com.mentesvivas.backend.dto.alert.AlertResponse;
import com.mentesvivas.backend.entity.alert.AlertStatus;
import com.mentesvivas.backend.entity.alert.SosAlert;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.mapper.alert.AlertMapper;
import com.mentesvivas.backend.repository.alert.SosAlertRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.service.link.CaregiverPatientAccess;
import com.mentesvivas.backend.service.patient.PatientCareAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final PatientCareAccess access;
    private final CaregiverPatientAccess caregiverAccess;
    private final SosAlertRepository repository;
    private final AlertDetailLoader detailLoader;
    private final AccessLog accessLog;
    private final AuditTrail auditTrail;
    private final Clock clock;

    @Transactional
    public List<AlertResponse> list(UUID patientId, boolean pendingOnly) {
        User reader = access.requireReader(patientId);
        accessLog.record(reader.getId(), patientId, PatientDataArea.ALERTS);
        List<SosAlert> found = pendingOnly
                ? repository.findByPatientIdAndStatusNotOrderByCreatedAtDesc(patientId, AlertStatus.RESOLVED)
                : repository.findByPatientIdOrderByCreatedAtDesc(patientId);
        return found.stream().map(AlertMapper::toResponse).toList();
    }

    @Transactional
    public AlertDetailResponse detail(UUID patientId, UUID alertId) {
        User reader = access.requireReader(patientId);
        accessLog.record(reader.getId(), patientId, PatientDataArea.ALERTS);
        return detailLoader.load(find(patientId, alertId));
    }

    @Transactional
    public AlertResponse markSeen(UUID patientId, UUID alertId) {
        return advance(caregiverAccess.requireRepresentativeOf(patientId), patientId, alertId, AlertStatus.SEEN,
                AuditEventType.ALERT_SEEN);
    }

    @Transactional
    public AlertResponse acknowledge(UUID patientId, UUID alertId) {
        return advance(caregiverAccess.requireRepresentativeOf(patientId), patientId, alertId,
                AlertStatus.ACKNOWLEDGED, AuditEventType.ALERT_ACKNOWLEDGED);
    }

    @Transactional
    public AlertResponse resolve(UUID patientId, UUID alertId) {
        return advance(access.requireWriter(patientId), patientId, alertId, AlertStatus.RESOLVED,
                AuditEventType.ALERT_RESOLVED);
    }

    private AlertResponse advance(User actor, UUID patientId, UUID alertId, AlertStatus target, AuditEventType event) {
        SosAlert alert = find(patientId, alertId);
        if (!alert.advance(target, actor.getId(), clock.instant())) {
            throw new ConflictException("O alerta ja esta no estado %s ou alem dele.".formatted(target));
        }
        auditTrail.record(AuditRecord.of(event, actor.getId()).forPatient(patientId));
        return AlertMapper.toResponse(repository.save(alert));
    }

    private SosAlert find(UUID patientId, UUID alertId) {
        return repository.findByIdAndPatientId(alertId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta nao encontrado."));
    }
}
