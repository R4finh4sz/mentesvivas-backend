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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionExpiryNotifierTest {

    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");
    private static final UUID PATIENT = UUID.randomUUID();

    @Mock
    private PrescriptionRepository repository;
    @Mock
    private PrescriptionItemRepository itemRepository;
    @Mock
    private CaregiverPatientLinkRepository linkRepository;
    @Mock
    private NotificationService notifications;

    private PrescriptionExpiryNotifier notifier;
    private Prescription prescription;

    @BeforeEach
    void setUp() {
        notifier = new PrescriptionExpiryNotifier(repository, itemRepository, linkRepository, notifications,
                new PrescriptionProperties(30, 5, false, "https://x/"), Clock.fixed(NOW, ZoneOffset.UTC));
        prescription = new Prescription();
        prescription.setId(UUID.randomUUID());
        prescription.setPatientId(PATIENT);
        when(repository.findByStatusAndReplacedByIdIsNullAndExpiryNoticeSentAtIsNullAndValidUntilBetween(
                PrescriptionStatus.SIGNED, NOW, NOW.plusSeconds(5 * 86400L))).thenReturn(List.of(prescription));
    }

    private PrescriptionItem item(boolean continuous) {
        PrescriptionItem item = new PrescriptionItem();
        item.setContinuousUse(continuous);
        return item;
    }

    @Test
    void notifiesEveryActiveCaregiverAboutAContinuousUsePrescriptionAndMarksTheNotice() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(itemRepository.findByPrescriptionIdOrderByPositionAsc(prescription.getId()))
                .thenReturn(List.of(item(false), item(true)));
        when(linkRepository.findByPatientIdAndStatus(PATIENT, LinkStatus.ACTIVE)).thenReturn(List.of(
                new CaregiverPatientLink(first, PATIENT, LinkStatus.ACTIVE),
                new CaregiverPatientLink(second, PATIENT, LinkStatus.ACTIVE)));

        assertThat(notifier.notifyExpiring()).isEqualTo(1);

        verify(notifications, org.mockito.Mockito.times(2))
                .notify(any(UUID.class), any(NotificationType.class), any(), any());
        assertThat(prescription.getExpiryNoticeSentAt()).isEqualTo(NOW);
    }

    @Test
    void prescriptionWithoutContinuousUseIsNotNotified() {
        when(itemRepository.findByPrescriptionIdOrderByPositionAsc(prescription.getId()))
                .thenReturn(List.of(item(false)));

        assertThat(notifier.notifyExpiring()).isZero();

        verify(notifications, never()).notify(any(), any(), any(), any());
        assertThat(prescription.getExpiryNoticeSentAt()).isNull();
    }
}
