package com.mentesvivas.backend.repository.notification;

import com.mentesvivas.backend.entity.notification.CaregiverNotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CaregiverNotificationPreferencesRepository
        extends JpaRepository<CaregiverNotificationPreferences, UUID> {}
