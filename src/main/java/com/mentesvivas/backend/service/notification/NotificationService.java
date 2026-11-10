package com.mentesvivas.backend.service.notification;

import com.mentesvivas.backend.dto.notification.NotificationResponse;
import com.mentesvivas.backend.entity.notification.Notification;
import com.mentesvivas.backend.entity.notification.NotificationType;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.mapper.link.LinkMapper;
import com.mentesvivas.backend.repository.notification.NotificationRepository;
import com.mentesvivas.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final CurrentUser currentUser;

    public void notify(UUID recipientId, NotificationType type, UUID referenceId, String message) {
        repository.save(new Notification(recipientId, type, referenceId, message));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> mine() {
        return repository.findByRecipientIdOrderByCreatedAtDesc(currentUser.get().getId()).stream()
                .map(LinkMapper::toResponse)
                .toList();
    }

    @Transactional
    public void markAsRead(UUID id) {
        Notification notification = repository.findByIdAndRecipientId(id, currentUser.get().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notificacao nao encontrada."));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
        }
    }
}
