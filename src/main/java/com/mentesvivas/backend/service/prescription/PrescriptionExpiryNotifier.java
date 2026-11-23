package com.mentesvivas.backend.service.prescription;

import com.mentesvivas.backend.config.PrescriptionProperties;
import com.mentesvivas.backend.entity.link.CaregiverPatientLink;
import com.mentesvivas.backend.entity.link.LinkStatus;
import com.mentesvivas.backend.entity.notification.NotificationType;
import com.mentesvivas.backend.entity.prescription.Prescription;
import com.mentesvivas.backend.entity.prescription.PrescriptionItem;
import com.mentesvivas.backend.entity.prescription.PrescriptionStatus;
import com.mentesvivas.backend.repository.link.CaregiverPatientLinkRepository;
import com.mentesvivas.backend.repository.prescription.PrescriptionItemRepository;
import com.mentesvivas.backend.repository.prescription.PrescriptionRepository;
import com.mentesvivas.backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrescriptionExpiryNotifier {

    private final PrescriptionRepository repository;
    private final PrescriptionItemRepository itemRepository;
    private final CaregiverPatientLinkRepository linkRepository;
    private final NotificationService notifications;
    private final PrescriptionProperties properties;
    private final Clock clock;

    @Scheduled(cron = "0 0 8 * * *")
    public void run() {
        notifyExpiring();
    }

    @Transactional
    public int notifyExpiring() {
        Instant now = clock.instant();
        Instant limit = now.plus(Duration.ofDays(properties.expiryNoticeDays()));
        int notified = 0;
        for (Prescription prescription : repository
                .findByStatusAndReplacedByIdIsNullAndExpiryNoticeSentAtIsNullAndValidUntilBetween(
                        PrescriptionStatus.SIGNED, now, limit)) {
            if (isContinuousUse(prescription)) {
                notifyCaregivers(prescription);
                prescription.setExpiryNoticeSentAt(now);
                notified++;
            }
        }
        return notified;
    }

    private boolean isContinuousUse(Prescription prescription) {
        List<PrescriptionItem> items = itemRepository.findByPrescriptionIdOrderByPositionAsc(prescription.getId());
        return items.stream().anyMatch(PrescriptionItem::isContinuousUse);
    }

    private void notifyCaregivers(Prescription prescription) {
        for (CaregiverPatientLink link : linkRepository.findByPatientIdAndStatus(prescription.getPatientId(),
                LinkStatus.ACTIVE)) {
            notifications.notify(link.getCaregiverId(), NotificationType.PRESCRIPTION_EXPIRING, prescription.getId(),
                    "Uma receita de uso continuo esta perto de vencer. Solicite a renovacao ao medico.");
        }
    }
}
