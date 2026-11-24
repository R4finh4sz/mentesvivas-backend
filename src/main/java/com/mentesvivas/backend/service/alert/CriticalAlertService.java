package com.mentesvivas.backend.service.alert;

import com.mentesvivas.backend.entity.alert.SosAlert;
import com.mentesvivas.backend.entity.link.LinkStatus;
import com.mentesvivas.backend.entity.notification.NotificationType;
import com.mentesvivas.backend.repository.alert.SosAlertRepository;
import com.mentesvivas.backend.repository.link.CaregiverPatientLinkRepository;
import com.mentesvivas.backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CriticalAlertService {

    private final SosAlertRepository alerts;
    private final CaregiverPatientLinkRepository caregivers;
    private final NotificationService notifications;
    private final Clock clock;

    @Transactional
    public void create(UUID patientId, UUID actorId, NotificationType type, UUID sourceId, String message) {
        SosAlert alert = alerts.save(new SosAlert(patientId, actorId, type, sourceId, clock.instant()));
        caregivers.findByPatientIdAndStatus(patientId, LinkStatus.ACTIVE).forEach(link ->
                notifications.notify(link.getCaregiverId(), type, alert.getId(), message));
    }
}
