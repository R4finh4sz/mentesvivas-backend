package com.mentesvivas.backend.service.notification;

import com.mentesvivas.backend.dto.notification.NotificationPreferences;
import com.mentesvivas.backend.entity.notification.CaregiverNotificationPreferences;
import com.mentesvivas.backend.entity.notification.NotificationType;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.repository.notification.CaregiverNotificationPreferencesRepository;
import com.mentesvivas.backend.security.CurrentUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final CurrentUser currentUser;
    private final EntityManager entityManager;
    private final CaregiverNotificationPreferencesRepository repository;

    @Transactional(readOnly = true)
    public NotificationPreferences mine() {
        return preferencesOf(caregiverId());
    }

    @Transactional
    public NotificationPreferences save(NotificationPreferences preferences) {
        UUID caregiverId = caregiverId();
        entityManager.find(User.class, caregiverId, LockModeType.PESSIMISTIC_WRITE);
        return repository.save(new CaregiverNotificationPreferences(caregiverId, preferences)).view();
    }

    public boolean allows(UUID recipient, NotificationType type) {
        return switch (type) {
            case SUNDOWN_REMINDER -> preferencesOf(recipient).sundownEnabled();
            case MEDICATION_REMINDER -> preferencesOf(recipient).medicationEnabled();
            case GEOFENCE_NOTICE -> preferencesOf(recipient).geofenceEnabled();
            default -> true;
        };
    }

    private NotificationPreferences preferencesOf(UUID recipient) {
        return repository.findById(recipient).map(CaregiverNotificationPreferences::view)
                .orElseGet(NotificationPreferences::defaults);
    }

    private UUID caregiverId() {
        User user = currentUser.get();
        if (user.getRole() != Role.CAREGIVER) {
            throw new AccessDeniedException("Acesso restrito a cuidadores.");
        }
        return user.getId();
    }
}
