package com.mentesvivas.backend.service.alert;

import com.mentesvivas.backend.dto.alert.AlertDetailResponse;
import com.mentesvivas.backend.entity.alert.SosAlert;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.notification.NotificationType;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.repository.alert.SosAlertRepository;
import com.mentesvivas.backend.security.CurrentPatient;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class SosService {

    private final CurrentPatient currentPatient;
    private final SosRecipients recipients;
    private final SosAlertRepository repository;
    private final NotificationService notifications;
    private final AlertDetailLoader detailLoader;
    private final AuditTrail auditTrail;
    private final Clock clock;

    @Transactional
    public AlertDetailResponse trigger() {
        User patient = currentPatient.get();
        var caregiverIds = recipients.requireCaregiversOrContacts(patient.getId());
        SosAlert alert = repository.save(new SosAlert(patient.getId(), clock.instant()));
        String message = "%s acionou o SOS.".formatted(patient.getFullName());
        caregiverIds.forEach(caregiverId ->
                notifications.notify(caregiverId, NotificationType.SOS_ALERT, alert.getId(), message));
        auditTrail.record(AuditRecord.of(AuditEventType.SOS_TRIGGERED, patient.getId()).forPatient(patient.getId()));
        return detailLoader.load(alert);
    }
}
