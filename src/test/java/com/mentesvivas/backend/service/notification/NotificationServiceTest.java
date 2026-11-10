package com.mentesvivas.backend.service.notification;

import com.mentesvivas.backend.entity.notification.Notification;
import com.mentesvivas.backend.entity.notification.NotificationType;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.notification.NotificationRepository;
import com.mentesvivas.backend.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;
    @Mock
    private CurrentUser currentUser;

    private NotificationService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository, currentUser);
        user = new User();
        user.setId(UUID.randomUUID());
        lenient().when(currentUser.get()).thenReturn(user);
    }

    @Test
    void notifyStoresAnUnreadNotificationForTheRecipient() {
        UUID recipient = UUID.randomUUID();

        service.notify(recipient, NotificationType.LINK_ENDED, null, "Encerrado");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getRecipientId()).isEqualTo(recipient);
        assertThat(captor.getValue().getReadAt()).isNull();
    }

    @Test
    void mineReturnsTheOwnNotificationsWithTheReadFlag() {
        Notification read = new Notification(user.getId(), NotificationType.LINK_ACCEPTED, null, "Aceito");
        read.setReadAt(java.time.Instant.now());
        Notification unread = new Notification(user.getId(), NotificationType.LINK_ENDED, null, "Encerrado");
        when(repository.findByRecipientIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(unread, read));

        var result = service.mine();

        assertThat(result).extracting("read").containsExactly(false, true);
    }

    @Test
    void markAsReadSetsTheDateOnceAndRejectsNotificationsOfOthers() {
        UUID id = UUID.randomUUID();
        Notification notification = new Notification(user.getId(), NotificationType.LINK_ENDED, null, "Encerrado");
        when(repository.findByIdAndRecipientId(id, user.getId())).thenReturn(Optional.of(notification));

        service.markAsRead(id);
        var firstRead = notification.getReadAt();
        service.markAsRead(id);

        assertThat(firstRead).isNotNull();
        assertThat(notification.getReadAt()).isEqualTo(firstRead);

        UUID other = UUID.randomUUID();
        when(repository.findByIdAndRecipientId(other, user.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markAsRead(other)).isInstanceOf(ResourceNotFoundException.class);
    }
}
